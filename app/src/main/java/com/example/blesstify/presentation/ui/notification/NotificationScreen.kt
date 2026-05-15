package com.example.blesstify.presentation.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.domain.model.Notification
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    onNavigateToPlaylist: (String) -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToAIFocus: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val notificationsResource by viewModel.notifications.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Notifications",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        when (val resource = notificationsResource) {
            is Resource.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is Resource.Success -> {
                val notifications = resource.data ?: emptyList()
                if (notifications.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No notifications yet", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(notifications) { notification ->
                            NotificationItem(
                                notification = notification,
                                onClick = {
                                    viewModel.markAsRead(notification.id)
                                    handleDeepLink(
                                        notification = notification,
                                        onNavigateToPlaylist = onNavigateToPlaylist,
                                        onNavigateToPlayer = onNavigateToPlayer,
                                        onNavigateToAIFocus = onNavigateToAIFocus,
                                        onNavigateToProfile = onNavigateToProfile
                                    )
                                }
                            )
                        }
                    }
                }
            }
            is Resource.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${resource.error?.message}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit
) {
    val isUnread = notification.readAt == null
    val icon = when (notification.type) {
        "playlist_update" -> Icons.Default.PlaylistPlay
        "new_song" -> Icons.Default.MusicNote
        "recommendation" -> Icons.Default.AutoAwesome
        "subscription" -> Icons.Default.Stars
        else -> Icons.Default.Notifications
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isUnread) Color.White.copy(alpha = 0.05f) else Color.Transparent)
            .padding(24.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isUnread) MaterialTheme.colorScheme.primaryContainer else Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isUnread) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    notification.title,
                    color = Color.White,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                notification.body,
                color = if (isUnread) Color.LightGray else Color.Gray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                formatDate(notification.createdAt),
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun handleDeepLink(
    notification: Notification,
    onNavigateToPlaylist: (String) -> Unit,
    onNavigateToPlayer: (String) -> Unit,
    onNavigateToAIFocus: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    when (notification.type) {
        "playlist_update" -> {
            notification.data["playlistId"]?.let { onNavigateToPlaylist(it) }
        }
        "new_song" -> {
            notification.data["songId"]?.let { onNavigateToPlayer(it) }
        }
        "recommendation" -> {
            onNavigateToAIFocus()
        }
        "subscription" -> {
            onNavigateToProfile()
        }
        "system" -> {
            // No navigation
        }
    }
}
