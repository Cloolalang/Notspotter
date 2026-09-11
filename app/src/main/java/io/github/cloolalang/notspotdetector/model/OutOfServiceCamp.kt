package io.github.cloolalang.notspotdetector.model

fun ConnectivityStats.withoutCampedRadio(): ConnectivityStats {
    return copy(
        rsrpDbm = null,
        rsrqDb = null,
        radioAccessType = null,
        lteEarfcn = null,
        ltePci = null,
        nrEarfcn = null,
        nrPci = null,
        nrBand = null,
        gsmEarfcn = null,
        gsmBsic = null,
        isOn2g = false,
        hasHomeGsmSignal = false,
        hasLteNrSignal = false,
        servingNetworkOperatorName = null,
        plmn = null,
        lteLayerResilience = null
    )
}

/**
 * The Android no-service icon follows [NetworkServiceMode.OUT_OF_SERVICE] and
 * [NetworkServiceMode.RADIO_OFF]. Those states are always RXSS 0 — leftover CellInfo must not
 * keep limited-service camp or bounce “signal restored”.
 */
fun ConnectivityStats.reconcileOutOfServiceCamp(): ConnectivityStats {
    if (!networkServiceMode.isNoCellularService()) return this
    return withoutCampedRadio().copy(
        isLimitedService = false,
        isCompleteNoService = true,
        cellularAvailable = false,
        isWifiCallingActive = if (networkServiceMode.isRadioPoweredOff()) {
            false
        } else {
            isWifiCallingActive
        }
    )
}
