package com.example.blesstify.presentation.ui.splash

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SplashUiState(
    val brandName: String = "Blessify",
    val logoUrl: String = "https://lh3.googleusercontent.com/aida/ADBb0uiEJ3spdZ4waR39TyPN3AhQPnn_ftTnJ9R3Vvg8fHTkMdDoszmozZVXED8dvSndLgvh1Zz-Qwcq0w9J1eCMiFwrJB3k-BO_a85gOUw-xK3ZLEfyWty6m0_oaOTSo5PzvlFlut6Y31s0x3G-OKMWrwn6YZQIha0SRYHGoWHE09Os-VoZcFTqIKmEFK1grbESa-BTZkwV_2ulKXzMr8uPnKSnJJse8C131AtgvoTq-G0Y-0fZBf9wylxtJwE",
    val mainTitle: String = "Elevate your resonance",
    val description: String = "Experience AI-curated soundscapes that synchronize with your biological rhythm.",
    val nowPlayingTitle: String = "Ethereal Frequency 432Hz",
    val artworkUrl: String = "https://lh3.googleusercontent.com/aida-public/AB6AXuCH5QdM9mY82M3wbNZvunkyOfw5NdBEKoSnnZQBfxCFKUXvsNJvOckz7NhOCwmzr3jVjlEaLSkzvbUiQ20iU1zMy_Vomy07BS43NDLh-KHPpH2He6-3Zwo7AXr3wJqGsTFU0RCCB6pHuSDxv4UB7tX68pMOZ8EjYrJAdxaKf9t6M92lkIUNgPps8y0-NQDi8sTCALbLwk4QmPeIx_pCFQIXKodKdG-7NWlLgO6LRaTwIV8NXujJl2o3d53J4YgArfPhIqeapOXDeqM",
    val currentPageIndex: Int = 0,
    val pageCount: Int = 3
)

class SplashViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    fun onGetStartedClicked() {
        // Navigate to main screen or registration
    }

    fun onLoginClicked() {
        // Navigate to login screen
    }

    fun onPageChanged(index: Int) {
        _uiState.value = _uiState.value.copy(currentPageIndex = index)
    }
}
