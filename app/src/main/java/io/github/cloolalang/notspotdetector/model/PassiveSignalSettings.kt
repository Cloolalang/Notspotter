package io.github.cloolalang.notspotdetector.model

/**
 * Configurable RSRP/RSRQ band edges for passive monitoring (alert tiers, reception LED, no-signal).
 *
 * RSRP tiers use minimum dBm thresholds (stronger signal = higher / less negative value):
 * very strong (tier 1) > mild (2) > good (3) > fair (4) > poor (5) > no-signal,
 * within [MIN_RSRP_DBM, MAX_RSRP_DBM].
 *
 * RSRQ uses a single fair/critical boundary: below triggers critical tier and poor reception.
 */
data class PassiveSignalSettings(
    val noSignalRsrpDbm: Int = DEFAULT_NO_SIGNAL_RSRP_DBM,
    val poorRsrpMinDbm: Int = DEFAULT_POOR_RSRP_MIN_DBM,
    val fairRsrpMinDbm: Int = DEFAULT_FAIR_RSRP_MIN_DBM,
    val goodRsrpMinDbm: Int = DEFAULT_GOOD_RSRP_MIN_DBM,
    val mildRsrpMinDbm: Int = DEFAULT_MILD_RSRP_MIN_DBM,
    /** RSRP above this value triggers the very strong alert tier. */
    val veryStrongRsrpMinDbm: Int = DEFAULT_VERY_STRONG_RSRP_MIN_DBM,
    val rsrqFairMinDb: Int = DEFAULT_RSRQ_FAIR_MIN_DB,
    val noisyRsrqPassiveClicks: Boolean = DEFAULT_NOISY_RSRQ_PASSIVE_CLICKS,
    val quietAlertRsrqDb: Int = DEFAULT_QUIET_ALERT_RSRQ_DB,
    val quietAlertRsrpMaxDbm: Int = DEFAULT_QUIET_ALERT_RSRP_MAX_DBM,
    /** Passive-only time between signal-pulse clicks in each RSRP tier (ms). */
    val criticalTierClickIntervalMs: Int = DEFAULT_CRITICAL_TIER_CLICK_INTERVAL_MS,
    val poorTierClickIntervalMs: Int = DEFAULT_POOR_TIER_CLICK_INTERVAL_MS,
    val fairTierClickIntervalMs: Int = DEFAULT_FAIR_TIER_CLICK_INTERVAL_MS,
    val goodTierClickIntervalMs: Int = DEFAULT_GOOD_TIER_CLICK_INTERVAL_MS,
    val mildTierClickIntervalMs: Int = DEFAULT_MILD_TIER_CLICK_INTERVAL_MS,
    val veryStrongTierClickIntervalMs: Int = DEFAULT_VERY_STRONG_TIER_CLICK_INTERVAL_MS
) {
    /** RSRQ below this value maps to the critical alert tier. */
    val criticalRsrqDb: Int
        get() = rsrqFairMinDb

    fun normalized(): PassiveSignalSettings {
        val gap = MIN_RSRP_BAND_GAP_DBM
        var veryStrong = veryStrongRsrpMinDbm.coerceIn(MIN_VERY_STRONG_RSRP_DBM, MAX_VERY_STRONG_RSRP_DBM)
        val maxMild = veryStrong - gap

        var noSignal = noSignalRsrpDbm.coerceIn(MIN_RSRP_DBM, maxMild - 4 * gap)
        var poor = poorRsrpMinDbm.coerceIn(noSignal + gap, maxMild - 3 * gap)
        var fair = fairRsrpMinDbm.coerceIn(poor + gap, maxMild - 2 * gap)
        var good = goodRsrpMinDbm.coerceIn(fair + gap, maxMild - gap)
        var mild = mildRsrpMinDbm.coerceIn(good + gap, maxMild)

        mild = mild.coerceIn(good + gap, maxMild)
        veryStrong = veryStrong.coerceIn(mild + gap, MAX_VERY_STRONG_RSRP_DBM)
        good = good.coerceIn(fair + gap, mild - gap)
        fair = fair.coerceIn(poor + gap, good - gap)
        poor = poor.coerceIn(noSignal + gap, fair - gap)
        noSignal = noSignal.coerceIn(MIN_RSRP_DBM, poor - gap)

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
            noisyRsrqPassiveClicks = noisyRsrqPassiveClicks,
            quietAlertRsrqDb = quietRsrq,
            quietAlertRsrpMaxDbm = quietRsrp,
            criticalTierClickIntervalMs = criticalTierClickIntervalMs.coerceTierClickInterval(),
            poorTierClickIntervalMs = poorTierClickIntervalMs.coerceTierClickInterval(),
            fairTierClickIntervalMs = fairTierClickIntervalMs.coerceTierClickInterval(),
            goodTierClickIntervalMs = goodTierClickIntervalMs.coerceTierClickInterval(),
            mildTierClickIntervalMs = mildTierClickIntervalMs.coerceTierClickInterval(),
            veryStrongTierClickIntervalMs = veryStrongTierClickIntervalMs.coerceTierClickInterval()
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
        const val DEFAULT_NOISY_RSRQ_PASSIVE_CLICKS = false
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
        /** Fair/critical RSRQ boundary slider range. */
        const val RSRQ_FAIR_MIN_DB = -30
        const val RSRQ_FAIR_MAX_DB = -13
        const val MIN_RSRQ_DB = -30
        const val MAX_RSRQ_DB = -1

        const val MIN_TIER_CLICK_INTERVAL_MS = 10
        const val MAX_TIER_CLICK_INTERVAL_MS = 5_000
        const val TIER_CLICK_INTERVAL_STEP_MS = 10

        const val DEFAULT_CRITICAL_TIER_CLICK_INTERVAL_MS = 250
        const val DEFAULT_POOR_TIER_CLICK_INTERVAL_MS = 500
        const val DEFAULT_FAIR_TIER_CLICK_INTERVAL_MS = 1_250
        const val DEFAULT_GOOD_TIER_CLICK_INTERVAL_MS = 2_000
        const val DEFAULT_MILD_TIER_CLICK_INTERVAL_MS = 2_500
        const val DEFAULT_VERY_STRONG_TIER_CLICK_INTERVAL_MS = 1_250
    }
}

private fun Int.coerceTierClickInterval(): Int {
    return coerceIn(
        PassiveSignalSettings.MIN_TIER_CLICK_INTERVAL_MS,
        PassiveSignalSettings.MAX_TIER_CLICK_INTERVAL_MS
    )
}

fun PassiveSignalSettings.clickIntervalMsForTier(tier: SignalStrengthTier): Long {
    return when (tier) {
        SignalStrengthTier.MILD -> mildTierClickIntervalMs
        SignalStrengthTier.GOOD -> goodTierClickIntervalMs
        SignalStrengthTier.FAIR -> fairTierClickIntervalMs
        SignalStrengthTier.POOR -> poorTierClickIntervalMs
        SignalStrengthTier.CRITICAL -> criticalTierClickIntervalMs
    }.toLong()
}

fun PassiveSignalSettings.shouldUseNoisyRsrqPassiveClick(rsrqDb: Int?): Boolean {
    if (!noisyRsrqPassiveClicks || rsrqDb == null) return false
    return rsrqDb < rsrqFairMinDb
}

fun PassiveSignalSettings.isRsrpTooWeakForService(rsrpDbm: Int?): Boolean {
    if (rsrpDbm == null) return false
    return rsrpDbm <= noSignalRsrpDbm
}

fun PassiveSignalSettings.isVeryStrongRsrp(rsrpDbm: Int): Boolean {
    return rsrpDbm > veryStrongRsrpMinDbm
}

fun PassiveSignalSettings.resolveSignalStrengthTier(
    rsrpDbm: Int?,
    rsrqDb: Int?,
    ignoreRsrqForRate: Boolean = false
): SignalStrengthTier? {
    if (rsrpDbm != null && isRsrpTooWeakForService(rsrpDbm)) return null
    if (!ignoreRsrqForRate && rsrqDb != null && rsrqDb < criticalRsrqDb) {
        return SignalStrengthTier.CRITICAL
    }
    val rsrp = rsrpDbm ?: return null
    return when {
        rsrp > mildRsrpMinDbm -> SignalStrengthTier.MILD
        rsrp > goodRsrpMinDbm -> SignalStrengthTier.GOOD
        rsrp > fairRsrpMinDbm -> SignalStrengthTier.FAIR
        rsrp > poorRsrpMinDbm -> SignalStrengthTier.POOR
        else -> SignalStrengthTier.CRITICAL
    }
}
