package io.github.cloolalang.notspotdetector.model

import android.telephony.SubscriptionManager

data class MonitoringSettings(
    val monitor2gFallback: Boolean = DEFAULT_MONITOR_2G_FALLBACK,
    val subscriptionId: Int = DEFAULT_SUBSCRIPTION_ID,
    val passiveQuietUntilCritical: Boolean = DEFAULT_PASSIVE_QUIET_UNTIL_CRITICAL,
    val passiveMeasurementIntervalMs: Long = DEFAULT_PASSIVE_MEASUREMENT_INTERVAL_MS
) {
    fun normalized(): MonitoringSettings {
        return copy(
            passiveMeasurementIntervalMs = passiveMeasurementIntervalMs.coerceIn(
                MIN_PASSIVE_MEASUREMENT_INTERVAL_MS,
                MAX_PASSIVE_MEASUREMENT_INTERVAL_MS
            )
        )
    }

    companion object {
        const val DEFAULT_MONITOR_2G_FALLBACK = false
        const val DEFAULT_SUBSCRIPTION_ID = SubscriptionManager.INVALID_SUBSCRIPTION_ID
        const val DEFAULT_PASSIVE_QUIET_UNTIL_CRITICAL = false
        const val DEFAULT_PASSIVE_MEASUREMENT_INTERVAL_MS = 5_000L
        const val MIN_PASSIVE_MEASUREMENT_INTERVAL_MS = 1_000L
        const val MAX_PASSIVE_MEASUREMENT_INTERVAL_MS = 10_000L
    }
}
