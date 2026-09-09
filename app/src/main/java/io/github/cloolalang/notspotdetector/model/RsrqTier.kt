package io.github.cloolalang.notspotdetector.model

fun PassiveSignalSettings.isRsrqPoor(rsrqDb: Int?): Boolean {
    if (rsrqDb == null) return false
    return rsrqDb < rsrqFairMinDb
}

fun ConnectivityStats.isRsrqPoor(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!signalPermissionGranted || usesG2SignalTiers()) return false
    if (isRsrpTooWeakForService(settings)) return false
    return settings.isRsrqPoor(rsrqDb)
}

fun PassiveSignalSettings.shouldCoupleRsrqWhiteNoise(
    rsrqDb: Int?,
    isPassiveOnlySession: Boolean
): Boolean {
    return isPassiveOnlySession &&
        rsrqTierSoundEnabled &&
        rsrqTierCoupledToSignalTier &&
        isRsrqPoor(rsrqDb)
}

fun PassiveSignalSettings.rsrqTierWhiteNoiseMix(
    rsrqDb: Int?,
    isPassiveOnlySession: Boolean
): Double {
    if (!shouldCoupleRsrqWhiteNoise(rsrqDb, isPassiveOnlySession)) return 0.0
    return rsrqTierWhiteNoiseVolume.toDouble()
        .coerceIn(PassiveSignalSettings.MIN_RSRQ_TIER_WHITE_NOISE_VOLUME.toDouble(), 1.0)
}

fun ConnectivityStats.shouldPlayDecoupledRsrqTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!isMonitoring || !isPassiveOnlySession || !cellularAvailable) return false
    if (shouldPlayFlatline(settings)) return false
    if (shouldPlayLimitedServiceTone() || shouldPlay2gLimitedServicePulse()) return false
    if (!signalPermissionGranted) return false
    if (!settings.rsrqTierSoundEnabled || settings.rsrqTierCoupledToSignalTier) return false
    return isRsrqPoor(settings)
}

fun ConnectivityStats.computeRsrqTierClickIntervalMs(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Long {
    return SettingsCompatibility.resolveTierClickIntervalMs(
        configuredMs = settings.rsrqTierClickIntervalMs.toLong(),
        signalPulseDurationMs = settings.rsrqTierPulseDurationMs,
        isPassiveOnlySession = isPassiveOnlySession,
        maxConfiguredMs = PassiveSignalSettings.MAX_RSRQ_TIER_CLICK_INTERVAL_MS.toLong()
    )
}
