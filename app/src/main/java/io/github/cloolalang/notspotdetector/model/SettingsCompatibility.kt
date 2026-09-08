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
        signalPulseDurationMs: Int
    ): Int {
        return configuredMs.coerceIn(
            minTierClickIntervalUiMs(signalPulseDurationMs),
            PassiveSignalSettings.MAX_TIER_CLICK_INTERVAL_MS
        )
    }

    fun normalizePassiveSignalSettings(
        settings: PassiveSignalSettings,
        signalPulseDurationMs: Int
    ): PassiveSignalSettings {
        val normalized = settings.normalized()
        return normalized.copy(
            criticalTierClickIntervalMs = coerceStoredTierClickIntervalMs(
                normalized.criticalTierClickIntervalMs,
                signalPulseDurationMs
            ),
            poorTierClickIntervalMs = coerceStoredTierClickIntervalMs(
                normalized.poorTierClickIntervalMs,
                signalPulseDurationMs
            ),
            fairTierClickIntervalMs = coerceStoredTierClickIntervalMs(
                normalized.fairTierClickIntervalMs,
                signalPulseDurationMs
            ),
            goodTierClickIntervalMs = coerceStoredTierClickIntervalMs(
                normalized.goodTierClickIntervalMs,
                signalPulseDurationMs
            ),
            mildTierClickIntervalMs = coerceStoredTierClickIntervalMs(
                normalized.mildTierClickIntervalMs,
                signalPulseDurationMs
            ),
            veryStrongTierClickIntervalMs = coerceStoredTierClickIntervalMs(
                normalized.veryStrongTierClickIntervalMs,
                signalPulseDurationMs
            ),
            g2StrongTierClickIntervalMs = coerceStoredTierClickIntervalMs(
                normalized.g2StrongTierClickIntervalMs,
                signalPulseDurationMs
            ),
            g2WeakTierClickIntervalMs = coerceStoredTierClickIntervalMs(
                normalized.g2WeakTierClickIntervalMs,
                signalPulseDurationMs
            ),
            deadzoneTierClickIntervalMs = coerceStoredTierClickIntervalMs(
                normalized.deadzoneTierClickIntervalMs,
                normalized.deadzoneTierPulseDurationMs
            )
        )
    }

    fun resolveTierClickIntervalMs(
        configuredMs: Long,
        signalPulseDurationMs: Int,
        isPassiveOnlySession: Boolean
    ): Long {
        val bounded = configuredMs.coerceIn(
            PassiveSignalSettings.MIN_TIER_CLICK_INTERVAL_MS.toLong(),
            PassiveSignalSettings.MAX_TIER_CLICK_INTERVAL_MS.toLong()
        )
        val scaled = if (isPassiveOnlySession) {
            bounded
        } else {
            bounded * PASSIVE_CLICK_RATE_MULTIPLIER
        }
        return scaled
            .coerceAtLeast(minTierClickIntervalMs(signalPulseDurationMs))
            .coerceAtLeast(MIN_PASSIVE_CLICK_INTERVAL_MS)
    }

    fun isTierIntervalMuchSlowerThanMeasurement(
        tierIntervalMs: Int,
        measurementIntervalMs: Long
    ): Boolean {
        return tierIntervalMs > measurementIntervalMs * MEASUREMENT_INTERVAL_WARNING_MULTIPLIER
    }
}
