package io.github.cloolalang.notspotdetector.model

/**
 * Pulsed no-signal tone while monitoring with no usable cellular/mobile data path,
 * on 2G when 2G monitoring is disabled, or when no radio metrics can be read at all.
 * Use [shouldPlayContinuousFlatline] for a steady tone in a complete dead zone.
 */
fun ConnectivityStats.shouldPlayFlatline(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!isMonitoring) return false

    if (isLimitedService) return false

    if (isRsrpTooWeakForService(settings)) return true

    if (isOn2g && !monitor2gFallbackEnabled) {
        return true
    }

    if (!cellularAvailable) return true

    if (isOn2g && monitor2gFallbackEnabled) {
        return !hasLteNrSignal
    }

    if (hasNoRadioSignal(settings)) return true
    return false
}

/** Steady no-signal tone when out of service on all technologies and no SOS on any SIM. */
fun ConnectivityStats.shouldPlayContinuousFlatline(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!shouldPlayFlatline(settings)) return false
    return isCompleteNoService
}

/**
 * Alternating two-tone alert while the network is in emergency-only (limited) service.
 */
fun ConnectivityStats.shouldPlayLimitedServiceTone(): Boolean {
    if (!isMonitoring) return false
    if (!isLimitedService) return false
    if (shouldPlay2gLimitedServicePulse()) return false
    return true
}

/**
 * 300 ms on / 300 ms off while 2G monitoring is enabled, in limited service, with no home GSM signal.
 */
fun ConnectivityStats.shouldPlay2gLimitedServicePulse(): Boolean {
    if (!isMonitoring) return false
    if (!isLimitedService || !monitor2gFallbackEnabled) return false
    return !hasHomeGsmSignal
}

/**
 * Interval clicks driven by RSRP/RSRQ on the low-signal volume slider, independent of ping RTT.
 */
fun ConnectivityStats.shouldPlaySignalStrengthInterval(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!isMonitoring || !cellularAvailable) return false
    if (shouldPlayFlatline(settings)) return false
    if (shouldPlayLimitedServiceTone() || shouldPlay2gLimitedServicePulse()) return false
    if (!signalPermissionGranted) return false
    return shouldPlayWeakSignalWarning(settings) || shouldPlayVeryStrongSignalIndicator(settings)
}

/**
 * Weak-signal clicks on the low-signal volume slider when RSRP/RSRQ fall below tier thresholds.
 */
fun ConnectivityStats.shouldPlayWeakSignalWarning(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!isMonitoring || !cellularAvailable || shouldPlayFlatline(settings)) return false
    if (shouldPlayLimitedServiceTone()) return false
    if (shouldPlay2gLimitedServicePulse()) return false
    if (!signalPermissionGranted) return false
    return shouldPlayWeakSignalTier(settings)
}

private fun ConnectivityStats.hasNoRadioSignal(
    settings: PassiveSignalSettings
): Boolean {
    if (!signalPermissionGranted) return false
    if (isRsrpTooWeakForService(settings)) return true
    if (rttMs != null) return false
    return radioAccessType == null && rsrpDbm == null && pingsSent > 0
}
