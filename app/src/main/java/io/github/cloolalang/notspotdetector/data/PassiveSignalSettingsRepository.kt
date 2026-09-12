package io.github.cloolalang.notspotdetector.data

import android.content.Context
import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings

class PassiveSignalSettingsRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * @param legacySignalPulseDurationMs Seed for [PassiveSignalSettings.criticalTierPulseDurationMs] the first
     * time it is loaded (before it had its own persisted key), taken from the global signal pulse duration.
     * @param legacyLevelRangeBcdPulseDurationMs Seed for the Level Range A–D (RXSS 2–5) per-tier pulse durations
     * the first time they are loaded (before each range had its own persisted key), taken from the formerly-shared
     * Level Range B–D pulse duration.
     */
    fun load(
        legacySignalPulseDurationMs: Int = AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS,
        legacyLevelRangeBcdPulseDurationMs: Int = AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
    ): PassiveSignalSettings {
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
            rsrqTierSoundEnabled = prefs.getBoolean(
                KEY_RSRQ_TIER_SOUND,
                prefs.getBoolean(KEY_NOISY_RSRQ_PASSIVE_CLICKS, PassiveSignalSettings.DEFAULT_RSRQ_TIER_SOUND_ENABLED)
            ),
            rsrqTierCoupledToSignalTier = prefs.getBoolean(
                KEY_RSRQ_TIER_COUPLED,
                PassiveSignalSettings.DEFAULT_RSRQ_TIER_COUPLED_TO_SIGNAL_TIER
            ),
            rsrqTierWhiteNoiseVolume = prefs.getFloat(
                KEY_RSRQ_TIER_WHITE_NOISE_VOLUME,
                PassiveSignalSettings.DEFAULT_RSRQ_TIER_WHITE_NOISE_VOLUME
            ),
            rsrqTierClickIntervalMs = prefs.getInt(
                KEY_RSRQ_TIER_CLICK_MS,
                PassiveSignalSettings.DEFAULT_RSRQ_TIER_CLICK_INTERVAL_MS
            ),
            rsrqTierPulseDurationMs = prefs.getInt(
                KEY_RSRQ_TIER_PULSE_MS,
                PassiveSignalSettings.DEFAULT_RSRQ_TIER_PULSE_DURATION_MS
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
            criticalTierPulseDurationMs = prefs.getInt(
                KEY_CRITICAL_TIER_PULSE_MS,
                legacySignalPulseDurationMs
            ),
            mildTierPulseDurationMs = prefs.getInt(
                KEY_MILD_TIER_PULSE_MS,
                legacyLevelRangeBcdPulseDurationMs
            ),
            goodTierPulseDurationMs = prefs.getInt(
                KEY_GOOD_TIER_PULSE_MS,
                legacyLevelRangeBcdPulseDurationMs
            ),
            fairTierPulseDurationMs = prefs.getInt(
                KEY_FAIR_TIER_PULSE_MS,
                legacyLevelRangeBcdPulseDurationMs
            ),
            poorTierPulseDurationMs = prefs.getInt(
                KEY_POOR_TIER_PULSE_MS,
                legacyLevelRangeBcdPulseDurationMs
            ),
            levelRangeAbcdClickIntervalMs = decodeLevelRangeAbcdClickIntervalMs(prefs),
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
            g2StrongTierPulseDurationMs = prefs.getInt(
                KEY_G2_STRONG_TIER_PULSE_MS,
                PassiveSignalSettings.DEFAULT_G2_STRONG_TIER_PULSE_DURATION_MS
            ),
            g2WeakTierClickIntervalMs = prefs.getInt(
                KEY_G2_WEAK_TIER_CLICK_MS,
                PassiveSignalSettings.DEFAULT_G2_WEAK_TIER_CLICK_INTERVAL_MS
            ),
            g2WeakTierPulseDurationMs = prefs.getInt(
                KEY_G2_WEAK_TIER_PULSE_MS,
                PassiveSignalSettings.DEFAULT_G2_WEAK_TIER_PULSE_DURATION_MS
            ),
            g2StrongTierSoundEnabled = prefs.getBoolean(
                KEY_G2_STRONG_TIER_SOUND,
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            g2WeakTierSoundEnabled = prefs.getBoolean(
                KEY_G2_WEAK_TIER_SOUND,
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            g2NoSignalTierClickIntervalMs = prefs.getInt(
                KEY_G2_NO_SIGNAL_TIER_CLICK_MS,
                PassiveSignalSettings.DEFAULT_G2_NO_SIGNAL_TIER_CLICK_INTERVAL_MS
            ),
            g2NoSignalTierSoundEnabled = prefs.getBoolean(
                KEY_G2_NO_SIGNAL_TIER_SOUND,
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            g2NoSignalTierPulseDurationMs = prefs.getInt(
                KEY_G2_NO_SIGNAL_TIER_PULSE_MS,
                PassiveSignalSettings.DEFAULT_G2_NO_SIGNAL_TIER_PULSE_DURATION_MS
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
            ),
            noSignalTierClickIntervalMs = prefs.getInt(
                KEY_NO_SIGNAL_TIER_CLICK_MS,
                PassiveSignalSettings.DEFAULT_NO_SIGNAL_TIER_CLICK_INTERVAL_MS
            ),
            noSignalTierSoundEnabled = prefs.getBoolean(
                KEY_NO_SIGNAL_TIER_SOUND,
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            noSignalTierPulseDurationMs = prefs.getInt(
                KEY_NO_SIGNAL_TIER_PULSE_MS,
                PassiveSignalSettings.DEFAULT_NO_SIGNAL_TIER_PULSE_DURATION_MS
            ),
            searching2gTierClickIntervalMs = prefs.getInt(
                KEY_SEARCHING_2G_TIER_CLICK_MS,
                PassiveSignalSettings.DEFAULT_SEARCHING_2G_TIER_CLICK_INTERVAL_MS
            ),
            searching2gTierSoundEnabled = prefs.getBoolean(
                KEY_SEARCHING_2G_TIER_SOUND,
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            searching2gTierPulseDurationMs = prefs.getInt(
                KEY_SEARCHING_2G_TIER_PULSE_MS,
                PassiveSignalSettings.DEFAULT_SEARCHING_2G_TIER_PULSE_DURATION_MS
            ),
            limitedServiceTierClickIntervalMs = prefs.getInt(
                KEY_LIMITED_SERVICE_TIER_CLICK_MS,
                PassiveSignalSettings.DEFAULT_LIMITED_SERVICE_TIER_CLICK_INTERVAL_MS
            ),
            limitedServiceTierSoundEnabled = prefs.getBoolean(
                KEY_LIMITED_SERVICE_TIER_SOUND,
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            limitedServiceTierPulseDurationMs = prefs.getInt(
                KEY_LIMITED_SERVICE_TIER_PULSE_MS,
                PassiveSignalSettings.DEFAULT_LIMITED_SERVICE_TIER_PULSE_DURATION_MS
            ),
            limitedAlt2gTierClickIntervalMs = prefs.getInt(
                KEY_LIMITED_ALT_2G_TIER_CLICK_MS,
                PassiveSignalSettings.DEFAULT_LIMITED_ALT_2G_TIER_CLICK_INTERVAL_MS
            ),
            limitedAlt2gTierSoundEnabled = prefs.getBoolean(
                KEY_LIMITED_ALT_2G_TIER_SOUND,
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            limitedAlt2gTierPulseDurationMs = prefs.getInt(
                KEY_LIMITED_ALT_2G_TIER_PULSE_MS,
                PassiveSignalSettings.DEFAULT_LIMITED_ALT_2G_TIER_PULSE_DURATION_MS
            ),
            wifiCallingTierClickIntervalMs = prefs.getInt(
                KEY_WIFI_CALLING_TIER_CLICK_MS,
                PassiveSignalSettings.DEFAULT_WIFI_CALLING_TIER_CLICK_INTERVAL_MS
            ),
            wifiCallingTierSoundEnabled = prefs.getBoolean(
                KEY_WIFI_CALLING_TIER_SOUND,
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            wifiCallingTierPulseDurationMs = prefs.getInt(
                KEY_WIFI_CALLING_TIER_PULSE_MS,
                PassiveSignalSettings.DEFAULT_WIFI_CALLING_TIER_PULSE_DURATION_MS
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
            .putBoolean(KEY_RSRQ_TIER_SOUND, normalized.rsrqTierSoundEnabled)
            .putBoolean(KEY_RSRQ_TIER_COUPLED, normalized.rsrqTierCoupledToSignalTier)
            .putFloat(KEY_RSRQ_TIER_WHITE_NOISE_VOLUME, normalized.rsrqTierWhiteNoiseVolume)
            .putInt(KEY_RSRQ_TIER_CLICK_MS, normalized.rsrqTierClickIntervalMs)
            .putInt(KEY_RSRQ_TIER_PULSE_MS, normalized.rsrqTierPulseDurationMs)
            .putInt(KEY_QUIET_ALERT_RSRQ, normalized.quietAlertRsrqDb)
            .putInt(KEY_QUIET_ALERT_RSRP_MAX, normalized.quietAlertRsrpMaxDbm)
            .putInt(KEY_CRITICAL_TIER_CLICK_MS, normalized.criticalTierClickIntervalMs)
            .putInt(KEY_CRITICAL_TIER_PULSE_MS, normalized.criticalTierPulseDurationMs)
            .putInt(KEY_MILD_TIER_PULSE_MS, normalized.mildTierPulseDurationMs)
            .putInt(KEY_GOOD_TIER_PULSE_MS, normalized.goodTierPulseDurationMs)
            .putInt(KEY_FAIR_TIER_PULSE_MS, normalized.fairTierPulseDurationMs)
            .putInt(KEY_POOR_TIER_PULSE_MS, normalized.poorTierPulseDurationMs)
            .putInt(KEY_LEVEL_RANGE_ABCD_CLICK_MS, normalized.levelRangeAbcdClickIntervalMs)
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
            .putInt(KEY_G2_STRONG_TIER_PULSE_MS, normalized.g2StrongTierPulseDurationMs)
            .putInt(KEY_G2_WEAK_TIER_CLICK_MS, normalized.g2WeakTierClickIntervalMs)
            .putInt(KEY_G2_WEAK_TIER_PULSE_MS, normalized.g2WeakTierPulseDurationMs)
            .putBoolean(KEY_G2_STRONG_TIER_SOUND, normalized.g2StrongTierSoundEnabled)
            .putBoolean(KEY_G2_WEAK_TIER_SOUND, normalized.g2WeakTierSoundEnabled)
            .putInt(KEY_G2_NO_SIGNAL_TIER_CLICK_MS, normalized.g2NoSignalTierClickIntervalMs)
            .putBoolean(KEY_G2_NO_SIGNAL_TIER_SOUND, normalized.g2NoSignalTierSoundEnabled)
            .putInt(KEY_G2_NO_SIGNAL_TIER_PULSE_MS, normalized.g2NoSignalTierPulseDurationMs)
            .putInt(KEY_DEADZONE_TIER_CLICK_MS, normalized.deadzoneTierClickIntervalMs)
            .putBoolean(KEY_DEADZONE_TIER_SOUND, normalized.deadzoneTierSoundEnabled)
            .putInt(KEY_DEADZONE_TIER_PULSE_MS, normalized.deadzoneTierPulseDurationMs)
            .putInt(KEY_NO_SIGNAL_TIER_CLICK_MS, normalized.noSignalTierClickIntervalMs)
            .putBoolean(KEY_NO_SIGNAL_TIER_SOUND, normalized.noSignalTierSoundEnabled)
            .putInt(KEY_NO_SIGNAL_TIER_PULSE_MS, normalized.noSignalTierPulseDurationMs)
            .putInt(KEY_SEARCHING_2G_TIER_CLICK_MS, normalized.searching2gTierClickIntervalMs)
            .putBoolean(KEY_SEARCHING_2G_TIER_SOUND, normalized.searching2gTierSoundEnabled)
            .putInt(KEY_SEARCHING_2G_TIER_PULSE_MS, normalized.searching2gTierPulseDurationMs)
            .putInt(KEY_LIMITED_SERVICE_TIER_CLICK_MS, normalized.limitedServiceTierClickIntervalMs)
            .putBoolean(KEY_LIMITED_SERVICE_TIER_SOUND, normalized.limitedServiceTierSoundEnabled)
            .putInt(KEY_LIMITED_SERVICE_TIER_PULSE_MS, normalized.limitedServiceTierPulseDurationMs)
            .putInt(KEY_LIMITED_ALT_2G_TIER_CLICK_MS, normalized.limitedAlt2gTierClickIntervalMs)
            .putBoolean(KEY_LIMITED_ALT_2G_TIER_SOUND, normalized.limitedAlt2gTierSoundEnabled)
            .putInt(KEY_LIMITED_ALT_2G_TIER_PULSE_MS, normalized.limitedAlt2gTierPulseDurationMs)
            .putInt(KEY_WIFI_CALLING_TIER_CLICK_MS, normalized.wifiCallingTierClickIntervalMs)
            .putBoolean(KEY_WIFI_CALLING_TIER_SOUND, normalized.wifiCallingTierSoundEnabled)
            .putInt(KEY_WIFI_CALLING_TIER_PULSE_MS, normalized.wifiCallingTierPulseDurationMs)
            .apply()
    }

    private fun decodeLevelRangeAbcdClickIntervalMs(
        prefs: android.content.SharedPreferences
    ): Int {
        if (prefs.contains(KEY_LEVEL_RANGE_ABCD_CLICK_MS)) {
            return prefs.getInt(
                KEY_LEVEL_RANGE_ABCD_CLICK_MS,
                PassiveSignalSettings.DEFAULT_LEVEL_RANGE_ABCD_CLICK_INTERVAL_MS
            )
        }
        return prefs.getInt(
            KEY_GOOD_TIER_CLICK_MS,
            PassiveSignalSettings.DEFAULT_LEVEL_RANGE_ABCD_CLICK_INTERVAL_MS
        )
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
        private const val KEY_RSRQ_TIER_SOUND = "rsrq_tier_sound_enabled"
        private const val KEY_RSRQ_TIER_COUPLED = "rsrq_tier_coupled_to_signal_tier"
        private const val KEY_RSRQ_TIER_WHITE_NOISE_VOLUME = "rsrq_tier_white_noise_volume"
        private const val KEY_RSRQ_TIER_CLICK_MS = "rsrq_tier_click_ms"
        private const val KEY_RSRQ_TIER_PULSE_MS = "rsrq_tier_pulse_ms"
        private const val KEY_QUIET_ALERT_RSRQ = "quiet_alert_rsrq_db"
        private const val KEY_QUIET_ALERT_RSRP_MAX = "quiet_alert_rsrp_max_dbm"
        private const val KEY_CRITICAL_TIER_CLICK_MS = "critical_tier_click_ms"
        private const val KEY_CRITICAL_TIER_PULSE_MS = "critical_tier_pulse_ms"
        private const val KEY_MILD_TIER_PULSE_MS = "mild_tier_pulse_ms"
        private const val KEY_GOOD_TIER_PULSE_MS = "good_tier_pulse_ms"
        private const val KEY_FAIR_TIER_PULSE_MS = "fair_tier_pulse_ms"
        private const val KEY_POOR_TIER_PULSE_MS = "poor_tier_pulse_ms"
        private const val KEY_LEVEL_RANGE_ABCD_CLICK_MS = "level_range_abcd_click_interval_ms"
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
        private const val KEY_G2_STRONG_TIER_PULSE_MS = "g2_strong_tier_pulse_ms"
        private const val KEY_G2_WEAK_TIER_CLICK_MS = "g2_weak_tier_click_ms"
        private const val KEY_G2_WEAK_TIER_PULSE_MS = "g2_weak_tier_pulse_ms"
        private const val KEY_G2_STRONG_TIER_SOUND = "g2_strong_tier_sound_enabled"
        private const val KEY_G2_WEAK_TIER_SOUND = "g2_weak_tier_sound_enabled"
        private const val KEY_G2_NO_SIGNAL_TIER_CLICK_MS = "g2_no_signal_tier_click_ms"
        private const val KEY_G2_NO_SIGNAL_TIER_SOUND = "g2_no_signal_tier_sound_enabled"
        private const val KEY_G2_NO_SIGNAL_TIER_PULSE_MS = "g2_no_signal_tier_pulse_ms"
        private const val KEY_DEADZONE_TIER_CLICK_MS = "deadzone_tier_click_ms"
        private const val KEY_DEADZONE_TIER_SOUND = "deadzone_tier_sound_enabled"
        private const val KEY_DEADZONE_TIER_PULSE_MS = "deadzone_tier_pulse_ms"
        private const val KEY_NO_SIGNAL_TIER_CLICK_MS = "no_signal_tier_click_ms"
        private const val KEY_NO_SIGNAL_TIER_SOUND = "no_signal_tier_sound_enabled"
        private const val KEY_NO_SIGNAL_TIER_PULSE_MS = "no_signal_tier_pulse_ms"
        private const val KEY_SEARCHING_2G_TIER_CLICK_MS = "searching_2g_tier_click_ms"
        private const val KEY_SEARCHING_2G_TIER_SOUND = "searching_2g_tier_sound_enabled"
        private const val KEY_SEARCHING_2G_TIER_PULSE_MS = "searching_2g_tier_pulse_ms"
        private const val KEY_LIMITED_SERVICE_TIER_CLICK_MS = "limited_service_tier_click_ms"
        private const val KEY_LIMITED_SERVICE_TIER_SOUND = "limited_service_tier_sound_enabled"
        private const val KEY_LIMITED_SERVICE_TIER_PULSE_MS = "limited_service_tier_pulse_ms"
        private const val KEY_LIMITED_ALT_2G_TIER_CLICK_MS = "limited_alt_2g_tier_click_ms"
        private const val KEY_LIMITED_ALT_2G_TIER_SOUND = "limited_alt_2g_tier_sound_enabled"
        private const val KEY_LIMITED_ALT_2G_TIER_PULSE_MS = "limited_alt_2g_tier_pulse_ms"
        private const val KEY_WIFI_CALLING_TIER_CLICK_MS = "wifi_calling_tier_click_ms"
        private const val KEY_WIFI_CALLING_TIER_SOUND = "wifi_calling_tier_sound_enabled"
        private const val KEY_WIFI_CALLING_TIER_PULSE_MS = "wifi_calling_tier_pulse_ms"
    }
}
