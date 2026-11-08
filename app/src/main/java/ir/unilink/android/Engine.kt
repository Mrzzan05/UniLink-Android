package ir.unilink.android

import android.content.Context
import android.net.Network
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.YearMonth
import java.util.Date
import java.util.Locale

enum class Phase { Stopped, Paused, WaitingWifi, NeedCertificate, Connecting, Connected, WaitingRetry, AccountNeeded }

data class CertPrompt(
    val groupId: String,
    val groupName: String,
    val url: String,
    val fingerprint: String,
    val subject: String,
    val changed: Boolean,
)

data class EngineStatus(
    val phase: Phase = Phase.Stopped,
    val title: String = "Stopped",
    val detail: String = "",
    val groupName: String? = null,
    val accountId: String? = null,
    val accountName: String? = null,
    val lastKeepalive: Long = 0L,
    val nextCheck: Long = 0L,
    val pendingCert: CertPrompt? = null,
)

/**
 * Sign-in / keepalive state machine. Accounts are tried strictly one at a time, in list order.
 * Only a recognised quota message marks an account exhausted; network errors, timeouts and unknown
 * replies are retried with back-off and never burn an account.
 */
class Engine(private val ctx: Context, private val store: Store) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val opLock = Mutex()

    private val _status = MutableStateFlow(EngineStatus())
    val status: StateFlow<EngineStatus> = _status.asStateFlow()

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val logLines: StateFlow<List<String>> = _log.asStateFlow()

    private var job: Job? = null
    private var tracker: WifiTracker? = null

    @Volatile private var paused = false
    @Volatile private var userRequested = false
    @Volatile private var retryAt = 0L
    private var lastSignature = ""
    private var lastMatchNote = ""
    private var failures = 0
    private var nextKeepalive = 0L
    private var lastKeepalive = 0L
    private var session: Session? = null

    private class Session(
        val groupId: String,
        val accountId: String,
        val accountName: String,
        val username: String,
        val url: String,
        val netHandle: Long,
    )

    private sealed class Outcome {
        class LoggedIn(val account: Account, val url: String) : Outcome()
        class NeedCert(val prompt: CertPrompt) : Outcome()
        object NoAccounts : Outcome()
        class Retry(val reason: String, val minSeconds: Int) : Outcome()
    }

    val isRunning: Boolean get() = job?.isActive == true

    // ---------- public controls ----------

    @Synchronized
    fun start() {
        if (job?.isActive == true) return
        val t = WifiTracker(ctx) { onNetworkEvent() }
        tracker = t
        paused = false
        t.start()
        job = scope.launch { loop(t) }
        log("Service started")
    }

    @Synchronized
    fun stop() {
        job?.cancel()
        job = null
        tracker?.stop()
        tracker = null
        session = null
        _status.value = EngineStatus()
        log("Service stopped")
    }

    /** User pressed Connect: resume if paused, skip any back-off and try right away. */
    fun connectNow() {
        paused = false
        userRequested = true
        failures = 0
        retryAt = 0L
        wake.trySend(Unit)
    }

    fun kick() {
        retryAt = 0L
        wake.trySend(Unit)
    }

    fun pauseAndSignOut() {
        paused = true
        userRequested = false
        scope.launch {
            val s = session
            session = null
            val net = tracker?.network
            val g = s?.let { x -> store.data.value.groups.firstOrNull { it.id == x.groupId } }
            if (s != null && net != null && g != null) {
                val ok = opLock.withLock { PortalClient(net, g.pins[s.url]).logout(s.url, s.username) }
                log(if (ok) "Signed out" else "Sign-out request failed")
            }
            setStatus(Phase.Paused, "Paused", "Signed out. Tap Connect to resume.")
        }
        wake.trySend(Unit)
    }

    fun approveCertificate(p: CertPrompt) {
        store.updateGroup(p.groupId) { it.copy(pins = it.pins + (p.url to p.fingerprint)) }
        _status.update { it.copy(pendingCert = null) }
        log("Certificate approved for ${p.url} (${p.fingerprint.take(11)}…)")
        failures = 0
        retryAt = 0L
        wake.trySend(Unit)
    }

    // ---------- main loop ----------

    private fun onNetworkEvent() {
        val t = tracker ?: return
        val n = t.network
        val sig = "${n?.networkHandle}|${n?.let { t.hasIpv4(it) }}|${t.ssid()}"
        if (sig != lastSignature) {
            lastSignature = sig
            failures = 0
            retryAt = 0L
            wake.trySend(Unit)
        }
    }

    private suspend fun sleep(ms: Long) {
        withTimeoutOrNull(ms.coerceAtLeast(500L)) { wake.receive() }
    }

    private suspend fun loop(t: WifiTracker) {
        while (currentCoroutineContext().isActive) {
            try {
                step(t)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log("Internal error: ${e.javaClass.simpleName}: ${e.message}")
                sleep(10_000)
            }
        }
    }

    private suspend fun step(t: WifiTracker) {
        val now = System.currentTimeMillis()
        if (paused) {
            setStatus(Phase.Paused, "Paused", "Signed out. Tap Connect to resume.")
            sleep(300_000)
            return
        }
        val net = t.network
        if (net == null || !t.hasIpv4(net)) {
            dropSession("Wi-Fi unavailable")
            userRequested = false
            setStatus(Phase.WaitingWifi, "Waiting for campus Wi-Fi", "No Wi-Fi connection with an IP address yet.")
            sleep(60_000)
            return
        }
        val ssid = t.ssid()
        val data = store.data.value
        val group = matchGroup(data.groups, ssid)
        val matchNote = "SSID=${ssid ?: "hidden"} group=${group?.name ?: "none"}"
        if (matchNote != lastMatchNote) {
            lastMatchNote = matchNote
            log("Wi-Fi detected: $matchNote")
        }
        if (group == null) {
            dropSession(null)
            val why = if (ssid == null)
                "The Wi-Fi name is hidden by Android. Allow location access on the Connection tab, or turn on portal-only matching for a network."
            else "\"$ssid\" is not in any configured network."
            setStatus(Phase.WaitingWifi, "Waiting for campus Wi-Fi", why)
            sleep(60_000)
            return
        }
        if (!data.settings.autoSignIn && !userRequested) {
            setStatus(Phase.WaitingWifi, "Automatic sign-in is off", "Tap Connect to sign in to ${group.name}.", group.name)
            sleep(60_000)
            return
        }

        val s = session
        if (s != null && (s.groupId != group.id || s.netHandle != net.networkHandle)) {
            log("Network changed; session discarded")
            session = null
        }

        if (session == null) {
            if (now < retryAt) {
                sleep(retryAt - now)
                return
            }
            doLogin(group, net)
            return
        }
        if (now < nextKeepalive) {
            sleep(nextKeepalive - now)
            return
        }
        doKeepalive(group, net)
    }

    private fun matchGroup(groups: List<NetworkGroup>, ssid: String?): NetworkGroup? {
        if (ssid != null) groups.firstOrNull { g -> g.ssids.any { it == ssid } }?.let { return it }
        groups.firstOrNull { it.portalOnly }?.let { return it }
        // Android sometimes hides the Wi-Fi name even with location permission. With a single
        // configured network, try its portal anyway: nothing is sent without an approved
        // (pinned) certificate, and an unreachable portal just fails and is retried later.
        if (ssid == null && groups.size == 1) return groups[0]
        return null
    }

    private fun dropSession(reason: String?) {
        if (session != null) {
            if (reason != null) log("Session dropped: $reason")
            session = null
        }
    }

    // ---------- login ----------

    private suspend fun doLogin(g: NetworkGroup, net: Network) {
        setStatus(Phase.Connecting, "Signing in…", g.name, g.name)
        when (val o = opLock.withLock { attemptLogin(g, net) }) {
            is Outcome.LoggedIn -> {
                val now = System.currentTimeMillis()
                session = Session(g.id, o.account.id, o.account.name, o.account.username, o.url, net.networkHandle)
                failures = 0
                retryAt = 0L
                lastKeepalive = now
                nextKeepalive = now + intervalMs(g)
                log("Signed in with \"${o.account.name}\" on ${g.name}")
                setConnected(g)
            }
            is Outcome.NeedCert -> {
                retryAt = System.currentTimeMillis() + 60_000
                log(
                    if (o.prompt.changed) "Certificate for ${o.prompt.url} changed — review required (no password sent)"
                    else "Certificate for ${o.prompt.url} not approved yet — review required (no password sent)"
                )
                _status.value = EngineStatus(
                    phase = Phase.NeedCertificate,
                    title = "Review certificate",
                    detail = if (o.prompt.changed) "The portal certificate changed. No password has been sent."
                    else "Approve the portal certificate once. No password has been sent.",
                    groupName = g.name,
                    pendingCert = o.prompt,
                )
            }
            Outcome.NoAccounts -> {
                val now = System.currentTimeMillis()
                _status.value = EngineStatus(
                    phase = Phase.AccountNeeded,
                    title = "Account needed",
                    detail = "No usable account in ${g.name}: none added, all disabled, quota used up or password rejected.",
                    groupName = g.name,
                    nextCheck = now + 60_000,
                )
                retryAt = now + 60_000
            }
            is Outcome.Retry -> {
                failures++
                val now = System.currentTimeMillis()
                val seconds = maxOf(o.minSeconds, backoffSeconds(failures))
                retryAt = now + seconds * 1000L
                log("Retry in ${seconds}s: ${o.reason}")
                _status.value = EngineStatus(
                    phase = Phase.WaitingRetry,
                    title = "Waiting to retry",
                    detail = o.reason,
                    groupName = g.name,
                    nextCheck = retryAt,
                )
            }
        }
    }

    private suspend fun attemptLogin(g: NetworkGroup, net: Network): Outcome {
        val now = System.currentTimeMillis()
        val candidates = g.accounts.filter { it.isUsable(g, now) }
        if (candidates.isEmpty()) return Outcome.NoAccounts

        for (acc in candidates) {
            var result: PortalResult? = null
            var usedUrl = ""
            var lastFailure = "No portal reachable"
            for (url in g.portalUrls) {
                val r = PortalClient(net, g.pins[url]).login(url, acc.username, acc.password, g.quotaPhrases)
                if (r is PortalResult.Failure) {
                    lastFailure = r.reason
                    log("Portal $url: ${r.reason}")
                    continue
                }
                result = r
                usedUrl = url
                break
            }
            when (val r = result) {
                null -> return Outcome.Retry(lastFailure, 5)
                is PortalResult.Success -> return Outcome.LoggedIn(acc, usedUrl)
                is PortalResult.Quota -> {
                    markExhausted(g, acc)
                    log("\"${acc.name}\": data allowance used up this month — trying next account")
                    delay(3_000)
                }
                is PortalResult.BadCredentials -> {
                    store.updateAccount(g.id, acc.id) { it.copy(passwordRejected = true) }
                    log("\"${acc.name}\": password rejected — account blocked until edited")
                }
                is PortalResult.CertReview ->
                    return Outcome.NeedCert(CertPrompt(g.id, g.name, usedUrl, r.fingerprint, r.subject, r.changed))
                is PortalResult.Rejected ->
                    return Outcome.Retry("Unrecognized portal reply: ${r.message.take(120)}", 300)
                is PortalResult.Failure -> return Outcome.Retry(r.reason, 5)
            }
        }
        return Outcome.NoAccounts
    }

    private fun markExhausted(g: NetworkGroup, acc: Account) {
        val now = System.currentTimeMillis()
        val month = YearMonth.from(Instant.ofEpochMilli(now).atZone(g.zone())).toString()
        store.updateAccount(g.id, acc.id) { it.copy(exhaustedMonth = month, exhaustedAt = now) }
    }

    // ---------- keepalive ----------

    private suspend fun doKeepalive(g: NetworkGroup, net: Network) {
        val s = session ?: return
        val r = opLock.withLock {
            PortalClient(net, g.pins[s.url]).keepalive(s.url, s.username, g.quotaPhrases)
        }
        val now = System.currentTimeMillis()
        when (r) {
            is PortalResult.Success -> {
                failures = 0
                lastKeepalive = now
                nextKeepalive = now + intervalMs(g)
                setConnected(g)
            }
            is PortalResult.Quota -> {
                g.accounts.firstOrNull { it.id == s.accountId }?.let { markExhausted(g, it) }
                log("\"${s.accountName}\": data allowance used up — switching account")
                session = null
                retryAt = now + 3_000
            }
            is PortalResult.CertReview -> {
                session = null
                retryAt = now + 60_000
                log("Certificate changed during keepalive — review required")
                _status.value = EngineStatus(
                    phase = Phase.NeedCertificate,
                    title = "Review certificate",
                    detail = "The portal certificate changed. No password has been sent.",
                    groupName = g.name,
                    pendingCert = CertPrompt(g.id, g.name, s.url, r.fingerprint, r.subject, r.changed),
                )
            }
            is PortalResult.Rejected, is PortalResult.BadCredentials -> {
                log("Session is no longer live — signing in again")
                session = null
                retryAt = 0L
            }
            is PortalResult.Failure -> {
                failures++
                val wait = minOf(backoffSeconds(failures) * 1000L, intervalMs(g))
                nextKeepalive = now + wait
                log("Keepalive failed (#$failures): ${r.reason}")
                if (failures >= 5) {
                    session = null
                    retryAt = 0L
                    log("Too many keepalive failures — signing in again")
                } else {
                    _status.value = EngineStatus(
                        phase = Phase.WaitingRetry,
                        title = "Waiting to retry",
                        detail = r.reason,
                        groupName = g.name,
                        accountId = s.accountId,
                        accountName = s.accountName,
                        lastKeepalive = lastKeepalive,
                        nextCheck = nextKeepalive,
                    )
                }
            }
        }
    }

    // ---------- helpers ----------

    private fun intervalMs(g: NetworkGroup) = g.keepaliveSec.coerceIn(30, 180) * 1000L

    private fun backoffSeconds(n: Int): Int {
        val table = intArrayOf(5, 15, 30, 60, 120, 300, 600, 900)
        return table[(n - 1).coerceIn(0, table.lastIndex)]
    }

    private fun setConnected(g: NetworkGroup) {
        val s = session ?: return
        _status.value = EngineStatus(
            phase = Phase.Connected,
            title = "Connected",
            detail = "Signed in as \"${s.accountName}\" on ${g.name}.",
            groupName = g.name,
            accountId = s.accountId,
            accountName = s.accountName,
            lastKeepalive = lastKeepalive,
            nextCheck = nextKeepalive,
        )
    }

    private fun setStatus(phase: Phase, title: String, detail: String, group: String? = null) {
        val cur = _status.value
        if (cur.phase == phase && cur.title == title && cur.detail == detail && cur.groupName == group) return
        _status.value = EngineStatus(phase = phase, title = title, detail = detail, groupName = group)
    }

    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun log(message: String) {
        val line = "${timeFmt.format(Date())}  $message"
        _log.update { (it + line).takeLast(300) }
    }

    fun clearLog() {
        _log.value = emptyList()
    }
}
