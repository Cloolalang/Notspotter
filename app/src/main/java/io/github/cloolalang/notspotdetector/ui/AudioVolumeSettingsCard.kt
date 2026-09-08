package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import kotlin.math.roundToInt

@Composable
fun AudioVolumeSettingsCard(
    audioVolumes: AudioVolumeSettings,
    passiveSoundSpeed: Int,
    previewEnabled: Boolean,
    onPingClickVolumeChange: (Float) -> Unit,
    onLowSignalClickVolumeChange: (Float) -> Unit,
    onSignalPulseDurationChange: (Int) -> Unit,
    onCellChangeBellVolumeChange: (Float) -> Unit,
    onTechnologyChangeVolumeChange: (Float) -> Unit,
    onNoSignalToneVolumeChange: (Float) -> Unit,
    onLimitedServiceToneVolumeChange: (Float) -> Unit,
    onPassiveSoundSpeedChange: (Int) -> Unit,
    onPreviewPingClick: () -> Unit,
    onPreviewLowSignalClick: () -> Unit,
    onPreviewCellChangeBell: () -> Unit,
    onPreviewTechnologyChange: () -> Unit,
    onPreviewNoSignalTone: () -> Unit,
    onPreviewLimitedServiceTone: () -> Unit,
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
                    text = stringResource(R.string.audio_volume_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = stringResource(R.string.audio_volume_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (expanded) {
                if (!previewEnabled) {
                    Text(
                        text = stringResource(R.string.audio_volume_test_disabled_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                PassiveSoundSpeedSlider(
                    speed = passiveSoundSpeed,
                    onSpeedChange = onPassiveSoundSpeedChange
                )

                VolumeSlider(
                    label = stringResource(R.string.audio_volume_ping_clicks),
                    value = audioVolumes.pingClickVolume,
                    onValueChange = onPingClickVolumeChange,
                    previewEnabled = previewEnabled,
                    onPreview = onPreviewPingClick
                )
                VolumeSlider(
                    label = stringResource(R.string.audio_volume_signal_strength),
                    value = audioVolumes.lowSignalClickVolume,
                    onValueChange = onLowSignalClickVolumeChange,
                    previewEnabled = previewEnabled,
                    onPreview = onPreviewLowSignalClick
                )
                DurationSlider(
                    label = stringResource(R.string.audio_signal_pulse_duration),
                    valueMs = audioVolumes.signalPulseDurationMs,
                    onValueChange = onSignalPulseDurationChange,
                    previewEnabled = previewEnabled,
                    onPreview = onPreviewLowSignalClick
                )
                VolumeSlider(
                    label = stringResource(R.string.audio_volume_cell_change_bell),
                    value = audioVolumes.cellChangeBellVolume,
                    onValueChange = onCellChangeBellVolumeChange,
                    previewEnabled = previewEnabled,
                    onPreview = onPreviewCellChangeBell
                )
                VolumeSlider(
                    label = stringResource(R.string.audio_volume_technology_change),
                    value = audioVolumes.technologyChangeVolume,
                    onValueChange = onTechnologyChangeVolumeChange,
                    previewEnabled = previewEnabled,
                    onPreview = onPreviewTechnologyChange
                )
                VolumeSlider(
                    label = stringResource(R.string.audio_volume_no_signal),
                    value = audioVolumes.noSignalToneVolume,
                    onValueChange = onNoSignalToneVolumeChange,
                    previewEnabled = previewEnabled,
                    onPreview = onPreviewNoSignalTone
                )
                VolumeSlider(
                    label = stringResource(R.string.audio_volume_limited_service),
                    value = audioVolumes.limitedServiceToneVolume,
                    onValueChange = onLimitedServiceToneVolumeChange,
                    previewEnabled = previewEnabled,
                    onPreview = onPreviewLimitedServiceTone
                )

                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.audio_volume_reset))
                }
            }
        }
    }
}

@Composable
private fun PassiveSoundSpeedSlider(
    speed: Int,
    onSpeedChange: (Int) -> Unit
) {
    val minSpeed = MonitoringSettings.MIN_PASSIVE_SOUND_SPEED
    val maxSpeed = MonitoringSettings.MAX_PASSIVE_SOUND_SPEED
    val value = speed.coerceIn(minSpeed, maxSpeed)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.passive_sound_speed_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.passive_sound_speed_value, value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = stringResource(R.string.passive_sound_speed_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onSpeedChange(it.roundToInt()) },
            valueRange = minSpeed.toFloat()..maxSpeed.toFloat(),
            steps = maxSpeed - minSpeed - 1
        )
    }
}

@Composable
private fun DurationSlider(
    label: String,
    valueMs: Int,
    onValueChange: (Int) -> Unit,
    previewEnabled: Boolean,
    onPreview: () -> Unit
) {
    val minMs = AudioVolumeSettings.MIN_SIGNAL_PULSE_DURATION_MS
    val maxMs = AudioVolumeSettings.MAX_SIGNAL_PULSE_DURATION_MS
    val stepMs = 10
    val steps = ((maxMs - minMs) / stepMs) - 1

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.audio_signal_pulse_duration_value, valueMs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            OutlinedButton(
                onClick = onPreview,
                enabled = previewEnabled,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(text = stringResource(R.string.audio_volume_test))
            }
        }
        Slider(
            value = valueMs.toFloat(),
            onValueChange = { raw ->
                val snapped = minMs + (((raw - minMs) / stepMs).roundToInt() * stepMs)
                onValueChange(snapped.coerceIn(minMs, maxMs))
            },
            valueRange = minMs.toFloat()..maxMs.toFloat(),
            steps = steps.coerceAtLeast(0)
        )
    }
}

@Composable
private fun VolumeSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    previewEnabled: Boolean,
    onPreview: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.audio_volume_percent, (value * 100f).roundToInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            OutlinedButton(
                onClick = onPreview,
                enabled = previewEnabled,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(text = stringResource(R.string.audio_volume_test))
            }
        }
        Slider(
            value = value.coerceIn(AudioVolumeSettings.MIN_VOLUME, AudioVolumeSettings.MAX_VOLUME),
            onValueChange = onValueChange,
            valueRange = AudioVolumeSettings.MIN_VOLUME..AudioVolumeSettings.MAX_VOLUME,
            steps = 19
        )
    }
}
