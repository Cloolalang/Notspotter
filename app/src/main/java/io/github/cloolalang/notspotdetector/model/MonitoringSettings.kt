package io.github.cloolalang.notspotdetector.model

import android.telephony.SubscriptionManager

data class MonitoringSettings(
    val monitor2gFallback: Boolean = DEFAULT_MONITOR_2G_FALLBACK,
    val subscriptionId: Int = DEFAULT_SUBSCRIPTION_ID,
    val passiveQuietUntilCritical: Boolean = DEFAULT_PASSIVE_QUIET_UNTIL_CRITICAL,
    val passiveMeasurementIntervalMs: Long = DEFAULT_PASSIVE_MEASUREMENT_INTERVAL_MS,
    /** Passive-only alert click rate: 5 = default; lower = slower; higher = faster. */
    val passiveSoundSpeed: Int = DEFAULT_PASSIVE_SOUND_SPEED
) {
    fun normalized(): MonitoringSettings {
        return copy(
            passiveMeasurementIntervalMs = passiveMeasurementIntervalMs.coerceIn(
                MIN_PASSIVE_MEASUREMENT_INTERVAL_MS,
                MAX_PASSIVE_MEASUREMENT_INTERVAL_MS
            ),
            passiveSoundSpeed = passiveSoundSpeed.coerceIn(
                MIN_PASSIVE_SOUND_SPEED,
                MAX_PASSIVE_SOUND_SPEED
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
        const val DEFAULT_PASSIVE_SOUND_SPEED = 5
        const val MIN_PASSIVE_SOUND_SPEED = 1
        const val MAX_PASSIVE_SOUND_SPEED = 20
    }
}
