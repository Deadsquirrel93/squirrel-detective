package com.packagespy.app.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.packagespy.app.domain.model.RiskLevel

object RiskColors {
    private val Red = Color(0xFFB3261E)
    private val RedContainer = Color(0xFFF9DEDC)
    private val Yellow = Color(0xFF7C5800)
    private val YellowContainer = Color(0xFFFFE7B0)
    private val Green = Color(0xFF1E6B3B)
    private val GreenContainer = Color(0xFFD3EFDB)
    private val Safe = Color(0xFF49454F)
    private val SafeContainer = Color(0xFFE6E0E9)

    fun bgFor(level: RiskLevel): Color = when (level) {
        RiskLevel.RED -> RedContainer
        RiskLevel.YELLOW -> YellowContainer
        RiskLevel.GREEN -> GreenContainer
        RiskLevel.SAFE -> SafeContainer
    }

    fun textFor(level: RiskLevel): Color = when (level) {
        RiskLevel.RED -> Red
        RiskLevel.YELLOW -> Yellow
        RiskLevel.GREEN -> Green
        RiskLevel.SAFE -> Safe
    }
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF24524E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFE8E3),
    onPrimaryContainer = Color(0xFF002523),
    secondary = Color(0xFF4B6361),
    onSecondary = Color.White,
    background = Color(0xFFF7F9F8),
    onBackground = Color(0xFF1A1C1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1B),
    surfaceVariant = Color(0xFFEAEEEC),
    onSurfaceVariant = Color(0xFF44494A),
    outline = Color(0xFF7A8987),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FCFC8),
    onPrimary = Color(0xFF003734),
    primaryContainer = Color(0xFF1F4F4B),
    onPrimaryContainer = Color(0xFFB1ECE5),
    secondary = Color(0xFFB1CCC9),
    onSecondary = Color(0xFF1B3533),
    background = Color(0xFF11181A),
    onBackground = Color(0xFFE2E3E1),
    surface = Color(0xFF1A2122),
    onSurface = Color(0xFFE2E3E1),
    surfaceVariant = Color(0xFF253031),
    onSurfaceVariant = Color(0xFFC2CAC9),
    outline = Color(0xFF8C9695),
)

@Composable
fun NutSpyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colors, content = content)
}
