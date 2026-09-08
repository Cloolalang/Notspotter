package io.github.cloolalang.notspotdetector.model

enum class ReceptionLevel {
    GOOD,
    FAIR,
    POOR,
    UNKNOWN
}

fun ConnectivityStats.receptionLevel(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): ReceptionLevel {
    if (hasPoorReceptionSignal(settings)) return ReceptionLevel.POOR

    val rsrp = rsrpDbm
    val rsrq = rsrqDb

    if (rsrp == null && rsrq == null) return ReceptionLevel.UNKNOWN

    if ((rsrp != null && rsrp <= settings.poorRsrpMinDbm) ||
        (rsrq != null && rsrq < settings.rsrqFairMinDb)
    ) {
        return ReceptionLevel.POOR
    }

    if (rsrq != null && rsrp == null) {
        return if (rsrq >= settings.rsrqFairMinDb) {
            ReceptionLevel.FAIR
        } else {
            ReceptionLevel.POOR
        }
    }

    if (rsrp != null) {
        return when {
            rsrp > settings.mildRsrpMinDbm -> ReceptionLevel.GOOD
            rsrp > settings.fairRsrpMinDbm -> ReceptionLevel.FAIR
            else -> ReceptionLevel.POOR
        }
    }

    return ReceptionLevel.FAIR
}

private fun ConnectivityStats.hasPoorReceptionSignal(
    settings: PassiveSignalSettings
): Boolean {
    if (isLimitedService) return true
    if (isRsrpTooWeakForService(settings)) return true
    if (isOn2g && !monitor2gFallbackEnabled) return true
    if (!cellularAvailable) return true
    if (isOn2g && monitor2gFallbackEnabled && !hasHomeGsmSignal && rsrpDbm == null) return true
    if (signalPermissionGranted && radioAccessType == null && rsrpDbm == null) return true
    return false
}
