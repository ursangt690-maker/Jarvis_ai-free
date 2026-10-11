package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class JarvisHudTheme(val displayName: String) {
    ARC_CYAN("Arc Reactor Cyan"),
    MARK_GOLD("Mark VII Gold & Crimson"),
    STEALTH_MATRIX("Stealth Matrix Emerald")
}

private val ArcCyanColorScheme = darkColorScheme(
    primary = ArcCyanPrimary,
    onPrimary = JarvisBgDeep,
    primaryContainer = Color(0xFF003B4D),
    onPrimaryContainer = ArcCyanPrimary,
    secondary = ArcCyanSecondary,
    onSecondary = JarvisBgDeep,
    secondaryContainer = Color(0xFF0D2847),
    onSecondaryContainer = ArcCyanSecondary,
    tertiary = ArcCyanTertiary,
    onTertiary = JarvisBgDeep,
    background = JarvisBgDeep,
    onBackground = JarvisTextPrimary,
    surface = JarvisBgSurface,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisBgCard,
    onSurfaceVariant = JarvisTextSecondary,
    error = JarvisCriticalRed,
    onError = Color.White
)

private val MarkGoldColorScheme = darkColorScheme(
    primary = MarkGoldPrimary,
    onPrimary = JarvisBgDeep,
    primaryContainer = Color(0xFF4A3800),
    onPrimaryContainer = MarkGoldPrimary,
    secondary = MarkCrimsonSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4D0F16),
    onSecondaryContainer = MarkCrimsonSecondary,
    tertiary = MarkOrangeTertiary,
    onTertiary = JarvisBgDeep,
    background = Color(0xFF0B0608),
    onBackground = Color(0xFFFFF8E1),
    surface = Color(0xFF170D10),
    onSurface = Color(0xFFFFF8E1),
    surfaceVariant = Color(0xFF261519),
    onSurfaceVariant = Color(0xFFFFCC80),
    error = JarvisCriticalRed,
    onError = Color.White
)

private val StealthMatrixColorScheme = darkColorScheme(
    primary = MatrixGreenPrimary,
    onPrimary = JarvisBgDeep,
    primaryContainer = Color(0xFF003D1E),
    onPrimaryContainer = MatrixGreenPrimary,
    secondary = MatrixTealSecondary,
    onSecondary = JarvisBgDeep,
    secondaryContainer = Color(0xFF003830),
    onSecondaryContainer = MatrixTealSecondary,
    tertiary = MatrixLimeTertiary,
    onTertiary = JarvisBgDeep,
    background = Color(0xFF030B06),
    onBackground = Color(0xFFE8F5E9),
    surface = Color(0xFF07170E),
    onSurface = Color(0xFFE8F5E9),
    surfaceVariant = Color(0xFF0E291A),
    onSurfaceVariant = Color(0xFFA5D6A7),
    error = JarvisCriticalRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    hudTheme: JarvisHudTheme = JarvisHudTheme.ARC_CYAN,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (hudTheme) {
        JarvisHudTheme.ARC_CYAN -> ArcCyanColorScheme
        JarvisHudTheme.MARK_GOLD -> MarkGoldColorScheme
        JarvisHudTheme.STEALTH_MATRIX -> StealthMatrixColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
