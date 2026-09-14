package io.github.cloolalang.notspotdetector.model

import android.telephony.SubscriptionManager

data class MonitoringSettings(
    val monitor2gFallback: Boolean = DEFAULT_MONITOR_2G_FALLBACK,
    val subscriptionId: Int = DEFAULT_SUBSCRIPTION_ID,
    val passiveQuietUntilCritical: Boolean = DEFAULT_PASSIVE_QUIET_UNTIL_CRITICAL,
    val passiveMeasurementIntervalMs: Long = DEFAULT_PASSIVE_MEASUREMENT_INTERVAL_MS,
    val rsrpHistogramWindowMs: Long = DEFAULT_RSRP_HISTOGRAM_WINDOW_MS,
    val rsrpHistogramBinningMode: RsrpHistogramBinningMode = RsrpHistogramBinningMode.DEFAULT,
    val rsrpHistogramThreshold1Dbm: Int = RsrpHistogram.DEFAULT_THRESHOLD_1_DBM,
    val rsrpHistogramThreshold2Dbm: Int = RsrpHistogram.DEFAULT_THRESHOLD_2_DBM,
    val rsrpHistogramThreshold3Dbm: Int = RsrpHistogram.DEFAULT_THRESHOLD_3_DBM,
    val fiveGFeaturesEnabled: Boolean = DEFAULT_FIVE_G_FEATURES_ENABLED,
    val showManualSelectOperatorButton: Boolean = DEFAULT_SHOW_MANUAL_SELECT_OPERATOR_BUTTON,
    /** When true, the phone’s allowed RATs exclude 2G. Requires root to apply. */
    val inhibit2g: Boolean = DEFAULT_INHIBIT_2G,
    /** Keep the display awake while monitoring is running (car cradle / field test). */
    val keepScreenOnWhileMonitoring: Boolean = DEFAULT_KEEP_SCREEN_ON_WHILE_MONITORING
) {
    fun normalized(): MonitoringSettings {
        return copy(
            passiveQuietUntilCritical = DEFAULT_PASSIVE_QUIET_UNTIL_CRITICAL,
            passiveMeasurementIntervalMs = DEFAULT_PASSIVE_MEASUREMENT_INTERVAL_MS,
            rsrpHistogramWindowMs = rsrpHistogramWindowMs.coerceToHistogramWindowStep(),
            rsrpHistogramThreshold1Dbm = rsrpHistogramThreshold1Dbm.coerceToHistogramThresholdDbm(),
            rsrpHistogramThreshold2Dbm = rsrpHistogramThreshold2Dbm.coerceToHistogramThresholdDbm(),
            rsrpHistogramThreshold3Dbm = rsrpHistogramThreshold3Dbm.coerceToHistogramThresholdDbm()
        )
    }

    fun rsrpHistogramThresholdsDbm(): List<Int> {
        return listOf(
            rsrpHistogramThreshold1Dbm,
            rsrpHistogramThreshold2Dbm,
            rsrpHistogramThreshold3Dbm
        )
    }

    companion object {
        const val DEFAULT_MONITOR_2G_FALLBACK = true
        const val DEFAULT_SUBSCRIPTION_ID = SubscriptionManager.INVALID_SUBSCRIPTION_ID
        const val DEFAULT_PASSIVE_QUIET_UNTIL_CRITICAL = false
        const val DEFAULT_PASSIVE_MEASUREMENT_INTERVAL_MS = 1_000L
        const val MIN_PASSIVE_MEASUREMENT_INTERVAL_MS = 1_000L
        const val MAX_PASSIVE_MEASUREMENT_INTERVAL_MS = 10_000L
        const val DEFAULT_RSRP_HISTOGRAM_WINDOW_MS = 300_000L
        const val MIN_RSRP_HISTOGRAM_WINDOW_MS = 30_000L
        const val MAX_RSRP_HISTOGRAM_WINDOW_MS = 3_600_000L
        const val RSRP_HISTOGRAM_WINDOW_STEP_MS = 30_000L
        const val DEFAULT_FIVE_G_FEATURES_ENABLED = false
        const val DEFAULT_SHOW_MANUAL_SELECT_OPERATOR_BUTTON = false
        const val DEFAULT_INHIBIT_2G = false
        const val DEFAULT_KEEP_SCREEN_ON_WHILE_MONITORING = true
    }
}

fun Int.coerceToHistogramThresholdDbm(): Int {
    return coerceIn(RsrpHistogram.MIN_THRESHOLD_DBM, RsrpHistogram.MAX_THRESHOLD_DBM)
}

fun Long.coerceToHistogramWindowStep(): Long {
    val stepped = MIN_RSRP_HISTOGRAM_WINDOW_MS +
        (((this - MIN_RSRP_HISTOGRAM_WINDOW_MS) / RSRP_HISTOGRAM_WINDOW_STEP_MS)
            .coerceAtLeast(0) * RSRP_HISTOGRAM_WINDOW_STEP_MS)
    return stepped.coerceIn(
        MIN_RSRP_HISTOGRAM_WINDOW_MS,
        MAX_RSRP_HISTOGRAM_WINDOW_MS
    )
}

private val MIN_RSRP_HISTOGRAM_WINDOW_MS = MonitoringSettings.MIN_RSRP_HISTOGRAM_WINDOW_MS
private val MAX_RSRP_HISTOGRAM_WINDOW_MS = MonitoringSettings.MAX_RSRP_HISTOGRAM_WINDOW_MS
private val RSRP_HISTOGRAM_WINDOW_STEP_MS = MonitoringSettings.RSRP_HISTOGRAM_WINDOW_STEP_MS
