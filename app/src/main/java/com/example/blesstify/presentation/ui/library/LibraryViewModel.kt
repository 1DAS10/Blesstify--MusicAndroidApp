package com.example.blesstify.presentation.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.domain.model.Playlist
import com.example.blesstify.domain.usecase.GetCurrentUserUseCase
import com.example.blesstify.domain.usecase.GetUserPlaylistsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.blesstify.domain.model.Song
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.domain.usecase.GetLikedSongIdsUseCase
import com.example.blesstify.domain.usecase.GetSongsByIdsUseCase
import com.example.blesstify.domain.usecase.GetUserSongsUseCase
import kotlinx.coroutines.async

data class LibraryAlbum(
    val name: String,
    val artist: String,
    val coverUrl: String?,
    val songs: List<Song>
)

data class LibraryArtist(
    val name: String,
    val coverUrl: String?,
    val songs: List<Song>
)

data class LibraryUiState(
    val playlists: List<Playlist> = emptyList(),
    val albums: List<LibraryAlbum> = emptyList(),
    val artists: List<LibraryArtist> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getUserPlaylistsUseCase: GetUserPlaylistsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getLikedSongIdsUseCase: GetLikedSongIdsUseCase,
    private val getSongsByIdsUseCase: GetSongsByIdsUseCase,
    private val getUserSongsUseCase: GetUserSongsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadPlaylists()
    }

    fun loadPlaylists() {
        val currentUser = getCurrentUserUseCase() ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Fetch playlists
                val playlists = getUserPlaylistsUseCase(currentUser.id)

                // Fetch user's own songs and liked songs in parallel
                val ownSongsDeferred = async { collectSongsFromFlow(getUserSongsUseCase(currentUser.id)) }
                val likedSongsDeferred = async {
                    val likedIds = getLikedSongIdsUseCase(currentUser.id)
                    if (likedIds.isNotEmpty()) collectSongsFromFlow(getSongsByIdsUseCase(likedIds))
                    else emptyList()
                }

                val ownSongs = ownSongsDeferred.await()
                val likedSongs = likedSongsDeferred.await()

                // Merge and deduplicate
                val allSongs = (ownSongs + likedSongs).distinctBy { it.id }

                val albums = allSongs.groupBy { it.album }
                    .filterKeys { !it.isNullOrBlank() }
                    .map { (albumName, albumSongs) ->
                        LibraryAlbum(
                            name = albumName ?: "Unknown",
                            artist = albumSongs.first().artist,
                            coverUrl = albumSongs.firstOrNull { it.coverUrl != null }?.coverUrl,
                            songs = albumSongs
                        )
                    }

                val artists = allSongs.groupBy { it.artist }
                    .filterKeys { it.isNotBlank() }
                    .map { (artistName, artistSongs) ->
                        LibraryArtist(
                            name = artistName,
                            coverUrl = artistSongs.firstOrNull { it.coverUrl != null }?.coverUrl,
                            songs = artistSongs
                        )
                    }

                _uiState.value = _uiState.value.copy(
                    playlists = playlists,
                    albums = albums,
                    artists = artists,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load library: ${e.message}"
                )
            }
        }
    }

    private suspend fun collectSongsFromFlow(flow: kotlinx.coroutines.flow.Flow<Resource<List<Song>>>): List<Song> {
        var result: List<Song> = emptyList()
        flow.collect { resource ->
            if (resource is Resource.Success) {
                result = resource.data ?: emptyList()
            }
        }
        return result
    }
}
