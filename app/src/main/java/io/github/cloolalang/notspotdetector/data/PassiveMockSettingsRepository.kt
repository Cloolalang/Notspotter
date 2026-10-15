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
            rsrqDb = prefs.getInt(KEY_RSRQ_DB, PassiveMockSettings.DEFAULT_RSRQ_DB),
            voiceOnlyNoData = prefs.getBoolean(
                KEY_VOICE_ONLY_NO_DATA,
                PassiveMockSettings.DEFAULT_VOICE_ONLY_NO_DATA
            ),
            pingMockEnabled = prefs.getBoolean(
                KEY_PING_MOCK_ENABLED,
                PassiveMockSettings.DEFAULT_PING_MOCK_ENABLED
            ),
            pingRttMs = prefs.getInt(KEY_PING_RTT_MS, PassiveMockSettings.DEFAULT_PING_RTT_MS),
            packetLossPercent = prefs.getInt(
                KEY_PACKET_LOSS_PERCENT,
                PassiveMockSettings.DEFAULT_PACKET_LOSS_PERCENT
            )
        ).normalized()
    }

    fun save(settings: PassiveMockSettings) {
        val normalized = settings.normalized()
        prefs.edit()
            .putBoolean(KEY_ENABLED, normalized.enabled)
            .putString(KEY_SCENARIO, normalized.scenario.name)
            .putInt(KEY_RSRP_DBM, normalized.rsrpDbm)
            .putInt(KEY_RSRQ_DB, normalized.rsrqDb)
            .putBoolean(KEY_VOICE_ONLY_NO_DATA, normalized.voiceOnlyNoData)
            .putBoolean(KEY_PING_MOCK_ENABLED, normalized.pingMockEnabled)
            .putInt(KEY_PING_RTT_MS, normalized.pingRttMs)
            .putInt(KEY_PACKET_LOSS_PERCENT, normalized.packetLossPercent)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "notspot_passive_mock_settings"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_SCENARIO = "scenario"
        private const val KEY_RSRP_DBM = "rsrp_dbm"
        private const val KEY_RSRQ_DB = "rsrq_db"
        private const val KEY_VOICE_ONLY_NO_DATA = "voice_only_no_data"
        private const val KEY_PING_MOCK_ENABLED = "ping_mock_enabled"
        private const val KEY_PING_RTT_MS = "ping_rtt_ms"
        private const val KEY_PACKET_LOSS_PERCENT = "packet_loss_percent"
    }
}
