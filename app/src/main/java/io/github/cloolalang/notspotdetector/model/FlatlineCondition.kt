package io.github.cloolalang.notspotdetector.model

/**
 * Raw no-signal condition from the latest radio metrics (single poll).
 * Prefer [shouldPlayFlatline] for alerts — it uses the debounced [noSignalActive] flag.
 */
internal fun ConnectivityStats.evaluateFlatlineCondition(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (isLimitedService) return false

    if (isRsrpTooWeakForService(settings)) return true

    if (isOn2g && !monitor2gFallbackEnabled) {
        return true
    }

    if (!cellularAvailable) return true

    if (isOn2g && monitor2gFallbackEnabled) {
        return !hasHomeGsmSignal && rsrpDbm == null
    }

    if (hasNoRadioSignal(settings)) return true
    return false
}

/**
 * Pulsed no-signal tone while monitoring with no usable cellular/mobile data path,
 * on 2G when 2G monitoring is disabled, or when no radio metrics can be read at all.
 * Requires two consecutive polls to enter or exit (see [noSignalActive]).
 * Use [shouldPlayContinuousFlatline] for a steady tone in a complete dead zone.
 */
fun ConnectivityStats.shouldPlayFlatline(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!isMonitoring) return false
    if (!noSignalActive) return false
    if (shouldPlayNoSignalCampTier(settings)) return false
    if (shouldPlayG2NoSignalCampTier(settings)) return false
    if (shouldPlaySearching2gCampTier(settings)) return false
    return true
}

/** No-signal flatline while camped on 2G with 2G monitoring enabled — legacy flatline tone path. */
fun ConnectivityStats.isG2FlatlineActive(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    return usesG2SignalTiers() && shouldPlayFlatline(settings)
}

/**
 * Home 2G no signal (RXSS 15) voice — entry, exit, and 30 s repeats.
 * Uses [noSignalActive] on the 2G fallback path; independent of camp-tier click sound toggles.
 */
fun ConnectivityStats.shouldPlayG2NoSignalVoiceAnnouncements(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!isMonitoring || isPassiveIdleMode) return false
    if (!usesG2SignalTiers()) return false
    if (searching2gFallbackActive || isCompleteNoService || isLimitedService) return false
    return noSignalActive
}

/**
 * Skip “Signal restored” when recovery should use the tier 5 announcer (“signal low”) instead:
 * - dead zone → tier 5 (poor)
 * - tier 10 (LTE/NR no signal camp) → tier 6 (critical) only — tier 10 → tier 5 keeps “Signal restored”.
 */
fun shouldSuppressSignalRestoredForWeakSignalRecovery(
    previous: ConnectivityStats,
    next: ConnectivityStats,
    previousNoSignalActive: Boolean,
    nextNoSignalActive: Boolean,
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (previous.isCompleteNoService && !next.isCompleteNoService && next.isTier5PoorSignal(settings)) {
        return true
    }
    if (!previousNoSignalActive || nextNoSignalActive || next.usesG2SignalTiers()) return false
    return !previous.isCompleteNoService && next.isTier6CriticalSignal(settings)
}

/** RXSS **20** / **23** — visited limited-service no-signal voice (entry + 30 s repeats). */
fun ConnectivityStats.shouldPlayLimitedVisitedNoSignalVoiceAnnouncements(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!isMonitoring || isPassiveIdleMode) return false
    return isLimitedService && isLimitedServiceNoSignalCamp(settings)
}

/** No-signal voice reminders (immediate entry + 30 s repeats), including tier 10 camp state. */
fun ConnectivityStats.shouldPlayNoSignalVoiceAnnouncements(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!isMonitoring || isPassiveIdleMode) return false
    if (shouldPlayLimitedVisitedNoSignalVoiceAnnouncements(settings)) return true
    if (usesG2SignalTiers() || isCompleteNoService) return false
    return shouldPlayFlatline(settings) || shouldPlayNoSignalCampTier(settings)
}

/** Steady no-signal tone when out of service on all technologies and no SOS on any SIM. */
fun ConnectivityStats.shouldPlayContinuousFlatline(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!shouldPlayFlatline(settings)) return false
    if (shouldPlayDeadzoneTier(settings)) return false
    return isCompleteNoService
}

/**
 * Alternating two-tone alert while the network is in emergency-only (limited) service.
 */
fun ConnectivityStats.shouldPlayLimitedServiceTone(): Boolean {
    if (!isMonitoring) return false
    if (!isLimitedService) return false
    if (shouldPlayLimitedServiceCampTier()) return false
    if (shouldPlayLimitedAlt2gCampTier()) return false
    if (shouldPlay2gLimitedServicePulse()) return false
    if (shouldPlayLimitedServiceSignalOverlay()) return false
    if (isLimitedServiceNoSignalCamp()) return false
    return true
}

/**
 * 300 ms on / 300 ms off while 2G monitoring is enabled, in limited service, with no home GSM signal
 * and no measurable LTE/NR camp (transitional limited-service state before alt 2G).
 */
fun ConnectivityStats.shouldPlay2gLimitedServicePulse(): Boolean {
    if (!isMonitoring) return false
    if (!isLimitedService || !monitor2gFallbackEnabled) return false
    if (shouldPlayLimitedAlt2gCampTier()) return false
    if (shouldPlayLimitedServiceSignalOverlay()) return false
    if (isLimitedServiceNoSignalCamp()) return false
    if (isOn2g || hasLteNrSignal) return false
    return !hasHomeGsmSignal
}

/**
 * Interval clicks driven by RSRP/RSRQ on the low-signal volume slider, independent of ping RTT.
 */
fun ConnectivityStats.shouldPlaySignalStrengthInterval(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!isMonitoring) return false
    if (shouldPlayLimitedServiceSignalOverlay(settings)) {
        return signalPermissionGranted && shouldPlayCurrentTierSignalPulse(settings)
    }
    if (!cellularAvailable) return false
    if (shouldPlayFlatline(settings)) return false
    if (shouldPlayLimitedServiceTone() || shouldPlay2gLimitedServicePulse()) return false
    if (!signalPermissionGranted) return false
    return shouldPlayCurrentTierSignalPulse(settings)
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
