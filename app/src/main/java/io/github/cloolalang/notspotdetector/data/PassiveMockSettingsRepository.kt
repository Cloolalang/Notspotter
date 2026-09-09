package io.github.cloolalang.notspotdetector.data

import android.content.Context
import io.github.cloolalang.notspotdetector.model.MockNetworkScenario
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings

class PassiveMockSettingsRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): PassiveMockSettings {
        return PassiveMockSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, PassiveMockSettings.DEFAULT_ENABLED),
            scenario = MockNetworkScenario.fromStoredName(prefs.getString(KEY_SCENARIO, null)),
            rsrpDbm = prefs.getInt(KEY_RSRP_DBM, PassiveMockSettings.DEFAULT_RSRP_DBM),
            rsrqDb = prefs.getInt(KEY_RSRQ_DB, PassiveMockSettings.DEFAULT_RSRQ_DB)
        ).normalized()
    }

    fun save(settings: PassiveMockSettings) {
        val normalized = settings.normalized()
        prefs.edit()
            .putBoolean(KEY_ENABLED, normalized.enabled)
            .putString(KEY_SCENARIO, normalized.scenario.name)
            .putInt(KEY_RSRP_DBM, normalized.rsrpDbm)
            .putInt(KEY_RSRQ_DB, normalized.rsrqDb)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "notspot_passive_mock_settings"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_SCENARIO = "scenario"
        private const val KEY_RSRP_DBM = "rsrp_dbm"
        private const val KEY_RSRQ_DB = "rsrq_db"
    }
}
