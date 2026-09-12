package io.github.cloolalang.notspotdetector.model

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager

/** User network-mode preference from telephony (API 31+) or Settings.Global fallback. */
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

fun readNetworkModePreference(
    telephonyManager: TelephonyManager,
    context: Context? = null
): NetworkModePreference {
    readAllowedNetworkTypesForUser(telephonyManager)?.let { return it }
    if (context != null) {
        readPreferredNetworkModeFromSettings(context, telephonyManager)?.let { return it }
    }
    return NetworkModePreference.UNKNOWN
}

private fun readAllowedNetworkTypesForUser(
    telephonyManager: TelephonyManager
): NetworkModePreference? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    return runCatching {
        val allowed = telephonyManager.getAllowedNetworkTypesForReason(
            TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER
        )
        parseNetworkModeFromAllowedBitmask(allowed)
    }.getOrNull()
}

/**
 * `getAllowedNetworkTypesForReason(USER)` often throws on consumer apps (carrier privileges).
 * AOSP still stores the user's preferred mode in Settings.Global.
 */
private fun readPreferredNetworkModeFromSettings(
    context: Context,
    telephonyManager: TelephonyManager
): NetworkModePreference? {
    val resolver = context.contentResolver
    val subId = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            telephonyManager.subscriptionId
        } else {
            null
        }
    }.getOrNull()?.takeIf { it >= 0 }

    val preferredKeys = buildList {
        if (subId != null) {
            add("preferred_network_mode$subId")
            add("preferred_network_mode_$subId")
        }
        add("preferred_network_mode")
    }
    for (key in preferredKeys) {
        val mode = runCatching { Settings.Global.getInt(resolver, key, Int.MIN_VALUE) }
            .getOrDefault(Int.MIN_VALUE)
        if (mode != Int.MIN_VALUE) {
            return parsePreferredNetworkModeSetting(mode)
        }
    }

    if (subId != null) {
        val bitmask = runCatching {
            Settings.Global.getLong(resolver, "allowed_network_types_user$subId", Long.MIN_VALUE)
        }.getOrDefault(Long.MIN_VALUE)
        if (bitmask != Long.MIN_VALUE) {
            return parseNetworkModeFromAllowedBitmask(bitmask)
        }
    }
    return null
}

/**
 * AOSP RIL `preferred_network_mode` integers (see `RILConstants`).
 * GSM-only is forced 2G; NR-only is forced 5G; LTE/NR without GSM is 4G/5G (no 2G).
 */
fun parsePreferredNetworkModeSetting(mode: Int): NetworkModePreference {
    return when (mode) {
        1 -> NetworkModePreference.FORCED_2G
        11, 12, 15, 19, 8 -> NetworkModePreference.FORCED_LTE_NR
        23 -> NetworkModePreference.FORCED_NR_ONLY
        24, 25, 28, 29, 31 -> NetworkModePreference.FORCED_LTE_NR
        else -> if (mode < 0) {
            NetworkModePreference.UNKNOWN
        } else {
            NetworkModePreference.ALL_TECHNOLOGIES
        }
    }
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
        // WiFi calling can report IN_SERVICE with no cellular RAT/RSRP, which otherwise satisfies
        // every condition above and gets misclassified as "searching for home 2G" (RXSS 11).
        !stats.isWifiCallingActive &&
        SignalStateAnnouncement.isLteNrRadioAccessType(lteRatBeforeNoSignalEpisode)
}
