package io.github.cloolalang.notspotdetector.model

/**
 * Whether camped-cell identity (EARFCN, PCI, etc.) should be shown.
 * Hidden during no-signal / flatline conditions so stale [TelephonyManager.getAllCellInfo]
 * neighbours are not displayed.
 */
fun ConnectivityStats.shouldShowCellIdentity(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (isLimitedService) return true
    if (isRsrpTooWeakForService(settings)) return false
    if (isOn2g && !monitor2gFallbackEnabled) return false
    if (!cellularAvailable) return false
    if (isOn2g && monitor2gFallbackEnabled && !hasLteNrSignal) return false
    if (isCompleteNoService) return false
    if (signalPermissionGranted &&
        radioAccessType == null &&
        rsrpDbm == null &&
        rsrqDb == null
    ) {
        return false
    }
    return true
}

fun ConnectivityStats.withCellIdentityForDisplay(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): ConnectivityStats {
    if (shouldShowCellIdentity(settings)) return this
    return copy(
        lteEarfcn = null,
        ltePci = null,
        nrEarfcn = null,
        nrPci = null,
        gsmEarfcn = null,
        gsmBsic = null
    )
}

fun CellularRadioMetrics.shouldShowCellIdentity(
    monitor2gFallback: Boolean,
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (isLimitedService) return true
    if (rsrpDbm != null && settings.isRsrpTooWeakForService(rsrpDbm)) return false
    if (isOn2g && !monitor2gFallback) return false
    if (isOn2g && monitor2gFallback && !hasLteNrSignal) return false
    if (isCompleteNoService) return false
    if (networkServiceMode == NetworkServiceMode.OUT_OF_SERVICE &&
        radioAccessType == null &&
        rsrpDbm == null
    ) {
        return false
    }
    if (!hasUsableSignalForMonitoring(monitor2gFallback, settings)) return false
    if (radioAccessType == null && rsrpDbm == null && rsrqDb == null && !hasLteNrSignal) {
        return false
    }
    return true
}

fun CellularRadioMetrics.withCellIdentityForDisplay(
    monitor2gFallback: Boolean,
    settings: PassiveSignalSettings = PassiveSignalSettings()
): CellularRadioMetrics {
    if (shouldShowCellIdentity(monitor2gFallback, settings)) return this
    return copy(
        lteEarfcn = null,
        ltePci = null,
        nrEarfcn = null,
        nrPci = null,
        gsmEarfcn = null,
        gsmBsic = null
    )
}
