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

/**
 * True SIM roaming for the Service metric: registered in-service on a roaming network.
 *
 * This is **not** visiting limited-service fallback (SOS / emergency camp on another
 * operator). Android often sets [android.telephony.ServiceState.getRoaming] in that case too.
 */
fun isRegisteredSimRoaming(
    modemRoaming: Boolean,
    serviceMode: NetworkServiceMode,
    isLimitedService: Boolean,
    emergencyCamp: Boolean = false
): Boolean {
    if (!modemRoaming) return false
    if (isLimitedService || emergencyCamp) return false
    return serviceMode == NetworkServiceMode.IN_SERVICE
}

fun ConnectivityStats.isRegisteredSimRoaming(): Boolean {
    return isRegisteredSimRoaming(
        modemRoaming = isNetworkRoaming,
        serviceMode = networkServiceMode,
        isLimitedService = isLimitedService
    )
}

/** Cellular metrics Service row — distinguishes RXSS 12 visiting limited service. */
enum class ServiceStateMetricLabel {
    IN_SERVICE,
    IN_SERVICE_ROAMING,
    IN_SERVICE_VOICE_ONLY,
    IN_SERVICE_VOICE_ONLY_ROAMING,
    LIMITED_SERVICE,
    LIMITED_SERVICE_VOICE_ONLY,
    VISITING_LIMITED_SERVICE,
    VISITING_LIMITED_SERVICE_VOICE_ONLY,
    NO_SERVICE,
    UNKNOWN,
    PERMISSION_REQUIRED
}

fun ConnectivityStats.resolveServiceStateMetricLabel(): ServiceStateMetricLabel {
    if (!signalPermissionGranted) return ServiceStateMetricLabel.PERMISSION_REQUIRED
    return when (networkServiceMode) {
        NetworkServiceMode.IN_SERVICE -> when {
            isRegisteredSimRoaming() && isVoiceOnlyNoData ->
                ServiceStateMetricLabel.IN_SERVICE_VOICE_ONLY_ROAMING
            isRegisteredSimRoaming() -> ServiceStateMetricLabel.IN_SERVICE_ROAMING
            isVoiceOnlyNoData -> ServiceStateMetricLabel.IN_SERVICE_VOICE_ONLY
            else -> ServiceStateMetricLabel.IN_SERVICE
        }
        NetworkServiceMode.LIMITED_SERVICE -> when {
            isLimitedServiceVisited4g() && isVoiceOnlyNoData ->
                ServiceStateMetricLabel.VISITING_LIMITED_SERVICE_VOICE_ONLY
            isLimitedServiceVisited4g() -> ServiceStateMetricLabel.VISITING_LIMITED_SERVICE
            isVoiceOnlyNoData -> ServiceStateMetricLabel.LIMITED_SERVICE_VOICE_ONLY
            else -> ServiceStateMetricLabel.LIMITED_SERVICE
        }
        NetworkServiceMode.OUT_OF_SERVICE,
        NetworkServiceMode.RADIO_OFF -> ServiceStateMetricLabel.NO_SERVICE
        NetworkServiceMode.UNKNOWN -> ServiceStateMetricLabel.UNKNOWN
    }
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
