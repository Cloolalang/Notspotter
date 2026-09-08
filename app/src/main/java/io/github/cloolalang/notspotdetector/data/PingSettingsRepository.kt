package io.github.cloolalang.notspotdetector.data

import android.content.Context
import io.github.cloolalang.notspotdetector.model.PingSettings

class PingSettingsRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): PingSettings {
        return PingSettings(
            host = prefs.getString(KEY_HOST, PingSettings.DEFAULT_HOST) ?: PingSettings.DEFAULT_HOST,
            port = prefs.getInt(KEY_PORT, PingSettings.DEFAULT_PORT),
            pingsPerTest = prefs.getInt(KEY_PINGS_PER_TEST, PingSettings.DEFAULT_PINGS_PER_TEST),
            testIntervalMs = prefs.getLong(KEY_TEST_INTERVAL, PingSettings.DEFAULT_TEST_INTERVAL_MS)
        ).normalized()
    }

    fun save(settings: PingSettings) {
        val normalized = settings.normalized()
        prefs.edit()
            .putString(KEY_HOST, normalized.host)
            .putInt(KEY_PORT, normalized.port)
            .putInt(KEY_PINGS_PER_TEST, normalized.pingsPerTest)
            .putLong(KEY_TEST_INTERVAL, normalized.testIntervalMs)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "notspot_ping_settings"
        private const val KEY_HOST = "host"
        private const val KEY_PORT = "port"
        private const val KEY_PINGS_PER_TEST = "pings_per_test"
        private const val KEY_TEST_INTERVAL = "test_interval_ms"
    }
}
