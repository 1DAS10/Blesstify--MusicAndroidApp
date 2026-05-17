package com.example.blesstify.presentation.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.blesstify.domain.model.Song
import com.example.blesstify.domain.model.detailArtworkUrl
import com.example.blesstify.domain.model.listArtworkUrl

@Composable
fun ExploreScreen(
    onFeaturedClick: (Song) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onAIFocusClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            uiState.featuredSong?.let { song ->
                FeaturedSection(song = song, onClick = { onFeaturedClick(song) })
            }
            Spacer(modifier = Modifier.height(24.dp))
            AIFocusEntry(onClick = onAIFocusClick)
            Spacer(modifier = Modifier.height(32.dp))
            if (uiState.continueListeningPlaylists.isNotEmpty()) {
                PlaylistHorizontalSection(
                    title = "Continue Listening",
                    playlists = uiState.continueListeningPlaylists,
                    onPlaylistClick = onPlaylistClick
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
            if (uiState.recommendedSongs.isNotEmpty()) {
                VerticalListSection(
                    title = "Recommend for Today",
                    songs = uiState.recommendedSongs,
                    onSongClick = onSongClick
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
            Spacer(modifier = Modifier.height(100.dp)) // Bottom Nav padding
        }
    }
}

@Composable
fun FeaturedSection(song: Song, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2A3F5F), Color(0xFF0B0E13))
                )
            )
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = song.detailArtworkUrl(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.5f
        )
        Column(modifier = Modifier.padding(24.dp).align(Alignment.BottomStart)) {
            Surface(
                color = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("FEATURED", style = MaterialTheme.typography.labelSmall, color = Color.White, letterSpacing = 1.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(song.title, style = MaterialTheme.typography.displayLarge, color = Color.White, fontWeight = FontWeight.Bold, lineHeight = 44.sp, maxLines = 2)
            Text(song.artist, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun AIFocusEntry(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp)),
        color = Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Mood Generator", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text(
                "Pick a mood and generate a focus playlist.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text("Generate Playlist", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PlaylistHorizontalSection(
    title: String,
    playlists: List<com.example.blesstify.domain.model.Playlist>,
    onPlaylistClick: (String) -> Unit
) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(playlists) { playlist ->
                Column(modifier = Modifier.width(140.dp).clickable { onPlaylistClick(playlist.id) }) {
                    AsyncImage(
                        model = playlist.coverUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.DarkGray),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = playlist.title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp),
                        maxLines = 1,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${playlist.trackCount} tracks",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun VerticalListSection(title: String, songs: List<Song>, onSongClick: (Song) -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        songs.forEach { song ->
            TrackItem(song = song, onClick = { onSongClick(song) })
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun TrackItem(song: Song, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.listArtworkUrl(),
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.Gray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(song.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🔥 TRENDING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}
