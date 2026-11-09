package ir.unilink.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScreenTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
}

/** Soft gradient backdrop with two colour blobs; the glass panels sit on top of it. */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val g = LocalGlass.current
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(g.backdropTop, g.backdropBottom)))
            .drawBehind {
                val a = Offset(size.width * 0.15f, size.height * 0.12f)
                val b = Offset(size.width * 0.9f, size.height * 0.62f)
                val ra = size.maxDimension * 0.55f
                val rb = size.maxDimension * 0.5f
                val strength = if (g.dark) 0.5f else 0.6f
                drawCircle(
                    Brush.radialGradient(listOf(g.blobA.copy(alpha = strength), Color.Transparent), a, ra),
                    ra, a,
                )
                drawCircle(
                    Brush.radialGradient(listOf(g.blobB.copy(alpha = strength * 0.8f), Color.Transparent), b, rb),
                    rb, b,
                )
            },
    ) { content() }
}

/** A frosted-glass surface: translucent fill and a bright top-left edge. */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit,
) {
    val g = LocalGlass.current
    Box(
        modifier
            .clip(shape)
            .background(if (strong) g.glassFillStrong else g.glassFill)
            .border(1.dp, Brush.linearGradient(listOf(g.glassEdgeHi, g.glassEdgeLo)), shape),
    ) { content() }
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    GlassSurface(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun Tag(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.18f), shape = RoundedCornerShape(50)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

fun formatTime(ts: Long): String =
    if (ts <= 0L) "—" else SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(ts))

/** Increments every time the activity returns to the foreground; use it to re-read permissions. */
@Composable
fun rememberResumeTick(): Int {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(ctx) {
        val owner = ctx as? LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }
    return tick
}
