package io.github.cloolalang.notspotdetector.model

import android.os.Build
import android.telephony.TelephonyManager

/** User network-mode preference from telephony (API 31+). */
enum class NetworkModePreference {
    ALL_TECHNOLOGIES,
    FORCED_2G,
    /** 2G excluded — LTE and/or NR only. */
    FORCED_LTE_NR,
    FORCED_NR_ONLY,
    UNKNOWN;

    fun allows2gFallbackScan(): Boolean = when (this) {
        FORCED_LTE_NR, FORCED_NR_ONLY -> false
        else -> true
    }
}

fun parseNetworkModeFromAllowedBitmask(allowed: Long): NetworkModePreference {
    if (allowed == 0L) return NetworkModePreference.ALL_TECHNOLOGIES

    val twoGMask = (
        TelephonyManager.NETWORK_TYPE_BITMASK_GSM or
            TelephonyManager.NETWORK_TYPE_BITMASK_GPRS or
            TelephonyManager.NETWORK_TYPE_BITMASK_EDGE
        ).toLong()
    val lteMask = (
        TelephonyManager.NETWORK_TYPE_BITMASK_LTE or
            TelephonyManager.NETWORK_TYPE_BITMASK_LTE_CA
        ).toLong()
    val nrMask = TelephonyManager.NETWORK_TYPE_BITMASK_NR.toLong()

    val has2g = allowed and twoGMask != 0L
    val hasLte = allowed and lteMask != 0L
    val hasNr = allowed and nrMask != 0L

    return when {
        has2g && !hasLte && !hasNr -> NetworkModePreference.FORCED_2G
        !has2g && hasNr && !hasLte -> NetworkModePreference.FORCED_NR_ONLY
        !has2g && (hasLte || hasNr) -> NetworkModePreference.FORCED_LTE_NR
        else -> NetworkModePreference.ALL_TECHNOLOGIES
    }
}

fun readNetworkModePreference(telephonyManager: TelephonyManager): NetworkModePreference {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return NetworkModePreference.UNKNOWN
    }
    return runCatching {
        val allowed = telephonyManager.getAllowedNetworkTypesForReason(
            TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER
        )
        parseNetworkModeFromAllowedBitmask(allowed)
    }.getOrDefault(NetworkModePreference.UNKNOWN)
}

/** True while LTE/NR is lost and the phone may still camp on home 2G (all-tech mode). */
fun computeSearching2gFallbackActive(
    stats: ConnectivityStats,
    lteRatBeforeNoSignalEpisode: String?
): Boolean {
    return stats.noSignalActive &&
        stats.monitor2gFallbackEnabled &&
        stats.networkModePreference.allows2gFallbackScan() &&
        !stats.isOn2g &&
        !stats.isCompleteNoService &&
        !stats.isLimitedService &&
        SignalStateAnnouncement.isLteNrRadioAccessType(lteRatBeforeNoSignalEpisode)
}
