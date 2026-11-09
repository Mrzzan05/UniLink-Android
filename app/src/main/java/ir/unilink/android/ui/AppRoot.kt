package ir.unilink.android.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
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

@Composable
fun AppRoot(app: UniLinkApp) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    GlassBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                GlassSurface(strong = true, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
                    NavigationBar(containerColor = Color.Transparent) {
                        tabs.forEachIndexed { i, t ->
                            val label = stringResource(t.label)
                            NavigationBarItem(
                                selected = tab == i,
                                onClick = { tab = i },
                                icon = { Icon(t.icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (tab) {
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
