package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerChoice
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerOption
import io.github.cloolalang.notspotdetector.ui.theme.Sushi
import kotlin.math.roundToInt

/**
 * "Global voice settings" panel — voice announcer selection (which TTS voice is used).
 * Per-alert operator / technology / band phrase toggles live on each RXSS that has a VA.
 */
@Composable
fun GlobalVoiceSettingsCard(
    voiceAnnouncerChoice: VoiceAnnouncerChoice,
    voiceAnnouncerOptions: List<VoiceAnnouncerOption>,
    voiceSpeechRate: Float,
    previewEnabled: Boolean,
    onVoiceAnnouncerChoiceChange: (VoiceAnnouncerChoice) -> Unit,
    onVoiceSpeechRateChange: (Float) -> Unit,
    onRefreshVoiceAnnouncerOptions: () -> Unit,
    onPreviewVoiceAnnouncer: () -> Unit,
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
                    text = stringResource(R.string.global_voice_settings_title),
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
                    text = stringResource(R.string.global_voice_settings_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LaunchedEffect(Unit) {
                    onRefreshVoiceAnnouncerOptions()
                }

                VoiceAnnouncerSelector(
                    selectedChoice = voiceAnnouncerChoice,
                    options = voiceAnnouncerOptions,
                    previewEnabled = previewEnabled,
                    onChoiceChange = onVoiceAnnouncerChoiceChange,
                    onPreview = onPreviewVoiceAnnouncer
                )
                VoiceSpeedSlider(
                    rate = voiceSpeechRate,
                    onRateChange = onVoiceSpeechRateChange
                )
            }
        }
    }
}

@Composable
private fun VoiceSpeedSlider(
    rate: Float,
    onRateChange: (Float) -> Unit
) {
    val min = AudioVolumeSettings.MIN_VOICE_SPEECH_RATE
    val max = AudioVolumeSettings.MAX_VOICE_SPEECH_RATE
    val step = AudioVolumeSettings.VOICE_SPEECH_RATE_STEP
    val snapped = AudioVolumeSettings.snapVoiceSpeechRate(rate)
    val steps = ((max - min) / step).roundToInt() - 1

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.global_voice_speed),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.global_voice_speed_value, snapped),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = stringResource(R.string.global_voice_speed_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            enabled = settingsControlsEnabled(),
            value = snapped,
            onValueChange = { onRateChange(AudioVolumeSettings.snapVoiceSpeechRate(it)) },
            valueRange = min..max,
            steps = steps.coerceAtLeast(0)
        )
    }
}
