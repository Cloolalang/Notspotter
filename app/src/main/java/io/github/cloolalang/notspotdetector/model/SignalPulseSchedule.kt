package io.github.cloolalang.notspotdetector.model

/**
 * Identifies which RXSS alert-pulse path [io.github.cloolalang.notspotdetector.audio.GeigerCounterPlayer]
 * is currently waiting on. A change of this key must cancel the leftover interval and play the
 * new RXSS pulse immediately.
 */
data class SignalPulseScheduleKey(
    val path: SignalPulsePath,
    val rxssNumber: Int? = null
)

enum class SignalPulsePath {
    NONE,
    DEADZONE,
    LIMITED_4G_NO_SIGNAL,
    LIMITED_ALT_2G_NO_SIGNAL,
    LIMITED_SERVICE_OVERLAY,
    LIMITED_ALT_2G,
    LIMITED_SERVICE,
    SEARCHING_2G,
    G2_NO_SIGNAL,
    NO_SIGNAL,
    WIFI_CALLING,
    LIMITED_2G_LEGACY,
    LIMITED_SERVICE_LEGACY,
    FLATLINE,
    RSRP_INTERVAL,
    GOOD_CONNECTION,
    PING_GEIGER,
    IDLE
}

/**
 * Same priority order as the geiger player's main loop. [rxssNumber] distinguishes RSRP bands
 * (RXSS 1–8) so a long interval on a strong band cannot delay a weaker band's first pulse.
 */
fun ConnectivityStats.resolveSignalPulseScheduleKey(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): SignalPulseScheduleKey {
    if (!isMonitoring) return SignalPulseScheduleKey(SignalPulsePath.NONE)
    if (shouldPlayDeadzoneTier(settings)) {
        return SignalPulseScheduleKey(SignalPulsePath.DEADZONE, Rxss.DEADZONE)
    }
    if (shouldPlayLimited4gNoSignalCampTier(settings)) {
        return SignalPulseScheduleKey(SignalPulsePath.LIMITED_4G_NO_SIGNAL, Rxss.LIMITED_4G_NO_SIGNAL)
    }
    if (shouldPlayLimitedHome2gNoSignalCampTier(settings)) {
        return SignalPulseScheduleKey(SignalPulsePath.LIMITED_ALT_2G_NO_SIGNAL, Rxss.LIMITED_HOME_2G_NO_SIGNAL)
    }
    if (shouldPlayLimitedAlt2gNoSignalCampTier(settings)) {
        return SignalPulseScheduleKey(SignalPulsePath.LIMITED_ALT_2G_NO_SIGNAL, Rxss.LIMITED_ALT_2G_NO_SIGNAL)
    }
    if (shouldPlayLimitedServiceSignalOverlay(settings)) {
        return SignalPulseScheduleKey(
            SignalPulsePath.LIMITED_SERVICE_OVERLAY,
            rsrpIntervalRxssNumber(settings)
        )
    }
    if (shouldPlayLimitedHome2gCampTier(settings)) {
        return SignalPulseScheduleKey(SignalPulsePath.LIMITED_ALT_2G, Rxss.LIMITED_HOME_2G)
    }
    if (shouldPlayLimitedAlt2gCampTier(settings)) {
        return SignalPulseScheduleKey(SignalPulsePath.LIMITED_ALT_2G, Rxss.LIMITED_ALT_2G)
    }
    if (shouldPlayLimitedHome4gCampTier(settings)) {
        return SignalPulseScheduleKey(SignalPulsePath.LIMITED_SERVICE, Rxss.LIMITED_HOME_4G)
    }
    if (shouldPlayLimitedServiceCampTier(settings)) {
        return SignalPulseScheduleKey(SignalPulsePath.LIMITED_SERVICE, Rxss.LIMITED_ALT_4G)
    }
    if (shouldPlaySearching2gCampTier(settings)) {
        return SignalPulseScheduleKey(SignalPulsePath.SEARCHING_2G, Rxss.SEARCH_HOME_2G)
    }
    if (shouldPlayG2NoSignalCampTier(settings)) {
        return SignalPulseScheduleKey(SignalPulsePath.G2_NO_SIGNAL, Rxss.HOME_2G_NO_SIGNAL)
    }
    if (shouldPlayNoSignalCampTier(settings)) {
        return SignalPulseScheduleKey(SignalPulsePath.NO_SIGNAL, Rxss.LTE_NR_NO_SIGNAL)
    }
    if (shouldPlayWifiCallingNoSignalCampTier(settings)) {
        return SignalPulseScheduleKey(SignalPulsePath.WIFI_CALLING, Rxss.WIFI_CALLING_NO_SIGNAL)
    }
    if (shouldPlay2gLimitedServicePulse()) {
        return SignalPulseScheduleKey(SignalPulsePath.LIMITED_2G_LEGACY, Rxss.LIMITED_ALT_2G)
    }
    if (shouldPlayLimitedServiceTone()) {
        return SignalPulseScheduleKey(SignalPulsePath.LIMITED_SERVICE_LEGACY, Rxss.LIMITED_ALT_4G)
    }
    if (shouldPlayFlatline(settings)) {
        return SignalPulseScheduleKey(SignalPulsePath.FLATLINE)
    }
    if (cellularAvailable && shouldPlaySignalStrengthInterval(settings)) {
        return SignalPulseScheduleKey(SignalPulsePath.RSRP_INTERVAL, rsrpIntervalRxssNumber(settings))
    }
    if (cellularAvailable && isPassiveIdleMode) {
        return SignalPulseScheduleKey(SignalPulsePath.IDLE)
    }
    if (cellularAvailable && quality == ConnectionQuality.GOOD && !hasExtremeLatency()) {
        return SignalPulseScheduleKey(SignalPulsePath.GOOD_CONNECTION)
    }
    if (cellularAvailable) {
        return SignalPulseScheduleKey(SignalPulsePath.PING_GEIGER, rsrpIntervalRxssNumber(settings))
    }
    return SignalPulseScheduleKey(SignalPulsePath.IDLE)
}

private fun ConnectivityStats.rsrpIntervalRxssNumber(
    settings: PassiveSignalSettings
): Int? {
    if (usesG2SignalTiers()) return resolveG2SignalStrengthTier(settings)?.rxssNumber
    if (shouldPlayVeryStrongSignalIndicator(settings)) return Rxss.SIGNAL_HIGH
    return resolvePassiveClickRateTier(settings)?.rxssNumber
        ?: resolveSignalStrengthTier(settings)?.rxssNumber
}
