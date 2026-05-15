package com.example.blesstify.domain.model

data class EqualizerSettings(
    val isEnabled: Boolean = false,
    val preset: String = "flat", // "custom", "flat", "bass_boost"
    val bands: List<Int> = listOf(0, 0, 0, 0, 0), // -12 to +12
    val virtualizer: Float = 0.0f, // 0.0 to 1.0
    val bassBoost: Float = 0.0f // 0.0 to 1.0
)
