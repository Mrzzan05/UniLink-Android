package ir.unilink.android

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import java.net.Inet4Address

/**
 * Tracks the physical (non-VPN) Wi-Fi network. Requests made through [network] are bound to the
 * Wi-Fi interface even while a VPN is the default route.
 */
class WifiTracker(context: Context, private val onEvent: () -> Unit) {
    private val appCtx = context.applicationContext
    private val cm = appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wm = appCtx.getSystemService(Context.WIFI_SERVICE) as WifiManager

    @Volatile
    var network: Network? = null
        private set

    private var registered = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            this@WifiTracker.network = network
            onEvent()
        }

        override fun onLost(network: Network) {
            if (this@WifiTracker.network == network) this@WifiTracker.network = null
            onEvent()
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                this@WifiTracker.network = network
            }
            onEvent()
        }

        override fun onLinkPropertiesChanged(network: Network, lp: android.net.LinkProperties) {
            onEvent()
        }
    }

    fun start() {
        if (registered) return
        // The default builder already requires NOT_VPN; no INTERNET capability is required, so a
        // captive-portal network that has no validated internet yet is still reported.
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        cm.registerNetworkCallback(request, callback)
        registered = true
    }

    fun stop() {
        if (!registered) return
        runCatching { cm.unregisterNetworkCallback(callback) }
        registered = false
        network = null
    }

    fun hasIpv4(n: Network): Boolean =
        cm.getLinkProperties(n)?.linkAddresses?.any { it.address is Inet4Address } == true

    /** Current Wi-Fi name, or null when Android hides it (needs the location permission). */
    @Suppress("DEPRECATION")
    fun ssid(): String? {
        var raw: String? = null
        try {
            raw = wm.connectionInfo?.ssid
        } catch (_: Exception) {
        }
        if (raw == null) return null
        val s = raw.trim().removeSurrounding("\"")
        return if (s.isEmpty() || s == "<unknown ssid>") null else s
    }
}
