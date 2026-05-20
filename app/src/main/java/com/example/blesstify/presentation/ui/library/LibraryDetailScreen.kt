package com.example.blesstify.presentation.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.blesstify.domain.model.Song

/**
 * A reusable detail screen that displays metadata + songs of a specific Album or Artist.
 *
 * @param type Either "album" or "artist"
 * @param itemId Firestore document id for album/artist
 * @param onBack Navigate back
 * @param onNavigateToPlayer Play songs list starting from given index
 * @param viewModel shared LibraryViewModel from Hilt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryDetailScreen(
    type: String,
    itemId: String,
    onBack: () -> Unit,
    onNavigateToPlayer: (List<Song>, Int) -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val selectedAlbum = if (type == "album") uiState.albums.find { it.id == itemId } else null
    val selectedArtist = if (type == "artist") uiState.artists.find { it.id == itemId } else null

    val title = when (type) {
        "album" -> selectedAlbum?.name ?: "Unknown Album"
        "artist" -> selectedArtist?.name ?: "Unknown Artist"
        else -> ""
    }

    val songs: List<Song> = when (type) {
        "album" -> selectedAlbum?.songs ?: emptyList()
        "artist" -> selectedArtist?.songs ?: emptyList()
        else -> emptyList()
    }

    val subtitle = when (type) {
        "album" -> selectedAlbum?.artist.orEmpty()
        "artist" -> "${songs.size} Songs"
        else -> ""
    }

    val coverUrl = when (type) {
        "album" -> selectedAlbum?.coverUrl
        "artist" -> selectedArtist?.coverUrl
        else -> null
    }

    val deleteLabel = if (type == "album") "album" else "artist"

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${deleteLabel.replaceFirstChar { it.uppercase() }}") },
            text = { Text("Are you sure you want to delete \"$title\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                showDeleteDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(
                                if (type == "artist") androidx.compose.foundation.shape.CircleShape
                                else RoundedCornerShape(16.dp)
                            )
                            .background(Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!coverUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = coverUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                if (type == "artist") Icons.Default.Person else Icons.Default.MusicNote,
                                null,
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (subtitle.isNotBlank()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (songs.isNotEmpty()) {
                        Button(
                            onClick = { onNavigateToPlayer(songs, 0) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play All", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${songs.size} Songs",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (songs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No songs found.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                itemsIndexed(songs) { index, song ->
                    com.example.blesstify.presentation.ui.main.SongResultItem(
                        song = song,
                        onClick = { onNavigateToPlayer(songs, index) },
                        onAddToQueue = { viewModel.addSongToQueue(it) },
                        onPlayNext = { viewModel.playSongNext(it) }
                    )
                    if (index < songs.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
