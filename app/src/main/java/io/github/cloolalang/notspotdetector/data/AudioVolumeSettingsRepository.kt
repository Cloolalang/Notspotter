package io.github.cloolalang.notspotdetector.data

import android.content.Context
import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings

class AudioVolumeSettingsRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AudioVolumeSettings {
        return AudioVolumeSettings(
            pingClickVolume = prefs.getFloat(KEY_PING_CLICK, AudioVolumeSettings.DEFAULT_VOLUME),
            lowSignalClickVolume = prefs.getFloat(KEY_LOW_SIGNAL_CLICK, AudioVolumeSettings.DEFAULT_VOLUME),
            signalPulseDurationMs = prefs.getInt(
                KEY_SIGNAL_PULSE_DURATION,
                AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
            ),
            cellChangeBellVolume = prefs.getFloat(
                KEY_CELL_CHANGE_BELL,
                AudioVolumeSettings.DEFAULT_VOLUME
            ),
            technologyChangeVolume = prefs.getFloat(
                KEY_TECHNOLOGY_CHANGE,
                AudioVolumeSettings.DEFAULT_VOLUME
            ),
            noSignalToneVolume = prefs.getFloat(KEY_NO_SIGNAL_TONE, AudioVolumeSettings.DEFAULT_VOLUME),
            limitedServiceToneVolume = prefs.getFloat(
                KEY_LIMITED_SERVICE_TONE,
                AudioVolumeSettings.DEFAULT_VOLUME
            )
        ).normalized()
    }

    fun save(settings: AudioVolumeSettings) {
        val normalized = settings.normalized()
        prefs.edit()
            .putFloat(KEY_PING_CLICK, normalized.pingClickVolume)
            .putFloat(KEY_LOW_SIGNAL_CLICK, normalized.lowSignalClickVolume)
            .putInt(KEY_SIGNAL_PULSE_DURATION, normalized.signalPulseDurationMs)
            .putFloat(KEY_CELL_CHANGE_BELL, normalized.cellChangeBellVolume)
            .putFloat(KEY_TECHNOLOGY_CHANGE, normalized.technologyChangeVolume)
            .putFloat(KEY_NO_SIGNAL_TONE, normalized.noSignalToneVolume)
            .putFloat(KEY_LIMITED_SERVICE_TONE, normalized.limitedServiceToneVolume)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "notspot_audio_volumes"
        private const val KEY_PING_CLICK = "ping_click_volume"
        private const val KEY_LOW_SIGNAL_CLICK = "low_signal_click_volume"
        private const val KEY_SIGNAL_PULSE_DURATION = "signal_pulse_duration_ms"
        private const val KEY_CELL_CHANGE_BELL = "cell_change_bell_volume"
        private const val KEY_TECHNOLOGY_CHANGE = "technology_change_volume"
        private const val KEY_NO_SIGNAL_TONE = "no_signal_tone_volume"
        private const val KEY_LIMITED_SERVICE_TONE = "limited_service_tone_volume"
    }
}
