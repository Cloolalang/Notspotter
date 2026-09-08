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
            veryStrongRsrpMinDbm = prefs.getInt(
                KEY_VERY_STRONG_RSRP_MIN,
                PassiveSignalSettings.DEFAULT_VERY_STRONG_RSRP_MIN_DBM
            ),
            rsrqFairMinDb = prefs.getInt(KEY_RSRQ_FAIR_MIN, PassiveSignalSettings.DEFAULT_RSRQ_FAIR_MIN_DB),
            noisyRsrqPassiveClicks = prefs.getBoolean(
                KEY_NOISY_RSRQ_PASSIVE_CLICKS,
                PassiveSignalSettings.DEFAULT_NOISY_RSRQ_PASSIVE_CLICKS
            ),
            quietAlertRsrqDb = prefs.getInt(KEY_QUIET_ALERT_RSRQ, PassiveSignalSettings.DEFAULT_QUIET_ALERT_RSRQ_DB),
            quietAlertRsrpMaxDbm = prefs.getInt(
                KEY_QUIET_ALERT_RSRP_MAX,
                PassiveSignalSettings.DEFAULT_QUIET_ALERT_RSRP_MAX_DBM
            ),
            criticalTierClickIntervalMs = prefs.getInt(
                KEY_CRITICAL_TIER_CLICK_MS,
                PassiveSignalSettings.DEFAULT_CRITICAL_TIER_CLICK_INTERVAL_MS
            ),
            poorTierClickIntervalMs = prefs.getInt(
                KEY_POOR_TIER_CLICK_MS,
                PassiveSignalSettings.DEFAULT_POOR_TIER_CLICK_INTERVAL_MS
            ),
            fairTierClickIntervalMs = prefs.getInt(
                KEY_FAIR_TIER_CLICK_MS,
                PassiveSignalSettings.DEFAULT_FAIR_TIER_CLICK_INTERVAL_MS
            ),
            goodTierClickIntervalMs = prefs.getInt(
                KEY_GOOD_TIER_CLICK_MS,
                PassiveSignalSettings.DEFAULT_GOOD_TIER_CLICK_INTERVAL_MS
            ),
            mildTierClickIntervalMs = prefs.getInt(
                KEY_MILD_TIER_CLICK_MS,
                PassiveSignalSettings.DEFAULT_MILD_TIER_CLICK_INTERVAL_MS
            ),
            veryStrongTierClickIntervalMs = prefs.getInt(
                KEY_VERY_STRONG_TIER_CLICK_MS,
                PassiveSignalSettings.DEFAULT_VERY_STRONG_TIER_CLICK_INTERVAL_MS
            ),
            veryStrongTierSoundEnabled = prefs.getBoolean(
                KEY_VERY_STRONG_TIER_SOUND,
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            mildTierSoundEnabled = prefs.getBoolean(
                KEY_MILD_TIER_SOUND,
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            goodTierSoundEnabled = prefs.getBoolean(
                KEY_GOOD_TIER_SOUND,
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            fairTierSoundEnabled = prefs.getBoolean(
                KEY_FAIR_TIER_SOUND,
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            poorTierSoundEnabled = prefs.getBoolean(
                KEY_POOR_TIER_SOUND,
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            criticalTierSoundEnabled = prefs.getBoolean(
                KEY_CRITICAL_TIER_SOUND,
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            g2StrongTierClickIntervalMs = prefs.getInt(
                KEY_G2_STRONG_TIER_CLICK_MS,
                PassiveSignalSettings.DEFAULT_G2_STRONG_TIER_CLICK_INTERVAL_MS
            ),
            g2WeakTierClickIntervalMs = prefs.getInt(
                KEY_G2_WEAK_TIER_CLICK_MS,
                PassiveSignalSettings.DEFAULT_G2_WEAK_TIER_CLICK_INTERVAL_MS
            ),
            g2StrongTierSoundEnabled = prefs.getBoolean(
                KEY_G2_STRONG_TIER_SOUND,
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            g2WeakTierSoundEnabled = prefs.getBoolean(
                KEY_G2_WEAK_TIER_SOUND,
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            deadzoneTierClickIntervalMs = prefs.getInt(
                KEY_DEADZONE_TIER_CLICK_MS,
                PassiveSignalSettings.DEFAULT_DEADZONE_TIER_CLICK_INTERVAL_MS
            ),
            deadzoneTierSoundEnabled = prefs.getBoolean(
                KEY_DEADZONE_TIER_SOUND,
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            deadzoneTierPulseDurationMs = prefs.getInt(
                KEY_DEADZONE_TIER_PULSE_MS,
                PassiveSignalSettings.DEFAULT_DEADZONE_TIER_PULSE_DURATION_MS
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
            .putInt(KEY_VERY_STRONG_RSRP_MIN, normalized.veryStrongRsrpMinDbm)
            .putInt(KEY_RSRQ_FAIR_MIN, normalized.rsrqFairMinDb)
            .putBoolean(KEY_NOISY_RSRQ_PASSIVE_CLICKS, normalized.noisyRsrqPassiveClicks)
            .putInt(KEY_QUIET_ALERT_RSRQ, normalized.quietAlertRsrqDb)
            .putInt(KEY_QUIET_ALERT_RSRP_MAX, normalized.quietAlertRsrpMaxDbm)
            .putInt(KEY_CRITICAL_TIER_CLICK_MS, normalized.criticalTierClickIntervalMs)
            .putInt(KEY_POOR_TIER_CLICK_MS, normalized.poorTierClickIntervalMs)
            .putInt(KEY_FAIR_TIER_CLICK_MS, normalized.fairTierClickIntervalMs)
            .putInt(KEY_GOOD_TIER_CLICK_MS, normalized.goodTierClickIntervalMs)
            .putInt(KEY_MILD_TIER_CLICK_MS, normalized.mildTierClickIntervalMs)
            .putInt(KEY_VERY_STRONG_TIER_CLICK_MS, normalized.veryStrongTierClickIntervalMs)
            .putBoolean(KEY_VERY_STRONG_TIER_SOUND, normalized.veryStrongTierSoundEnabled)
            .putBoolean(KEY_MILD_TIER_SOUND, normalized.mildTierSoundEnabled)
            .putBoolean(KEY_GOOD_TIER_SOUND, normalized.goodTierSoundEnabled)
            .putBoolean(KEY_FAIR_TIER_SOUND, normalized.fairTierSoundEnabled)
            .putBoolean(KEY_POOR_TIER_SOUND, normalized.poorTierSoundEnabled)
            .putBoolean(KEY_CRITICAL_TIER_SOUND, normalized.criticalTierSoundEnabled)
            .putInt(KEY_G2_STRONG_TIER_CLICK_MS, normalized.g2StrongTierClickIntervalMs)
            .putInt(KEY_G2_WEAK_TIER_CLICK_MS, normalized.g2WeakTierClickIntervalMs)
            .putBoolean(KEY_G2_STRONG_TIER_SOUND, normalized.g2StrongTierSoundEnabled)
            .putBoolean(KEY_G2_WEAK_TIER_SOUND, normalized.g2WeakTierSoundEnabled)
            .putInt(KEY_DEADZONE_TIER_CLICK_MS, normalized.deadzoneTierClickIntervalMs)
            .putBoolean(KEY_DEADZONE_TIER_SOUND, normalized.deadzoneTierSoundEnabled)
            .putInt(KEY_DEADZONE_TIER_PULSE_MS, normalized.deadzoneTierPulseDurationMs)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "notspot_passive_signal_settings"
        private const val KEY_NO_SIGNAL_RSRP = "no_signal_rsrp_dbm"
        private const val KEY_POOR_RSRP_MIN = "poor_rsrp_min_dbm"
        private const val KEY_FAIR_RSRP_MIN = "fair_rsrp_min_dbm"
        private const val KEY_GOOD_RSRP_MIN = "good_rsrp_min_dbm"
        private const val KEY_MILD_RSRP_MIN = "mild_rsrp_min_dbm"
        private const val KEY_VERY_STRONG_RSRP_MIN = "very_strong_rsrp_min_dbm"
        private const val KEY_RSRQ_FAIR_MIN = "rsrq_fair_min_db"
        private const val KEY_NOISY_RSRQ_PASSIVE_CLICKS = "noisy_rsrq_passive_clicks"
        private const val KEY_QUIET_ALERT_RSRQ = "quiet_alert_rsrq_db"
        private const val KEY_QUIET_ALERT_RSRP_MAX = "quiet_alert_rsrp_max_dbm"
        private const val KEY_CRITICAL_TIER_CLICK_MS = "critical_tier_click_ms"
        private const val KEY_POOR_TIER_CLICK_MS = "poor_tier_click_ms"
        private const val KEY_FAIR_TIER_CLICK_MS = "fair_tier_click_ms"
        private const val KEY_GOOD_TIER_CLICK_MS = "good_tier_click_ms"
        private const val KEY_MILD_TIER_CLICK_MS = "mild_tier_click_ms"
        private const val KEY_VERY_STRONG_TIER_CLICK_MS = "very_strong_tier_click_ms"
        private const val KEY_VERY_STRONG_TIER_SOUND = "very_strong_tier_sound_enabled"
        private const val KEY_MILD_TIER_SOUND = "mild_tier_sound_enabled"
        private const val KEY_GOOD_TIER_SOUND = "good_tier_sound_enabled"
        private const val KEY_FAIR_TIER_SOUND = "fair_tier_sound_enabled"
        private const val KEY_POOR_TIER_SOUND = "poor_tier_sound_enabled"
        private const val KEY_CRITICAL_TIER_SOUND = "critical_tier_sound_enabled"
        private const val KEY_G2_STRONG_TIER_CLICK_MS = "g2_strong_tier_click_ms"
        private const val KEY_G2_WEAK_TIER_CLICK_MS = "g2_weak_tier_click_ms"
        private const val KEY_G2_STRONG_TIER_SOUND = "g2_strong_tier_sound_enabled"
        private const val KEY_G2_WEAK_TIER_SOUND = "g2_weak_tier_sound_enabled"
        private const val KEY_DEADZONE_TIER_CLICK_MS = "deadzone_tier_click_ms"
        private const val KEY_DEADZONE_TIER_SOUND = "deadzone_tier_sound_enabled"
        private const val KEY_DEADZONE_TIER_PULSE_MS = "deadzone_tier_pulse_ms"
    }
}
