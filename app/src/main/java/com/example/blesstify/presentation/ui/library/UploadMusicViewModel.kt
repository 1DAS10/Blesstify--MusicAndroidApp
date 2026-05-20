package com.example.blesstify.presentation.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.domain.model.Song
import com.example.blesstify.domain.repository.AlbumRepository
import com.example.blesstify.domain.repository.ArtistRepository
import com.example.blesstify.domain.usecase.UploadSongUseCase
import com.example.blesstify.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UploadUiState(
    val title: String = "",

    // Artist select/create
    val artist: String = "", // manual entry (new) or mirrors selected
    val artistId: String? = null,
    val artistOptions: List<com.example.blesstify.domain.model.Artist> = emptyList(),

    // Album select/create
    val album: String = "", // manual entry (new) or mirrors selected
    val albumId: String? = null,
    val albumOptions: List<com.example.blesstify.domain.model.Album> = emptyList(),

    val genres: List<String> = listOf("Other"),
    val audioUri: Uri? = null,
    val audioFileName: String? = null,
    val coverUri: Uri? = null,
    val coverFileName: String? = null,
    val isPublic: Boolean = false,
    val isLoading: Boolean = false,
    val uploadProgress: String? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

sealed interface UploadUiEvent {
    data class TitleChanged(val value: String) : UploadUiEvent

    data class ArtistChanged(val value: String) : UploadUiEvent
    data class ArtistSelected(val artistId: String?) : UploadUiEvent

    data class AlbumChanged(val value: String) : UploadUiEvent
    data class AlbumSelected(val albumId: String?) : UploadUiEvent

    data class GenreToggled(val value: String) : UploadUiEvent
    data class PublicChanged(val value: Boolean) : UploadUiEvent
    data class AudioSelected(val uri: Uri, val fileName: String) : UploadUiEvent
    data class CoverSelected(val uri: Uri, val fileName: String) : UploadUiEvent
    object RemoveCover : UploadUiEvent
    object RemoveAudio : UploadUiEvent
    object Submit : UploadUiEvent
    object ClearMessages : UploadUiEvent
}


@HiltViewModel
class UploadMusicViewModel @Inject constructor(
    private val uploadSongUseCase: UploadSongUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository
) : ViewModel() {

    private val allowedGenres = setOf(
        "Pop", "Rock", "Hip Hop", "Rap", "EDM", "Electronic", "Jazz", "Blues",
        "Classical", "R&B", "Country", "Folk", "Metal", "Reggae", "K-pop", "J-pop",
        "C-pop", "Latin", "Phonk", "Lo-fi", "Other"
    )

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                val artists = artistRepository.getAllArtists()
                val albums = albumRepository.getAllAlbums()
                _uiState.update { it.copy(artistOptions = artists, albumOptions = albums) }
            }.onFailure { e ->
                _uiState.update { it.copy(errorMessage = "Load albums/artists failed: ${e.message ?: "Unknown"}") }
            }
        }
    }


    fun onEvent(event: UploadUiEvent) {
        when (event) {
            is UploadUiEvent.TitleChanged -> _uiState.update { it.copy(title = event.value) }

            is UploadUiEvent.ArtistChanged -> _uiState.update { it.copy(artist = event.value, artistId = null) }
            is UploadUiEvent.ArtistSelected -> {
                val selected = event.artistId?.let { id -> _uiState.value.artistOptions.firstOrNull { it.id == id } }
                _uiState.update {
                    it.copy(
                        artistId = event.artistId,
                        artist = selected?.name ?: it.artist
                    )
                }
            }

            is UploadUiEvent.AlbumChanged -> _uiState.update { it.copy(album = event.value, albumId = null) }
            is UploadUiEvent.AlbumSelected -> {
                val selected = event.albumId?.let { id -> _uiState.value.albumOptions.firstOrNull { it.id == id } }
                _uiState.update {
                    it.copy(
                        albumId = event.albumId,
                        album = selected?.name ?: it.album
                    )
                }
            }

            is UploadUiEvent.GenreToggled -> _uiState.update { state ->
                val currentGenres = state.genres.toMutableList()
                if (currentGenres.contains(event.value)) {
                    currentGenres.remove(event.value)
                    if (currentGenres.isEmpty()) currentGenres.add("Other")
                } else {
                    currentGenres.add(event.value)
                }
                state.copy(genres = currentGenres.distinct())
            }

            is UploadUiEvent.PublicChanged -> _uiState.update { it.copy(isPublic = event.value) }
            is UploadUiEvent.AudioSelected -> _uiState.update {
                it.copy(audioUri = event.uri, audioFileName = event.fileName)
            }

            is UploadUiEvent.CoverSelected -> _uiState.update {
                it.copy(coverUri = event.uri, coverFileName = event.fileName)
            }

            UploadUiEvent.RemoveCover -> _uiState.update { it.copy(coverUri = null, coverFileName = null) }
            UploadUiEvent.RemoveAudio -> _uiState.update { it.copy(audioUri = null, audioFileName = null) }
            UploadUiEvent.Submit -> submitUpload()
            UploadUiEvent.ClearMessages -> _uiState.update { it.copy(successMessage = null, errorMessage = null) }
        }
    }

    private fun submitUpload() {
        val state = _uiState.value
        // Validation
        if (state.title.isBlank() || state.artist.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title and Artist are required") }
            return
        }
        if (state.audioUri == null || state.audioFileName == null) {
            _uiState.update { it.copy(errorMessage = "Please select an audio file") }
            return
        }

        val currentUser = getCurrentUserUseCase()
        if (currentUser == null) {
            _uiState.update { it.copy(errorMessage = "You must be logged in to upload") }
            return
        }

        val selectedGenres = state.genres.filter { it in allowedGenres }
        val finalGenres = if (selectedGenres.isEmpty()) listOf("Other") else selectedGenres

        val song = Song(
            title = state.title.trim(),
            artist = state.artist.trim(),
            artistId = state.artistId,
            album = state.album.trim().ifBlank { null },
            albumId = state.albumId,
            genre = finalGenres,
            ownerId = currentUser.id,
            isPublic = state.isPublic,
            status = "draft"
        )

        viewModelScope.launch {
            uploadSongUseCase(
                audioUri = state.audioUri,
                audioFileName = state.audioFileName,
                coverUri = state.coverUri,
                coverFileName = state.coverFileName,
                song = song
            ).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update {
                            it.copy(isLoading = true, errorMessage = null, uploadProgress = "Uploading...")
                        }
                    }
                    is Resource.Success -> {
                        _uiState.value = UploadUiState(
                            successMessage = "Song uploaded successfully!",
                            uploadProgress = null
                        )
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                uploadProgress = null,
                                errorMessage = "Upload failed: ${result.error?.toString() ?: "Unknown error"}"
                            )
                        }
                    }
                }
            }
        }
    }
}
