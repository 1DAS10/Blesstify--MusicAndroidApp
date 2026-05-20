package com.example.blesstify.presentation.ui.library.edit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumEditorScreen(
    albumId: String?,
    viewModel: AlbumEditorViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val pickCoverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            viewModel.onLocalCoverSelected(uri)
        }
    )

    LaunchedEffect(albumId) { viewModel.load(albumId) }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (albumId.isNullOrBlank()) "Create Album" else "Edit Album") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.save(onSuccess = onSaved) },
                        enabled = !state.isSaving && !state.isLoading
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            Text(
                text = "Album info",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Text
                )
            )

            // ─────────────────────────────────────────────
            // Artists (searchable multi-select)
            // ─────────────────────────────────────────────
            Text(
                text = "Artists",
                style = MaterialTheme.typography.titleSmall
            )

            var artistsDialogOpen by remember { mutableStateOf(false) }

            val selectedArtists = remember(state.allArtists, state.selectedArtistIds) {
                state.allArtists.filter { state.selectedArtistIds.contains(it.id) }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !state.isSaving && !state.isLoading) { artistsDialogOpen = true }
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = if (selectedArtists.isEmpty()) "" else "${selectedArtists.size} selected",
                    onValueChange = { },
                    readOnly = true,
                    enabled = false,
                    label = { Text("Select artists") },
                    placeholder = { Text("Tap to search + select multiple") },
                    trailingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") }
                )
            }

            if (selectedArtists.isNotEmpty()) {
                val chipRows = remember(selectedArtists) { selectedArtists.chunked(3) }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chipRows.forEach { rowArtists ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowArtists.forEach { artist ->
                                InputChip(
                                    selected = true,
                                    onClick = { viewModel.toggleArtist(artist.id) },
                                    label = { Text(artist.name) }
                                )
                            }
                        }
                    }
                }
            }

            if (artistsDialogOpen) {
                var query by remember { mutableStateOf("") }

                val filtered = remember(query, state.allArtists) {
                    val q = query.trim().lowercase()
                    if (q.isBlank()) state.allArtists
                    else state.allArtists.filter { it.name.lowercase().contains(q) }
                }

                Dialog(onDismissRequest = { artistsDialogOpen = false }) {
                    androidx.compose.material3.Surface(
                        shape = MaterialTheme.shapes.large,
                        tonalElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Select artists", style = MaterialTheme.typography.titleMedium)

                            OutlinedTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                label = { Text("Search") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                            )

                            Box(modifier = Modifier.height(360.dp)) {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    items(filtered, key = { it.id }) { artist ->
                                        val checked = state.selectedArtistIds.contains(artist.id)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(MaterialTheme.shapes.medium)
                                                .clickable { viewModel.toggleArtist(artist.id) }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = checked,
                                                onCheckedChange = { viewModel.toggleArtist(artist.id) }
                                            )
                                            Text(
                                                text = artist.name,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { artistsDialogOpen = false }) {
                                    Text("Done")
                                }
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────
            // Cover
            // ─────────────────────────────────────────────
            Text(
                text = "Cover",
                style = MaterialTheme.typography.titleSmall
            )

            val previewModel = state.localCoverUri ?: state.coverUrl.takeIf { it.isNotBlank() }

            if (previewModel != null) {
                AsyncImage(
                    model = previewModel,
                    contentDescription = "Album cover preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { pickCoverLauncher.launch(arrayOf("image/*")) },
                    enabled = !state.isSaving && !state.isLoading
                ) {
                    Text("Choose image")
                }

                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.removeLocalCover() },
                    enabled = state.localCoverUri != null && !state.isSaving && !state.isLoading
                ) {
                    Text("Remove")
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save(onSuccess = onSaved) },
                enabled = !state.isSaving && !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Keep stable layout: reserve space for spinner so text never shifts
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                    } else {
                        Spacer(modifier = Modifier.size(28.dp))
                    }

                    Text(if (albumId.isNullOrBlank()) "Create" else "Save")

                    // symmetric spacer
                    Spacer(modifier = Modifier.size(28.dp))
                }
            }

            Text(
                text = "Note: Firestore document id equals albumId (slug from name on create).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
