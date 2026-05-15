package com.example.blesstify.presentation.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.domain.model.Song
import com.example.blesstify.domain.usecase.GetCurrentUserUseCase
import com.example.blesstify.domain.usecase.GetPublicSongsUseCase
import com.example.blesstify.domain.usecase.GetSongsByGenreUseCase
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.core.error.AppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    private val getSongsByGenreUseCase: GetSongsByGenreUseCase,
    private val getPublicSongsUseCase: GetPublicSongsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {
    private val _songs = MutableStateFlow<Resource<List<Song>>>(Resource.Loading<List<Song>>())
    val songs: StateFlow<Resource<List<Song>>> = _songs

    private val otherCategoryName = "Other"
    private val otherCategoryFilter = setOf(
        "Pop", "Rock", "Hip Hop", "Rap", "EDM", "Electronic", "Jazz", "Blues",
        "Classical", "R&B", "Country", "Folk", "Metal", "Reggae", "K-pop", "J-pop",
        "C-pop", "Latin", "Phonk", "Lo-fi"
    )

    fun loadSongs(genre: String) {
        val currentUserId = getCurrentUserUseCase()?.id
        viewModelScope.launch {
            if (genre == otherCategoryName) {
                getPublicSongsUseCase(limit = 50).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            val filteredSongs = (result.data ?: emptyList()).filter { song ->
                                song.genre.isEmpty() || song.genre.none { it in otherCategoryFilter }
                            }
                            _songs.value = Resource.Success(filteredSongs)
                        }
                        is Resource.Error -> {
                            _songs.value = Resource.Error(
                                result.error ?: AppError.Unknown("Unknown error"),
                                result.data
                            )
                        }
                        is Resource.Loading -> {
                            _songs.value = Resource.Loading(result.data)
                        }
                    }
                }
            } else {
                getSongsByGenreUseCase(genre, currentUserId).collectLatest { result ->
                    _songs.value = result
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    genre: String,
    onBack: () -> Unit,
    onNavigateToPlayer: (List<Song>, Int) -> Unit,
    viewModel: CategoryDetailViewModel = hiltViewModel()
) {
    val songsState by viewModel.songs.collectAsState()

    LaunchedEffect(genre) {
        viewModel.loadSongs(genre)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(genre, color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                .padding(horizontal = 16.dp)
        ) {
            when (val resource = songsState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is Resource.Success -> {
                    val songs = resource.data ?: emptyList()
                    if (songs.isEmpty()) {
                        Text(
                            "No songs found in this category",
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            items(songs) { song ->
                                SongResultItem(
                                    song = song,
                                    onClick = {
                                        val index = songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                                        onNavigateToPlayer(songs, index)
                                    }
                                )
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Text(
                        "Error: ${resource.error?.message ?: "Unknown error"}",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
