package com.example.blesstify.presentation.ui.login

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.blesstify.R
import com.example.blesstify.core.error.AppError
import com.example.blesstify.presentation.auth.AuthUiEvent
import com.example.blesstify.presentation.auth.AuthViewModel
import com.example.blesstify.presentation.auth.FacebookAuthHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private const val LOGIN_TAG = "LoginScreen"

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToRegister: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                viewModel.onEvent(AuthUiEvent.SubmitGoogle(idToken))
            } ?: run {
                viewModel.onEvent(AuthUiEvent.GoogleSignInFailed("Google Sign-In failed: No ID Token"))
            }
        } catch (e: ApiException) {
            viewModel.onEvent(AuthUiEvent.GoogleSignInFailed("Google Sign-In failed: ${e.statusCode}"))
        }
    }

    // Chuyển hướng khi đã đăng nhập thành công
    LaunchedEffect(uiState.authState) {
        if (uiState.authState is com.example.blesstify.domain.auth.AuthState.Authenticated) {
            onNavigateToMain()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        // Header / Logo
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = "Blesstify logo",
            modifier = Modifier.size(84.dp)
        )
        Text(
            "Blesstify",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Please sign in to continue your journey.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
        )

        // Inputs
        OutlinedTextField(
            value = uiState.email,
            onValueChange = { viewModel.onEvent(AuthUiEvent.EmailChanged(it)) },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = { viewModel.onEvent(AuthUiEvent.PasswordChanged(it)) },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = PasswordVisualTransformation()
        )

        if (uiState.error != null) {
            Text(
                text = mapErrorMessage(uiState.error!!),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = { viewModel.onEvent(AuthUiEvent.SubmitLogin) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("Log In", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                // Sign out first to always show the account picker
                googleSignInClient.signOut().addOnCompleteListener {
                    googleLauncher.launch(googleSignInClient.signInIntent)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !uiState.isLoading
        ) {
            Text("Sign in with Google", fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                Log.d(LOGIN_TAG, "Facebook sign-in button clicked")
                val activity = context as? Activity
                if (activity == null) {
                    Log.e(LOGIN_TAG, "Facebook sign-in failed: invalid Activity context")
                    viewModel.onEvent(AuthUiEvent.FacebookSignInFailed("Facebook Sign-In failed: invalid Activity"))
                    return@OutlinedButton
                }
                FacebookAuthHelper.login(
                    activity = activity,
                    onToken = { token ->
                        Log.d(LOGIN_TAG, "Facebook sign-in success, token received")
                        viewModel.onEvent(AuthUiEvent.SubmitFacebook(token))
                    },
                    onCancel = {
                        Log.w(LOGIN_TAG, "Facebook sign-in canceled by user")
                        viewModel.onEvent(AuthUiEvent.FacebookSignInFailed("Facebook Sign-In canceled"))
                    },
                    onError = { err ->
                        Log.e(LOGIN_TAG, "Facebook sign-in error: ${err.message}", err)
                        viewModel.onEvent(AuthUiEvent.FacebookSignInFailed(err.message))
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !uiState.isLoading
        ) {
            Text("Sign in with Facebook", fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToRegister) {
            Text("Don't have an account? Sign Up", color = Color.White)
        }
    }
}

private fun mapErrorMessage(error: AppError): String {
    return when (error) {
        AppError.Network -> "Network connection error"
        AppError.InvalidCredentials -> "Invalid email or password"
        AppError.NotFound -> "User not found"
        AppError.EmailAlreadyInUse -> "Email is already in use"
        is AppError.Unknown -> error.message ?: "An unknown error occurred"
        else -> "An error occurred. Please try again."
    }
}
