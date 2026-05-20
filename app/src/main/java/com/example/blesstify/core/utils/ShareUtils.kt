package com.example.blesstify.core.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.blesstify.domain.model.Song

object ShareUtils {

    fun shareSong(context: Context, song: Song) {
        val text = buildString {
            append("Listening: ")
            append(song.title)
            if (song.artist.isNotBlank()) {
                append(" — ")
                append(song.artist)
            }
            append("\n")
            // If backend deep link exists later, replace.
            append("Song ID: ")
            append(song.id)
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, song.title)
        }

        val chooser = Intent.createChooser(intent, "Share song")
        ContextCompat.startActivity(context, chooser, null)
    }
}
