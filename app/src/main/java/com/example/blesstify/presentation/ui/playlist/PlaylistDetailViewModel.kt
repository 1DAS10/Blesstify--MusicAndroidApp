package com.example.blesstify.presentation.ui.playlist

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.domain.model.Playlist
import com.example.blesstify.domain.model.PlaylistTrack
import com.example.blesstify.domain.model.Song
import com.example.blesstify.domain.usecase.AddTrackToPlaylistUseCase
import com.example.blesstify.domain.usecase.DeletePlaylistUseCase
import com.example.blesstify.domain.usecase.IsPlaylistLikedUseCase
import com.example.blesstify.domain.usecase.LikePlaylistUseCase
import com.example.blesstify.domain.usecase.UnlikePlaylistUseCase
import com.example.blesstify.domain.usecase.GetCurrentUserUseCase
import com.example.blesstify.domain.usecase.GetPlaylistByIdUseCase
import com.example.blesstify.domain.usecase.GetPlaylistTracksUseCase
import com.example.blesstify.domain.usecase.GetSongsByIdsUseCase
import com.example.blesstify.domain.usecase.RemoveTrackFromPlaylistUseCase
import com.example.blesstify.domain.usecase.SearchSongsUseCase
import com.example.blesstify.domain.usecase.UpdatePlaylistUseCase
import com.example.blesstify.domain.usecase.UploadPlaylistCoverUseCase
import com.example.blesstify.presentation.player.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val tracks: List<PlaylistTrack> = emptyList(),
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOwner: Boolean = false,
    val canEdit: Boolean = false,
    val isDeleted: Boolean = false,
    
    // Dialog states
    val showAddSongsDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showCollaboratorDialog: Boolean = false,
    
    // Search within dialog
    val searchQuery: String = "",
    val searchResults: List<Song> = emptyList(),
    val isSearching: Boolean = false,
    val isAddingTrack: Boolean = false,
    val addedMessage: String? = null,
    val currentPlayingSongId: String? = null,
    val isPlaying: Boolean = false,
    val isCurrentPlaylistPlaying: Boolean = false,
    val isLiked: Boolean = false
)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPlaylistByIdUseCase: GetPlaylistByIdUseCase,
    private val getPlaylistTracksUseCase: GetPlaylistTracksUseCase,
    private val addTrackToPlaylistUseCase: AddTrackToPlaylistUseCase,
    private val removeTrackFromPlaylistUseCase: RemoveTrackFromPlaylistUseCase,
    private val getSongsByIdsUseCase: GetSongsByIdsUseCase,
    private val searchSongsUseCase: SearchSongsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updatePlaylistUseCase: UpdatePlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val uploadPlaylistCoverUseCase: UploadPlaylistCoverUseCase,
    private val likePlaylistUseCase: LikePlaylistUseCase,
    private val unlikePlaylistUseCase: UnlikePlaylistUseCase,
    private val isPlaylistLikedUseCase: IsPlaylistLikedUseCase,
    private val musicController: MusicController
) : ViewModel() {

    private val playlistId: String = savedStateHandle.get<String>("playlistId") ?: ""
    private val currentUserId = getCurrentUserUseCase()?.id ?: ""

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        if (playlistId.isNotBlank()) {
            loadPlaylist()
            observeLikeStatus()
        }
        
        viewModelScope.launch {
            musicController.currentSong.collect { song ->
                val isCurrent = _uiState.value.songs.any { it.id == song?.id }
                _uiState.value = _uiState.value.copy(
                    currentPlayingSongId = song?.id,
                    isCurrentPlaylistPlaying = isCurrent
                )
            }
        }

        viewModelScope.launch {
            musicController.isPlaying.collect { isPlaying ->
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }
        }
    }

    private fun observeLikeStatus() {
        if (currentUserId.isBlank() || playlistId.isBlank()) return
        viewModelScope.launch {
            isPlaylistLikedUseCase(currentUserId, playlistId).collectLatest { isLiked ->
                _uiState.value = _uiState.value.copy(isLiked = isLiked)
            }
        }
    }

    fun toggleLike() {
        if (currentUserId.isBlank() || playlistId.isBlank()) return
        val currentlyLiked = _uiState.value.isLiked
        viewModelScope.launch {
            try {
                if (currentlyLiked) {
                    unlikePlaylistUseCase(currentUserId, playlistId)
                } else {
                    likePlaylistUseCase(currentUserId, playlistId)
                }
            } catch (e: Exception) {
                // Best effort
            }
        }
    }

    fun loadPlaylist() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val playlist = getPlaylistByIdUseCase(playlistId)
                if (playlist == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Playlist not found")
                    return@launch
                }

                val tracks = getPlaylistTracksUseCase(playlistId)
                val isOwner = playlist.ownerId == currentUserId
                val isCollaborator = playlist.collaboratorIds.contains(currentUserId)

                // Update basic info first
                _uiState.value = _uiState.value.copy(
                    playlist = playlist,
                    tracks = tracks,
                    isOwner = isOwner,
                    canEdit = isOwner || isCollaborator
                )

                if (tracks.isNotEmpty()) {
                    val songIds = tracks.map { it.songId }
                    getSongsByIdsUseCase(songIds).collect { result ->
                        when (result) {
                            is Resource.Success -> {
                                val songs = result.data ?: emptyList()
                                val isCurrent = songs.any { it.id == _uiState.value.currentPlayingSongId }
                                _uiState.value = _uiState.value.copy(
                                    songs = songs,
                                    isCurrentPlaylistPlaying = isCurrent,
                                    isLoading = false
                                )
                            }
                            is Resource.Error -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "Failed to load some tracks"
                                )
                            }
                            is Resource.Loading -> {
                                _uiState.value = _uiState.value.copy(isLoading = true)
                            }
                        }
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, songs = emptyList(), isCurrentPlaylistPlaying = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load: ${e.message}"
                )
            }
        }
    }

    fun updatePlaylistInfo(title: String, description: String, isPublic: Boolean) {
        val currentPlaylist = _uiState.value.playlist ?: return
        viewModelScope.launch {
            try {
                val updated = currentPlaylist.copy(
                    title = title,
                    description = description,
                    isPublic = isPublic
                )
                updatePlaylistUseCase(updated)
                loadPlaylist()
                hideEditDialog()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Update failed")
            }
        }
    }

    fun deletePlaylist() {
        if (!_uiState.value.isOwner) return
        viewModelScope.launch {
            try {
                deletePlaylistUseCase(playlistId)
                _uiState.value = _uiState.value.copy(isDeleted = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Delete failed")
            }
        }
    }

    fun uploadCover(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                uploadPlaylistCoverUseCase(playlistId, uri)
                loadPlaylist()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Upload failed")
            }
        }
    }

    fun addCollaborator(collaboratorUid: String) {
        if (!_uiState.value.isOwner) return
        val currentPlaylist = _uiState.value.playlist ?: return
        if (currentPlaylist.collaboratorIds.contains(collaboratorUid)) return

        viewModelScope.launch {
            try {
                val updated = currentPlaylist.copy(
                    collaboratorIds = currentPlaylist.collaboratorIds + collaboratorUid
                )
                updatePlaylistUseCase(updated)
                loadPlaylist()
                hideCollaboratorDialog()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to add collaborator")
            }
        }
    }

    // --- Dialog Management ---
    fun showAddSongsDialog() {
        _uiState.value = _uiState.value.copy(showAddSongsDialog = true, addedMessage = null)
    }
    fun hideAddSongsDialog() {
        _uiState.value = _uiState.value.copy(showAddSongsDialog = false)
        searchJob?.cancel()
    }
    fun showEditDialog() {
        _uiState.value = _uiState.value.copy(showEditDialog = true)
    }
    fun hideEditDialog() {
        _uiState.value = _uiState.value.copy(showEditDialog = false)
    }
    fun showCollaboratorDialog() {
        _uiState.value = _uiState.value.copy(showCollaboratorDialog = true)
    }
    fun hideCollaboratorDialog() {
        _uiState.value = _uiState.value.copy(showCollaboratorDialog = false)
    }

    // --- Search Logic ---
    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            _uiState.value = _uiState.value.copy(isSearching = true)
            searchSongsUseCase(query, currentUserId).collect { result ->
                if (result is Resource.Success) {
                    val existingIds = _uiState.value.tracks.map { it.songId }.toSet()
                    val filtered = (result.data ?: emptyList()).filter { it.id !in existingIds }
                    _uiState.value = _uiState.value.copy(searchResults = filtered, isSearching = false)
                } else if (result is Resource.Error) {
                    _uiState.value = _uiState.value.copy(isSearching = false)
                }
            }
        }
    }

    // --- Track Management ---
    fun addTrack(song: Song) {
        if (!_uiState.value.canEdit || _uiState.value.isAddingTrack) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAddingTrack = true, addedMessage = null)
            try {
                val track = PlaylistTrack(
                    songId = song.id,
                    order = _uiState.value.tracks.size,
                    addedBy = currentUserId
                )
                addTrackToPlaylistUseCase(playlistId, track)
                _uiState.value = _uiState.value.copy(
                    isAddingTrack = false,
                    addedMessage = "Added \"${song.title}\"",
                    searchResults = _uiState.value.searchResults.filter { it.id != song.id }
                )
                loadPlaylist()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAddingTrack = false,
                    addedMessage = "Failed: ${e.message}"
                )
            }
        }
    }

    fun removeTrack(songId: String) {
        if (!_uiState.value.canEdit) return
        viewModelScope.launch {
            try {
                removeTrackFromPlaylistUseCase(playlistId, songId)
                loadPlaylist()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to remove track")
            }
        }
    }

    fun clearAddedMessage() {
        _uiState.value = _uiState.value.copy(addedMessage = null)
    }

    // --- Playback ---
    fun playPlaylist(startIndex: Int = 0) {
        val songs = _uiState.value.songs
        if (songs.isNotEmpty()) {
            musicController.setPlaylist(songs, startIndex, playlistId, "playlist")
        }
    }

    fun shufflePlaylist() {
        val songs = _uiState.value.songs.shuffled()
        if (songs.isNotEmpty()) {
            musicController.setPlaylist(songs, 0, playlistId, "playlist")
        }
    }

    fun togglePlayPause() {
        val isCurrentPlaylistPlaying = _uiState.value.songs.any { it.id == _uiState.value.currentPlayingSongId }
        
        if (_uiState.value.isPlaying && isCurrentPlaylistPlaying) {
            musicController.pause()
        } else {
            // If playing something else or nothing, start this playlist
            if (isCurrentPlaylistPlaying && _uiState.value.currentPlayingSongId != null) {
                musicController.resume()
            } else {
                playPlaylist()
            }
        }
    }
}
