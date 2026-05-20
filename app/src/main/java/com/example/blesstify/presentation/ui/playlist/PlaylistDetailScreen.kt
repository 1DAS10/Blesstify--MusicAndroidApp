package com.example.blesstify.presentation.ui.playlist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.blesstify.domain.model.Playlist
import com.example.blesstify.domain.model.Song
import com.example.blesstify.domain.model.listArtworkUrl
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit,
    onPlay: () -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadCover(it) }
    }

    LaunchedEffect(uiState.addedMessage) {
        uiState.addedMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAddedMessage()
        }
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Hero Background with Cover
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                if (uiState.playlist?.coverUrl != null) {
                    AsyncImage(
                        model = uiState.playlist?.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.5f
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF0B0E13))
                            )
                        )
                )
            }

            if (uiState.isLoading && uiState.playlist == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error ?: "", color = Color(0xFFFF6B6B))
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { viewModel.loadPlaylist() }) {
                            Text("Retry", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            } else {
                val playlist = uiState.playlist
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Color.White)
                            }
                            Text(
                                "Playlist",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.7f),
                                letterSpacing = 1.sp
                            )
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreHoriz, contentDescription = "Options", tint = Color.White)
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    modifier = Modifier.background(Color(0xFF1A1D23))
                                ) {
                                    if (uiState.isOwner) {
                                        DropdownMenuItem(
                                            text = { Text("Edit Info", color = Color.White) },
                                            leadingIcon = { Icon(Icons.Default.Edit, null, tint = Color.Gray) },
                                            onClick = {
                                                showMenu = false
                                                viewModel.showEditDialog()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Upload Cover", color = Color.White) },
                                            leadingIcon = { Icon(Icons.Default.Image, null, tint = Color.Gray) },
                                            onClick = {
                                                showMenu = false
                                                photoPickerLauncher.launch("image/*")
                                            }
                                        )
                                        HorizontalDivider(color = Color.White.copy(0.1f))
                                        DropdownMenuItem(
                                            text = { Text("Delete Playlist", color = Color(0xFFFF6B6B)) },
                                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF6B6B)) },
                                            onClick = {
                                                showMenu = false
                                                viewModel.deletePlaylist()
                                            }
                                        )
                                    } else {
                                        DropdownMenuItem(
                                            text = { Text("No owner options", color = Color.Gray) },
                                            onClick = { showMenu = false }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            playlist?.title ?: "Untitled",
                            style = MaterialTheme.typography.displayLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 50.sp
                        )

                        if (playlist?.description != null) {
                            Text(
                                playlist.description,
                                color = Color.White.copy(0.6f),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                "${playlist?.trackCount ?: 0} tracks",
                                color = Color.White.copy(0.7f)
                            )
                            if (playlist?.isPublic == true) {
                                Text(" • Public", color = Color.White.copy(0.5f))
                            }
                            if (playlist?.collaboratorIds?.isNotEmpty() == true) {
                                Text(" • Collaborative", color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            IconButton(
                                onClick = { 
                                    viewModel.togglePlayPause() 
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(
                                    if (uiState.isPlaying && uiState.isCurrentPlaylistPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                                    null, 
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(
                                onClick = { 
                                    viewModel.shufflePlaylist() 
                                },
                                modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)
                            ) {
                                Icon(Icons.Default.Shuffle, null, tint = Color.White)
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            IconButton(
                                onClick = { viewModel.toggleLike() },
                                modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)
                            ) {
                                Icon(
                                    if (uiState.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    null,
                                    tint = if (uiState.isLiked) Color(0xFFFF4B4B) else Color.White
                                )
                            }
                            
                            if ((playlist?.likeCount ?: 0) > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${playlist?.likeCount}", 
                                    color = Color.White.copy(0.7f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            if (uiState.canEdit) {
                                Spacer(modifier = Modifier.width(12.dp))
                                OutlinedButton(
                                    onClick = { viewModel.showAddSongsDialog() },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.2f)),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Songs", color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Table Header
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Text("#", modifier = Modifier.width(32.dp), color = Color.Gray)
                            Text("TITLE", modifier = Modifier.weight(1f), color = Color.Gray)
                        }
                        HorizontalDivider(color = Color.White.copy(0.1f))
                    }

                    if (uiState.tracks.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("No tracks yet", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
                                    
                                    if (uiState.canEdit) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { viewModel.showAddSongsDialog() },
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.5f)),
                                            shape = RoundedCornerShape(24.dp)
                                        ) {
                                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Add your first song", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(uiState.tracks) { index, track ->
                            val song = uiState.songs.find { it.id == track.songId }
                            PlaylistTrackRow(
                                index = index + 1,
                                song = song,
                                title = song?.title ?: track.songId,
                                artist = song?.artist ?: "Unknown Artist",
                                coverUrl = song?.coverUrl,
                                canEdit = uiState.canEdit,
                                isPlaying = track.songId == uiState.currentPlayingSongId,
                                onClick = {
                                    viewModel.playPlaylist(index)
                                    onPlay()
                                },
                                onAddToQueue = { s -> viewModel.addSongToQueue(s) },
                                onPlayNext = { s -> viewModel.playSongNext(s) },
                                onRemove = { viewModel.removeTrack(track.songId) }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
    }

    // --- Dialogs ---

    if (uiState.showAddSongsDialog) {
        AddSongsDialog(
            searchQuery = uiState.searchQuery,
            searchResults = uiState.searchResults,
            isSearching = uiState.isSearching,
            isAddingTrack = uiState.isAddingTrack,
            addedMessage = uiState.addedMessage,
            onQueryChange = { viewModel.onSearchQueryChange(it) },
            onAddSong = { viewModel.addTrack(it) },
            onDismiss = { viewModel.hideAddSongsDialog() }
        )
    }

    if (uiState.showEditDialog) {
        EditPlaylistDialog(
            playlist = uiState.playlist ?: return,
            onDismiss = { viewModel.hideEditDialog() },
            onSave = { t, d, p -> viewModel.updatePlaylistInfo(t, d, p) }
        )
    }
}

@Composable
fun EditPlaylistDialog(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf<String>(playlist.title) }
    var description by remember { mutableStateOf<String>(playlist.description ?: "") }
    var isPublic by remember { mutableStateOf<Boolean>(playlist.isPublic) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1D23),
        title = { Text("Edit Playlist", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPublic, onCheckedChange = { isPublic = it })
                    Text("Public Playlist", color = Color.White)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, description, isPublic) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddSongsDialog(
    searchQuery: String,
    searchResults: List<Song>,
    isSearching: Boolean,
    isAddingTrack: Boolean,
    addedMessage: String?,
    onQueryChange: (String) -> Unit,
    onAddSong: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.75f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1A1D23)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Add Songs", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
                }
                
                if (addedMessage != null) {
                    Surface(
                        color = if (addedMessage.startsWith("Failed")) Color(0xFFFF6B6B).copy(0.1f) else MaterialTheme.colorScheme.primary.copy(0.1f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text(
                            addedMessage,
                            modifier = Modifier.padding(12.dp),
                            color = if (addedMessage.startsWith("Failed")) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("Search songs...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(0.05f),
                        unfocusedContainerColor = Color.White.copy(0.05f),
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (isSearching) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                        items(searchResults) { song ->
                            AddSongItem(song = song, onAdd = { onAddSong(song) }, isLoading = isAddingTrack)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddSongItem(song: Song, onAdd: () -> Unit, isLoading: Boolean = false) {
    Surface(color = Color.White.copy(0.05f), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (song.coverUrl != null) {
                    AsyncImage(
                        model = song.listArtworkUrl(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = Color.Gray, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(
                onClick = onAdd, 
                enabled = !isLoading,
                modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary.copy(0.15f), CircleShape)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun PlaylistTrackRow(
    index: Int,
    song: Song?,
    title: String,
    artist: String,
    coverUrl: String?,
    canEdit: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onAddToQueue: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        color = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isPlaying) {
                Icon(
                    Icons.Default.MusicNote,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(32.dp).size(16.dp)
                )
            } else {
                Text(index.toString(), modifier = Modifier.width(32.dp), color = Color.Gray)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(0.05f)),
                contentAlignment = Alignment.Center
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.MusicNote, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(artist, color = Color.Gray, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(0.06f), CircleShape)
                ) {
                    Icon(Icons.Default.MoreVert, null, tint = Color.White.copy(0.85f), modifier = Modifier.size(18.dp))
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF1A1D23))
                ) {
                    DropdownMenuItem(
                        text = { Text("Add to queue", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.QueueMusic, null, tint = Color.Gray) },
                        enabled = song != null,
                        onClick = {
                            val s = song ?: return@DropdownMenuItem
                            showMenu = false
                            onAddToQueue(s)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Play next", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.SkipNext, null, tint = Color.Gray) },
                        enabled = song != null,
                        onClick = {
                            val s = song ?: return@DropdownMenuItem
                            showMenu = false
                            onPlayNext(s)
                        }
                    )

                    if (canEdit) {
                        HorizontalDivider(color = Color.White.copy(0.1f))
                        DropdownMenuItem(
                            text = { Text("Remove from playlist", color = Color(0xFFFF6B6B)) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF6B6B)) },
                            onClick = {
                                showMenu = false
                                onRemove()
                            }
                        )
                    }
                }
            }
        }
    }
}
