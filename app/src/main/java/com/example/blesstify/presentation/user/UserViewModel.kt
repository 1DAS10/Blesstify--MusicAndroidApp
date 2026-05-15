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
    val error: AppError? = null
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
    private val updateUserAvatarUseCase: UpdateUserAvatarUseCase
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
            
            // Parallel fetch
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
            
            userJob.join()
            subJob.join()
            _uiState.update { it.copy(isLoading = false) }
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
