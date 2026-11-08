package ir.unilink.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.unilink.android.NetworkGroup
import ir.unilink.android.UniLinkApp
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.time.ZoneId

@Composable
fun NetworksScreen(app: UniLinkApp) {
    val data by app.store.data.collectAsState()
    var editing by remember { mutableStateOf<NetworkGroup?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<NetworkGroup?>(null) }

    Box(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Column(Modifier.fillMaxSize().padding(top = 16.dp)) {
            ScreenTitle("Networks")
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(data.groups, key = { it.id }) { g ->
                    Panel {
                        Text(g.name, style = MaterialTheme.typography.titleMedium)
                        InfoRow("Wi-Fi names", g.ssids.joinToString().ifEmpty { "—" })
                        InfoRow("Accounts", g.accounts.size.toString())
                        InfoRow("Keepalive", "${g.keepaliveSec} s")
                        InfoRow("Time zone", g.timeZone)
                        g.portalUrls.forEach { url ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(url, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                if (g.pins.containsKey(url)) Tag("Certificate pinned", StatusGreen)
                                else Tag("Not pinned", StatusAmber)
                            }
                        }
                        if (g.portalOnly) {
                            Text(
                                "Also matches by reachable, pinned portal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { editing = g }) { Text("Edit") }
                            if (g.pins.isNotEmpty()) {
                                TextButton(onClick = {
                                    app.store.updateGroup(g.id) { it.copy(pins = emptyMap()) }
                                    app.engine.log("Pinned certificates cleared for ${g.name}")
                                    app.engine.kick()
                                }) { Text("Forget certificates") }
                            }
                            TextButton(onClick = { deleting = g }) { Text("Delete") }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { adding = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 16.dp),
        ) { Icon(Icons.Default.Add, contentDescription = "Add network group") }
    }

    if (adding) {
        GroupDialog(
            initial = null,
            onDismiss = { adding = false },
            onSave = { app.store.upsertGroup(it); adding = false; app.engine.kick() },
        )
    }
    editing?.let { g ->
        GroupDialog(
            initial = g,
            onDismiss = { editing = null },
            onSave = { app.store.upsertGroup(it); editing = null; app.engine.kick() },
        )
    }
    deleting?.let { g ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete network group?") },
            text = { Text("\"${g.name}\" and its ${g.accounts.size} account(s) will be removed from this phone.") },
            confirmButton = {
                TextButton(onClick = { app.store.deleteGroup(g.id); deleting = null; app.engine.kick() }) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

private fun lines(s: String) = s.lines().map { it.trim() }.filter { it.isNotEmpty() }

@Composable
private fun GroupDialog(initial: NetworkGroup?, onDismiss: () -> Unit, onSave: (NetworkGroup) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var ssids by remember { mutableStateOf(initial?.ssids?.joinToString("\n") ?: "") }
    var urls by remember { mutableStateOf(initial?.portalUrls?.joinToString("\n") ?: "https://") }
    var portalOnly by remember { mutableStateOf(initial?.portalOnly ?: false) }
    var keep by remember { mutableStateOf((initial?.keepaliveSec ?: 180).toString()) }
    var tz by remember { mutableStateOf(initial?.timeZone ?: NetworkGroup.DEFAULT_TZ) }
    var phrases by remember { mutableStateOf(initial?.quotaPhrases?.joinToString("\n") ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    fun trySave() {
        val urlList = lines(urls)
        val keepInt = keep.trim().toIntOrNull()
        error = when {
            name.isBlank() -> "Give the network a name."
            urlList.isEmpty() -> "Add at least one portal URL."
            urlList.any { it.toHttpUrlOrNull()?.isHttps != true } -> "Portal URLs must be valid https:// addresses."
            keepInt == null || keepInt !in 30..180 -> "Keepalive must be between 30 and 180 seconds."
            runCatching { ZoneId.of(tz.trim()) }.isFailure -> "Unknown time zone. Example: Asia/Tehran"
            lines(ssids).isEmpty() && !portalOnly -> "Add a Wi-Fi name, or enable portal-only matching."
            else -> null
        }
        if (error != null) return
        val base = initial ?: NetworkGroup(name = "", ssids = emptyList(), portalUrls = emptyList())
        onSave(
            base.copy(
                name = name.trim(),
                ssids = lines(ssids),
                portalUrls = urlList,
                portalOnly = portalOnly,
                keepaliveSec = keepInt ?: 180,
                timeZone = tz.trim(),
                quotaPhrases = lines(phrases),
                pins = base.pins.filterKeys { it in urlList },
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add network group" else "Edit network group") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") }, singleLine = true,
                )
                OutlinedTextField(
                    value = ssids, onValueChange = { ssids = it },
                    label = { Text("Wi-Fi names (one per line, exact match)") }, minLines = 2,
                )
                OutlinedTextField(
                    value = urls, onValueChange = { urls = it },
                    label = { Text("Portal URLs (https, one per line)") }, minLines = 2,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = portalOnly, onCheckedChange = { portalOnly = it })
                    Text("Also match by a reachable, pinned portal (when the Wi-Fi name is unknown)")
                }
                OutlinedTextField(
                    value = keep, onValueChange = { keep = it },
                    label = { Text("Keepalive seconds (30–180)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = tz, onValueChange = { tz = it },
                    label = { Text("Time zone (for monthly reset)") }, singleLine = true,
                )
                OutlinedTextField(
                    value = phrases, onValueChange = { phrases = it },
                    label = { Text("Extra quota-exceeded phrases (one per line)") }, minLines = 2,
                    supportingText = { Text("Optional. The university's standard message is built in.") },
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = { trySave() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
