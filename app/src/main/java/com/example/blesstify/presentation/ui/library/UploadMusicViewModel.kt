package com.example.blesstify.presentation.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.domain.model.Song
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
    val artist: String = "",
    val album: String = "",
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
    data class AlbumChanged(val value: String) : UploadUiEvent
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
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val allowedGenres = setOf(
        "Pop", "Rock", "Hip Hop", "Rap", "EDM", "Electronic", "Jazz", "Blues",
        "Classical", "R&B", "Country", "Folk", "Metal", "Reggae", "K-pop", "J-pop",
        "C-pop", "Latin", "Phonk", "Lo-fi", "Other"
    )

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    fun onEvent(event: UploadUiEvent) {
        when (event) {
            is UploadUiEvent.TitleChanged -> _uiState.update { it.copy(title = event.value) }
            is UploadUiEvent.ArtistChanged -> _uiState.update { it.copy(artist = event.value) }
            is UploadUiEvent.AlbumChanged -> _uiState.update { it.copy(album = event.value) }
            is UploadUiEvent.GenreToggled -> _uiState.update { state ->
                val currentGenres = state.genres.toMutableList()
                if (currentGenres.contains(event.value)) {
                    // Don't remove if it's the last one? Or just remove.
                    // If removing the last one, maybe default to "Other"?
                    currentGenres.remove(event.value)
                    if (currentGenres.isEmpty()) currentGenres.add("Other")
                } else {
                    currentGenres.add(event.value)
                    // If adding something else and "Other" was the only one, maybe remove "Other"?
                    if (event.value != "Other" && currentGenres.size > 1 && currentGenres.contains("Other")) {
                        // Optional: remove "Other" when a specific genre is selected?
                        // Let's keep it simple for now.
                    }
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
            album = state.album.trim().ifBlank { null },
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
