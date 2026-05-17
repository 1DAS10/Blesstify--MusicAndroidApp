package com.example.blesstify.presentation.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.LocalImageLoader
import com.example.blesstify.util.ImageLoaderFactory

/**
 * Provides optimized ImageLoader to all AsyncImage components in the app
 * Wrap your app content with this composable
 */
@Composable
fun ImageLoaderProvider(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val imageLoader = ImageLoaderFactory.getImageLoader(context)

    CompositionLocalProvider(
        LocalImageLoader provides imageLoader,
        content = content
    )
}

/**
 * Helper defaults for image loading.
 */
object ImageLoadingDefaults {
    val SONG_COVER_CONTENT_SCALE: ContentScale = ContentScale.Crop
    val PROFILE_CONTENT_SCALE: ContentScale = ContentScale.Crop
    val BANNER_CONTENT_SCALE: ContentScale = ContentScale.FillBounds

    val PLACEHOLDER_COLOR = androidx.compose.ui.graphics.Color(0xFF2D2D2D)
    val ERROR_COLOR = androidx.compose.ui.graphics.Color(0xFF4A4A4A)
}
