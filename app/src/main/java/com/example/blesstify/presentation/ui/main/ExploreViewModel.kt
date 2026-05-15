package com.example.blesstify.presentation.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.domain.model.Song
import com.example.blesstify.domain.auth.AuthUser
import com.example.blesstify.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class ExploreUiState(
    val displayName: String = "",
    val greeting: String = "",
    val featuredSong: Song? = null,
    val continueListeningPlaylists: List<com.example.blesstify.domain.model.Playlist> = emptyList(),
    val recommendedSongs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val getPublicSongsUseCase: GetPublicSongsUseCase,
    private val getListeningHistoryUseCase: GetListeningHistoryUseCase,
    private val getRecentPlaylistsUseCase: GetRecentPlaylistsUseCase,
    private val getPublicPlaylistsUseCase: GetPublicPlaylistsUseCase,
    private val getLatestRecommendationUseCase: GetLatestRecommendationUseCase,
    private val getSongsByIdsUseCase: GetSongsByIdsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val user = getCurrentUserUseCase()
            val name = user?.displayName ?: "User"
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greeting = when {
                hour < 12 -> "Good morning"
                hour < 18 -> "Good afternoon"
                else -> "Good evening"
            }
            _uiState.update { it.copy(displayName = name, greeting = greeting) }

            // Load Featured in parallel
            launch {
                getPublicSongsUseCase(5).collect { resource ->
                    if (resource is Resource.Success) {
                        _uiState.update { it.copy(featuredSong = resource.data?.firstOrNull()) }
                    }
                }
            }

            val uid = user?.id ?: ""
            if (uid.isNotBlank()) {
                // Load History Playlists in parallel
                launch {
                    try {
                        val playlists = getRecentPlaylistsUseCase(uid, 10)
                        if (playlists.isNotEmpty()) {
                            _uiState.update { it.copy(continueListeningPlaylists = playlists) }
                        } else {
                            val publicPlaylists = getPublicPlaylistsUseCase()
                            _uiState.update { it.copy(continueListeningPlaylists = publicPlaylists.take(10)) }
                        }
                    } catch (e: Exception) {
                        // Silent fail
                    }
                }

                // Load Recommendations in parallel
                launch {
                    try {
                        val recommendation = getLatestRecommendationUseCase(uid)
                        if (recommendation != null && recommendation.songIds.isNotEmpty()) {
                            getSongsByIdsUseCase(recommendation.songIds).collect { resource ->
                                if (resource is Resource.Success) {
                                    _uiState.update { it.copy(recommendedSongs = resource.data ?: emptyList()) }
                                }
                            }
                        } else {
                            // Fallback
                            getPublicSongsUseCase(10).collect { resource ->
                                if (resource is Resource.Success) {
                                    _uiState.update { it.copy(recommendedSongs = resource.data ?: emptyList()) }
                                } else if (resource is Resource.Error) {
                                    android.util.Log.e("ExploreVM", "Fallback songs failed: ${resource.error}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ExploreVM", "Recommendation load failed", e)
                    }
                }
            }
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
