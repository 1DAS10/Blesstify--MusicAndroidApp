package com.example.blesstify.presentation.ui.main

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.blesstify.domain.model.Song
import com.example.blesstify.domain.model.listArtworkUrl
import java.util.Locale
import androidx.compose.material.icons.filled.Edit

@Composable
fun SearchScreen(
    onNavigateToPlayer: (Song) -> Unit = {},
    onNavigateToPlayerFromSearch: (List<Song>, Int) -> Unit = { _, _ -> },
    onNavigateToCategory: (String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    
    var isListening by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    // Create SpeechRecognizer instance
    val speechRecognizer = androidx.compose.runtime.remember {
        SpeechRecognizer.createSpeechRecognizer(context)
    }

    // Recognition listener
    val recognitionListener = androidx.compose.runtime.remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("MicSearch", "onReadyForSpeech")
                isListening = true
            }

            override fun onBeginningOfSpeech() {
                Log.d("MicSearch", "onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changed
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Partial audio buffer
            }

            override fun onEndOfSpeech() {
                Log.d("MicSearch", "onEndOfSpeech")
                isListening = false
            }

            override fun onError(error: Int) {
                Log.e("MicSearch", "onError: error code=$error")
                isListening = false
                
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Lỗi ghi âm"
                    SpeechRecognizer.ERROR_CLIENT -> "Lỗi client"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Chưa cấp quyền microphone"
                    SpeechRecognizer.ERROR_NETWORK -> "Lỗi mạng"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Hết thời gian kết nối"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Không nhận diện được giọng nói"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Đang bận, thử lại"
                    SpeechRecognizer.ERROR_SERVER -> "Lỗi server"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Không nghe thấy giọng nói"
                    else -> "Lỗi không xác định ($error)"
                }
                
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                Log.d("MicSearch", "onResults")
                isListening = false
                
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.d("MicSearch", "speech matches count=${matches?.size ?: 0}")
                
                val spokenText = matches?.firstOrNull()?.trim().orEmpty()
                if (spokenText.isNotBlank()) {
                    Log.d("MicSearch", "spokenText=$spokenText")
                    viewModel.onQueryChange(spokenText)
                    viewModel.onSearchSubmit()
                    keyboardController?.hide()
                } else {
                    Log.w("MicSearch", "spoken text blank")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Partial recognition results
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Reserved for future events
            }
        }
    }

    // Set listener
    androidx.compose.runtime.DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        
        onDispose {
            Log.d("MicSearch", "Disposing SpeechRecognizer")
            try {
                speechRecognizer.stopListening()
                speechRecognizer.cancel()
                speechRecognizer.destroy()
            } catch (e: Exception) {
                Log.e("MicSearch", "Error disposing SpeechRecognizer", e)
            }
        }
    }

    fun startSpeechRecognition() {
        try {
            // Check if speech recognition is available
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.e("MicSearch", "Speech recognition not available on device")
                Toast.makeText(
                    context,
                    "Vui lòng dùng nút mic trên bàn phím để tìm kiếm bằng giọng nói",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            Log.d("MicSearch", "Starting speech recognition")
            
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer.startListening(intent)
            isListening = true
            
        } catch (e: Exception) {
            Log.e("MicSearch", "Error starting speech recognition", e)
            isListening = false
            Toast.makeText(
                context,
                "Không thể bắt đầu nhận diện giọng nói",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d("MicSearch", "mic permission granted=$granted")
        if (granted) {
            startSpeechRecognition()
        } else {
            Log.w("MicSearch", "mic permission denied")
            Toast.makeText(
                context,
                "Cần quyền microphone để tìm kiếm bằng giọng nói",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun launchVoiceSearch() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        Log.d("MicSearch", "launchVoiceSearch hasPermission=$hasPermission")
        if (hasPermission) {
            startSpeechRecognition()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Search Bar
        OutlinedTextField(
            value = uiState.query,
            onValueChange = { viewModel.onQueryChange(it) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            placeholder = { Text("What do you want to listen to?", color = Color.Gray) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                if (uiState.query.isNotBlank()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                    }
                } else {
                    IconButton(onClick = { launchVoiceSearch() }) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice search",
                            tint = Color.Gray
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    viewModel.onSearchSubmit()
                    keyboardController?.hide()
                }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Recent searches show between search bar and Categories (default browse mode)
        if (uiState.query.isBlank() && uiState.recentSearches.isNotEmpty()) {
            RecentSearchesSection(
                searches = uiState.recentSearches,
                onClearAll = { viewModel.onClearAllHistory() },
                onSearchClick = { viewModel.onRecentSearchClick(it) },
                onRemoveSearch = { viewModel.onDeleteRecentSearch(it) }
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        if (uiState.query.isNotBlank()) {
            // ─── Search Results Mode ───
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.error != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.MusicNote,
                            null,
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Error: ${uiState.error}",
                            color = Color(0xFFFF6B6B),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else if (uiState.hasSearched && uiState.results.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.MusicNote,
                            null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No results found for \"${uiState.query}\"",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else if (uiState.results.isNotEmpty()) {
                Text(
                    "Results (${uiState.results.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(uiState.results) { index, song ->
                        SongResultItem(
                            song = song,
                            onClick = {
                                // treat as explicit search action -> save history
                                viewModel.onSearchSubmit()
                                onNavigateToPlayerFromSearch(uiState.results, index)
                            },
                            onAddToQueue = { viewModel.addSongToQueue(it) },
                            onPlayNext = { viewModel.playSongNext(it) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        } else {
            // ─── Default Browse Mode ───
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        "Categories",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                val categoryColors = listOf(
                    Color(0xFF2A3F5F), Color(0xFF3E2A5F), Color(0xFF2A5F45),
                    Color(0xFF5F452A), Color(0xFF5F2A2A), Color(0xFF454545)
                )

                itemsIndexed(uiState.browseCategories) { index, category ->
                    val color = categoryColors[index % categoryColors.size]
                    CategoryCard(
                        name = category.name,
                        colors = listOf(color, Color(0xFF0B0E13)),
                        onClick = { onNavigateToCategory(category.name) }
                    )
                }

                item(span = { GridItemSpan(2) }) { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
fun SongResultItem(
    song: Song,
    onClick: () -> Unit,
    onAddToQueue: ((Song) -> Unit)? = null,
    onPlayNext: ((Song) -> Unit)? = null,
    onEditSong: ((Song) -> Unit)? = null
) {
    var showMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Surface(
        onClick = onClick,
        color = Color.White.copy(0.05f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ) {
                val artworkUrl = song.listArtworkUrl()
                if (!artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = "Cover art",
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(12.dp).size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (onAddToQueue != null && onPlayNext != null) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More actions",
                            tint = Color.White.copy(0.85f)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF1A1D23))
                    ) {
                        if (onEditSong != null) {
                            DropdownMenuItem(
                                text = { Text("Edit info", color = Color.White) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        null,
                                        tint = Color.Gray
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onEditSong(song)
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text("Add to queue", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.QueueMusic, null, tint = Color.Gray) },
                            onClick = {
                                showMenu = false
                                onAddToQueue(song)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Play next", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.SkipNext, null, tint = Color.Gray) },
                            onClick = {
                                showMenu = false
                                onPlayNext(song)
                            }
                        )
                    }
                }
            } else {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun CategorySection(
    category: com.example.blesstify.presentation.ui.main.BrowseCategory,
    onSongClick: (Song) -> Unit
) {
    Column {
        Text(
            text = category.name,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        if (category.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        } else if (category.songs.isEmpty()) {
            Text("No songs in this category", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(category.songs) { song ->
                    CategorySongItem(song = song, onClick = { onSongClick(song) })
                }
            }
        }
    }
}

@Composable
fun CategorySongItem(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(0.05f))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        // Simple cover placeholder/icon
        Surface(
            modifier = Modifier.size(116.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(0.2f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(32.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(song.artist, color = Color.Gray, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RecentSearchesSection(
    searches: List<com.example.blesstify.domain.model.SearchHistory>,
    onClearAll: () -> Unit,
    onSearchClick: (String) -> Unit,
    onRemoveSearch: (String) -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.07f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recent",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = onClearAll, contentPadding = PaddingValues(0.dp)) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = "Clear all",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear", color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(searches) { search ->
                    InputChip(
                        selected = false,
                        onClick = { onSearchClick(search.query) },
                        label = {
                            Text(
                                search.query,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { onRemoveSearch(search.id) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = Color.White
                        ),
                        border = null,
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryCard(name: String, colors: List<Color>, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(brush = Brush.verticalGradient(colors))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(
            name,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}
