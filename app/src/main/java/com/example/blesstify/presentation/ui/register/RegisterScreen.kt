package com.example.blesstify.presentation.ui.register

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.blesstify.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.blesstify.presentation.auth.AuthUiEvent
import com.example.blesstify.presentation.auth.AuthViewModel
import com.example.blesstify.core.error.AppError
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            val message = when (error) {
                AppError.InvalidCredentials -> "Invalid email or password"
                AppError.NotFound -> "Account not found"
                AppError.EmailAlreadyInUse -> "Email already in use"
                AppError.WeakPassword -> "Password is too weak"
                AppError.Network -> "Network error, please try again"
                is AppError.Unknown -> error.message ?: "Unknown error"
                else -> "An error occurred"
            }
            scope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.onEvent(AuthUiEvent.ClearError)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(24.dp)
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Header / Logo
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "Blesstify logo",
                modifier = Modifier.size(84.dp)
            )
            Text(
                "Blesstify",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Basic Info", style = MaterialTheme.typography.labelSmall, color = Color.White)
                HorizontalDivider(
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Join the Symphony",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Create your account to start listening.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
            )

            // Inputs
            RegisterInputField(
                label = "Full Name",
                placeholder = "John Doe",
                value = uiState.displayName,
                onValueChange = { viewModel.onEvent(AuthUiEvent.DisplayNameChanged(it)) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            RegisterInputField(
                label = "Email Address",
                placeholder = "name@example.com",
                value = uiState.email,
                onValueChange = { viewModel.onEvent(AuthUiEvent.EmailChanged(it)) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            RegisterInputField(
                label = "Password",
                placeholder = "********",
                value = uiState.password,
                onValueChange = {
                    viewModel.onEvent(AuthUiEvent.PasswordChanged(it))
                    if (confirmPassword.isNotEmpty()) {
                        localError = if (it != confirmPassword) "Passwords do not match" else null
                    }
                },
                isPassword = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            RegisterInputField(
                label = "Confirm Password",
                placeholder = "********",
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    localError = if (uiState.password.isNotEmpty() && it != uiState.password) {
                        "Passwords do not match"
                    } else {
                        null
                    }
                },
                isPassword = true
            )

            if (localError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(localError!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    if (localError == null && confirmPassword.isNotBlank()) {
                        viewModel.onEvent(AuthUiEvent.SubmitRegister)
                    } else if (confirmPassword.isBlank()) {
                        localError = "Please confirm your password"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Text("Next Step →", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text("← Back", color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onNavigateToLogin, contentPadding = PaddingValues(0.dp)) {
                    Text("Log In", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RegisterInputField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
