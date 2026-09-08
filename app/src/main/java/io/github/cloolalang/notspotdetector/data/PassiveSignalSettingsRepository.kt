package io.github.cloolalang.notspotdetector.data

import android.content.Context
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings

class PassiveSignalSettingsRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): PassiveSignalSettings {
        return PassiveSignalSettings(
            noSignalRsrpDbm = prefs.getInt(KEY_NO_SIGNAL_RSRP, PassiveSignalSettings.DEFAULT_NO_SIGNAL_RSRP_DBM),
            poorRsrpMinDbm = prefs.getInt(KEY_POOR_RSRP_MIN, PassiveSignalSettings.DEFAULT_POOR_RSRP_MIN_DBM),
            fairRsrpMinDbm = prefs.getInt(KEY_FAIR_RSRP_MIN, PassiveSignalSettings.DEFAULT_FAIR_RSRP_MIN_DBM),
            goodRsrpMinDbm = prefs.getInt(KEY_GOOD_RSRP_MIN, PassiveSignalSettings.DEFAULT_GOOD_RSRP_MIN_DBM),
            mildRsrpMinDbm = prefs.getInt(KEY_MILD_RSRP_MIN, PassiveSignalSettings.DEFAULT_MILD_RSRP_MIN_DBM),
            rsrqFairMinDb = prefs.getInt(KEY_RSRQ_FAIR_MIN, PassiveSignalSettings.DEFAULT_RSRQ_FAIR_MIN_DB),
            noisyRsrqPassiveClicks = prefs.getBoolean(
                KEY_NOISY_RSRQ_PASSIVE_CLICKS,
                PassiveSignalSettings.DEFAULT_NOISY_RSRQ_PASSIVE_CLICKS
            ),
            quietAlertRsrqDb = prefs.getInt(KEY_QUIET_ALERT_RSRQ, PassiveSignalSettings.DEFAULT_QUIET_ALERT_RSRQ_DB),
            quietAlertRsrpMaxDbm = prefs.getInt(
                KEY_QUIET_ALERT_RSRP_MAX,
                PassiveSignalSettings.DEFAULT_QUIET_ALERT_RSRP_MAX_DBM
            )
        ).normalized()
    }

    fun save(settings: PassiveSignalSettings) {
        val normalized = settings.normalized()
        prefs.edit()
            .putInt(KEY_NO_SIGNAL_RSRP, normalized.noSignalRsrpDbm)
            .putInt(KEY_POOR_RSRP_MIN, normalized.poorRsrpMinDbm)
            .putInt(KEY_FAIR_RSRP_MIN, normalized.fairRsrpMinDbm)
            .putInt(KEY_GOOD_RSRP_MIN, normalized.goodRsrpMinDbm)
            .putInt(KEY_MILD_RSRP_MIN, normalized.mildRsrpMinDbm)
            .putInt(KEY_RSRQ_FAIR_MIN, normalized.rsrqFairMinDb)
            .putBoolean(KEY_NOISY_RSRQ_PASSIVE_CLICKS, normalized.noisyRsrqPassiveClicks)
            .putInt(KEY_QUIET_ALERT_RSRQ, normalized.quietAlertRsrqDb)
            .putInt(KEY_QUIET_ALERT_RSRP_MAX, normalized.quietAlertRsrpMaxDbm)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "notspot_passive_signal_settings"
        private const val KEY_NO_SIGNAL_RSRP = "no_signal_rsrp_dbm"
        private const val KEY_POOR_RSRP_MIN = "poor_rsrp_min_dbm"
        private const val KEY_FAIR_RSRP_MIN = "fair_rsrp_min_dbm"
        private const val KEY_GOOD_RSRP_MIN = "good_rsrp_min_dbm"
        private const val KEY_MILD_RSRP_MIN = "mild_rsrp_min_dbm"
        private const val KEY_RSRQ_FAIR_MIN = "rsrq_fair_min_db"
        private const val KEY_NOISY_RSRQ_PASSIVE_CLICKS = "noisy_rsrq_passive_clicks"
        private const val KEY_QUIET_ALERT_RSRQ = "quiet_alert_rsrq_db"
        private const val KEY_QUIET_ALERT_RSRP_MAX = "quiet_alert_rsrp_max_dbm"
    }
}
