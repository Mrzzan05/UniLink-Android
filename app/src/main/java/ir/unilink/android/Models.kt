package ir.unilink.android

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

/** One university account. The first account in a group's list is its default. */
data class Account(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val username: String,
    val password: String,
    val enabled: Boolean = true,
    /** "yyyy-MM" (in the group's time zone) of the month the quota ran out, or null. */
    val exhaustedMonth: String? = null,
    val exhaustedAt: Long = 0L,
    val passwordRejected: Boolean = false,
)

data class NetworkGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ssids: List<String>,
    val portalUrls: List<String>,
    /** Also match when the Wi-Fi name is unknown or different, relying on the pinned portal. */
    val portalOnly: Boolean = false,
    val keepaliveSec: Int = 180,
    val timeZone: String = DEFAULT_TZ,
    val quotaPhrases: List<String> = emptyList(),
    /** portal base URL -> SHA-256 fingerprint of the approved certificate. */
    val pins: Map<String, String> = emptyMap(),
    val accounts: List<Account> = emptyList(),
) {
    companion object {
        const val DEFAULT_TZ = "Asia/Tehran"
    }

    fun zone(): ZoneId = runCatching { ZoneId.of(timeZone) }.getOrDefault(ZoneId.of(DEFAULT_TZ))
}

data class Settings(
    val autoSignIn: Boolean = true,
    val startOnBoot: Boolean = true,
    val holdWakeLock: Boolean = true,
)

data class AppData(
    val groups: List<NetworkGroup>,
    val settings: Settings = Settings(),
) {
    companion object {
        fun default() = AppData(
            groups = listOf(
                NetworkGroup(
                    name = "Urmia University",
                    ssids = listOf("UUWN"),
                    portalUrls = listOf("https://192.168.100.40:8090/"),
                )
            )
        )
    }
}

/** True while the account is excluded because its monthly quota ran out. */
fun Account.isExhaustedNow(group: NetworkGroup, now: Long): Boolean {
    val month = exhaustedMonth ?: return false
    val zone = group.zone()
    val current = YearMonth.from(Instant.ofEpochMilli(now).atZone(zone)).toString()
    if (month != current) return false // new month: limit lifted automatically
    // Day-one probe: if it was marked on the 1st and it is still the 1st, allow an hourly re-check
    // (the portal may not have renewed the quota yet).
    val markedDay = Instant.ofEpochMilli(exhaustedAt).atZone(zone).dayOfMonth
    val nowDay = Instant.ofEpochMilli(now).atZone(zone).dayOfMonth
    return !(markedDay == 1 && nowDay == 1 && now - exhaustedAt >= 3_600_000L)
}

fun Account.isUsable(group: NetworkGroup, now: Long): Boolean =
    enabled && !passwordRejected && !isExhaustedNow(group, now)

fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    val m = toMutableList()
    val item = m.removeAt(from)
    m.add(to, item)
    return m
}

// ---------- JSON ----------

private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
private fun List<String>.toJson() = JSONArray().also { a -> forEach { a.put(it) } }

fun Account.toJson() = JSONObject()
    .put("id", id).put("name", name).put("username", username).put("password", password)
    .put("enabled", enabled).put("exhaustedMonth", exhaustedMonth ?: "")
    .put("exhaustedAt", exhaustedAt).put("passwordRejected", passwordRejected)

fun accountFromJson(o: JSONObject) = Account(
    id = o.optString("id").ifEmpty { UUID.randomUUID().toString() },
    name = o.optString("name"),
    username = o.optString("username"),
    password = o.optString("password"),
    enabled = o.optBoolean("enabled", true),
    exhaustedMonth = o.optString("exhaustedMonth", "").ifEmpty { null },
    exhaustedAt = o.optLong("exhaustedAt", 0L),
    passwordRejected = o.optBoolean("passwordRejected", false),
)

fun NetworkGroup.toJson(): JSONObject {
    val pinObj = JSONObject()
    pins.forEach { (k, v) -> pinObj.put(k, v) }
    val accs = JSONArray()
    accounts.forEach { accs.put(it.toJson()) }
    return JSONObject()
        .put("id", id).put("name", name)
        .put("ssids", ssids.toJson()).put("portalUrls", portalUrls.toJson())
        .put("portalOnly", portalOnly).put("keepaliveSec", keepaliveSec)
        .put("timeZone", timeZone).put("quotaPhrases", quotaPhrases.toJson())
        .put("pins", pinObj).put("accounts", accs)
}

fun groupFromJson(o: JSONObject): NetworkGroup {
    val pinObj = o.optJSONObject("pins") ?: JSONObject()
    val pins = pinObj.keys().asSequence().associateWith { pinObj.getString(it) }
    val accArr = o.optJSONArray("accounts") ?: JSONArray()
    return NetworkGroup(
        id = o.optString("id").ifEmpty { UUID.randomUUID().toString() },
        name = o.optString("name"),
        ssids = (o.optJSONArray("ssids") ?: JSONArray()).strings(),
        portalUrls = (o.optJSONArray("portalUrls") ?: JSONArray()).strings(),
        portalOnly = o.optBoolean("portalOnly", false),
        keepaliveSec = o.optInt("keepaliveSec", 180),
        timeZone = o.optString("timeZone", NetworkGroup.DEFAULT_TZ),
        quotaPhrases = (o.optJSONArray("quotaPhrases") ?: JSONArray()).strings(),
        pins = pins,
        accounts = (0 until accArr.length()).map { accountFromJson(accArr.getJSONObject(it)) },
    )
}

fun AppData.toJson(): JSONObject {
    val g = JSONArray()
    groups.forEach { g.put(it.toJson()) }
    return JSONObject().put("version", 1).put("groups", g).put(
        "settings",
        JSONObject()
            .put("autoSignIn", settings.autoSignIn)
            .put("startOnBoot", settings.startOnBoot)
            .put("holdWakeLock", settings.holdWakeLock)
    )
}

fun appDataFromJson(o: JSONObject): AppData {
    val arr = o.optJSONArray("groups") ?: JSONArray()
    val s = o.optJSONObject("settings") ?: JSONObject()
    return AppData(
        groups = (0 until arr.length()).map { groupFromJson(arr.getJSONObject(it)) },
        settings = Settings(
            autoSignIn = s.optBoolean("autoSignIn", true),
            startOnBoot = s.optBoolean("startOnBoot", true),
            holdWakeLock = s.optBoolean("holdWakeLock", true),
        ),
    )
}
