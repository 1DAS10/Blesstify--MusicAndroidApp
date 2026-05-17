package com.example.blesstify.presentation.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.domain.model.Song
import com.example.blesstify.domain.model.listArtworkUrl
import com.example.blesstify.presentation.ui.main.MoodGeneratorViewModel

enum class FocusState { Selection, Timer, Session }

data class Mood(val name: String, val icon: String)

@Composable
fun AIFocusScreen(
    onBack: () -> Unit,
    viewModel: MoodGeneratorViewModel = hiltViewModel()
) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AIFocusHome(onBack = onBack, viewModel = viewModel)
    }
}

@Composable
fun AIFocusHome(
    onBack: () -> Unit,
    viewModel: MoodGeneratorViewModel
) {
    val moods = listOf(
        Mood("Focused", "🧘"), Mood("Energetic", "⚡"), Mood("Melancholic", "🌧️"),
        Mood("Hopeful", "🌅"), Mood("Aggressive", "🔥"), Mood("Chill", "☕"),
        Mood("Euphoric", "✨"), Mood("Groovy", "🪩"), Mood("Ambient", "🌌")
    )
    val selectedMood by viewModel.selectedMood.collectAsState()
    val energyLevel by viewModel.energyLevel.collectAsState()
    val generatedPlaylist by viewModel.generatedPlaylist.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Mood Generator", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "How are you\nfeeling?",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Let our AI craft the perfect auditory atmosphere for your current state of mind.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
            )

            Surface(
                color = Color.White.copy(0.05f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color.White.copy(0.1f))
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.padding(16.dp).height(280.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(moods) { mood ->
                        MoodItem(
                            mood = mood,
                            isSelected = selectedMood == mood.name,
                            onSelect = { viewModel.setSelectedMood(mood.name) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                color = Color.White.copy(0.05f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color.White.copy(0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("CALM", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("ENERGY LEVEL", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        Text("ENERGETIC", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Slider(
                        value = energyLevel,
                        onValueChange = { viewModel.setEnergyLevel(it) },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(0.1f)
                        )
                    )
                    Button(
                        onClick = { viewModel.generatePlaylist() },
                        enabled = generatedPlaylist !is Resource.Loading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        if (generatedPlaylist is Resource.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        } else {
                            Icon(Icons.Default.AutoAwesome, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Playlist", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val resource = generatedPlaylist) {
                is Resource.Success -> {
                    val songs = resource.data ?: emptyList()
                    if (songs.isNotEmpty()) {
                        AIPickedCard(
                            title = selectedMood,
                            subtitle = "Curated for your $selectedMood state with ${String.format("%.1f", energyLevel)} energy level.",
                            songs = songs,
                            onPlayAll = { viewModel.playAll() }
                        )
                    }
                }
                is Resource.Error -> {
                    Text("Failed to generate: ${resource.error?.message}", color = MaterialTheme.colorScheme.error)
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun TimerCircle(
    remainingTime: String,
    progress: Float,
    label: String,
    sessionInfo: String
) {
    Surface(
        modifier = Modifier.size(280.dp),
        color = Color.White.copy(0.05f),
        shape = CircleShape,
        border = BorderStroke(2.dp, Color.White.copy(0.1f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.size(240.dp),
                strokeWidth = 8.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(0.05f)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(color = Color.White.copy(0.1f), shape = RoundedCornerShape(20.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.White))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(remainingTime, style = MaterialTheme.typography.displayLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Text(sessionInfo, color = Color.Gray, style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
fun TimerControls(
    isRunning: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onAdd: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        IconButton(onClick = onReset) {
            Icon(Icons.Default.Refresh, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
        }
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(88.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        ) {
            Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        IconButton(onClick = onAdd) {
            Icon(Icons.Default.Add, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun FocusSettings(
    workMin: Int,
    breakMin: Int,
    onUpdateWork: (Int) -> Unit,
    onUpdateBreak: (Int) -> Unit
) {
    Surface(
        color = Color.White.copy(0.05f),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, null, tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Settings", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
            SettingSlider("Work Duration", "$workMin min", workMin.toFloat() / 60f) { onUpdateWork((it * 60).toInt().coerceIn(5, 60)) }
            Spacer(modifier = Modifier.height(24.dp))
            SettingSlider("Short Break", "$breakMin min", breakMin.toFloat() / 30f) { onUpdateBreak((it * 30).toInt().coerceIn(1, 30)) }
        }
    }
}

@Composable
fun SettingSlider(label: String, value: String, progress: Float, onValueChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text(value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = progress,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(0.05f)
            )
        )
    }
}

@Composable
fun FocusMusicSection(
    songs: List<Song>,
    currentSong: Song?,
    onSelect: (Song) -> Unit,
    onRemove: (Song) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Headphones, null, tint = Color.White)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Focus Music", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Curated soundscapes for deep concentration.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(16.dp))
        songs.forEach { song ->
            FocusMusicItem(
                song = song,
                isSelected = currentSong?.id == song.id,
                onSelect = { onSelect(song) },
                onRemove = { onRemove(song) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun FocusMusicItem(
    song: Song,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        onClick = onSelect,
        color = Color.White.copy(if (isSelected) 0.1f else 0.05f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(0.5f) else Color.White.copy(0.1f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = song.listArtworkUrl(),
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(song.artist, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.RemoveCircleOutline, contentDescription = "Remove", tint = Color(0xFFFF6B6B))
            }
            if (isSelected) {
                Icon(Icons.Default.BarChart, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun FocusSessionView(onEndSession: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(48.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dynamic Flow", style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            }
            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowChip("Working", true)
                FlowChip("Running")
                FlowChip("Relaxing")
            }

            Spacer(modifier = Modifier.height(48.dp))

            SessionNowPlaying()

            Spacer(modifier = Modifier.height(48.dp))

            Text("Up Next", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            SessionTrackItem("Synaptic Drift", "Neon Vectors", "4:05")
            Spacer(modifier = Modifier.height(16.dp))
            SessionTrackItem("Quantum Echo", "The Aurora Project", "5:12")
            Spacer(modifier = Modifier.height(16.dp))
            SessionTrackItem("Deep State", "Vocalion", "3:45")

            Spacer(modifier = Modifier.height(32.dp))
            TextButton(onClick = onEndSession, modifier = Modifier.fillMaxWidth()) {
                Text("End Session", color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun FlowChip(text: String, isSelected: Boolean = false) {
    Surface(
        color = if (isSelected) Color.White.copy(0.1f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isSelected) Color.White else Color.White.copy(0.2f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Icon(Icons.Default.Work, null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SessionNowPlaying() {
    Surface(
        color = Color.White.copy(0.05f),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(240.dp).clip(RoundedCornerShape(24.dp)).background(Color.DarkGray))
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Midnight Reverie", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("The Aurora Project", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
                }
                Surface(color = Color.White.copy(0.1f), shape = RoundedCornerShape(12.dp)) {
                    Text("HI-RES", color = Color.Gray, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("2:14", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    IconButton(onClick = {}) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White) }
                    IconButton(onClick = {}, modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) {
                        Icon(Icons.Default.Pause, null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = {}) { Icon(Icons.Default.SkipNext, null, tint = Color.White) }
                }
                Text("-3:42", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun SessionTrackItem(title: String, artist: String, duration: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(artist, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        Text(duration, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.width(16.dp))
        Icon(Icons.Default.AutoAwesome, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
    }
}

@Composable
fun MoodItem(mood: Mood, isSelected: Boolean, onSelect: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(0.2f) else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(mood.icon, fontSize = 24.sp)
        Text(mood.name, style = MaterialTheme.typography.labelSmall, color = if (isSelected) Color.White else Color.Gray)
    }
}

@Composable
fun AIPickedCard(title: String, subtitle: String, songs: List<Song>, onPlayAll: () -> Unit) {
    Surface(
        color = Color.White.copy(0.05f),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            for (song in songs) {
                SongTrackItem(song)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = onPlayAll,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text("Play All", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SongTrackItem(song: Song) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        AsyncImage(
            model = song.listArtworkUrl(),
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(song.artist, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        val duration = "${song.durationSec / 60}:${String.format("%02d", song.durationSec % 60)}"
        Text(duration, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun AddSongsFocusDialog(
    searchQuery: String,
    searchResults: List<Song>,
    recommendedSongs: List<Song>,
    isSearching: Boolean,
    addingSongId: String?,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onAdd: (Song) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.8f),
            color = Color(0xFF121417),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color.White.copy(0.1f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Add Focus Music", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search songs...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(0.05f),
                        unfocusedContainerColor = Color.White.copy(0.05f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                val displaySongs = if (searchQuery.isEmpty()) recommendedSongs else searchResults
                val title = if (searchQuery.isEmpty()) "Recommended" else "Results"

                Text(title, style = MaterialTheme.typography.labelMedium, color = Color.Gray, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))

                if (isSearching) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(displaySongs) { song ->
                            FocusSearchItem(
                                song = song, 
                                isAdding = addingSongId == song.id, 
                                onAdd = { onAdd(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FocusSearchItem(song: Song, isAdding: Boolean, onAdd: () -> Unit) {
    Surface(
        color = Color.White.copy(0.05f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = song.listArtworkUrl(),
                contentDescription = null,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(Color.DarkGray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = Color.Gray, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(
                onClick = onAdd,
                enabled = !isAdding,
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(0.15f), CircleShape)
            ) {
                if (isAdding) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}


