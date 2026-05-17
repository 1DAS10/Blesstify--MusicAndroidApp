package com.example.blesstify.util

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Factory for creating optimized Coil ImageLoader
 * Provides disk caching, memory caching, and network optimization
 */
object ImageLoaderFactory {
    
    private var imageLoader: ImageLoader? = null
    
    /**
     * Get or create singleton ImageLoader instance
     */
    fun getImageLoader(context: Context): ImageLoader {
        return imageLoader ?: createImageLoader(context).also { imageLoader = it }
    }
    
    private fun createImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            // Memory Cache Configuration
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // Use 25% of app memory for image cache
                    .build()
            }
            // Disk Cache Configuration
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100 MB disk cache
                    .build()
            }
            // Network Configuration
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build()
            }
            // Cache Policies
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            // Enable crossfade animation
            .crossfade(true)
            .crossfade(300) // 300ms crossfade
            // Keep Firebase Storage token URLs in Coil disk cache even when server headers are conservative
            .respectCacheHeaders(false)
            .build()
    }

    fun preloadUrls(context: Context, urls: List<String?>, maxCount: Int = 30) {
        val loader = getImageLoader(context)
        urls.asSequence()
            .filterNotNull()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(maxCount)
            .forEach { url ->
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build()
                loader.enqueue(request)
            }
    }
    
    /**
     * Clear all image caches
     */
    fun clearCache(context: Context) {
        imageLoader?.let {
            it.memoryCache?.clear()
            it.diskCache?.clear()
        }
    }
}

