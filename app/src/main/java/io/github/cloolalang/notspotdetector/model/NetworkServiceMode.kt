package io.github.cloolalang.notspotdetector.model

enum class NetworkServiceMode {
    IN_SERVICE,
    LIMITED_SERVICE,
    OUT_OF_SERVICE,
    /** Airplane mode / radio powered off ([android.telephony.ServiceState.STATE_POWER_OFF]). */
    RADIO_OFF,
    UNKNOWN
}

fun NetworkServiceMode.isRadioPoweredOff(): Boolean = this == NetworkServiceMode.RADIO_OFF

fun NetworkServiceMode.isNoCellularService(): Boolean {
    return this == NetworkServiceMode.OUT_OF_SERVICE || this == NetworkServiceMode.RADIO_OFF
}

/** True when leaving limited service for a real camp (not radio-off / no-service). */
fun ConnectivityStats.shouldAnnounceInServiceAfterLimited(): Boolean {
    if (isLimitedService) return false
    if (isCompleteNoService) return false
    if (networkServiceMode.isNoCellularService()) return false
    return networkServiceMode == NetworkServiceMode.IN_SERVICE
}

/** Voice/CS camped (in service or limited) with no packet-switched / data registration. */
fun isVoiceOnlyNoData(
    serviceMode: NetworkServiceMode,
    packetSwitchedRegistered: Boolean
): Boolean {
    if (packetSwitchedRegistered) return false
    return serviceMode == NetworkServiceMode.IN_SERVICE ||
        serviceMode == NetworkServiceMode.LIMITED_SERVICE
}

/**
 * Maps Android [android.telephony.ServiceState.getState] to the app service mode.
 *
 * Top-level [STATE_OUT_OF_SERVICE] is often data/PS only (e.g. all 4G bands locked). If
 * circuit-switched / voice is still registered or emergency-camped — typical limited 2G —
 * the status bar does not show the no-service icon, so this must not become RXSS 0.
 * Leftover LTE CellInfo with no CS/emergency camp stays [OUT_OF_SERVICE].
 */
fun resolveNetworkServiceMode(
    serviceState: Int,
    circuitSwitchedRegistered: Boolean,
    emergencyCamp: Boolean
): NetworkServiceMode {
    return when (serviceState) {
        SERVICE_STATE_POWER_OFF -> NetworkServiceMode.RADIO_OFF
        SERVICE_STATE_EMERGENCY_ONLY -> NetworkServiceMode.LIMITED_SERVICE
        SERVICE_STATE_IN_SERVICE -> NetworkServiceMode.IN_SERVICE
        SERVICE_STATE_OUT_OF_SERVICE -> when {
            circuitSwitchedRegistered -> NetworkServiceMode.IN_SERVICE
            emergencyCamp -> NetworkServiceMode.LIMITED_SERVICE
            else -> NetworkServiceMode.OUT_OF_SERVICE
        }
        else -> if (emergencyCamp) {
            NetworkServiceMode.LIMITED_SERVICE
        } else {
            NetworkServiceMode.UNKNOWN
        }
    }
}

/** Matches [android.telephony.ServiceState] voice/data state integers. */
const val SERVICE_STATE_IN_SERVICE = 0
const val SERVICE_STATE_OUT_OF_SERVICE = 1
const val SERVICE_STATE_EMERGENCY_ONLY = 2
const val SERVICE_STATE_POWER_OFF = 3
