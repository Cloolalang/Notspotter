package io.github.cloolalang.notspotdetector.model

/** Visited operator 2G in limited service (tier 13). */
fun ConnectivityStats.isLimitedServiceAlt2g(): Boolean {
    if (!isLimitedService || !isOn2g || !monitor2gFallbackEnabled) return false
    return resolveLimitedServiceVisitedOperatorName() != null
}

fun ConnectivityStats.shouldPlayNoSignalCampTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    return isMonitoring &&
        noSignalActive &&
        !usesG2SignalTiers() &&
        !searching2gFallbackActive &&
        !isCompleteNoService &&
        !isLimitedService &&
        settings.noSignalTierSoundEnabled
}

fun ConnectivityStats.shouldPlayG2NoSignalCampTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    return isMonitoring &&
        noSignalActive &&
        usesG2SignalTiers() &&
        !searching2gFallbackActive &&
        !isCompleteNoService &&
        !isLimitedService &&
        settings.g2NoSignalTierSoundEnabled
}

fun ConnectivityStats.shouldPlaySearching2gCampTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    return isMonitoring &&
        searching2gFallbackActive &&
        settings.searching2gTierSoundEnabled
}

fun ConnectivityStats.shouldPlayLimitedServiceCampTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    return isMonitoring &&
        isLimitedService &&
        !isLimitedServiceAlt2g() &&
        !isLimitedServiceNoSignalCamp(settings) &&
        !shouldPlayLimitedServiceSignalOverlay(settings) &&
        settings.limitedServiceTierSoundEnabled
}

fun ConnectivityStats.shouldPlayLimitedAlt2gCampTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    return isMonitoring &&
        isLimitedServiceAlt2g() &&
        !isLimitedServiceNoSignalCamp(settings) &&
        !shouldPlayLimitedServiceSignalOverlay(settings) &&
        settings.limitedAlt2gTierSoundEnabled
}

/** RXSS **20** — limited visited 4G/5G camp with no usable RSRP. */
fun ConnectivityStats.shouldPlayLimited4gNoSignalCampTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    return isMonitoring &&
        isLimitedService &&
        !isLimitedServiceAlt2g() &&
        isLimitedServiceNoSignalCamp(settings) &&
        settings.noSignalTierSoundEnabled
}

/** RXSS **23** — limited visited 2G camp with no usable RX. */
fun ConnectivityStats.shouldPlayLimitedAlt2gNoSignalCampTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    return isMonitoring &&
        isLimitedServiceAlt2g() &&
        isLimitedServiceNoSignalCamp(settings) &&
        settings.g2NoSignalTierSoundEnabled
}

fun ConnectivityStats.computeCampTierClickIntervalMs(
    settings: PassiveSignalSettings,
    tier: SignalStrengthTier,
    pulseDurationMs: Int = settings.pulseDurationMsForTier(tier)
): Long {
    return SettingsCompatibility.resolveTierClickIntervalMs(
        configuredMs = settings.clickIntervalMsForTier(tier),
        signalPulseDurationMs = pulseDurationMs,
        isPassiveOnlySession = isPassiveOnlySession
    )
}

fun ConnectivityStats.isActiveCampStateTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    return shouldPlayDeadzoneTier(settings) ||
        shouldPlayLimited4gNoSignalCampTier(settings) ||
        shouldPlayLimitedAlt2gNoSignalCampTier(settings) ||
        shouldPlayLimitedAlt2gCampTier(settings) ||
        shouldPlayLimitedServiceCampTier(settings) ||
        shouldPlaySearching2gCampTier(settings) ||
        shouldPlayG2NoSignalCampTier(settings) ||
        shouldPlayNoSignalCampTier(settings)
}
