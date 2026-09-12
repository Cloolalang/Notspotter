package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader

/**
 * Simulated network camp state for passive-only monitoring.
 * Trigger states and VA testing: MOCK_NETWORK_SCENARIOS.md (repo root).
 */
enum class MockNetworkScenario {
    HOME_4G,
    HOME_2G,
    HOME_LIMITED_4G,
    HOME_LIMITED_2G,
    ALT_OPERATOR_4G,
    ALT_OPERATOR_2G,
    NO_SERVICE,
    SEARCHING_2G,
    HOME_5G_ENDC,
    /** RXSS 31 — in service via WiFi calling only, no cellular RAT/RSRP. */
    WIFI_CALLING;

    companion object {
        val DEFAULT = HOME_4G

        fun fromStoredName(name: String?): MockNetworkScenario {
            if (name.isNullOrBlank()) return DEFAULT
            return entries.find { it.name == name } ?: DEFAULT
        }
    }

    fun usesLteNrSignalStrength(): Boolean =
        this == HOME_4G || this == HOME_LIMITED_4G || this == ALT_OPERATOR_4G || this == HOME_5G_ENDC

    fun usesG2SignalStrength(): Boolean =
        this == HOME_2G || this == HOME_LIMITED_2G || this == ALT_OPERATOR_2G

    /** Scenarios that feed mock RSRP/RSRQ (or 2G RX level) into simulated metrics. */
    fun appliesMockSignalStrength(): Boolean =
        usesLteNrSignalStrength() || usesG2SignalStrength()
}

/**
 * Manual network state for passive-only monitoring (testing tiers and voice without live signal).
 * Saved in settings profiles as `passiveMock` — see SETTINGS_PROFILES.md.
 */
data class PassiveMockSettings(
    val enabled: Boolean = DEFAULT_ENABLED,
    val scenario: MockNetworkScenario = MockNetworkScenario.DEFAULT,
    val rsrpDbm: Int = DEFAULT_RSRP_DBM,
    val rsrqDb: Int = DEFAULT_RSRQ_DB
) {
    fun normalized(): PassiveMockSettings {
        return copy(
            rsrpDbm = rsrpDbm.coerceIn(MIN_MOCK_RSRP_DBM, PassiveSignalSettings.MAX_RSRP_DBM),
            rsrqDb = rsrqDb.coerceIn(PassiveSignalSettings.MIN_RSRQ_DB, PassiveSignalSettings.MAX_RSRQ_DB)
        )
    }

    fun toRadioMetrics(): CellularRadioMetrics {
        return when (scenario) {
            MockNetworkScenario.HOME_4G -> home4gMetrics()
            MockNetworkScenario.HOME_2G -> home2gMetrics()
            MockNetworkScenario.HOME_LIMITED_4G -> homeLimited4gMetrics()
            MockNetworkScenario.HOME_LIMITED_2G -> homeLimited2gMetrics()
            MockNetworkScenario.ALT_OPERATOR_4G -> altOperator4gMetrics()
            MockNetworkScenario.ALT_OPERATOR_2G -> altOperator2gMetrics()
            MockNetworkScenario.NO_SERVICE -> noServiceMetrics()
            MockNetworkScenario.SEARCHING_2G -> searching2gMetrics()
            MockNetworkScenario.HOME_5G_ENDC -> home5gEndcMetrics()
            MockNetworkScenario.WIFI_CALLING -> wifiCallingMetrics()
        }
    }

    private fun home4gMetrics(): CellularRadioMetrics {
        return baseMetrics(
            rsrpDbm = rsrpDbm,
            rsrqDb = rsrqDb,
            radioAccessType = CellularSignalReader.RADIO_4G,
            lteEarfcn = MOCK_LTE_EARFCN,
            ltePci = MOCK_LTE_PCI,
            isOn2g = false,
            isLimitedService = false,
            networkServiceMode = NetworkServiceMode.IN_SERVICE,
            hasLimitedServiceOnAnySim = false,
            isCompleteNoService = false,
            hasHomeGsmSignal = false,
            hasLteNrSignal = true,
            networkOperatorName = MOCK_HOME_OPERATOR,
            homeNetworkOperatorName = MOCK_HOME_OPERATOR,
            servingNetworkOperatorName = MOCK_HOME_OPERATOR,
            plmn = MOCK_HOME_PLMN,
            homePlmn = MOCK_HOME_PLMN
        )
    }

    /** Home operator 5G NSA (EN-DC) — LTE anchor + NR secondary carrier, same RSRP tiers as [home4gMetrics]. */
    private fun home5gEndcMetrics(): CellularRadioMetrics {
        return baseMetrics(
            rsrpDbm = rsrpDbm,
            rsrqDb = rsrqDb,
            radioAccessType = CellularSignalReader.RADIO_5G_ENDC,
            lteEarfcn = MOCK_LTE_EARFCN,
            ltePci = MOCK_LTE_PCI,
            nrEarfcn = MOCK_NR_EARFCN,
            nrPci = MOCK_NR_PCI,
            nrBand = MOCK_NR_BAND,
            isOn2g = false,
            isLimitedService = false,
            networkServiceMode = NetworkServiceMode.IN_SERVICE,
            hasLimitedServiceOnAnySim = false,
            isCompleteNoService = false,
            hasHomeGsmSignal = false,
            hasLteNrSignal = true,
            networkOperatorName = MOCK_HOME_OPERATOR,
            homeNetworkOperatorName = MOCK_HOME_OPERATOR,
            servingNetworkOperatorName = MOCK_HOME_OPERATOR,
            plmn = MOCK_HOME_PLMN,
            homePlmn = MOCK_HOME_PLMN
        )
    }

    private fun home2gMetrics(): CellularRadioMetrics {
        return baseMetrics(
            rsrpDbm = rsrpDbm,
            rsrqDb = null,
            radioAccessType = CellularSignalReader.RADIO_2G,
            gsmEarfcn = MOCK_GSM_EARFCN,
            gsmBsic = MOCK_GSM_BSIC,
            isOn2g = true,
            isLimitedService = false,
            networkServiceMode = NetworkServiceMode.IN_SERVICE,
            hasLimitedServiceOnAnySim = false,
            isCompleteNoService = false,
            hasHomeGsmSignal = true,
            hasLteNrSignal = false,
            networkOperatorName = MOCK_HOME_OPERATOR,
            homeNetworkOperatorName = MOCK_HOME_OPERATOR,
            servingNetworkOperatorName = MOCK_HOME_OPERATOR,
            plmn = MOCK_HOME_PLMN,
            homePlmn = MOCK_HOME_PLMN
        )
    }

    private fun homeLimited4gMetrics(): CellularRadioMetrics {
        return baseMetrics(
            rsrpDbm = rsrpDbm,
            rsrqDb = rsrqDb,
            radioAccessType = CellularSignalReader.RADIO_4G,
            lteEarfcn = MOCK_LTE_EARFCN,
            ltePci = MOCK_LTE_PCI,
            isOn2g = false,
            isLimitedService = true,
            networkServiceMode = NetworkServiceMode.LIMITED_SERVICE,
            hasLimitedServiceOnAnySim = true,
            isCompleteNoService = false,
            hasHomeGsmSignal = false,
            hasLteNrSignal = true,
            networkOperatorName = MOCK_HOME_OPERATOR,
            homeNetworkOperatorName = MOCK_HOME_OPERATOR,
            servingNetworkOperatorName = MOCK_HOME_OPERATOR,
            plmn = MOCK_HOME_PLMN,
            homePlmn = MOCK_HOME_PLMN
        )
    }

    private fun homeLimited2gMetrics(): CellularRadioMetrics {
        return baseMetrics(
            rsrpDbm = rsrpDbm,
            rsrqDb = null,
            radioAccessType = CellularSignalReader.RADIO_2G,
            gsmEarfcn = MOCK_GSM_EARFCN,
            gsmBsic = MOCK_GSM_BSIC,
            isOn2g = true,
            isLimitedService = true,
            networkServiceMode = NetworkServiceMode.LIMITED_SERVICE,
            hasLimitedServiceOnAnySim = true,
            isCompleteNoService = false,
            hasHomeGsmSignal = true,
            hasLteNrSignal = false,
            networkOperatorName = MOCK_HOME_OPERATOR,
            homeNetworkOperatorName = MOCK_HOME_OPERATOR,
            servingNetworkOperatorName = MOCK_HOME_OPERATOR,
            plmn = MOCK_HOME_PLMN,
            homePlmn = MOCK_HOME_PLMN
        )
    }

    private fun altOperator4gMetrics(): CellularRadioMetrics {
        return baseMetrics(
            rsrpDbm = rsrpDbm,
            rsrqDb = rsrqDb,
            radioAccessType = CellularSignalReader.RADIO_4G,
            lteEarfcn = MOCK_ALT_LTE_EARFCN,
            ltePci = MOCK_ALT_LTE_PCI,
            isOn2g = false,
            isLimitedService = true,
            networkServiceMode = NetworkServiceMode.LIMITED_SERVICE,
            hasLimitedServiceOnAnySim = true,
            isCompleteNoService = false,
            hasHomeGsmSignal = false,
            hasLteNrSignal = true,
            networkOperatorName = MOCK_VISITED_OPERATOR,
            homeNetworkOperatorName = MOCK_HOME_OPERATOR,
            servingNetworkOperatorName = MOCK_VISITED_OPERATOR,
            plmn = MOCK_VISITED_PLMN,
            homePlmn = MOCK_HOME_PLMN
        )
    }

    private fun altOperator2gMetrics(): CellularRadioMetrics {
        return baseMetrics(
            rsrpDbm = rsrpDbm,
            rsrqDb = null,
            radioAccessType = CellularSignalReader.RADIO_2G,
            gsmEarfcn = MOCK_ALT_GSM_EARFCN,
            gsmBsic = MOCK_ALT_GSM_BSIC,
            isOn2g = true,
            isLimitedService = true,
            networkServiceMode = NetworkServiceMode.LIMITED_SERVICE,
            hasLimitedServiceOnAnySim = true,
            isCompleteNoService = false,
            hasHomeGsmSignal = false,
            hasLteNrSignal = false,
            networkOperatorName = MOCK_VISITED_OPERATOR,
            homeNetworkOperatorName = MOCK_HOME_OPERATOR,
            servingNetworkOperatorName = MOCK_VISITED_OPERATOR,
            plmn = MOCK_VISITED_PLMN,
            homePlmn = MOCK_HOME_PLMN
        )
    }

    private fun noServiceMetrics(): CellularRadioMetrics {
        return baseMetrics(
            rsrpDbm = null,
            rsrqDb = null,
            radioAccessType = null,
            isOn2g = false,
            isLimitedService = false,
            networkServiceMode = NetworkServiceMode.OUT_OF_SERVICE,
            hasLimitedServiceOnAnySim = false,
            isCompleteNoService = true,
            hasHomeGsmSignal = false,
            hasLteNrSignal = false,
            networkOperatorName = MOCK_HOME_OPERATOR,
            homeNetworkOperatorName = MOCK_HOME_OPERATOR,
            servingNetworkOperatorName = null,
            plmn = null,
            homePlmn = MOCK_HOME_PLMN
        )
    }

    private fun searching2gMetrics(): CellularRadioMetrics {
        return baseMetrics(
            rsrpDbm = null,
            rsrqDb = null,
            radioAccessType = null,
            isOn2g = false,
            isLimitedService = false,
            networkServiceMode = NetworkServiceMode.UNKNOWN,
            hasLimitedServiceOnAnySim = false,
            isCompleteNoService = false,
            hasHomeGsmSignal = false,
            hasLteNrSignal = false,
            networkModePreference = NetworkModePreference.ALL_TECHNOLOGIES,
            networkOperatorName = MOCK_HOME_OPERATOR,
            homeNetworkOperatorName = MOCK_HOME_OPERATOR,
            servingNetworkOperatorName = MOCK_HOME_OPERATOR,
            plmn = MOCK_HOME_PLMN,
            homePlmn = MOCK_HOME_PLMN
        )
    }

    /** RXSS 31 — WiFi calling registered as the in-service transport, no cellular RAT/RSRP. */
    private fun wifiCallingMetrics(): CellularRadioMetrics {
        return baseMetrics(
            rsrpDbm = null,
            rsrqDb = null,
            radioAccessType = null,
            isOn2g = false,
            isLimitedService = false,
            networkServiceMode = NetworkServiceMode.IN_SERVICE,
            isWifiCallingActive = true,
            hasLimitedServiceOnAnySim = false,
            isCompleteNoService = false,
            hasHomeGsmSignal = false,
            hasLteNrSignal = false,
            networkOperatorName = MOCK_HOME_OPERATOR,
            homeNetworkOperatorName = MOCK_HOME_OPERATOR,
            servingNetworkOperatorName = MOCK_HOME_OPERATOR,
            plmn = MOCK_HOME_PLMN,
            homePlmn = MOCK_HOME_PLMN
        )
    }

    private fun baseMetrics(
        rsrpDbm: Int?,
        rsrqDb: Int?,
        radioAccessType: String?,
        lteEarfcn: Int? = null,
        ltePci: Int? = null,
        nrEarfcn: Int? = null,
        nrPci: Int? = null,
        nrBand: Int? = null,
        gsmEarfcn: Int? = null,
        gsmBsic: Int? = null,
        isOn2g: Boolean,
        isLimitedService: Boolean,
        networkServiceMode: NetworkServiceMode,
        isWifiCallingActive: Boolean = false,
        hasLimitedServiceOnAnySim: Boolean,
        isCompleteNoService: Boolean,
        hasHomeGsmSignal: Boolean,
        hasLteNrSignal: Boolean,
        networkModePreference: NetworkModePreference = NetworkModePreference.ALL_TECHNOLOGIES,
        networkOperatorName: String?,
        homeNetworkOperatorName: String?,
        servingNetworkOperatorName: String?,
        plmn: String?,
        homePlmn: String?
    ): CellularRadioMetrics {
        return CellularRadioMetrics(
            rsrpDbm = rsrpDbm,
            rsrqDb = rsrqDb,
            radioAccessType = radioAccessType,
            lteEarfcn = lteEarfcn,
            ltePci = ltePci,
            nrEarfcn = nrEarfcn,
            nrPci = nrPci,
            nrBand = nrBand,
            gsmEarfcn = gsmEarfcn,
            gsmBsic = gsmBsic,
            isOn2g = isOn2g,
            networkModePreference = networkModePreference,
            restrictedTo2gNetwork = networkModePreference == NetworkModePreference.FORCED_2G,
            isLimitedService = isLimitedService,
            networkServiceMode = networkServiceMode,
            isWifiCallingActive = isWifiCallingActive,
            hasLimitedServiceOnAnySim = hasLimitedServiceOnAnySim,
            isCompleteNoService = isCompleteNoService,
            hasHomeGsmSignal = hasHomeGsmSignal,
            hasLteNrSignal = hasLteNrSignal,
            networkOperatorName = networkOperatorName,
            homeNetworkOperatorName = homeNetworkOperatorName,
            servingNetworkOperatorName = servingNetworkOperatorName,
            plmn = plmn,
            homePlmn = homePlmn,
            permissionGranted = true,
            cellIdentityPermissionGranted = true
        )
    }

    companion object {
        const val DEFAULT_ENABLED = false
        /** Mock RSRP slider floor — below tier-setting minimum so very weak values can be simulated. */
        const val MIN_MOCK_RSRP_DBM = -130
        const val DEFAULT_RSRP_DBM = -115
        const val DEFAULT_RSRQ_DB = -18
        /** Home SIM operator — real UK name for easier voice-announcement testing. */
        const val MOCK_HOME_OPERATOR = "Vodafone"
        /** Camped visited-operator PLMN during limited-service mock scenarios. */
        const val MOCK_VISITED_OPERATOR = "EE"
        /** @deprecated Use [MOCK_VISITED_OPERATOR]. */
        const val MOCK_ALT_OPERATOR = MOCK_VISITED_OPERATOR
        const val MOCK_HOME_PLMN = "23415"
        const val MOCK_VISITED_PLMN = "23430"
        /** @deprecated Use [MOCK_VISITED_PLMN]. */
        const val MOCK_ALT_PLMN = MOCK_VISITED_PLMN
        const val MOCK_LTE_EARFCN = 1_800
        const val MOCK_LTE_PCI = 42
        /** NR secondary carrier for the Home operator 5G EN-DC mock scenario. */
        const val MOCK_NR_EARFCN = 158_760
        const val MOCK_NR_PCI = 231
        /** n20 (700 MHz APT) — matches [MOCK_NR_EARFCN]'s NR-ARFCN range per 3GPP TS 38.101-1. */
        const val MOCK_NR_BAND = 20
        const val MOCK_ALT_LTE_EARFCN = 1_850
        const val MOCK_ALT_LTE_PCI = 87
        const val MOCK_GSM_EARFCN = 62
        const val MOCK_GSM_BSIC = 7
        const val MOCK_ALT_GSM_EARFCN = 71
        const val MOCK_ALT_GSM_BSIC = 12
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
    val hasSignal = when (normalized.scenario) {
        MockNetworkScenario.NO_SERVICE -> false
        MockNetworkScenario.SEARCHING_2G -> false
        MockNetworkScenario.WIFI_CALLING -> false
        else -> radio.hasUsableSignalForMonitoring(monitor2gFallback, passiveSettings)
    }
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
        nrBand = radio.nrBand,
        gsmEarfcn = radio.gsmEarfcn,
        gsmBsic = radio.gsmBsic,
        isOn2g = radio.isOn2g,
        networkModePreference = radio.networkModePreference,
        restrictedTo2gNetwork = radio.restrictedTo2gNetwork,
        isLimitedService = radio.isLimitedService,
        networkServiceMode = radio.networkServiceMode,
        isVoiceOnlyNoData = radio.isVoiceOnlyNoData,
        isWifiCallingActive = radio.isWifiCallingActive,
        hasLimitedServiceOnAnySim = radio.hasLimitedServiceOnAnySim,
        isCompleteNoService = radio.isCompleteNoService,
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
