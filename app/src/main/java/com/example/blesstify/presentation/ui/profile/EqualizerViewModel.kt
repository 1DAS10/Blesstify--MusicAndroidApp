package com.example.blesstify.presentation.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blesstify.core.utils.Resource
import com.example.blesstify.domain.auth.AuthState
import com.example.blesstify.domain.model.EqualizerSettings
import com.example.blesstify.domain.usecase.GetEqualizerSettingsUseCase
import com.example.blesstify.domain.usecase.ObserveAuthStateUseCase
import com.example.blesstify.domain.usecase.SaveEqualizerSettingsUseCase
import com.example.blesstify.domain.user.UserResult
import com.example.blesstify.presentation.player.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val getEqualizerSettingsUseCase: GetEqualizerSettingsUseCase,
    private val saveEqualizerSettingsUseCase: SaveEqualizerSettingsUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val musicController: MusicController
) : ViewModel() {

    private val _settings = MutableStateFlow(EqualizerSettings())
    val settings = _settings.asStateFlow()

    private var currentUserId: String? = null
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().collectLatest { state ->
                if (state is AuthState.Authenticated) {
                    currentUserId = state.userId
                    loadSettings(state.userId)
                }
            }
        }
    }

    private fun loadSettings(userId: String) {
        viewModelScope.launch {
            val result = getEqualizerSettingsUseCase(userId)
            if (result is UserResult.Success && result.data != null) {
                _settings.value = result.data
                musicController.applyEqualizer(result.data)
            }
        }
    }

    fun toggleEqualizer(enabled: Boolean) {
        updateSettings(_settings.value.copy(isEnabled = enabled))
    }

    fun setPreset(preset: String) {
        val newBands = when (preset) {
            "flat" -> listOf(0, 0, 0, 0, 0)
            "bass_boost" -> listOf(6, 4, 0, 0, 0)
            "rock" -> listOf(5, 3, -1, 3, 5)
            "pop" -> listOf(-1, 3, 5, 3, -1)
            "jazz" -> listOf(3, 0, 1, 2, 4)
            "vocal" -> listOf(-2, 0, 4, 3, 1)
            "classical" -> listOf(4, 2, 0, 2, 4)
            else -> _settings.value.bands
        }
        updateSettings(_settings.value.copy(preset = preset, bands = newBands))
    }

    fun updateBand(index: Int, level: Int) {
        val newBands = _settings.value.bands.toMutableList()
        newBands[index] = level
        updateSettings(_settings.value.copy(bands = newBands, preset = "custom"))
    }

    fun setVirtualizer(level: Float) {
        updateSettings(_settings.value.copy(virtualizer = level))
    }

    fun setBassBoost(level: Float) {
        updateSettings(_settings.value.copy(bassBoost = level))
    }

    private fun updateSettings(newSettings: EqualizerSettings) {
        _settings.value = newSettings
        if (newSettings.isEnabled) {
            musicController.applyEqualizer(newSettings)
        } else {
            musicController.applyEqualizer(newSettings.copy(isEnabled = false))
        }
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500) // Debounce
            val userId = currentUserId ?: return@launch
            saveEqualizerSettingsUseCase(userId, _settings.value)
        }
    }
}
