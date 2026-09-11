package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.CellReselectBandNamingStyle
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.Rxss
import io.github.cloolalang.notspotdetector.model.SettingsCompatibility
import io.github.cloolalang.notspotdetector.model.CELL_CHANGE_RXSS_NUMBER
import io.github.cloolalang.notspotdetector.model.TechnologyChangeTarget
import io.github.cloolalang.notspotdetector.model.DEADZONE_TIER_NUMBER
import io.github.cloolalang.notspotdetector.model.G2_NO_SIGNAL_TIER_NUMBER
import io.github.cloolalang.notspotdetector.model.G2_STRONG_TIER_NUMBER
import io.github.cloolalang.notspotdetector.model.G2_WEAK_TIER_NUMBER
import io.github.cloolalang.notspotdetector.model.LIMITED_ALT_2G_TIER_NUMBER
import io.github.cloolalang.notspotdetector.model.levelRangeAbcdMaxPulseDurationMs
import io.github.cloolalang.notspotdetector.ui.theme.Sushi
import io.github.cloolalang.notspotdetector.model.levelRangeAbcdMinClickIntervalMs
import io.github.cloolalang.notspotdetector.model.LIMITED_SERVICE_TIER_NUMBER
import io.github.cloolalang.notspotdetector.model.NO_SIGNAL_TIER_NUMBER
import io.github.cloolalang.notspotdetector.model.SEARCHING_2G_TIER_NUMBER
import io.github.cloolalang.notspotdetector.model.SignalStrengthTier
import io.github.cloolalang.notspotdetector.model.RSRQ_POOR_TIER_NUMBER
import io.github.cloolalang.notspotdetector.model.VERY_STRONG_TIER_NUMBER
import kotlin.math.roundToInt

@Composable
fun PassiveSignalSettingsCard(
    settings: PassiveSignalSettings,
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    passiveMeasurementIntervalMs: Long,
    signalPulseDurationMs: Int,
    veryStrongTierPulseFrequencyHz: Int,
    g2StrongTierPulseFrequencyHz: Int,
    g2WeakTierPulseFrequencyHz: Int,
    onSettingsChange: (PassiveSignalSettings) -> Unit,
    onVeryStrongTierPulseFrequencyChange: (Int) -> Unit,
    onG2StrongTierPulseFrequencyChange: (Int) -> Unit,
    onG2WeakTierPulseFrequencyChange: (Int) -> Unit,
    onTechnologyChangeToneVolumeChange: (TechnologyChangeTarget, Float) -> Unit,
    onTechnologyChangeVoiceEnabledChange: (TechnologyChangeTarget, Boolean) -> Unit,
    onTechnologyChangeVoiceVolumeChange: (TechnologyChangeTarget, Float) -> Unit,
    onTier5AnnouncerEnabledChange: (Boolean) -> Unit,
    onTier5AnnouncerVolumeChange: (Float) -> Unit,
    onNoSignalTierPulseFrequencyChange: (Int) -> Unit,
    onNoSignalToneVolumeChange: (Float) -> Unit,
    onNoSignalVibrationEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceVolumeChange: (Float) -> Unit,
    onLimitedServiceTierPulseFrequencyChange: (Int) -> Unit,
    onLimitedServiceToneVolumeChange: (Float) -> Unit,
    onLimitedServiceVoiceEnabledChange: (Boolean) -> Unit,
    onLimitedServiceVoiceVolumeChange: (Float) -> Unit,
    onPreviewTechnologyChangeTone: (TechnologyChangeTarget) -> Unit,
    onPreviewTechnologyChangeVoice: (TechnologyChangeTarget) -> Unit,
    onPreviewTier5Announcer: () -> Unit,
    onPreviewNoSignalTone: () -> Unit,
    onPreviewNoSignalVoice: () -> Unit,
    onPreviewLimitedServiceTone: () -> Unit,
    onPreviewLimitedServiceVoice: () -> Unit,
    onCellChangeBellVolumeChange: (Float) -> Unit,
    onCellChangeVoiceEnabledChange: (Boolean) -> Unit,
    onCellChangeVoiceVolumeChange: (Float) -> Unit,
    onPreviewCellChangeBell: () -> Unit,
    onPreviewCellChangeVoice: () -> Unit,
    onCellChangeSpeakBandEnabledChange: (Boolean) -> Unit = {},
    onCellChangeBandNamingStyleChange: (CellReselectBandNamingStyle) -> Unit = {},
    onLowSignalClickVolumeChange: (Float) -> Unit,
    onSignalPulseDurationChange: (Int) -> Unit,
    onPreviewLowSignalClick: (frequencyHz: Int, pulseDurationMs: Int) -> Unit,
    onPreviewSignalPulse: (volume: Float, frequencyHz: Int, pulseDurationMs: Int) -> Unit,
    onLevelRangeBcdClickVolumeChange: (Float) -> Unit,
    onLevelRangeBcdPulseFrequencyChange: (Int) -> Unit,
    onPreviewLevelRangeBcdClick: (frequencyHz: Int, pulseDurationMs: Int) -> Unit,
    onPreviewRsrqWhiteNoise: () -> Unit,
    onSignalPulseFrequencyChange: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.passive_signal_settings_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Sushi,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                if (!previewEnabled) {
                    Text(
                        text = stringResource(R.string.audio_volume_test_disabled_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DeadzoneTierSettings(
                    settings = settings,
                    audioVolumes = audioVolumes,
                    previewEnabled = previewEnabled,
                    passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                    onSettingsChange = onSettingsChange,
                    onNoSignalTierPulseFrequencyChange = onNoSignalTierPulseFrequencyChange,
                    onNoSignalToneVolumeChange = onNoSignalToneVolumeChange,
                    onNoSignalVibrationEnabledChange = onNoSignalVibrationEnabledChange,
                    onNoSignalVoiceEnabledChange = onNoSignalVoiceEnabledChange,
                    onNoSignalVoiceVolumeChange = onNoSignalVoiceVolumeChange,
                    onPreviewNoSignalVoice = onPreviewNoSignalVoice,
                    onPreviewSignalPulse = onPreviewSignalPulse
                )

                RsrpTierSettings(
                    settings = settings,
                    audioVolumes = audioVolumes,
                    previewEnabled = previewEnabled,
                    signalPulseDurationMs = signalPulseDurationMs,
                    veryStrongTierPulseFrequencyHz = veryStrongTierPulseFrequencyHz,
                    passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                    onSettingsChange = onSettingsChange,
                    onVeryStrongTierPulseFrequencyChange = onVeryStrongTierPulseFrequencyChange,
                    onTier5AnnouncerEnabledChange = onTier5AnnouncerEnabledChange,
                    onTier5AnnouncerVolumeChange = onTier5AnnouncerVolumeChange,
                    onPreviewTier5Announcer = onPreviewTier5Announcer,
                    onLowSignalClickVolumeChange = onLowSignalClickVolumeChange,
                    onSignalPulseDurationChange = onSignalPulseDurationChange,
                    onPreviewLowSignalClick = onPreviewLowSignalClick,
                    onPreviewSignalPulse = onPreviewSignalPulse,
                    onLevelRangeBcdClickVolumeChange = onLevelRangeBcdClickVolumeChange,
                    onLevelRangeBcdPulseFrequencyChange = onLevelRangeBcdPulseFrequencyChange,
                    onPreviewLevelRangeBcdClick = onPreviewLevelRangeBcdClick,
                    onSignalPulseFrequencyChange = onSignalPulseFrequencyChange
                )

                G2TierSettings(
                    settings = settings,
                    audioVolumes = audioVolumes,
                    previewEnabled = previewEnabled,
                    g2StrongTierPulseFrequencyHz = g2StrongTierPulseFrequencyHz,
                    g2WeakTierPulseFrequencyHz = g2WeakTierPulseFrequencyHz,
                    passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                    onSettingsChange = onSettingsChange,
                    onG2StrongTierPulseFrequencyChange = onG2StrongTierPulseFrequencyChange,
                    onG2WeakTierPulseFrequencyChange = onG2WeakTierPulseFrequencyChange,
                    onTier5AnnouncerEnabledChange = onTier5AnnouncerEnabledChange,
                    onTier5AnnouncerVolumeChange = onTier5AnnouncerVolumeChange,
                    onPreviewTier5Announcer = onPreviewTier5Announcer,
                    onLowSignalClickVolumeChange = onLowSignalClickVolumeChange,
                    onPreviewLowSignalClick = onPreviewLowSignalClick
                )

                CellChangeTierSettings(
                    audioVolumes = audioVolumes,
                    previewEnabled = previewEnabled,
                    onCellChangeBellVolumeChange = onCellChangeBellVolumeChange,
                    onCellChangeVoiceEnabledChange = onCellChangeVoiceEnabledChange,
                    onCellChangeVoiceVolumeChange = onCellChangeVoiceVolumeChange,
                    onPreviewCellChangeBell = onPreviewCellChangeBell,
                    onPreviewCellChangeVoice = onPreviewCellChangeVoice,
                    onCellChangeSpeakBandEnabledChange = onCellChangeSpeakBandEnabledChange,
                    onCellChangeBandNamingStyleChange = onCellChangeBandNamingStyleChange
                )

                CampStateTierSettings(
                    settings = settings,
                    audioVolumes = audioVolumes,
                    previewEnabled = previewEnabled,
                    passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                    onSettingsChange = onSettingsChange,
                    onNoSignalTierPulseFrequencyChange = onNoSignalTierPulseFrequencyChange,
                    onNoSignalToneVolumeChange = onNoSignalToneVolumeChange,
                    onNoSignalVibrationEnabledChange = onNoSignalVibrationEnabledChange,
                    onNoSignalVoiceEnabledChange = onNoSignalVoiceEnabledChange,
                    onNoSignalVoiceVolumeChange = onNoSignalVoiceVolumeChange,
                    onLimitedServiceTierPulseFrequencyChange = onLimitedServiceTierPulseFrequencyChange,
                    onLimitedServiceToneVolumeChange = onLimitedServiceToneVolumeChange,
                    onLimitedServiceVoiceEnabledChange = onLimitedServiceVoiceEnabledChange,
                    onLimitedServiceVoiceVolumeChange = onLimitedServiceVoiceVolumeChange,
                    onPreviewNoSignalTone = onPreviewNoSignalTone,
                    onPreviewNoSignalVoice = onPreviewNoSignalVoice,
                    onPreviewLimitedServiceTone = onPreviewLimitedServiceTone,
                    onPreviewLimitedServiceVoice = onPreviewLimitedServiceVoice,
                    onLowSignalClickVolumeChange = onLowSignalClickVolumeChange,
                    onPreviewLowSignalClick = onPreviewLowSignalClick,
                    onPreviewSignalPulse = onPreviewSignalPulse
                )

                Text(
                    text = stringResource(R.string.passive_signal_rsrq_section),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                RsrqTierSettings(
                    settings = settings,
                    previewEnabled = previewEnabled,
                    passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                    onSettingsChange = onSettingsChange,
                    onPreviewRsrqWhiteNoise = onPreviewRsrqWhiteNoise
                )

                Text(
                    text = stringResource(R.string.passive_signal_technology_change_section),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                TechnologyChangeTierSettings(
                    audioVolumes = audioVolumes,
                    previewEnabled = previewEnabled,
                    onTechnologyChangeToneVolumeChange = onTechnologyChangeToneVolumeChange,
                    onTechnologyChangeVoiceEnabledChange = onTechnologyChangeVoiceEnabledChange,
                    onTechnologyChangeVoiceVolumeChange = onTechnologyChangeVoiceVolumeChange,
                    onPreviewTechnologyChangeTone = onPreviewTechnologyChangeTone,
                    onPreviewTechnologyChangeVoice = onPreviewTechnologyChangeVoice
                )

                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.passive_signal_settings_reset))
                }
            }
        }
    }
}

/**
 * Standard RXSS threshold-panel control order:
 * sound toggle → range → volume → duration → interval → frequency → voice.
 */
@Composable
private fun RxssSectionControlsOrdered(
    accentColor: Color,
    soundToggle: @Composable () -> Unit,
    rangeControls: @Composable () -> Unit = {},
    volumeControls: @Composable () -> Unit = {},
    durationControls: @Composable () -> Unit = {},
    intervalControls: @Composable () -> Unit = {},
    frequencyControls: @Composable () -> Unit = {},
    voiceControls: @Composable () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        soundToggle()
        rangeControls()
        volumeControls()
        durationControls()
        intervalControls()
        frequencyControls()
        voiceControls()
    }
}

@Composable
private fun TierSoundEnabledOption(
    tierNumber: Int,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = enabled,
            onCheckedChange = onEnabledChange
        )
        Text(
            text = rxssSoundEnabledLabel(tierNumber),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TierPulseFrequencySlider(
    label: String,
    frequencyHz: Int,
    accentColor: Color,
    onFrequencyChange: (Int) -> Unit
) {
    val minHz = AudioVolumeSettings.MIN_SIGNAL_PULSE_FREQUENCY_HZ
    val maxHz = AudioVolumeSettings.MAX_SIGNAL_PULSE_FREQUENCY_HZ
    val stepHz = AudioVolumeSettings.SIGNAL_PULSE_FREQUENCY_STEP_HZ
    val minStep = minHz / stepHz
    val maxStep = maxHz / stepHz
    val coercedHz = frequencyHz.coerceIn(minHz, maxHz)
    val step = (coercedHz / stepHz).coerceIn(minStep, maxStep)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = accentColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.audio_signal_pulse_frequency_value, coercedHz),
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            value = step.toFloat(),
            onValueChange = { onFrequencyChange(it.roundToInt() * stepHz) },
            valueRange = minStep.toFloat()..maxStep.toFloat(),
            steps = (maxStep - minStep - 1).coerceAtLeast(0),
            colors = tierSliderColors(accentColor)
        )
    }
}

@Composable
private fun TierPulseDurationSlider(
    label: String,
    durationMs: Int,
    accentColor: Color,
    onDurationChange: (Int) -> Unit
) {
    val minMs = AudioVolumeSettings.MIN_SIGNAL_PULSE_DURATION_MS
    val maxMs = AudioVolumeSettings.MAX_SIGNAL_PULSE_DURATION_MS
    val stepMs = 10
    val steps = ((maxMs - minMs) / stepMs) - 1
    val coercedMs = durationMs.coerceIn(minMs, maxMs)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = accentColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.audio_signal_pulse_duration_value, coercedMs),
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = stringResource(R.string.passive_signal_tier_pulse_duration_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = coercedMs.toFloat(),
            onValueChange = { raw ->
                val snapped = minMs + (((raw - minMs) / stepMs).roundToInt() * stepMs)
                onDurationChange(snapped.coerceIn(minMs, maxMs))
            },
            valueRange = minMs.toFloat()..maxMs.toFloat(),
            steps = steps.coerceAtLeast(0),
            colors = tierSliderColors(accentColor)
        )
    }
}

@Composable
private fun TierClickSpeedSlider(
    label: String,
    intervalMs: Int,
    signalPulseDurationMs: Int,
    passiveMeasurementIntervalMs: Long,
    accentColor: Color,
    onIntervalChange: (Int) -> Unit,
    maxIntervalMs: Int = PassiveSignalSettings.MAX_TIER_CLICK_INTERVAL_MS
) {
    val minUiMs = SettingsCompatibility.minTierClickIntervalUiMs(signalPulseDurationMs)
    val uiStepSize = TierSliderSupport.tierClickSliderStepSizeMs(minUiMs, maxIntervalMs)
    val maxStep = TierSliderSupport.tierClickSliderMaxIndex(minUiMs, uiStepSize, maxIntervalMs)
    val coercedIntervalMs = intervalMs.coerceIn(minUiMs, maxIntervalMs)
    val step = TierSliderSupport.tierClickSliderIndexFromMs(
        coercedIntervalMs,
        minUiMs,
        uiStepSize,
        maxIntervalMs
    )
    val passiveOnlyEffectiveMs = SettingsCompatibility.resolveTierClickIntervalMs(
        configuredMs = coercedIntervalMs.toLong(),
        signalPulseDurationMs = signalPulseDurationMs,
        isPassiveOnlySession = true
    )
    val showPulseDurationNote = intervalMs < minUiMs
    val showMeasurementWarning = SettingsCompatibility.isTierIntervalMuchSlowerThanMeasurement(
        tierIntervalMs = coercedIntervalMs,
        measurementIntervalMs = passiveMeasurementIntervalMs
    )
    if (!TierSliderSupport.isValidClickIndexRange(0, maxStep)) return

    val sliderSteps = TierSliderSupport.tierClickIndexSteps(0, maxStep)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = accentColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.passive_signal_tier_click_value, coercedIntervalMs),
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                fontWeight = FontWeight.Medium
            )
        }
        if (showPulseDurationNote) {
            Text(
                text = stringResource(
                    R.string.passive_signal_tier_click_effective,
                    passiveOnlyEffectiveMs
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        if (showMeasurementWarning) {
            Text(
                text = stringResource(
                    R.string.passive_signal_tier_click_measurement_warning,
                    passiveMeasurementIntervalMs / 1_000L
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        Slider(
            value = step.toFloat().coerceIn(0f, maxStep.toFloat()),
            onValueChange = {
                val selectedMs = TierSliderSupport.tierClickSliderMsFromIndex(
                    index = it.roundToInt(),
                    minUiMs = minUiMs,
                    uiStepSize = uiStepSize,
                    maxIntervalMs = maxIntervalMs
                )
                onIntervalChange(selectedMs)
            },
            valueRange = 0f..maxStep.toFloat(),
            steps = sliderSteps,
            colors = tierSliderColors(accentColor)
        )
    }
}

@Composable
private fun RsrpBandTierIntervalControls(
    tier: SignalStrengthTier,
    settings: PassiveSignalSettings,
    gap: Int,
    passiveMeasurementIntervalMs: Long,
    onSettingsChange: (PassiveSignalSettings) -> Unit
) {
    when (tier) {
        SignalStrengthTier.GOOD -> {
            val accent = SignalTierColors.forStrengthTier(tier)
            val tierNumber = tier.displayNumber
            RxssSectionControlsOrdered(
                accentColor = accent,
                soundToggle = {
                    TierSoundEnabledOption(
                        tierNumber = tierNumber,
                        enabled = settings.goodTierSoundEnabled,
                        onEnabledChange = { onSettingsChange(settings.copy(goodTierSoundEnabled = it)) }
                    )
                },
                rangeControls = {
                    BoundarySlider(
                        label = rxssBoundaryAboveLabel(tierNumber),
                        rangeLabel = stringResource(
                            R.string.passive_signal_rsrp_band_range,
                            settings.goodRsrpMinDbm + gap,
                            settings.mildRsrpMinDbm
                        ),
                        value = settings.goodRsrpMinDbm,
                        valueRange = (settings.fairRsrpMinDbm + gap)..(settings.mildRsrpMinDbm - gap),
                        accentColor = accent,
                        onValueChange = { onSettingsChange(settings.copy(goodRsrpMinDbm = it)) }
                    )
                },
                durationControls = {
                    TierPulseDurationSlider(
                        label = stringResource(R.string.passive_signal_tier_pulse_duration, tierNumber),
                        durationMs = settings.goodTierPulseDurationMs,
                        accentColor = accent,
                        onDurationChange = { onSettingsChange(settings.copy(goodTierPulseDurationMs = it)) }
                    )
                },
                intervalControls = {
                    TierClickSpeedSlider(
                        label = stringResource(R.string.passive_signal_tier_click_interval, tierNumber),
                        intervalMs = settings.goodTierClickIntervalMs,
                        signalPulseDurationMs = settings.goodTierPulseDurationMs,
                        passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                        accentColor = accent,
                        onIntervalChange = { onSettingsChange(settings.copy(goodTierClickIntervalMs = it)) }
                    )
                }
            )
        }
        SignalStrengthTier.FAIR -> {
            val accent = SignalTierColors.forStrengthTier(tier)
            val tierNumber = tier.displayNumber
            RxssSectionControlsOrdered(
                accentColor = accent,
                soundToggle = {
                    TierSoundEnabledOption(
                        tierNumber = tierNumber,
                        enabled = settings.fairTierSoundEnabled,
                        onEnabledChange = { onSettingsChange(settings.copy(fairTierSoundEnabled = it)) }
                    )
                },
                rangeControls = {
                    BoundarySlider(
                        label = rxssBoundaryAboveLabel(tierNumber),
                        rangeLabel = stringResource(
                            R.string.passive_signal_rsrp_band_range,
                            settings.fairRsrpMinDbm + gap,
                            settings.goodRsrpMinDbm
                        ),
                        value = settings.fairRsrpMinDbm,
                        valueRange = (settings.poorRsrpMinDbm + gap)..(settings.goodRsrpMinDbm - gap),
                        accentColor = accent,
                        onValueChange = { onSettingsChange(settings.copy(fairRsrpMinDbm = it)) }
                    )
                },
                durationControls = {
                    TierPulseDurationSlider(
                        label = stringResource(R.string.passive_signal_tier_pulse_duration, tierNumber),
                        durationMs = settings.fairTierPulseDurationMs,
                        accentColor = accent,
                        onDurationChange = { onSettingsChange(settings.copy(fairTierPulseDurationMs = it)) }
                    )
                },
                intervalControls = {
                    TierClickSpeedSlider(
                        label = stringResource(R.string.passive_signal_tier_click_interval, tierNumber),
                        intervalMs = settings.fairTierClickIntervalMs,
                        signalPulseDurationMs = settings.fairTierPulseDurationMs,
                        passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                        accentColor = accent,
                        onIntervalChange = { onSettingsChange(settings.copy(fairTierClickIntervalMs = it)) }
                    )
                }
            )
        }
        else -> Unit
    }
}

@Composable
private fun RsrpTierSettings(
    settings: PassiveSignalSettings,
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    signalPulseDurationMs: Int,
    veryStrongTierPulseFrequencyHz: Int,
    passiveMeasurementIntervalMs: Long,
    onSettingsChange: (PassiveSignalSettings) -> Unit,
    onVeryStrongTierPulseFrequencyChange: (Int) -> Unit,
    onTier5AnnouncerEnabledChange: (Boolean) -> Unit,
    onTier5AnnouncerVolumeChange: (Float) -> Unit,
    onPreviewTier5Announcer: () -> Unit,
    onLowSignalClickVolumeChange: (Float) -> Unit,
    onSignalPulseDurationChange: (Int) -> Unit,
    onPreviewLowSignalClick: (frequencyHz: Int, pulseDurationMs: Int) -> Unit,
    onPreviewSignalPulse: (volume: Float, frequencyHz: Int, pulseDurationMs: Int) -> Unit,
    onLevelRangeBcdClickVolumeChange: (Float) -> Unit,
    onLevelRangeBcdPulseFrequencyChange: (Int) -> Unit,
    onPreviewLevelRangeBcdClick: (frequencyHz: Int, pulseDurationMs: Int) -> Unit,
    onSignalPulseFrequencyChange: (Int) -> Unit
) {
    val levelRangeBcdAccent = SignalTierColors.forStrengthTier(SignalStrengthTier.MILD)
    val gap = PassiveSignalSettings.MIN_RSRP_BAND_GAP_DBM
    val maxMild = settings.veryStrongRsrpMinDbm - gap
    val rxss1Accent = SignalTierColors.forTierNumber(VERY_STRONG_TIER_NUMBER)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TierSettingSection(
            tierNumber = VERY_STRONG_TIER_NUMBER,
            accentColor = rxss1Accent
        ) {
            RxssSectionControlsOrdered(
                accentColor = rxss1Accent,
               
                soundToggle = {
                    TierSoundEnabledOption(
                        tierNumber = VERY_STRONG_TIER_NUMBER,
                        enabled = settings.veryStrongTierSoundEnabled,
                        onEnabledChange = { onSettingsChange(settings.copy(veryStrongTierSoundEnabled = it)) }
                    )
                },
                rangeControls = {
                    BoundarySlider(
                        label = stringResource(R.string.passive_signal_boundary_tier_above, VERY_STRONG_TIER_NUMBER),
                        rangeLabel = stringResource(
                            R.string.passive_signal_rsrp_band_range_open,
                            settings.veryStrongRsrpMinDbm + gap,
                            PassiveSignalSettings.MAX_RSRP_DBM
                        ),
                        value = settings.veryStrongRsrpMinDbm,
                        valueRange = (settings.mildRsrpMinDbm + gap)..PassiveSignalSettings.MAX_VERY_STRONG_RSRP_DBM,
                        accentColor = rxss1Accent,
                        onValueChange = { onSettingsChange(settings.copy(veryStrongRsrpMinDbm = it)) }
                    )
                },
                volumeControls = {
                    RxssVolumeSlider(
                        label = stringResource(R.string.passive_signal_rxss1_click_volume),
                        value = audioVolumes.lowSignalClickVolume,
                        onValueChange = onLowSignalClickVolumeChange,
                        previewEnabled = previewEnabled,
                        onPreview = {
                            onPreviewSignalPulse(
                                audioVolumes.lowSignalClickVolume,
                                veryStrongTierPulseFrequencyHz,
                                signalPulseDurationMs
                            )
                        },
                        previewRepeatIntervalMs = tierPreviewRepeatIntervalMs(
                            settings.veryStrongTierClickIntervalMs,
                            signalPulseDurationMs
                        ),
                        accentColor = rxss1Accent
                    )
                },
                durationControls = {
                    TierPulseDurationSlider(
                        label = stringResource(R.string.passive_signal_tier_pulse_duration, VERY_STRONG_TIER_NUMBER),
                        durationMs = signalPulseDurationMs,
                        accentColor = rxss1Accent,
                        onDurationChange = onSignalPulseDurationChange
                    )
                },
                intervalControls = {
                    TierClickSpeedSlider(
                        label = stringResource(R.string.passive_signal_tier_click_interval, VERY_STRONG_TIER_NUMBER),
                        intervalMs = settings.veryStrongTierClickIntervalMs,
                        signalPulseDurationMs = signalPulseDurationMs,
                        passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                        accentColor = rxss1Accent,
                        onIntervalChange = { onSettingsChange(settings.copy(veryStrongTierClickIntervalMs = it)) }
                    )
                },
                frequencyControls = {
                    TierPulseFrequencySlider(
                        label = stringResource(R.string.passive_signal_tier_pulse_frequency, VERY_STRONG_TIER_NUMBER),
                        frequencyHz = veryStrongTierPulseFrequencyHz,
                        accentColor = rxss1Accent,
                        onFrequencyChange = onVeryStrongTierPulseFrequencyChange
                    )
                }
            )
        }

        LevelRangeBcdSharedSoundControls(
            settings = settings,
            audioVolumes = audioVolumes,
            previewEnabled = previewEnabled,
            accentColor = levelRangeBcdAccent,
            onLevelRangeBcdClickVolumeChange = onLevelRangeBcdClickVolumeChange,
            onLevelRangeBcdPulseFrequencyChange = onLevelRangeBcdPulseFrequencyChange,
            onPreviewLevelRangeBcdClick = onPreviewLevelRangeBcdClick
        )

        TierSettingSection(
            tierNumber = SignalStrengthTier.MILD.displayNumber,
            accentColor = SignalTierColors.forStrengthTier(SignalStrengthTier.MILD)
        ) {
            val accent = SignalTierColors.forStrengthTier(SignalStrengthTier.MILD)
            val tierNumber = SignalStrengthTier.MILD.displayNumber
            RxssSectionControlsOrdered(
                accentColor = accent,
                soundToggle = {
                    TierSoundEnabledOption(
                        tierNumber = tierNumber,
                        enabled = settings.mildTierSoundEnabled,
                        onEnabledChange = { onSettingsChange(settings.copy(mildTierSoundEnabled = it)) }
                    )
                },
                rangeControls = {
                    BoundarySlider(
                        label = rxssBoundaryAboveLabel(tierNumber),
                        rangeLabel = stringResource(
                            R.string.passive_signal_rsrp_band_range,
                            settings.mildRsrpMinDbm + gap,
                            settings.veryStrongRsrpMinDbm
                        ),
                        value = settings.mildRsrpMinDbm,
                        valueRange = (settings.goodRsrpMinDbm + gap)..maxMild,
                        accentColor = accent,
                        onValueChange = { onSettingsChange(settings.copy(mildRsrpMinDbm = it)) }
                    )
                },
                durationControls = {
                    TierPulseDurationSlider(
                        label = stringResource(R.string.passive_signal_tier_pulse_duration, tierNumber),
                        durationMs = settings.mildTierPulseDurationMs,
                        accentColor = accent,
                        onDurationChange = { onSettingsChange(settings.copy(mildTierPulseDurationMs = it)) }
                    )
                },
                intervalControls = {
                    TierClickSpeedSlider(
                        label = stringResource(R.string.passive_signal_tier_click_interval, tierNumber),
                        intervalMs = settings.mildTierClickIntervalMs,
                        signalPulseDurationMs = settings.mildTierPulseDurationMs,
                        passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                        accentColor = accent,
                        onIntervalChange = { onSettingsChange(settings.copy(mildTierClickIntervalMs = it)) }
                    )
                }
            )
        }

        TierSettingSection(
            tierNumber = SignalStrengthTier.GOOD.displayNumber,
            accentColor = SignalTierColors.forStrengthTier(SignalStrengthTier.GOOD)
        ) {
            RsrpBandTierIntervalControls(
                tier = SignalStrengthTier.GOOD,
                settings = settings,
                gap = gap,
                passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                onSettingsChange = onSettingsChange
            )
        }

        TierSettingSection(
            tierNumber = SignalStrengthTier.FAIR.displayNumber,
            accentColor = SignalTierColors.forStrengthTier(SignalStrengthTier.FAIR)
        ) {
            RsrpBandTierIntervalControls(
                tier = SignalStrengthTier.FAIR,
                settings = settings,
                gap = gap,
                passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                onSettingsChange = onSettingsChange
            )
        }

        TierSettingSection(
            tierNumber = SignalStrengthTier.POOR.displayNumber,
            accentColor = SignalTierColors.forStrengthTier(SignalStrengthTier.POOR)
        ) {
            val accent = SignalTierColors.forStrengthTier(SignalStrengthTier.POOR)
            val tierNumber = SignalStrengthTier.POOR.displayNumber
            RxssSectionControlsOrdered(
                accentColor = accent,
                soundToggle = {
                    TierSoundEnabledOption(
                        tierNumber = tierNumber,
                        enabled = settings.poorTierSoundEnabled,
                        onEnabledChange = { onSettingsChange(settings.copy(poorTierSoundEnabled = it)) }
                    )
                },
                rangeControls = {
                    BoundarySlider(
                        label = rxssBoundaryAboveLabel(tierNumber),
                        rangeLabel = stringResource(
                            R.string.passive_signal_rsrp_band_range,
                            settings.poorRsrpMinDbm + gap,
                            settings.fairRsrpMinDbm
                        ),
                        value = settings.poorRsrpMinDbm,
                        valueRange = (PassiveSignalSettings.DEFAULT_NO_SIGNAL_RSRP_DBM + gap)..
                            (settings.fairRsrpMinDbm - gap),
                        accentColor = accent,
                        onValueChange = { onSettingsChange(settings.copy(poorRsrpMinDbm = it)) }
                    )
                },
                durationControls = {
                    TierPulseDurationSlider(
                        label = stringResource(R.string.passive_signal_tier_pulse_duration, tierNumber),
                        durationMs = settings.poorTierPulseDurationMs,
                        accentColor = accent,
                        onDurationChange = { onSettingsChange(settings.copy(poorTierPulseDurationMs = it)) }
                    )
                },
                intervalControls = {
                    TierClickSpeedSlider(
                        label = stringResource(R.string.passive_signal_tier_click_interval, tierNumber),
                        intervalMs = settings.poorTierClickIntervalMs,
                        signalPulseDurationMs = settings.poorTierPulseDurationMs,
                        passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                        accentColor = accent,
                        onIntervalChange = { onSettingsChange(settings.copy(poorTierClickIntervalMs = it)) }
                    )
                }
            )
        }

        TierSettingSection(
            tierNumber = SignalStrengthTier.CRITICAL.displayNumber,
            accentColor = SignalTierColors.forStrengthTier(SignalStrengthTier.CRITICAL)
        ) {
            val accent = SignalTierColors.forStrengthTier(SignalStrengthTier.CRITICAL)
            val tierNumber = SignalStrengthTier.CRITICAL.displayNumber
            RxssSectionControlsOrdered(
                accentColor = accent,
                soundToggle = {
                    TierSoundEnabledOption(
                        tierNumber = tierNumber,
                        enabled = settings.criticalTierSoundEnabled,
                        onEnabledChange = { onSettingsChange(settings.copy(criticalTierSoundEnabled = it)) }
                    )
                },
                rangeControls = {
                    Text(
                        text = stringResource(R.string.passive_signal_rxss6_rsrp_threshold),
                        style = MaterialTheme.typography.bodySmall,
                        color = accent
                    )
                },
                volumeControls = {
                    RxssVolumeSlider(
                        label = stringResource(R.string.passive_signal_rxss1_click_volume),
                        value = audioVolumes.lowSignalClickVolume,
                        onValueChange = onLowSignalClickVolumeChange,
                        previewEnabled = previewEnabled,
                        onPreview = {
                            onPreviewSignalPulse(
                                audioVolumes.lowSignalClickVolume,
                                audioVolumes.signalPulseFrequencyHz,
                                settings.criticalTierPulseDurationMs
                            )
                        },
                        previewRepeatIntervalMs = tierPreviewRepeatIntervalMs(
                            settings.criticalTierClickIntervalMs,
                            settings.criticalTierPulseDurationMs
                        ),
                        accentColor = accent
                    )
                },
                durationControls = {
                    TierPulseDurationSlider(
                        label = stringResource(R.string.passive_signal_tier_pulse_duration, tierNumber),
                        durationMs = settings.criticalTierPulseDurationMs,
                        accentColor = accent,
                        onDurationChange = { onSettingsChange(settings.copy(criticalTierPulseDurationMs = it)) }
                    )
                },
                intervalControls = {
                    TierClickSpeedSlider(
                        label = stringResource(R.string.passive_signal_tier_click_interval, tierNumber),
                        intervalMs = settings.criticalTierClickIntervalMs,
                        signalPulseDurationMs = settings.criticalTierPulseDurationMs,
                        passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                        accentColor = accent,
                        onIntervalChange = { onSettingsChange(settings.copy(criticalTierClickIntervalMs = it)) }
                    )
                },
                frequencyControls = {
                    TierPulseFrequencySlider(
                        label = stringResource(R.string.passive_signal_tier_pulse_frequency, tierNumber),
                        frequencyHz = audioVolumes.signalPulseFrequencyHz,
                        accentColor = accent,
                        onFrequencyChange = onSignalPulseFrequencyChange
                    )
                },
                voiceControls = {
                    Tier5SignalLowVoiceControls(
                        audioVolumes = audioVolumes,
                        previewEnabled = previewEnabled,
                        accentColor = accent,
                        onTier5AnnouncerEnabledChange = onTier5AnnouncerEnabledChange,
                        onTier5AnnouncerVolumeChange = onTier5AnnouncerVolumeChange,
                        onPreviewTier5Announcer = onPreviewTier5Announcer,
                        enabledTitle = stringResource(R.string.passive_signal_rxss6_voice_enabled),
                        volumeLabel = stringResource(R.string.passive_signal_rxss6_voice_volume)
                    )
                }
            )
        }

    }
}

@Composable
private fun CellChangeTierSettings(
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    onCellChangeBellVolumeChange: (Float) -> Unit,
    onCellChangeVoiceEnabledChange: (Boolean) -> Unit,
    onCellChangeVoiceVolumeChange: (Float) -> Unit,
    onPreviewCellChangeBell: () -> Unit,
    onPreviewCellChangeVoice: () -> Unit,
    onCellChangeSpeakBandEnabledChange: (Boolean) -> Unit = {},
    onCellChangeBandNamingStyleChange: (CellReselectBandNamingStyle) -> Unit = {}
) {
    val accent = SignalTierColors.forRxssNumber(CELL_CHANGE_RXSS_NUMBER)
    TierSettingSection(tierNumber = CELL_CHANGE_RXSS_NUMBER, accentColor = accent) {
        CellChangeAlertControls(
            audioVolumes = audioVolumes,
            previewEnabled = previewEnabled,
            accentColor = accent,
            onCellChangeBellVolumeChange = onCellChangeBellVolumeChange,
            onCellChangeVoiceEnabledChange = onCellChangeVoiceEnabledChange,
            onCellChangeVoiceVolumeChange = onCellChangeVoiceVolumeChange,
            onPreviewCellChangeBell = onPreviewCellChangeBell,
            onPreviewCellChangeVoice = onPreviewCellChangeVoice,
            onCellChangeSpeakBandEnabledChange = onCellChangeSpeakBandEnabledChange,
            onCellChangeBandNamingStyleChange = onCellChangeBandNamingStyleChange
        )
    }
}

@Composable
private fun TechnologyChangeTierSettings(
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    onTechnologyChangeToneVolumeChange: (TechnologyChangeTarget, Float) -> Unit,
    onTechnologyChangeVoiceEnabledChange: (TechnologyChangeTarget, Boolean) -> Unit,
    onTechnologyChangeVoiceVolumeChange: (TechnologyChangeTarget, Float) -> Unit,
    onPreviewTechnologyChangeTone: (TechnologyChangeTarget) -> Unit,
    onPreviewTechnologyChangeVoice: (TechnologyChangeTarget) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TechnologyChangeTarget.thresholdPanelOrder.forEach { target ->
            val accent = SignalTierColors.forRxssNumber(target.rxssNumber)
            TierSettingSection(tierNumber = target.rxssNumber, accentColor = accent) {
                TechnologyChangeAlertControls(
                    target = target,
                    audioVolumes = audioVolumes,
                    previewEnabled = previewEnabled,
                    accentColor = accent,
                    voiceEnabledTitle = technologyChangeVoiceEnabledTitle(target),
                    voiceVolumeLabel = stringResource(R.string.audio_volume_technology_change_voice),
                    onToneVolumeChange = { onTechnologyChangeToneVolumeChange(target, it) },
                    onVoiceEnabledChange = { onTechnologyChangeVoiceEnabledChange(target, it) },
                    onVoiceVolumeChange = { onTechnologyChangeVoiceVolumeChange(target, it) },
                    onPreviewTone = { onPreviewTechnologyChangeTone(target) },
                    onPreviewVoice = { onPreviewTechnologyChangeVoice(target) }
                )
            }
        }
    }
}

@Composable
private fun technologyChangeVoiceEnabledTitle(target: TechnologyChangeTarget): String {
    return when (target) {
        TechnologyChangeTarget.TO_2G -> stringResource(R.string.passive_signal_rxss28_voice_enabled)
        TechnologyChangeTarget.TO_4G -> stringResource(R.string.passive_signal_rxss29_voice_enabled)
        TechnologyChangeTarget.TO_5G_ENDC -> stringResource(R.string.passive_signal_rxss30_voice_enabled)
    }
}

@Composable
private fun G2TierSettings(
    settings: PassiveSignalSettings,
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    g2StrongTierPulseFrequencyHz: Int,
    g2WeakTierPulseFrequencyHz: Int,
    passiveMeasurementIntervalMs: Long,
    onSettingsChange: (PassiveSignalSettings) -> Unit,
    onG2StrongTierPulseFrequencyChange: (Int) -> Unit,
    onG2WeakTierPulseFrequencyChange: (Int) -> Unit,
    onTier5AnnouncerEnabledChange: (Boolean) -> Unit,
    onTier5AnnouncerVolumeChange: (Float) -> Unit,
    onPreviewTier5Announcer: () -> Unit,
    onLowSignalClickVolumeChange: (Float) -> Unit,
    onPreviewLowSignalClick: (frequencyHz: Int, pulseDurationMs: Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TierSettingSection(
            tierNumber = G2_STRONG_TIER_NUMBER,
            accentColor = SignalTierColors.forTierNumber(G2_STRONG_TIER_NUMBER)
        ) {
            val accent = SignalTierColors.forTierNumber(G2_STRONG_TIER_NUMBER)
            RxssSectionControlsOrdered(
                accentColor = accent,
                soundToggle = {
                    TierSoundEnabledOption(
                        tierNumber = G2_STRONG_TIER_NUMBER,
                        enabled = settings.g2StrongTierSoundEnabled,
                        onEnabledChange = { onSettingsChange(settings.copy(g2StrongTierSoundEnabled = it)) }
                    )
                },
                rangeControls = {
                    Text(
                        text = stringResource(
                            R.string.passive_signal_g2_tier7_threshold,
                            PassiveSignalSettings.G2_TIER_RX_LEVEL_SPLIT_DBM
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = accent
                    )
                },
                volumeControls = {
                    RxssVolumeSlider(
                        label = stringResource(R.string.passive_signal_g2_tier_click_volume),
                        value = audioVolumes.lowSignalClickVolume,
                        onValueChange = onLowSignalClickVolumeChange,
                        previewEnabled = previewEnabled,
                        onPreview = {
                            onPreviewLowSignalClick(
                                g2StrongTierPulseFrequencyHz,
                                settings.g2StrongTierPulseDurationMs
                            )
                        },
                        previewRepeatIntervalMs = tierPreviewRepeatIntervalMs(
                            settings.g2StrongTierClickIntervalMs,
                            settings.g2StrongTierPulseDurationMs
                        ),
                        accentColor = accent
                    )
                },
                durationControls = {
                    TierPulseDurationSlider(
                        label = stringResource(R.string.passive_signal_tier_pulse_duration, G2_STRONG_TIER_NUMBER),
                        durationMs = settings.g2StrongTierPulseDurationMs,
                        accentColor = accent,
                        onDurationChange = { onSettingsChange(settings.copy(g2StrongTierPulseDurationMs = it)) }
                    )
                },
                intervalControls = {
                    TierClickSpeedSlider(
                        label = stringResource(R.string.passive_signal_tier_click_interval, G2_STRONG_TIER_NUMBER),
                        intervalMs = settings.g2StrongTierClickIntervalMs,
                        signalPulseDurationMs = settings.g2StrongTierPulseDurationMs,
                        passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                        accentColor = accent,
                        onIntervalChange = { onSettingsChange(settings.copy(g2StrongTierClickIntervalMs = it)) }
                    )
                },
                frequencyControls = {
                    TierPulseFrequencySlider(
                        label = stringResource(R.string.passive_signal_tier_pulse_frequency, G2_STRONG_TIER_NUMBER),
                        frequencyHz = g2StrongTierPulseFrequencyHz,
                        accentColor = accent,
                        onFrequencyChange = onG2StrongTierPulseFrequencyChange
                    )
                }
            )
        }

        TierSettingSection(
            tierNumber = G2_WEAK_TIER_NUMBER,
            accentColor = SignalTierColors.forTierNumber(G2_WEAK_TIER_NUMBER)
        ) {
            val accent = SignalTierColors.forTierNumber(G2_WEAK_TIER_NUMBER)
            RxssSectionControlsOrdered(
                accentColor = accent,
               
                soundToggle = {
                    TierSoundEnabledOption(
                        tierNumber = G2_WEAK_TIER_NUMBER,
                        enabled = settings.g2WeakTierSoundEnabled,
                        onEnabledChange = { onSettingsChange(settings.copy(g2WeakTierSoundEnabled = it)) }
                    )
                },
                rangeControls = {
                    Text(
                        text = stringResource(
                            R.string.passive_signal_g2_tier8_threshold,
                            PassiveSignalSettings.G2_TIER_RX_LEVEL_SPLIT_DBM
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = accent
                    )
                },
                volumeControls = {
                    RxssVolumeSlider(
                        label = stringResource(R.string.passive_signal_g2_tier_click_volume),
                        value = audioVolumes.lowSignalClickVolume,
                        onValueChange = onLowSignalClickVolumeChange,
                        previewEnabled = previewEnabled,
                        onPreview = {
                            onPreviewLowSignalClick(
                                g2WeakTierPulseFrequencyHz,
                                settings.g2WeakTierPulseDurationMs
                            )
                        },
                        previewRepeatIntervalMs = tierPreviewRepeatIntervalMs(
                            settings.g2WeakTierClickIntervalMs,
                            settings.g2WeakTierPulseDurationMs
                        ),
                        accentColor = accent
                    )
                },
                durationControls = {
                    TierPulseDurationSlider(
                        label = stringResource(R.string.passive_signal_tier_pulse_duration, G2_WEAK_TIER_NUMBER),
                        durationMs = settings.g2WeakTierPulseDurationMs,
                        accentColor = accent,
                        onDurationChange = { onSettingsChange(settings.copy(g2WeakTierPulseDurationMs = it)) }
                    )
                },
                intervalControls = {
                    TierClickSpeedSlider(
                        label = stringResource(R.string.passive_signal_tier_click_interval, G2_WEAK_TIER_NUMBER),
                        intervalMs = settings.g2WeakTierClickIntervalMs,
                        signalPulseDurationMs = settings.g2WeakTierPulseDurationMs,
                        passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                        accentColor = accent,
                        onIntervalChange = { onSettingsChange(settings.copy(g2WeakTierClickIntervalMs = it)) }
                    )
                },
                frequencyControls = {
                    TierPulseFrequencySlider(
                        label = stringResource(R.string.passive_signal_tier_pulse_frequency, G2_WEAK_TIER_NUMBER),
                        frequencyHz = g2WeakTierPulseFrequencyHz,
                        accentColor = accent,
                        onFrequencyChange = onG2WeakTierPulseFrequencyChange
                    )
                },
                voiceControls = {
                    Tier5SignalLowVoiceControls(
                        audioVolumes = audioVolumes,
                        previewEnabled = previewEnabled,
                        accentColor = accent,
                        onTier5AnnouncerEnabledChange = onTier5AnnouncerEnabledChange,
                        onTier5AnnouncerVolumeChange = onTier5AnnouncerVolumeChange,
                        onPreviewTier5Announcer = onPreviewTier5Announcer,
                        enabledTitle = stringResource(R.string.passive_signal_rxss8_voice_enabled),
                        volumeLabel = stringResource(R.string.passive_signal_rxss8_voice_volume)
                    )
                }
            )
        }
    }
}

/**
 * Collapsible sub-panel ("RXSS 2-5 common settings") for the signal-pulse volume/frequency
 * shared by RXSS 2-5 (Level Ranges A-D). Each of those tiers still keeps its own enable toggle,
 * RSRP boundary, pulse duration, and click interval in its own [TierSettingSection].
 */
@Composable
private fun LevelRangeBcdSharedSoundControls(
    settings: PassiveSignalSettings,
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    accentColor: Color,
    onLevelRangeBcdClickVolumeChange: (Float) -> Unit,
    onLevelRangeBcdPulseFrequencyChange: (Int) -> Unit,
    onPreviewLevelRangeBcdClick: (frequencyHz: Int, pulseDurationMs: Int) -> Unit
) {
    // Each Level Range (A–D) has its own pulse duration and click interval; use the longest
    // duration and fastest interval among the four as a representative preview rate.
    val sharedFloorPulseDurationMs = settings.levelRangeAbcdMaxPulseDurationMs()
    val previewRepeatIntervalMs = tierPreviewRepeatIntervalMs(
        settings.levelRangeAbcdMinClickIntervalMs(),
        sharedFloorPulseDurationMs
    )
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.passive_signal_level_range_bcd_section),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                RxssVolumeSlider(
                    label = stringResource(R.string.passive_signal_level_range_bcd_volume),
                    value = audioVolumes.levelRangeBcdClickVolume,
                    onValueChange = onLevelRangeBcdClickVolumeChange,
                    previewEnabled = previewEnabled,
                    onPreview = {
                        onPreviewLevelRangeBcdClick(
                            audioVolumes.levelRangeBcdPulseFrequencyHz,
                            sharedFloorPulseDurationMs
                        )
                    },
                    previewRepeatIntervalMs = previewRepeatIntervalMs,
                    accentColor = accentColor
                )
                TierPulseFrequencySlider(
                    label = stringResource(R.string.passive_signal_level_range_bcd_frequency),
                    frequencyHz = audioVolumes.levelRangeBcdPulseFrequencyHz,
                    accentColor = accentColor,
                    onFrequencyChange = onLevelRangeBcdPulseFrequencyChange
                )
            }
        }
    }
}

@Composable
private fun DeadzoneTierSettings(
    settings: PassiveSignalSettings,
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    passiveMeasurementIntervalMs: Long,
    onSettingsChange: (PassiveSignalSettings) -> Unit,
    onNoSignalTierPulseFrequencyChange: (Int) -> Unit,
    onNoSignalToneVolumeChange: (Float) -> Unit,
    onNoSignalVibrationEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceVolumeChange: (Float) -> Unit,
    onPreviewNoSignalVoice: () -> Unit,
    onPreviewSignalPulse: (volume: Float, frequencyHz: Int, pulseDurationMs: Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TierSettingSection(
            tierNumber = DEADZONE_TIER_NUMBER,
            accentColor = SignalTierColors.forTierNumber(DEADZONE_TIER_NUMBER)
        ) {
            val accent = SignalTierColors.forTierNumber(DEADZONE_TIER_NUMBER)
            RxssSectionControlsOrdered(
                accentColor = accent,
                soundToggle = {
                    TierSoundEnabledOption(
                        tierNumber = DEADZONE_TIER_NUMBER,
                        enabled = settings.deadzoneTierSoundEnabled,
                        onEnabledChange = { onSettingsChange(settings.copy(deadzoneTierSoundEnabled = it)) }
                    )
                },
                volumeControls = {
                    CampSignalPulseVolumeControl(
                        label = stringResource(R.string.audio_volume_no_signal),
                        volume = audioVolumes.noSignalToneVolume,
                        onVolumeChange = onNoSignalToneVolumeChange,
                        previewEnabled = previewEnabled,
                        accentColor = accent,
                        pulseDurationMs = settings.deadzoneTierPulseDurationMs,
                        clickIntervalMs = settings.deadzoneTierClickIntervalMs,
                        frequencyHz = audioVolumes.noSignalTierPulseFrequencyHz,
                        onPreviewSignalPulse = onPreviewSignalPulse
                    )
                },
                durationControls = {
                    TierPulseDurationSlider(
                        label = stringResource(R.string.passive_signal_tier_pulse_duration, DEADZONE_TIER_NUMBER),
                        durationMs = settings.deadzoneTierPulseDurationMs,
                        accentColor = accent,
                        onDurationChange = { onSettingsChange(settings.copy(deadzoneTierPulseDurationMs = it)) }
                    )
                },
                intervalControls = {
                    TierClickSpeedSlider(
                        label = stringResource(R.string.passive_signal_tier_click_interval, DEADZONE_TIER_NUMBER),
                        intervalMs = settings.deadzoneTierClickIntervalMs,
                        signalPulseDurationMs = settings.deadzoneTierPulseDurationMs,
                        passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                        accentColor = accent,
                        onIntervalChange = { onSettingsChange(settings.copy(deadzoneTierClickIntervalMs = it)) }
                    )
                },
                frequencyControls = {
                    TierPulseFrequencySlider(
                        label = stringResource(R.string.passive_signal_tier_pulse_frequency, DEADZONE_TIER_NUMBER),
                        frequencyHz = audioVolumes.noSignalTierPulseFrequencyHz,
                        accentColor = accent,
                        onFrequencyChange = onNoSignalTierPulseFrequencyChange
                    )
                },
                voiceControls = {
                    NoSignalVoiceAnnouncementControls(
                        audioVolumes = audioVolumes,
                        previewEnabled = previewEnabled,
                        accentColor = accent,
                        onNoSignalVibrationEnabledChange = onNoSignalVibrationEnabledChange,
                        onNoSignalVoiceEnabledChange = onNoSignalVoiceEnabledChange,
                        onNoSignalVoiceVolumeChange = onNoSignalVoiceVolumeChange,
                        onPreviewNoSignalVoice = onPreviewNoSignalVoice
                    )
                }
            )
        }
    }
}

@Composable
private fun CampStateTierSettings(
    settings: PassiveSignalSettings,
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    passiveMeasurementIntervalMs: Long,
    onSettingsChange: (PassiveSignalSettings) -> Unit,
    onNoSignalTierPulseFrequencyChange: (Int) -> Unit,
    onNoSignalToneVolumeChange: (Float) -> Unit,
    onNoSignalVibrationEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceVolumeChange: (Float) -> Unit,
    onLimitedServiceTierPulseFrequencyChange: (Int) -> Unit,
    onLimitedServiceToneVolumeChange: (Float) -> Unit,
    onLimitedServiceVoiceEnabledChange: (Boolean) -> Unit,
    onLimitedServiceVoiceVolumeChange: (Float) -> Unit,
    onPreviewNoSignalTone: () -> Unit,
    onPreviewNoSignalVoice: () -> Unit,
    onPreviewLimitedServiceTone: () -> Unit,
    onPreviewLimitedServiceVoice: () -> Unit,
    onLowSignalClickVolumeChange: (Float) -> Unit,
    onPreviewLowSignalClick: (frequencyHz: Int, pulseDurationMs: Int) -> Unit,
    onPreviewSignalPulse: (volume: Float, frequencyHz: Int, pulseDurationMs: Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NoSignalCampTierBlock(
            settings = settings,
            audioVolumes = audioVolumes,
            previewEnabled = previewEnabled,
            passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
            onSettingsChange = onSettingsChange,
            onNoSignalTierPulseFrequencyChange = onNoSignalTierPulseFrequencyChange,
            onNoSignalToneVolumeChange = onNoSignalToneVolumeChange,
            onNoSignalVibrationEnabledChange = onNoSignalVibrationEnabledChange,
            onNoSignalVoiceEnabledChange = onNoSignalVoiceEnabledChange,
            onNoSignalVoiceVolumeChange = onNoSignalVoiceVolumeChange,
            onPreviewNoSignalTone = onPreviewNoSignalTone,
            onPreviewNoSignalVoice = onPreviewNoSignalVoice,
            onPreviewSignalPulse = onPreviewSignalPulse
        )
        WifiCallingCampTierBlock(
            settings = settings,
            audioVolumes = audioVolumes,
            previewEnabled = previewEnabled,
            passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
            onSettingsChange = onSettingsChange,
            onNoSignalTierPulseFrequencyChange = onNoSignalTierPulseFrequencyChange,
            onNoSignalToneVolumeChange = onNoSignalToneVolumeChange,
            onNoSignalVibrationEnabledChange = onNoSignalVibrationEnabledChange,
            onNoSignalVoiceEnabledChange = onNoSignalVoiceEnabledChange,
            onNoSignalVoiceVolumeChange = onNoSignalVoiceVolumeChange,
            onPreviewNoSignalVoice = onPreviewNoSignalVoice,
            onPreviewSignalPulse = onPreviewSignalPulse
        )
        G2NoSignalCampTierBlock(
            settings = settings,
            audioVolumes = audioVolumes,
            previewEnabled = previewEnabled,
            passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
            onSettingsChange = onSettingsChange,
            onNoSignalTierPulseFrequencyChange = onNoSignalTierPulseFrequencyChange,
            onNoSignalToneVolumeChange = onNoSignalToneVolumeChange,
            onNoSignalVibrationEnabledChange = onNoSignalVibrationEnabledChange,
            onNoSignalVoiceEnabledChange = onNoSignalVoiceEnabledChange,
            onNoSignalVoiceVolumeChange = onNoSignalVoiceVolumeChange,
            onPreviewNoSignalTone = onPreviewNoSignalTone,
            onPreviewNoSignalVoice = onPreviewNoSignalVoice,
            onPreviewSignalPulse = onPreviewSignalPulse
        )
        Searching2gCampTierBlock(
            settings = settings,
            audioVolumes = audioVolumes,
            previewEnabled = previewEnabled,
            passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
            onSettingsChange = onSettingsChange,
            onNoSignalTierPulseFrequencyChange = onNoSignalTierPulseFrequencyChange,
            onNoSignalToneVolumeChange = onNoSignalToneVolumeChange,
            onNoSignalVibrationEnabledChange = onNoSignalVibrationEnabledChange,
            onNoSignalVoiceEnabledChange = onNoSignalVoiceEnabledChange,
            onNoSignalVoiceVolumeChange = onNoSignalVoiceVolumeChange,
            onPreviewNoSignalVoice = onPreviewNoSignalVoice,
            onPreviewSignalPulse = onPreviewSignalPulse
        )
        LimitedServiceCampTierBlock(
            settings = settings,
            audioVolumes = audioVolumes,
            previewEnabled = previewEnabled,
            passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
            onSettingsChange = onSettingsChange,
            onLimitedServiceTierPulseFrequencyChange = onLimitedServiceTierPulseFrequencyChange,
            onLimitedServiceToneVolumeChange = onLimitedServiceToneVolumeChange,
            onLimitedServiceVoiceEnabledChange = onLimitedServiceVoiceEnabledChange,
            onLimitedServiceVoiceVolumeChange = onLimitedServiceVoiceVolumeChange,
            onPreviewLimitedServiceTone = onPreviewLimitedServiceTone,
            onPreviewLimitedServiceVoice = onPreviewLimitedServiceVoice,
            onPreviewSignalPulse = onPreviewSignalPulse
        )
        LimitedAlt2gCampTierBlock(
            settings = settings,
            audioVolumes = audioVolumes,
            previewEnabled = previewEnabled,
            passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
            onSettingsChange = onSettingsChange,
            onLimitedServiceTierPulseFrequencyChange = onLimitedServiceTierPulseFrequencyChange,
            onLimitedServiceToneVolumeChange = onLimitedServiceToneVolumeChange,
            onLimitedServiceVoiceEnabledChange = onLimitedServiceVoiceEnabledChange,
            onLimitedServiceVoiceVolumeChange = onLimitedServiceVoiceVolumeChange,
            onPreviewLimitedServiceTone = onPreviewLimitedServiceTone,
            onPreviewLimitedServiceVoice = onPreviewLimitedServiceVoice,
            onPreviewSignalPulse = onPreviewSignalPulse
        )
    }
}

@Composable
private fun NoSignalCampTierBlock(
    settings: PassiveSignalSettings,
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    passiveMeasurementIntervalMs: Long,
    onSettingsChange: (PassiveSignalSettings) -> Unit,
    onNoSignalTierPulseFrequencyChange: (Int) -> Unit,
    onNoSignalToneVolumeChange: (Float) -> Unit,
    onNoSignalVibrationEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceVolumeChange: (Float) -> Unit,
    onPreviewNoSignalTone: () -> Unit,
    onPreviewNoSignalVoice: () -> Unit,
    onPreviewSignalPulse: (volume: Float, frequencyHz: Int, pulseDurationMs: Int) -> Unit
) {
    val accentColor = SignalTierColors.forTierNumber(NO_SIGNAL_TIER_NUMBER)
    TierSettingSection(tierNumber = NO_SIGNAL_TIER_NUMBER, accentColor = accentColor) {
        RxssSectionControlsOrdered(
            accentColor = accentColor,
           
            soundToggle = {
                TierSoundEnabledOption(
                    tierNumber = NO_SIGNAL_TIER_NUMBER,
                    enabled = settings.noSignalTierSoundEnabled,
                    onEnabledChange = { onSettingsChange(settings.copy(noSignalTierSoundEnabled = it)) }
                )
            },
            rangeControls = {
                Text(
                    text = stringResource(R.string.passive_signal_no_signal_tier_threshold),
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor
                )
            },
            volumeControls = {
                CampSignalPulseVolumeControl(
                    label = stringResource(R.string.audio_volume_no_signal),
                    volume = audioVolumes.noSignalToneVolume,
                    onVolumeChange = onNoSignalToneVolumeChange,
                    previewEnabled = previewEnabled,
                    accentColor = accentColor,
                    pulseDurationMs = settings.noSignalTierPulseDurationMs,
                    clickIntervalMs = settings.noSignalTierClickIntervalMs,
                    frequencyHz = audioVolumes.noSignalTierPulseFrequencyHz,
                    onPreviewSignalPulse = onPreviewSignalPulse
                )
            },
            durationControls = {
                TierPulseDurationSlider(
                    label = stringResource(R.string.passive_signal_tier_pulse_duration, NO_SIGNAL_TIER_NUMBER),
                    durationMs = settings.noSignalTierPulseDurationMs,
                    accentColor = accentColor,
                    onDurationChange = { onSettingsChange(settings.copy(noSignalTierPulseDurationMs = it)) }
                )
            },
            intervalControls = {
                TierClickSpeedSlider(
                    label = stringResource(R.string.passive_signal_tier_click_interval, NO_SIGNAL_TIER_NUMBER),
                    intervalMs = settings.noSignalTierClickIntervalMs,
                    signalPulseDurationMs = settings.noSignalTierPulseDurationMs,
                    passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                    accentColor = accentColor,
                    onIntervalChange = { onSettingsChange(settings.copy(noSignalTierClickIntervalMs = it)) }
                )
            },
            frequencyControls = {
                TierPulseFrequencySlider(
                    label = stringResource(R.string.passive_signal_tier_pulse_frequency, NO_SIGNAL_TIER_NUMBER),
                    frequencyHz = audioVolumes.noSignalTierPulseFrequencyHz,
                    accentColor = accentColor,
                    onFrequencyChange = onNoSignalTierPulseFrequencyChange
                )
            },
            voiceControls = {
                NoSignalVoiceAnnouncementControls(
                    audioVolumes = audioVolumes,
                    previewEnabled = previewEnabled,
                    accentColor = accentColor,
                    onNoSignalVibrationEnabledChange = onNoSignalVibrationEnabledChange,
                    onNoSignalVoiceEnabledChange = onNoSignalVoiceEnabledChange,
                    onNoSignalVoiceVolumeChange = onNoSignalVoiceVolumeChange,
                    onPreviewNoSignalVoice = onPreviewNoSignalVoice
                )
            }
        )
    }
}

@Composable
private fun WifiCallingCampTierBlock(
    settings: PassiveSignalSettings,
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    passiveMeasurementIntervalMs: Long,
    onSettingsChange: (PassiveSignalSettings) -> Unit,
    onNoSignalTierPulseFrequencyChange: (Int) -> Unit,
    onNoSignalToneVolumeChange: (Float) -> Unit,
    onNoSignalVibrationEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceVolumeChange: (Float) -> Unit,
    onPreviewNoSignalVoice: () -> Unit,
    onPreviewSignalPulse: (volume: Float, frequencyHz: Int, pulseDurationMs: Int) -> Unit
) {
    val tierNumber = Rxss.WIFI_CALLING_NO_SIGNAL
    val accentColor = SignalTierColors.forTierNumber(tierNumber)
    TierSettingSection(tierNumber = tierNumber, accentColor = accentColor) {
        RxssSectionControlsOrdered(
            accentColor = accentColor,
           
            soundToggle = {
                TierSoundEnabledOption(
                    tierNumber = tierNumber,
                    enabled = settings.wifiCallingTierSoundEnabled,
                    onEnabledChange = { onSettingsChange(settings.copy(wifiCallingTierSoundEnabled = it)) }
                )
            },
            rangeControls = {
                Text(
                    text = stringResource(R.string.passive_signal_wifi_calling_tier_threshold),
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor
                )
            },
            volumeControls = {
                CampSignalPulseVolumeControl(
                    label = stringResource(R.string.audio_volume_no_signal),
                    volume = audioVolumes.noSignalToneVolume,
                    onVolumeChange = onNoSignalToneVolumeChange,
                    previewEnabled = previewEnabled,
                    accentColor = accentColor,
                    pulseDurationMs = settings.wifiCallingTierPulseDurationMs,
                    clickIntervalMs = settings.wifiCallingTierClickIntervalMs,
                    frequencyHz = audioVolumes.noSignalTierPulseFrequencyHz,
                    onPreviewSignalPulse = onPreviewSignalPulse
                )
            },
            durationControls = {
                TierPulseDurationSlider(
                    label = stringResource(R.string.passive_signal_tier_pulse_duration, tierNumber),
                    durationMs = settings.wifiCallingTierPulseDurationMs,
                    accentColor = accentColor,
                    onDurationChange = { onSettingsChange(settings.copy(wifiCallingTierPulseDurationMs = it)) }
                )
            },
            intervalControls = {
                TierClickSpeedSlider(
                    label = stringResource(R.string.passive_signal_tier_click_interval, tierNumber),
                    intervalMs = settings.wifiCallingTierClickIntervalMs,
                    signalPulseDurationMs = settings.wifiCallingTierPulseDurationMs,
                    passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                    accentColor = accentColor,
                    onIntervalChange = { onSettingsChange(settings.copy(wifiCallingTierClickIntervalMs = it)) }
                )
            },
            frequencyControls = {
                TierPulseFrequencySlider(
                    label = stringResource(R.string.passive_signal_tier_pulse_frequency, tierNumber),
                    frequencyHz = audioVolumes.noSignalTierPulseFrequencyHz,
                    accentColor = accentColor,
                    onFrequencyChange = onNoSignalTierPulseFrequencyChange
                )
            },
            voiceControls = {
                NoSignalVoiceAnnouncementControls(
                    audioVolumes = audioVolumes,
                    previewEnabled = previewEnabled,
                    accentColor = accentColor,
                    onNoSignalVibrationEnabledChange = onNoSignalVibrationEnabledChange,
                    onNoSignalVoiceEnabledChange = onNoSignalVoiceEnabledChange,
                    onNoSignalVoiceVolumeChange = onNoSignalVoiceVolumeChange,
                    onPreviewNoSignalVoice = onPreviewNoSignalVoice
                )
            }
        )
    }
}

@Composable
private fun G2NoSignalCampTierBlock(
    settings: PassiveSignalSettings,
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    passiveMeasurementIntervalMs: Long,
    onSettingsChange: (PassiveSignalSettings) -> Unit,
    onNoSignalTierPulseFrequencyChange: (Int) -> Unit,
    onNoSignalToneVolumeChange: (Float) -> Unit,
    onNoSignalVibrationEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceVolumeChange: (Float) -> Unit,
    onPreviewNoSignalTone: () -> Unit,
    onPreviewNoSignalVoice: () -> Unit,
    onPreviewSignalPulse: (volume: Float, frequencyHz: Int, pulseDurationMs: Int) -> Unit
) {
    val accentColor = SignalTierColors.forTierNumber(G2_NO_SIGNAL_TIER_NUMBER)
    TierSettingSection(tierNumber = G2_NO_SIGNAL_TIER_NUMBER, accentColor = accentColor) {
        RxssSectionControlsOrdered(
            accentColor = accentColor,
           
            soundToggle = {
                TierSoundEnabledOption(
                    tierNumber = G2_NO_SIGNAL_TIER_NUMBER,
                    enabled = settings.g2NoSignalTierSoundEnabled,
                    onEnabledChange = { onSettingsChange(settings.copy(g2NoSignalTierSoundEnabled = it)) }
                )
            },
            rangeControls = {
                Text(
                    text = stringResource(R.string.passive_signal_g2_no_signal_tier_threshold),
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor
                )
            },
            volumeControls = {
                CampSignalPulseVolumeControl(
                    label = stringResource(R.string.audio_volume_no_signal),
                    volume = audioVolumes.noSignalToneVolume,
                    onVolumeChange = onNoSignalToneVolumeChange,
                    previewEnabled = previewEnabled,
                    accentColor = accentColor,
                    pulseDurationMs = settings.g2NoSignalTierPulseDurationMs,
                    clickIntervalMs = settings.g2NoSignalTierClickIntervalMs,
                    frequencyHz = audioVolumes.noSignalTierPulseFrequencyHz,
                    onPreviewSignalPulse = onPreviewSignalPulse
                )
            },
            durationControls = {
                TierPulseDurationSlider(
                    label = stringResource(R.string.passive_signal_tier_pulse_duration, G2_NO_SIGNAL_TIER_NUMBER),
                    durationMs = settings.g2NoSignalTierPulseDurationMs,
                    accentColor = accentColor,
                    onDurationChange = { onSettingsChange(settings.copy(g2NoSignalTierPulseDurationMs = it)) }
                )
            },
            intervalControls = {
                TierClickSpeedSlider(
                    label = stringResource(R.string.passive_signal_tier_click_interval, G2_NO_SIGNAL_TIER_NUMBER),
                    intervalMs = settings.g2NoSignalTierClickIntervalMs,
                    signalPulseDurationMs = settings.g2NoSignalTierPulseDurationMs,
                    passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                    accentColor = accentColor,
                    onIntervalChange = { onSettingsChange(settings.copy(g2NoSignalTierClickIntervalMs = it)) }
                )
            },
            frequencyControls = {
                TierPulseFrequencySlider(
                    label = stringResource(R.string.passive_signal_tier_pulse_frequency, G2_NO_SIGNAL_TIER_NUMBER),
                    frequencyHz = audioVolumes.noSignalTierPulseFrequencyHz,
                    accentColor = accentColor,
                    onFrequencyChange = onNoSignalTierPulseFrequencyChange
                )
            },
            voiceControls = {
                NoSignalVoiceAnnouncementControls(
                    audioVolumes = audioVolumes,
                    previewEnabled = previewEnabled,
                    accentColor = accentColor,
                    onNoSignalVibrationEnabledChange = onNoSignalVibrationEnabledChange,
                    onNoSignalVoiceEnabledChange = onNoSignalVoiceEnabledChange,
                    onNoSignalVoiceVolumeChange = onNoSignalVoiceVolumeChange,
                    onPreviewNoSignalVoice = onPreviewNoSignalVoice
                )
            }
        )
    }
}

@Composable
private fun Searching2gCampTierBlock(
    settings: PassiveSignalSettings,
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    passiveMeasurementIntervalMs: Long,
    onSettingsChange: (PassiveSignalSettings) -> Unit,
    onNoSignalTierPulseFrequencyChange: (Int) -> Unit,
    onNoSignalToneVolumeChange: (Float) -> Unit,
    onNoSignalVibrationEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceVolumeChange: (Float) -> Unit,
    onPreviewNoSignalVoice: () -> Unit,
    onPreviewSignalPulse: (volume: Float, frequencyHz: Int, pulseDurationMs: Int) -> Unit
) {
    val accentColor = SignalTierColors.forTierNumber(SEARCHING_2G_TIER_NUMBER)
    TierSettingSection(tierNumber = SEARCHING_2G_TIER_NUMBER, accentColor = accentColor) {
        RxssSectionControlsOrdered(
            accentColor = accentColor,
           
            soundToggle = {
                TierSoundEnabledOption(
                    tierNumber = SEARCHING_2G_TIER_NUMBER,
                    enabled = settings.searching2gTierSoundEnabled,
                    onEnabledChange = { onSettingsChange(settings.copy(searching2gTierSoundEnabled = it)) }
                )
            },
            rangeControls = {
                Text(
                    text = stringResource(R.string.passive_signal_searching_2g_tier_threshold),
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor
                )
            },
            volumeControls = {
                CampSignalPulseVolumeControl(
                    label = stringResource(R.string.audio_volume_no_signal),
                    volume = audioVolumes.noSignalToneVolume,
                    onVolumeChange = onNoSignalToneVolumeChange,
                    previewEnabled = previewEnabled,
                    accentColor = accentColor,
                    pulseDurationMs = settings.searching2gTierPulseDurationMs,
                    clickIntervalMs = settings.searching2gTierClickIntervalMs,
                    frequencyHz = audioVolumes.noSignalTierPulseFrequencyHz,
                    onPreviewSignalPulse = onPreviewSignalPulse
                )
            },
            durationControls = {
                TierPulseDurationSlider(
                    label = stringResource(R.string.passive_signal_tier_pulse_duration, SEARCHING_2G_TIER_NUMBER),
                    durationMs = settings.searching2gTierPulseDurationMs,
                    accentColor = accentColor,
                    onDurationChange = { onSettingsChange(settings.copy(searching2gTierPulseDurationMs = it)) }
                )
            },
            intervalControls = {
                TierClickSpeedSlider(
                    label = stringResource(R.string.passive_signal_tier_click_interval, SEARCHING_2G_TIER_NUMBER),
                    intervalMs = settings.searching2gTierClickIntervalMs,
                    signalPulseDurationMs = settings.searching2gTierPulseDurationMs,
                    passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                    accentColor = accentColor,
                    onIntervalChange = { onSettingsChange(settings.copy(searching2gTierClickIntervalMs = it)) }
                )
            },
            frequencyControls = {
                TierPulseFrequencySlider(
                    label = stringResource(R.string.passive_signal_tier_pulse_frequency, SEARCHING_2G_TIER_NUMBER),
                    frequencyHz = audioVolumes.noSignalTierPulseFrequencyHz,
                    accentColor = accentColor,
                    onFrequencyChange = onNoSignalTierPulseFrequencyChange
                )
            },
            voiceControls = {
                NoSignalVoiceAnnouncementControls(
                    audioVolumes = audioVolumes,
                    previewEnabled = previewEnabled,
                    accentColor = accentColor,
                    onNoSignalVibrationEnabledChange = onNoSignalVibrationEnabledChange,
                    onNoSignalVoiceEnabledChange = onNoSignalVoiceEnabledChange,
                    onNoSignalVoiceVolumeChange = onNoSignalVoiceVolumeChange,
                    onPreviewNoSignalVoice = onPreviewNoSignalVoice
                )
            }
        )
    }
}

@Composable
private fun LimitedServiceCampTierBlock(
    settings: PassiveSignalSettings,
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    passiveMeasurementIntervalMs: Long,
    onSettingsChange: (PassiveSignalSettings) -> Unit,
    onLimitedServiceTierPulseFrequencyChange: (Int) -> Unit,
    onLimitedServiceToneVolumeChange: (Float) -> Unit,
    onLimitedServiceVoiceEnabledChange: (Boolean) -> Unit,
    onLimitedServiceVoiceVolumeChange: (Float) -> Unit,
    onPreviewLimitedServiceTone: () -> Unit,
    onPreviewLimitedServiceVoice: () -> Unit,
    onPreviewSignalPulse: (volume: Float, frequencyHz: Int, pulseDurationMs: Int) -> Unit
) {
    val accentColor = SignalTierColors.forTierNumber(LIMITED_SERVICE_TIER_NUMBER)
    TierSettingSection(tierNumber = LIMITED_SERVICE_TIER_NUMBER, accentColor = accentColor) {
        RxssSectionControlsOrdered(
            accentColor = accentColor,
           
            soundToggle = {
                TierSoundEnabledOption(
                    tierNumber = LIMITED_SERVICE_TIER_NUMBER,
                    enabled = settings.limitedServiceTierSoundEnabled,
                    onEnabledChange = { onSettingsChange(settings.copy(limitedServiceTierSoundEnabled = it)) }
                )
            },
            rangeControls = {
                Text(
                    text = stringResource(R.string.passive_signal_limited_service_tier_threshold),
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor
                )
            },
            volumeControls = {
                CampSignalPulseVolumeControl(
                    label = stringResource(R.string.audio_volume_limited_service),
                    volume = audioVolumes.limitedServiceToneVolume,
                    onVolumeChange = onLimitedServiceToneVolumeChange,
                    previewEnabled = previewEnabled,
                    accentColor = accentColor,
                    pulseDurationMs = settings.limitedServiceTierPulseDurationMs,
                    clickIntervalMs = settings.limitedServiceTierClickIntervalMs,
                    frequencyHz = audioVolumes.limitedServiceTierPulseFrequencyHz,
                    onPreviewSignalPulse = onPreviewSignalPulse
                )
            },
            durationControls = {
                TierPulseDurationSlider(
                    label = stringResource(R.string.passive_signal_tier_pulse_duration, LIMITED_SERVICE_TIER_NUMBER),
                    durationMs = settings.limitedServiceTierPulseDurationMs,
                    accentColor = accentColor,
                    onDurationChange = { onSettingsChange(settings.copy(limitedServiceTierPulseDurationMs = it)) }
                )
            },
            intervalControls = {
                TierClickSpeedSlider(
                    label = stringResource(R.string.passive_signal_tier_click_interval, LIMITED_SERVICE_TIER_NUMBER),
                    intervalMs = settings.limitedServiceTierClickIntervalMs,
                    signalPulseDurationMs = settings.limitedServiceTierPulseDurationMs,
                    passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                    accentColor = accentColor,
                    onIntervalChange = { onSettingsChange(settings.copy(limitedServiceTierClickIntervalMs = it)) }
                )
            },
            frequencyControls = {
                TierPulseFrequencySlider(
                    label = stringResource(R.string.passive_signal_tier_pulse_frequency, LIMITED_SERVICE_TIER_NUMBER),
                    frequencyHz = audioVolumes.limitedServiceTierPulseFrequencyHz,
                    accentColor = accentColor,
                    onFrequencyChange = onLimitedServiceTierPulseFrequencyChange
                )
            },
            voiceControls = {
                LimitedServiceVoiceAnnouncementControls(
                    audioVolumes = audioVolumes,
                    previewEnabled = previewEnabled,
                    accentColor = accentColor,
                    onLimitedServiceVoiceEnabledChange = onLimitedServiceVoiceEnabledChange,
                    onLimitedServiceVoiceVolumeChange = onLimitedServiceVoiceVolumeChange,
                    onPreviewLimitedServiceVoice = onPreviewLimitedServiceVoice
                )
            }
        )
    }
}

@Composable
private fun LimitedAlt2gCampTierBlock(
    settings: PassiveSignalSettings,
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    passiveMeasurementIntervalMs: Long,
    onSettingsChange: (PassiveSignalSettings) -> Unit,
    onLimitedServiceTierPulseFrequencyChange: (Int) -> Unit,
    onLimitedServiceToneVolumeChange: (Float) -> Unit,
    onLimitedServiceVoiceEnabledChange: (Boolean) -> Unit,
    onLimitedServiceVoiceVolumeChange: (Float) -> Unit,
    onPreviewLimitedServiceTone: () -> Unit,
    onPreviewLimitedServiceVoice: () -> Unit,
    onPreviewSignalPulse: (volume: Float, frequencyHz: Int, pulseDurationMs: Int) -> Unit
) {
    val accentColor = SignalTierColors.forTierNumber(LIMITED_ALT_2G_TIER_NUMBER)
    TierSettingSection(tierNumber = LIMITED_ALT_2G_TIER_NUMBER, accentColor = accentColor) {
        RxssSectionControlsOrdered(
            accentColor = accentColor,
           
            soundToggle = {
                TierSoundEnabledOption(
                    tierNumber = LIMITED_ALT_2G_TIER_NUMBER,
                    enabled = settings.limitedAlt2gTierSoundEnabled,
                    onEnabledChange = { onSettingsChange(settings.copy(limitedAlt2gTierSoundEnabled = it)) }
                )
            },
            rangeControls = {
                Text(
                    text = stringResource(R.string.passive_signal_limited_alt_2g_tier_threshold),
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor
                )
            },
            volumeControls = {
                CampSignalPulseVolumeControl(
                    label = stringResource(R.string.audio_volume_limited_service),
                    volume = audioVolumes.limitedServiceToneVolume,
                    onVolumeChange = onLimitedServiceToneVolumeChange,
                    previewEnabled = previewEnabled,
                    accentColor = accentColor,
                    pulseDurationMs = settings.limitedAlt2gTierPulseDurationMs,
                    clickIntervalMs = settings.limitedAlt2gTierClickIntervalMs,
                    frequencyHz = audioVolumes.limitedServiceTierPulseFrequencyHz,
                    onPreviewSignalPulse = onPreviewSignalPulse
                )
            },
            durationControls = {
                TierPulseDurationSlider(
                    label = stringResource(R.string.passive_signal_tier_pulse_duration, LIMITED_ALT_2G_TIER_NUMBER),
                    durationMs = settings.limitedAlt2gTierPulseDurationMs,
                    accentColor = accentColor,
                    onDurationChange = { onSettingsChange(settings.copy(limitedAlt2gTierPulseDurationMs = it)) }
                )
            },
            intervalControls = {
                TierClickSpeedSlider(
                    label = stringResource(R.string.passive_signal_tier_click_interval, LIMITED_ALT_2G_TIER_NUMBER),
                    intervalMs = settings.limitedAlt2gTierClickIntervalMs,
                    signalPulseDurationMs = settings.limitedAlt2gTierPulseDurationMs,
                    passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                    accentColor = accentColor,
                    onIntervalChange = { onSettingsChange(settings.copy(limitedAlt2gTierClickIntervalMs = it)) }
                )
            },
            frequencyControls = {
                TierPulseFrequencySlider(
                    label = stringResource(R.string.passive_signal_tier_pulse_frequency, LIMITED_ALT_2G_TIER_NUMBER),
                    frequencyHz = audioVolumes.limitedServiceTierPulseFrequencyHz,
                    accentColor = accentColor,
                    onFrequencyChange = onLimitedServiceTierPulseFrequencyChange
                )
            },
            voiceControls = {
                LimitedServiceVoiceAnnouncementControls(
                    audioVolumes = audioVolumes,
                    previewEnabled = previewEnabled,
                    accentColor = accentColor,
                    onLimitedServiceVoiceEnabledChange = onLimitedServiceVoiceEnabledChange,
                    onLimitedServiceVoiceVolumeChange = onLimitedServiceVoiceVolumeChange,
                    onPreviewLimitedServiceVoice = onPreviewLimitedServiceVoice
                )
            }
        )
    }
}

@Composable
private fun CampTierSignalPulseControls(
    tierNumber: Int,
    passiveMeasurementIntervalMs: Long,
    clickIntervalMs: Int,
    pulseDurationMs: Int,
    soundEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    onPulseDurationChange: (Int) -> Unit,
    onClickIntervalChange: (Int) -> Unit,
    thresholdText: String,
    volumeControls: @Composable () -> Unit = {},
    frequencyControls: @Composable () -> Unit = {},
    voiceControls: @Composable () -> Unit = {}
) {
    val accentColor = SignalTierColors.forTierNumber(tierNumber)
    RxssSectionControlsOrdered(
        accentColor = accentColor,
        
        soundToggle = {
            TierSoundEnabledOption(
                tierNumber = tierNumber,
                enabled = soundEnabled,
                onEnabledChange = onSoundEnabledChange
            )
        },
        rangeControls = {
            Text(
                text = thresholdText,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor
            )
        },
        volumeControls = volumeControls,
        durationControls = {
            TierPulseDurationSlider(
                label = stringResource(R.string.passive_signal_tier_pulse_duration, tierNumber),
                durationMs = pulseDurationMs,
                accentColor = accentColor,
                onDurationChange = onPulseDurationChange
            )
        },
        intervalControls = {
            TierClickSpeedSlider(
                label = stringResource(R.string.passive_signal_tier_click_interval, tierNumber),
                intervalMs = clickIntervalMs,
                signalPulseDurationMs = pulseDurationMs,
                passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                accentColor = accentColor,
                onIntervalChange = onClickIntervalChange
            )
        },
        frequencyControls = frequencyControls,
        voiceControls = voiceControls
    )
}

/**
 * Collapsible sub-panel for a single RXSS tier's controls, nested inside the "Passive signal
 * thresholds and alert settings" panel. One instance per RXSS number (see [rxssSectionTitle]).
 */
@Composable
private fun TierSettingSection(
    tierNumber: Int,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rxssSectionTitle(tierNumber),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun RsrqBoundarySliders(
    settings: PassiveSignalSettings,
    onSettingsChange: (PassiveSignalSettings) -> Unit
) {
    BoundarySlider(
        label = stringResource(R.string.passive_signal_boundary_rsrq_fair),
        rangeLabel = stringResource(
            R.string.passive_signal_rsrq_single_band_hint,
            PassiveSignalSettings.RSRQ_FAIR_MIN_DB,
            settings.rsrqFairMinDb - PassiveSignalSettings.MIN_RSRQ_BAND_GAP_DB,
            settings.rsrqFairMinDb,
            PassiveSignalSettings.RSRQ_FAIR_MAX_DB
        ),
        value = settings.rsrqFairMinDb,
        valueRange = PassiveSignalSettings.RSRQ_FAIR_MIN_DB..PassiveSignalSettings.RSRQ_FAIR_MAX_DB,
        unit = "dB",
        accentColor = SignalTierColors.forTierNumber(RSRQ_POOR_TIER_NUMBER),
        onValueChange = { onSettingsChange(settings.copy(rsrqFairMinDb = it)) }
    )
}

@Composable
private fun RsrqTierSettings(
    settings: PassiveSignalSettings,
    previewEnabled: Boolean,
    passiveMeasurementIntervalMs: Long,
    onSettingsChange: (PassiveSignalSettings) -> Unit,
    onPreviewRsrqWhiteNoise: () -> Unit
) {
    val accentColor = SignalTierColors.forTierNumber(RSRQ_POOR_TIER_NUMBER)

    TierSettingSection(
        tierNumber = RSRQ_POOR_TIER_NUMBER,
        accentColor = accentColor
    ) {
        TierSoundEnabledOption(
            tierNumber = RSRQ_POOR_TIER_NUMBER,
            enabled = settings.rsrqTierSoundEnabled,
            onEnabledChange = { onSettingsChange(settings.copy(rsrqTierSoundEnabled = it)) }
        )
        RsrqBoundarySliders(
            settings = settings,
            onSettingsChange = onSettingsChange
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = settings.rsrqTierCoupledToSignalTier,
                onCheckedChange = { onSettingsChange(settings.copy(rsrqTierCoupledToSignalTier = it)) }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.passive_signal_rsrq_tier_coupled),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        TierWhiteNoiseVolumeSlider(
            volume = settings.rsrqTierWhiteNoiseVolume,
            accentColor = accentColor,
            previewEnabled = previewEnabled,
            previewRepeatIntervalMs = if (settings.rsrqTierCoupledToSignalTier) {
                null
            } else {
                tierPreviewRepeatIntervalMs(
                    settings.rsrqTierClickIntervalMs,
                    settings.rsrqTierPulseDurationMs
                )
            },
            onVolumeChange = { onSettingsChange(settings.copy(rsrqTierWhiteNoiseVolume = it)) },
            onPreview = onPreviewRsrqWhiteNoise
        )
        if (!settings.rsrqTierCoupledToSignalTier) {
            TierPulseDurationSlider(
                label = stringResource(R.string.passive_signal_rsrq_tier_pulse_duration),
                durationMs = settings.rsrqTierPulseDurationMs,
                accentColor = accentColor,
                onDurationChange = { onSettingsChange(settings.copy(rsrqTierPulseDurationMs = it)) }
            )
            TierClickSpeedSlider(
                label = stringResource(R.string.passive_signal_tier_click_interval, RSRQ_POOR_TIER_NUMBER),
                intervalMs = settings.rsrqTierClickIntervalMs,
                signalPulseDurationMs = settings.rsrqTierPulseDurationMs,
                passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                accentColor = accentColor,
                maxIntervalMs = PassiveSignalSettings.MAX_RSRQ_TIER_CLICK_INTERVAL_MS,
                onIntervalChange = { onSettingsChange(settings.copy(rsrqTierClickIntervalMs = it)) }
            )
        }
    }
}

@Composable
private fun TierWhiteNoiseVolumeSlider(
    volume: Float,
    accentColor: Color,
    previewEnabled: Boolean,
    previewRepeatIntervalMs: Long?,
    onVolumeChange: (Float) -> Unit,
    onPreview: () -> Unit
) {
    val min = PassiveSignalSettings.MIN_RSRQ_TIER_WHITE_NOISE_VOLUME
    val max = PassiveSignalSettings.MAX_RSRQ_TIER_WHITE_NOISE_VOLUME
    val coerced = volume.coerceIn(min, max)
    val percent = (coerced * 100f).roundToInt()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.passive_signal_rsrq_tier_white_noise_volume),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = accentColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.audio_volume_percent, percent),
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                fontWeight = FontWeight.Medium
            )
            RepeatablePreviewTestButton(
                enabled = previewEnabled && coerced > 0f,
                onPreview = onPreview,
                repeatIntervalMs = previewRepeatIntervalMs,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Slider(
            value = coerced,
            onValueChange = onVolumeChange,
            valueRange = min..max,
            colors = tierSliderColors(accentColor)
        )
    }
}

@Composable
private fun CampSignalPulseVolumeControl(
    label: String,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    previewEnabled: Boolean,
    accentColor: Color,
    pulseDurationMs: Int,
    clickIntervalMs: Int,
    frequencyHz: Int,
    onPreviewSignalPulse: (volume: Float, frequencyHz: Int, pulseDurationMs: Int) -> Unit
) {
    RxssVolumeSlider(
        label = label,
        value = volume,
        onValueChange = onVolumeChange,
        previewEnabled = previewEnabled,
        onPreview = {
            onPreviewSignalPulse(volume, frequencyHz, pulseDurationMs)
        },
        previewRepeatIntervalMs = tierPreviewRepeatIntervalMs(clickIntervalMs, pulseDurationMs),
        accentColor = accentColor
    )
}

@Composable
private fun CampSharedSignalPulseVolumeControl(
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    accentColor: Color,
    pulseDurationMs: Int,
    clickIntervalMs: Int,
    previewFrequencyHz: Int = audioVolumes.signalPulseFrequencyHz,
    onLowSignalClickVolumeChange: (Float) -> Unit,
    onPreviewSignalPulse: (volume: Float, frequencyHz: Int, pulseDurationMs: Int) -> Unit
) {
    CampSignalPulseVolumeControl(
        label = stringResource(R.string.passive_signal_camp_signal_pulse_volume),
        volume = audioVolumes.lowSignalClickVolume,
        onVolumeChange = onLowSignalClickVolumeChange,
        previewEnabled = previewEnabled,
        accentColor = accentColor,
        pulseDurationMs = pulseDurationMs,
        clickIntervalMs = clickIntervalMs,
        frequencyHz = previewFrequencyHz,
        onPreviewSignalPulse = onPreviewSignalPulse
    )
}

@Composable
private fun BoundarySlider(
    label: String,
    rangeLabel: String,
    value: Int,
    valueRange: IntRange,
    accentColor: Color? = null,
    unit: String = "dBm",
    onValueChange: (Int) -> Unit
) {
    if (!TierSliderSupport.isValidIntRange(valueRange)) return

    val controlColor = accentColor ?: MaterialTheme.colorScheme.primary
    val sliderSteps = TierSliderSupport.discreteIntRangeSteps(valueRange)
    val sliderValue = value.toFloat().coerceIn(valueRange.first.toFloat(), valueRange.last.toFloat())

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = controlColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.passive_signal_bound_value, value, unit),
                style = MaterialTheme.typography.bodySmall,
                color = controlColor,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = rangeLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = sliderValue,
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = sliderSteps,
            colors = tierSliderColors(controlColor)
        )
    }
}

private fun tierPreviewRepeatIntervalMs(configuredIntervalMs: Int, pulseDurationMs: Int): Long {
    return SettingsCompatibility.resolveTierClickIntervalMs(
        configuredMs = configuredIntervalMs.toLong(),
        signalPulseDurationMs = pulseDurationMs,
        isPassiveOnlySession = true
    )
}
