package io.github.cloolalang.notspotdetector.model

/**
 * Configurable RSRP/RSRQ band edges for passive monitoring (alert tiers, reception LED, no-signal).
 *
 * RSRP tiers use minimum dBm thresholds (stronger signal = higher / less negative value):
 * very strong (tier 1) > mild (2) > good (3) > fair (4) > poor (5) > no-signal,
 * within [MIN_RSRP_DBM, MAX_RSRP_DBM].
 *
 * RSRQ uses a single fair boundary: below triggers tier 14 (RSRQ poor) and poor reception.
 */
data class PassiveSignalSettings(
    /** Fixed at [DEFAULT_NO_SIGNAL_RSRP_DBM] — lower bound for RXSS 6 (not user-configurable). */
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
    /** Tier 15 — home 2G no signal camp state. */
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
    val searching2gTierSoundEnabled: Boolean = DEFAULT_TIER_SOUND_ENABLED,
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
    val wifiCallingTierPulseDurationMs: Int = DEFAULT_WIFI_CALLING_TIER_PULSE_DURATION_MS
) {
    /** RSRQ below this value maps to tier 14 and poor reception. */
    val criticalRsrqDb: Int
        get() = rsrqFairMinDb

    fun normalized(): PassiveSignalSettings {
        val gap = MIN_RSRP_BAND_GAP_DBM
        var veryStrong = veryStrongRsrpMinDbm.coerceIn(MIN_VERY_STRONG_RSRP_DBM, MAX_VERY_STRONG_RSRP_DBM)
        val maxMild = veryStrong - gap

        val noSignal = DEFAULT_NO_SIGNAL_RSRP_DBM
        var poor = poorRsrpMinDbm.coerceIn(noSignal + gap, maxMild - 3 * gap)
        var fair = fairRsrpMinDbm.coerceIn(poor + gap, maxMild - 2 * gap)
        var good = goodRsrpMinDbm.coerceIn(fair + gap, maxMild - gap)
        var mild = mildRsrpMinDbm.coerceIn(good + gap, maxMild)

        mild = mild.coerceIn(good + gap, maxMild)
        veryStrong = veryStrong.coerceIn(mild + gap, MAX_VERY_STRONG_RSRP_DBM)
        good = good.coerceIn(fair + gap, mild - gap)
        fair = fair.coerceIn(poor + gap, good - gap)
        poor = poor.coerceIn(noSignal + gap, fair - gap)

        val rsrqFair = rsrqFairMinDb.coerceIn(RSRQ_FAIR_MIN_DB, RSRQ_FAIR_MAX_DB)
        val quietRsrq = quietAlertRsrqDb.coerceIn(MIN_RSRQ_DB, MAX_RSRQ_DB)
        val quietRsrp = quietAlertRsrpMaxDbm.coerceIn(MIN_RSRP_DBM, MAX_RSRP_DBM)

        return copy(
            noSignalRsrpDbm = noSignal,
            poorRsrpMinDbm = poor,
            fairRsrpMinDbm = fair,
            goodRsrpMinDbm = good,
            mildRsrpMinDbm = mild,
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
            wifiCallingTierPulseDurationMs = wifiCallingTierPulseDurationMs.coerceCampTierPulseDuration()
        )
    }

    companion object {
        const val MIN_RSRP_DBM = -126
        const val MAX_RSRP_DBM = -50
        const val MIN_VERY_STRONG_RSRP_DBM = -90
        const val MAX_VERY_STRONG_RSRP_DBM = -30
        const val DEFAULT_VERY_STRONG_RSRP_MIN_DBM = -80

        /** @deprecated Use [DEFAULT_VERY_STRONG_RSRP_MIN_DBM]. */
        const val VERY_STRONG_RSRP_DBM = DEFAULT_VERY_STRONG_RSRP_MIN_DBM
        const val DEFAULT_POOR_RSRP_MIN_DBM = -120
        const val DEFAULT_FAIR_RSRP_MIN_DBM = -105
        const val DEFAULT_GOOD_RSRP_MIN_DBM = -100
        const val DEFAULT_MILD_RSRP_MIN_DBM = -95
        const val DEFAULT_RSRQ_FAIR_MIN_DB = -18
        const val DEFAULT_RSRQ_TIER_SOUND_ENABLED = false
        const val DEFAULT_RSRQ_TIER_COUPLED_TO_SIGNAL_TIER = true
        const val DEFAULT_RSRQ_TIER_WHITE_NOISE_VOLUME = 0.38f
        const val MIN_RSRQ_TIER_WHITE_NOISE_VOLUME = 0.05f
        const val MAX_RSRQ_TIER_WHITE_NOISE_VOLUME = 1f
        const val DEFAULT_RSRQ_TIER_CLICK_INTERVAL_MS = 800
        const val MAX_RSRQ_TIER_CLICK_INTERVAL_MS = 5_000
        const val DEFAULT_RSRQ_TIER_PULSE_DURATION_MS = 150
        const val DEFAULT_QUIET_ALERT_RSRQ_DB = -20
        const val DEFAULT_QUIET_ALERT_RSRP_MAX_DBM = -105

        /** @deprecated Use [DEFAULT_QUIET_ALERT_RSRQ_OFFSET_DB]. */
        const val QUIET_ALERT_RSRQ_OFFSET_DB = 2

        const val DEFAULT_NO_SIGNAL_RSRP_DBM = -125
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

        const val MIN_TIER_CLICK_INTERVAL_MS = 10
        const val MAX_TIER_CLICK_INTERVAL_MS = 20_000
        const val TIER_CLICK_INTERVAL_STEP_MS = 10

        const val DEFAULT_MILD_TIER_PULSE_DURATION_MS = AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
        const val DEFAULT_GOOD_TIER_PULSE_DURATION_MS = AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
        const val DEFAULT_FAIR_TIER_PULSE_DURATION_MS = AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
        const val DEFAULT_POOR_TIER_PULSE_DURATION_MS = AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
        const val DEFAULT_CRITICAL_TIER_PULSE_DURATION_MS = AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
        const val DEFAULT_CRITICAL_TIER_CLICK_INTERVAL_MS = 250
        const val DEFAULT_POOR_TIER_CLICK_INTERVAL_MS = 500
        const val DEFAULT_FAIR_TIER_CLICK_INTERVAL_MS = 1_250
        const val DEFAULT_GOOD_TIER_CLICK_INTERVAL_MS = 2_000
        const val DEFAULT_MILD_TIER_CLICK_INTERVAL_MS = 2_500
        const val DEFAULT_LEVEL_RANGE_ABCD_CLICK_INTERVAL_MS = DEFAULT_GOOD_TIER_CLICK_INTERVAL_MS
        const val DEFAULT_VERY_STRONG_TIER_CLICK_INTERVAL_MS = 1_250
        const val DEFAULT_TIER_SOUND_ENABLED = true

        /** Fixed RX level split between 2G fallback tier 7 (at or above) and tier 8 (below). */
        const val G2_TIER_RX_LEVEL_SPLIT_DBM = -100
        const val DEFAULT_G2_STRONG_TIER_CLICK_INTERVAL_MS = 2_000
        const val DEFAULT_G2_WEAK_TIER_CLICK_INTERVAL_MS = 500
        const val DEFAULT_G2_STRONG_TIER_PULSE_DURATION_MS = AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
        const val DEFAULT_G2_WEAK_TIER_PULSE_DURATION_MS = AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
        const val DEFAULT_G2_NO_SIGNAL_TIER_CLICK_INTERVAL_MS = 600
        const val DEFAULT_G2_NO_SIGNAL_TIER_PULSE_DURATION_MS = AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
        const val DEFAULT_DEADZONE_TIER_CLICK_INTERVAL_MS = 250
        const val DEFAULT_DEADZONE_TIER_PULSE_DURATION_MS = AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
        const val DEFAULT_NO_SIGNAL_TIER_CLICK_INTERVAL_MS = 600
        const val DEFAULT_SEARCHING_2G_TIER_CLICK_INTERVAL_MS = 600
        const val DEFAULT_LIMITED_SERVICE_TIER_CLICK_INTERVAL_MS = 900
        const val DEFAULT_LIMITED_ALT_2G_TIER_CLICK_INTERVAL_MS = 600
        const val DEFAULT_NO_SIGNAL_TIER_PULSE_DURATION_MS = AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
        const val DEFAULT_SEARCHING_2G_TIER_PULSE_DURATION_MS = AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
        const val DEFAULT_LIMITED_SERVICE_TIER_PULSE_DURATION_MS = AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
        const val DEFAULT_LIMITED_ALT_2G_TIER_PULSE_DURATION_MS = 300
        const val DEFAULT_WIFI_CALLING_TIER_CLICK_INTERVAL_MS = 600
        const val DEFAULT_WIFI_CALLING_TIER_PULSE_DURATION_MS = AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
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
    if (rsrpDbm == null || isRsrpTooWeakForService(rsrpDbm)) return null
    return if (rsrpDbm < PassiveSignalSettings.G2_TIER_RX_LEVEL_SPLIT_DBM) {
        SignalStrengthTier.G2_WEAK
    } else {
        SignalStrengthTier.G2_STRONG
    }
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
