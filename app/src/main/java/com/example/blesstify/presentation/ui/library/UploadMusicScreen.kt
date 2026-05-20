package com.example.blesstify.presentation.ui.library

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.SideEffect

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UploadMusicScreen(
    onBack: () -> Unit,
    viewModel: UploadMusicViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Configure status bar
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            it.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
        }
    }

    // Audio file picker
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            val fileName = cursor?.use { c ->
                val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                c.moveToFirst()
                if (nameIndex >= 0) c.getString(nameIndex) else "audio_file"
            } ?: "audio_file"
            // Take persistable permission so the URI remains accessible
            context.contentResolver.takePersistableUriPermission(
                it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.onEvent(UploadUiEvent.AudioSelected(it, fileName))
        }
    }

    // Cover image picker
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            val fileName = cursor?.use { c ->
                val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                c.moveToFirst()
                if (nameIndex >= 0) c.getString(nameIndex) else "cover_image"
            } ?: "cover_image"
            context.contentResolver.takePersistableUriPermission(
                it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.onEvent(UploadUiEvent.CoverSelected(it, fileName))
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(UploadUiEvent.ClearMessages)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(UploadUiEvent.ClearMessages)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ChevronLeft, null, tint = Color.White)
                    }
                    Text(
                        "Blessify",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Upload Music",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Add high-fidelity tracks to your personal library.",
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ─── Audio File Selection Area ───
                AudioDropArea(
                    audioFileName = uiState.audioFileName,
                    isLoading = uiState.isLoading,
                    onBrowseClick = {
                        audioPickerLauncher.launch(arrayOf("audio/*", "application/octet-stream"))
                    },
                    onRemoveClick = { viewModel.onEvent(UploadUiEvent.RemoveAudio) }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ─── Upload Progress ───
                if (uiState.uploadProgress != null) {
                    Surface(
                        color = Color.White.copy(0.05f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            Text(
                                uiState.uploadProgress!!,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                MetadataForm(
                    title = uiState.title,

                    artist = uiState.artist,
                    artistId = uiState.artistId,
                    artistOptions = uiState.artistOptions,

                    album = uiState.album,
                    albumId = uiState.albumId,
                    albumOptions = uiState.albumOptions,

                    genres = uiState.genres,
                    coverUri = uiState.coverUri,
                    isPublic = uiState.isPublic,
                    isLoading = uiState.isLoading,
                    hasAudio = uiState.audioUri != null,
                    onTitleChange = { viewModel.onEvent(UploadUiEvent.TitleChanged(it)) },

                    onArtistChange = { viewModel.onEvent(UploadUiEvent.ArtistChanged(it)) },
                    onArtistSelected = { viewModel.onEvent(UploadUiEvent.ArtistSelected(it)) },

                    onAlbumChange = { viewModel.onEvent(UploadUiEvent.AlbumChanged(it)) },
                    onAlbumSelected = { viewModel.onEvent(UploadUiEvent.AlbumSelected(it)) },

                    onGenreToggle = { viewModel.onEvent(UploadUiEvent.GenreToggled(it)) },
                    onPublicChange = { viewModel.onEvent(UploadUiEvent.PublicChanged(it)) },
                    onCoverClick = {
                        coverPickerLauncher.launch(arrayOf("image/*"))
                    },
                    onRemoveCover = { viewModel.onEvent(UploadUiEvent.RemoveCover) },
                    onSave = { viewModel.onEvent(UploadUiEvent.Submit) },
                    onCancel = onBack
                )
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

private val uploadGenres = listOf(
    "Pop", "Rock", "Hip Hop", "Rap", "EDM", "Electronic", "Jazz", "Blues",
    "Classical", "R&B", "Country", "Folk", "Metal", "Reggae", "K-pop", "J-pop",
    "C-pop", "Latin", "Phonk", "Lo-fi", "Other"
)

// ─── Audio Drop Area ───
@Composable
private fun AudioDropArea(
    audioFileName: String?,
    isLoading: Boolean,
    onBrowseClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(0.3f))
    ) {
        if (audioFileName != null) {
            // File selected state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.AudioFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    audioFileName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onBrowseClick,
                        shape = RoundedCornerShape(24.dp),
                        enabled = !isLoading
                    ) {
                        Text("Change")
                    }
                    OutlinedButton(
                        onClick = onRemoveClick,
                        shape = RoundedCornerShape(24.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350))
                    ) {
                        Text("Remove")
                    }
                }
            }
        } else {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Select audio file",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    "FLAC, WAV, or MP3 (max 200MB)",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onBrowseClick,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text("Browse Files")
                }
            }
        }
    }
}

// ─── Metadata Form ───
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MetadataForm(
    title: String,

    artist: String,
    artistId: String?,
    artistOptions: List<com.example.blesstify.domain.model.Artist>,

    album: String,
    albumId: String?,
    albumOptions: List<com.example.blesstify.domain.model.Album>,

    genres: List<String>,
    coverUri: android.net.Uri?,
    isPublic: Boolean,
    isLoading: Boolean,
    hasAudio: Boolean,
    onTitleChange: (String) -> Unit,

    onArtistChange: (String) -> Unit,
    onArtistSelected: (String?) -> Unit,

    onAlbumChange: (String) -> Unit,
    onAlbumSelected: (String?) -> Unit,

    onGenreToggle: (String) -> Unit,
    onPublicChange: (Boolean) -> Unit,
    onCoverClick: () -> Unit,
    onRemoveCover: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {

    Surface(
        color = Color.White.copy(0.05f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "Edit Metadata",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Cover art + Title
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Cover art selector
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(0.05f))
                        .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                        .clickable(enabled = !isLoading) { onCoverClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (coverUri != null) {
                        AsyncImage(
                            model = coverUri,
                            contentDescription = "Cover art",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        // Remove button overlay
                        IconButton(
                            onClick = onRemoveCover,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(Color.Black.copy(0.6f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove cover",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, null, tint = Color.Gray)
                            Text(
                                "Cover Art",
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Track Title *", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Enter track title", color = Color.Gray.copy(0.5f)) },
                        enabled = !isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── Artist selector ───
            Text("Artist *", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            var artistExpanded by remember { mutableStateOf(false) }
            val selectedArtistName = remember(artistId, artistOptions) {
                artistId?.let { id -> artistOptions.firstOrNull { it.id == id }?.name }
            }

            ExposedDropdownMenuBox(
                expanded = artistExpanded,
                onExpandedChange = { if (!isLoading) artistExpanded = !artistExpanded }
            ) {
                OutlinedTextField(
                    value = selectedArtistName ?: "",
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = artistExpanded) },
                    placeholder = { Text("Select existing artist (optional)", color = Color.Gray.copy(0.5f)) },
                    enabled = !isLoading
                )
                ExposedDropdownMenu(
                    expanded = artistExpanded,
                    onDismissRequest = { artistExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("(New artist)") },
                        onClick = {
                            artistExpanded = false
                            onArtistSelected(null)
                        }
                    )
                    artistOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt.name) },
                            onClick = {
                                artistExpanded = false
                                onArtistSelected(opt.id)
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = artist,
                onValueChange = onArtistChange,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Enter artist name", color = Color.Gray.copy(0.5f)) },
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ─── Album selector ───
            Text("Album", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            var albumExpanded by remember { mutableStateOf(false) }
            val selectedAlbumName = remember(albumId, albumOptions) {
                albumId?.let { id -> albumOptions.firstOrNull { it.id == id }?.name }
            }

            ExposedDropdownMenuBox(
                expanded = albumExpanded,
                onExpandedChange = { if (!isLoading) albumExpanded = !albumExpanded }
            ) {
                OutlinedTextField(
                    value = selectedAlbumName ?: "",
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = albumExpanded) },
                    placeholder = { Text("Select existing album (optional)", color = Color.Gray.copy(0.5f)) },
                    enabled = !isLoading
                )
                ExposedDropdownMenu(
                    expanded = albumExpanded,
                    onDismissRequest = { albumExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("(New album)") },
                        onClick = {
                            albumExpanded = false
                            onAlbumSelected(null)
                        }
                    )
                    albumOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt.name) },
                            onClick = {
                                albumExpanded = false
                                onAlbumSelected(opt.id)
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = album,
                onValueChange = onAlbumChange,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Album name", color = Color.Gray.copy(0.5f)) },
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Genres (Select one or more) *", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Stable alternative to FlowRow using chunked Rows
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uploadGenres.chunked(3).forEach { rowGenres ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowGenres.forEach { genreOption ->
                            val isSelected = genres.contains(genreOption)
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = isSelected,
                                onClick = { onGenreToggle(genreOption) },
                                label = { 
                                    Text(
                                        genreOption,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall
                                    ) 
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                    labelColor = Color.Gray,
                                    containerColor = Color.White.copy(0.05f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = Color.Gray.copy(0.3f),
                                    selectedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        // Fill remaining space if the row is not full
                        if (rowGenres.size < 3) {
                            repeat(3 - rowGenres.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Public", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Allow others to search and listen",
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Switch(
                    checked = isPublic,
                    onCheckedChange = { onPublicChange(it) },
                    enabled = !isLoading
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(56.dp),
                    enabled = !isLoading
                ) {
                    Text("Cancel", color = Color.White)
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading && hasAudio
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload")
                    }
                }
            }
        }
    }
}
