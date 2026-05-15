package com.example.blesstify.data.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.example.blesstify.presentation.player.ExoPlayerMusicController
import com.example.blesstify.presentation.player.MusicController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun provideExoPlayer(@ApplicationContext context: Context): ExoPlayer {
        return ExoPlayer.Builder(context).build()
    }

    @Provides
    @Singleton
    fun provideMusicController(player: ExoPlayer): MusicController {
        return ExoPlayerMusicController(player)
    }
}
