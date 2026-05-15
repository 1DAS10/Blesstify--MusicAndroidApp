package com.example.blesstify.presentation.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun EqualizerScreen(
    onBack: () -> Unit,
    viewModel: EqualizerViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val isEqualizerOn = settings.isEnabled
    val selectedPreset = settings.preset
    val virtualizerLevel = settings.virtualizer
    val bassBoostLevel = settings.bassBoost
    val bands = settings.bands

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ChevronLeft, null, tint = Color.White)
            }
            Text("Blessify", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            IconButton(onClick = {}) {
                Icon(Icons.Default.SettingsBackupRestore, null, tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Equalizer", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold)
        Text("Fine-tune your high-fidelity experience.", color = Color.Gray, modifier = Modifier.padding(top = 6.dp))

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            color = Color.White.copy(0.05f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.08f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Equalizer ${if (isEqualizerOn) "ON" else "OFF"}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = isEqualizerOn, onCheckedChange = { viewModel.toggleEqualizer(it) })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val allPresets = listOf("custom", "flat", "bass_boost", "rock", "pop", "jazz", "vocal", "classical")
        val firstRow = allPresets.take(4)
        val secondRow = allPresets.drop(4)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            firstRow.forEach { preset ->
                PresetChip(
                    text = preset.replace("_", " ").uppercase(),
                    isSelected = selectedPreset == preset,
                    onClick = { viewModel.setPreset(preset) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            secondRow.forEach { preset ->
                PresetChip(
                    text = preset.replace("_", " ").uppercase(),
                    isSelected = selectedPreset == preset,
                    onClick = { viewModel.setPreset(preset) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth().height(280.dp),
            color = Color.White.copy(0.05f),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f))
        ) {
            Column(modifier = Modifier.padding(top = 24.dp, bottom = 12.dp, start = 12.dp, end = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bands.forEachIndexed { index, level ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxHeight().width(50.dp)
                        ) {
                            Text(
                                text = if (level > 0) "+$level" else "$level",
                                color = if (isEqualizerOn) MaterialTheme.colorScheme.primary else Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Slider(
                                    value = level.toFloat(),
                                    onValueChange = { viewModel.updateBand(index, it.toInt()) },
                                    valueRange = -12f..12f,
                                    steps = 23,
                                    modifier = Modifier
                                        .width(160.dp) // The "height" of the slider when rotated
                                        .graphicsLayer {
                                            rotationZ = -90f
                                        },
                                    enabled = isEqualizerOn,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = Color.White.copy(0.1f)
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val label = listOf("60", "230", "910", "3.6k", "14k")[index]
                            Text(label, color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        EffectSlider(
            icon = Icons.Default.SurroundSound,
            label = "Virtualizer",
            value = virtualizerLevel,
            onValueChange = { viewModel.setVirtualizer(it) },
            percentage = "${(virtualizerLevel * 100).toInt()}%",
            enabled = isEqualizerOn
        )
        Spacer(modifier = Modifier.height(16.dp))
        EffectSlider(
            icon = Icons.Default.Speaker,
            label = "Bass Boost",
            value = bassBoostLevel,
            onValueChange = { viewModel.setBassBoost(it) },
            percentage = "${(bassBoostLevel * 100).toInt()}%",
            enabled = isEqualizerOn
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun PresetChip(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White.copy(0.05f),
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.White
        )
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
fun EffectSlider(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: Float, onValueChange: (Float) -> Unit, percentage: String, enabled: Boolean = true) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = if (enabled) Color.White else Color.Gray, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, color = if (enabled) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
            }
            Text(percentage, color = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(0.1f)
            )
        )
    }
}
