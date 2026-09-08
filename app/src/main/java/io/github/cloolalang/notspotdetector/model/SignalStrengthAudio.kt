package io.github.cloolalang.notspotdetector.model

enum class SignalStrengthTier {
    /** Strongest RSRP band below very strong — 5 s interval. */
    MILD,
    /** Between fair and mild — 4 s interval. */
    GOOD,
    /** Mid RSRP band — 2.5 s interval. */
    FAIR,
    /** Weak RSRP band — 1 s interval. */
    POOR,
    /** Weakest usable RSRP or critical RSRQ — 0.5 s interval. */
    CRITICAL;

    val intervalMs: Long
        get() = when (this) {
            MILD -> 5_000L
            GOOD -> 4_000L
            FAIR -> 2_500L
            POOR -> 1_000L
            CRITICAL -> 500L
        }

    val pulseDurationRatio: Float
        get() = 1f

    fun pulseDurationMs(baseDurationMs: Int): Int {
        return (baseDurationMs * pulseDurationRatio).toInt().coerceAtLeast(MIN_SIGNAL_PULSE_DURATION_MS)
    }
}

const val DEFAULT_SIGNAL_PULSE_DURATION_MS = AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
const val MIN_SIGNAL_PULSE_DURATION_MS = AudioVolumeSettings.MIN_SIGNAL_PULSE_DURATION_MS

/** @deprecated Use [DEFAULT_SIGNAL_PULSE_DURATION_MS] from alert settings. */
const val SIGNAL_STRENGTH_PULSE_MS = DEFAULT_SIGNAL_PULSE_DURATION_MS
const val SIGNAL_STRENGTH_TONE_HZ = 600.0
const val VERY_STRONG_SIGNAL_TONE_HZ = 800.0
const val VERY_STRONG_SIGNAL_INTERVAL_MS = 2_500L

/** Passive-only sessions play tier clicks at this multiple of the base rate (2 = twice as fast). */
const val PASSIVE_CLICK_RATE_MULTIPLIER = 2L
const val MIN_PASSIVE_CLICK_INTERVAL_MS = 125L

/** Scales passive-only alert intervals: [MonitoringSettings.DEFAULT_PASSIVE_SOUND_SPEED] = current rate. */
fun scalePassiveSoundIntervalMs(intervalMsAtDefaultSpeed: Long, soundSpeed: Int): Long {
    val speed = soundSpeed.coerceIn(
        MonitoringSettings.MIN_PASSIVE_SOUND_SPEED,
        MonitoringSettings.MAX_PASSIVE_SOUND_SPEED
    )
    return (intervalMsAtDefaultSpeed * MonitoringSettings.DEFAULT_PASSIVE_SOUND_SPEED / speed)
        .coerceAtLeast(MIN_PASSIVE_CLICK_INTERVAL_MS)
}

fun scalePassiveSoundIntervalMs(intervalMsAtDefaultSpeed: Int, soundSpeed: Int): Int {
    val speed = soundSpeed.coerceIn(
        MonitoringSettings.MIN_PASSIVE_SOUND_SPEED,
        MonitoringSettings.MAX_PASSIVE_SOUND_SPEED
    )
    return (intervalMsAtDefaultSpeed * MonitoringSettings.DEFAULT_PASSIVE_SOUND_SPEED / speed)
        .coerceAtLeast(1)
}

/** @deprecated Use [PassiveSignalSettings.DEFAULT_NO_SIGNAL_RSRP_DBM]. */
const val NO_USABLE_SIGNAL_RSRP_DBM = PassiveSignalSettings.DEFAULT_NO_SIGNAL_RSRP_DBM

/** @deprecated Use [PassiveSignalSettings.DEFAULT_MILD_RSRP_MIN_DBM]. */
const val SIGNAL_TIER_MILD_RSRP_DBM = PassiveSignalSettings.DEFAULT_MILD_RSRP_MIN_DBM

/** @deprecated Use [PassiveSignalSettings.DEFAULT_FAIR_RSRP_MIN_DBM]. */
const val SIGNAL_TIER_FAIR_RSRP_DBM = PassiveSignalSettings.DEFAULT_FAIR_RSRP_MIN_DBM

/** @deprecated Use [PassiveSignalSettings.DEFAULT_POOR_RSRP_MIN_DBM]. */
const val SIGNAL_TIER_POOR_RSRP_DBM = PassiveSignalSettings.DEFAULT_POOR_RSRP_MIN_DBM

/** @deprecated Use [PassiveSignalSettings.DEFAULT_CRITICAL_RSRQ_DB]. */
const val SIGNAL_TIER_CRITICAL_RSRQ_DB = PassiveSignalSettings.DEFAULT_CRITICAL_RSRQ_DB

/** @deprecated Use [PassiveSignalSettings.VERY_STRONG_RSRP_DBM]. */
const val VERY_STRONG_SIGNAL_RSRP_DBM = PassiveSignalSettings.VERY_STRONG_RSRP_DBM

fun ConnectivityStats.isRsrpTooWeakForService(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    return settings.isRsrpTooWeakForService(rsrpDbm)
}

fun CellularRadioMetrics.hasUsableSignalForMonitoring(
    monitor2gFallback: Boolean,
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (rsrpDbm != null && settings.isRsrpTooWeakForService(rsrpDbm)) return false

    return when {
        isLimitedService -> true
        isOn2g && !monitor2gFallback -> true
        isOn2g && monitor2gFallback -> radioAccessType != null || rsrpDbm != null
        else -> radioAccessType != null || rsrpDbm != null || rsrqDb != null
    }
}

fun ConnectivityStats.resolveSignalStrengthTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): SignalStrengthTier? {
    if (!signalPermissionGranted) return null
    if (isRsrpTooWeakForService(settings)) return null
    return settings.resolveSignalStrengthTier(rsrpDbm, rsrqDb)
}

/** Tier used for passive click interval when noisy RSRQ clicks may suppress RSRQ-driven rate increases. */
fun ConnectivityStats.resolvePassiveClickRateTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): SignalStrengthTier? {
    if (!signalPermissionGranted) return null
    if (isRsrpTooWeakForService(settings)) return null
    val ignoreRsrqForRate = isPassiveOnlySession && settings.shouldUseNoisyRsrqPassiveClick(rsrqDb)
    return settings.resolveSignalStrengthTier(rsrpDbm, rsrqDb, ignoreRsrqForRate = ignoreRsrqForRate)
}

fun ConnectivityStats.shouldPlayWeakSignalTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (isRsrpTooWeakForService(settings)) return false
    if (shouldPlayVeryStrongSignalIndicator(settings)) return false
    if (rsrqDb != null && rsrqDb < settings.criticalRsrqDb) return true
    val rsrp = rsrpDbm ?: return false
    return rsrp <= PassiveSignalSettings.VERY_STRONG_RSRP_DBM
}

fun ConnectivityStats.computeSignalStrengthClickIntervalMs(
    settings: PassiveSignalSettings = PassiveSignalSettings(),
    passiveSoundSpeed: Int = MonitoringSettings.DEFAULT_PASSIVE_SOUND_SPEED
): Long {
    val baseInterval = if (shouldPlayVeryStrongSignalIndicator(settings)) {
        VERY_STRONG_SIGNAL_INTERVAL_MS
    } else {
        resolvePassiveClickRateTier(settings)?.intervalMs ?: 1_200L
    }
    if (!isPassiveOnlySession) return baseInterval
    val passiveInterval = (baseInterval / PASSIVE_CLICK_RATE_MULTIPLIER)
        .coerceAtLeast(MIN_PASSIVE_CLICK_INTERVAL_MS)
    return scalePassiveSoundIntervalMs(passiveInterval, passiveSoundSpeed)
}

fun ConnectivityStats.computeSignalStrengthClickDurationMs(
    baseDurationMs: Int,
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Int {
    return resolveSignalStrengthTier(settings)?.pulseDurationMs(baseDurationMs) ?: baseDurationMs
}

fun ConnectivityStats.shouldPlayVeryStrongSignalIndicator(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!isMonitoring || isPassiveIdleMode || !cellularAvailable || shouldPlayFlatline(settings)) return false
    if (shouldPlayLimitedServiceTone()) return false
    if (shouldPlay2gLimitedServicePulse()) return false
    if (!signalPermissionGranted) return false
    val rsrp = rsrpDbm ?: return false
    return settings.isVeryStrongRsrp(rsrp)
}
