package com.example.blesstify.presentation.ui.library.edit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.domain.model.Album
import com.example.blesstify.domain.model.Artist
import com.example.blesstify.domain.repository.AlbumRepository
import com.example.blesstify.domain.repository.ArtistRepository
import com.example.blesstify.util.SlugUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumEditorViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,
        val error: String? = null,
        val albumId: String? = null,
        val name: String = "",
        val coverUrl: String = "",
        val localCoverUri: Uri? = null,
        val allArtists: List<Artist> = emptyList(),
        val selectedArtistIds: Set<String> = emptySet()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load(albumId: String?) {
        val id = albumId?.trim().orEmpty()
        if (id.isEmpty()) {
            // Create mode: still preload artists
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                try {
                    val artists = artistRepository.getAllArtists()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            albumId = null,
                            name = "",
                            coverUrl = "",
                            localCoverUri = null,
                            allArtists = artists,
                            selectedArtistIds = emptySet(),
                            error = null
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Load failed") }
                }
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val album = albumRepository.getAlbumById(id)
                val artists = artistRepository.getAllArtists()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        albumId = id,
                        name = album?.name.orEmpty(),
                        coverUrl = album?.coverUrl.orEmpty(),
                        localCoverUri = null,
                        allArtists = artists,
                        selectedArtistIds = album?.artistIds?.toSet().orEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Load failed") }
            }
        }
    }

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v) }
    fun onCoverUrlChange(v: String) = _uiState.update { it.copy(coverUrl = v, localCoverUri = null) }

    fun onLocalCoverSelected(uri: Uri?) {
        _uiState.update { it.copy(localCoverUri = uri) }
    }

    fun removeLocalCover() {
        _uiState.update { it.copy(localCoverUri = null) }
    }

    fun toggleArtist(artistId: String) {
        val id = artistId.trim()
        if (id.isEmpty()) return
        _uiState.update { s ->
            val next = s.selectedArtistIds.toMutableSet()
            if (next.contains(id)) next.remove(id) else next.add(id)
            s.copy(selectedArtistIds = next)
        }
    }

    fun save(onSuccess: () -> Unit) {
        val name = _uiState.value.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(error = "Name empty") }
            return
        }

        val selectedIds = _uiState.value.selectedArtistIds
        val selectedNames = _uiState.value.allArtists
            .filter { selectedIds.contains(it.id) }
            .map { it.name }

        val existingId = _uiState.value.albumId?.trim().orEmpty()

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                // 1) Create new doc with auto-id if needed
                val id = if (existingId.isNotEmpty()) {
                    existingId
                } else {
                    val newId = albumRepository.createAlbum(
                        Album(
                            id = "",
                            name = name,
                            coverUrl = _uiState.value.coverUrl.trim().takeIf { it.isNotBlank() },
                            artistIds = selectedIds.toList(),
                            artistNames = selectedNames
                        )
                    )
                    _uiState.update { it.copy(albumId = newId) }
                    newId
                }

                // 2) Upload cover if picked locally (needs id)
                val uploadedCoverUrl = _uiState.value.localCoverUri?.let { uri ->
                    albumRepository.uploadAlbumCover(albumId = id, imageUri = uri)
                }

                val cover = uploadedCoverUrl
                    ?: _uiState.value.coverUrl.trim().takeIf { it.isNotBlank() }

                // 3) Update doc with latest fields (merge)
                albumRepository.updateAlbum(
                    Album(
                        id = id,
                        name = name,
                        coverUrl = cover,
                        artistIds = selectedIds.toList(),
                        artistNames = selectedNames
                    )
                )

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        albumId = id,
                        coverUrl = cover.orEmpty(),
                        localCoverUri = null
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "Save failed") }
            }
        }
    }
}

@HiltViewModel
class ArtistEditorViewModel @Inject constructor(
    private val artistRepository: ArtistRepository
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,
        val error: String? = null,
        val artistId: String? = null,
        val name: String = "",
        val coverUrl: String = "",
        val localCoverUri: Uri? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun load(artistId: String?) {
        val id = artistId?.trim().orEmpty()
        if (id.isEmpty()) {
            _uiState.update {
                it.copy(artistId = null, name = "", coverUrl = "", localCoverUri = null, error = null)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val artist = artistRepository.getArtistById(id)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        artistId = id,
                        name = artist?.name.orEmpty(),
                        coverUrl = artist?.coverUrl.orEmpty(),
                        localCoverUri = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Load failed") }
            }
        }
    }

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v) }
    fun onCoverUrlChange(v: String) = _uiState.update { it.copy(coverUrl = v, localCoverUri = null) }

    fun onLocalCoverSelected(uri: Uri?) {
        _uiState.update { it.copy(localCoverUri = uri) }
    }

    fun removeLocalCover() {
        _uiState.update { it.copy(localCoverUri = null) }
    }

    fun save(onSuccess: () -> Unit) {
        val name = _uiState.value.name.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(error = "Name empty") }
            return
        }

        val existingId = _uiState.value.artistId?.trim().orEmpty()

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                // 1) Create new doc with auto-id if needed
                val id = if (existingId.isNotEmpty()) {
                    existingId
                } else {
                    val newId = artistRepository.createArtist(
                        Artist(
                            id = "",
                            name = name,
                            coverUrl = _uiState.value.coverUrl.trim().takeIf { it.isNotBlank() }
                        )
                    )
                    _uiState.update { it.copy(artistId = newId) }
                    newId
                }

                // 2) Upload cover if picked locally (needs id)
                val uploadedCoverUrl = _uiState.value.localCoverUri?.let { uri ->
                    artistRepository.uploadArtistCover(artistId = id, imageUri = uri)
                }

                val cover = uploadedCoverUrl
                    ?: _uiState.value.coverUrl.trim().takeIf { it.isNotBlank() }

                // 3) Update doc with latest fields (merge)
                artistRepository.updateArtist(
                    Artist(
                        id = id,
                        name = name,
                        coverUrl = cover
                    )
                )

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        artistId = id,
                        coverUrl = cover.orEmpty(),
                        localCoverUri = null
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "Save failed") }
            }
        }
    }
}
