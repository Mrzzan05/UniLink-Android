package ir.unilink.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.unilink.android.CertPrompt
import ir.unilink.android.KeepAliveService
import ir.unilink.android.Phase
import ir.unilink.android.R
import ir.unilink.android.UniLinkApp

@Composable
fun ConnectionScreen(app: UniLinkApp, goToTab: (Int) -> Unit) {
    val st by app.engine.status.collectAsState()
    val stats by app.engine.stats.collectAsState()
    val ctx = LocalContext.current
    val color = when (st.phase) {
        Phase.Connected -> StatusGreen
        Phase.WaitingRetry, Phase.NeedCertificate, Phase.AccountNeeded -> StatusAmber
        Phase.Connecting -> AccentBlue
        else -> StatusGrey
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle(stringResource(R.string.tab_connection))

        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).background(color, CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(st.title, style = MaterialTheme.typography.titleLarge)
            }
            if (st.detail.isNotEmpty()) {
                Text(st.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow(stringResource(R.string.conn_network), st.groupName ?: "—")
            InfoRow(stringResource(R.string.conn_account), st.accountName ?: "—")
            InfoRow(stringResource(R.string.conn_last_keepalive), formatTime(st.lastKeepalive))
            InfoRow(stringResource(R.string.conn_next_check), formatTime(st.nextCheck))
            InfoRow(
                stringResource(R.string.conn_keepalives),
                stringResource(R.string.conn_keepalives_value, stats.ok, stats.failed),
            )
            if (stats.late > 0) {
                InfoRow(
                    stringResource(R.string.conn_late),
                    stringResource(R.string.conn_late_value, stats.late, stats.maxLateSec),
                )
            }
        }

        st.pendingCert?.let { prompt ->
            CertificateCard(prompt) { app.engine.approveCertificate(prompt) }
        }

        SetupChecklist(app, goToTab)

        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    KeepAliveService.start(ctx)
                    app.engine.connectNow()
                },
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.conn_connect)) }
            OutlinedButton(
                onClick = { app.engine.pauseAndSignOut() },
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.conn_pause)) }
        }
    }
}

@Composable
private fun CertificateCard(p: CertPrompt, onTrust: () -> Unit) {
    Panel {
        Text(
            stringResource(if (p.changed) R.string.cert_changed_title else R.string.cert_review_title),
            style = MaterialTheme.typography.titleMedium,
            color = StatusAmber,
        )
        Text(
            stringResource(R.string.cert_body),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        InfoRow(stringResource(R.string.cert_network), p.groupName)
        Text(p.url, style = MaterialTheme.typography.bodySmall)
        Text(
            stringResource(R.string.cert_sha256),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(p.fingerprint, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        if (p.subject.isNotBlank()) {
            Text(p.subject, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = onTrust, modifier = Modifier.padding(top = 6.dp)) {
            Text(stringResource(R.string.cert_trust))
        }
    }
}
