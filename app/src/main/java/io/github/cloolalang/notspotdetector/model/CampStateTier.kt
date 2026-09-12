package io.github.cloolalang.notspotdetector.model

/** Visited-operator 2G in limited service (RXSS 13). Independent of 2G-fallback monitoring. */
fun ConnectivityStats.isLimitedServiceAlt2g(): Boolean = isLimitedServiceVisited2g()

fun ConnectivityStats.isLimitedServiceVisited2g(): Boolean {
    if (!isLimitedService || !isOn2g) return false
    return resolveLimitedServiceVisitedOperatorName() != null
}

/** Home-operator 2G in limited service (RXSS 22). */
fun ConnectivityStats.isLimitedServiceHome2g(): Boolean {
    if (!isLimitedService || !isOn2g) return false
    return resolveLimitedServiceVisitedOperatorName() == null
}

/** Home-operator 4G/5G in limited service (RXSS 19). */
fun ConnectivityStats.isLimitedServiceHome4g(): Boolean {
    if (!isLimitedService || isOn2g) return false
    return resolveLimitedServiceVisitedOperatorName() == null
}

/** Visited-operator 4G/5G in limited service (RXSS 12). */
fun ConnectivityStats.isLimitedServiceVisited4g(): Boolean {
    if (!isLimitedService || isOn2g) return false
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
        !isWifiCallingActive &&
        settings.noSignalTierSoundEnabled
}

/** RXSS **31** — WiFi calling, no cellular signal. Independent sound settings from RXSS 10. */
fun ConnectivityStats.shouldPlayWifiCallingNoSignalCampTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    return isMonitoring &&
        noSignalActive &&
        isWifiCallingActive &&
        settings.wifiCallingTierSoundEnabled
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
        isLimitedServiceVisited4g() &&
        !isLimitedServiceNoSignalCamp(settings) &&
        !shouldPlayLimitedServiceSignalOverlay(settings) &&
        settings.limitedServiceTierSoundEnabled
}

/** RXSS **19** — limited home 4G/5G. Shares RXSS 12 two-tone settings. */
fun ConnectivityStats.shouldPlayLimitedHome4gCampTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    return isMonitoring &&
        isLimitedServiceHome4g() &&
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

/** RXSS **22** — limited home 2G. Shares RXSS 13 pulse settings. */
fun ConnectivityStats.shouldPlayLimitedHome2gCampTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    return isMonitoring &&
        isLimitedServiceHome2g() &&
        !isLimitedServiceNoSignalCamp(settings) &&
        !shouldPlayLimitedServiceSignalOverlay(settings) &&
        settings.limitedAlt2gTierSoundEnabled
}

/** RXSS **20** — limited 4G/5G camp (home or visited) with no usable RSRP. */
fun ConnectivityStats.shouldPlayLimited4gNoSignalCampTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    return isMonitoring &&
        isLimitedService &&
        !isOn2g &&
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

/** RXSS **21** — limited home 2G camp with no usable RX. Shares RXSS 15 pulse settings. */
fun ConnectivityStats.shouldPlayLimitedHome2gNoSignalCampTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    return isMonitoring &&
        isLimitedServiceHome2g() &&
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
        shouldPlayLimitedHome2gNoSignalCampTier(settings) ||
        shouldPlayLimitedAlt2gNoSignalCampTier(settings) ||
        shouldPlayLimitedHome2gCampTier(settings) ||
        shouldPlayLimitedAlt2gCampTier(settings) ||
        shouldPlayLimitedHome4gCampTier(settings) ||
        shouldPlayLimitedServiceCampTier(settings) ||
        shouldPlaySearching2gCampTier(settings) ||
        shouldPlayG2NoSignalCampTier(settings) ||
        shouldPlayNoSignalCampTier(settings) ||
        shouldPlayWifiCallingNoSignalCampTier(settings)
}
