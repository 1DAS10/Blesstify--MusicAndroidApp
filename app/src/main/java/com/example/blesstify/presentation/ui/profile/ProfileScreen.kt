package com.example.blesstify.presentation.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.blesstify.domain.model.User
import com.example.blesstify.core.error.AppError
import com.example.blesstify.presentation.user.UserUiEvent
import com.example.blesstify.presentation.user.UserViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToUpload: () -> Unit,
    viewModel: UserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.onEvent(UserUiEvent.AvatarSelected(uri))
        }
    }

    if (uiState.isEditing) {
        EditProfileDialog(
            state = uiState,
            onDismiss = { viewModel.onEvent(UserUiEvent.DismissEdit) },
            onDisplayNameChange = { viewModel.onEvent(UserUiEvent.DisplayNameChanged(it)) },
            onEmailChange = { viewModel.onEvent(UserUiEvent.EmailChanged(it)) },
            onLocaleChange = { viewModel.onEvent(UserUiEvent.LocaleChanged(it)) },
            onPickAvatar = { avatarPicker.launch("image/*") },
            onSave = { viewModel.onEvent(UserUiEvent.SaveChanges) }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            ProfileHeader(
                user = uiState.user,
                subscription = uiState.subscription,
                onEditProfile = { viewModel.onEvent(UserUiEvent.StartEdit) }
            )
            Spacer(modifier = Modifier.height(32.dp))
            LibraryStats()
            Spacer(modifier = Modifier.height(32.dp))
            WeeklyFocus()
            Spacer(modifier = Modifier.height(32.dp))
            PreferenceSection(
                onAccountClick = { viewModel.onEvent(UserUiEvent.StartEdit) },
                onHistoryClick = onNavigateToHistory,
                onEqualizerClick = onNavigateToEqualizer,
                onUploadClick = onNavigateToUpload
            )
            Spacer(modifier = Modifier.height(32.dp))
            LogoutButton(onClick = onLogout)
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun ProfileHeader(
    user: User?,
    subscription: com.example.blesstify.domain.model.Subscription?,
    onEditProfile: () -> Unit
) {
    val displayName = if (user?.displayName.isNullOrBlank()) "Guest" else user!!.displayName
    val subtitle = listOfNotNull(
        user?.email?.takeIf { it.isNotBlank() },
        user?.locale?.takeIf { it.isNotBlank() }
    ).joinToString(" • ").ifBlank { "No account info" }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(0.05f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = user?.photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                )
                IconButton(
                    onClick = onEditProfile,
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = Color.Black)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            val tier = user?.tier ?: "free"
            val isPremium = tier == "premium"
            
            Surface(
                color = if (isPremium) MaterialTheme.colorScheme.primaryContainer.copy(0.1f) else Color.White.copy(0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPremium) Icons.Default.Star else Icons.Default.Person, 
                        null, 
                        modifier = Modifier.size(14.dp), 
                        tint = if (isPremium) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isPremium) "Premium Listener" else "Free Account", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = if (isPremium) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
            
            if (isPremium && subscription != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                val expiry = subscription.currentPeriodEnd?.toDate()?.let { sdf.format(it) } ?: "Unknown"
                Text(
                    text = "Plan: ${subscription.plan.replaceFirstChar { it.uppercase() }} • Expires: $expiry",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            } else if (!isPremium) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { /* Navigate to upgrade or show info */ },
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Upgrade to Premium", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(displayName, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Update your profile to personalize your music experience.",
                color = Color.White.copy(0.7f),
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun LibraryStats() {
    Column {
        Text("Library Stats", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard("1.2k", "SONGS", Modifier.weight(1f))
            StatCard("48", "PLAYLISTS", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(0.05f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Headphones, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("342", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("HOURS LISTENED", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(0.05f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun WeeklyFocus() {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text("Weekly Focus", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text("Your listening patterns over the last 7 days.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            Text("24h 12m\ntotal", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            color = Color.White.copy(0.05f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
        ) {
            // Placeholder for Chart
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                        Text(day, color = Color.Gray, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PreferenceSection(
    onAccountClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onUploadClick: () -> Unit
) {
    Column {
        Text("Preferences", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            color = Color.White.copy(0.05f),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
        ) {
            Column {
                PreferenceItem(Icons.Default.Person, "Account", "Subscription, email, password", onClick = onAccountClick)
                PreferenceItem(Icons.Default.BarChart, "Equalizer & Effects", "Custom EQ, Bass Boost, and Virtualizer", onClick = onEqualizerClick)
                PreferenceItem(Icons.Default.UploadFile, "Upload", "Upload your own music", onClick = onUploadClick)
                PreferenceItem(Icons.Default.History, "Listening history", "Replay your favorite tracks", onClick = onHistoryClick)
            }
        }
    }
}

@Composable
fun PreferenceItem(icon: ImageVector, title: String, subTitle: String, onClick: () -> Unit, isLast: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subTitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
        if (!isLast) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.White.copy(0.05f))
        }
    }
}

@Composable
fun LogoutButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.05f), contentColor = MaterialTheme.colorScheme.error),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(0.2f))
    ) {
        Icon(Icons.AutoMirrored.Filled.Logout, null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Log Out", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EditProfileDialog(
    state: com.example.blesstify.presentation.user.UserUiState,
    onDismiss: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onLocaleChange: (String) -> Unit,
    onPickAvatar: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(
                    model = state.avatarLocalUri ?: state.user?.photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                        .align(Alignment.CenterHorizontally)
                )
                OutlinedButton(onClick = onPickAvatar, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Change avatar")
                }
                OutlinedTextField(
                    value = state.displayNameInput,
                    onValueChange = onDisplayNameChange,
                    label = { Text("Display name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.emailInput,
                    onValueChange = onEmailChange,
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.localeInput,
                    onValueChange = onLocaleChange,
                    label = { Text("Locale") },
                    modifier = Modifier.fillMaxWidth()
                )
                val errorMessage = state.error?.let { mapAppErrorMessage(it) }
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = !state.isLoading) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun mapAppErrorMessage(error: AppError): String = when (error) {
    AppError.Network -> "Network error. Please try again."
    AppError.NotFound -> "User not found."
    AppError.PermissionDenied -> "Permission denied."
    AppError.InvalidData -> "Invalid profile data."
    is AppError.Unknown -> error.message ?: "Unknown error."
    else -> "An error occurred."
}
