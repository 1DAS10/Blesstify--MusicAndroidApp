package com.example.blesstify.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.domain.model.User
import com.example.blesstify.domain.auth.AuthResult
import com.example.blesstify.domain.auth.AuthState
import com.example.blesstify.domain.usecase.*
import com.example.blesstify.core.error.AppError
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val authState: AuthState = AuthState.Unknown,
    val isLoading: Boolean = false,
    val error: AppError? = null
)

sealed interface AuthUiEvent {
    data class EmailChanged(val value: String) : AuthUiEvent
    data class PasswordChanged(val value: String) : AuthUiEvent
    data class DisplayNameChanged(val value: String) : AuthUiEvent
    object SubmitLogin : AuthUiEvent
    object SubmitRegister : AuthUiEvent
    object SignOut : AuthUiEvent
    object ClearError : AuthUiEvent
    data class SubmitGoogle(val idToken: String) : AuthUiEvent
    data class GoogleSignInFailed(val message: String?) : AuthUiEvent
    data class SubmitFacebook(val accessToken: String) : AuthUiEvent
    data class FacebookSignInFailed(val message: String?) : AuthUiEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signInWithFacebookUseCase: SignInWithFacebookUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val createUserUseCase: CreateUserUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().collectLatest { state ->
                _uiState.update { it.copy(authState = state) }
                if (state is AuthState.Authenticated) {
                    // Ensure user doc exists even on app restart or fresh install.
                    ensureUserDocument(state.userId)
                }
            }
        }
    }

    fun onEvent(event: AuthUiEvent) {
        when (event) {
            is AuthUiEvent.EmailChanged -> _uiState.update { it.copy(email = event.value) }
            is AuthUiEvent.PasswordChanged -> _uiState.update { it.copy(password = event.value) }
            is AuthUiEvent.DisplayNameChanged -> _uiState.update { it.copy(displayName = event.value) }
            AuthUiEvent.ClearError -> _uiState.update { it.copy(error = null) }
            AuthUiEvent.SubmitLogin -> submitLogin()
            AuthUiEvent.SubmitRegister -> submitRegister()
            AuthUiEvent.SignOut -> submitSignOut()
            is AuthUiEvent.SubmitGoogle -> submitGoogle(event.idToken)
            is AuthUiEvent.GoogleSignInFailed -> _uiState.update {
                it.copy(error = AppError.Unknown(event.message))
            }
            is AuthUiEvent.SubmitFacebook -> submitFacebook(event.accessToken)
            is AuthUiEvent.FacebookSignInFailed -> _uiState.update {
                it.copy(error = AppError.Unknown(event.message))
            }
        }
    }

    private fun submitLogin() {
        val current = _uiState.value
        if (current.email.isBlank() || current.password.isBlank()) {
            _uiState.update { it.copy(error = AppError.Unknown("Email and password are required")) }
            return
        }
        viewModelScope.launch {
            signInUseCase(current.email.trim(), current.password)
                .collectLatest { result -> handleAuthResult(result) }
        }
    }

    private fun submitRegister() {
        val current = _uiState.value
        if (current.email.isBlank() || current.password.isBlank()) {
            _uiState.update { it.copy(error = AppError.Unknown("Email and password are required")) }
            return
        }
        viewModelScope.launch {
            signUpUseCase(current.email.trim(), current.password, current.displayName.trim().ifBlank { null })
                .collectLatest { result -> handleAuthResult(result) }
        }
    }

    private fun submitSignOut() {
        viewModelScope.launch {
            signOutUseCase().collectLatest { result -> handleAuthResult(result) }
        }
    }

    private fun submitGoogle(idToken: String) {
        viewModelScope.launch {
            signInWithGoogleUseCase(idToken)
                .collectLatest { result -> handleAuthResult(result) }
        }
    }

    private fun submitFacebook(accessToken: String) {
        viewModelScope.launch {
            signInWithFacebookUseCase(accessToken)
                .collectLatest { result -> handleAuthResult(result) }
        }
    }

    private fun handleAuthResult(result: AuthResult) {
        when (result) {
            AuthResult.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
            AuthResult.SignedOut -> _uiState.update { it.copy(isLoading = false, error = null) }
            is AuthResult.Success -> {
                _uiState.update { it.copy(isLoading = false, error = null) }
                ensureUserDocument(result.userId)
            }
            is AuthResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.error) }
        }
    }

    private fun ensureUserDocument(userId: String) {
        viewModelScope.launch {
            val existing = getUserUseCase(userId)
            if (existing is com.example.blesstify.domain.user.UserResult.Success && existing.data == null) {
                val authUser = getCurrentUserUseCase()
                val fallbackDisplayName = _uiState.value.displayName.trim().ifBlank { null }
                val fallbackEmail = _uiState.value.email.trim().ifBlank { null }
                val payload = User(
                    id = userId,
                    displayName = authUser?.displayName ?: fallbackDisplayName.orEmpty(),
                    photoUrl = authUser?.photoUrl,
                    email = authUser?.email ?: fallbackEmail ?: "",
                    tier = "free",
                    locale = Locale.getDefault().toLanguageTag()
                    // createdAt & updatedAt are set by FieldValue.serverTimestamp() in repository
                )
                createUserUseCase(payload)
            }
        }
    }
}
