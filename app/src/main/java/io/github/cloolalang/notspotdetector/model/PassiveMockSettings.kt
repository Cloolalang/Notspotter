package io.github.cloolalang.notspotdetector.model

/** Manual RSRP/RSRQ for passive-only monitoring (testing tiers without live signal). */
data class PassiveMockSettings(
    val enabled: Boolean = DEFAULT_ENABLED,
    val rsrpDbm: Int = DEFAULT_RSRP_DBM,
    val rsrqDb: Int = DEFAULT_RSRQ_DB
) {
    fun normalized(): PassiveMockSettings {
        return copy(
            rsrpDbm = rsrpDbm.coerceIn(PassiveSignalSettings.MIN_RSRP_DBM, PassiveSignalSettings.MAX_RSRP_DBM),
            rsrqDb = rsrqDb.coerceIn(PassiveSignalSettings.MIN_RSRQ_DB, PassiveSignalSettings.MAX_RSRQ_DB)
        )
    }

    fun toRadioMetrics(): CellularRadioMetrics {
        return CellularRadioMetrics(
            rsrpDbm = rsrpDbm,
            rsrqDb = rsrqDb,
            radioAccessType = MOCK_RADIO_ACCESS_TYPE,
            lteEarfcn = MOCK_LTE_EARFCN,
            ltePci = MOCK_LTE_PCI,
            isOn2g = false,
            isLimitedService = false,
            networkServiceMode = NetworkServiceMode.IN_SERVICE,
            hasLimitedServiceOnAnySim = false,
            isCompleteNoService = false,
            hasHomeGsmSignal = false,
            hasLteNrSignal = true,
            networkOperatorName = MOCK_NETWORK_OPERATOR,
            homeNetworkOperatorName = MOCK_NETWORK_OPERATOR,
            servingNetworkOperatorName = MOCK_NETWORK_OPERATOR,
            plmn = MOCK_PLMN,
            homePlmn = MOCK_PLMN,
            permissionGranted = true,
            cellIdentityPermissionGranted = true
        )
    }

    companion object {
        const val DEFAULT_ENABLED = false
        const val DEFAULT_RSRP_DBM = -95
        const val DEFAULT_RSRQ_DB = -12
        const val MOCK_RADIO_ACCESS_TYPE = "LTE (mock)"
        const val MOCK_LTE_EARFCN = 1_800
        const val MOCK_LTE_PCI = 42
        const val MOCK_NETWORK_OPERATOR = "Mock network"
        const val MOCK_PLMN = "001-01"
    }
}

fun PassiveMockSettings.toConnectivityStats(
    monitor2gFallback: Boolean,
    passiveSettings: PassiveSignalSettings,
    passiveIdleMode: Boolean,
    passiveOnlySession: Boolean
): ConnectivityStats {
    val normalized = normalized()
    val radio = normalized.toRadioMetrics()
    val hasSignal = radio.hasUsableSignalForMonitoring(monitor2gFallback, passiveSettings)
    return ConnectivityStats(
        isMonitoring = true,
        isPassiveIdleMode = passiveIdleMode,
        isPassiveOnlySession = passiveOnlySession,
        cellularAvailable = hasSignal,
        rsrpDbm = radio.rsrpDbm,
        rsrqDb = radio.rsrqDb,
        radioAccessType = radio.radioAccessType,
        lteEarfcn = radio.lteEarfcn,
        ltePci = radio.ltePci,
        nrEarfcn = radio.nrEarfcn,
        nrPci = radio.nrPci,
        gsmEarfcn = radio.gsmEarfcn,
        gsmBsic = radio.gsmBsic,
        isOn2g = radio.isOn2g,
        isLimitedService = radio.isLimitedService,
        networkServiceMode = if (hasSignal) {
            NetworkServiceMode.IN_SERVICE
        } else {
            NetworkServiceMode.OUT_OF_SERVICE
        },
        hasLimitedServiceOnAnySim = radio.hasLimitedServiceOnAnySim,
        isCompleteNoService = !hasSignal,
        hasHomeGsmSignal = radio.hasHomeGsmSignal,
        hasLteNrSignal = radio.hasLteNrSignal,
        monitor2gFallbackEnabled = monitor2gFallback,
        networkOperatorName = radio.networkOperatorName,
        homeNetworkOperatorName = radio.homeNetworkOperatorName,
        servingNetworkOperatorName = radio.servingNetworkOperatorName,
        plmn = radio.plmn,
        homePlmn = radio.homePlmn,
        subscriptionId = radio.subscriptionId,
        simSlotIndex = radio.simSlotIndex,
        simDisplayName = radio.simDisplayName,
        signalPermissionGranted = radio.permissionGranted,
        cellIdentityPermissionGranted = radio.cellIdentityPermissionGranted
    )
}
