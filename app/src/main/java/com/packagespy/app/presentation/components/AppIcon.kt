package com.packagespy.app.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private sealed interface IconState {
    data object Loading : IconState
    data object Missing : IconState
    data class Loaded(val bitmap: ImageBitmap) : IconState
}

@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val sizePx = with(LocalDensity.current) { 44.dp.roundToPx() }
    val loader = LocalAppIconLoader.current
    // key() resets the produced state when the package changes; produceState alone
    // would keep the previous package's icon because its holder is not keyed.
    val state by key(packageName, sizePx) {
        produceState<IconState>(
            initialValue = loader.cached(packageName, sizePx)?.let(IconState::Loaded) ?: IconState.Loading,
        ) {
            if (value is IconState.Loading) {
                value = loader.load(packageName, sizePx)?.let(IconState::Loaded) ?: IconState.Missing
            }
        }
    }
    val shape = RoundedCornerShape(12.dp)
    when (val current = state) {
        is IconState.Loaded -> Image(
            painter = remember(current.bitmap) { BitmapPainter(current.bitmap) },
            contentDescription = null,
            modifier = modifier.size(44.dp).clip(shape)
        )
        is IconState.Missing -> Icon(
            imageVector = Icons.Filled.Android,
            contentDescription = null,
            modifier = modifier
                .size(44.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        is IconState.Loading -> Box(
            modifier = modifier
                .size(44.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}
