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
    val fiveGFeaturesEnabled: Boolean = DEFAULT_FIVE_G_FEATURES_ENABLED
) {
    fun normalized(): MonitoringSettings {
        return copy(
            passiveMeasurementIntervalMs = passiveMeasurementIntervalMs.coerceIn(
                MIN_PASSIVE_MEASUREMENT_INTERVAL_MS,
                MAX_PASSIVE_MEASUREMENT_INTERVAL_MS
            ),
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
