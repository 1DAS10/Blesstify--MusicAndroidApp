package com.example.blesstify.presentation.ui.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.data.repository.FirestoreKeys
import com.example.blesstify.domain.model.Playlist
import com.example.blesstify.domain.model.Song
import com.example.blesstify.domain.repository.AlbumRepository
import com.example.blesstify.domain.repository.ArtistRepository
import com.example.blesstify.domain.usecase.GetCurrentUserUseCase
import com.example.blesstify.domain.usecase.GetLikedSongIdsUseCase
import com.example.blesstify.domain.usecase.GetSongsByIdsUseCase
import com.example.blesstify.domain.usecase.GetUserPlaylistsUseCase
import com.example.blesstify.domain.usecase.GetUserSongsUseCase
import com.example.blesstify.presentation.player.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryAlbum(
    val id: String,
    val name: String,
    val artist: String,
    val coverUrl: String?,
    val songs: List<Song>
)

data class LibraryArtist(
    val id: String,
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
    private val getUserSongsUseCase: GetUserSongsUseCase,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val musicController: MusicController
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
                Log.d(FirestoreKeys.LIBRARY_TAG, "[library] load start userId=${currentUser.id}")

                val playlistsDeferred = async { getUserPlaylistsUseCase(currentUser.id) }
                val firestoreAlbumsDeferred = async { albumRepository.getAllAlbums() }
                val firestoreArtistsDeferred = async { artistRepository.getAllArtists() }

                val ownSongsDeferred = async { collectSongsFromFlow(getUserSongsUseCase(currentUser.id)) }
                val likedSongsDeferred = async {
                    val likedIds = getLikedSongIdsUseCase(currentUser.id)
                    if (likedIds.isNotEmpty()) collectSongsFromFlow(getSongsByIdsUseCase(likedIds))
                    else emptyList()
                }

                val playlists = playlistsDeferred.await()
                val firestoreAlbums = firestoreAlbumsDeferred.await()
                val firestoreArtists = firestoreArtistsDeferred.await()
                val ownSongs = ownSongsDeferred.await()
                val likedSongs = likedSongsDeferred.await()

                val allSongs = (ownSongs + likedSongs).distinctBy { it.id }

                val songDerivedAlbums = allSongs.groupBy { it.album }
                    .filterKeys { !it.isNullOrBlank() }
                    .map { (albumName, albumSongs) ->
                        LibraryAlbum(
                            id = albumName.orEmpty(),
                            name = albumName ?: "Unknown",
                            artist = albumSongs.first().artist,
                            coverUrl = albumSongs.firstOrNull { it.coverUrl != null }?.coverUrl,
                            songs = albumSongs
                        )
                    }

                val firestoreLibraryAlbums = firestoreAlbums.map { album ->
                    val matchedSongs = allSongs.filter { song ->
                        song.id in album.songIds || song.album.equals(album.name, ignoreCase = true)
                    }
                    LibraryAlbum(
                        id = album.id,
                        name = album.name,
                        artist = album.artistNames.joinToString(", ").ifBlank {
                            matchedSongs.firstOrNull()?.artist ?: "Unknown Artist"
                        },
                        coverUrl = album.coverUrl ?: album.thumbnailUrl ?: matchedSongs.firstOrNull { it.coverUrl != null }?.coverUrl,
                        songs = matchedSongs
                    )
                }

                val albums = (firestoreLibraryAlbums + songDerivedAlbums)
                    .distinctBy { it.id.ifBlank { it.name.lowercase() } }

                val songDerivedArtists = allSongs.groupBy { it.artist }
                    .filterKeys { it.isNotBlank() }
                    .map { (artistName, artistSongs) ->
                        LibraryArtist(
                            id = artistName,
                            name = artistName,
                            coverUrl = artistSongs.firstOrNull { it.coverUrl != null }?.coverUrl,
                            songs = artistSongs
                        )
                    }

                val firestoreLibraryArtists = firestoreArtists.map { artist ->
                    val matchedSongs = allSongs.filter { song ->
                        song.id in artist.songIds || song.artist.equals(artist.name, ignoreCase = true)
                    }
                    LibraryArtist(
                        id = artist.id,
                        name = artist.name,
                        coverUrl = artist.coverUrl ?: artist.thumbnailUrl ?: matchedSongs.firstOrNull { it.coverUrl != null }?.coverUrl,
                        songs = matchedSongs
                    )
                }

                val artists = (firestoreLibraryArtists + songDerivedArtists)
                    .distinctBy { it.id.ifBlank { it.name.lowercase() } }

                Log.d(
                    FirestoreKeys.LIBRARY_TAG,
                    "[library] load success playlists=${playlists.size} albums=${albums.size} artists=${artists.size} firestoreAlbums=${firestoreAlbums.size} firestoreArtists=${firestoreArtists.size} songs=${allSongs.size}"
                )

                _uiState.value = _uiState.value.copy(
                    playlists = playlists,
                    albums = albums,
                    artists = artists,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e(FirestoreKeys.LIBRARY_TAG, "[library] load failed: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load library: ${e.message}"
                )
            }
        }
    }

    fun addSongToQueue(song: Song) {
        musicController.addToQueue(song)
    }

    fun playSongNext(song: Song) {
        musicController.addToQueueNext(song)
    }

    fun deleteAlbumById(albumId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                albumRepository.deleteAlbum(albumId)
                loadPlaylists()
                onComplete(true)
            } catch (e: Exception) {
                Log.e(FirestoreKeys.LIBRARY_TAG, "[library] delete album failed: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to delete album: ${e.message}"
                )
                onComplete(false)
            }
        }
    }

    fun deleteArtistById(artistId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                artistRepository.deleteArtist(artistId)
                loadPlaylists()
                onComplete(true)
            } catch (e: Exception) {
                Log.e(FirestoreKeys.LIBRARY_TAG, "[library] delete artist failed: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to delete artist: ${e.message}"
                )
                onComplete(false)
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
