package com.example.blesstify.presentation.user

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.domain.model.User
import com.example.blesstify.domain.auth.AuthState
import com.example.blesstify.domain.usecase.GetCurrentUserUseCase
import com.example.blesstify.domain.usecase.GetUserUseCase
import com.example.blesstify.domain.usecase.ObserveAuthStateUseCase
import com.example.blesstify.domain.usecase.UpdateUserAvatarUseCase
import com.example.blesstify.domain.usecase.UpdateUserUseCase
import com.example.blesstify.domain.user.UserResult
import com.example.blesstify.core.error.AppError
import com.example.blesstify.core.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UserUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val displayNameInput: String = "",
    val emailInput: String = "",
    val localeInput: String = "",
    val avatarLocalUri: Uri? = null,
    val subscription: com.example.blesstify.domain.model.Subscription? = null,
    val error: AppError? = null,
    val likedSongsCount: Int = 0,
    val playlistsCount: Int = 0,
    val totalHoursListened: Int = 0,
    val weeklyFocusData: List<Float> = emptyList(),
    val weeklyFocusTotalHours: String = "0h 0m"
)

sealed interface UserUiEvent {
    object StartEdit : UserUiEvent
    object DismissEdit : UserUiEvent
    data class DisplayNameChanged(val value: String) : UserUiEvent
    data class EmailChanged(val value: String) : UserUiEvent
    data class LocaleChanged(val value: String) : UserUiEvent
    data class AvatarSelected(val uri: Uri) : UserUiEvent
    object SaveChanges : UserUiEvent
    object ClearError : UserUiEvent
}

@HiltViewModel
class UserViewModel @Inject constructor(
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val getSubscriptionUseCase: com.example.blesstify.domain.usecase.GetSubscriptionUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val updateUserAvatarUseCase: UpdateUserAvatarUseCase,
    private val getLikedSongIdsUseCase: com.example.blesstify.domain.usecase.GetLikedSongIdsUseCase,
    private val getUserPlaylistsUseCase: com.example.blesstify.domain.usecase.GetUserPlaylistsUseCase,
    private val observeListeningHistoryUseCase: com.example.blesstify.domain.usecase.ObserveListeningHistoryUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().collectLatest { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        currentUserId = state.userId
                        loadUser(state.userId)
                    }
                    else -> {
                        currentUserId = null
                        _uiState.update { it.copy(user = null) }
                    }
                }
            }
        }
    }

    fun onEvent(event: UserUiEvent) {
        when (event) {
            UserUiEvent.StartEdit -> startEdit()
            UserUiEvent.DismissEdit -> _uiState.update { it.copy(isEditing = false, error = null) }
            is UserUiEvent.DisplayNameChanged -> _uiState.update { it.copy(displayNameInput = event.value) }
            is UserUiEvent.EmailChanged -> _uiState.update { it.copy(emailInput = event.value) }
            is UserUiEvent.LocaleChanged -> _uiState.update { it.copy(localeInput = event.value) }
            is UserUiEvent.AvatarSelected -> _uiState.update { it.copy(avatarLocalUri = event.uri) }
            UserUiEvent.SaveChanges -> saveChanges()
            UserUiEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun startEdit() {
        val user = _uiState.value.user
        _uiState.update {
            it.copy(
                isEditing = true,
                displayNameInput = user?.displayName.orEmpty(),
                emailInput = user?.email.orEmpty(),
                localeInput = user?.locale.orEmpty(),
                avatarLocalUri = null,
                error = null
            )
        }
    }

    private fun loadUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Parallel fetch for basic user info
            val userJob = viewModelScope.launch {
                when (val result = getUserUseCase(userId)) {
                    is UserResult.Success -> _uiState.update { it.copy(user = result.data) }
                    is UserResult.Error -> _uiState.update { it.copy(error = result.error) }
                }
            }
            
            val subJob = viewModelScope.launch {
                val result = getSubscriptionUseCase(userId)
                when (result) {
                    is Resource.Success -> _uiState.update { it.copy(subscription = result.data) }
                    is Resource.Error -> Unit // Silent error for subscription
                    is Resource.Loading -> Unit
                }
            }

            // Stats fetching
            val statsJob = viewModelScope.launch {
                try {
                    val likedSongs = getLikedSongIdsUseCase(userId)
                    val playlists = getUserPlaylistsUseCase(userId)
                    _uiState.update {
                        it.copy(
                            likedSongsCount = likedSongs.size,
                            playlistsCount = playlists.size
                        )
                    }
                } catch (e: Exception) {
                    // Ignore stats error
                }
            }

            // History Observer for Weekly Focus
            viewModelScope.launch {
                observeListeningHistoryUseCase(userId, limit = 1000).collectLatest { historyList ->
                    calculateListeningStats(historyList)
                }
            }
            
            userJob.join()
            subJob.join()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun calculateListeningStats(historyList: List<com.example.blesstify.domain.model.ListeningHistory>) {
        if (historyList.isEmpty()) {
            _uiState.update { it.copy(totalHoursListened = 0, weeklyFocusData = emptyList(), weeklyFocusTotalHours = "0h 0m") }
            return
        }

        val totalSeconds = historyList.sumOf { it.durationSec.toLong() }
        val totalHours = (totalSeconds / 3600).toInt()

        // Calculate last 7 days distribution
        val now = System.currentTimeMillis()
        val millisInDay = 86400000L
        val weekData = FloatArray(7) { 0f }
        var weeklyTotalSeconds = 0L

        historyList.forEach { item ->
            val itemTime = item.playedAt
            val diffMillis = now - itemTime
            if (diffMillis >= 0) {
                val daysAgo = (diffMillis / millisInDay).toInt()
                if (daysAgo in 0..6) {
                    // Index 6 is today, 0 is 6 days ago (Monday if today is Sunday)
                    // We'll map it backwards: 6 - daysAgo is the chronological index
                    val index = 6 - daysAgo
                    weekData[index] += item.durationSec.toFloat() / 3600f // Convert to hours
                    weeklyTotalSeconds += item.durationSec
                }
            }
        }

        val weeklyHours = weeklyTotalSeconds / 3600
        val weeklyMinutes = (weeklyTotalSeconds % 3600) / 60
        val weeklyFocusTotal = "${weeklyHours}h ${weeklyMinutes}m"

        _uiState.update { 
            it.copy(
                totalHoursListened = totalHours,
                weeklyFocusData = weekData.toList(),
                weeklyFocusTotalHours = weeklyFocusTotal
            ) 
        }
    }

    private fun saveChanges() {
        val userId = currentUserId ?: getCurrentUserUseCase()?.id
        if (userId.isNullOrBlank()) {
            _uiState.update { it.copy(error = AppError.NotFound) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val avatarUrl = when (val avatarUri = _uiState.value.avatarLocalUri) {
                null -> _uiState.value.user?.photoUrl
                else -> {
                    when (val avatarResult = updateUserAvatarUseCase(userId, avatarUri)) {
                        is UserResult.Success -> avatarResult.data
                        is UserResult.Error -> {
                            _uiState.update { it.copy(isLoading = false, error = avatarResult.error) }
                            return@launch
                        }
                    }
                }
            }

            val displayName = _uiState.value.displayNameInput.trim().ifBlank { null }
            val email = _uiState.value.emailInput.trim().ifBlank { null }
            val locale = _uiState.value.localeInput.trim().ifBlank { null }

            when (val result = updateUserUseCase(userId, displayName, avatarUrl, email, locale)) {
                is UserResult.Success -> {
                    loadUser(userId)
                    _uiState.update { it.copy(isEditing = false, avatarLocalUri = null) }
                }
                is UserResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }
}
