package ir.unilink.android.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.unilink.android.UniLinkApp

private class Step(
    val title: String,
    val hint: String,
    val done: Boolean,
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null,
)

/**
 * First-run checklist. Shown on the Connection tab until everything is ready, then it disappears
 * on its own. [goToTab] switches the bottom tab (1 = Accounts).
 */
@Composable
fun SetupChecklist(app: UniLinkApp, goToTab: (Int) -> Unit) {
    val ctx = LocalContext.current
    val data by app.store.data.collectAsState()
    val resumeTick = rememberResumeTick()
    var refresh by remember { mutableIntStateOf(0) }

    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // If Android won't show the dialog again (or the app was blocked in system settings),
        // take the user straight to the notification settings instead.
        if (!granted || !notificationsEnabled(ctx)) openNotificationSettings(ctx)
        refresh++
    }

    // Recomputed whenever the screen resumes or a permission dialog returns.
    val steps = remember(resumeTick, refresh, data) {
        listOf(
            Step(
                "Add an account",
                "Student number and password, on the Accounts tab.",
                data.groups.any { g -> g.accounts.isNotEmpty() },
                "Open Accounts",
            ) { goToTab(1) },
            Step(
                "Allow location",
                "Android only shows the Wi-Fi name to apps with this. It is never stored or sent.",
                locationGranted(ctx),
                "Allow",
            ) { locLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
            Step(
                "Allow notifications",
                "Shows the connection status in the notification shade.",
                notificationsEnabled(ctx),
                "Allow",
            ) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    androidx.core.content.ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else openNotificationSettings(ctx)
            },
            Step(
                "Unrestricted battery",
                "Stops Android from pausing UniLink while the screen is off.",
                batteryUnrestricted(ctx),
                "Allow",
            ) { requestBatteryUnrestricted(ctx) },
            Step(
                "Trust the portal certificate",
                "Appears here the first time you are on campus Wi-Fi. Nothing is sent before you approve it.",
                data.groups.any { it.pins.isNotEmpty() },
            ),
        )
    }

    if (steps.all { it.done }) return
    val doneCount = steps.count { it.done }

    Panel {
        Text("Get ready  ($doneCount/${steps.size})", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
            steps.forEach { s ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            (if (s.done) "✓  " else "○  ") + s.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (s.done) StatusGreen else MaterialTheme.colorScheme.onSurface,
                        )
                        if (!s.done) {
                            Text(
                                s.hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (!s.done && s.action != null && s.actionLabel != null) {
                        OutlinedButton(onClick = s.action) { Text(s.actionLabel) }
                    }
                }
            }
        }
    }
}
