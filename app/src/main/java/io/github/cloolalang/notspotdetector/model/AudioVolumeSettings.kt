package io.github.cloolalang.notspotdetector.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class AudioVolumeSettings(
    /**
     * Master on/off switch for every spoken voice announcement (VA-1 through VA-18 and beyond).
     * When false, all TTS speech is suppressed at the [io.github.cloolalang.notspotdetector.service.ConnectivityMonitorService]
     * playback choke points — alert tones, bells, clicks, and vibration are unaffected, only the
     * spoken announcement text is muted. Manual "preview" buttons in Settings are also unaffected,
     * so users can still audition a voice while announcements are globally muted.
     */
    val masterVoiceAnnouncementsEnabled: Boolean = DEFAULT_MASTER_VOICE_ANNOUNCEMENTS_ENABLED,
    val pingClickVolume: Float = DEFAULT_VOLUME,
    val lowSignalClickVolume: Float = DEFAULT_VOLUME,
    /** RXSS 6 (signal low / critical) signal pulse frequency. */
    val signalPulseFrequencyHz: Int = DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ,
    /** RXSS 10/0/11/15 no-signal camp signal pulse frequency — independent of [signalPulseFrequencyHz]. */
    val noSignalTierPulseFrequencyHz: Int = DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ,
    /** RXSS 12/13 limited-service camp signal pulse frequency — independent of [signalPulseFrequencyHz]. */
    val limitedServiceTierPulseFrequencyHz: Int = DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ,
    /** Level Ranges A–D (RXSS 2–5) tone frequency — independent of [signalPulseFrequencyHz]. */
    val levelRangeBcdPulseFrequencyHz: Int = DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ,
    /** Tier 1 (very strong RSRP) tone frequency — independent of [signalPulseFrequencyHz]. */
    val veryStrongTierPulseFrequencyHz: Int = DEFAULT_VERY_STRONG_TIER_PULSE_FREQUENCY_HZ,
    /** Tier 7 (2G strong) tone frequency — independent of [signalPulseFrequencyHz]. */
    val g2StrongTierPulseFrequencyHz: Int = DEFAULT_G2_STRONG_TIER_PULSE_FREQUENCY_HZ,
    /** Tier 8 (2G weak) tone frequency — independent of [signalPulseFrequencyHz]. */
    val g2WeakTierPulseFrequencyHz: Int = DEFAULT_G2_WEAK_TIER_PULSE_FREQUENCY_HZ,
    val signalPulseDurationMs: Int = DEFAULT_SIGNAL_PULSE_DURATION_MS,
    /**
     * @deprecated Level Ranges A–D (RXSS 2–5) now each have an independent pulse duration
     * ([PassiveSignalSettings.mildTierPulseDurationMs] and siblings). This field is kept only as the
     * one-time migration seed for those per-range values and is no longer read during playback or
     * writable from the UI.
     */
    val levelRangeBcdPulseDurationMs: Int = DEFAULT_SIGNAL_PULSE_DURATION_MS,
    /** Level Ranges A–D (RXSS 2–5) click volume — independent of [lowSignalClickVolume]. */
    val levelRangeBcdClickVolume: Float = DEFAULT_VOLUME,
    val cellChangeBellVolume: Float = DEFAULT_VOLUME,
    val cellChangeVoiceEnabled: Boolean = DEFAULT_CELL_CHANGE_VOICE_ENABLED,
    val cellChangeVoiceVolume: Float = DEFAULT_VOLUME,
    /**
     * RXSS 9 alternative announcement — speak the E-UTRA band (derived from the LTE EARFCN)
     * instead of "cell reselect, channel …, PCI …". Falls back to the normal phrasing when the
     * reselected cell has no LTE EARFCN (2G-only or NR-only reselect).
     */
    val cellChangeSpeakBandEnabled: Boolean = DEFAULT_CELL_CHANGE_SPEAK_BAND_ENABLED,
    /** How [cellChangeSpeakBandEnabled] speaks the resolved band — number vs. MHz nickname. */
    val cellChangeBandNamingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT,
    val technologyChangeTo2gToneVolume: Float = DEFAULT_VOLUME,
    val technologyChangeTo2gVoiceEnabled: Boolean = DEFAULT_VOICE_ANNOUNCEMENT_ENABLED,
    val technologyChangeTo2gVoiceVolume: Float = DEFAULT_VOLUME,
    val technologyChangeTo4gToneVolume: Float = DEFAULT_VOLUME,
    val technologyChangeTo4gVoiceEnabled: Boolean = DEFAULT_VOICE_ANNOUNCEMENT_ENABLED,
    val technologyChangeTo4gVoiceVolume: Float = DEFAULT_VOLUME,
    val technologyChangeTo5gEndcToneVolume: Float = DEFAULT_VOLUME,
    val technologyChangeTo5gEndcVoiceEnabled: Boolean = DEFAULT_VOICE_ANNOUNCEMENT_ENABLED,
    val technologyChangeTo5gEndcVoiceVolume: Float = DEFAULT_VOLUME,
    val tier5AnnouncerEnabled: Boolean = DEFAULT_VOICE_ANNOUNCEMENT_ENABLED,
    val tier5AnnouncerVolume: Float = DEFAULT_VOLUME,
    val voiceAnnouncerChoice: VoiceAnnouncerChoice = DEFAULT_VOICE_ANNOUNCER_CHOICE,
    val voiceAnnouncerEngineId: String? = null,
    val noSignalToneVolume: Float = DEFAULT_VOLUME,
    val noSignalVibrationEnabled: Boolean = DEFAULT_NO_SIGNAL_VIBRATION_ENABLED,
    val noSignalVoiceEnabled: Boolean = DEFAULT_VOICE_ANNOUNCEMENT_ENABLED,
    val noSignalVoiceVolume: Float = DEFAULT_VOLUME,
    val limitedServiceToneVolume: Float = DEFAULT_VOLUME,
    val limitedServiceVoiceEnabled: Boolean = DEFAULT_VOICE_ANNOUNCEMENT_ENABLED,
    val limitedServiceVoiceVolume: Float = DEFAULT_VOLUME,
    /**
     * Global VA panel — when false, the operator name (e.g. "E E") is omitted from every spoken
     * voice announcement app-wide (cell reselect, technology change, no-signal, limited service,
     * tier 5, deadzone, searching 2G, 2G camped). Does not affect tones/clicks/vibration.
     */
    val speakOperatorNameEnabled: Boolean = DEFAULT_SPEAK_OPERATOR_NAME_ENABLED,
    /**
     * Global VA panel — when false, the technology (e.g. "4 G", "5 G E N D C") is omitted from
     * every spoken voice announcement app-wide. Does not affect tones/clicks/vibration.
     */
    val speakTechnologyEnabled: Boolean = DEFAULT_SPEAK_TECHNOLOGY_ENABLED
) {
    fun pulseFrequencyHzForTier(tier: SignalStrengthTier): Int {
        return when (tier) {
            SignalStrengthTier.G2_STRONG -> g2StrongTierPulseFrequencyHz
            SignalStrengthTier.G2_WEAK -> g2WeakTierPulseFrequencyHz
            SignalStrengthTier.MILD, SignalStrengthTier.GOOD, SignalStrengthTier.FAIR,
            SignalStrengthTier.POOR -> levelRangeBcdPulseFrequencyHz
            SignalStrengthTier.NO_SIGNAL,
            SignalStrengthTier.G2_NO_SIGNAL,
            SignalStrengthTier.DEADZONE,
            SignalStrengthTier.SEARCHING_2G,
            SignalStrengthTier.WIFI_CALLING -> noSignalTierPulseFrequencyHz
            SignalStrengthTier.LIMITED_SERVICE,
            SignalStrengthTier.LIMITED_ALT_2G -> limitedServiceTierPulseFrequencyHz
            SignalStrengthTier.CRITICAL -> signalPulseFrequencyHz
            else -> signalPulseFrequencyHz
        }
    }

    fun pulseDurationMsForTier(tier: SignalStrengthTier): Int {
        return when (tier) {
            SignalStrengthTier.MILD, SignalStrengthTier.GOOD, SignalStrengthTier.FAIR,
            SignalStrengthTier.POOR -> levelRangeBcdPulseDurationMs
            else -> signalPulseDurationMs
        }
    }

    fun clickVolumeForTier(tier: SignalStrengthTier): Float {
        return when (tier) {
            SignalStrengthTier.MILD, SignalStrengthTier.GOOD, SignalStrengthTier.FAIR,
            SignalStrengthTier.POOR -> levelRangeBcdClickVolume
            SignalStrengthTier.NO_SIGNAL,
            SignalStrengthTier.G2_NO_SIGNAL,
            SignalStrengthTier.DEADZONE,
            SignalStrengthTier.SEARCHING_2G,
            SignalStrengthTier.WIFI_CALLING -> noSignalToneVolume
            SignalStrengthTier.LIMITED_SERVICE,
            SignalStrengthTier.LIMITED_ALT_2G -> limitedServiceToneVolume
            else -> lowSignalClickVolume
        }
    }

    /** RXSS 1 (signal high) signal pulse volume — see [lowSignalClickVolume]. */
    fun veryStrongTierClickVolume(): Float = lowSignalClickVolume

    /** RXSS 6 (signal low) signal pulse volume — see [lowSignalClickVolume]. */
    fun criticalTierClickVolume(): Float = lowSignalClickVolume

    /** RXSS 1 (signal high) signal pulse length — see [signalPulseDurationMs]. */
    fun veryStrongTierPulseDurationMs(): Int = signalPulseDurationMs

    fun technologyChangeAlertVolumes(target: TechnologyChangeTarget): TechnologyChangeAlertVolumes {
        return when (target) {
            TechnologyChangeTarget.TO_2G -> TechnologyChangeAlertVolumes(
                toneVolume = technologyChangeTo2gToneVolume,
                voiceEnabled = technologyChangeTo2gVoiceEnabled,
                voiceVolume = technologyChangeTo2gVoiceVolume
            )
            TechnologyChangeTarget.TO_4G -> TechnologyChangeAlertVolumes(
                toneVolume = technologyChangeTo4gToneVolume,
                voiceEnabled = technologyChangeTo4gVoiceEnabled,
                voiceVolume = technologyChangeTo4gVoiceVolume
            )
            TechnologyChangeTarget.TO_5G_ENDC -> TechnologyChangeAlertVolumes(
                toneVolume = technologyChangeTo5gEndcToneVolume,
                voiceEnabled = technologyChangeTo5gEndcVoiceEnabled,
                voiceVolume = technologyChangeTo5gEndcVoiceVolume
            )
        }
    }

    fun technologyChangeAlertVolumes(radioAccessType: String?): TechnologyChangeAlertVolumes? {
        val target = TechnologyChangeTarget.fromRadioAccessType(radioAccessType) ?: return null
        return technologyChangeAlertVolumes(target)
    }

    fun withTechnologyChangeToneVolume(target: TechnologyChangeTarget, value: Float): AudioVolumeSettings {
        return when (target) {
            TechnologyChangeTarget.TO_2G -> copy(technologyChangeTo2gToneVolume = value)
            TechnologyChangeTarget.TO_4G -> copy(technologyChangeTo4gToneVolume = value)
            TechnologyChangeTarget.TO_5G_ENDC -> copy(technologyChangeTo5gEndcToneVolume = value)
        }
    }

    fun withTechnologyChangeVoiceEnabled(target: TechnologyChangeTarget, enabled: Boolean): AudioVolumeSettings {
        return when (target) {
            TechnologyChangeTarget.TO_2G -> copy(technologyChangeTo2gVoiceEnabled = enabled)
            TechnologyChangeTarget.TO_4G -> copy(technologyChangeTo4gVoiceEnabled = enabled)
            TechnologyChangeTarget.TO_5G_ENDC -> copy(technologyChangeTo5gEndcVoiceEnabled = enabled)
        }
    }

    fun withTechnologyChangeVoiceVolume(target: TechnologyChangeTarget, value: Float): AudioVolumeSettings {
        return when (target) {
            TechnologyChangeTarget.TO_2G -> copy(technologyChangeTo2gVoiceVolume = value)
            TechnologyChangeTarget.TO_4G -> copy(technologyChangeTo4gVoiceVolume = value)
            TechnologyChangeTarget.TO_5G_ENDC -> copy(technologyChangeTo5gEndcVoiceVolume = value)
        }
    }

    fun normalized(): AudioVolumeSettings {
        return copy(
            pingClickVolume = pingClickVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            lowSignalClickVolume = lowSignalClickVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            signalPulseFrequencyHz = signalPulseFrequencyHz.coerceIn(
                MIN_SIGNAL_PULSE_FREQUENCY_HZ,
                MAX_SIGNAL_PULSE_FREQUENCY_HZ
            ),
            noSignalTierPulseFrequencyHz = noSignalTierPulseFrequencyHz.coerceIn(
                MIN_SIGNAL_PULSE_FREQUENCY_HZ,
                MAX_SIGNAL_PULSE_FREQUENCY_HZ
            ),
            limitedServiceTierPulseFrequencyHz = limitedServiceTierPulseFrequencyHz.coerceIn(
                MIN_SIGNAL_PULSE_FREQUENCY_HZ,
                MAX_SIGNAL_PULSE_FREQUENCY_HZ
            ),
            levelRangeBcdPulseFrequencyHz = levelRangeBcdPulseFrequencyHz.coerceIn(
                MIN_SIGNAL_PULSE_FREQUENCY_HZ,
                MAX_SIGNAL_PULSE_FREQUENCY_HZ
            ),
            veryStrongTierPulseFrequencyHz = veryStrongTierPulseFrequencyHz.coerceIn(
                MIN_SIGNAL_PULSE_FREQUENCY_HZ,
                MAX_SIGNAL_PULSE_FREQUENCY_HZ
            ),
            g2StrongTierPulseFrequencyHz = g2StrongTierPulseFrequencyHz.coerceIn(
                MIN_SIGNAL_PULSE_FREQUENCY_HZ,
                MAX_SIGNAL_PULSE_FREQUENCY_HZ
            ),
            g2WeakTierPulseFrequencyHz = g2WeakTierPulseFrequencyHz.coerceIn(
                MIN_SIGNAL_PULSE_FREQUENCY_HZ,
                MAX_SIGNAL_PULSE_FREQUENCY_HZ
            ),
            signalPulseDurationMs = signalPulseDurationMs.coerceIn(
                MIN_SIGNAL_PULSE_DURATION_MS,
                MAX_SIGNAL_PULSE_DURATION_MS
            ),
            levelRangeBcdPulseDurationMs = levelRangeBcdPulseDurationMs.coerceIn(
                MIN_SIGNAL_PULSE_DURATION_MS,
                MAX_SIGNAL_PULSE_DURATION_MS
            ),
            levelRangeBcdClickVolume = levelRangeBcdClickVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            cellChangeBellVolume = cellChangeBellVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            cellChangeVoiceVolume = cellChangeVoiceVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            technologyChangeTo2gToneVolume = technologyChangeTo2gToneVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            technologyChangeTo2gVoiceVolume = technologyChangeTo2gVoiceVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            technologyChangeTo4gToneVolume = technologyChangeTo4gToneVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            technologyChangeTo4gVoiceVolume = technologyChangeTo4gVoiceVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            technologyChangeTo5gEndcToneVolume = technologyChangeTo5gEndcToneVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            technologyChangeTo5gEndcVoiceVolume = technologyChangeTo5gEndcVoiceVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            tier5AnnouncerVolume = tier5AnnouncerVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            noSignalToneVolume = noSignalToneVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            noSignalVoiceVolume = noSignalVoiceVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            limitedServiceToneVolume = limitedServiceToneVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            limitedServiceVoiceVolume = limitedServiceVoiceVolume.coerceIn(MIN_VOLUME, MAX_VOLUME)
        )
    }

    companion object {
        const val DEFAULT_VOLUME = 1f
        const val MIN_VOLUME = 0f
        const val MAX_VOLUME = 1f
        const val DEFAULT_MASTER_VOICE_ANNOUNCEMENTS_ENABLED = true
        const val DEFAULT_CELL_CHANGE_VOICE_ENABLED = false
        const val DEFAULT_CELL_CHANGE_SPEAK_BAND_ENABLED = false
        const val DEFAULT_VOICE_ANNOUNCEMENT_ENABLED = false
        const val DEFAULT_NO_SIGNAL_VIBRATION_ENABLED = false
        const val DEFAULT_SPEAK_OPERATOR_NAME_ENABLED = true
        const val DEFAULT_SPEAK_TECHNOLOGY_ENABLED = true
        val DEFAULT_VOICE_ANNOUNCER_CHOICE = VoiceAnnouncerChoice.SYSTEM_DEFAULT

        const val DEFAULT_SIGNAL_PULSE_DURATION_MS = 250
        const val MIN_SIGNAL_PULSE_DURATION_MS = 10
        const val MAX_SIGNAL_PULSE_DURATION_MS = 600

        const val DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ = 600
        const val DEFAULT_VERY_STRONG_TIER_PULSE_FREQUENCY_HZ = 750
        const val DEFAULT_G2_STRONG_TIER_PULSE_FREQUENCY_HZ = DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ
        const val DEFAULT_G2_WEAK_TIER_PULSE_FREQUENCY_HZ = DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ
        const val MIN_SIGNAL_PULSE_FREQUENCY_HZ = 400
        const val MAX_SIGNAL_PULSE_FREQUENCY_HZ = 5_000
        const val SIGNAL_PULSE_FREQUENCY_STEP_HZ = 10
        /** Used when migrating stored settings that coupled tier 1 to [signalPulseFrequencyHz]. */
        const val LEGACY_VERY_STRONG_FREQUENCY_MULTIPLIER = 1.25

        /** Gap between end of alert tone and start of TTS. */
        const val ALERT_VOICE_GAP_MS = 50L

        fun voiceDelayAfterAlertTone(toneDurationMs: Int): Duration {
            return (toneDurationMs.coerceAtLeast(0).toLong() + ALERT_VOICE_GAP_MS).milliseconds
        }
    }
}
