package ir.unilink.android.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ir.unilink.android.BuildConfig
import ir.unilink.android.KeepAliveService
import ir.unilink.android.UniLinkApp

@Composable
fun ActivityScreen(app: UniLinkApp) {
    val log by app.engine.logLines.collectAsState()
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ScreenTitle("Activity")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "UniLink diagnostic log")
                    putExtra(Intent.EXTRA_TEXT, log.joinToString("\n"))
                }
                ctx.startActivity(Intent.createChooser(send, "Share log"))
            }) { Text("Share log") }
            OutlinedButton(onClick = { app.engine.clearLog() }) { Text("Clear") }
        }
        Text(
            "Passwords and student numbers are never written to this log. It is kept in memory only.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (log.isEmpty()) {
            Text("Nothing yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(log.asReversed()) { line ->
                Text(line, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PermissionRow(title: String, subtitle: String, granted: Boolean, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (granted) Tag("OK", StatusGreen) else OutlinedButton(onClick = onFix) { Text("Allow") }
    }
}

@Composable
fun SettingsScreen(app: UniLinkApp) {
    val data by app.store.data.collectAsState()
    val s = data.settings
    val ctx = LocalContext.current
    val tick = rememberResumeTick()

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val notifGranted = remember(tick) { notificationsEnabled(ctx) }
    val locGranted = remember(tick) {
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }
    val batteryOk = remember(tick) {
        (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ScreenTitle("Settings")

        Panel {
            SwitchRow(
                "Automatic sign-in",
                "Sign in as soon as a configured Wi-Fi network is detected.",
                s.autoSignIn,
            ) { v -> app.store.updateSettings { it.copy(autoSignIn = v) }; app.engine.kick() }
            SwitchRow(
                "Start when the phone boots",
                "Starts the background service after a restart.",
                s.startOnBoot,
            ) { v -> app.store.updateSettings { it.copy(startOnBoot = v) } }
            SwitchRow(
                "Keep CPU and Wi-Fi awake",
                "Most reliable with the screen off. Uses more battery; turn off if the phone is rarely on campus Wi-Fi.",
                s.holdWakeLock,
            ) { v -> app.store.updateSettings { it.copy(holdWakeLock = v) } }
        }

        Panel {
            Text("Permissions", style = MaterialTheme.typography.titleMedium)
            PermissionRow(
                "Location (reads the Wi-Fi name)", "Needed to recognise UUWN.", locGranted,
            ) { locLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
            PermissionRow(
                "Notifications", "Shows the status in the notification shade.", notifGranted,
            ) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else openNotificationSettings(ctx)
            }
            PermissionRow(
                "Unrestricted battery", "Stops Android from pausing UniLink in the background.", batteryOk,
            ) {
                runCatching {
                    ctx.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            .setData(Uri.parse("package:${ctx.packageName}"))
                    )
                }
            }
        }

        Panel {
            Text("About", style = MaterialTheme.typography.titleMedium)
            Text(
                "UniLink for Android ${BuildConfig.VERSION_NAME}. Accounts and settings are encrypted with a " +
                    "key held in the Android Keystore and never leave this phone. Only add accounts you are " +
                    "allowed to use.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = {
                KeepAliveService.stop(ctx)
                (ctx as? Activity)?.finishAffinity()
            },
            modifier = Modifier.padding(top = 6.dp),
        ) { Text("Exit UniLink") }
    }
}
