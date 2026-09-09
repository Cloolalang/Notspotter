package io.github.cloolalang.notspotdetector.ui

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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import io.github.cloolalang.notspotdetector.model.TechnologyChangeTarget
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun RxssAlertSubsectionTitle(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        text = stringResource(R.string.passive_signal_rxss_voice_alert_subsection),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = accentColor,
        modifier = modifier.padding(top = 4.dp)
    )
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
            steps = 19
        )
    }
}

@Composable
fun RxssVoiceAnnouncementOption(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    title: String,
    hint: String,
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
            onCheckedChange = onEnabledChange
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
    RxssAlertSubsectionTitle(accentColor = accentColor)
    RxssVoiceAnnouncementOption(
        enabled = audioVolumes.noSignalVoiceEnabled,
        onEnabledChange = onNoSignalVoiceEnabledChange,
        title = stringResource(R.string.audio_no_signal_voice_enabled),
        hint = stringResource(R.string.audio_no_signal_voice_enabled_hint),
        volumeLabel = stringResource(R.string.audio_volume_no_signal_voice),
        volume = audioVolumes.noSignalVoiceVolume,
        onVolumeChange = onNoSignalVoiceVolumeChange,
        previewEnabled = previewEnabled,
        onPreviewVoice = onPreviewNoSignalVoice,
        accentColor = accentColor
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = audioVolumes.noSignalVibrationEnabled,
            onCheckedChange = onNoSignalVibrationEnabledChange
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.audio_no_signal_vibration_enabled),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = accentColor
            )
            Text(
                text = stringResource(R.string.audio_no_signal_vibration_enabled_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    enabledHint: String = stringResource(R.string.passive_signal_rxss5_voice_hint),
    volumeLabel: String = stringResource(R.string.audio_volume_tier5_announcer)
) {
    RxssAlertSubsectionTitle(accentColor = accentColor)
    RxssVoiceAnnouncementOption(
        enabled = audioVolumes.tier5AnnouncerEnabled,
        onEnabledChange = onTier5AnnouncerEnabledChange,
        title = enabledTitle,
        hint = enabledHint,
        volumeLabel = volumeLabel,
        volume = audioVolumes.tier5AnnouncerVolume,
        onVolumeChange = onTier5AnnouncerVolumeChange,
        previewEnabled = previewEnabled,
        onPreviewVoice = onPreviewTier5Announcer,
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
    onPreviewCellChangeVoice: () -> Unit
) {
    RxssAlertSubsectionTitle(accentColor = accentColor)
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
        hint = stringResource(R.string.passive_signal_rxss9_voice_hint),
        volumeLabel = stringResource(R.string.audio_volume_cell_change_voice),
        volume = audioVolumes.cellChangeVoiceVolume,
        onVolumeChange = onCellChangeVoiceVolumeChange,
        previewEnabled = previewEnabled,
        onPreviewVoice = onPreviewCellChangeVoice,
        accentColor = accentColor
    )
}

@Composable
fun TechnologyChangeAlertControls(
    target: TechnologyChangeTarget,
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    accentColor: Color,
    sectionHint: String,
    voiceEnabledTitle: String,
    voiceHint: String,
    voiceVolumeLabel: String,
    onToneVolumeChange: (Float) -> Unit,
    onVoiceEnabledChange: (Boolean) -> Unit,
    onVoiceVolumeChange: (Float) -> Unit,
    onPreviewTone: () -> Unit,
    onPreviewVoice: () -> Unit
) {
    val alertVolumes = audioVolumes.technologyChangeAlertVolumes(target)
    RxssAlertSubsectionTitle(accentColor = accentColor)
    Text(
        text = sectionHint,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
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
        hint = voiceHint,
        volumeLabel = voiceVolumeLabel,
        volume = alertVolumes.voiceVolume,
        onVolumeChange = onVoiceVolumeChange,
        previewEnabled = previewEnabled,
        onPreviewVoice = onPreviewVoice,
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
    RxssAlertSubsectionTitle(accentColor = accentColor)
    RxssVoiceAnnouncementOption(
        enabled = audioVolumes.limitedServiceVoiceEnabled,
        onEnabledChange = onLimitedServiceVoiceEnabledChange,
        title = stringResource(R.string.audio_limited_service_voice_enabled),
        hint = stringResource(R.string.audio_limited_service_voice_enabled_hint),
        volumeLabel = stringResource(R.string.audio_volume_limited_service_voice),
        volume = audioVolumes.limitedServiceVoiceVolume,
        onVolumeChange = onLimitedServiceVoiceVolumeChange,
        previewEnabled = previewEnabled,
        onPreviewVoice = onPreviewLimitedServiceVoice,
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
