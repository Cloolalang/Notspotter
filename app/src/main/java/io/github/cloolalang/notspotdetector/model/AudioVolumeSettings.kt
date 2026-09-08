package io.github.cloolalang.notspotdetector.model

data class AudioVolumeSettings(
    val pingClickVolume: Float = DEFAULT_VOLUME,
    val lowSignalClickVolume: Float = DEFAULT_VOLUME,
    val signalPulseDurationMs: Int = DEFAULT_SIGNAL_PULSE_DURATION_MS,
    val cellChangeBellVolume: Float = DEFAULT_VOLUME,
    val technologyChangeVolume: Float = DEFAULT_VOLUME,
    val noSignalToneVolume: Float = DEFAULT_VOLUME,
    val limitedServiceToneVolume: Float = DEFAULT_VOLUME
) {
    fun normalized(): AudioVolumeSettings {
        return copy(
            pingClickVolume = pingClickVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            lowSignalClickVolume = lowSignalClickVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            signalPulseDurationMs = signalPulseDurationMs.coerceIn(
                MIN_SIGNAL_PULSE_DURATION_MS,
                MAX_SIGNAL_PULSE_DURATION_MS
            ),
            cellChangeBellVolume = cellChangeBellVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            technologyChangeVolume = technologyChangeVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            noSignalToneVolume = noSignalToneVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            limitedServiceToneVolume = limitedServiceToneVolume.coerceIn(MIN_VOLUME, MAX_VOLUME)
        )
    }

    companion object {
        const val DEFAULT_VOLUME = 1f
        const val MIN_VOLUME = 0f
        const val MAX_VOLUME = 1f

        const val DEFAULT_SIGNAL_PULSE_DURATION_MS = 250
        const val MIN_SIGNAL_PULSE_DURATION_MS = 10
        const val MAX_SIGNAL_PULSE_DURATION_MS = 600
    }
}
