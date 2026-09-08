package io.github.cloolalang.notspotdetector.model

enum class ConnectionQuality {
    GOOD,
    DEGRADED,
    POOR,
    NO_CELLULAR,
    PASSIVE_IDLE,
    MONITORING_STOPPED
}

data class ConnectivityStats(
    val rttMs: Long? = null,
    val jitterMs: Long = 0,
    val packetLossPercent: Float = 0f,
    val pingsSent: Int = 0,
    val pingsFailed: Int = 0,
    val cellularAvailable: Boolean = false,
    val isMonitoring: Boolean = false,
    val isPassiveIdleMode: Boolean = false,
    val isPassiveOnlySession: Boolean = false,
    val lastPingTimestampMs: Long = 0,
    val quality: ConnectionQuality = ConnectionQuality.MONITORING_STOPPED,
    val severity: Float = 0f,
    val rsrpDbm: Int? = null,
    val rsrqDb: Int? = null,
    val radioAccessType: String? = null,
    val lteEarfcn: Int? = null,
    val ltePci: Int? = null,
    val nrEarfcn: Int? = null,
    val nrPci: Int? = null,
    val gsmEarfcn: Int? = null,
    val gsmBsic: Int? = null,
    val isOn2g: Boolean = false,
    val isLimitedService: Boolean = false,
    val networkServiceMode: NetworkServiceMode = NetworkServiceMode.UNKNOWN,
    val hasLimitedServiceOnAnySim: Boolean = false,
    val isCompleteNoService: Boolean = false,
    val hasHomeGsmSignal: Boolean = false,
    val hasLteNrSignal: Boolean = false,
    val monitor2gFallbackEnabled: Boolean = false,
    val networkOperatorName: String? = null,
    val plmn: String? = null,
    val subscriptionId: Int? = null,
    val simSlotIndex: Int? = null,
    val simDisplayName: String? = null,
    val signalPermissionGranted: Boolean = false,
    val cellIdentityPermissionGranted: Boolean = false
)
