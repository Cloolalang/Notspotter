package io.github.cloolalang.notspotdetector.model

enum class SignalStrengthTier {
    /** Strongest RSRP band below very strong — tier 2. */
    MILD,
    /** Between fair and mild — tier 3. */
    GOOD,
    /** Mid RSRP band — tier 4. */
    FAIR,
    /** Weak RSRP band — tier 5. */
    POOR,
    /** Weakest usable RSRP or critical RSRQ — tier 6. */
    CRITICAL;

    /** UI tier number: 2 (mild) through 6 (critical). Tier 1 is very strong RSRP. */
    val displayNumber: Int
        get() = when (this) {
            MILD -> 2
            GOOD -> 3
            FAIR -> 4
            POOR -> 5
            CRITICAL -> 6
        }

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

/** Passive signal tier for the latest RSRP/RSRQ measurement (UI and diagnostics). */
enum class SignalMeasurementTier {
    VERY_STRONG,
    MILD,
    GOOD,
    FAIR,
    POOR,
    CRITICAL,
    NO_SIGNAL,
    LIMITED_SERVICE,
    PERMISSION_REQUIRED,
    UNAVAILABLE;

    /** UI tier number 1 (best) through 6 (worst), or null for non-tier states. */
    val displayNumber: Int?
        get() = when (this) {
            VERY_STRONG -> VERY_STRONG_TIER_NUMBER
            MILD -> SignalStrengthTier.MILD.displayNumber
            GOOD -> SignalStrengthTier.GOOD.displayNumber
            FAIR -> SignalStrengthTier.FAIR.displayNumber
            POOR -> SignalStrengthTier.POOR.displayNumber
            CRITICAL -> SignalStrengthTier.CRITICAL.displayNumber
            else -> null
        }
}

/** RSRP tier 1 — above [PassiveSignalSettings.veryStrongRsrpMinDbm]. */
const val VERY_STRONG_TIER_NUMBER = 1

const val DEFAULT_SIGNAL_PULSE_DURATION_MS = AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
const val MIN_SIGNAL_PULSE_DURATION_MS = AudioVolumeSettings.MIN_SIGNAL_PULSE_DURATION_MS

/** @deprecated Use [DEFAULT_SIGNAL_PULSE_DURATION_MS] from alert settings. */
const val SIGNAL_STRENGTH_PULSE_MS = DEFAULT_SIGNAL_PULSE_DURATION_MS
const val SIGNAL_STRENGTH_TONE_HZ = 600.0
const val VERY_STRONG_SIGNAL_INTERVAL_MS = 2_500L

/** Passive-only sessions play tier clicks at this multiple of the base rate (2 = twice as fast). */
const val PASSIVE_CLICK_RATE_MULTIPLIER = 2L
const val MIN_PASSIVE_CLICK_INTERVAL_MS = 125L

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

/** @deprecated Use [PassiveSignalSettings.DEFAULT_VERY_STRONG_RSRP_MIN_DBM]. */
const val VERY_STRONG_SIGNAL_RSRP_DBM = PassiveSignalSettings.DEFAULT_VERY_STRONG_RSRP_MIN_DBM

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

/**
 * Classifies the latest cellular measurement into a passive signal tier for display.
 * Uses the same RSRP/RSRQ boundaries as alert clicks, without alert-play gating.
 */
fun ConnectivityStats.resolveSignalMeasurementTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): SignalMeasurementTier {
    if (!signalPermissionGranted) return SignalMeasurementTier.PERMISSION_REQUIRED
    if (isLimitedService) return SignalMeasurementTier.LIMITED_SERVICE
    if (isMonitoring && noSignalActive) return SignalMeasurementTier.NO_SIGNAL
    if (isRsrpTooWeakForService(settings)) return SignalMeasurementTier.NO_SIGNAL
    if (!cellularAvailable && rsrpDbm == null && rsrqDb == null) {
        return SignalMeasurementTier.UNAVAILABLE
    }

    val rsrp = rsrpDbm
    if (rsrp != null && settings.isVeryStrongRsrp(rsrp)) {
        return SignalMeasurementTier.VERY_STRONG
    }

    return when (resolveSignalStrengthTier(settings)) {
        SignalStrengthTier.MILD -> SignalMeasurementTier.MILD
        SignalStrengthTier.GOOD -> SignalMeasurementTier.GOOD
        SignalStrengthTier.FAIR -> SignalMeasurementTier.FAIR
        SignalStrengthTier.POOR -> SignalMeasurementTier.POOR
        SignalStrengthTier.CRITICAL -> SignalMeasurementTier.CRITICAL
        null -> SignalMeasurementTier.UNAVAILABLE
    }
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
    return rsrp <= settings.veryStrongRsrpMinDbm
}

fun ConnectivityStats.computeSignalStrengthClickIntervalMs(
    settings: PassiveSignalSettings = PassiveSignalSettings(),
    signalPulseDurationMs: Int = DEFAULT_SIGNAL_PULSE_DURATION_MS
): Long {
    val configuredMs = if (shouldPlayVeryStrongSignalIndicator(settings)) {
        settings.veryStrongTierClickIntervalMs.toLong()
    } else {
        resolvePassiveClickRateTier(settings)?.let { settings.clickIntervalMsForTier(it) }
            ?: settings.fairTierClickIntervalMs.toLong()
    }
    return SettingsCompatibility.resolveTierClickIntervalMs(
        configuredMs = configuredMs,
        signalPulseDurationMs = signalPulseDurationMs,
        isPassiveOnlySession = isPassiveOnlySession
    )
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
    if (!isMonitoring || !cellularAvailable || shouldPlayFlatline(settings)) return false
    if (shouldPlayLimitedServiceTone()) return false
    if (shouldPlay2gLimitedServicePulse()) return false
    if (!signalPermissionGranted) return false
    val rsrp = rsrpDbm ?: return false
    return settings.isVeryStrongRsrp(rsrp)
}
