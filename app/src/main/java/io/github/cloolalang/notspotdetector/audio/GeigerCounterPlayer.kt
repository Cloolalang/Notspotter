package io.github.cloolalang.notspotdetector.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.ConnectivityStats
import io.github.cloolalang.notspotdetector.model.ConnectionQuality
import io.github.cloolalang.notspotdetector.model.ThresholdSettings
import io.github.cloolalang.notspotdetector.model.computeSignalStrengthClickIntervalMs
import io.github.cloolalang.notspotdetector.model.resolveG2SignalStrengthTier
import io.github.cloolalang.notspotdetector.model.resolvePassiveClickRateTier
import io.github.cloolalang.notspotdetector.model.resolveSignalStrengthTier
import io.github.cloolalang.notspotdetector.model.SignalStrengthTier
import io.github.cloolalang.notspotdetector.model.usesG2SignalTiers
import io.github.cloolalang.notspotdetector.model.shouldPlay2gLimitedServicePulse
import io.github.cloolalang.notspotdetector.model.computeDeadzoneClickIntervalMs
import io.github.cloolalang.notspotdetector.model.shouldPlayContinuousFlatline
import io.github.cloolalang.notspotdetector.model.shouldPlayDeadzoneTier
import io.github.cloolalang.notspotdetector.model.shouldPlayFlatline
import io.github.cloolalang.notspotdetector.model.shouldPlayLimitedServiceTone
import io.github.cloolalang.notspotdetector.model.shouldPlayCurrentTierSignalPulse
import io.github.cloolalang.notspotdetector.model.shouldPlaySignalStrengthInterval
import io.github.cloolalang.notspotdetector.model.shouldPlayVeryStrongSignalIndicator
import io.github.cloolalang.notspotdetector.model.shouldPlayWeakSignalWarning
import io.github.cloolalang.notspotdetector.model.shouldSuppressGeigerClicks
import io.github.cloolalang.notspotdetector.model.hasExtremeLatency
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.isTierSoundEnabled
import io.github.cloolalang.notspotdetector.model.shouldUseNoisyRsrqPassiveClick
import io.github.cloolalang.notspotdetector.model.shouldPlayPassiveSignalAndQualityAlerts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class GeigerCounterPlayer {
    private var audioJob: Job? = null
    private var flatlineTrack: AudioTrack? = null
    private var flatlineTrackVolume = -1f
    private var flatlineTrackContinuous = false
    private var flatlineTrackPassiveOnly = false
    private var limitedServiceTrack: AudioTrack? = null
    private var limitedServiceTrackVolume = -1f
    private var limitedServiceTrack2gPulse = false
    private var limitedServiceTrackPassiveOnly = false
    private var lastHandledPingTimestampMs = 0L
    private var monitoringSettingsProvider: () -> MonitoringSettings = { MonitoringSettings() }
    private var passiveSignalSettingsProvider: () -> PassiveSignalSettings = { PassiveSignalSettings() }
    private val sampleRate = 44_100
    private val goodConnectionClickDurationMs = 6
    private val lowQualityClickDurationMs = 150

    fun start(
        scope: CoroutineScope,
        statsProvider: () -> ConnectivityStats,
        thresholdsProvider: () -> ThresholdSettings,
        audioVolumesProvider: () -> AudioVolumeSettings,
        monitoringSettingsProvider: () -> MonitoringSettings,
        passiveSignalSettingsProvider: () -> PassiveSignalSettings
    ) {
        stop()
        this.monitoringSettingsProvider = monitoringSettingsProvider
        this.passiveSignalSettingsProvider = passiveSignalSettingsProvider
        lastHandledPingTimestampMs = 0L
        audioJob = scope.launch {
            while (isActive) {
                val stats = statsProvider()
                val thresholds = thresholdsProvider()
                val volumes = audioVolumesProvider().normalized()
                val passiveSettings = passiveSignalSettingsProvider()
                val passiveAlertsEnabled = shouldPlayPassiveSignalAndQualityAlerts(stats, passiveSettings)
                when {
                    !stats.isMonitoring -> {
                        stopAlertTones()
                        delay(500)
                    }
                    stats.shouldPlay2gLimitedServicePulse() -> {
                        stopFlatline()
                        if (passiveAlertsEnabled) {
                            ensureLimitedServicePlaying(
                                volume = volumes.limitedServiceToneVolume,
                                use2gPulse = true,
                                passiveOnly = stats.isPassiveOnlySession
                            )
                        } else {
                            stopLimitedService()
                        }
                        delay(200)
                    }
                    stats.shouldPlayLimitedServiceTone() -> {
                        stopFlatline()
                        if (passiveAlertsEnabled) {
                            ensureLimitedServicePlaying(
                                volume = volumes.limitedServiceToneVolume,
                                use2gPulse = false,
                                passiveOnly = stats.isPassiveOnlySession
                            )
                        } else {
                            stopLimitedService()
                        }
                        delay(200)
                    }
                    stats.shouldPlayDeadzoneTier(passiveSettings) -> {
                        stopLimitedService()
                        stopFlatline()
                        handleDeadzoneTierAudio(stats, volumes, passiveSettings)
                    }
                    stats.shouldPlayFlatline(passiveSettings) -> {
                        stopLimitedService()
                        ensureFlatlinePlaying(
                            volume = volumes.noSignalToneVolume,
                            continuous = stats.shouldPlayContinuousFlatline(passiveSettings),
                            passiveOnly = stats.isPassiveOnlySession
                        )
                        delay(200)
                    }
                    else -> {
                        stopAlertTones()
                        if (stats.cellularAvailable) {
                            when {
                                stats.shouldPlaySignalStrengthInterval(passiveSettings) ->
                                    handleSignalStrengthIntervalAudio(stats, volumes, passiveSettings)
                                stats.isPassiveIdleMode -> delay(POLL_INTERVAL_MS)
                                stats.quality == ConnectionQuality.GOOD && !stats.hasExtremeLatency() ->
                                    handleGoodConnectionAudio(stats, thresholds, volumes, passiveSettings)
                                else -> handleIntervalAudio(stats, thresholds, volumes, passiveSettings)
                            }
                        } else {
                            delay(computeClickIntervalMs(stats))
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        audioJob?.cancel()
        audioJob = null
        lastHandledPingTimestampMs = 0L
        stopAlertTones()
    }

    private suspend fun handleGoodConnectionAudio(
        stats: ConnectivityStats,
        thresholds: ThresholdSettings,
        volumes: AudioVolumeSettings,
        passiveSettings: PassiveSignalSettings
    ) {
        if (stats.lastPingTimestampMs > lastHandledPingTimestampMs) {
            lastHandledPingTimestampMs = stats.lastPingTimestampMs
            if (!stats.shouldSuppressGeigerClicks(thresholds, passiveSettings)) {
                playGoodConnectionClicks(thresholds, volumes)
            }
        }
        delay(POLL_INTERVAL_MS)
    }

    private suspend fun handleSignalStrengthIntervalAudio(
        stats: ConnectivityStats,
        volumes: AudioVolumeSettings,
        passiveSettings: PassiveSignalSettings
    ) {
        val interval = stats.computeSignalStrengthClickIntervalMs(
            passiveSettings,
            volumes.signalPulseDurationMs
        )
        if (shouldPlayPassiveSignalAndQualityAlerts(stats, passiveSettings) &&
            stats.shouldPlayCurrentTierSignalPulse(passiveSettings)
        ) {
            val tier = when {
                stats.usesG2SignalTiers() ->
                    stats.resolveG2SignalStrengthTier(passiveSettings) ?: SignalStrengthTier.G2_WEAK
                stats.shouldPlayVeryStrongSignalIndicator(passiveSettings) -> SignalStrengthTier.MILD
                else -> stats.resolvePassiveClickRateTier(passiveSettings) ?: SignalStrengthTier.FAIR
            }
            val isVeryStrong = stats.shouldPlayVeryStrongSignalIndicator(passiveSettings)
            val pulseFrequencyHz = if (isVeryStrong) {
                volumes.veryStrongPulseFrequencyHz().toDouble()
            } else {
                volumes.signalPulseFrequencyHz.toDouble()
            }
            val mixWhiteNoise = stats.isPassiveOnlySession &&
                passiveSettings.shouldUseNoisyRsrqPassiveClick(stats.rsrqDb)
            playTieredWeakSignalClick(
                volumes = volumes,
                tier = tier,
                frequencyHz = pulseFrequencyHz,
                mixWhiteNoise = mixWhiteNoise
            )
        }
        delay(interval)
    }

    private suspend fun handleDeadzoneTierAudio(
        stats: ConnectivityStats,
        volumes: AudioVolumeSettings,
        passiveSettings: PassiveSignalSettings
    ) {
        val pulseDurationMs = passiveSettings.deadzoneTierPulseDurationMs
        val interval = stats.computeDeadzoneClickIntervalMs(
            passiveSettings,
            pulseDurationMs
        )
        if (shouldPlayPassiveSignalAndQualityAlerts(stats, passiveSettings)) {
            playTieredWeakSignalClick(
                volumes = volumes,
                tier = SignalStrengthTier.DEADZONE,
                pulseDurationMs = pulseDurationMs
            )
        }
        delay(interval)
    }

    private fun shouldPlayPassiveSignalAndQualityAlerts(
        stats: ConnectivityStats,
        passiveSettings: PassiveSignalSettings
    ): Boolean {
        return stats.shouldPlayPassiveSignalAndQualityAlerts(
            monitoringSettingsProvider(),
            passiveSettings
        )
    }

    private suspend fun playGoodConnectionClicks(
        thresholds: ThresholdSettings,
        volumes: AudioVolumeSettings
    ) {
        val clickCount = thresholds.goodConnectionClicksPerPing
        repeat(clickCount) { index ->
            playConnectionClick(volumes)
            if (index < clickCount - 1) {
                delay(GOOD_CLICK_GAP_MS)
            }
        }
    }

    private suspend fun handleIntervalAudio(
        stats: ConnectivityStats,
        thresholds: ThresholdSettings,
        volumes: AudioVolumeSettings,
        passiveSettings: PassiveSignalSettings
    ) {
        val interval = computeClickIntervalMs(stats)
        if (stats.shouldSuppressGeigerClicks(thresholds, passiveSettings)) {
            delay(interval)
            return
        }

        if (stats.shouldPlayWeakSignalWarning(passiveSettings)) {
            val tier = if (stats.usesG2SignalTiers()) {
                stats.resolveG2SignalStrengthTier(passiveSettings) ?: SignalStrengthTier.G2_WEAK
            } else {
                stats.resolveSignalStrengthTier(passiveSettings) ?: SignalStrengthTier.FAIR
            }
            if (shouldPlayPassiveSignalAndQualityAlerts(stats, passiveSettings) &&
                passiveSettings.isTierSoundEnabled(tier)
            ) {
                playTieredWeakSignalClick(
                    volumes = volumes,
                    tier = tier
                )
            }
            delay(
                stats.computeSignalStrengthClickIntervalMs(
                    passiveSettings,
                    volumes.signalPulseDurationMs
                )
            )
        } else {
            playLowQualityClick(volumes)
            delay(interval)
        }
    }

    private fun playConnectionClick(volumes: AudioVolumeSettings) {
        if (volumes.pingClickVolume <= 0f) return
        playClick(
            frequencyHz = TONE_FREQUENCY_HZ,
            durationMs = goodConnectionClickDurationMs,
            amplitude = CLICK_AMPLITUDE * volumes.pingClickVolume
        )
    }

    private fun playLowQualityClick(volumes: AudioVolumeSettings) {
        if (volumes.pingClickVolume <= 0f) return
        playClick(
            frequencyHz = TONE_FREQUENCY_HZ,
            durationMs = lowQualityClickDurationMs,
            amplitude = CLICK_AMPLITUDE * volumes.pingClickVolume
        )
    }

    private fun playTieredWeakSignalClick(
        volumes: AudioVolumeSettings,
        tier: SignalStrengthTier,
        frequencyHz: Double = volumes.signalPulseFrequencyHz.toDouble(),
        mixWhiteNoise: Boolean = false,
        pulseDurationMs: Int? = null
    ) {
        if (volumes.lowSignalClickVolume <= 0f) return
        playSineToneBurst(
            frequencyHz = frequencyHz,
            durationMs = pulseDurationMs ?: tier.pulseDurationMs(volumes.signalPulseDurationMs),
            amplitude = FLATLINE_AMPLITUDE * volumes.lowSignalClickVolume,
            mixWhiteNoise = mixWhiteNoise
        )
    }

    /**
     * Sustained sine burst for RSRP/RSRQ tier alerts (no click decay).
     */
    private fun playSineToneBurst(
        frequencyHz: Double,
        durationMs: Int,
        amplitude: Float,
        mixWhiteNoise: Boolean = false
    ) {
        if (amplitude <= 0f) return

        val sampleCount = sampleRate * durationMs / 1_000
        val buffer = ShortArray(sampleCount)
        val fadeSamples = (sampleRate * SINE_BURST_FADE_MS / 1_000).coerceAtMost(sampleCount / 4)
        val noiseMix = if (mixWhiteNoise) NOISY_RSRQ_WHITE_NOISE_MIX else 0.0
        val toneMix = 1.0 - noiseMix

        for (i in buffer.indices) {
            val timeSec = i.toDouble() / sampleRate
            val sine = sin(2.0 * PI * frequencyHz * timeSec)
            val noise = if (mixWhiteNoise) Random.nextDouble(-1.0, 1.0) else 0.0
            val sample = sine * toneMix + noise * noiseMix
            val envelope = when {
                fadeSamples <= 0 -> 1.0
                i < fadeSamples -> i.toDouble() / fadeSamples
                i >= sampleCount - fadeSamples -> (sampleCount - i).toDouble() / fadeSamples
                else -> 1.0
            }
            buffer[i] = (sample * envelope * Short.MAX_VALUE * amplitude).toInt().toShort()
        }

        val audioTrack = buildAudioTrack(buffer.size * 2, AudioTrack.MODE_STATIC)
        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        audioTrack.setNotificationMarkerPosition(sampleCount)
        audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack?) {
                track?.release()
            }

            override fun onPeriodicNotification(track: AudioTrack?) = Unit
        })
    }

    private fun playWeakSignalClick(volumes: AudioVolumeSettings) {
        playTieredWeakSignalClick(volumes, SignalStrengthTier.MILD)
    }

    fun computeClickIntervalMs(stats: ConnectivityStats): Long {
        if (!stats.isMonitoring || !stats.cellularAvailable) {
            return 2_000L
        }

        val interval = when (stats.quality) {
            ConnectionQuality.GOOD -> 1_800L
            ConnectionQuality.DEGRADED -> {
                val scaled = 1_800L - (stats.severity * 1_200L).toLong()
                scaled.coerceIn(400L, 1_800L)
            }
            ConnectionQuality.POOR -> {
                val scaled = 400L - (stats.severity * 320L).toLong()
                scaled.coerceIn(60L, 400L)
            }
            ConnectionQuality.NO_CELLULAR -> 2_000L
            ConnectionQuality.PASSIVE_IDLE -> 2_000L
            ConnectionQuality.MONITORING_STOPPED -> 2_000L
        }

        return if (stats.hasExtremeLatency()) {
            (interval / EXTREME_LATENCY_SPEED_MULTIPLIER).coerceAtLeast(MIN_CLICK_INTERVAL_MS)
        } else {
            interval
        }
    }

    @Synchronized
    private fun ensureFlatlinePlaying(
        volume: Float,
        continuous: Boolean,
        passiveOnly: Boolean
    ) {
        if (volume <= 0f) {
            stopFlatline()
            return
        }

        val track = flatlineTrack
        if (track != null &&
            track.playState == AudioTrack.PLAYSTATE_PLAYING &&
            continuous == flatlineTrackContinuous &&
            passiveOnly == flatlineTrackPassiveOnly
        ) {
            if (volume != flatlineTrackVolume) {
                track.setVolume(volume)
                flatlineTrackVolume = volume
            }
            return
        }

        stopFlatline()
        flatlineTrack = if (continuous) {
            buildContinuousFlatlineTrack()
        } else {
            buildPulsedFlatlineTrack(passiveOffMs = flatlineOffMs(passiveOnly))
        }.apply {
            setVolume(volume)
            play()
        }
        flatlineTrackVolume = volume
        flatlineTrackContinuous = continuous
        flatlineTrackPassiveOnly = passiveOnly
    }

    @Synchronized
    private fun stopFlatline() {
        flatlineTrack?.let { track ->
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
            track.release()
        }
        flatlineTrack = null
        flatlineTrackVolume = -1f
        flatlineTrackContinuous = false
        flatlineTrackPassiveOnly = false
    }

    @Synchronized
    private fun ensureLimitedServicePlaying(
        volume: Float,
        use2gPulse: Boolean,
        passiveOnly: Boolean
    ) {
        if (volume <= 0f) {
            stopLimitedService()
            return
        }

        val track = limitedServiceTrack
        if (track != null &&
            track.playState == AudioTrack.PLAYSTATE_PLAYING &&
            use2gPulse == limitedServiceTrack2gPulse &&
            passiveOnly == limitedServiceTrackPassiveOnly
        ) {
            if (volume != limitedServiceTrackVolume) {
                track.setVolume(volume)
                limitedServiceTrackVolume = volume
            }
            return
        }

        stopLimitedService()
        limitedServiceTrack = if (use2gPulse) {
            build2gLimitedServicePulseTrack(
                passiveOffMs = gsmLimitedOffMs(passiveOnly)
            )
        } else {
            buildLimitedServiceLoopTrack(
                passivePauseMs = limitedServicePauseMs(passiveOnly)
            )
        }.apply {
            setVolume(volume)
            play()
        }
        limitedServiceTrackVolume = volume
        limitedServiceTrack2gPulse = use2gPulse
        limitedServiceTrackPassiveOnly = passiveOnly
    }

    @Synchronized
    private fun stopLimitedService() {
        limitedServiceTrack?.let { track ->
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
            track.release()
        }
        limitedServiceTrack = null
        limitedServiceTrackVolume = -1f
        limitedServiceTrack2gPulse = false
        limitedServiceTrackPassiveOnly = false
    }

    @Synchronized
    private fun stopAlertTones() {
        stopFlatline()
        stopLimitedService()
    }

    private fun buildLimitedServiceLoopTrack(passivePauseMs: Int = LIMITED_SERVICE_PAUSE_MS): AudioTrack {
        val toneMs = LIMITED_SERVICE_TONE_MS
        val pauseMs = passivePauseMs
        val toneSampleCount = sampleRate * toneMs / 1_000
        val pauseSampleCount = sampleRate * pauseMs / 1_000
        val frequencies = doubleArrayOf(
            LIMITED_SERVICE_TONE_LOW_HZ,
            LIMITED_SERVICE_TONE_HIGH_HZ,
            LIMITED_SERVICE_TONE_LOW_HZ,
            LIMITED_SERVICE_TONE_HIGH_HZ
        )
        val buffer = ShortArray(frequencies.size * toneSampleCount + pauseSampleCount)
        var offset = 0

        for (frequencyHz in frequencies) {
            for (i in 0 until toneSampleCount) {
                val timeSec = i.toDouble() / sampleRate
                val sample = sin(2.0 * PI * frequencyHz * timeSec)
                buffer[offset + i] = (sample * Short.MAX_VALUE * LIMITED_SERVICE_AMPLITUDE).toInt().toShort()
            }
            offset += toneSampleCount
        }

        val track = buildAudioTrack(buffer.size * 2, AudioTrack.MODE_STATIC)
        track.write(buffer, 0, buffer.size)
        track.setLoopPoints(0, buffer.size, -1)
        return track
    }

    private fun build2gLimitedServicePulseTrack(passiveOffMs: Int = GSM_LIMITED_OFF_MS): AudioTrack {
        val onSampleCount = sampleRate * GSM_LIMITED_ON_MS / 1_000
        val offSampleCount = sampleRate * passiveOffMs / 1_000
        val buffer = ShortArray(onSampleCount + offSampleCount)

        for (i in 0 until onSampleCount) {
            val timeSec = i.toDouble() / sampleRate
            val sample = sin(2.0 * PI * LIMITED_SERVICE_TONE_LOW_HZ * timeSec)
            buffer[i] = (sample * Short.MAX_VALUE * LIMITED_SERVICE_AMPLITUDE).toInt().toShort()
        }

        val track = buildAudioTrack(buffer.size * 2, AudioTrack.MODE_STATIC)
        track.write(buffer, 0, buffer.size)
        track.setLoopPoints(0, buffer.size, -1)
        return track
    }

    private fun buildPulsedFlatlineTrack(passiveOffMs: Int = FLATLINE_OFF_MS): AudioTrack {
        val onSampleCount = sampleRate * FLATLINE_ON_MS / 1_000
        val offSampleCount = sampleRate * passiveOffMs / 1_000
        val buffer = ShortArray(onSampleCount + offSampleCount)

        for (i in 0 until onSampleCount) {
            val timeSec = i.toDouble() / sampleRate
            val sample = sin(2.0 * PI * NO_SIGNAL_TONE_HZ * timeSec)
            buffer[i] = (sample * Short.MAX_VALUE * FLATLINE_AMPLITUDE).toInt().toShort()
        }

        val track = buildAudioTrack(buffer.size * 2, AudioTrack.MODE_STATIC)
        track.write(buffer, 0, buffer.size)
        track.setLoopPoints(0, buffer.size, -1)
        return track
    }

    private fun buildContinuousFlatlineTrack(): AudioTrack {
        val sampleCount = sampleRate * FLATLINE_CONTINUOUS_LOOP_MS / 1_000
        val buffer = ShortArray(sampleCount)

        for (i in buffer.indices) {
            val timeSec = i.toDouble() / sampleRate
            val sample = sin(2.0 * PI * NO_SIGNAL_TONE_HZ * timeSec)
            buffer[i] = (sample * Short.MAX_VALUE * FLATLINE_AMPLITUDE).toInt().toShort()
        }

        val track = buildAudioTrack(buffer.size * 2, AudioTrack.MODE_STATIC)
        track.write(buffer, 0, buffer.size)
        track.setLoopPoints(0, buffer.size, -1)
        return track
    }

    fun playCellChangeBell(volume: Float) {
        if (volume <= 0f) return
        playBellChime(volume * BELL_AMPLITUDE)
    }

    fun playTechnologyChangeTone(volume: Float) {
        if (volume <= 0f) return
        playTechnologySweep(volume * TECHNOLOGY_CHANGE_AMPLITUDE)
    }

    fun previewPingClick(volume: Float) {
        if (volume <= 0f) return
        playClick(
            frequencyHz = TONE_FREQUENCY_HZ,
            durationMs = goodConnectionClickDurationMs,
            amplitude = CLICK_AMPLITUDE * volume
        )
    }

    fun previewLowSignalClick(volume: Float, pulseDurationMs: Int, frequencyHz: Int) {
        if (volume <= 0f) return
        playSineToneBurst(
            frequencyHz = frequencyHz.coerceIn(
                AudioVolumeSettings.MIN_SIGNAL_PULSE_FREQUENCY_HZ,
                AudioVolumeSettings.MAX_SIGNAL_PULSE_FREQUENCY_HZ
            ).toDouble(),
            durationMs = pulseDurationMs.coerceIn(
                AudioVolumeSettings.MIN_SIGNAL_PULSE_DURATION_MS,
                AudioVolumeSettings.MAX_SIGNAL_PULSE_DURATION_MS
            ),
            amplitude = FLATLINE_AMPLITUDE * volume
        )
    }

    fun previewNoSignalTone(volume: Float) {
        if (volume <= 0f) return
        playSineToneBurst(
            frequencyHz = NO_SIGNAL_TONE_HZ,
            durationMs = NO_SIGNAL_ALERT_TONE_DURATION_MS,
            amplitude = FLATLINE_AMPLITUDE * volume
        )
    }

    fun previewLimitedServiceTone(volume: Float) {
        if (volume <= 0f) return
        val amplitude = LIMITED_SERVICE_AMPLITUDE * volume
        val toneSampleCount = sampleRate * LIMITED_SERVICE_TONE_MS / 1_000
        val frequencies = doubleArrayOf(
            LIMITED_SERVICE_TONE_LOW_HZ,
            LIMITED_SERVICE_TONE_HIGH_HZ,
            LIMITED_SERVICE_TONE_LOW_HZ,
            LIMITED_SERVICE_TONE_HIGH_HZ
        )
        val buffer = ShortArray(frequencies.size * toneSampleCount)
        var offset = 0
        for (frequencyHz in frequencies) {
            for (i in 0 until toneSampleCount) {
                val timeSec = i.toDouble() / sampleRate
                val sample = sin(2.0 * PI * frequencyHz * timeSec)
                buffer[offset + i] = (sample * Short.MAX_VALUE * amplitude).toInt().toShort()
            }
            offset += toneSampleCount
        }
        playOneShotBuffer(buffer)
    }

    private fun playOneShotBuffer(buffer: ShortArray) {
        val audioTrack = buildAudioTrack(buffer.size * 2, AudioTrack.MODE_STATIC)
        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        audioTrack.setNotificationMarkerPosition(buffer.size)
        audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack?) {
                track?.release()
            }

            override fun onPeriodicNotification(track: AudioTrack?) = Unit
        })
    }

    private fun playTechnologySweep(amplitude: Float) {
        if (amplitude <= 0f) return

        val durationMs = TECHNOLOGY_CHANGE_TONE_DURATION_MS
        val sampleCount = sampleRate * durationMs / 1_000
        val buffer = ShortArray(sampleCount)
        var phase = 0.0

        for (i in buffer.indices) {
            val progress = i.toDouble() / sampleCount.coerceAtLeast(1)
            val frequencyHz = TECHNOLOGY_CHANGE_START_HZ +
                (TECHNOLOGY_CHANGE_END_HZ - TECHNOLOGY_CHANGE_START_HZ) * progress
            phase += 2.0 * PI * frequencyHz / sampleRate
            val envelope = sin(PI * progress)
            buffer[i] = (sin(phase) * envelope * Short.MAX_VALUE * amplitude).toInt().toShort()
        }

        val audioTrack = buildAudioTrack(buffer.size * 2, AudioTrack.MODE_STATIC)
        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        audioTrack.setNotificationMarkerPosition(sampleCount)
        audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack?) {
                track?.release()
            }

            override fun onPeriodicNotification(track: AudioTrack?) = Unit
        })
    }

    private fun playBellChime(amplitude: Float) {
        if (amplitude <= 0f) return

        val durationMs = CELL_CHANGE_BELL_DURATION_MS
        val sampleCount = sampleRate * durationMs / 1_000
        val buffer = ShortArray(sampleCount)
        val partials = arrayOf(
            880.0 to 1.0,
            1_318.5 to 0.55,
            1_760.0 to 0.3
        )

        for (i in buffer.indices) {
            val timeSec = i.toDouble() / sampleRate
            val envelope = exp(-timeSec * BELL_DECAY_RATE)
            var sample = 0.0
            for ((frequencyHz, weight) in partials) {
                sample += sin(2.0 * PI * frequencyHz * timeSec) * weight
            }
            buffer[i] = (sample * envelope * Short.MAX_VALUE * amplitude).toInt().toShort()
        }

        val audioTrack = buildAudioTrack(buffer.size * 2, AudioTrack.MODE_STATIC)
        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        audioTrack.setNotificationMarkerPosition(sampleCount)
        audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack?) {
                track?.release()
            }

            override fun onPeriodicNotification(track: AudioTrack?) = Unit
        })
    }

    private fun playClick(frequencyHz: Double, durationMs: Int, amplitude: Float) {
        if (amplitude <= 0f) return

        val sampleCount = sampleRate * durationMs / 1_000
        val buffer = ShortArray(sampleCount)
        val decayRate = if (durationMs <= goodConnectionClickDurationMs) 900.0 else 650.0

        for (i in buffer.indices) {
            val timeSec = i.toFloat() / sampleRate
            val envelope = exp(-timeSec * decayRate).toFloat()
            val tick = sin(2.0 * PI * frequencyHz * timeSec).toFloat()
            buffer[i] = (tick * envelope * Short.MAX_VALUE * amplitude).toInt().toShort()
        }

        val audioTrack = buildAudioTrack(buffer.size * 2, AudioTrack.MODE_STATIC)
        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        audioTrack.setNotificationMarkerPosition(sampleCount)
        audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack?) {
                track?.release()
            }

            override fun onPeriodicNotification(track: AudioTrack?) = Unit
        })
    }

    private fun buildAudioTrack(bufferSizeBytes: Int, mode: Int): AudioTrack {
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSizeBytes)
            .setTransferMode(mode)
            .build()
    }

    companion object {
        /** Durations of one-shot alert tones (used for voice-announcement timing). */
        const val CELL_CHANGE_BELL_DURATION_MS = 450
        const val TECHNOLOGY_CHANGE_TONE_DURATION_MS = 300
        const val NO_SIGNAL_ALERT_TONE_DURATION_MS = 250
        const val LIMITED_SERVICE_ALERT_TONE_DURATION_MS = 800

        const val TONE_FREQUENCY_HZ = 3_200.0
        private const val PASSIVE_PULSE_RATE_MULTIPLIER = 2
        private fun flatlineOffMs(passiveOnly: Boolean): Int {
            val base = if (passiveOnly) FLATLINE_OFF_MS / PASSIVE_PULSE_RATE_MULTIPLIER else FLATLINE_OFF_MS
            return base
        }

        private fun limitedServicePauseMs(passiveOnly: Boolean): Int {
            val base = if (passiveOnly) {
                LIMITED_SERVICE_PAUSE_MS / PASSIVE_PULSE_RATE_MULTIPLIER
            } else {
                LIMITED_SERVICE_PAUSE_MS
            }
            return base
        }

        private fun gsmLimitedOffMs(passiveOnly: Boolean): Int {
            val base = if (passiveOnly) GSM_LIMITED_OFF_MS / PASSIVE_PULSE_RATE_MULTIPLIER else GSM_LIMITED_OFF_MS
            return base
        }

        private const val CLICK_AMPLITUDE = 0.45f
        private const val WARNING_CLICK_AMPLITUDE = 0.48f
        private const val FLATLINE_AMPLITUDE = 0.35f
        /** D♭4 — no-signal / flatline tone (was 600 Hz). */
        private const val NO_SIGNAL_TONE_HZ = 554.0
        private const val FLATLINE_ON_MS = 250
        private const val NOISY_RSRQ_WHITE_NOISE_MIX = 0.38
        private const val SINE_BURST_FADE_MS = 8
        private const val FLATLINE_OFF_MS = 1_000
        private const val FLATLINE_CONTINUOUS_LOOP_MS = 250
        private const val LIMITED_SERVICE_TONE_LOW_HZ = 880.0
        private const val LIMITED_SERVICE_TONE_HIGH_HZ = 660.0
        private const val LIMITED_SERVICE_TONE_MS = 200
        private const val LIMITED_SERVICE_PAUSE_MS = 1_000
        private const val GSM_LIMITED_ON_MS = 300
        private const val GSM_LIMITED_OFF_MS = 300
        private const val LIMITED_SERVICE_AMPLITUDE = 0.38f
        private const val BELL_AMPLITUDE = 0.5f
        private const val BELL_DECAY_RATE = 3.5
        private const val TECHNOLOGY_CHANGE_AMPLITUDE = 0.46f
        private const val TECHNOLOGY_CHANGE_START_HZ = 320.0
        private const val TECHNOLOGY_CHANGE_END_HZ = 720.0
        private const val GOOD_CLICK_GAP_MS = 80L
        private const val POLL_INTERVAL_MS = 200L
        private const val EXTREME_LATENCY_SPEED_MULTIPLIER = 10L
        private const val MIN_CLICK_INTERVAL_MS = 8L
    }
}
