package io.github.cloolalang.notspotdetector.model

import kotlin.math.roundToInt
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
    val pingClickVolume: Float = DEFAULT_PING_CLICK_VOLUME,
    val lowSignalClickVolume: Float = DEFAULT_VOLUME,
    /** RXSS 6 (signal low / critical) signal pulse frequency. */
    val signalPulseFrequencyHz: Int = DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ,
    /** RXSS 10/0/11/15 no-signal camp signal pulse frequency — independent of [signalPulseFrequencyHz]. */
    val noSignalTierPulseFrequencyHz: Int = DEFAULT_NO_SIGNAL_TIER_PULSE_FREQUENCY_HZ,
    /** RXSS 12/13 limited-service lower tone (and RXSS 13 pulse) frequency. */
    val limitedServiceTierPulseFrequencyHz: Int = DEFAULT_LIMITED_SERVICE_TIER_PULSE_FREQUENCY_HZ,
    /** RXSS 12 two-tone: upper tone is this percent above [limitedServiceTierPulseFrequencyHz]. */
    val limitedServiceTwoToneSpreadPercent: Int = DEFAULT_LIMITED_SERVICE_TWO_TONE_SPREAD_PERCENT,
    /** Level Ranges A–D (RXSS 2–5) tone frequency — independent of [signalPulseFrequencyHz]. */
    val levelRangeBcdPulseFrequencyHz: Int = DEFAULT_LEVEL_RANGE_BCD_PULSE_FREQUENCY_HZ,
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
    val levelRangeBcdPulseDurationMs: Int = DEFAULT_LEVEL_RANGE_BCD_PULSE_DURATION_MS,
    /** Level Ranges A–D (RXSS 2–5) click volume — independent of [lowSignalClickVolume]. */
    val levelRangeBcdClickVolume: Float = DEFAULT_VOLUME,
    val cellChangeBellVolume: Float = DEFAULT_VOLUME,
    val cellChangeVoiceEnabled: Boolean = DEFAULT_CELL_CHANGE_VOICE_ENABLED,
    val cellChangeVoiceVolume: Float = DEFAULT_VOLUME,
    /**
     * RXSS 9 alternative announcement — speak the E-UTRA band (derived from the LTE EARFCN)
     * instead of "cell reselect, channel …, PCI …", or the GSM band (900 / 1800) instead of
     * channel/BSIC on 2G. Falls back to the normal phrasing when the channel cannot be mapped.
     */
    val cellChangeSpeakBandEnabled: Boolean = DEFAULT_CELL_CHANGE_SPEAK_BAND_ENABLED,
    /** How [cellChangeSpeakBandEnabled] speaks the resolved band — number vs. MHz nickname. */
    val cellChangeBandNamingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT,
    val technologyChangeTo2gToneVolume: Float = DEFAULT_VOLUME,
    val technologyChangeTo2gVoiceEnabled: Boolean = DEFAULT_VOICE_ANNOUNCEMENT_ENABLED,
    val technologyChangeTo2gPeriodicVoiceEnabled: Boolean = DEFAULT_PERIODIC_VOICE_ENABLED,
    val technologyChangeTo2gVoiceVolume: Float = DEFAULT_VOLUME,
    val technologyChangeTo4gToneVolume: Float = DEFAULT_VOLUME,
    val technologyChangeTo4gVoiceEnabled: Boolean = DEFAULT_VOICE_ANNOUNCEMENT_ENABLED,
    val technologyChangeTo4gVoiceVolume: Float = DEFAULT_VOLUME,
    val technologyChangeTo5gEndcToneVolume: Float = DEFAULT_VOLUME,
    val technologyChangeTo5gEndcVoiceEnabled: Boolean = DEFAULT_VOICE_ANNOUNCEMENT_ENABLED,
    val technologyChangeTo5gEndcVoiceVolume: Float = DEFAULT_VOLUME,
    val tier5AnnouncerEnabled: Boolean = DEFAULT_VOICE_ANNOUNCEMENT_ENABLED,
    val tier5PeriodicVoiceEnabled: Boolean = DEFAULT_PERIODIC_VOICE_ENABLED,
    val tier5AnnouncerVolume: Float = DEFAULT_TIER5_ANNOUNCER_VOLUME,
    val voiceAnnouncerChoice: VoiceAnnouncerChoice = DEFAULT_VOICE_ANNOUNCER_CHOICE,
    val voiceAnnouncerEngineId: String? = null,
    /** TTS speech rate for every VA. 1.0 is the engine default; the app default is slightly faster. */
    val voiceSpeechRate: Float = DEFAULT_VOICE_SPEECH_RATE,
    val noSignalToneVolume: Float = DEFAULT_VOLUME,
    val noSignalVibrationEnabled: Boolean = DEFAULT_NO_SIGNAL_VIBRATION_ENABLED,
    val noSignalVoiceEnabled: Boolean = DEFAULT_VOICE_ANNOUNCEMENT_ENABLED,
    val noSignalPeriodicVoiceEnabled: Boolean = DEFAULT_PERIODIC_VOICE_ENABLED,
    val noSignalVoiceVolume: Float = DEFAULT_NO_SIGNAL_VOICE_VOLUME,
    val limitedServiceToneVolume: Float = DEFAULT_LIMITED_SERVICE_TONE_VOLUME,
    val limitedServiceVoiceEnabled: Boolean = DEFAULT_VOICE_ANNOUNCEMENT_ENABLED,
    val limitedServicePeriodicVoiceEnabled: Boolean = DEFAULT_PERIODIC_VOICE_ENABLED,
    val limitedServiceVoiceVolume: Float = DEFAULT_LIMITED_SERVICE_VOICE_VOLUME,
    /**
     * Legacy app-wide seed for [VoicePhraseOptions.speakOperatorName]. Per-RXSS copies live in
     * [cellChangePhrases] and siblings; kept so older profiles migrate cleanly.
     */
    val speakOperatorNameEnabled: Boolean = DEFAULT_SPEAK_OPERATOR_NAME_ENABLED,
    /**
     * Legacy app-wide seed for [VoicePhraseOptions.speakTechnology].
     */
    val speakTechnologyEnabled: Boolean = DEFAULT_SPEAK_TECHNOLOGY_ENABLED,
    val cellChangePhrases: VoicePhraseOptions = DEFAULT_CELL_CHANGE_PHRASES,
    val technologyChangeTo2gPhrases: VoicePhraseOptions = DEFAULT_TECH_CHANGE_TO_2G_PHRASES,
    val technologyChangeTo4gPhrases: VoicePhraseOptions = DEFAULT_TECH_CHANGE_TO_4G_PHRASES,
    val technologyChangeTo5gEndcPhrases: VoicePhraseOptions = DEFAULT_TECH_CHANGE_TO_5G_ENDC_PHRASES,
    val signalLowPhrases: VoicePhraseOptions = DEFAULT_SIGNAL_LOW_PHRASES,
    val noSignalPhrases: VoicePhraseOptions = DEFAULT_NO_SIGNAL_PHRASES,
    val limitedServicePhrases: VoicePhraseOptions = DEFAULT_LIMITED_SERVICE_PHRASES
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

    fun phrasesFor(group: VoicePhraseGroup): VoicePhraseOptions {
        return when (group) {
            VoicePhraseGroup.CELL_CHANGE -> cellChangePhrases
            VoicePhraseGroup.TECH_CHANGE_TO_2G -> technologyChangeTo2gPhrases
            VoicePhraseGroup.TECH_CHANGE_TO_4G -> technologyChangeTo4gPhrases
            VoicePhraseGroup.TECH_CHANGE_TO_5G_ENDC -> technologyChangeTo5gEndcPhrases
            VoicePhraseGroup.SIGNAL_LOW -> signalLowPhrases
            VoicePhraseGroup.NO_SIGNAL -> noSignalPhrases
            VoicePhraseGroup.LIMITED_SERVICE -> limitedServicePhrases
        }
    }

    fun withPhrases(group: VoicePhraseGroup, phrases: VoicePhraseOptions): AudioVolumeSettings {
        return when (group) {
            VoicePhraseGroup.CELL_CHANGE -> copy(cellChangePhrases = phrases)
            VoicePhraseGroup.TECH_CHANGE_TO_2G -> copy(technologyChangeTo2gPhrases = phrases)
            VoicePhraseGroup.TECH_CHANGE_TO_4G -> copy(technologyChangeTo4gPhrases = phrases)
            VoicePhraseGroup.TECH_CHANGE_TO_5G_ENDC -> copy(technologyChangeTo5gEndcPhrases = phrases)
            VoicePhraseGroup.SIGNAL_LOW -> copy(signalLowPhrases = phrases)
            VoicePhraseGroup.NO_SIGNAL -> copy(noSignalPhrases = phrases)
            VoicePhraseGroup.LIMITED_SERVICE -> copy(limitedServicePhrases = phrases)
        }
    }

    fun phrasesForTechnologyChange(target: TechnologyChangeTarget): VoicePhraseOptions {
        return when (target) {
            TechnologyChangeTarget.TO_2G -> technologyChangeTo2gPhrases
            TechnologyChangeTarget.TO_4G -> technologyChangeTo4gPhrases
            TechnologyChangeTarget.TO_5G_ENDC -> technologyChangeTo5gEndcPhrases
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

    fun limitedServiceTwoToneHighFrequencyHz(): Int {
        return twoToneHighFrequencyHz(limitedServiceTierPulseFrequencyHz, limitedServiceTwoToneSpreadPercent)
    }

    fun allowsNoSignalPeriodicVoice(): Boolean =
        masterVoiceAnnouncementsEnabled && noSignalVoiceEnabled && noSignalPeriodicVoiceEnabled

    fun allowsLimitedServicePeriodicVoice(): Boolean =
        masterVoiceAnnouncementsEnabled &&
            limitedServiceVoiceEnabled &&
            limitedServicePeriodicVoiceEnabled

    fun allowsTier5PeriodicVoice(): Boolean =
        masterVoiceAnnouncementsEnabled && tier5AnnouncerEnabled && tier5PeriodicVoiceEnabled

    fun allowsG2CampedPeriodicVoice(): Boolean =
        masterVoiceAnnouncementsEnabled &&
            technologyChangeTo2gVoiceEnabled &&
            technologyChangeTo2gPeriodicVoiceEnabled

    fun withPeriodicVoiceRepeat(kind: PeriodicVoiceRepeat, enabled: Boolean): AudioVolumeSettings {
        return when (kind) {
            PeriodicVoiceRepeat.NO_SIGNAL -> copy(noSignalPeriodicVoiceEnabled = enabled)
            PeriodicVoiceRepeat.LIMITED_SERVICE -> copy(limitedServicePeriodicVoiceEnabled = enabled)
            PeriodicVoiceRepeat.SIGNAL_LOW -> copy(tier5PeriodicVoiceEnabled = enabled)
            PeriodicVoiceRepeat.G2_CAMPED -> copy(technologyChangeTo2gPeriodicVoiceEnabled = enabled)
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
            limitedServiceTwoToneSpreadPercent = limitedServiceTwoToneSpreadPercent.coerceIn(
                MIN_LIMITED_SERVICE_TWO_TONE_SPREAD_PERCENT,
                MAX_LIMITED_SERVICE_TWO_TONE_SPREAD_PERCENT
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
            limitedServiceVoiceVolume = limitedServiceVoiceVolume.coerceIn(MIN_VOLUME, MAX_VOLUME),
            voiceSpeechRate = snapVoiceSpeechRate(voiceSpeechRate)
        )
    }

    companion object {
        const val DEFAULT_VOLUME = 0.5f
        const val DEFAULT_PING_CLICK_VOLUME = 0.45f
        const val DEFAULT_TIER5_ANNOUNCER_VOLUME = 0.45f
        const val DEFAULT_NO_SIGNAL_VOICE_VOLUME = 0.45f
        const val DEFAULT_LIMITED_SERVICE_TONE_VOLUME = 0.45f
        const val DEFAULT_LIMITED_SERVICE_VOICE_VOLUME = 0.55f
        const val MIN_VOLUME = 0f
        const val MAX_VOLUME = 1f
        const val DEFAULT_MASTER_VOICE_ANNOUNCEMENTS_ENABLED = true
        const val DEFAULT_CELL_CHANGE_VOICE_ENABLED = true
        const val DEFAULT_CELL_CHANGE_SPEAK_BAND_ENABLED = true
        const val DEFAULT_VOICE_ANNOUNCEMENT_ENABLED = true
        const val DEFAULT_PERIODIC_VOICE_ENABLED = true
        const val DEFAULT_NO_SIGNAL_VIBRATION_ENABLED = true
        const val DEFAULT_SPEAK_OPERATOR_NAME_ENABLED = false
        const val DEFAULT_SPEAK_TECHNOLOGY_ENABLED = false
        val DEFAULT_VOICE_ANNOUNCER_CHOICE = VoiceAnnouncerChoice.SYSTEM_DEFAULT
        const val DEFAULT_VOICE_SPEECH_RATE = 1.2f
        const val MIN_VOICE_SPEECH_RATE = 0.6f
        const val MAX_VOICE_SPEECH_RATE = 5.0f
        const val VOICE_SPEECH_RATE_STEP = 0.1f

        fun snapVoiceSpeechRate(rate: Float): Float {
            val clamped = rate.coerceIn(MIN_VOICE_SPEECH_RATE, MAX_VOICE_SPEECH_RATE)
            val steps = ((clamped - MIN_VOICE_SPEECH_RATE) / VOICE_SPEECH_RATE_STEP).roundToInt()
            return (MIN_VOICE_SPEECH_RATE + steps * VOICE_SPEECH_RATE_STEP)
                .coerceIn(MIN_VOICE_SPEECH_RATE, MAX_VOICE_SPEECH_RATE)
        }
        val DEFAULT_CELL_CHANGE_PHRASES = VoicePhraseOptions(
            speakOperatorName = false,
            speakTechnology = false,
            speakBand = false
        )
        val DEFAULT_TECH_CHANGE_TO_2G_PHRASES = VoicePhraseOptions(
            speakOperatorName = false,
            speakTechnology = true,
            speakBand = false
        )
        val DEFAULT_TECH_CHANGE_TO_4G_PHRASES = VoicePhraseOptions(
            speakOperatorName = false,
            speakTechnology = true,
            speakBand = true
        )
        val DEFAULT_TECH_CHANGE_TO_5G_ENDC_PHRASES = VoicePhraseOptions(
            speakOperatorName = false,
            speakTechnology = false,
            speakBand = false
        )
        val DEFAULT_SIGNAL_LOW_PHRASES = VoicePhraseOptions(
            speakOperatorName = false,
            speakTechnology = true,
            speakBand = false
        )
        val DEFAULT_NO_SIGNAL_PHRASES = VoicePhraseOptions(
            speakOperatorName = false,
            speakTechnology = true,
            speakBand = true
        )
        val DEFAULT_LIMITED_SERVICE_PHRASES = VoicePhraseOptions(
            speakOperatorName = false,
            speakTechnology = false,
            speakBand = false
        )

        const val DEFAULT_SIGNAL_PULSE_DURATION_MS = 70
        const val DEFAULT_LEVEL_RANGE_BCD_PULSE_DURATION_MS = 60
        const val MIN_SIGNAL_PULSE_DURATION_MS = 10
        const val MAX_SIGNAL_PULSE_DURATION_MS = 5_000
        const val SIGNAL_PULSE_DURATION_STEP_MS = 10

        const val DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ = 720
        const val DEFAULT_NO_SIGNAL_TIER_PULSE_FREQUENCY_HZ = 910
        const val DEFAULT_LIMITED_SERVICE_TIER_PULSE_FREQUENCY_HZ = 880
        const val DEFAULT_LEVEL_RANGE_BCD_PULSE_FREQUENCY_HZ = 890
        const val DEFAULT_VERY_STRONG_TIER_PULSE_FREQUENCY_HZ = 4_140
        const val DEFAULT_G2_STRONG_TIER_PULSE_FREQUENCY_HZ = 740
        const val DEFAULT_G2_WEAK_TIER_PULSE_FREQUENCY_HZ = 740
        const val MIN_SIGNAL_PULSE_FREQUENCY_HZ = 400
        const val MAX_SIGNAL_PULSE_FREQUENCY_HZ = 5_000
        const val SIGNAL_PULSE_FREQUENCY_STEP_HZ = 10
        const val MIN_LIMITED_SERVICE_TWO_TONE_SPREAD_PERCENT = 5
        const val MAX_LIMITED_SERVICE_TWO_TONE_SPREAD_PERCENT = 30
        const val DEFAULT_LIMITED_SERVICE_TWO_TONE_SPREAD_PERCENT = 15

        fun twoToneHighFrequencyHz(lowHz: Int, spreadPercent: Int): Int {
            val clampedLow = lowHz.coerceIn(MIN_SIGNAL_PULSE_FREQUENCY_HZ, MAX_SIGNAL_PULSE_FREQUENCY_HZ)
            val clampedSpread = spreadPercent.coerceIn(
                MIN_LIMITED_SERVICE_TWO_TONE_SPREAD_PERCENT,
                MAX_LIMITED_SERVICE_TWO_TONE_SPREAD_PERCENT
            )
            return (clampedLow * (1.0 + clampedSpread / 100.0)).roundToInt()
                .coerceAtMost(MAX_SIGNAL_PULSE_FREQUENCY_HZ)
        }
        /** Used when migrating stored settings that coupled tier 1 to [signalPulseFrequencyHz]. */
        const val LEGACY_VERY_STRONG_FREQUENCY_MULTIPLIER = 1.25

        /** Gap between end of alert tone and start of TTS. */
        const val ALERT_VOICE_GAP_MS = 50L

        fun voiceDelayAfterAlertTone(toneDurationMs: Int): Duration {
            return (toneDurationMs.coerceAtLeast(0).toLong() + ALERT_VOICE_GAP_MS).milliseconds
        }
    }
}

/** Independent on/off for 30 s cycling voice reminders (entry VAs stay on the main toggle). */
enum class PeriodicVoiceRepeat {
    NO_SIGNAL,
    LIMITED_SERVICE,
    SIGNAL_LOW,
    G2_CAMPED
}
