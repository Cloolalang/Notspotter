package io.github.cloolalang.notspotdetector.model

data class CellularRadioMetrics(
    val rsrpDbm: Int? = null,
    val rsrqDb: Int? = null,
    val radioAccessType: String? = null,
    val lteEarfcn: Int? = null,
    val ltePci: Int? = null,
    val nrEarfcn: Int? = null,
    val nrPci: Int? = null,
    /**
     * Serving NR operating band (e.g. 78 for n78), read directly from
     * [android.telephony.CellIdentityNr.getBands] (API 30+) rather than derived from the
     * NR-ARFCN, since NR-ARFCN ranges overlap across multiple bands (e.g. n1/n66) and the modem
     * reports the actual serving band(s) unambiguously. Null below API 30 or when unavailable.
     */
    val nrBand: Int? = null,
    val gsmEarfcn: Int? = null,
    val gsmBsic: Int? = null,
    /**
     * True when [android.telephony.ServiceState.getNetworkRegistrationInfoList] reports a
     * home-registered WLAN (WiFi calling / VoWiFi) transport — i.e. the modem/carrier is treating
     * WiFi calling as the in-service path, independent of any cellular RAT camp. Read via the
     * public API added in API 30; on older API levels this falls back to
     * `serviceState.dataNetworkType == TelephonyManager.NETWORK_TYPE_IWLAN`. See
     * `RXSS_CATALOGUE.md` RXSS 31.
     */
    val isWifiCallingActive: Boolean = false,
    val isOn2g: Boolean = false,
    val networkModePreference: NetworkModePreference = NetworkModePreference.UNKNOWN,
    val restrictedTo2gNetwork: Boolean = false,
    val isLimitedService: Boolean = false,
    val networkServiceMode: NetworkServiceMode = NetworkServiceMode.UNKNOWN,
    val hasLimitedServiceOnAnySim: Boolean = false,
    val isCompleteNoService: Boolean = false,
    val hasHomeGsmSignal: Boolean = false,
    val hasLteNrSignal: Boolean = false,
    val networkOperatorName: String? = null,
    /** SIM/home operator (e.g. Vodafone UK on a Vodafone SIM). */
    val homeNetworkOperatorName: String? = null,
    /** Camped/serving operator from telephony (may differ in limited service). */
    val servingNetworkOperatorName: String? = null,
    val plmn: String? = null,
    val homePlmn: String? = null,
    val subscriptionId: Int? = null,
    val simSlotIndex: Int? = null,
    val simDisplayName: String? = null,
    val permissionGranted: Boolean = false,
    val cellIdentityPermissionGranted: Boolean = false
)
