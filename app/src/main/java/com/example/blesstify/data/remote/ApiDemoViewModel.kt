package com.example.blesstify.data.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.data.remote.dto.ApiSongDto
import com.example.blesstify.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApiDemoViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val songs: List<ApiSongDto> = emptyList()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun loadTrending() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val res = songRepository.getTrendingSongs()) {
                is NetworkResult.Success -> {
                    _uiState.value = UiState(isLoading = false, songs = res.data.data)
                }
                is NetworkResult.HttpError -> {
                    _uiState.value = UiState(
                        isLoading = false,
                        error = "HTTP ${res.code}: ${res.message}"
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = UiState(isLoading = false, error = res.message)
                }
                NetworkResult.Loading -> Unit
            }
        }
    }
}
