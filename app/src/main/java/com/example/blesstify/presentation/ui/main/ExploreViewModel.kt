package com.example.blesstify.presentation.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.data.remote.BlesstifyRemoteDataSource
import com.example.blesstify.data.remote.NetworkResult
import com.example.blesstify.domain.model.Song
import com.example.blesstify.domain.usecase.*
import com.google.firebase.Timestamp
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
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val remoteDataSource: BlesstifyRemoteDataSource
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

                // Load Trending Songs (Retrofit first, Firebase fallback)
                launch {
                    val TAG = "RecommendForToday"
                    try {
                        when (val apiResult = remoteDataSource.getTrendingSongs(limit = 10)) {
                            is NetworkResult.Success -> {
                                val trendingSongs = apiResult.data.data.map { dto ->
                                    Song(
                                        id = dto.id,
                                        title = dto.title,
                                        artist = dto.artist,
                                        coverUrl = dto.coverUrl,
                                        thumbnailUrl = dto.thumbnailUrl,
                                        createdAt = Timestamp.now(),
                                        updatedAt = Timestamp.now(),
                                        isPublic = true,
                                        status = "published"
                                    )
                                }
                                Log.d(TAG, "ViewModel: Retrofit success with ${trendingSongs.size} songs.")
                                _uiState.update {
                                    it.copy(
                                        recommendedSongs = trendingSongs,
                                        featuredSong = trendingSongs.firstOrNull() ?: it.featuredSong
                                    )
                                }
                            }

                            is NetworkResult.HttpError -> {
                                Log.e(TAG, "ViewModel: Retrofit HTTP ${apiResult.code} (${apiResult.message}), fallback Firebase...")
                                getTrendingSongsUseCase(limit = 10, monthsBack = 2).collect { resource ->
                                    when (resource) {
                                        is Resource.Success -> {
                                            val trendingSongs = resource.data ?: emptyList()
                                            _uiState.update {
                                                it.copy(
                                                    recommendedSongs = trendingSongs,
                                                    featuredSong = trendingSongs.firstOrNull() ?: it.featuredSong
                                                )
                                            }
                                        }
                                        is Resource.Error -> Log.e(TAG, "ViewModel: Firebase fallback error: ${resource.error}")
                                        is Resource.Loading -> Unit
                                    }
                                }
                            }

                            is NetworkResult.Error -> {
                                Log.e(TAG, "ViewModel: Retrofit failed (${apiResult.message}), fallback Firebase...")
                                getTrendingSongsUseCase(limit = 10, monthsBack = 2).collect { resource ->
                                    when (resource) {
                                        is Resource.Success -> {
                                            val trendingSongs = resource.data ?: emptyList()
                                            _uiState.update {
                                                it.copy(
                                                    recommendedSongs = trendingSongs,
                                                    featuredSong = trendingSongs.firstOrNull() ?: it.featuredSong
                                                )
                                            }
                                        }
                                        is Resource.Error -> Log.e(TAG, "ViewModel: Firebase fallback error: ${resource.error}")
                                        is Resource.Loading -> Unit
                                    }
                                }
                            }

                            NetworkResult.Loading -> Unit
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "ViewModel: Trending launch exception: ${e.message}", e)
                    }
                }
            }
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
