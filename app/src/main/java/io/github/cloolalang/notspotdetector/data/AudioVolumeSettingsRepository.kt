package io.github.cloolalang.notspotdetector.data

import android.content.Context
import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings

class AudioVolumeSettingsRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AudioVolumeSettings {
        return AudioVolumeSettings(
            pingClickVolume = prefs.getFloat(KEY_PING_CLICK, AudioVolumeSettings.DEFAULT_VOLUME),
            lowSignalClickVolume = prefs.getFloat(KEY_LOW_SIGNAL_CLICK, AudioVolumeSettings.DEFAULT_VOLUME),
            signalPulseFrequencyHz = prefs.getInt(
                KEY_SIGNAL_PULSE_FREQUENCY,
                AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ
            ),
            signalPulseDurationMs = prefs.getInt(
                KEY_SIGNAL_PULSE_DURATION,
                AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
            ),
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
            technologyChangeVolume = prefs.getFloat(
                KEY_TECHNOLOGY_CHANGE,
                AudioVolumeSettings.DEFAULT_VOLUME
            ),
            technologyChangeVoiceEnabled = prefs.getBoolean(
                KEY_TECHNOLOGY_CHANGE_VOICE_ENABLED,
                AudioVolumeSettings.DEFAULT_VOICE_ANNOUNCEMENT_ENABLED
            ),
            technologyChangeVoiceVolume = prefs.getFloat(
                KEY_TECHNOLOGY_CHANGE_VOICE,
                AudioVolumeSettings.DEFAULT_VOLUME
            ),
            noSignalToneVolume = prefs.getFloat(KEY_NO_SIGNAL_TONE, AudioVolumeSettings.DEFAULT_VOLUME),
            noSignalVoiceEnabled = prefs.getBoolean(
                KEY_NO_SIGNAL_VOICE_ENABLED,
                AudioVolumeSettings.DEFAULT_VOICE_ANNOUNCEMENT_ENABLED
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
            limitedServiceVoiceVolume = prefs.getFloat(
                KEY_LIMITED_SERVICE_VOICE,
                AudioVolumeSettings.DEFAULT_VOLUME
            )
        ).normalized()
    }

    fun save(settings: AudioVolumeSettings) {
        val normalized = settings.normalized()
        prefs.edit()
            .putFloat(KEY_PING_CLICK, normalized.pingClickVolume)
            .putFloat(KEY_LOW_SIGNAL_CLICK, normalized.lowSignalClickVolume)
            .putInt(KEY_SIGNAL_PULSE_FREQUENCY, normalized.signalPulseFrequencyHz)
            .putInt(KEY_SIGNAL_PULSE_DURATION, normalized.signalPulseDurationMs)
            .putFloat(KEY_CELL_CHANGE_BELL, normalized.cellChangeBellVolume)
            .putBoolean(KEY_CELL_CHANGE_VOICE_ENABLED, normalized.cellChangeVoiceEnabled)
            .putFloat(KEY_CELL_CHANGE_VOICE, normalized.cellChangeVoiceVolume)
            .putFloat(KEY_TECHNOLOGY_CHANGE, normalized.technologyChangeVolume)
            .putBoolean(KEY_TECHNOLOGY_CHANGE_VOICE_ENABLED, normalized.technologyChangeVoiceEnabled)
            .putFloat(KEY_TECHNOLOGY_CHANGE_VOICE, normalized.technologyChangeVoiceVolume)
            .putFloat(KEY_NO_SIGNAL_TONE, normalized.noSignalToneVolume)
            .putBoolean(KEY_NO_SIGNAL_VOICE_ENABLED, normalized.noSignalVoiceEnabled)
            .putFloat(KEY_NO_SIGNAL_VOICE, normalized.noSignalVoiceVolume)
            .putFloat(KEY_LIMITED_SERVICE_TONE, normalized.limitedServiceToneVolume)
            .putBoolean(KEY_LIMITED_SERVICE_VOICE_ENABLED, normalized.limitedServiceVoiceEnabled)
            .putFloat(KEY_LIMITED_SERVICE_VOICE, normalized.limitedServiceVoiceVolume)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "notspot_audio_volumes"
        private const val KEY_PING_CLICK = "ping_click_volume"
        private const val KEY_LOW_SIGNAL_CLICK = "low_signal_click_volume"
        private const val KEY_SIGNAL_PULSE_FREQUENCY = "signal_pulse_frequency_hz"
        private const val KEY_SIGNAL_PULSE_DURATION = "signal_pulse_duration_ms"
        private const val KEY_CELL_CHANGE_BELL = "cell_change_bell_volume"
        private const val KEY_CELL_CHANGE_VOICE_ENABLED = "cell_change_voice_enabled"
        private const val KEY_CELL_CHANGE_VOICE = "cell_change_voice_volume"
        private const val KEY_TECHNOLOGY_CHANGE = "technology_change_volume"
        private const val KEY_TECHNOLOGY_CHANGE_VOICE_ENABLED = "technology_change_voice_enabled"
        private const val KEY_TECHNOLOGY_CHANGE_VOICE = "technology_change_voice_volume"
        private const val KEY_NO_SIGNAL_TONE = "no_signal_tone_volume"
        private const val KEY_NO_SIGNAL_VOICE_ENABLED = "no_signal_voice_enabled"
        private const val KEY_NO_SIGNAL_VOICE = "no_signal_voice_volume"
        private const val KEY_LIMITED_SERVICE_TONE = "limited_service_tone_volume"
        private const val KEY_LIMITED_SERVICE_VOICE_ENABLED = "limited_service_voice_enabled"
        private const val KEY_LIMITED_SERVICE_VOICE = "limited_service_voice_volume"
    }
}
