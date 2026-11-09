package ir.unilink.android.ui

import androidx.compose.foundation.background
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
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

        StatusHero(
            color = color,
            icon = when (st.phase) {
                Phase.Connected -> Icons.Default.Check
                Phase.Connecting, Phase.WaitingRetry -> Icons.Default.Refresh
                Phase.NeedCertificate, Phase.AccountNeeded -> Icons.Default.Warning
                else -> Icons.Default.Info
            },
            pulsing = st.phase == Phase.Connecting || st.phase == Phase.WaitingRetry,
            title = st.title,
            detail = st.detail,
        )

        Panel {
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
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(26.dp),
            ) { Text(stringResource(R.string.conn_connect)) }
            OutlinedButton(
                onClick = { app.engine.pauseAndSignOut() },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(26.dp),
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

/** Big status badge: a glowing ring in the status colour that pulses while work is in progress. */
@Composable
private fun StatusHero(color: Color, icon: ImageVector, pulsing: Boolean, title: String, detail: String) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    val glow = if (pulsing) pulse else 1f
    GlassSurface(Modifier.fillMaxWidth(), strong = true) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .drawBehind {
                        val c = Offset(size.width / 2, size.height / 2)
                        val r = size.minDimension * 0.95f
                        drawCircle(
                            Brush.radialGradient(listOf(color.copy(alpha = 0.40f * glow), Color.Transparent), c, r),
                            r, c,
                        )
                    }
                    .padding(14.dp)
                    .background(color.copy(alpha = 0.18f), CircleShape)
                    .border(2.dp, color.copy(alpha = 0.55f + 0.45f * glow), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(34.dp))
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            if (detail.isNotEmpty()) {
                Text(
                    detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
