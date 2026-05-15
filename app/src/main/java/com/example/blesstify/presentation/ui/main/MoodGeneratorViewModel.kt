package com.example.blesstify.presentation.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.domain.auth.AuthState
import com.example.blesstify.domain.model.Song
import com.example.blesstify.domain.usecase.GenerateMoodPlaylistUseCase
import com.example.blesstify.domain.usecase.ObserveAuthStateUseCase
import com.example.blesstify.presentation.player.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoodGeneratorViewModel @Inject constructor(
    private val generateMoodPlaylistUseCase: GenerateMoodPlaylistUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val musicController: MusicController
) : ViewModel() {

    private val _selectedMood = MutableStateFlow("Focused")
    val selectedMood = _selectedMood.asStateFlow()

    private val _energyLevel = MutableStateFlow(0.4f)
    val energyLevel = _energyLevel.asStateFlow()

    private val _generatedPlaylist =
        MutableStateFlow<Resource<List<Song>>>(Resource.Success(emptyList()))
    val generatedPlaylist = _generatedPlaylist.asStateFlow()

    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().collectLatest { state ->
                currentUserId = if (state is AuthState.Authenticated) state.userId else null
            }
        }
    }

    fun setSelectedMood(mood: String) {
        _selectedMood.value = mood
    }

    fun setEnergyLevel(level: Float) {
        _energyLevel.value = level
    }

    fun generatePlaylist() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            generateMoodPlaylistUseCase(userId, _selectedMood.value, _energyLevel.value.toDouble()).collect { resource ->
                _generatedPlaylist.value = resource
            }
        }
    }

    fun playAll() {
        val songs = (_generatedPlaylist.value as? Resource.Success<*>)?.data as? List<Song> ?: return
        if (songs.isNotEmpty()) {
            musicController.setPlaylist(
                songs = songs,
                startIndex = 0,
                playlistId = "ai_mood_${_selectedMood.value.lowercase()}",
                source = "ai_mood_generator"
            )
        }
    }
}