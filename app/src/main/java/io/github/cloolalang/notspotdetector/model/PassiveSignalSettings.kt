package io.github.cloolalang.notspotdetector.model

/**
 * Configurable RSRP/RSRQ band edges for passive monitoring (alert tiers, reception LED, no-signal).
 *
 * RSRP tiers use minimum dBm thresholds (stronger signal = higher / less negative value).
 * Adjacent bands are half-open (`rsrp >` lower edge) so they never overlap:
 * RXSS 1 (above an adjustable −85…−50 floor) > RXSS 2 (−95 up to the RXSS 1 floor) >
 * RXSS 3 (−105 to −95) > RXSS 4 (−115 to −105) > RXSS 5 (RXSS 6 high end to −115) >
 * RXSS 6 (above the RXSS 10 floor through an adjustable high end up to −125) >
 * RXSS 10 (at or below an adjustable −135…−125 floor).
 *
 * RSRQ uses a single fair boundary: below triggers tier 14 (RSRQ poor) and poor reception.
 */
data class PassiveSignalSettings(
    /** RXSS 10 upper bound — camped LTE/NR no-signal RSRP, adjustable in [MIN_NO_SIGNAL_RSRP_DBM]..[MAX_NO_SIGNAL_RSRP_DBM]. */
    val noSignalRsrpDbm: Int = DEFAULT_NO_SIGNAL_RSRP_DBM,
    val poorRsrpMinDbm: Int = DEFAULT_POOR_RSRP_MIN_DBM,
    val fairRsrpMinDbm: Int = DEFAULT_FAIR_RSRP_MIN_DBM,
    val goodRsrpMinDbm: Int = DEFAULT_GOOD_RSRP_MIN_DBM,
    val mildRsrpMinDbm: Int = DEFAULT_MILD_RSRP_MIN_DBM,
    /** RSRP above this value triggers the very strong alert tier. */
    val veryStrongRsrpMinDbm: Int = DEFAULT_VERY_STRONG_RSRP_MIN_DBM,
    val rsrqFairMinDb: Int = DEFAULT_RSRQ_FAIR_MIN_DB,
    /** Tier 14 — poor RSRQ white-noise alert during passive-only monitoring. */
    val rsrqTierSoundEnabled: Boolean = DEFAULT_RSRQ_TIER_SOUND_ENABLED,
    /** When true, white noise mixes into RSRP tier signal pulses; when false, tier 14 plays on its own schedule. */
    val rsrqTierCoupledToSignalTier: Boolean = DEFAULT_RSRQ_TIER_COUPLED_TO_SIGNAL_TIER,
    val rsrqTierWhiteNoiseVolume: Float = DEFAULT_RSRQ_TIER_WHITE_NOISE_VOLUME,
    val rsrqTierClickIntervalMs: Int = DEFAULT_RSRQ_TIER_CLICK_INTERVAL_MS,
    val rsrqTierPulseDurationMs: Int = DEFAULT_RSRQ_TIER_PULSE_DURATION_MS,
    val quietAlertRsrqDb: Int = DEFAULT_QUIET_ALERT_RSRQ_DB,
    val quietAlertRsrpMaxDbm: Int = DEFAULT_QUIET_ALERT_RSRP_MAX_DBM,
    /** RXSS 2 (Level Range A) — length of each signal pulse (ms). */
    val mildTierPulseDurationMs: Int = DEFAULT_MILD_TIER_PULSE_DURATION_MS,
    /** RXSS 3 (Level Range B) — length of each signal pulse (ms). */
    val goodTierPulseDurationMs: Int = DEFAULT_GOOD_TIER_PULSE_DURATION_MS,
    /** RXSS 4 (Level Range C) — length of each signal pulse (ms). */
    val fairTierPulseDurationMs: Int = DEFAULT_FAIR_TIER_PULSE_DURATION_MS,
    /** RXSS 5 (Level Range D) — length of each signal pulse (ms). */
    val poorTierPulseDurationMs: Int = DEFAULT_POOR_TIER_PULSE_DURATION_MS,
    /** RXSS 6 (signal low) — length of each signal pulse (ms). */
    val criticalTierPulseDurationMs: Int = DEFAULT_CRITICAL_TIER_PULSE_DURATION_MS,
    /** Passive-only time between signal-pulse clicks in each RSRP tier (ms). */
    val criticalTierClickIntervalMs: Int = DEFAULT_CRITICAL_TIER_CLICK_INTERVAL_MS,
    /**
     * @deprecated Level Ranges A–D (RXSS 2–5) now each have an independent click interval
     * ([mildTierClickIntervalMs] and siblings). This field is kept only as an internal fallback
     * default (used when no specific tier can be resolved) and is no longer writable from the UI.
     */
    val levelRangeAbcdClickIntervalMs: Int = DEFAULT_LEVEL_RANGE_ABCD_CLICK_INTERVAL_MS,
    /** RXSS 5 (Level Range D) — time between signal-pulse clicks (ms). */
    val poorTierClickIntervalMs: Int = DEFAULT_POOR_TIER_CLICK_INTERVAL_MS,
    /** RXSS 4 (Level Range C) — time between signal-pulse clicks (ms). */
    val fairTierClickIntervalMs: Int = DEFAULT_FAIR_TIER_CLICK_INTERVAL_MS,
    /** RXSS 3 (Level Range B) — time between signal-pulse clicks (ms). */
    val goodTierClickIntervalMs: Int = DEFAULT_GOOD_TIER_CLICK_INTERVAL_MS,
    /** RXSS 2 (Level Range A) — time between signal-pulse clicks (ms). */
    val mildTierClickIntervalMs: Int = DEFAULT_MILD_TIER_CLICK_INTERVAL_MS,
    val veryStrongTierClickIntervalMs: Int = DEFAULT_VERY_STRONG_TIER_CLICK_INTERVAL_MS,
    val veryStrongTierSoundEnabled: Boolean = DEFAULT_TIER_SOUND_ENABLED,
    val mildTierSoundEnabled: Boolean = DEFAULT_TIER_SOUND_ENABLED,
    val goodTierSoundEnabled: Boolean = DEFAULT_TIER_SOUND_ENABLED,
    val fairTierSoundEnabled: Boolean = DEFAULT_TIER_SOUND_ENABLED,
    val poorTierSoundEnabled: Boolean = DEFAULT_TIER_SOUND_ENABLED,
    val criticalTierSoundEnabled: Boolean = DEFAULT_TIER_SOUND_ENABLED,
    /** 2G fallback tier 7 — click interval, pulse length, and sound toggle. */
    val g2StrongTierClickIntervalMs: Int = DEFAULT_G2_STRONG_TIER_CLICK_INTERVAL_MS,
    val g2StrongTierPulseDurationMs: Int = DEFAULT_G2_STRONG_TIER_PULSE_DURATION_MS,
    val g2StrongTierSoundEnabled: Boolean = DEFAULT_TIER_SOUND_ENABLED,
    /** 2G fallback tier 8 — click interval, pulse length, and sound toggle. */
    val g2WeakTierClickIntervalMs: Int = DEFAULT_G2_WEAK_TIER_CLICK_INTERVAL_MS,
    val g2WeakTierPulseDurationMs: Int = DEFAULT_G2_WEAK_TIER_PULSE_DURATION_MS,
    val g2WeakTierSoundEnabled: Boolean = DEFAULT_TIER_SOUND_ENABLED,
    /** RXSS 8 high end — 2G weak band sits above [g2NoSignalRsrpDbm] through this value. */
    val g2WeakMaxDbm: Int = DEFAULT_G2_WEAK_MAX_DBM,
    /** RXSS 15 upper bound — 2G no-signal RX level. */
    val g2NoSignalRsrpDbm: Int = DEFAULT_G2_NO_SIGNAL_RSRP_DBM,
    /** Tier 15 — 2G no signal camp state. */
    val g2NoSignalTierClickIntervalMs: Int = DEFAULT_G2_NO_SIGNAL_TIER_CLICK_INTERVAL_MS,
    val g2NoSignalTierSoundEnabled: Boolean = DEFAULT_TIER_SOUND_ENABLED,
    val g2NoSignalTierPulseDurationMs: Int = DEFAULT_G2_NO_SIGNAL_TIER_PULSE_DURATION_MS,
    /** Dead zone RXSS 0 — no service on any technology; click interval and sound toggle. */
    val deadzoneTierClickIntervalMs: Int = DEFAULT_DEADZONE_TIER_CLICK_INTERVAL_MS,
    val deadzoneTierSoundEnabled: Boolean = DEFAULT_TIER_SOUND_ENABLED,
    /** Length of each RXSS 0 signal pulse (ms). Independent of the global signal pulse duration. */
    val deadzoneTierPulseDurationMs: Int = DEFAULT_DEADZONE_TIER_PULSE_DURATION_MS,
    /** Tier 10 — LTE/NR no signal camp state. */
    val noSignalTierClickIntervalMs: Int = DEFAULT_NO_SIGNAL_TIER_CLICK_INTERVAL_MS,
    val noSignalTierSoundEnabled: Boolean = DEFAULT_TIER_SOUND_ENABLED,
    val noSignalTierPulseDurationMs: Int = DEFAULT_NO_SIGNAL_TIER_PULSE_DURATION_MS,
    /** Tier 11 — searching for home 2G. */
    val searching2gTierClickIntervalMs: Int = DEFAULT_SEARCHING_2G_TIER_CLICK_INTERVAL_MS,
    val searching2gTierSoundEnabled: Boolean = DEFAULT_SEARCHING_2G_TIER_SOUND_ENABLED,
    val searching2gTierPulseDurationMs: Int = DEFAULT_SEARCHING_2G_TIER_PULSE_DURATION_MS,
    /** Tier 12 — limited service (home or alt 4G). */
    val limitedServiceTierClickIntervalMs: Int = DEFAULT_LIMITED_SERVICE_TIER_CLICK_INTERVAL_MS,
    val limitedServiceTierSoundEnabled: Boolean = DEFAULT_TIER_SOUND_ENABLED,
    val limitedServiceTierPulseDurationMs: Int = DEFAULT_LIMITED_SERVICE_TIER_PULSE_DURATION_MS,
    /** Tier 13 — limited service on visited operator 2G. */
    val limitedAlt2gTierClickIntervalMs: Int = DEFAULT_LIMITED_ALT_2G_TIER_CLICK_INTERVAL_MS,
    val limitedAlt2gTierSoundEnabled: Boolean = DEFAULT_TIER_SOUND_ENABLED,
    val limitedAlt2gTierPulseDurationMs: Int = DEFAULT_LIMITED_ALT_2G_TIER_PULSE_DURATION_MS,
    /** RXSS 31 — WiFi calling, no cellular signal. Tone/frequency share the tier 10 no-signal group. */
    val wifiCallingTierClickIntervalMs: Int = DEFAULT_WIFI_CALLING_TIER_CLICK_INTERVAL_MS,
    val wifiCallingTierSoundEnabled: Boolean = DEFAULT_TIER_SOUND_ENABLED,
    val wifiCallingTierPulseDurationMs: Int = DEFAULT_WIFI_CALLING_TIER_PULSE_DURATION_MS,
    /** RXSS 6 / 8 — rolling confirmation before signal-low is adopted. */
    val lowSignalFilter: RxssStateFilterSettings = RxssStateFilterSettings.INACTIVE,
    /** RXSS 10 / 15 / 31 — rolling confirmation before no-signal is adopted. */
    val noSignalFilter: RxssStateFilterSettings = RxssStateFilterSettings.DEFAULT_NO_SIGNAL,
    /** RXSS 0 — rolling confirmation before dead zone is adopted. */
    val deadzoneFilter: RxssStateFilterSettings = RxssStateFilterSettings.INACTIVE,
    /** RXSS 14 — rolling confirmation before poor RSRQ is adopted. */
    val rsrqFilter: RxssStateFilterSettings = RxssStateFilterSettings.INACTIVE
) {
    /** RSRQ below this value maps to tier 14 and poor reception. */
    val criticalRsrqDb: Int
        get() = rsrqFairMinDb

    fun normalized(): PassiveSignalSettings {
        val veryStrong = veryStrongRsrpMinDbm.coerceIn(
            MIN_VERY_STRONG_RSRP_DBM,
            MAX_VERY_STRONG_RSRP_DBM
        ).coerceAtLeast(FIXED_RXSS2_MIN_DBM + MIN_RSRP_BAND_GAP_DBM)
        val noSignalRequested = noSignalRsrpDbm.coerceIn(MIN_NO_SIGNAL_RSRP_DBM, MAX_NO_SIGNAL_RSRP_DBM)
        val poorClamped = poorRsrpMinDbm.coerceIn(MIN_RXSS6_HIGH_DBM, MAX_RXSS6_HIGH_DBM)
        val poorMinFromRxss10 = noSignalRequested + MIN_RSRP_BAND_GAP_DBM
        val poor = if (poorMinFromRxss10 <= MAX_RXSS6_HIGH_DBM) {
            poorClamped.coerceAtLeast(poorMinFromRxss10)
        } else {
            poorClamped
        }
        val noSignal = noSignalRequested.coerceAtMost(poor - MIN_RSRP_BAND_GAP_DBM)
        val g2NoSignalRequested = g2NoSignalRsrpDbm.coerceIn(
            MIN_G2_NO_SIGNAL_RSRP_DBM,
            MAX_G2_NO_SIGNAL_RSRP_DBM
        )
        val g2WeakMax = g2WeakMaxDbm.coerceIn(
            maxOf(MIN_G2_WEAK_MAX_DBM, g2NoSignalRequested + MIN_RSRP_BAND_GAP_DBM),
            MAX_G2_WEAK_MAX_DBM
        )
        val g2NoSignal = g2NoSignalRequested.coerceAtMost(g2WeakMax - MIN_RSRP_BAND_GAP_DBM)

        val rsrqFair = rsrqFairMinDb.coerceIn(RSRQ_FAIR_MIN_DB, RSRQ_FAIR_MAX_DB)
        val quietRsrq = quietAlertRsrqDb.coerceIn(MIN_RSRQ_DB, MAX_RSRQ_DB)
        val quietRsrp = quietAlertRsrpMaxDbm.coerceIn(MIN_RSRP_DBM, MAX_RSRP_DBM)

        return copy(
            noSignalRsrpDbm = noSignal,
            poorRsrpMinDbm = poor,
            fairRsrpMinDbm = FIXED_RXSS4_MIN_DBM,
            goodRsrpMinDbm = FIXED_RXSS3_MIN_DBM,
            mildRsrpMinDbm = FIXED_RXSS2_MIN_DBM,
            veryStrongRsrpMinDbm = veryStrong,
            rsrqFairMinDb = rsrqFair,
            rsrqTierWhiteNoiseVolume = rsrqTierWhiteNoiseVolume.coerceIn(
                MIN_RSRQ_TIER_WHITE_NOISE_VOLUME,
                MAX_RSRQ_TIER_WHITE_NOISE_VOLUME
            ),
            rsrqTierClickIntervalMs = rsrqTierClickIntervalMs.coerceRsrqTierClickInterval(),
            rsrqTierPulseDurationMs = rsrqTierPulseDurationMs.coerceCampTierPulseDuration(),
            quietAlertRsrqDb = quietRsrq,
            quietAlertRsrpMaxDbm = quietRsrp,
            mildTierPulseDurationMs = mildTierPulseDurationMs.coerceCampTierPulseDuration(),
            goodTierPulseDurationMs = goodTierPulseDurationMs.coerceCampTierPulseDuration(),
            fairTierPulseDurationMs = fairTierPulseDurationMs.coerceCampTierPulseDuration(),
            poorTierPulseDurationMs = poorTierPulseDurationMs.coerceCampTierPulseDuration(),
            criticalTierPulseDurationMs = criticalTierPulseDurationMs.coerceCampTierPulseDuration(),
            criticalTierClickIntervalMs = criticalTierClickIntervalMs.coerceTierClickInterval(),
            levelRangeAbcdClickIntervalMs = levelRangeAbcdClickIntervalMs.coerceTierClickInterval(),
            poorTierClickIntervalMs = poorTierClickIntervalMs.coerceTierClickInterval(),
            fairTierClickIntervalMs = fairTierClickIntervalMs.coerceTierClickInterval(),
            goodTierClickIntervalMs = goodTierClickIntervalMs.coerceTierClickInterval(),
            mildTierClickIntervalMs = mildTierClickIntervalMs.coerceTierClickInterval(),
            veryStrongTierClickIntervalMs = veryStrongTierClickIntervalMs.coerceTierClickInterval(),
            g2StrongTierClickIntervalMs = g2StrongTierClickIntervalMs.coerceTierClickInterval(),
            g2WeakTierClickIntervalMs = g2WeakTierClickIntervalMs.coerceTierClickInterval(),
            g2StrongTierPulseDurationMs = g2StrongTierPulseDurationMs.coerceCampTierPulseDuration(),
            g2WeakTierPulseDurationMs = g2WeakTierPulseDurationMs.coerceCampTierPulseDuration(),
            g2WeakMaxDbm = g2WeakMax,
            g2NoSignalRsrpDbm = g2NoSignal,
            g2NoSignalTierClickIntervalMs = g2NoSignalTierClickIntervalMs.coerceTierClickInterval(),
            g2NoSignalTierPulseDurationMs = g2NoSignalTierPulseDurationMs.coerceCampTierPulseDuration(),
            deadzoneTierClickIntervalMs = deadzoneTierClickIntervalMs.coerceTierClickInterval(),
            deadzoneTierPulseDurationMs = deadzoneTierPulseDurationMs.coerceIn(
                AudioVolumeSettings.MIN_SIGNAL_PULSE_DURATION_MS,
                AudioVolumeSettings.MAX_SIGNAL_PULSE_DURATION_MS
            ),
            noSignalTierClickIntervalMs = noSignalTierClickIntervalMs.coerceTierClickInterval(),
            searching2gTierClickIntervalMs = searching2gTierClickIntervalMs.coerceTierClickInterval(),
            limitedServiceTierClickIntervalMs = limitedServiceTierClickIntervalMs.coerceTierClickInterval(),
            limitedAlt2gTierClickIntervalMs = limitedAlt2gTierClickIntervalMs.coerceTierClickInterval(),
            noSignalTierPulseDurationMs = noSignalTierPulseDurationMs.coerceCampTierPulseDuration(),
            searching2gTierPulseDurationMs = searching2gTierPulseDurationMs.coerceCampTierPulseDuration(),
            limitedServiceTierPulseDurationMs = limitedServiceTierPulseDurationMs.coerceCampTierPulseDuration(),
            limitedAlt2gTierPulseDurationMs = limitedAlt2gTierPulseDurationMs.coerceCampTierPulseDuration(),
            wifiCallingTierClickIntervalMs = wifiCallingTierClickIntervalMs.coerceTierClickInterval(),
            wifiCallingTierPulseDurationMs = wifiCallingTierPulseDurationMs.coerceCampTierPulseDuration(),
            lowSignalFilter = lowSignalFilter.normalized(),
            noSignalFilter = noSignalFilter.normalized(),
            deadzoneFilter = deadzoneFilter.normalized(),
            rsrqFilter = rsrqFilter.normalized()
        )
    }

    companion object {
        const val MIN_RSRP_DBM = -135
        const val MAX_RSRP_DBM = -50
        const val MIN_NO_SIGNAL_RSRP_DBM = -135
        const val MAX_NO_SIGNAL_RSRP_DBM = -125
        /** @deprecated Use [MIN_NO_SIGNAL_RSRP_DBM] / [DEFAULT_NO_SIGNAL_RSRP_DBM]. */
        const val FIXED_NO_SIGNAL_RSRP_DBM = MIN_NO_SIGNAL_RSRP_DBM
        const val MIN_VERY_STRONG_RSRP_DBM = -85
        const val MAX_VERY_STRONG_RSRP_DBM = -50
        const val DEFAULT_VERY_STRONG_RSRP_MIN_DBM = -75

        /** @deprecated Use [DEFAULT_VERY_STRONG_RSRP_MIN_DBM]. */
        const val VERY_STRONG_RSRP_DBM = DEFAULT_VERY_STRONG_RSRP_MIN_DBM
        /** RXSS 6 high end — between the RXSS 10 floor and the fixed −125 dBm top. */
        const val MIN_RXSS6_HIGH_DBM = MIN_NO_SIGNAL_RSRP_DBM + 1
        const val MAX_RXSS6_HIGH_DBM = -125
        const val DEFAULT_POOR_RSRP_MIN_DBM = MAX_RXSS6_HIGH_DBM
        const val FIXED_RXSS4_MIN_DBM = -115
        const val FIXED_RXSS3_MIN_DBM = -105
        const val FIXED_RXSS2_MIN_DBM = -95
        const val DEFAULT_FAIR_RSRP_MIN_DBM = FIXED_RXSS4_MIN_DBM
        const val DEFAULT_GOOD_RSRP_MIN_DBM = FIXED_RXSS3_MIN_DBM
        const val DEFAULT_MILD_RSRP_MIN_DBM = FIXED_RXSS2_MIN_DBM
        const val DEFAULT_RSRQ_FAIR_MIN_DB = -18
        const val DEFAULT_RSRQ_TIER_SOUND_ENABLED = true
        const val DEFAULT_RSRQ_TIER_COUPLED_TO_SIGNAL_TIER = false
        const val DEFAULT_RSRQ_TIER_WHITE_NOISE_VOLUME = 0.48f
        const val MIN_RSRQ_TIER_WHITE_NOISE_VOLUME = 0.05f
        const val MAX_RSRQ_TIER_WHITE_NOISE_VOLUME = 1f
        const val DEFAULT_RSRQ_TIER_CLICK_INTERVAL_MS = 925
        const val MAX_RSRQ_TIER_CLICK_INTERVAL_MS = 20_000
        const val DEFAULT_RSRQ_TIER_PULSE_DURATION_MS = 190
        const val DEFAULT_QUIET_ALERT_RSRQ_DB = -18
        const val DEFAULT_QUIET_ALERT_RSRP_MAX_DBM = -85

        /** @deprecated Use [DEFAULT_QUIET_ALERT_RSRQ_OFFSET_DB]. */
        const val QUIET_ALERT_RSRQ_OFFSET_DB = 2

        const val DEFAULT_NO_SIGNAL_RSRP_DBM = MIN_NO_SIGNAL_RSRP_DBM
        const val DEFAULT_CRITICAL_RSRQ_DB = DEFAULT_RSRQ_FAIR_MIN_DB

        /** @deprecated Removed — RSRQ uses a single fair/critical boundary. */
        const val DEFAULT_RSRQ_GOOD_MIN_DB = -12

        const val MIN_RSRP_BAND_GAP_DBM = 1
        const val MIN_RSRQ_BAND_GAP_DB = 1
        /** Fair/critical RSRQ boundary slider range (tier 14 trigger). */
        const val RSRQ_FAIR_MIN_DB = -20
        const val RSRQ_FAIR_MAX_DB = -13
        const val MIN_RSRQ_DB = -30
        const val MAX_RSRQ_DB = -1

        const val MIN_TIER_CLICK_INTERVAL_MS = 30
        const val MAX_TIER_CLICK_INTERVAL_MS = 20_000
        const val TIER_CLICK_INTERVAL_STEP_MS = 10

        const val DEFAULT_MILD_TIER_PULSE_DURATION_MS = 30
        const val DEFAULT_GOOD_TIER_PULSE_DURATION_MS = 40
        const val DEFAULT_FAIR_TIER_PULSE_DURATION_MS = 50
        const val DEFAULT_POOR_TIER_PULSE_DURATION_MS = 60
        const val DEFAULT_CRITICAL_TIER_PULSE_DURATION_MS = 250
        const val DEFAULT_CRITICAL_TIER_CLICK_INTERVAL_MS = 100
        const val DEFAULT_POOR_TIER_CLICK_INTERVAL_MS = 840
        const val DEFAULT_FAIR_TIER_CLICK_INTERVAL_MS = 10_230
        const val DEFAULT_GOOD_TIER_CLICK_INTERVAL_MS = 15_320
        const val DEFAULT_MILD_TIER_CLICK_INTERVAL_MS = 19_210
        const val DEFAULT_LEVEL_RANGE_ABCD_CLICK_INTERVAL_MS = 880
        const val DEFAULT_VERY_STRONG_TIER_CLICK_INTERVAL_MS = 2_320
        const val DEFAULT_TIER_SOUND_ENABLED = true
        const val DEFAULT_SEARCHING_2G_TIER_SOUND_ENABLED = false

        /** Strongest allowed RXSS 8 high end — RXSS 7 is anything stronger than [g2WeakMaxDbm]. */
        const val G2_STRONG_MIN_DBM = -85
        /** @deprecated Use [G2_STRONG_MIN_DBM]. */
        const val G2_TIER_RX_LEVEL_SPLIT_DBM = G2_STRONG_MIN_DBM
        const val MIN_G2_NO_SIGNAL_RSRP_DBM = -135
        const val MAX_G2_NO_SIGNAL_RSRP_DBM = -95
        const val DEFAULT_G2_NO_SIGNAL_RSRP_DBM = MIN_G2_NO_SIGNAL_RSRP_DBM
        /** RXSS 8 high end — between the RXSS 15 floor and −85 dBm. */
        const val MIN_G2_WEAK_MAX_DBM = MIN_G2_NO_SIGNAL_RSRP_DBM + MIN_RSRP_BAND_GAP_DBM
        const val MAX_G2_WEAK_MAX_DBM = G2_STRONG_MIN_DBM
        const val DEFAULT_G2_WEAK_MAX_DBM = MAX_G2_WEAK_MAX_DBM
        const val DEFAULT_G2_STRONG_TIER_CLICK_INTERVAL_MS = 10_350
        const val DEFAULT_G2_WEAK_TIER_CLICK_INTERVAL_MS = 1_120
        const val DEFAULT_G2_STRONG_TIER_PULSE_DURATION_MS = 170
        const val DEFAULT_G2_WEAK_TIER_PULSE_DURATION_MS = 90
        const val DEFAULT_G2_NO_SIGNAL_TIER_CLICK_INTERVAL_MS = 600
        const val DEFAULT_G2_NO_SIGNAL_TIER_PULSE_DURATION_MS = 250
        const val DEFAULT_DEADZONE_TIER_CLICK_INTERVAL_MS = 1_600
        const val DEFAULT_DEADZONE_TIER_PULSE_DURATION_MS = 1_530
        const val DEFAULT_NO_SIGNAL_TIER_CLICK_INTERVAL_MS = 12_470
        const val DEFAULT_SEARCHING_2G_TIER_CLICK_INTERVAL_MS = 600
        const val DEFAULT_LIMITED_SERVICE_TIER_CLICK_INTERVAL_MS = 900
        const val DEFAULT_LIMITED_ALT_2G_TIER_CLICK_INTERVAL_MS = 600
        const val DEFAULT_NO_SIGNAL_TIER_PULSE_DURATION_MS = 490
        const val DEFAULT_SEARCHING_2G_TIER_PULSE_DURATION_MS = 50
        const val DEFAULT_LIMITED_SERVICE_TIER_PULSE_DURATION_MS = 250
        const val DEFAULT_LIMITED_ALT_2G_TIER_PULSE_DURATION_MS = 140
        const val DEFAULT_WIFI_CALLING_TIER_CLICK_INTERVAL_MS = 600
        const val DEFAULT_WIFI_CALLING_TIER_PULSE_DURATION_MS = 250
    }
}

/** Longest pulse among Level Ranges A–D (RXSS 2–5) — used for the shared volume/frequency preview. */
fun PassiveSignalSettings.levelRangeAbcdMaxPulseDurationMs(): Int {
    return maxOf(
        mildTierPulseDurationMs,
        goodTierPulseDurationMs,
        fairTierPulseDurationMs,
        poorTierPulseDurationMs
    )
}

/** Fastest click interval among Level Ranges A–D (RXSS 2–5) — used for the shared volume/frequency preview. */
fun PassiveSignalSettings.levelRangeAbcdMinClickIntervalMs(): Int {
    return minOf(
        mildTierClickIntervalMs,
        goodTierClickIntervalMs,
        fairTierClickIntervalMs,
        poorTierClickIntervalMs
    )
}

private fun Int.coerceCampTierPulseDuration(): Int {
    return coerceIn(
        AudioVolumeSettings.MIN_SIGNAL_PULSE_DURATION_MS,
        AudioVolumeSettings.MAX_SIGNAL_PULSE_DURATION_MS
    )
}

fun PassiveSignalSettings.isTierSoundEnabled(tier: SignalStrengthTier): Boolean {
    return when (tier) {
        SignalStrengthTier.MILD -> mildTierSoundEnabled
        SignalStrengthTier.GOOD -> goodTierSoundEnabled
        SignalStrengthTier.FAIR -> fairTierSoundEnabled
        SignalStrengthTier.POOR -> poorTierSoundEnabled
        SignalStrengthTier.CRITICAL -> criticalTierSoundEnabled
        SignalStrengthTier.G2_STRONG -> g2StrongTierSoundEnabled
        SignalStrengthTier.G2_WEAK -> g2WeakTierSoundEnabled
        SignalStrengthTier.G2_NO_SIGNAL -> g2NoSignalTierSoundEnabled
        SignalStrengthTier.DEADZONE -> deadzoneTierSoundEnabled
        SignalStrengthTier.NO_SIGNAL -> noSignalTierSoundEnabled
        SignalStrengthTier.SEARCHING_2G -> searching2gTierSoundEnabled
        SignalStrengthTier.LIMITED_SERVICE -> limitedServiceTierSoundEnabled
        SignalStrengthTier.LIMITED_ALT_2G -> limitedAlt2gTierSoundEnabled
        SignalStrengthTier.RSRQ_POOR -> rsrqTierSoundEnabled
        SignalStrengthTier.WIFI_CALLING -> wifiCallingTierSoundEnabled
    }
}

fun PassiveSignalSettings.pulseDurationMsForTier(tier: SignalStrengthTier): Int {
    return when (tier) {
        SignalStrengthTier.MILD -> mildTierPulseDurationMs
        SignalStrengthTier.GOOD -> goodTierPulseDurationMs
        SignalStrengthTier.FAIR -> fairTierPulseDurationMs
        SignalStrengthTier.POOR -> poorTierPulseDurationMs
        SignalStrengthTier.CRITICAL -> criticalTierPulseDurationMs
        SignalStrengthTier.G2_STRONG -> g2StrongTierPulseDurationMs
        SignalStrengthTier.G2_WEAK -> g2WeakTierPulseDurationMs
        SignalStrengthTier.G2_NO_SIGNAL -> g2NoSignalTierPulseDurationMs
        SignalStrengthTier.DEADZONE -> deadzoneTierPulseDurationMs
        SignalStrengthTier.NO_SIGNAL -> noSignalTierPulseDurationMs
        SignalStrengthTier.SEARCHING_2G -> searching2gTierPulseDurationMs
        SignalStrengthTier.LIMITED_SERVICE -> limitedServiceTierPulseDurationMs
        SignalStrengthTier.LIMITED_ALT_2G -> limitedAlt2gTierPulseDurationMs
        SignalStrengthTier.RSRQ_POOR -> rsrqTierPulseDurationMs
        SignalStrengthTier.WIFI_CALLING -> wifiCallingTierPulseDurationMs
    }
}

private fun Int.coerceTierClickInterval(): Int {
    return coerceIn(
        PassiveSignalSettings.MIN_TIER_CLICK_INTERVAL_MS,
        PassiveSignalSettings.MAX_TIER_CLICK_INTERVAL_MS
    )
}

private fun Int.coerceRsrqTierClickInterval(): Int {
    return coerceIn(
        PassiveSignalSettings.MIN_TIER_CLICK_INTERVAL_MS,
        PassiveSignalSettings.MAX_RSRQ_TIER_CLICK_INTERVAL_MS
    )
}

fun PassiveSignalSettings.clickIntervalMsForTier(tier: SignalStrengthTier): Long {
    return when (tier) {
        SignalStrengthTier.MILD -> mildTierClickIntervalMs
        SignalStrengthTier.GOOD -> goodTierClickIntervalMs
        SignalStrengthTier.FAIR -> fairTierClickIntervalMs
        SignalStrengthTier.POOR -> poorTierClickIntervalMs
        SignalStrengthTier.CRITICAL -> criticalTierClickIntervalMs
        SignalStrengthTier.G2_STRONG -> g2StrongTierClickIntervalMs
        SignalStrengthTier.G2_WEAK -> g2WeakTierClickIntervalMs
        SignalStrengthTier.G2_NO_SIGNAL -> g2NoSignalTierClickIntervalMs
        SignalStrengthTier.DEADZONE -> deadzoneTierClickIntervalMs
        SignalStrengthTier.NO_SIGNAL -> noSignalTierClickIntervalMs
        SignalStrengthTier.SEARCHING_2G -> searching2gTierClickIntervalMs
        SignalStrengthTier.LIMITED_SERVICE -> limitedServiceTierClickIntervalMs
        SignalStrengthTier.LIMITED_ALT_2G -> limitedAlt2gTierClickIntervalMs
        SignalStrengthTier.RSRQ_POOR -> rsrqTierClickIntervalMs
        SignalStrengthTier.WIFI_CALLING -> wifiCallingTierClickIntervalMs
    }.toLong()
}

fun PassiveSignalSettings.resolveG2SignalStrengthTier(rsrpDbm: Int?): SignalStrengthTier? {
    if (rsrpDbm == null || isG2RsrpTooWeak(rsrpDbm)) return null
    return if (rsrpDbm > g2WeakMaxDbm) {
        SignalStrengthTier.G2_STRONG
    } else {
        SignalStrengthTier.G2_WEAK
    }
}

fun PassiveSignalSettings.isG2RsrpTooWeak(rsrpDbm: Int?): Boolean {
    if (rsrpDbm == null) return false
    return rsrpDbm <= g2NoSignalRsrpDbm
}

fun PassiveSignalSettings.isRsrpTooWeakForService(rsrpDbm: Int?): Boolean {
    if (rsrpDbm == null) return false
    return rsrpDbm <= noSignalRsrpDbm
}

fun PassiveSignalSettings.isVeryStrongRsrp(rsrpDbm: Int): Boolean {
    return rsrpDbm > veryStrongRsrpMinDbm
}

fun PassiveSignalSettings.resolveSignalStrengthTier(
    rsrpDbm: Int?
): SignalStrengthTier? {
    if (rsrpDbm != null && isRsrpTooWeakForService(rsrpDbm)) return null
    val rsrp = rsrpDbm ?: return null
    return when {
        rsrp > mildRsrpMinDbm -> SignalStrengthTier.MILD
        rsrp > goodRsrpMinDbm -> SignalStrengthTier.GOOD
        rsrp > fairRsrpMinDbm -> SignalStrengthTier.FAIR
        rsrp > poorRsrpMinDbm -> SignalStrengthTier.POOR
        else -> SignalStrengthTier.CRITICAL
    }
}
