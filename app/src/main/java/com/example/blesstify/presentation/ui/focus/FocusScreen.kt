package com.example.blesstify.presentation.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.blesstify.presentation.ui.main.AddSongsFocusDialog
import com.example.blesstify.presentation.ui.main.FocusMusicSection
import com.example.blesstify.presentation.ui.main.FocusSettings
import com.example.blesstify.presentation.ui.main.TimerCircle
import com.example.blesstify.presentation.ui.main.TimerControls

@Composable
fun FocusScreen(
    viewModel: FocusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddSongDialog by remember { mutableStateOf(false) }

    if (showAddSongDialog) {
        AddSongsFocusDialog(
            searchQuery = uiState.searchQuery,
            searchResults = uiState.searchResults,
            recommendedSongs = uiState.recommendedSongs,
            isSearching = uiState.isSearching,
            addingSongId = uiState.addingSongId,
            onQueryChange = { viewModel.onSearchQueryChange(it) },
            onDismiss = { showAddSongDialog = false },
            onAdd = { song ->
                viewModel.addSongToFocus(song)
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Focus Mode", 
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                color = Color.White.copy(0.05f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimerCircle(
                        remainingTime = uiState.remainingTime,
                        progress = uiState.progress,
                        label = uiState.sessionLabel,
                        sessionInfo = uiState.sessionInfo
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    TimerControls(
                        isRunning = uiState.isRunning,
                        onToggle = { viewModel.toggleTimer() },
                        onReset = { viewModel.resetTimer() },
                        onAdd = { showAddSongDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            FocusSettings(
                workMin = uiState.workDurationMin,
                breakMin = uiState.breakDurationMin,
                onUpdateWork = { viewModel.updateWorkDuration(it) },
                onUpdateBreak = { viewModel.updateBreakDuration(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            FocusMusicSection(
                songs = uiState.focusSongs,
                currentSong = uiState.currentSong,
                onSelect = { viewModel.playSong(it) },
                onRemove = { viewModel.removeSongFromFocus(it) }
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
