package com.example.blesstify.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.blesstify.domain.model.Song
import java.io.ByteArrayOutputStream

object SongImageUtils {
    private const val THUMBNAIL_MAX_SIZE_PX = 320
    private const val THUMBNAIL_JPEG_QUALITY = 75

    fun Song.listArtworkUrl(): String? = thumbnailUrl?.takeIf { it.isNotBlank() }
        ?: coverUrl?.takeIf { it.isNotBlank() }

    fun createThumbnailBytes(context: Context, sourceUri: Uri): ByteArray? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOptions)
            }

            val sourceWidth = boundsOptions.outWidth
            val sourceHeight = boundsOptions.outHeight
            if (sourceWidth <= 0 || sourceHeight <= 0) return null

            val sampleSize = calculateInSampleSize(sourceWidth, sourceHeight, THUMBNAIL_MAX_SIZE_PX)
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return null

            val scaled = scaleToMaxSize(decoded, THUMBNAIL_MAX_SIZE_PX)
            ByteArrayOutputStream().use { output ->
                scaled.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_JPEG_QUALITY, output)
                output.toByteArray()
            }.also {
                if (scaled !== decoded) scaled.recycle()
                decoded.recycle()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxSize: Int): Int {
        var inSampleSize = 1
        var halfWidth = width / 2
        var halfHeight = height / 2
        while (halfWidth / inSampleSize >= maxSize && halfHeight / inSampleSize >= maxSize) {
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun scaleToMaxSize(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val largestSide = maxOf(width, height)
        if (largestSide <= maxSize) return bitmap

        val scale = maxSize.toFloat() / largestSide.toFloat()
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}
