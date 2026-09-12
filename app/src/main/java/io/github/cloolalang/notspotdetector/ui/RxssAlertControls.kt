package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.CellReselectBandNamingStyle
import io.github.cloolalang.notspotdetector.model.PeriodicVoiceRepeat
import io.github.cloolalang.notspotdetector.model.TechnologyChangeTarget
import io.github.cloolalang.notspotdetector.model.VoicePhraseFragment
import io.github.cloolalang.notspotdetector.model.VoicePhraseGroup
import io.github.cloolalang.notspotdetector.model.VoicePhraseOptions
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

val LocalOnVoicePhrasesChange = staticCompositionLocalOf<(VoicePhraseGroup, VoicePhraseOptions) -> Unit> {
    { _, _ -> }
}

val LocalOnPreviewVoicePhrase = staticCompositionLocalOf<(VoicePhraseGroup, VoicePhraseFragment) -> Unit> {
    { _, _ -> }
}

val LocalOnPeriodicVoiceRepeatChange = staticCompositionLocalOf<(PeriodicVoiceRepeat, Boolean) -> Unit> {
    { _, _ -> }
}

@Composable
fun VoicePhraseToggles(
    group: VoicePhraseGroup,
    phrases: VoicePhraseOptions,
    previewEnabled: Boolean,
    accentColor: Color
) {
    val onPhrasesChange = LocalOnVoicePhrasesChange.current
    val onPreviewFragment = LocalOnPreviewVoicePhrase.current
    val controlsEnabled = settingsControlsEnabled()

    VoicePhraseToggleRow(
        checked = phrases.speakOperatorName,
        onCheckedChange = { onPhrasesChange(group, phrases.withOperator(it)) },
        title = stringResource(R.string.voice_announcement_speak_operator_name),
        hint = stringResource(R.string.voice_phrase_speak_operator_hint),
        previewEnabled = previewEnabled,
        accentColor = accentColor,
        controlsEnabled = controlsEnabled,
        onPreview = { onPreviewFragment(group, VoicePhraseFragment.OPERATOR) }
    )
    VoicePhraseToggleRow(
        checked = phrases.speakTechnology,
        onCheckedChange = { onPhrasesChange(group, phrases.withTechnology(it)) },
        title = stringResource(R.string.voice_announcement_speak_technology),
        hint = stringResource(R.string.voice_phrase_speak_technology_hint),
        previewEnabled = previewEnabled,
        accentColor = accentColor,
        controlsEnabled = controlsEnabled,
        onPreview = { onPreviewFragment(group, VoicePhraseFragment.TECHNOLOGY) }
    )
    VoicePhraseToggleRow(
        checked = phrases.speakBand,
        onCheckedChange = { onPhrasesChange(group, phrases.withBand(it)) },
        title = stringResource(R.string.voice_phrase_speak_band),
        hint = stringResource(R.string.voice_phrase_speak_band_hint),
        previewEnabled = previewEnabled,
        accentColor = accentColor,
        controlsEnabled = controlsEnabled,
        onPreview = { onPreviewFragment(group, VoicePhraseFragment.BAND) }
    )
    if (group == VoicePhraseGroup.LIMITED_SERVICE) {
        VoicePhraseToggleRow(
            checked = phrases.speakHomeLimitedService,
            onCheckedChange = { onPhrasesChange(group, phrases.withHomeLimitedService(it)) },
            title = stringResource(R.string.voice_phrase_speak_home_limited_service),
            hint = stringResource(R.string.voice_phrase_speak_home_limited_service_hint),
            previewEnabled = previewEnabled,
            accentColor = accentColor,
            controlsEnabled = controlsEnabled,
            onPreview = { onPreviewFragment(group, VoicePhraseFragment.HOME_LIMITED_SERVICE) }
        )
        VoicePhraseToggleRow(
            checked = phrases.speakVisitingLimitedService,
            onCheckedChange = { onPhrasesChange(group, phrases.withVisitingLimitedService(it)) },
            title = stringResource(R.string.voice_phrase_speak_visiting_limited_service),
            hint = stringResource(R.string.voice_phrase_speak_visiting_limited_service_hint),
            previewEnabled = previewEnabled,
            accentColor = accentColor,
            controlsEnabled = controlsEnabled,
            onPreview = { onPreviewFragment(group, VoicePhraseFragment.VISITING_LIMITED_SERVICE) }
        )
    }
}

@Composable
private fun VoicePhraseToggleRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    hint: String,
    previewEnabled: Boolean,
    accentColor: Color,
    controlsEnabled: Boolean,
    onPreview: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = controlsEnabled
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = accentColor
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RepeatablePreviewTestButton(
            enabled = previewEnabled,
            onPreview = onPreview,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun PeriodicVoiceRepeatOption(
    kind: PeriodicVoiceRepeat,
    checked: Boolean,
    enabled: Boolean,
    accentColor: Color,
    title: String = stringResource(R.string.audio_periodic_voice_enabled),
    hint: String = stringResource(R.string.audio_periodic_voice_enabled_hint)
) {
    val onChange = LocalOnPeriodicVoiceRepeatChange.current
    val controlsEnabled = settingsControlsEnabled() && enabled
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onChange(kind, it) },
            enabled = controlsEnabled
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = accentColor
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RxssSharedAlertHint(
    text: String,
    accentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = accentColor
    )
}

@Composable
fun RepeatablePreviewTestButton(
    enabled: Boolean,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier,
    repeatIntervalMs: Long? = null,
) {
    if (repeatIntervalMs == null) {
        OutlinedButton(onClick = onPreview, enabled = enabled, modifier = modifier) {
            Text(text = stringResource(R.string.audio_volume_test))
        }
        return
    }

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(pressed, enabled, repeatIntervalMs) {
        if (!pressed || !enabled) return@LaunchedEffect
        onPreview()
        while (pressed && enabled) {
            delay(repeatIntervalMs.coerceAtLeast(50L))
            if (pressed && enabled) {
                onPreview()
            }
        }
    }

    OutlinedButton(
        onClick = {},
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier.semantics {
            role = Role.Button
            onClick(label = null) {
                onPreview()
                true
            }
        },
    ) {
        Text(text = stringResource(R.string.audio_volume_test))
    }
}

@Composable
fun RxssVolumeSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    previewEnabled: Boolean,
    onPreview: () -> Unit,
    previewEnabledOverride: Boolean = previewEnabled,
    previewRepeatIntervalMs: Long? = null,
    accentColor: Color = MaterialTheme.colorScheme.onSurface
) {
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
                text = stringResource(R.string.audio_volume_percent, (value * 100f).roundToInt()),
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                fontWeight = FontWeight.Medium
            )
            RepeatablePreviewTestButton(
                enabled = previewEnabledOverride,
                onPreview = onPreview,
                repeatIntervalMs = previewRepeatIntervalMs,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Slider(
            value = value.coerceIn(AudioVolumeSettings.MIN_VOLUME, AudioVolumeSettings.MAX_VOLUME),
            onValueChange = onValueChange,
            valueRange = AudioVolumeSettings.MIN_VOLUME..AudioVolumeSettings.MAX_VOLUME,
            steps = 19,
            enabled = settingsControlsEnabled()
        )
    }
}

@Composable
fun RxssVoiceAnnouncementOption(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    title: String,
    hint: String? = null,
    volumeLabel: String,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    previewEnabled: Boolean,
    onPreviewVoice: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.onSurface,
    showVolumeControlsWhenDisabled: Boolean = true,
    previewRequiresEnabled: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            enabled = settingsControlsEnabled()
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = accentColor
            )
            if (!hint.isNullOrBlank()) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    if (enabled || showVolumeControlsWhenDisabled) {
        RxssVolumeSlider(
            label = volumeLabel,
            value = volume,
            onValueChange = onVolumeChange,
            previewEnabled = previewEnabled,
            previewEnabledOverride = previewEnabled &&
                volume > 0f &&
                (!previewRequiresEnabled || enabled),
            onPreview = onPreviewVoice,
            accentColor = accentColor
        )
    }
}

@Composable
fun NoSignalAlertToneVolumeControl(
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    accentColor: Color,
    onNoSignalToneVolumeChange: (Float) -> Unit,
    onPreviewNoSignalTone: () -> Unit
) {
    RxssVolumeSlider(
        label = stringResource(R.string.audio_volume_no_signal),
        value = audioVolumes.noSignalToneVolume,
        onValueChange = onNoSignalToneVolumeChange,
        previewEnabled = previewEnabled,
        onPreview = onPreviewNoSignalTone,
        accentColor = accentColor
    )
}

@Composable
fun NoSignalVoiceAnnouncementControls(
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    accentColor: Color,
    onNoSignalVibrationEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceVolumeChange: (Float) -> Unit,
    onPreviewNoSignalVoice: () -> Unit
) {
    RxssVoiceAnnouncementOption(
        enabled = audioVolumes.noSignalVoiceEnabled,
        onEnabledChange = onNoSignalVoiceEnabledChange,
        title = stringResource(R.string.audio_no_signal_voice_enabled),
        volumeLabel = stringResource(R.string.audio_volume_no_signal_voice),
        volume = audioVolumes.noSignalVoiceVolume,
        onVolumeChange = onNoSignalVoiceVolumeChange,
        previewEnabled = previewEnabled,
        onPreviewVoice = onPreviewNoSignalVoice,
        accentColor = accentColor
    )
    PeriodicVoiceRepeatOption(
        kind = PeriodicVoiceRepeat.NO_SIGNAL,
        checked = audioVolumes.noSignalPeriodicVoiceEnabled,
        enabled = audioVolumes.noSignalVoiceEnabled,
        accentColor = accentColor
    )
    VoicePhraseToggles(
        group = VoicePhraseGroup.NO_SIGNAL,
        phrases = audioVolumes.noSignalPhrases,
        previewEnabled = previewEnabled,
        accentColor = accentColor
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = audioVolumes.noSignalVibrationEnabled,
            onCheckedChange = onNoSignalVibrationEnabledChange,
            enabled = settingsControlsEnabled()
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.audio_no_signal_vibration_enabled),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = accentColor
            )
        }
    }
}

@Composable
fun NoSignalVoiceAlertControls(
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    accentColor: Color,
    onNoSignalToneVolumeChange: (Float) -> Unit,
    onNoSignalVibrationEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceVolumeChange: (Float) -> Unit,
    onPreviewNoSignalTone: () -> Unit,
    onPreviewNoSignalVoice: () -> Unit
) {
    NoSignalAlertToneVolumeControl(
        audioVolumes = audioVolumes,
        previewEnabled = previewEnabled,
        accentColor = accentColor,
        onNoSignalToneVolumeChange = onNoSignalToneVolumeChange,
        onPreviewNoSignalTone = onPreviewNoSignalTone
    )
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

@Composable
fun Tier5SignalLowVoiceControls(
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    accentColor: Color,
    onTier5AnnouncerEnabledChange: (Boolean) -> Unit,
    onTier5AnnouncerVolumeChange: (Float) -> Unit,
    onPreviewTier5Announcer: () -> Unit,
    enabledTitle: String = stringResource(R.string.audio_tier5_announcer_enabled),
    volumeLabel: String = stringResource(R.string.audio_volume_tier5_announcer)
) {
    RxssVoiceAnnouncementOption(
        enabled = audioVolumes.tier5AnnouncerEnabled,
        onEnabledChange = onTier5AnnouncerEnabledChange,
        title = enabledTitle,
        volumeLabel = volumeLabel,
        volume = audioVolumes.tier5AnnouncerVolume,
        onVolumeChange = onTier5AnnouncerVolumeChange,
        previewEnabled = previewEnabled,
        onPreviewVoice = onPreviewTier5Announcer,
        accentColor = accentColor
    )
    PeriodicVoiceRepeatOption(
        kind = PeriodicVoiceRepeat.SIGNAL_LOW,
        checked = audioVolumes.tier5PeriodicVoiceEnabled,
        enabled = audioVolumes.tier5AnnouncerEnabled,
        accentColor = accentColor
    )
    VoicePhraseToggles(
        group = VoicePhraseGroup.SIGNAL_LOW,
        phrases = audioVolumes.signalLowPhrases,
        previewEnabled = previewEnabled,
        accentColor = accentColor
    )
}

@Composable
fun CellChangeAlertControls(
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    accentColor: Color,
    onCellChangeBellVolumeChange: (Float) -> Unit,
    onCellChangeVoiceEnabledChange: (Boolean) -> Unit,
    onCellChangeVoiceVolumeChange: (Float) -> Unit,
    onPreviewCellChangeBell: () -> Unit,
    onPreviewCellChangeVoice: () -> Unit,
    onCellChangeSpeakBandEnabledChange: (Boolean) -> Unit = {},
    onCellChangeBandNamingStyleChange: (CellReselectBandNamingStyle) -> Unit = {}
) {
    RxssVolumeSlider(
        label = stringResource(R.string.audio_volume_cell_change_bell),
        value = audioVolumes.cellChangeBellVolume,
        onValueChange = onCellChangeBellVolumeChange,
        previewEnabled = previewEnabled,
        onPreview = onPreviewCellChangeBell,
        accentColor = accentColor
    )
    RxssVoiceAnnouncementOption(
        enabled = audioVolumes.cellChangeVoiceEnabled,
        onEnabledChange = onCellChangeVoiceEnabledChange,
        title = stringResource(R.string.audio_cell_change_voice_enabled),
        volumeLabel = stringResource(R.string.audio_volume_cell_change_voice),
        volume = audioVolumes.cellChangeVoiceVolume,
        onVolumeChange = onCellChangeVoiceVolumeChange,
        previewEnabled = previewEnabled,
        onPreviewVoice = onPreviewCellChangeVoice,
        accentColor = accentColor
    )
    VoicePhraseToggles(
        group = VoicePhraseGroup.CELL_CHANGE,
        phrases = audioVolumes.cellChangePhrases,
        previewEnabled = previewEnabled,
        accentColor = accentColor
    )
    if (audioVolumes.cellChangeVoiceEnabled) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = audioVolumes.cellChangeSpeakBandEnabled,
                onCheckedChange = onCellChangeSpeakBandEnabledChange,
                enabled = settingsControlsEnabled()
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.audio_cell_change_speak_band_enabled),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = accentColor
                )
            }
        }
        if (audioVolumes.cellChangeSpeakBandEnabled) {
            CellChangeBandNamingStyleOption(
                selected = CellReselectBandNamingStyle.BAND_NUMBER,
                current = audioVolumes.cellChangeBandNamingStyle,
                label = stringResource(R.string.audio_cell_change_band_naming_number),
                onSelect = onCellChangeBandNamingStyleChange
            )
            CellChangeBandNamingStyleOption(
                selected = CellReselectBandNamingStyle.MHZ_NICKNAME,
                current = audioVolumes.cellChangeBandNamingStyle,
                label = stringResource(R.string.audio_cell_change_band_naming_mhz),
                onSelect = onCellChangeBandNamingStyleChange
            )
        }
    }
}

@Composable
private fun CellChangeBandNamingStyleOption(
    selected: CellReselectBandNamingStyle,
    current: CellReselectBandNamingStyle,
    label: String,
    onSelect: (CellReselectBandNamingStyle) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = settingsControlsEnabled()) { onSelect(selected) },
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(
            selected = current == selected,
            onClick = { onSelect(selected) },
            enabled = settingsControlsEnabled()
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun TechnologyChangeAlertControls(
    target: TechnologyChangeTarget,
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    accentColor: Color,
    voiceEnabledTitle: String,
    voiceVolumeLabel: String,
    onToneVolumeChange: (Float) -> Unit,
    onVoiceEnabledChange: (Boolean) -> Unit,
    onVoiceVolumeChange: (Float) -> Unit,
    onPreviewTone: () -> Unit,
    onPreviewVoice: () -> Unit
) {
    val alertVolumes = audioVolumes.technologyChangeAlertVolumes(target)
    RxssVolumeSlider(
        label = stringResource(R.string.audio_volume_technology_change),
        value = alertVolumes.toneVolume,
        onValueChange = onToneVolumeChange,
        previewEnabled = previewEnabled,
        onPreview = onPreviewTone,
        accentColor = accentColor
    )
    RxssVoiceAnnouncementOption(
        enabled = alertVolumes.voiceEnabled,
        onEnabledChange = onVoiceEnabledChange,
        title = voiceEnabledTitle,
        volumeLabel = voiceVolumeLabel,
        volume = alertVolumes.voiceVolume,
        onVolumeChange = onVoiceVolumeChange,
        previewEnabled = previewEnabled,
        onPreviewVoice = onPreviewVoice,
        accentColor = accentColor
    )
    if (target == TechnologyChangeTarget.TO_2G) {
        PeriodicVoiceRepeatOption(
            kind = PeriodicVoiceRepeat.G2_CAMPED,
            checked = audioVolumes.technologyChangeTo2gPeriodicVoiceEnabled,
            enabled = alertVolumes.voiceEnabled,
            accentColor = accentColor,
            title = stringResource(R.string.audio_g2_camped_periodic_voice_enabled),
            hint = stringResource(R.string.audio_g2_camped_periodic_voice_enabled_hint)
        )
    }
    VoicePhraseToggles(
        group = VoicePhraseGroup.forTechnologyChange(target),
        phrases = audioVolumes.phrasesForTechnologyChange(target),
        previewEnabled = previewEnabled,
        accentColor = accentColor
    )
}

@Composable
fun LimitedServiceAlertToneVolumeControl(
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    accentColor: Color,
    onLimitedServiceToneVolumeChange: (Float) -> Unit,
    onPreviewLimitedServiceTone: () -> Unit
) {
    RxssVolumeSlider(
        label = stringResource(R.string.audio_volume_limited_service),
        value = audioVolumes.limitedServiceToneVolume,
        onValueChange = onLimitedServiceToneVolumeChange,
        previewEnabled = previewEnabled,
        onPreview = onPreviewLimitedServiceTone,
        accentColor = accentColor
    )
}

@Composable
fun LimitedServiceVoiceAnnouncementControls(
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    accentColor: Color,
    onLimitedServiceVoiceEnabledChange: (Boolean) -> Unit,
    onLimitedServiceVoiceVolumeChange: (Float) -> Unit,
    onPreviewLimitedServiceVoice: () -> Unit
) {
    RxssVoiceAnnouncementOption(
        enabled = audioVolumes.limitedServiceVoiceEnabled,
        onEnabledChange = onLimitedServiceVoiceEnabledChange,
        title = stringResource(R.string.audio_limited_service_voice_enabled),
        volumeLabel = stringResource(R.string.audio_volume_limited_service_voice),
        volume = audioVolumes.limitedServiceVoiceVolume,
        onVolumeChange = onLimitedServiceVoiceVolumeChange,
        previewEnabled = previewEnabled,
        onPreviewVoice = onPreviewLimitedServiceVoice,
        accentColor = accentColor
    )
    PeriodicVoiceRepeatOption(
        kind = PeriodicVoiceRepeat.LIMITED_SERVICE,
        checked = audioVolumes.limitedServicePeriodicVoiceEnabled,
        enabled = audioVolumes.limitedServiceVoiceEnabled,
        accentColor = accentColor
    )
    VoicePhraseToggles(
        group = VoicePhraseGroup.LIMITED_SERVICE,
        phrases = audioVolumes.limitedServicePhrases,
        previewEnabled = previewEnabled,
        accentColor = accentColor
    )
}

@Composable
fun LimitedServiceAlertControls(
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    accentColor: Color,
    onLimitedServiceToneVolumeChange: (Float) -> Unit,
    onLimitedServiceVoiceEnabledChange: (Boolean) -> Unit,
    onLimitedServiceVoiceVolumeChange: (Float) -> Unit,
    onPreviewLimitedServiceTone: () -> Unit,
    onPreviewLimitedServiceVoice: () -> Unit
) {
    LimitedServiceAlertToneVolumeControl(
        audioVolumes = audioVolumes,
        previewEnabled = previewEnabled,
        accentColor = accentColor,
        onLimitedServiceToneVolumeChange = onLimitedServiceToneVolumeChange,
        onPreviewLimitedServiceTone = onPreviewLimitedServiceTone
    )
    LimitedServiceVoiceAnnouncementControls(
        audioVolumes = audioVolumes,
        previewEnabled = previewEnabled,
        accentColor = accentColor,
        onLimitedServiceVoiceEnabledChange = onLimitedServiceVoiceEnabledChange,
        onLimitedServiceVoiceVolumeChange = onLimitedServiceVoiceVolumeChange,
        onPreviewLimitedServiceVoice = onPreviewLimitedServiceVoice
    )
}
