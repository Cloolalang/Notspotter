package io.github.cloolalang.notspotdetector.model

private val NON_DATA_APN_EXTRAS = setOf(
    "ims",
    "sos",
    "emergency",
    "xcap",
    "cbs",
    "mms",
    "ia",
    "bip",
    "null",
    "unknown",
    "[unknown]"
)

private val NON_DATA_APN_TYPES = setOf(
    "ims",
    "sos",
    "emergency",
    "xcap",
    "cbs",
    "mms",
    "ia",
    "bip"
)

data class ApnCandidate(
    val name: String?,
    val apn: String?,
    val type: String? = null,
    val current: Boolean = false,
    val enabled: Boolean = true
)

/** APN host from [android.net.NetworkInfo.getExtraInfo], or null if it is not a data APN. */
fun sanitizeApnExtra(extra: String?): String? {
    val value = extra?.trim()?.trim('"')?.takeIf { it.isNotBlank() } ?: return null
    if (value.equals("null", ignoreCase = true)) return null
    if (NON_DATA_APN_EXTRAS.any { it.equals(value, ignoreCase = true) }) return null
    return value
}

fun formatSelectedApnDisplay(name: String?, apn: String?): String? {
    val profile = name?.trim()?.takeIf { it.isNotBlank() }
    val host = apn?.trim()?.takeIf { it.isNotBlank() }
    return when {
        profile != null && host != null && !profile.equals(host, ignoreCase = true) ->
            "$profile · $host"
        profile != null -> profile
        host != null -> host
        else -> null
    }
}

/** APN from the telephony data-state broadcast (`apn` / `apnType`). */
fun apnFromDataConnectionState(
    apn: String?,
    apnType: String?,
    subscriptionId: Int? = null,
    wantedSubscriptionId: Int? = null
): String? {
    if (wantedSubscriptionId != null &&
        subscriptionId != null &&
        subscriptionId != wantedSubscriptionId
    ) {
        return null
    }
    if (!isUsableDataApnType(apnType)) return null
    return sanitizeApnExtra(apn)
}

fun pickPreferredApnDisplay(candidates: List<ApnCandidate>): String? {
    return candidates
        .mapNotNull { candidate ->
            val display = formatSelectedApnDisplay(candidate.name, candidate.apn)
                ?: return@mapNotNull null
            val score = scoreDataApn(candidate.type, candidate.current, candidate.enabled)
            if (score < 0) null else display to score
        }
        .maxByOrNull { it.second }
        ?.first
}

internal fun isUsableDataApnType(apnType: String?): Boolean {
    val types = parseApnTypes(apnType)
    if (types.isEmpty()) return true
    if (types.any { it == "default" || it == "hipri" || it == "dun" || it == "internet" || it == "*" }) {
        return true
    }
    return types.any { it !in NON_DATA_APN_TYPES }
}

private fun scoreDataApn(type: String?, current: Boolean, enabled: Boolean): Int {
    val types = parseApnTypes(type)
    if (types.isNotEmpty() && types.all { it in NON_DATA_APN_TYPES }) return -1
    var score = 0
    if (types.isEmpty() || types.any { it == "default" || it == "*" || it == "internet" }) score += 10
    if (types.any { it == "hipri" || it == "dun" }) score += 4
    if (current) score += 20
    if (enabled) score += 2
    return score
}

private fun parseApnTypes(raw: String?): List<String> {
    return raw.orEmpty()
        .split(',', '+')
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
}
