package ir.unilink.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val StatusGreen = Color(0xFF3DDC84)
val StatusAmber = Color(0xFFFFB74D)
val StatusRed = Color(0xFFFF6B6B)
val StatusGrey = Color(0xFF8A97AD)
val AccentBlue = Color(0xFF5B9BFF)

private val Scheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color(0xFF00255C),
    primaryContainer = Color(0xFF1F3F7A),
    onPrimaryContainer = Color(0xFFD8E6FF),
    secondary = Color(0xFF9DB8E8),
    background = Color(0xFF0E1522),
    onBackground = Color(0xFFE6EBF3),
    surface = Color(0xFF0E1522),
    onSurface = Color(0xFFE6EBF3),
    surfaceVariant = Color(0xFF18233A),
    onSurfaceVariant = Color(0xFFAAB6C9),
    surfaceContainerLowest = Color(0xFF0A101B),
    surfaceContainerLow = Color(0xFF121B2D),
    surfaceContainer = Color(0xFF16213A),
    surfaceContainerHigh = Color(0xFF1B2742),
    surfaceContainerHighest = Color(0xFF21304F),
    outline = Color(0xFF3A4A68),
    outlineVariant = Color(0xFF2A3753),
    error = StatusRed,
)

@Composable
fun UniLinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
