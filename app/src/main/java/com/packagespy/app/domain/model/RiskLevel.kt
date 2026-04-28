package com.packagespy.app.domain.model

enum class RiskLevel {
    RED,
    YELLOW,
    GREEN,
    SAFE;

    val sortOrder: Int
        get() = when (this) {
            RED -> 0
            YELLOW -> 1
            GREEN -> 2
            SAFE -> 3
        }
}
