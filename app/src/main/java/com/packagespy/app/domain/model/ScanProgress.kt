package com.packagespy.app.domain.model

data class ScanProgress(
    val current: Int,
    val total: Int
) {
    val fraction: Float
        get() = if (total <= 0) 0f else (current.toFloat() / total).coerceIn(0f, 1f)
}
