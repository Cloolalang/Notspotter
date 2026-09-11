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
import io.github.cloolalang.notspotdetector.ui.theme.Sushi
import kotlin.math.roundToInt

@Composable
fun AudioVolumeSettingsCard(
    audioVolumes: AudioVolumeSettings,
    previewEnabled: Boolean,
    signalPulsePreviewRepeatIntervalMs: Long,
    onPingClickVolumeChange: (Float) -> Unit,
    onLowSignalClickVolumeChange: (Float) -> Unit,
    onSignalPulseFrequencyChange: (Int) -> Unit,
    onSignalPulseDurationChange: (Int) -> Unit,
    onPreviewPingClick: () -> Unit,
    onPreviewLowSignalClick: (frequencyHz: Int, pulseDurationMs: Int) -> Unit,
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
                Text(
                    text = stringResource(R.string.audio_volume_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!previewEnabled) {
                    Text(
                        text = stringResource(R.string.audio_volume_test_disabled_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                VolumeSlider(
                    label = stringResource(R.string.audio_volume_ping_clicks),
                    value = audioVolumes.pingClickVolume,
                    onValueChange = onPingClickVolumeChange,
                    previewEnabled = previewEnabled,
                    onPreview = onPreviewPingClick
                )
                VolumeSlider(
                    label = stringResource(R.string.audio_volume_signal_pulse),
                    value = audioVolumes.lowSignalClickVolume,
                    onValueChange = onLowSignalClickVolumeChange,
                    previewEnabled = previewEnabled,
                    onPreview = {
                        onPreviewLowSignalClick(
                            audioVolumes.signalPulseFrequencyHz,
                            audioVolumes.signalPulseDurationMs
                        )
                    },
                    previewRepeatIntervalMs = signalPulsePreviewRepeatIntervalMs
                )
                FrequencySlider(
                    label = stringResource(R.string.audio_signal_pulse_frequency),
                    frequencyHz = audioVolumes.signalPulseFrequencyHz,
                    onValueChange = onSignalPulseFrequencyChange
                )
                DurationSlider(
                    label = stringResource(R.string.audio_signal_pulse_duration),
                    valueMs = audioVolumes.signalPulseDurationMs,
                    hint = stringResource(R.string.audio_signal_pulse_duration_hint),
                    onValueChange = onSignalPulseDurationChange
                )
                OutlinedButton(
                    onClick = onReset,
                    enabled = settingsControlsEnabled(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.audio_volume_reset))
                }
            }
        }
    }
}

@Composable
private fun FrequencySlider(
    label: String,
    frequencyHz: Int,
    onValueChange: (Int) -> Unit
) {
    val minHz = AudioVolumeSettings.MIN_SIGNAL_PULSE_FREQUENCY_HZ
    val maxHz = AudioVolumeSettings.MAX_SIGNAL_PULSE_FREQUENCY_HZ
    val stepHz = AudioVolumeSettings.SIGNAL_PULSE_FREQUENCY_STEP_HZ
    val minStep = minHz / stepHz
    val maxStep = maxHz / stepHz
    val step = (frequencyHz / stepHz).coerceIn(minStep, maxStep)

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
                text = stringResource(R.string.audio_signal_pulse_frequency_value, frequencyHz),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            enabled = settingsControlsEnabled(),
            value = step.toFloat(),
            onValueChange = { onValueChange(it.roundToInt() * stepHz) },
            valueRange = minStep.toFloat()..maxStep.toFloat(),
            steps = maxStep - minStep - 1
        )
    }
}

@Composable
private fun DurationSlider(
    label: String,
    valueMs: Int,
    hint: String? = null,
    onValueChange: (Int) -> Unit
) {
    val minMs = AudioVolumeSettings.MIN_SIGNAL_PULSE_DURATION_MS
    val maxMs = AudioVolumeSettings.MAX_SIGNAL_PULSE_DURATION_MS
    val stepMs = AudioVolumeSettings.SIGNAL_PULSE_DURATION_STEP_MS
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
        }
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            enabled = settingsControlsEnabled(),
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
    onPreview: () -> Unit,
    previewEnabledOverride: Boolean = previewEnabled,
    previewRepeatIntervalMs: Long? = null,
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
            RepeatablePreviewTestButton(
                enabled = previewEnabledOverride,
                onPreview = onPreview,
                repeatIntervalMs = previewRepeatIntervalMs,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Slider(
            enabled = settingsControlsEnabled(),
            value = value.coerceIn(AudioVolumeSettings.MIN_VOLUME, AudioVolumeSettings.MAX_VOLUME),
            onValueChange = onValueChange,
            valueRange = AudioVolumeSettings.MIN_VOLUME..AudioVolumeSettings.MAX_VOLUME,
            steps = 19
        )
    }
}
