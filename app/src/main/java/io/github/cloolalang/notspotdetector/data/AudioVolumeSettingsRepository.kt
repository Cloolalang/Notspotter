package io.github.cloolalang.notspotdetector.data

import android.content.Context
import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.CellReselectBandNamingStyle
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerChoice
import io.github.cloolalang.notspotdetector.model.VoicePhraseOptions
import kotlin.math.roundToInt

class AudioVolumeSettingsRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AudioVolumeSettings {
        val signalPulseFrequencyHz = prefs.getInt(
            KEY_SIGNAL_PULSE_FREQUENCY,
            AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ
        )
        val veryStrongTierPulseFrequencyHz = if (prefs.contains(KEY_VERY_STRONG_TIER_PULSE_FREQUENCY)) {
            prefs.getInt(
                KEY_VERY_STRONG_TIER_PULSE_FREQUENCY,
                AudioVolumeSettings.DEFAULT_VERY_STRONG_TIER_PULSE_FREQUENCY_HZ
            )
        } else {
            (signalPulseFrequencyHz * AudioVolumeSettings.LEGACY_VERY_STRONG_FREQUENCY_MULTIPLIER)
                .roundToInt()
        }
        val g2StrongTierPulseFrequencyHz = prefs.getInt(
            KEY_G2_STRONG_TIER_PULSE_FREQUENCY,
            signalPulseFrequencyHz
        )
        val g2WeakTierPulseFrequencyHz = prefs.getInt(
            KEY_G2_WEAK_TIER_PULSE_FREQUENCY,
            signalPulseFrequencyHz
        )
        val noSignalTierPulseFrequencyHz = if (prefs.contains(KEY_NO_SIGNAL_TIER_PULSE_FREQUENCY)) {
            prefs.getInt(
                KEY_NO_SIGNAL_TIER_PULSE_FREQUENCY,
                AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ
            )
        } else {
            signalPulseFrequencyHz
        }
        val limitedServiceTierPulseFrequencyHz = if (prefs.contains(KEY_LIMITED_SERVICE_TIER_PULSE_FREQUENCY)) {
            prefs.getInt(
                KEY_LIMITED_SERVICE_TIER_PULSE_FREQUENCY,
                AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ
            )
        } else {
            signalPulseFrequencyHz
        }
        val signalPulseDurationMs = prefs.getInt(
            KEY_SIGNAL_PULSE_DURATION,
            AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
        )
        val levelRangeBcdPulseFrequencyHz = if (prefs.contains(KEY_LEVEL_RANGE_BCD_PULSE_FREQUENCY)) {
            prefs.getInt(
                KEY_LEVEL_RANGE_BCD_PULSE_FREQUENCY,
                AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ
            )
        } else {
            signalPulseFrequencyHz
        }
        val levelRangeBcdPulseDurationMs = if (prefs.contains(KEY_LEVEL_RANGE_BCD_PULSE_DURATION)) {
            prefs.getInt(
                KEY_LEVEL_RANGE_BCD_PULSE_DURATION,
                AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
            )
        } else {
            signalPulseDurationMs
        }
        val levelRangeBcdClickVolume = if (prefs.contains(KEY_LEVEL_RANGE_BCD_CLICK_VOLUME)) {
            prefs.getFloat(KEY_LEVEL_RANGE_BCD_CLICK_VOLUME, AudioVolumeSettings.DEFAULT_VOLUME)
        } else {
            prefs.getFloat(KEY_LOW_SIGNAL_CLICK, AudioVolumeSettings.DEFAULT_VOLUME)
        }
        val legacyTechnologyChangeToneVolume = prefs.getFloat(
            KEY_TECHNOLOGY_CHANGE,
            AudioVolumeSettings.DEFAULT_VOLUME
        )
        val legacyTechnologyChangeVoiceEnabled = prefs.getBoolean(
            KEY_TECHNOLOGY_CHANGE_VOICE_ENABLED,
            AudioVolumeSettings.DEFAULT_VOICE_ANNOUNCEMENT_ENABLED
        )
        val legacyTechnologyChangeVoiceVolume = prefs.getFloat(
            KEY_TECHNOLOGY_CHANGE_VOICE,
            AudioVolumeSettings.DEFAULT_VOLUME
        )
        return AudioVolumeSettings(
            masterVoiceAnnouncementsEnabled = prefs.getBoolean(
                KEY_MASTER_VOICE_ANNOUNCEMENTS_ENABLED,
                AudioVolumeSettings.DEFAULT_MASTER_VOICE_ANNOUNCEMENTS_ENABLED
            ),
            pingClickVolume = prefs.getFloat(KEY_PING_CLICK, AudioVolumeSettings.DEFAULT_VOLUME),
            lowSignalClickVolume = prefs.getFloat(KEY_LOW_SIGNAL_CLICK, AudioVolumeSettings.DEFAULT_VOLUME),
            signalPulseFrequencyHz = signalPulseFrequencyHz,
            noSignalTierPulseFrequencyHz = noSignalTierPulseFrequencyHz,
            limitedServiceTierPulseFrequencyHz = limitedServiceTierPulseFrequencyHz,
            limitedServiceTwoToneSpreadPercent = prefs.getInt(
                KEY_LIMITED_SERVICE_TWO_TONE_SPREAD,
                AudioVolumeSettings.DEFAULT_LIMITED_SERVICE_TWO_TONE_SPREAD_PERCENT
            ),
            levelRangeBcdPulseFrequencyHz = levelRangeBcdPulseFrequencyHz,
            veryStrongTierPulseFrequencyHz = veryStrongTierPulseFrequencyHz,
            g2StrongTierPulseFrequencyHz = g2StrongTierPulseFrequencyHz,
            g2WeakTierPulseFrequencyHz = g2WeakTierPulseFrequencyHz,
            signalPulseDurationMs = signalPulseDurationMs,
            levelRangeBcdPulseDurationMs = levelRangeBcdPulseDurationMs,
            levelRangeBcdClickVolume = levelRangeBcdClickVolume,
            cellChangeBellVolume = prefs.getFloat(
                KEY_CELL_CHANGE_BELL,
                AudioVolumeSettings.DEFAULT_VOLUME
            ),
            cellChangeVoiceEnabled = prefs.getBoolean(
                KEY_CELL_CHANGE_VOICE_ENABLED,
                AudioVolumeSettings.DEFAULT_CELL_CHANGE_VOICE_ENABLED
            ),
            cellChangeVoiceVolume = prefs.getFloat(
                KEY_CELL_CHANGE_VOICE,
                AudioVolumeSettings.DEFAULT_VOLUME
            ),
            cellChangeSpeakBandEnabled = prefs.getBoolean(
                KEY_CELL_CHANGE_SPEAK_BAND_ENABLED,
                AudioVolumeSettings.DEFAULT_CELL_CHANGE_SPEAK_BAND_ENABLED
            ),
            cellChangeBandNamingStyle = CellReselectBandNamingStyle.fromId(
                prefs.getString(KEY_CELL_CHANGE_BAND_NAMING_STYLE, null)
            ),
            technologyChangeTo2gToneVolume = prefs.getFloat(
                KEY_TECHNOLOGY_CHANGE_TO_2G_TONE,
                legacyTechnologyChangeToneVolume
            ),
            technologyChangeTo2gVoiceEnabled = prefs.getBoolean(
                KEY_TECHNOLOGY_CHANGE_TO_2G_VOICE_ENABLED,
                legacyTechnologyChangeVoiceEnabled
            ),
            technologyChangeTo2gPeriodicVoiceEnabled = prefs.getBoolean(
                KEY_TECHNOLOGY_CHANGE_TO_2G_PERIODIC_VOICE_ENABLED,
                AudioVolumeSettings.DEFAULT_PERIODIC_VOICE_ENABLED
            ),
            technologyChangeTo2gVoiceVolume = prefs.getFloat(
                KEY_TECHNOLOGY_CHANGE_TO_2G_VOICE,
                legacyTechnologyChangeVoiceVolume
            ),
            technologyChangeTo4gToneVolume = prefs.getFloat(
                KEY_TECHNOLOGY_CHANGE_TO_4G_TONE,
                legacyTechnologyChangeToneVolume
            ),
            technologyChangeTo4gVoiceEnabled = prefs.getBoolean(
                KEY_TECHNOLOGY_CHANGE_TO_4G_VOICE_ENABLED,
                legacyTechnologyChangeVoiceEnabled
            ),
            technologyChangeTo4gVoiceVolume = prefs.getFloat(
                KEY_TECHNOLOGY_CHANGE_TO_4G_VOICE,
                legacyTechnologyChangeVoiceVolume
            ),
            technologyChangeTo5gEndcToneVolume = prefs.getFloat(
                KEY_TECHNOLOGY_CHANGE_TO_5G_ENDC_TONE,
                legacyTechnologyChangeToneVolume
            ),
            technologyChangeTo5gEndcVoiceEnabled = prefs.getBoolean(
                KEY_TECHNOLOGY_CHANGE_TO_5G_ENDC_VOICE_ENABLED,
                legacyTechnologyChangeVoiceEnabled
            ),
            technologyChangeTo5gEndcVoiceVolume = prefs.getFloat(
                KEY_TECHNOLOGY_CHANGE_TO_5G_ENDC_VOICE,
                legacyTechnologyChangeVoiceVolume
            ),
            tier5AnnouncerEnabled = prefs.getBoolean(
                KEY_TIER5_ANNOUNCER_ENABLED,
                AudioVolumeSettings.DEFAULT_VOICE_ANNOUNCEMENT_ENABLED
            ),
            tier5PeriodicVoiceEnabled = prefs.getBoolean(
                KEY_TIER5_PERIODIC_VOICE_ENABLED,
                AudioVolumeSettings.DEFAULT_PERIODIC_VOICE_ENABLED
            ),
            tier5AnnouncerVolume = prefs.getFloat(
                KEY_TIER5_ANNOUNCER_VOLUME,
                AudioVolumeSettings.DEFAULT_VOLUME
            ),
            voiceAnnouncerChoice = VoiceAnnouncerChoice.fromId(
                prefs.getString(KEY_VOICE_ANNOUNCER_CHOICE, null)
            ),
            voiceAnnouncerEngineId = prefs.getString(KEY_VOICE_ANNOUNCER_ENGINE_ID, null),
            voiceSpeechRate = prefs.getFloat(
                KEY_VOICE_SPEECH_RATE,
                AudioVolumeSettings.DEFAULT_VOICE_SPEECH_RATE
            ),
            noSignalToneVolume = prefs.getFloat(KEY_NO_SIGNAL_TONE, AudioVolumeSettings.DEFAULT_VOLUME),
            noSignalVibrationEnabled = prefs.getBoolean(
                KEY_NO_SIGNAL_VIBRATION_ENABLED,
                AudioVolumeSettings.DEFAULT_NO_SIGNAL_VIBRATION_ENABLED
            ),
            noSignalVoiceEnabled = prefs.getBoolean(
                KEY_NO_SIGNAL_VOICE_ENABLED,
                AudioVolumeSettings.DEFAULT_VOICE_ANNOUNCEMENT_ENABLED
            ),
            noSignalPeriodicVoiceEnabled = prefs.getBoolean(
                KEY_NO_SIGNAL_PERIODIC_VOICE_ENABLED,
                AudioVolumeSettings.DEFAULT_PERIODIC_VOICE_ENABLED
            ),
            noSignalVoiceVolume = prefs.getFloat(
                KEY_NO_SIGNAL_VOICE,
                AudioVolumeSettings.DEFAULT_VOLUME
            ),
            limitedServiceToneVolume = prefs.getFloat(
                KEY_LIMITED_SERVICE_TONE,
                AudioVolumeSettings.DEFAULT_VOLUME
            ),
            limitedServiceVoiceEnabled = prefs.getBoolean(
                KEY_LIMITED_SERVICE_VOICE_ENABLED,
                AudioVolumeSettings.DEFAULT_VOICE_ANNOUNCEMENT_ENABLED
            ),
            limitedServicePeriodicVoiceEnabled = prefs.getBoolean(
                KEY_LIMITED_SERVICE_PERIODIC_VOICE_ENABLED,
                AudioVolumeSettings.DEFAULT_PERIODIC_VOICE_ENABLED
            ),
            limitedServiceVoiceVolume = prefs.getFloat(
                KEY_LIMITED_SERVICE_VOICE,
                AudioVolumeSettings.DEFAULT_VOLUME
            ),
            speakOperatorNameEnabled = prefs.getBoolean(
                KEY_SPEAK_OPERATOR_NAME_ENABLED,
                AudioVolumeSettings.DEFAULT_SPEAK_OPERATOR_NAME_ENABLED
            ),
            speakTechnologyEnabled = prefs.getBoolean(
                KEY_SPEAK_TECHNOLOGY_ENABLED,
                AudioVolumeSettings.DEFAULT_SPEAK_TECHNOLOGY_ENABLED
            ),
            cellChangePhrases = loadPhrases(
                KEY_CELL_CHANGE_SPEAK_OPERATOR,
                KEY_CELL_CHANGE_SPEAK_TECHNOLOGY,
                KEY_CELL_CHANGE_PHRASE_SPEAK_BAND
            ),
            technologyChangeTo2gPhrases = loadPhrases(
                KEY_TECH_CHANGE_2G_SPEAK_OPERATOR,
                KEY_TECH_CHANGE_2G_SPEAK_TECHNOLOGY,
                KEY_TECH_CHANGE_2G_SPEAK_BAND
            ),
            technologyChangeTo4gPhrases = loadPhrases(
                KEY_TECH_CHANGE_4G_SPEAK_OPERATOR,
                KEY_TECH_CHANGE_4G_SPEAK_TECHNOLOGY,
                KEY_TECH_CHANGE_4G_SPEAK_BAND
            ),
            technologyChangeTo5gEndcPhrases = loadPhrases(
                KEY_TECH_CHANGE_5G_SPEAK_OPERATOR,
                KEY_TECH_CHANGE_5G_SPEAK_TECHNOLOGY,
                KEY_TECH_CHANGE_5G_SPEAK_BAND
            ),
            signalLowPhrases = loadPhrases(
                KEY_SIGNAL_LOW_SPEAK_OPERATOR,
                KEY_SIGNAL_LOW_SPEAK_TECHNOLOGY,
                KEY_SIGNAL_LOW_SPEAK_BAND
            ),
            noSignalPhrases = loadPhrases(
                KEY_NO_SIGNAL_SPEAK_OPERATOR,
                KEY_NO_SIGNAL_SPEAK_TECHNOLOGY,
                KEY_NO_SIGNAL_SPEAK_BAND
            ),
            limitedServicePhrases = loadPhrases(
                KEY_LIMITED_SERVICE_SPEAK_OPERATOR,
                KEY_LIMITED_SERVICE_SPEAK_TECHNOLOGY,
                KEY_LIMITED_SERVICE_SPEAK_BAND
            ).copy(
                speakHomeLimitedService = prefs.getBoolean(
                    KEY_LIMITED_SERVICE_SPEAK_HOME_LIMITED,
                    VoicePhraseOptions.DEFAULT_SPEAK_HOME_LIMITED_SERVICE
                ),
                speakVisitingLimitedService = prefs.getBoolean(
                    KEY_LIMITED_SERVICE_SPEAK_VISITING_LIMITED,
                    VoicePhraseOptions.DEFAULT_SPEAK_VISITING_LIMITED_SERVICE
                )
            )
        ).normalized()
    }

    private fun loadPhrases(
        operatorKey: String,
        technologyKey: String,
        bandKey: String
    ): VoicePhraseOptions {
        val fallbackOperator = prefs.getBoolean(
            KEY_SPEAK_OPERATOR_NAME_ENABLED,
            AudioVolumeSettings.DEFAULT_SPEAK_OPERATOR_NAME_ENABLED
        )
        val fallbackTechnology = prefs.getBoolean(
            KEY_SPEAK_TECHNOLOGY_ENABLED,
            AudioVolumeSettings.DEFAULT_SPEAK_TECHNOLOGY_ENABLED
        )
        return VoicePhraseOptions(
            speakOperatorName = if (prefs.contains(operatorKey)) {
                prefs.getBoolean(operatorKey, VoicePhraseOptions.DEFAULT_SPEAK_OPERATOR_NAME)
            } else {
                fallbackOperator
            },
            speakTechnology = if (prefs.contains(technologyKey)) {
                prefs.getBoolean(technologyKey, VoicePhraseOptions.DEFAULT_SPEAK_TECHNOLOGY)
            } else {
                fallbackTechnology
            },
            speakBand = prefs.getBoolean(bandKey, VoicePhraseOptions.DEFAULT_SPEAK_BAND)
        )
    }

    fun save(settings: AudioVolumeSettings) {
        val normalized = settings.normalized()
        prefs.edit()
            .putBoolean(KEY_MASTER_VOICE_ANNOUNCEMENTS_ENABLED, normalized.masterVoiceAnnouncementsEnabled)
            .putFloat(KEY_PING_CLICK, normalized.pingClickVolume)
            .putFloat(KEY_LOW_SIGNAL_CLICK, normalized.lowSignalClickVolume)
            .putInt(KEY_SIGNAL_PULSE_FREQUENCY, normalized.signalPulseFrequencyHz)
            .putInt(KEY_NO_SIGNAL_TIER_PULSE_FREQUENCY, normalized.noSignalTierPulseFrequencyHz)
            .putInt(KEY_LIMITED_SERVICE_TIER_PULSE_FREQUENCY, normalized.limitedServiceTierPulseFrequencyHz)
            .putInt(KEY_LIMITED_SERVICE_TWO_TONE_SPREAD, normalized.limitedServiceTwoToneSpreadPercent)
            .putInt(KEY_LEVEL_RANGE_BCD_PULSE_FREQUENCY, normalized.levelRangeBcdPulseFrequencyHz)
            .putInt(KEY_VERY_STRONG_TIER_PULSE_FREQUENCY, normalized.veryStrongTierPulseFrequencyHz)
            .putInt(KEY_G2_STRONG_TIER_PULSE_FREQUENCY, normalized.g2StrongTierPulseFrequencyHz)
            .putInt(KEY_G2_WEAK_TIER_PULSE_FREQUENCY, normalized.g2WeakTierPulseFrequencyHz)
            .putInt(KEY_SIGNAL_PULSE_DURATION, normalized.signalPulseDurationMs)
            .putInt(KEY_LEVEL_RANGE_BCD_PULSE_DURATION, normalized.levelRangeBcdPulseDurationMs)
            .putFloat(KEY_LEVEL_RANGE_BCD_CLICK_VOLUME, normalized.levelRangeBcdClickVolume)
            .putFloat(KEY_CELL_CHANGE_BELL, normalized.cellChangeBellVolume)
            .putBoolean(KEY_CELL_CHANGE_VOICE_ENABLED, normalized.cellChangeVoiceEnabled)
            .putFloat(KEY_CELL_CHANGE_VOICE, normalized.cellChangeVoiceVolume)
            .putBoolean(KEY_CELL_CHANGE_SPEAK_BAND_ENABLED, normalized.cellChangeSpeakBandEnabled)
            .putString(KEY_CELL_CHANGE_BAND_NAMING_STYLE, normalized.cellChangeBandNamingStyle.id)
            .putFloat(KEY_TECHNOLOGY_CHANGE_TO_2G_TONE, normalized.technologyChangeTo2gToneVolume)
            .putBoolean(KEY_TECHNOLOGY_CHANGE_TO_2G_VOICE_ENABLED, normalized.technologyChangeTo2gVoiceEnabled)
            .putBoolean(KEY_TECHNOLOGY_CHANGE_TO_2G_PERIODIC_VOICE_ENABLED, normalized.technologyChangeTo2gPeriodicVoiceEnabled)
            .putFloat(KEY_TECHNOLOGY_CHANGE_TO_2G_VOICE, normalized.technologyChangeTo2gVoiceVolume)
            .putFloat(KEY_TECHNOLOGY_CHANGE_TO_4G_TONE, normalized.technologyChangeTo4gToneVolume)
            .putBoolean(KEY_TECHNOLOGY_CHANGE_TO_4G_VOICE_ENABLED, normalized.technologyChangeTo4gVoiceEnabled)
            .putFloat(KEY_TECHNOLOGY_CHANGE_TO_4G_VOICE, normalized.technologyChangeTo4gVoiceVolume)
            .putFloat(KEY_TECHNOLOGY_CHANGE_TO_5G_ENDC_TONE, normalized.technologyChangeTo5gEndcToneVolume)
            .putBoolean(KEY_TECHNOLOGY_CHANGE_TO_5G_ENDC_VOICE_ENABLED, normalized.technologyChangeTo5gEndcVoiceEnabled)
            .putFloat(KEY_TECHNOLOGY_CHANGE_TO_5G_ENDC_VOICE, normalized.technologyChangeTo5gEndcVoiceVolume)
            .putBoolean(KEY_TIER5_ANNOUNCER_ENABLED, normalized.tier5AnnouncerEnabled)
            .putBoolean(KEY_TIER5_PERIODIC_VOICE_ENABLED, normalized.tier5PeriodicVoiceEnabled)
            .putFloat(KEY_TIER5_ANNOUNCER_VOLUME, normalized.tier5AnnouncerVolume)
            .putString(KEY_VOICE_ANNOUNCER_CHOICE, normalized.voiceAnnouncerChoice.id)
            .putString(KEY_VOICE_ANNOUNCER_ENGINE_ID, normalized.voiceAnnouncerEngineId)
            .putFloat(KEY_VOICE_SPEECH_RATE, normalized.voiceSpeechRate)
            .putFloat(KEY_NO_SIGNAL_TONE, normalized.noSignalToneVolume)
            .putBoolean(KEY_NO_SIGNAL_VIBRATION_ENABLED, normalized.noSignalVibrationEnabled)
            .putBoolean(KEY_NO_SIGNAL_VOICE_ENABLED, normalized.noSignalVoiceEnabled)
            .putBoolean(KEY_NO_SIGNAL_PERIODIC_VOICE_ENABLED, normalized.noSignalPeriodicVoiceEnabled)
            .putFloat(KEY_NO_SIGNAL_VOICE, normalized.noSignalVoiceVolume)
            .putFloat(KEY_LIMITED_SERVICE_TONE, normalized.limitedServiceToneVolume)
            .putBoolean(KEY_LIMITED_SERVICE_VOICE_ENABLED, normalized.limitedServiceVoiceEnabled)
            .putBoolean(KEY_LIMITED_SERVICE_PERIODIC_VOICE_ENABLED, normalized.limitedServicePeriodicVoiceEnabled)
            .putFloat(KEY_LIMITED_SERVICE_VOICE, normalized.limitedServiceVoiceVolume)
            .putBoolean(KEY_SPEAK_OPERATOR_NAME_ENABLED, normalized.speakOperatorNameEnabled)
            .putBoolean(KEY_SPEAK_TECHNOLOGY_ENABLED, normalized.speakTechnologyEnabled)
            .putBoolean(KEY_CELL_CHANGE_SPEAK_OPERATOR, normalized.cellChangePhrases.speakOperatorName)
            .putBoolean(KEY_CELL_CHANGE_SPEAK_TECHNOLOGY, normalized.cellChangePhrases.speakTechnology)
            .putBoolean(KEY_CELL_CHANGE_PHRASE_SPEAK_BAND, normalized.cellChangePhrases.speakBand)
            .putBoolean(KEY_TECH_CHANGE_2G_SPEAK_OPERATOR, normalized.technologyChangeTo2gPhrases.speakOperatorName)
            .putBoolean(KEY_TECH_CHANGE_2G_SPEAK_TECHNOLOGY, normalized.technologyChangeTo2gPhrases.speakTechnology)
            .putBoolean(KEY_TECH_CHANGE_2G_SPEAK_BAND, normalized.technologyChangeTo2gPhrases.speakBand)
            .putBoolean(KEY_TECH_CHANGE_4G_SPEAK_OPERATOR, normalized.technologyChangeTo4gPhrases.speakOperatorName)
            .putBoolean(KEY_TECH_CHANGE_4G_SPEAK_TECHNOLOGY, normalized.technologyChangeTo4gPhrases.speakTechnology)
            .putBoolean(KEY_TECH_CHANGE_4G_SPEAK_BAND, normalized.technologyChangeTo4gPhrases.speakBand)
            .putBoolean(KEY_TECH_CHANGE_5G_SPEAK_OPERATOR, normalized.technologyChangeTo5gEndcPhrases.speakOperatorName)
            .putBoolean(KEY_TECH_CHANGE_5G_SPEAK_TECHNOLOGY, normalized.technologyChangeTo5gEndcPhrases.speakTechnology)
            .putBoolean(KEY_TECH_CHANGE_5G_SPEAK_BAND, normalized.technologyChangeTo5gEndcPhrases.speakBand)
            .putBoolean(KEY_SIGNAL_LOW_SPEAK_OPERATOR, normalized.signalLowPhrases.speakOperatorName)
            .putBoolean(KEY_SIGNAL_LOW_SPEAK_TECHNOLOGY, normalized.signalLowPhrases.speakTechnology)
            .putBoolean(KEY_SIGNAL_LOW_SPEAK_BAND, normalized.signalLowPhrases.speakBand)
            .putBoolean(KEY_NO_SIGNAL_SPEAK_OPERATOR, normalized.noSignalPhrases.speakOperatorName)
            .putBoolean(KEY_NO_SIGNAL_SPEAK_TECHNOLOGY, normalized.noSignalPhrases.speakTechnology)
            .putBoolean(KEY_NO_SIGNAL_SPEAK_BAND, normalized.noSignalPhrases.speakBand)
            .putBoolean(KEY_LIMITED_SERVICE_SPEAK_OPERATOR, normalized.limitedServicePhrases.speakOperatorName)
            .putBoolean(KEY_LIMITED_SERVICE_SPEAK_TECHNOLOGY, normalized.limitedServicePhrases.speakTechnology)
            .putBoolean(KEY_LIMITED_SERVICE_SPEAK_BAND, normalized.limitedServicePhrases.speakBand)
            .putBoolean(
                KEY_LIMITED_SERVICE_SPEAK_HOME_LIMITED,
                normalized.limitedServicePhrases.speakHomeLimitedService
            )
            .putBoolean(
                KEY_LIMITED_SERVICE_SPEAK_VISITING_LIMITED,
                normalized.limitedServicePhrases.speakVisitingLimitedService
            )
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "notspot_audio_volumes"
        private const val KEY_MASTER_VOICE_ANNOUNCEMENTS_ENABLED = "master_voice_announcements_enabled"
        private const val KEY_PING_CLICK = "ping_click_volume"
        private const val KEY_LOW_SIGNAL_CLICK = "low_signal_click_volume"
        private const val KEY_SIGNAL_PULSE_FREQUENCY = "signal_pulse_frequency_hz"
        private const val KEY_NO_SIGNAL_TIER_PULSE_FREQUENCY = "no_signal_tier_pulse_frequency_hz"
        private const val KEY_LIMITED_SERVICE_TIER_PULSE_FREQUENCY = "limited_service_tier_pulse_frequency_hz"
        private const val KEY_LIMITED_SERVICE_TWO_TONE_SPREAD = "limited_service_two_tone_spread_percent"
        private const val KEY_LEVEL_RANGE_BCD_PULSE_FREQUENCY = "level_range_bcd_pulse_frequency_hz"
        private const val KEY_VERY_STRONG_TIER_PULSE_FREQUENCY = "very_strong_tier_pulse_frequency_hz"
        private const val KEY_G2_STRONG_TIER_PULSE_FREQUENCY = "g2_strong_tier_pulse_frequency_hz"
        private const val KEY_G2_WEAK_TIER_PULSE_FREQUENCY = "g2_weak_tier_pulse_frequency_hz"
        private const val KEY_SIGNAL_PULSE_DURATION = "signal_pulse_duration_ms"
        private const val KEY_LEVEL_RANGE_BCD_PULSE_DURATION = "level_range_bcd_pulse_duration_ms"
        private const val KEY_LEVEL_RANGE_BCD_CLICK_VOLUME = "level_range_bcd_click_volume"
        private const val KEY_CELL_CHANGE_BELL = "cell_change_bell_volume"
        private const val KEY_CELL_CHANGE_VOICE_ENABLED = "cell_change_voice_enabled"
        private const val KEY_CELL_CHANGE_VOICE = "cell_change_voice_volume"
        private const val KEY_CELL_CHANGE_SPEAK_BAND_ENABLED = "cell_change_speak_band_enabled"
        private const val KEY_CELL_CHANGE_BAND_NAMING_STYLE = "cell_change_band_naming_style"
        private const val KEY_TECHNOLOGY_CHANGE = "technology_change_volume"
        private const val KEY_TECHNOLOGY_CHANGE_VOICE_ENABLED = "technology_change_voice_enabled"
        private const val KEY_TECHNOLOGY_CHANGE_VOICE = "technology_change_voice_volume"
        private const val KEY_TECHNOLOGY_CHANGE_TO_2G_TONE = "technology_change_to_2g_tone_volume"
        private const val KEY_TECHNOLOGY_CHANGE_TO_2G_VOICE_ENABLED = "technology_change_to_2g_voice_enabled"
        private const val KEY_TECHNOLOGY_CHANGE_TO_2G_PERIODIC_VOICE_ENABLED = "technology_change_to_2g_periodic_voice_enabled"
        private const val KEY_TECHNOLOGY_CHANGE_TO_2G_VOICE = "technology_change_to_2g_voice_volume"
        private const val KEY_TECHNOLOGY_CHANGE_TO_4G_TONE = "technology_change_to_4g_tone_volume"
        private const val KEY_TECHNOLOGY_CHANGE_TO_4G_VOICE_ENABLED = "technology_change_to_4g_voice_enabled"
        private const val KEY_TECHNOLOGY_CHANGE_TO_4G_VOICE = "technology_change_to_4g_voice_volume"
        private const val KEY_TECHNOLOGY_CHANGE_TO_5G_ENDC_TONE = "technology_change_to_5g_endc_tone_volume"
        private const val KEY_TECHNOLOGY_CHANGE_TO_5G_ENDC_VOICE_ENABLED = "technology_change_to_5g_endc_voice_enabled"
        private const val KEY_TECHNOLOGY_CHANGE_TO_5G_ENDC_VOICE = "technology_change_to_5g_endc_voice_volume"
        private const val KEY_TIER5_ANNOUNCER_ENABLED = "tier5_announcer_enabled"
        private const val KEY_TIER5_PERIODIC_VOICE_ENABLED = "tier5_periodic_voice_enabled"
        private const val KEY_TIER5_ANNOUNCER_VOLUME = "tier5_announcer_volume"
        private const val KEY_VOICE_ANNOUNCER_CHOICE = "voice_announcer_choice"
        private const val KEY_VOICE_ANNOUNCER_ENGINE_ID = "voice_announcer_engine_id"
        private const val KEY_VOICE_SPEECH_RATE = "voice_speech_rate"
        private const val KEY_NO_SIGNAL_TONE = "no_signal_tone_volume"
        private const val KEY_NO_SIGNAL_VIBRATION_ENABLED = "no_signal_vibration_enabled"
        private const val KEY_NO_SIGNAL_VOICE_ENABLED = "no_signal_voice_enabled"
        private const val KEY_NO_SIGNAL_PERIODIC_VOICE_ENABLED = "no_signal_periodic_voice_enabled"
        private const val KEY_NO_SIGNAL_VOICE = "no_signal_voice_volume"
        private const val KEY_LIMITED_SERVICE_TONE = "limited_service_tone_volume"
        private const val KEY_LIMITED_SERVICE_VOICE_ENABLED = "limited_service_voice_enabled"
        private const val KEY_LIMITED_SERVICE_PERIODIC_VOICE_ENABLED = "limited_service_periodic_voice_enabled"
        private const val KEY_LIMITED_SERVICE_VOICE = "limited_service_voice_volume"
        private const val KEY_SPEAK_OPERATOR_NAME_ENABLED = "speak_operator_name_enabled"
        private const val KEY_SPEAK_TECHNOLOGY_ENABLED = "speak_technology_enabled"
        private const val KEY_CELL_CHANGE_SPEAK_OPERATOR = "cell_change_phrase_speak_operator"
        private const val KEY_CELL_CHANGE_SPEAK_TECHNOLOGY = "cell_change_phrase_speak_technology"
        private const val KEY_CELL_CHANGE_PHRASE_SPEAK_BAND = "cell_change_phrase_speak_band"
        private const val KEY_TECH_CHANGE_2G_SPEAK_OPERATOR = "tech_change_2g_phrase_speak_operator"
        private const val KEY_TECH_CHANGE_2G_SPEAK_TECHNOLOGY = "tech_change_2g_phrase_speak_technology"
        private const val KEY_TECH_CHANGE_2G_SPEAK_BAND = "tech_change_2g_phrase_speak_band"
        private const val KEY_TECH_CHANGE_4G_SPEAK_OPERATOR = "tech_change_4g_phrase_speak_operator"
        private const val KEY_TECH_CHANGE_4G_SPEAK_TECHNOLOGY = "tech_change_4g_phrase_speak_technology"
        private const val KEY_TECH_CHANGE_4G_SPEAK_BAND = "tech_change_4g_phrase_speak_band"
        private const val KEY_TECH_CHANGE_5G_SPEAK_OPERATOR = "tech_change_5g_phrase_speak_operator"
        private const val KEY_TECH_CHANGE_5G_SPEAK_TECHNOLOGY = "tech_change_5g_phrase_speak_technology"
        private const val KEY_TECH_CHANGE_5G_SPEAK_BAND = "tech_change_5g_phrase_speak_band"
        private const val KEY_SIGNAL_LOW_SPEAK_OPERATOR = "signal_low_phrase_speak_operator"
        private const val KEY_SIGNAL_LOW_SPEAK_TECHNOLOGY = "signal_low_phrase_speak_technology"
        private const val KEY_SIGNAL_LOW_SPEAK_BAND = "signal_low_phrase_speak_band"
        private const val KEY_NO_SIGNAL_SPEAK_OPERATOR = "no_signal_phrase_speak_operator"
        private const val KEY_NO_SIGNAL_SPEAK_TECHNOLOGY = "no_signal_phrase_speak_technology"
        private const val KEY_NO_SIGNAL_SPEAK_BAND = "no_signal_phrase_speak_band"
        private const val KEY_LIMITED_SERVICE_SPEAK_OPERATOR = "limited_service_phrase_speak_operator"
        private const val KEY_LIMITED_SERVICE_SPEAK_TECHNOLOGY = "limited_service_phrase_speak_technology"
        private const val KEY_LIMITED_SERVICE_SPEAK_BAND = "limited_service_phrase_speak_band"
        private const val KEY_LIMITED_SERVICE_SPEAK_HOME_LIMITED =
            "limited_service_phrase_speak_home_limited"
        private const val KEY_LIMITED_SERVICE_SPEAK_VISITING_LIMITED =
            "limited_service_phrase_speak_visiting_limited"
    }
}
