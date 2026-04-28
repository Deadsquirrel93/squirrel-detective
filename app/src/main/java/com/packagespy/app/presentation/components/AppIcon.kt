package com.packagespy.app.presentation.components

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val painter = remember(packageName) {
        try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            BitmapPainter(drawable.toBitmap(96, 96).asImageBitmap())
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
    val shape = RoundedCornerShape(12.dp)
    if (painter != null) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = modifier.size(44.dp).clip(shape)
        )
    } else {
        Icon(
            imageVector = Icons.Filled.Android,
            contentDescription = null,
            modifier = modifier
                .size(44.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}
