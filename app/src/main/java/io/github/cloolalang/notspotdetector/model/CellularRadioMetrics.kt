package io.github.cloolalang.notspotdetector.model

data class CellularRadioMetrics(
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
    val networkOperatorName: String? = null,
    val plmn: String? = null,
    val subscriptionId: Int? = null,
    val simSlotIndex: Int? = null,
    val simDisplayName: String? = null,
    val permissionGranted: Boolean = false,
    val cellIdentityPermissionGranted: Boolean = false
)
