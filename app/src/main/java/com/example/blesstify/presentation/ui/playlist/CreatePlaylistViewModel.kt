package com.example.blesstify.presentation.ui.playlist

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.domain.model.Playlist
import com.example.blesstify.domain.usecase.CreatePlaylistUseCase
import com.example.blesstify.domain.usecase.GetCurrentUserUseCase
import com.example.blesstify.domain.usecase.UploadPlaylistCoverUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreatePlaylistUiState(
    val title: String = "",
    val description: String = "",
    val isPublic: Boolean = true,
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val createdPlaylistId: String? = null,
    val coverUri: Uri? = null
)

@HiltViewModel
class CreatePlaylistViewModel @Inject constructor(
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val uploadPlaylistCoverUseCase: UploadPlaylistCoverUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePlaylistUiState())
    val uiState: StateFlow<CreatePlaylistUiState> = _uiState.asStateFlow()

    fun onTitleChange(value: String) {
        _uiState.value = _uiState.value.copy(title = value)
    }

    fun onDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(description = value)
    }

    fun onPublicChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(isPublic = value)
    }

    fun onCoverSelected(uri: Uri) {
        _uiState.value = _uiState.value.copy(coverUri = uri)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }

    fun createPlaylist() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Playlist name is required")
            return
        }

        val currentUser = getCurrentUserUseCase()
        if (currentUser == null) {
            _uiState.value = state.copy(errorMessage = "You must be logged in")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val playlist = Playlist(
                    ownerId = currentUser.id,
                    title = state.title.trim(),
                    description = state.description.trim().ifBlank { null },
                    isPublic = state.isPublic
                )
                val playlistId = createPlaylistUseCase(playlist)
                
                // Upload cover if exists
                state.coverUri?.let { uri ->
                    uploadPlaylistCoverUseCase(playlistId, uri)
                }

                _uiState.value = CreatePlaylistUiState(
                    successMessage = "Playlist created!",
                    createdPlaylistId = playlistId
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to create: ${e.message}"
                )
            }
        }
    }
}
