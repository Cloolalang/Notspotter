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
    /** LTE RSSNR (SNIR) in dB. */
    val lteSinrDb: Int? = null,
    /** NR SS-SINR (SNIR) in dB. */
    val nrSinrDb: Int? = null,
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
    /** Voice/CS camped, packet data not registered (e.g. 4G bands locked, 2G voice remains). */
    val isVoiceOnlyNoData: Boolean = false,
    val isNetworkRoaming: Boolean = false,
    /** WiFi calling / VoWiFi registered as the in-service transport — see [CellularRadioMetrics.isWifiCallingActive]. */
    val isWifiCallingActive: Boolean = false,
    val hasLimitedServiceOnAnySim: Boolean = false,
    val isCompleteNoService: Boolean = false,
    val hasHomeGsmSignal: Boolean = false,
    val hasLteNrSignal: Boolean = false,
    val monitor2gFallbackEnabled: Boolean = false,
    val networkOperatorName: String? = null,
    val homeNetworkOperatorName: String? = null,
    val virtualNetworkOperatorName: String? = null,
    val servingNetworkOperatorName: String? = null,
    val plmn: String? = null,
    val homePlmn: String? = null,
    val subscriptionId: Int? = null,
    val simSlotIndex: Int? = null,
    val simDisplayName: String? = null,
    val simOperatorSelectionMode: SimOperatorSelectionMode = SimOperatorSelectionMode.UNKNOWN,
    val manualSimOperatorName: String? = null,
    val mobileDataEnabled: Boolean? = null,
    val selectedApn: String? = null,
    val signalPermissionGranted: Boolean = false,
    val cellIdentityPermissionGranted: Boolean = false,
    /**
     * Confirmed no-signal state after the RXSS 10/15/31 filter (or the default ~1 s debounce).
     * Unit tests that skip [io.github.cloolalang.notspotdetector.MonitorState] should set this
     * when they want the no-signal RXSS, not only a raw weak RSRP.
     */
    val noSignalActive: Boolean = false,
    /**
     * Confirmed dead-zone RXSS after the RXSS 0 filter. Null means “not filtered” — use
     * [isCompleteNoService] (unit tests that only set that flag).
     */
    val deadzoneActive: Boolean? = null,
    /**
     * Confirmed signal-low RXSS after the RXSS 6/8 filter. Null means use the raw RSRP band.
     */
    val lowSignalActive: Boolean? = null,
    /**
     * Confirmed Level Range C (RXSS 4) after its flicker filter. Null means use the raw RSRP band.
     */
    val levelRangeCActive: Boolean? = null,
    /**
     * Confirmed Level Range D (RXSS 5) after its flicker filter. Null means use the raw RSRP band.
     */
    val levelRangeDActive: Boolean? = null,
    /**
     * Confirmed poor-RSRQ overlay after the RXSS 14 filter. Null means use the raw RSRQ reading.
     */
    val rsrqPoorActive: Boolean? = null,
    /**
     * Last in-service RSRP pulse band (RXSS 1–8). Kept while the no-signal filter is still
     * waiting so RXSS 5/6 pulses do not go silent before RXSS 10/15 confirms.
     */
    val heldInServiceSignalTier: SignalStrengthTier? = null,
    /**
     * Last confirmed non-RXSS-4 in-service band. Kept while the Level Range C filter is still
     * waiting to enter, so pulses stay on RXSS 3 or 5 instead of jumping on a one-poll flicker.
     */
    val heldFairNeighborTier: SignalStrengthTier? = null,
    /**
     * Last confirmed non-RXSS-5 in-service band. Kept while the Level Range D filter is still
     * waiting to enter, so pulses stay on RXSS 4 or 6 instead of jumping on a one-poll flicker.
     */
    val heldPoorNeighborTier: SignalStrengthTier? = null,
    /**
     * Debounced "4G layers detected" reading — see [CellularRadioMetrics.lteLayerResilience].
     * Smoothed via [LteLayerResilienceDebouncer] (two consecutive matching polls before the
     * displayed values change) so a single flickering neighbour reading doesn't cause the values
     * shown to the user to jump around.
     */
    val lteLayerResilience: LteLayerResilienceReading? = null,
    /**
     * False for visiting limited service, and when in-service / home-limited CellInfo is older
     * than the live-measurement window. Cellular metrics then blank RSRP / RSRQ / 2G RX rather
     * than showing a frozen figure.
     */
    val signalQualityFresh: Boolean = true,
    /** Serving-cell reselects in the last 60 s (rolling). */
    val cellReselectsPerMinute: Int = 0
)

/** RSRP / 2G RX shown in cellular metrics — blank when not a live scan. */
fun ConnectivityStats.displayedRsrpDbm(): Int? =
    rsrpDbm.takeIf { signalQualityFresh && !isLimitedServiceVisited() }

/** RSRQ shown in cellular metrics — blank when not a live scan. */
fun ConnectivityStats.displayedRsrqDb(): Int? =
    rsrqDb.takeIf { signalQualityFresh && !isLimitedServiceVisited() }

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
        nrBand = null,
        nrSinrDb = null
    )
}
