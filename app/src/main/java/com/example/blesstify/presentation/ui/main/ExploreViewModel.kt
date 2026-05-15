package com.example.blesstify.presentation.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.domain.model.Song
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
    private val getUserPlaylistsUseCase: GetUserPlaylistsUseCase,
    private val getLikedPlaylistsUseCase: GetLikedPlaylistsUseCase,
    private val getTrendingSongsUseCase: GetTrendingSongsUseCase,
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

            val uid = user?.id ?: ""
            if (uid.isNotBlank()) {
                // Load Playlists (Liked + Own)
                launch {
                    try {
                        val likedPlaylists = getLikedPlaylistsUseCase(uid)
                        val ownPlaylists = getUserPlaylistsUseCase(uid, 50)
                        
                        val mergedPlaylists = (likedPlaylists + ownPlaylists)
                            .distinctBy { it.id }
                            .take(10)
                        
                        _uiState.update { it.copy(continueListeningPlaylists = mergedPlaylists) }
                    } catch (e: Exception) {
                        android.util.Log.e("ExploreVM", "Playlists load failed", e)
                    }
                }

                // Load Trending Songs
                launch {
                    try {
                        getTrendingSongsUseCase(limit = 10, monthsBack = 2).collect { resource ->
                            if (resource is Resource.Success) {
                                val trendingSongs = resource.data ?: emptyList()
                                _uiState.update { 
                                    it.copy(
                                        recommendedSongs = trendingSongs,
                                        // Set the most popular song as featured if available
                                        featuredSong = trendingSongs.firstOrNull() ?: it.featuredSong
                                    ) 
                                }
                            } else if (resource is Resource.Error) {
                                android.util.Log.e("ExploreVM", "Trending songs failed: ${resource.error}")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ExploreVM", "Trending load failed", e)
                    }
                }
            }
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
