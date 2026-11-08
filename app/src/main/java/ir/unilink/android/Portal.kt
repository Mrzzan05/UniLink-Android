package ir.unilink.android

import android.net.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.ConnectionSpec
import okhttp3.Dns
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.Proxy
import java.net.UnknownHostException
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

sealed class PortalResult {
    data class Success(val message: String) : PortalResult()
    /** The portal says the monthly data allowance is used up. */
    data class Quota(val message: String) : PortalResult()
    data class BadCredentials(val message: String) : PortalResult()
    /** Parsed portal reply that is negative but not recognised (not quota, not a password error). */
    data class Rejected(val message: String) : PortalResult()
    /** The server certificate is not pinned yet, or differs from the pinned one. No password was sent. */
    data class CertReview(val fingerprint: String, val subject: String, val changed: Boolean) : PortalResult()
    /** Transport problem, timeout, HTTP error or a non-portal reply. */
    data class Failure(val reason: String) : PortalResult()
}

/** Pure parsing/classification of Sophos / Cyberoam portal XML replies. No Android dependencies. */
object PortalParser {
    const val BUILTIN_QUOTA = "Your data transfer has been exceeded"
    private val BAD_CREDENTIALS = listOf(
        "could not log you on", "password is correct", "invalid user", "authentication failed"
    )

    fun tag(xml: String, name: String): String? {
        val re = Regex(
            "<$name>\\s*(?:<!\\[CDATA\\[(.*?)\\]\\]>|([^<]*))\\s*</$name>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val m = re.find(xml) ?: return null
        return (m.groups[1]?.value ?: m.groups[2]?.value ?: "").trim()
    }

    fun classify(xml: String, isLogin: Boolean, extraQuotaPhrases: List<String>): PortalResult {
        val status = tag(xml, "status").orEmpty()
        val ack = tag(xml, "ack").orEmpty()
        val message = tag(xml, "message").orEmpty()

        // A genuine positive status always wins over any message text.
        if (status.equals("LIVE", ignoreCase = true) || (!isLogin && ack.equals("ack", ignoreCase = true))) {
            return PortalResult.Success(message.ifBlank { "OK" })
        }
        if (status.isBlank() && ack.isBlank() && message.isBlank()) {
            return PortalResult.Failure("Unrecognized reply (not portal XML)")
        }
        val phrases = listOf(BUILTIN_QUOTA) + extraQuotaPhrases.filter { it.isNotBlank() }
        if (phrases.any { xml.contains(it, ignoreCase = true) }) {
            return PortalResult.Quota(message.ifBlank { BUILTIN_QUOTA })
        }
        if (isLogin && BAD_CREDENTIALS.any { xml.contains(it, ignoreCase = true) }) {
            return PortalResult.BadCredentials(message)
        }
        return PortalResult.Rejected(message.ifBlank { status.ifBlank { ack } })
    }
}

private class Capture {
    @Volatile var fingerprint: String? = null
    @Volatile var subject: String? = null
}

/** Accepts only the certificate whose SHA-256 fingerprint was approved by the user. */
private class PinTrust(private val pin: String?, private val cap: Capture) : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        throw CertificateException("Client certificates are not supported")
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        val leaf = chain?.firstOrNull() ?: throw CertificateException("Empty certificate chain")
        val fp = sha256Hex(leaf.encoded)
        cap.fingerprint = fp
        cap.subject = leaf.subjectX500Principal.name
        if (pin == null || !fp.equals(pin, ignoreCase = true)) {
            throw CertificateException("Certificate is not pinned")
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}

fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(":") { "%02X".format(it) }

/**
 * Talks to the portal strictly over the given Wi-Fi [Network]: sockets and DNS are bound to it,
 * the system proxy is ignored, redirects are refused and the certificate must match the pin.
 */
class PortalClient(private val network: Network, private val pin: String?) {

    private fun buildClient(cap: Capture): OkHttpClient {
        val trust = PinTrust(pin, cap)
        val ssl = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trust), SecureRandom())
        }
        val dns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val v4 = network.getAllByName(hostname).filterIsInstance<Inet4Address>()
                if (v4.isEmpty()) throw UnknownHostException(hostname)
                return v4
            }
        }
        return OkHttpClient.Builder()
            .socketFactory(network.socketFactory)
            .dns(dns)
            .proxy(Proxy.NO_PROXY)
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .connectionPool(ConnectionPool(0, 1, TimeUnit.NANOSECONDS))
            .connectionSpecs(listOf(ConnectionSpec.COMPATIBLE_TLS))
            .sslSocketFactory(ssl.socketFactory, trust)
            .hostnameVerifier { _, _ -> true } // identity is established by the pinned fingerprint
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    private fun execute(request: Request, isLogin: Boolean, extra: List<String>): PortalResult {
        val cap = Capture()
        return try {
            buildClient(cap).newCall(request).execute().use { resp ->
                if (resp.isRedirect) return PortalResult.Failure("Redirect rejected by policy")
                val body = resp.peekBody(65536).string()
                val result = PortalParser.classify(body, isLogin, extra)
                if (result is PortalResult.Failure && !resp.isSuccessful) {
                    PortalResult.Failure("HTTP ${resp.code}")
                } else result
            }
        } catch (e: IOException) {
            val fp = cap.fingerprint
            if (fp != null && !fp.equals(pin, ignoreCase = true)) {
                PortalResult.CertReview(fp, cap.subject.orEmpty(), changed = pin != null)
            } else {
                PortalResult.Failure(e.message ?: e.javaClass.simpleName)
            }
        } catch (e: Exception) {
            PortalResult.Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun endpoint(base: String, path: String) =
        (base.trim().trimEnd('/') + "/" + path).toHttpUrlOrNull()

    suspend fun login(base: String, username: String, password: String, extra: List<String>): PortalResult =
        withContext(Dispatchers.IO) {
            val url = endpoint(base, "login.xml") ?: return@withContext PortalResult.Failure("Invalid portal URL")
            val body = FormBody.Builder()
                .add("mode", "191")
                .add("username", username)
                .add("password", password)
                .add("a", System.currentTimeMillis().toString())
                .add("producttype", "0")
                .build()
            execute(Request.Builder().url(url).post(body).build(), isLogin = true, extra = extra)
        }

    suspend fun keepalive(base: String, username: String, extra: List<String>): PortalResult =
        withContext(Dispatchers.IO) {
            val url = endpoint(base, "live")?.newBuilder()
                ?.addQueryParameter("mode", "192")
                ?.addQueryParameter("username", username)
                ?.addQueryParameter("a", System.currentTimeMillis().toString())
                ?.addQueryParameter("producttype", "0")
                ?.build()
                ?: return@withContext PortalResult.Failure("Invalid portal URL")
            execute(Request.Builder().url(url).get().build(), isLogin = false, extra = extra)
        }

    suspend fun logout(base: String, username: String): Boolean = withContext(Dispatchers.IO) {
        val url = endpoint(base, "logout.xml") ?: return@withContext false
        val body = FormBody.Builder()
            .add("mode", "193")
            .add("username", username)
            .add("a", System.currentTimeMillis().toString())
            .add("producttype", "0")
            .build()
        execute(Request.Builder().url(url).post(body).build(), isLogin = false, extra = emptyList()) !is PortalResult.Failure
    }
}
