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
    /** Serving NR operating band (e.g. 78 for n78) — see [CellularRadioMetrics.nrBand]. */
    val nrBand: Int? = null,
    val gsmEarfcn: Int? = null,
    val gsmBsic: Int? = null,
    val isOn2g: Boolean = false,
    val networkModePreference: NetworkModePreference = NetworkModePreference.UNKNOWN,
    /** Phone network-mode preference allows 2G only (API 31+). */
    val restrictedTo2gNetwork: Boolean = false,
    /** LTE/NR lost; phone may still camp on home 2G (all-tech mode, 2G allowed). */
    val searching2gFallbackActive: Boolean = false,
    val isLimitedService: Boolean = false,
    val networkServiceMode: NetworkServiceMode = NetworkServiceMode.UNKNOWN,
    /** WiFi calling / VoWiFi registered as the in-service transport — see [CellularRadioMetrics.isWifiCallingActive]. */
    val isWifiCallingActive: Boolean = false,
    val hasLimitedServiceOnAnySim: Boolean = false,
    val isCompleteNoService: Boolean = false,
    val hasHomeGsmSignal: Boolean = false,
    val hasLteNrSignal: Boolean = false,
    val monitor2gFallbackEnabled: Boolean = false,
    val networkOperatorName: String? = null,
    val homeNetworkOperatorName: String? = null,
    val servingNetworkOperatorName: String? = null,
    val plmn: String? = null,
    val homePlmn: String? = null,
    val subscriptionId: Int? = null,
    val simSlotIndex: Int? = null,
    val simDisplayName: String? = null,
    val signalPermissionGranted: Boolean = false,
    val cellIdentityPermissionGranted: Boolean = false,
    /** Debounced no-signal state (two consecutive polls to enter/exit). */
    val noSignalActive: Boolean = false,
    /**
     * Debounced "4G layers detected" reading — see [CellularRadioMetrics.lteLayerResilience].
     * Smoothed via [LteLayerResilienceDebouncer] (two consecutive matching polls before the
     * displayed values change) so a single flickering neighbour reading doesn't cause the values
     * shown to the user to jump around.
     */
    val lteLayerResilience: LteLayerResilienceReading? = null
)

fun ConnectivityStats.hidingFiveGIfDisabled(fiveGFeaturesEnabled: Boolean): ConnectivityStats {
    if (fiveGFeaturesEnabled) return this
    val isFiveG = radioAccessType == io.github.cloolalang.notspotdetector.network.CellularSignalReader.RADIO_5G ||
        radioAccessType == io.github.cloolalang.notspotdetector.network.CellularSignalReader.RADIO_5G_ENDC
    if (!isFiveG && nrEarfcn == null && nrPci == null && nrBand == null) return this
    return copy(
        radioAccessType = if (isFiveG) {
            io.github.cloolalang.notspotdetector.network.CellularSignalReader.RADIO_4G
        } else {
            radioAccessType
        },
        nrEarfcn = null,
        nrPci = null,
        nrBand = null
    )
}
