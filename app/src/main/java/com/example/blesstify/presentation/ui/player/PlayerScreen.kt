package com.example.blesstify.presentation.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.blesstify.domain.model.detailArtworkUrl
import com.example.blesstify.presentation.player.PlayerViewModel
import com.example.blesstify.presentation.ui.theme.pressScale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    onNavigateToArtist: (String) -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val isLiked by viewModel.isLiked.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val canNext by viewModel.canNext.collectAsState()
    val canPrevious by viewModel.canPrevious.collectAsState()
    val sleepTimerRemainingMs by viewModel.sleepTimerRemainingMs.collectAsState()
    val userPlaylists by viewModel.userPlaylists.collectAsState()
    val uiEvent by viewModel.uiEvents.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    var showMoreSheet by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiEvent) {
        when (val ev = uiEvent) {
            is com.example.blesstify.presentation.player.PlayerUiEvent.Message -> {
                snackbarHostState.showSnackbar(ev.text)
            }
            null -> Unit
        }
    }

    val verticalOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = remember(configuration, density) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }
    val dismissThresholdPx = remember(screenHeightPx) { screenHeightPx * 0.22f }

    suspend fun animateDismissThenBack() {
        verticalOffset.animateTo(
            targetValue = screenHeightPx,
            animationSpec = tween(
                durationMillis = 260,
                easing = androidx.compose.animation.core.LinearOutSlowInEasing
            )
        )
        onBack()
    }

    // Enter animation: slide panel up from bottom on first appear
    LaunchedEffect(Unit) {
        verticalOffset.snapTo(screenHeightPx)
        verticalOffset.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 400,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        )
    }

    // Handle back button
    BackHandler {
        coroutineScope.launch {
            animateDismissThenBack()
        }
    }

    // Album art rotation animation - continuous when playing
    val rotationAngle by animateFloatAsState(
        targetValue = if (isPlaying) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing), // 60 seconds per full rotation
            repeatMode = RepeatMode.Restart
        )
    )

    // Slider thumb scale on drag: 1.0f -> 1.4f (spring)
    val sliderInteractionSource = remember { MutableInteractionSource() }
    val isSliderDragged by sliderInteractionSource.collectIsDraggedAsState()
    val sliderThumbScale by animateFloatAsState(
        targetValue = if (isSliderDragged) 1.4f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f)
    )

    val sleepTimerBadgeText = sleepTimerRemainingMs?.let { remainingMs ->
        val minutesLeft = ((remainingMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
        "${minutesLeft}m"
    }

    LaunchedEffect(currentPosition, duration, isDragging) {
        if (!isDragging) {
            val safeDuration = duration.toFloat().coerceAtLeast(1f)
            sliderPosition = currentPosition.toFloat().coerceIn(0f, safeDuration)
        }
    }

    val displayPositionMs = if (isDragging) sliderPosition.toLong() else currentPosition

    // Gradient background - FULLY OPAQUE for clean modal appearance
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.background
        )
    )

    fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    // Scrim alpha based on drag progress (0 = fully dragged down, 0.5 = at top)
    val scrimAlpha = (1f - (verticalOffset.value / screenHeightPx)).coerceIn(0f, 1f) * 0.5f

    // Root container: transparent, shows previous screen
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Scrim backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
        )

        // Player panel (draggable modal)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        x = 0,
                        y = verticalOffset.value.roundToInt().coerceAtLeast(0)
                    )
                }
                .background(backgroundGradient)
                .statusBarsPadding()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            if (dragAmount > 0f || verticalOffset.value > 0f) {
                                val nextOffset = (verticalOffset.value + dragAmount)
                                    .coerceIn(0f, screenHeightPx)
                                coroutineScope.launch {
                                    verticalOffset.snapTo(nextOffset)
                                }
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (verticalOffset.value > dismissThresholdPx) {
                                    animateDismissThenBack()
                                } else {
                                    verticalOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = 0.85f,
                                            stiffness = 500f
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                verticalOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.85f,
                                        stiffness = 500f
                                    )
                                )
                            }
                        }
                    )
                }
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Top Bar ---
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            animateDismissThenBack()
                        }
                    },
                    modifier = Modifier.background(Color.White.copy(0.08f), CircleShape)
                ) {
                    Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 2.sp
                    )
                }

                IconButton(
                    onClick = { showMoreSheet = true },
                    modifier = Modifier.background(Color.White.copy(0.08f), CircleShape)
                ) {
                    Icon(Icons.Default.MoreVert, null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // --- Album Art với Shadow và Glow nhẹ ---
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f)
                    .shadow(30.dp, RoundedCornerShape(28.dp), ambientColor = Color.Black, spotColor = MaterialTheme.colorScheme.primary)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = currentSong?.detailArtworkUrl(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotationAngle),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // --- Song Info ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong?.title ?: "Select a song",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong?.artist ?: "Unknown Artist",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleLike() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Crossfade(
                        targetState = isLiked,
                        animationSpec = tween(durationMillis = 200)
                    ) { liked ->
                        Icon(
                            imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (liked) Color(0xFFFF4B4B) else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Progress Bar ---
            Column {
                Slider(
                    value = sliderPosition,
                    onValueChange = {
                        isDragging = true
                        sliderPosition = it
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        viewModel.seekTo(sliderPosition.toLong())
                    },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    interactionSource = sliderInteractionSource,
                    thumb = {
                        SliderDefaults.Thumb(
                            interactionSource = sliderInteractionSource,
                            colors = SliderDefaults.colors(thumbColor = Color.White),
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    scaleX = sliderThumbScale
                                    scaleY = sliderThumbScale
                                }
                        )
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(displayPositionMs), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
                    Text(formatTime(duration), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.5f))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Main Controls ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle,
                        null,
                        tint = if (isShuffleEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.previous() },
                    modifier = Modifier.size(44.dp),
                    enabled = canPrevious
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        null,
                        tint = if (canPrevious) Color.White else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Nút Play/Pause nổi bật
                Surface(
                    onClick = { viewModel.playPause() },
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(10.dp, CircleShape)
                        .pressScale()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.next() },
                    modifier = Modifier.size(44.dp),
                    enabled = canNext
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        null,
                        tint = if (canNext) Color.White else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
                    )
                }

                BadgedBox(
                    badge = {
                        sleepTimerBadgeText?.let { label ->
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ) {
                                Text(label)
                            }
                        }
                    }
                ) {
                    IconButton(
                        onClick = { showSleepTimerDialog = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AccessTime,
                            null,
                            tint = if (sleepTimerRemainingMs != null) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
        }
    }

    if (showMoreSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = {
                showMoreSheet = false
                showPlaylistPicker = false
            },
            sheetState = sheetState,
            containerColor = Color(0xFF101218),
            contentColor = Color.White,
            tonalElevation = 0.dp,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .size(width = 56.dp, height = 6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 22.dp)
            ) {
                Text(
                    text = currentSong?.title ?: "More",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))

                ListItem(
                    headlineContent = { Text("Go to artist") },
                    supportingContent = { Text(currentSong?.artist?.takeIf { it.isNotBlank() } ?: "Open artist page", color = Color.White.copy(alpha = 0.65f)) },
                    leadingContent = {
                        Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.08f)) {
                            Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .clickable(enabled = currentSong?.artist?.isNotBlank() == true) {
                            val artistName = currentSong?.artist ?: return@clickable
                            onNavigateToArtist(artistName)
                            showMoreSheet = false
                        },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                ListItem(
                    headlineContent = { Text("Share") },
                    supportingContent = { Text("Send link to Facebook, Zalo, Messenger…", color = Color.White.copy(alpha = 0.65f)) },
                    leadingContent = {
                        Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.08f)) {
                            Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Share, contentDescription = null, tint = Color.White)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .clickable(enabled = currentSong != null) { viewModel.shareCurrentSong() },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )


                ListItem(
                    headlineContent = { Text("Add to playlist…") },
                    supportingContent = { Text("Choose playlist", color = Color.White.copy(alpha = 0.65f)) },
                    leadingContent = {
                        Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.08f)) {
                            Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.LibraryAdd, contentDescription = null, tint = Color.White)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .clickable(enabled = currentSong != null) { showPlaylistPicker = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )


                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showPlaylistPicker) {
        AlertDialog(
            onDismissRequest = { showPlaylistPicker = false },
            title = { Text("Select playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (userPlaylists.isEmpty()) {
                        Text("No playlists")
                    } else {
                        userPlaylists.forEach { pl ->
                            TextButton(
                                onClick = {
                                    viewModel.addCurrentSongToPlaylist(pl.id)
                                    showPlaylistPicker = false
                                    showMoreSheet = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(pl.title, fontWeight = FontWeight.SemiBold)
                                    pl.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistPicker = false }) { Text("Close") }
            }
        )
    }

    if (showSleepTimerDialog) {
        val initialMinutes = sleepTimerRemainingMs?.let { remainingMs ->
            ((remainingMs + 59_999L) / 60_000L).toInt().coerceIn(5, 120)
        } ?: 30
        var minutes by remember { mutableStateOf(initialMinutes) }
        val presetMinutes = listOf(5, 10, 20, 30, 45, 60, 90, 120)

        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep timer") },
            text = {
                Column {
                    Text("Play music for $minutes minutes, then stop.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetMinutes.forEach { preset ->
                            AssistChip(
                                onClick = { minutes = preset },
                                label = { Text("${preset}m") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (minutes == preset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = if (minutes == preset) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Slider(
                        value = minutes.toFloat(),
                        onValueChange = { value ->
                            val rounded = (value / 5f).roundToInt() * 5
                            minutes = rounded.coerceIn(5, 120)
                        },
                        valueRange = 5f..120f,
                        steps = 22
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setSleepTimer(minutes)
                    showSleepTimerDialog = false
                }) {
                    Text("Start")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (sleepTimerRemainingMs != null) {
                        viewModel.clearSleepTimer()
                    }
                    showSleepTimerDialog = false
                }) {
                    Text(if (sleepTimerRemainingMs != null) "Clear" else "Cancel")
                }
            }
        )
    }
}