package com.example.blesstify.presentation.player

sealed interface PlayerUiEvent {
    data class Message(val text: String) : PlayerUiEvent
}
