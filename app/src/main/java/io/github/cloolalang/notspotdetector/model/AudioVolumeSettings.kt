package io.github.cloolalang.notspotdetector.model

import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class AudioVolumeSettings(
    val pingClickVolume: Float = DEFAULT_VOLUME,
    val lowSignalClickVolume: Float = DEFAULT_VOLUME,
    val signalPulseFrequencyHz: Int = DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ,
    val signalPulseDurationMs: Int = DEFAULT_SIGNAL_PULSE_DURATION_MS,
    val cellChangeBellVolume: Float = DEFAULT_VOLUME,
    val cellChangeVoiceEnabled: Boolean = DEFAULT_CELL_CHANGE_VOICE_ENABLED,
    val cellChangeVoiceVolume: Float = DEFAULT_VOLUME,
    val technologyChangeVolume: Float = DEFAULT_VOLUME,
    val technologyChangeVoiceEnabled: Boolean = DEFAULT_VOICE_ANNOUNCEMENT_ENABLED,
    val technologyChangeVoiceVolume: Float = DEFAULT_VOLUME,
    val noSignalToneVolume: Float = DEFAULT_VOLUME,
    val noSignalVoiceEnabled: Boolean = DEFAULT_VOICE_ANNOUNCEMENT_ENABLED,
    val noSignalVoiceVolume: Float = DEFAULT_VOLUME,
    val limitedServiceToneVolume: Float = DEFAULT_VOLUME,
    val limitedServiceVoiceEnabled: Boolean = DEFAULT_VOICE_ANNOUNCEMENT_ENABLED,
    val limitedServiceVoiceVolume: Float = DEFAULT_VOLUME
) {
    fun normalized(): AudioVolumeSettings {
        return copy(
            pingClickVolume = pingClickVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            lowSignalClickVolume = lowSignalClickVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            signalPulseFrequencyHz = signalPulseFrequencyHz.coerceIn(
                MIN_SIGNAL_PULSE_FREQUENCY_HZ,
                MAX_SIGNAL_PULSE_FREQUENCY_HZ
            ),
            signalPulseDurationMs = signalPulseDurationMs.coerceIn(
                MIN_SIGNAL_PULSE_DURATION_MS,
                MAX_SIGNAL_PULSE_DURATION_MS
            ),
            cellChangeBellVolume = cellChangeBellVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            cellChangeVoiceVolume = cellChangeVoiceVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            technologyChangeVolume = technologyChangeVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            technologyChangeVoiceVolume = technologyChangeVoiceVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            noSignalToneVolume = noSignalToneVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            noSignalVoiceVolume = noSignalVoiceVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            limitedServiceToneVolume = limitedServiceToneVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            limitedServiceVoiceVolume = limitedServiceVoiceVolume.coerceIn(MIN_VOLUME, MAX_VOLUME)
        )
    }

    /** Very strong tier tone: 25% above [signalPulseFrequencyHz]. */
    fun veryStrongPulseFrequencyHz(): Int {
        return (signalPulseFrequencyHz * VERY_STRONG_FREQUENCY_MULTIPLIER).roundToInt()
    }

    companion object {
        const val DEFAULT_VOLUME = 1f
        const val MIN_VOLUME = 0f
        const val MAX_VOLUME = 1f
        const val DEFAULT_CELL_CHANGE_VOICE_ENABLED = false
        const val DEFAULT_VOICE_ANNOUNCEMENT_ENABLED = false

        const val DEFAULT_SIGNAL_PULSE_DURATION_MS = 250
        const val MIN_SIGNAL_PULSE_DURATION_MS = 10
        const val MAX_SIGNAL_PULSE_DURATION_MS = 600

        const val DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ = 600
        const val MIN_SIGNAL_PULSE_FREQUENCY_HZ = 400
        const val MAX_SIGNAL_PULSE_FREQUENCY_HZ = 5_000
        const val SIGNAL_PULSE_FREQUENCY_STEP_HZ = 10
        const val VERY_STRONG_FREQUENCY_MULTIPLIER = 1.25

        /** Gap between end of alert tone and start of TTS. */
        const val ALERT_VOICE_GAP_MS = 50L

        fun voiceDelayAfterAlertTone(toneDurationMs: Int): Duration {
            return (toneDurationMs.coerceAtLeast(0).toLong() + ALERT_VOICE_GAP_MS).milliseconds
        }
    }
}
