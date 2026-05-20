package com.example.blesstify.presentation.ui.library.edit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
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

            Text(
                text = "Artists",
                style = MaterialTheme.typography.titleSmall
            )

            if (state.allArtists.isEmpty()) {
                Text(
                    text = "No artists yet. Create artists first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.allArtists.forEach { artist ->
                        val checked = state.selectedArtistIds.contains(artist.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .padding(vertical = 2.dp),
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

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.coverUrl,
                onValueChange = viewModel::onCoverUrlChange,
                label = { Text("Cover URL (optional)") },
                placeholder = { Text("https://...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Uri
                )
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save(onSuccess = onSaved) },
                enabled = !state.isSaving && !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(18.dp))
                    Spacer(Modifier.height(0.dp))
                }
                Text(if (albumId.isNullOrBlank()) "Create" else "Save")
            }

            Text(
                text = "Note: Firestore document id equals albumId (slug from name on create).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
