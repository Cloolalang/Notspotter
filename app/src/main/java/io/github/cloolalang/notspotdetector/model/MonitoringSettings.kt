package io.github.cloolalang.notspotdetector.model

import android.telephony.SubscriptionManager

data class MonitoringSettings(
    val monitor2gFallback: Boolean = DEFAULT_MONITOR_2G_FALLBACK,
    val subscriptionId: Int = DEFAULT_SUBSCRIPTION_ID,
    val passiveQuietUntilCritical: Boolean = DEFAULT_PASSIVE_QUIET_UNTIL_CRITICAL,
    val passiveMeasurementIntervalMs: Long = DEFAULT_PASSIVE_MEASUREMENT_INTERVAL_MS,
    val rsrpHistogramWindowMs: Long = DEFAULT_RSRP_HISTOGRAM_WINDOW_MS
) {
    fun normalized(): MonitoringSettings {
        return copy(
            passiveMeasurementIntervalMs = passiveMeasurementIntervalMs.coerceIn(
                MIN_PASSIVE_MEASUREMENT_INTERVAL_MS,
                MAX_PASSIVE_MEASUREMENT_INTERVAL_MS
            ),
            rsrpHistogramWindowMs = rsrpHistogramWindowMs.coerceToHistogramWindowStep()
        )
    }

    companion object {
        const val DEFAULT_MONITOR_2G_FALLBACK = false
        const val DEFAULT_SUBSCRIPTION_ID = SubscriptionManager.INVALID_SUBSCRIPTION_ID
        const val DEFAULT_PASSIVE_QUIET_UNTIL_CRITICAL = false
        const val DEFAULT_PASSIVE_MEASUREMENT_INTERVAL_MS = 5_000L
        const val MIN_PASSIVE_MEASUREMENT_INTERVAL_MS = 1_000L
        const val MAX_PASSIVE_MEASUREMENT_INTERVAL_MS = 10_000L
        const val DEFAULT_RSRP_HISTOGRAM_WINDOW_MS = 30_000L
        const val MIN_RSRP_HISTOGRAM_WINDOW_MS = 30_000L
        const val MAX_RSRP_HISTOGRAM_WINDOW_MS = 300_000L
        const val RSRP_HISTOGRAM_WINDOW_STEP_MS = 30_000L
    }
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
