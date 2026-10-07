package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * FlowRec Design Tokens (Minimalist Monochrome - Ink on Paper)
 * Tokens defined in files/SKILL.md & compose.md
 */
@Immutable
data class FlowColors(
    val bg: Color,
    val fg: Color,
    val muted: Color,
    val fill: Color,
    val separator: Color,
    val glass: Color,
    val sheet: Color,
    val record: Color,
    val onFg: Color
)

val DarkFlow = FlowColors(
    bg = Color(0xFF0A0A0A),
    fg = Color(0xFFF5F5F3),
    muted = Color(0xFFF5F5F3).copy(alpha = 0.47f),
    fill = Color.White.copy(alpha = 0.07f),
    separator = Color.White.copy(alpha = 0.08f),
    glass = Color(0xFF161616).copy(alpha = 0.72f),
    sheet = Color(0xFF121212).copy(alpha = 0.95f),
    record = Color(0xFFFF4533),
    onFg = Color(0xFF0A0A0A)
)

val LightFlow = FlowColors(
    bg = Color(0xFFF4F4F1),
    fg = Color(0xFF111111),
    muted = Color(0xFF111111).copy(alpha = 0.47f),
    fill = Color(0xFF111111).copy(alpha = 0.05f),
    separator = Color(0xFF111111).copy(alpha = 0.09f),
    glass = Color.White.copy(alpha = 0.80f),
    sheet = Color(0xFFF4F4F1).copy(alpha = 0.95f),
    record = Color(0xFFFF4533),
    onFg = Color(0xFFF4F4F1)
)

val LocalFlow = staticCompositionLocalOf { DarkFlow }

/**
 * Direct accessor for FlowRec design tokens in any Composable:
 * Example: flow.bg, flow.fg, flow.fill, flow.record, flow.glass
 */
val flow: FlowColors
    @Composable
    get() = LocalFlow.current

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF5F5F3),
    onPrimary = Color(0xFF0A0A0A),
    primaryContainer = Color(0xFF161616),
    onPrimaryContainer = Color(0xFFF5F5F3),
    secondary = Color(0xFFF5F5F3).copy(alpha = 0.47f),
    onSecondary = Color(0xFFF5F5F3),
    secondaryContainer = Color.White.copy(alpha = 0.07f),
    onSecondaryContainer = Color(0xFFF5F5F3),
    tertiary = AccentBlue,
    onTertiary = Color.White,
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFF5F5F3),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFF5F5F3),
    surfaceVariant = Color(0xFF161616),
    onSurfaceVariant = Color(0xFFF5F5F3).copy(alpha = 0.65f),
    outline = Color.White.copy(alpha = 0.12f),
    outlineVariant = Color.White.copy(alpha = 0.08f),
    error = Color(0xFFFF4533),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF111111),
    onPrimary = Color(0xFFF4F4F1),
    primaryContainer = Color(0xFFFFFFFF),
    onPrimaryContainer = Color(0xFF111111),
    secondary = Color(0xFF111111).copy(alpha = 0.47f),
    onSecondary = Color(0xFF111111),
    secondaryContainer = Color(0xFF111111).copy(alpha = 0.05f),
    onSecondaryContainer = Color(0xFF111111),
    tertiary = AccentBlue,
    onTertiary = Color.White,
    background = Color(0xFFF4F4F1),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFF4F4F1),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color.White.copy(alpha = 0.80f),
    onSurfaceVariant = Color(0xFF111111).copy(alpha = 0.65f),
    outline = Color(0xFF111111).copy(alpha = 0.14f),
    outlineVariant = Color(0xFF111111).copy(alpha = 0.09f),
    error = Color(0xFFFF4533),
    onError = Color.White
)

@Composable
fun FlowRecTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val activeFlow = if (darkTheme) DarkFlow else LightFlow
    val animatedFlow = activeFlow.copy(
        bg = animateColorAsState(activeFlow.bg, tween(250), label = "flow_bg").value,
        fg = animateColorAsState(activeFlow.fg, tween(250), label = "flow_fg").value
    )

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(LocalFlow provides animatedFlow) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
