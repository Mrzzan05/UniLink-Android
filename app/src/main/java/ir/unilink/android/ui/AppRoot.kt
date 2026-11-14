package ir.unilink.android.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.unilink.android.R
import ir.unilink.android.UniLinkApp

private data class TabItem(@StringRes val label: Int, val icon: ImageVector)

private val tabs = listOf(
    TabItem(R.string.tab_connection, Icons.Default.Refresh),
    TabItem(R.string.tab_accounts, Icons.Default.Person),
    TabItem(R.string.tab_networks, Icons.Default.Place),
    TabItem(R.string.tab_activity, Icons.AutoMirrored.Filled.List),
    TabItem(R.string.tab_settings, Icons.Default.Settings),
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppRoot(app: UniLinkApp) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tick = rememberTick()

    // Back from any other tab returns to the first one before the app closes.
    BackHandler(enabled = tab != 0) { tab = 0 }

    GlassBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                Box(Modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    GlassSurface(Modifier.fillMaxWidth(), strong = true, shape = RoundedCornerShape(30.dp)) {
                        Row(
                            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            tabs.forEachIndexed { i, t ->
                                NavPill(
                                    item = t,
                                    selected = tab == i,
                                    modifier = Modifier.weight(
                                        animateFloatAsState(
                                            if (tab == i) 2.1f else 1f,
                                            bouncySpring(),
                                            label = "tabWeight",
                                        ).value,
                                    ),
                                ) {
                                    if (tab != i) {
                                        tick()
                                        tab = i
                                    }
                                }
                            }
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (fadeIn(tween(260, delayMillis = 70)) +
                            slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 9 * dir } +
                            scaleIn(tween(320, easing = FastOutSlowInEasing), initialScale = 0.96f)) togetherWith
                            (fadeOut(tween(110)) +
                                slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { -it / 9 * dir }) using
                            SizeTransform(clip = false)
                    },
                    label = "tabs",
                ) { t ->
                    when (t) {
                        0 -> ConnectionScreen(app) { tab = it }
                        1 -> AccountsScreen(app)
                        2 -> NetworksScreen(app)
                        3 -> ActivityScreen(app)
                        else -> SettingsScreen(app)
                    }
                }
            }
        }
    }
}

/** One tab: an icon, and a label that slides out next to it while the tab is selected. */
@Composable
private fun NavPill(item: TabItem, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val label = stringResource(item.label)
    val primary = MaterialTheme.colorScheme.primary
    val bg by animateColorAsState(
        if (selected) primary.copy(alpha = 0.22f) else Color.Transparent,
        tween(220),
        label = "pillBg",
    )
    val fg by animateColorAsState(
        if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(220),
        label = "pillFg",
    )
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .selectable(selected = selected, interactionSource = source, indication = null, role = Role.Tab, onClick = onClick)
            .pressScale(source, 0.92f),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(item.icon, contentDescription = if (selected) null else label, tint = fg, modifier = Modifier.size(24.dp))
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(tween(200, delayMillis = 60)) + expandHorizontally(bouncySpring()),
                exit = fadeOut(tween(90)) + shrinkHorizontally(softSpring()),
            ) {
                Text(
                    label,
                    color = fg,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}
