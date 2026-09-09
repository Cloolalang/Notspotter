package io.github.cloolalang.notspotdetector.model

/**
 * Cross-setting rules so alert timing stays physically playable.
 */
object SettingsCompatibility {
    /** Minimum gap between end of one pulse and start of the next. */
    const val PULSE_INTERVAL_GAP_MS = 25L

    /** Tier click interval must exceed measurement cycle by this factor to trigger a UI warning. */
    const val MEASUREMENT_INTERVAL_WARNING_MULTIPLIER = 2

    fun minTierClickIntervalMs(signalPulseDurationMs: Int): Long {
        return signalPulseDurationMs.coerceAtLeast(MIN_SIGNAL_PULSE_DURATION_MS).toLong() + PULSE_INTERVAL_GAP_MS
    }

    /** Slider minimum for tier click intervals, rounded up to [PassiveSignalSettings.TIER_CLICK_INTERVAL_STEP_MS]. */
    fun minTierClickIntervalUiMs(signalPulseDurationMs: Int): Int {
        val minMs = minTierClickIntervalMs(signalPulseDurationMs).toInt()
        val step = PassiveSignalSettings.TIER_CLICK_INTERVAL_STEP_MS
        return ((minMs + step - 1) / step) * step
    }

    fun coerceStoredTierClickIntervalMs(
        configuredMs: Int,
        signalPulseDurationMs: Int,
        maxIntervalMs: Int = PassiveSignalSettings.MAX_TIER_CLICK_INTERVAL_MS
    ): Int {
        return configuredMs.coerceIn(
            minTierClickIntervalUiMs(signalPulseDurationMs),
            maxIntervalMs
        )
    }

    fun normalizePassiveSignalSettings(
        settings: PassiveSignalSettings,
        signalPulseDurationMs: Int,
        levelRangeBcdPulseDurationMs: Int = signalPulseDurationMs
    ): PassiveSignalSettings {
        val normalized = settings.normalized()
        val levelRangeAbcdClickIntervalMs = coerceStoredTierClickIntervalMs(
            normalized.levelRangeAbcdClickIntervalMs,
            levelRangeBcdPulseDurationMs
        )
        return normalized.copy(
            criticalTierClickIntervalMs = coerceStoredTierClickIntervalMs(
                normalized.criticalTierClickIntervalMs,
                signalPulseDurationMs
            ),
            levelRangeAbcdClickIntervalMs = levelRangeAbcdClickIntervalMs,
            mildTierClickIntervalMs = levelRangeAbcdClickIntervalMs,
            goodTierClickIntervalMs = levelRangeAbcdClickIntervalMs,
            fairTierClickIntervalMs = levelRangeAbcdClickIntervalMs,
            poorTierClickIntervalMs = levelRangeAbcdClickIntervalMs,
            veryStrongTierClickIntervalMs = coerceStoredTierClickIntervalMs(
                normalized.veryStrongTierClickIntervalMs,
                signalPulseDurationMs
            ),
            g2StrongTierClickIntervalMs = coerceStoredTierClickIntervalMs(
                normalized.g2StrongTierClickIntervalMs,
                normalized.g2StrongTierPulseDurationMs
            ),
            g2WeakTierClickIntervalMs = coerceStoredTierClickIntervalMs(
                normalized.g2WeakTierClickIntervalMs,
                normalized.g2WeakTierPulseDurationMs
            ),
            g2NoSignalTierClickIntervalMs = coerceStoredTierClickIntervalMs(
                normalized.g2NoSignalTierClickIntervalMs,
                normalized.g2NoSignalTierPulseDurationMs
            ),
            deadzoneTierClickIntervalMs = coerceStoredTierClickIntervalMs(
                normalized.deadzoneTierClickIntervalMs,
                normalized.deadzoneTierPulseDurationMs
            ),
            noSignalTierClickIntervalMs = coerceStoredTierClickIntervalMs(
                normalized.noSignalTierClickIntervalMs,
                normalized.noSignalTierPulseDurationMs
            ),
            searching2gTierClickIntervalMs = coerceStoredTierClickIntervalMs(
                normalized.searching2gTierClickIntervalMs,
                normalized.searching2gTierPulseDurationMs
            ),
            limitedServiceTierClickIntervalMs = coerceStoredTierClickIntervalMs(
                normalized.limitedServiceTierClickIntervalMs,
                normalized.limitedServiceTierPulseDurationMs
            ),
            limitedAlt2gTierClickIntervalMs = coerceStoredTierClickIntervalMs(
                normalized.limitedAlt2gTierClickIntervalMs,
                normalized.limitedAlt2gTierPulseDurationMs
            ),
            rsrqTierClickIntervalMs = coerceStoredTierClickIntervalMs(
                normalized.rsrqTierClickIntervalMs,
                normalized.rsrqTierPulseDurationMs,
                PassiveSignalSettings.MAX_RSRQ_TIER_CLICK_INTERVAL_MS
            )
        )
    }

    fun resolveTierClickIntervalMs(
        configuredMs: Long,
        signalPulseDurationMs: Int,
        isPassiveOnlySession: Boolean,
        maxConfiguredMs: Long = PassiveSignalSettings.MAX_TIER_CLICK_INTERVAL_MS.toLong()
    ): Long {
        val bounded = configuredMs.coerceIn(
            PassiveSignalSettings.MIN_TIER_CLICK_INTERVAL_MS.toLong(),
            maxConfiguredMs
        )
        val scaled = if (isPassiveOnlySession) {
            bounded
        } else {
            bounded * PASSIVE_CLICK_RATE_MULTIPLIER
        }
        return scaled.coerceAtLeast(minTierClickIntervalMs(signalPulseDurationMs))
    }

    fun isTierIntervalMuchSlowerThanMeasurement(
        tierIntervalMs: Int,
        measurementIntervalMs: Long
    ): Boolean {
        return tierIntervalMs > measurementIntervalMs * MEASUREMENT_INTERVAL_WARNING_MULTIPLIER
    }
}
