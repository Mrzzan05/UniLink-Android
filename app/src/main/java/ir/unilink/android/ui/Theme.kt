package ir.unilink.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colors that are not part of Material's scheme: the glass look and the status palette. */
data class GlassColors(
    val dark: Boolean,
    val backdropTop: Color,
    val backdropBottom: Color,
    val blobA: Color,
    val blobB: Color,
    val glassFill: Color,
    val glassFillStrong: Color,
    val glassEdgeHi: Color,
    val glassEdgeLo: Color,
    val green: Color,
    val amber: Color,
    val red: Color,
    val grey: Color,
    val blue: Color,
)

private val DarkGlass = GlassColors(
    dark = true,
    backdropTop = Color(0xFF0B1220),
    backdropBottom = Color(0xFF101A33),
    blobA = Color(0xFF3B6FE0),
    blobB = Color(0xFF8A5BE8),
    glassFill = Color(0x14FFFFFF),
    glassFillStrong = Color(0x24FFFFFF),
    glassEdgeHi = Color(0x66FFFFFF),
    glassEdgeLo = Color(0x0FFFFFFF),
    green = Color(0xFF3DDC84),
    amber = Color(0xFFFFB74D),
    red = Color(0xFFFF6B6B),
    grey = Color(0xFF8A97AD),
    blue = Color(0xFF6FA8FF),
)

private val LightGlass = GlassColors(
    dark = false,
    backdropTop = Color(0xFFE9F0FF),
    backdropBottom = Color(0xFFF6F1FF),
    blobA = Color(0xFF8DB4FF),
    blobB = Color(0xFFC9A8FF),
    glassFill = Color(0x80FFFFFF),
    glassFillStrong = Color(0xB3FFFFFF),
    glassEdgeHi = Color(0xE6FFFFFF),
    glassEdgeLo = Color(0x33FFFFFF),
    green = Color(0xFF0E9F55),
    amber = Color(0xFFC77700),
    red = Color(0xFFD93A3A),
    grey = Color(0xFF5E6B82),
    blue = Color(0xFF2A62D9),
)

val LocalGlass = staticCompositionLocalOf { DarkGlass }

val StatusGreen: Color @Composable get() = LocalGlass.current.green
val StatusAmber: Color @Composable get() = LocalGlass.current.amber
val StatusRed: Color @Composable get() = LocalGlass.current.red
val StatusGrey: Color @Composable get() = LocalGlass.current.grey
val AccentBlue: Color @Composable get() = LocalGlass.current.blue

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF6FA8FF),
    onPrimary = Color(0xFF00255C),
    primaryContainer = Color(0xFF1F3F7A),
    onPrimaryContainer = Color(0xFFD8E6FF),
    secondary = Color(0xFF9DB8E8),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE9EEF7),
    surface = Color(0xFF0B1220),
    onSurface = Color(0xFFE9EEF7),
    surfaceVariant = Color(0xFF18233A),
    onSurfaceVariant = Color(0xFFB4BFD2),
    surfaceContainer = Color(0xFF16213A),
    surfaceContainerHigh = Color(0xFF1B2742),
    surfaceContainerHighest = Color(0xFF21304F),
    outline = Color(0xFF3A4A68),
    outlineVariant = Color(0x33FFFFFF),
    error = Color(0xFFFF6B6B),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF2A62D9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E6FF),
    onPrimaryContainer = Color(0xFF0A2A66),
    secondary = Color(0xFF4A5F8A),
    background = Color(0xFFE9F0FF),
    onBackground = Color(0xFF111A2B),
    surface = Color(0xFFF3F6FD),
    onSurface = Color(0xFF111A2B),
    surfaceVariant = Color(0xFFE2E9F6),
    onSurfaceVariant = Color(0xFF45526B),
    surfaceContainer = Color(0xFFEAF0FB),
    surfaceContainerHigh = Color(0xFFE2EAF8),
    surfaceContainerHighest = Color(0xFFD9E3F5),
    outline = Color(0xFF8593AC),
    outlineVariant = Color(0x33000000),
    error = Color(0xFFD93A3A),
)

@Composable
fun UniLinkTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalGlass provides if (dark) DarkGlass else LightGlass) {
        MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
    }
}
