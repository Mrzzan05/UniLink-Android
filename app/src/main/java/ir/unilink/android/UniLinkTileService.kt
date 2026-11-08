package ir.unilink.android

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quick Settings tile. Tap while connected (or connecting) to pause and sign out; tap otherwise to
 * start the background service and connect.
 */
class UniLinkTileService : TileService() {
    private val app get() = application as UniLinkApp
    private var scope: CoroutineScope? = null
    private var watcher: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        val s = CoroutineScope(Dispatchers.Main)
        scope = s
        watcher = s.launch {
            app.engine.status.collect { render(it) }
        }
    }

    override fun onStopListening() {
        watcher?.cancel()
        scope?.cancel()
        watcher = null
        scope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val phase = app.engine.status.value.phase
        if (phase == Phase.Connected || phase == Phase.Connecting) {
            app.engine.pauseAndSignOut()
        } else {
            runCatching { KeepAliveService.start(this) }
            app.engine.connectNow()
        }
    }

    private fun render(st: EngineStatus) {
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(this, R.drawable.ic_stat)
        tile.label = "UniLink"
        when (st.phase) {
            Phase.Connected -> {
                tile.state = Tile.STATE_ACTIVE
                tile.setSubtitleCompat(st.accountName ?: "Connected")
            }
            Phase.Connecting -> {
                tile.state = Tile.STATE_ACTIVE
                tile.setSubtitleCompat("Signing in…")
            }
            Phase.NeedCertificate -> {
                tile.state = Tile.STATE_INACTIVE
                tile.setSubtitleCompat("Review certificate")
            }
            Phase.WaitingRetry -> {
                tile.state = Tile.STATE_INACTIVE
                tile.setSubtitleCompat("Retrying")
            }
            Phase.Paused -> {
                tile.state = Tile.STATE_INACTIVE
                tile.setSubtitleCompat("Paused")
            }
            Phase.WaitingWifi, Phase.AccountNeeded -> {
                tile.state = Tile.STATE_INACTIVE
                tile.setSubtitleCompat(st.title)
            }
            Phase.Stopped -> {
                tile.state = Tile.STATE_INACTIVE
                tile.setSubtitleCompat("Tap to connect")
            }
        }
        tile.updateTile()
    }

    private fun Tile.setSubtitleCompat(text: String) {
        if (Build.VERSION.SDK_INT >= 29) subtitle = text
    }
}
