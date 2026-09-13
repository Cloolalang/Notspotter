package io.github.cloolalang.notspotdetector.model

import android.annotation.SuppressLint
import android.os.Build
import android.telephony.ServiceState
import android.telephony.TelephonyManager

/** SIM network-operator selection from the phone’s operator list (auto vs a locked PLMN). */
enum class SimOperatorSelectionMode {
    AUTO,
    MANUAL,
    UNKNOWN
}

@SuppressLint("MissingPermission")
fun readSimOperatorSelectionMode(
    telephonyManager: TelephonyManager,
    serviceState: ServiceState?
): SimOperatorSelectionMode {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val mode = runCatching { telephonyManager.networkSelectionMode }.getOrNull()
        when (mode) {
            TelephonyManager.NETWORK_SELECTION_MODE_AUTO -> return SimOperatorSelectionMode.AUTO
            TelephonyManager.NETWORK_SELECTION_MODE_MANUAL -> return SimOperatorSelectionMode.MANUAL
        }
    }
    if (serviceState == null) return SimOperatorSelectionMode.UNKNOWN
    return if (serviceState.isManualSelection) {
        SimOperatorSelectionMode.MANUAL
    } else {
        SimOperatorSelectionMode.AUTO
    }
}

@SuppressLint("MissingPermission")
fun readManualNetworkSelectionPlmn(telephonyManager: TelephonyManager): String? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
    return runCatching { telephonyManager.manualNetworkSelectionPlmn }
        .getOrNull()
        ?.filter { it.isDigit() }
        ?.takeIf { it.length >= 5 }
}

fun resolveManualSelectedOperatorName(
    selectedPlmn: String?,
    homePlmn: String?,
    servingPlmn: String?,
    homeOperatorName: String?,
    servingOperatorName: String?
): String? {
    val plmn = selectedPlmn?.filter { it.isDigit() }?.takeIf { it.length >= 5 }
    val matchedName = when {
        plmn != null && plmn == servingPlmn -> servingOperatorName
        plmn != null && plmn == homePlmn -> homeOperatorName
        plmn != null -> null
        else -> servingOperatorName ?: homeOperatorName
    }?.trim()?.takeIf { it.isNotBlank() }
    return matchedName ?: plmn
}

/** Suffix on the SIM metric: `Auto` or `Manual`. The locked operator is a separate row. */
fun formatSimOperatorSelectionSuffix(mode: SimOperatorSelectionMode): String? {
    return when (mode) {
        SimOperatorSelectionMode.UNKNOWN -> null
        SimOperatorSelectionMode.AUTO -> "Auto"
        SimOperatorSelectionMode.MANUAL -> "Manual"
    }
}
