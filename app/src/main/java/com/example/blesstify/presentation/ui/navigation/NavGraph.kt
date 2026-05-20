package com.example.blesstify.presentation.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Main : Screen("main")
    object Player : Screen("player")
    object PlaylistDetail : Screen("playlist_detail/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist_detail/$playlistId"
    }
    object CreatePlaylist : Screen("create_playlist")
    object AIFocus : Screen("ai_focus")
    object Equalizer : Screen("equalizer")
    object UploadMusic : Screen("upload_music")
    object History : Screen("history")
    object Notification : Screen("notification")
    object CategoryDetail : Screen("category_detail/{genre}") {
        fun createRoute(genre: String) = "category_detail/${Uri.encode(genre)}"
    }
    object AlbumDetail : Screen("album_detail/{albumId}") {
        fun createRoute(albumId: String) = "album_detail/${Uri.encode(albumId)}"
    }
    object ArtistDetail : Screen("artist_detail/{artistId}") {
        fun createRoute(artistId: String) = "artist_detail/${Uri.encode(artistId)}"
    }

    object AlbumEditor : Screen("album_editor?albumId={albumId}") {
        fun createRoute(albumId: String? = null): String =
            if (albumId.isNullOrBlank()) "album_editor" else "album_editor?albumId=${Uri.encode(albumId)}"
    }

    object ArtistEditor : Screen("artist_editor?artistId={artistId}") {
        fun createRoute(artistId: String? = null): String =
            if (artistId.isNullOrBlank()) "artist_editor" else "artist_editor?artistId=${Uri.encode(artistId)}"
    }
}

sealed class MainTab(val route: String, val label: String) {
    object Explore : MainTab("explore", "EXPLORE")
    object Search : MainTab("search", "SEARCH")
    object Focus : MainTab("focus", "FOCUS")
    object Library : MainTab("library", "LIBRARY")
    object Profile : MainTab("profile", "PROFILE")
}
