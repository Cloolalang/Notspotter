package io.github.cloolalang.notspotdetector.data

import android.content.Context
import io.github.cloolalang.notspotdetector.model.MonitoringSettings

class MonitoringSettingsRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): MonitoringSettings {
        return MonitoringSettings(
            monitor2gFallback = prefs.getBoolean(
                KEY_MONITOR_2G_FALLBACK,
                MonitoringSettings.DEFAULT_MONITOR_2G_FALLBACK
            ),
            subscriptionId = prefs.getInt(
                KEY_SUBSCRIPTION_ID,
                MonitoringSettings.DEFAULT_SUBSCRIPTION_ID
            ),
            passiveQuietUntilCritical = prefs.getBoolean(
                KEY_PASSIVE_QUIET_UNTIL_CRITICAL,
                MonitoringSettings.DEFAULT_PASSIVE_QUIET_UNTIL_CRITICAL
            ),
            passiveMeasurementIntervalMs = prefs.getLong(
                KEY_PASSIVE_MEASUREMENT_INTERVAL,
                MonitoringSettings.DEFAULT_PASSIVE_MEASUREMENT_INTERVAL_MS
            ),
            rsrpHistogramWindowMs = prefs.getLong(
                KEY_RSRP_HISTOGRAM_WINDOW,
                MonitoringSettings.DEFAULT_RSRP_HISTOGRAM_WINDOW_MS
            )
        ).normalized()
    }

    fun save(settings: MonitoringSettings) {
        val normalized = settings.normalized()
        prefs.edit()
            .putBoolean(KEY_MONITOR_2G_FALLBACK, normalized.monitor2gFallback)
            .putInt(KEY_SUBSCRIPTION_ID, normalized.subscriptionId)
            .putBoolean(KEY_PASSIVE_QUIET_UNTIL_CRITICAL, normalized.passiveQuietUntilCritical)
            .putLong(KEY_PASSIVE_MEASUREMENT_INTERVAL, normalized.passiveMeasurementIntervalMs)
            .putLong(KEY_RSRP_HISTOGRAM_WINDOW, normalized.rsrpHistogramWindowMs)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "notspot_monitoring_settings"
        private const val KEY_MONITOR_2G_FALLBACK = "monitor_2g_fallback"
        private const val KEY_SUBSCRIPTION_ID = "subscription_id"
        private const val KEY_PASSIVE_QUIET_UNTIL_CRITICAL = "passive_quiet_until_critical"
        private const val KEY_PASSIVE_MEASUREMENT_INTERVAL = "passive_measurement_interval_ms"
        private const val KEY_RSRP_HISTOGRAM_WINDOW = "rsrp_histogram_window_ms"
    }
}
