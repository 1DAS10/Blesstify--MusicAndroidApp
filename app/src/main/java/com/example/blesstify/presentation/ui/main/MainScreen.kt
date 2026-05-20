package com.example.blesstify.presentation.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.blesstify.domain.model.Song
import com.example.blesstify.domain.model.listArtworkUrl
import com.example.blesstify.presentation.player.PlayerViewModel
import com.example.blesstify.presentation.user.UserViewModel
import com.example.blesstify.presentation.ui.navigation.MainTab
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MainScreen(
    onNavigateToPlayer: (Song) -> Unit,
    onNavigateToPlayerFromSearch: (List<Song>, Int) -> Unit,
    onNavigateToPlaylist: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToUpload: () -> Unit,
    onNavigateToCreatePlaylist: () -> Unit,
    onNavigateToCreateAlbum: () -> Unit,
    onNavigateToCreateAlbumWithId: (String) -> Unit,
    onNavigateToCreateArtist: () -> Unit,
    onNavigateToCreateArtistWithId: (String) -> Unit,
    onNavigateToAIFocus: () -> Unit,
    onNavigateToNotification: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onOpenPlayer: () -> Unit,
    onLogout: () -> Unit,
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val notificationViewModel: com.example.blesstify.presentation.ui.notification.NotificationViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val userState by userViewModel.uiState.collectAsState()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        notificationViewModel.inAppEvents.collectLatest { event ->
            when (event) {
                is com.example.blesstify.presentation.ui.notification.InAppNotificationEvent.Show -> {
                    snackbarHostState.showSnackbar(
                        message = "${event.notification.title}: ${event.notification.body}",
                        withDismissAction = true,
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }

    var selectedTabRoute by rememberSaveable { mutableStateOf(MainTab.Explore.route) }
    val selectedTab = when (selectedTabRoute) {
        MainTab.Search.route -> MainTab.Search
        MainTab.Focus.route -> MainTab.Focus
        MainTab.Library.route -> MainTab.Library
        MainTab.Profile.route -> MainTab.Profile
        else -> MainTab.Explore
    }

    Scaffold(
        topBar = {
            MainTopBar(
                unreadCount = unreadCount,
                userPhotoUrl = userState.user?.photoUrl,
                userName = userState.user?.displayName ?: "User",
                selectedTab = selectedTab,
                onNotificationClick = onNavigateToNotification,
                onProfileClick = { selectedTabRoute = MainTab.Profile.route }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            MainBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTabRoute = it.route }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                MainTab.Explore -> ExploreScreen(
                    onFeaturedClick = { song -> onNavigateToPlayer(song) },
                    onPlaylistClick = { playlistId -> onNavigateToPlaylist(playlistId) },
                    onAIFocusClick = onNavigateToAIFocus,
                    onSongClick = { song -> onNavigateToPlayer(song) }
                )
                MainTab.Search -> SearchScreen(
                    onNavigateToPlayer = { song -> onNavigateToPlayer(song) },
                    onNavigateToPlayerFromSearch = { songs, index -> onNavigateToPlayerFromSearch(songs, index) },
                    onNavigateToCategory = onNavigateToCategory
                )
                MainTab.Focus -> com.example.blesstify.presentation.ui.focus.FocusScreen()
                MainTab.Library -> com.example.blesstify.presentation.ui.library.LibraryMainScreen(
                    onOpenPlaylist = { playlistId -> onNavigateToPlaylist(playlistId) },
                    onCreatePlaylist = onNavigateToCreatePlaylist,
                    onCreateAlbum = onNavigateToCreateAlbum,
                    onCreateArtist = onNavigateToCreateArtist,
                    onUploadClick = onNavigateToUpload,
                    onOpenAlbum = onNavigateToAlbum,
                    onOpenArtist = onNavigateToArtist,
                    onEditAlbum = { albumId -> onNavigateToCreateAlbumWithId(albumId) },
                    onEditArtist = { artistId -> onNavigateToCreateArtistWithId(artistId) }
                )
                MainTab.Profile -> com.example.blesstify.presentation.ui.profile.ProfileScreen(
                    onLogout = onLogout,
                    onNavigateToHistory = onNavigateToHistory,
                    onNavigateToEqualizer = onNavigateToEqualizer,
                    onNavigateToUpload = onNavigateToUpload
                )
            }
        }
    }
}

@Composable
fun MainTopBar(
    unreadCount: Int,
    userPhotoUrl: String?,
    userName: String,
    selectedTab: MainTab,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }

    val subtitle = when (selectedTab) {
        MainTab.Explore -> "Discover something new"
        MainTab.Search -> "Find your favorite tracks"
        MainTab.Focus -> "Time to focus"
        MainTab.Library -> "Your music collection"
        MainTab.Profile -> "Manage your account"
        else -> "Discover something new"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Avatar + Greeting
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                if (!userPhotoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = userPhotoUrl,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = "$greeting, $userName \uD83D\uDC4B",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // Right side: Icons
        Row(verticalAlignment = Alignment.CenterVertically) {
            BadgedBox(
                badge = {
                    if (unreadCount > 0) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text(unreadCount.toString(), color = Color.White)
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                IconButton(onClick = onNotificationClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White
                    )
                }
            }
        }
    }
}



@Composable
fun MainBottomBar(selectedTab: MainTab, onTabSelected: (MainTab) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
        tonalElevation = 0.dp
    ) {
        val tabs = listOf(
            MainTabItem(MainTab.Explore, Icons.Default.Explore),
            MainTabItem(MainTab.Search, Icons.Default.Search),
            MainTabItem(MainTab.Focus, Icons.Default.Timer),
            MainTabItem(MainTab.Library, Icons.Default.LibraryMusic),
            MainTabItem(MainTab.Profile, Icons.Default.Person)
        )

        tabs.forEach { item ->
            NavigationBarItem(
                selected = selectedTab == item.tab,
                onClick = { onTabSelected(item.tab) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.tab.label) },
                label = { Text(item.tab.label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun MiniPlayerBar(
    song: Song,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onOpenPlayer,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val artworkUrl = song.listArtworkUrl()
            if (!artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(12.dp).size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

data class MainTabItem(val tab: MainTab, val icon: ImageVector)
