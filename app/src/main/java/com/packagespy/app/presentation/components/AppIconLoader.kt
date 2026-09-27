package com.packagespy.app.presentation.components

import android.content.Context
import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

val LocalAppIconLoader = staticCompositionLocalOf<AppIconLoader> {
    error("AppIconLoader not provided")
}

@Singleton
class AppIconLoader @Inject constructor(@ApplicationContext private val context: Context) {

    private val cache = object : LruCache<String, ImageBitmap>(
        minOf(Runtime.getRuntime().maxMemory() / 8, 16L * 1024 * 1024).toInt()
    ) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    fun cached(packageName: String, sizePx: Int): ImageBitmap? = cache.get(key(packageName, sizePx))

    suspend fun load(packageName: String, sizePx: Int): ImageBitmap? {
        cached(packageName, sizePx)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = context.packageManager
                    .getApplicationIcon(packageName)
                    .toBitmap(sizePx, sizePx)
                    .asImageBitmap()
                cache.put(key(packageName, sizePx), bitmap)
                bitmap
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }
    }

    private fun key(packageName: String, sizePx: Int) = "$packageName@$sizePx"
}
