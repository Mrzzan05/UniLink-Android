package ir.unilink.android

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
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

    /** SSID delivered through the network callback (the reliable way on Android 12+). */
    @Volatile
    private var callbackSsid: String? = null

    private var registered = false
    private var callback: ConnectivityManager.NetworkCallback? = null

    private fun cleanSsid(raw: String?): String? {
        val s = raw?.trim()?.removeSurrounding("\"") ?: return null
        return if (s.isEmpty() || s == "<unknown ssid>" || s == WifiManager.UNKNOWN_SSID) null else s
    }

    private fun handleAvailable(n: Network) {
        network = n
        onEvent()
    }

    private fun handleLost(n: Network) {
        if (network == n) {
            network = null
            callbackSsid = null
        }
        onEvent()
    }

    private fun handleCaps(n: Network, caps: NetworkCapabilities) {
        if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) network = n
        if (Build.VERSION.SDK_INT >= 29) {
            val info = caps.transportInfo as? WifiInfo
            cleanSsid(info?.ssid)?.let { callbackSsid = it }
        }
        onEvent()
    }

    private inner class PlainCallback : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = handleAvailable(network)
        override fun onLost(network: Network) = handleLost(network)
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = handleCaps(network, caps)
        override fun onLinkPropertiesChanged(network: Network, lp: LinkProperties) = onEvent()
    }

    private inner class LocationCallback :
        ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
        override fun onAvailable(network: Network) = handleAvailable(network)
        override fun onLost(network: Network) = handleLost(network)
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = handleCaps(network, caps)
        override fun onLinkPropertiesChanged(network: Network, lp: LinkProperties) = onEvent()
    }

    fun start() {
        if (registered) return
        // No INTERNET capability is required, so a captive-portal network without validated
        // internet is still reported.
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        val cb: ConnectivityManager.NetworkCallback =
            if (Build.VERSION.SDK_INT >= 31) LocationCallback() else PlainCallback()
        callback = cb
        cm.registerNetworkCallback(request, cb)
        registered = true
    }

    fun stop() {
        if (!registered) return
        callback?.let { runCatching { cm.unregisterNetworkCallback(it) } }
        callback = null
        registered = false
        network = null
        callbackSsid = null
    }

    fun hasIpv4(n: Network): Boolean =
        cm.getLinkProperties(n)?.linkAddresses?.any { it.address is Inet4Address } == true

    /** Current Wi-Fi name, or null when Android hides it. */
    @Suppress("DEPRECATION")
    fun ssid(): String? {
        callbackSsid?.let { return it }
        var raw: String? = null
        try {
            raw = wm.connectionInfo?.ssid
        } catch (_: Exception) {
        }
        return cleanSsid(raw)
    }
}
