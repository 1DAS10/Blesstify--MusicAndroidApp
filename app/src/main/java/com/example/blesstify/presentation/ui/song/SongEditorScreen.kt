package com.example.blesstify.presentation.ui.song

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongEditorScreen(
    songId: String,
    onBack: () -> Unit,
    viewModel: SongEditorViewModel = hiltViewModel()
) {
    val tag = "SongEditorUI"
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(songId) {
        Log.d(tag, "[load] launch songId=$songId")
        viewModel.load(songId)
    }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            Log.d(tag, "[coverPicker] result uri=$uri")
            if (uri != null) {
                // File name not always available; keep stable
                viewModel.onCoverSelected(uri, "cover_${System.currentTimeMillis()}.jpg")
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit song", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !uiState.isLoading) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.save() },
                        enabled = !uiState.isLoading && uiState.canSave
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0B0E13), Color(0xFF0E1220), Color(0xFF0B0E13))
                    )
                )
        ) {
            val scrollState = rememberScrollState()
            LaunchedEffect(scrollState.value) {
                Log.v(tag, "[scroll] y=${scrollState.value}")
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp)
                    .padding(bottom = 32.dp)
            ) {
                HeaderCard(title = uiState.originalTitle.ifBlank { "Song" })
                Spacer(modifier = Modifier.height(16.dp))
                MetadataCard(
                    uiState = uiState,
                    onTitleChange = viewModel::onTitleChange,
                    onArtistChange = viewModel::onArtistChange,
                    onArtistSelected = viewModel::onArtistSelected,
                    onAlbumChange = viewModel::onAlbumChange,
                    onAlbumSelected = viewModel::onAlbumSelected,
                    onGenreToggle = viewModel::onGenreToggle,
                    onPublicChange = viewModel::onPublicChange,
                    onPickCover = { if (!uiState.isLoading) coverPicker.launch("image/*") },
                    onRemoveCover = { if (!uiState.isLoading) viewModel.removeCoverSelection() },
                    onSave = viewModel::save,
                    onCancel = onBack
                )

                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiState.error ?: "",
                        color = Color(0xFFFF6B6B),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (uiState.didSave) {
                LaunchedEffect(Unit) {
                    Log.d(tag, "[nav] didSave=true -> onBack")
                    onBack()
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(title: String) {
    Surface(
        color = Color.White.copy(alpha = 0.04f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.07f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    text = "Update metadata & cover",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetadataCard(
    uiState: SongEditorUiState,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onArtistSelected: (String?) -> Unit,
    onAlbumChange: (String) -> Unit,
    onAlbumSelected: (String?) -> Unit,
    onGenreToggle: (String) -> Unit,
    onPublicChange: (Boolean) -> Unit,
    onPickCover: () -> Unit,
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
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(0.05f))
                        .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                        .clickable(enabled = !uiState.isLoading) { onPickCover() },
                    contentAlignment = Alignment.Center
                ) {
                    val coverModel: Any? = uiState.coverUri ?: uiState.existingCoverUrl
                    if (coverModel != null) {
                        AsyncImage(
                            model = coverModel,
                            contentDescription = "Cover art",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
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
                        value = uiState.title,
                        onValueChange = onTitleChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Enter track title", color = Color.Gray.copy(0.5f)) },
                        enabled = !uiState.isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Artist selector
            Text("Artist *", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            var artistExpanded by remember { mutableStateOf(false) }
            val selectedArtistName = remember(uiState.artistId, uiState.artistOptions) {
                uiState.artistId?.let { id -> uiState.artistOptions.firstOrNull { it.id == id }?.name }
            }

            ExposedDropdownMenuBox(
                expanded = artistExpanded,
                onExpandedChange = { if (!uiState.isLoading) artistExpanded = !artistExpanded }
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
                    enabled = !uiState.isLoading
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
                    uiState.artistOptions.forEach { opt ->
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
                value = uiState.artist,
                onValueChange = onArtistChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Enter artist name", color = Color.Gray.copy(0.5f)) },
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Album selector
            Text("Album", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            var albumExpanded by remember { mutableStateOf(false) }
            val selectedAlbumName = remember(uiState.albumId, uiState.albumOptions) {
                uiState.albumId?.let { id -> uiState.albumOptions.firstOrNull { it.id == id }?.name }
            }

            ExposedDropdownMenuBox(
                expanded = albumExpanded,
                onExpandedChange = { if (!uiState.isLoading) albumExpanded = !albumExpanded }
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
                    enabled = !uiState.isLoading
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
                    uiState.albumOptions.forEach { opt ->
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
                value = uiState.album,
                onValueChange = onAlbumChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Album name", color = Color.Gray.copy(0.5f)) },
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Genres (Select one or more) *", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SongEditorViewModel.allowedGenres.chunked(3).forEach { rowGenres ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowGenres.forEach { genreOption ->
                            val isSelected = uiState.genres.contains(genreOption)
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
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
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
                        if (rowGenres.size < 3) {
                            repeat(3 - rowGenres.size) { Spacer(modifier = Modifier.weight(1f)) }
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
                    checked = uiState.isPublic,
                    onCheckedChange = onPublicChange,
                    enabled = !uiState.isLoading
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    enabled = !uiState.isLoading
                ) {
                    Text("Cancel", color = Color.White)
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !uiState.isLoading && uiState.canSave
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
