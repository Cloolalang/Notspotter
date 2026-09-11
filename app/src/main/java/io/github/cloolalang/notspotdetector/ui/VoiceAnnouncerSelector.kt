package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerChoice
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerOption

@Composable
fun VoiceAnnouncerSelector(
    selectedChoice: VoiceAnnouncerChoice,
    options: List<VoiceAnnouncerOption>,
    previewEnabled: Boolean,
    onChoiceChange: (VoiceAnnouncerChoice) -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val optionByChoice = remember(options) { options.associateBy { it.choice } }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.audio_voice_announcer_title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = stringResource(R.string.audio_voice_announcer_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            VoiceAnnouncerChoice.selectableChoices.forEach { choice ->
                val option = optionByChoice[choice]
                VoiceRadioOption(
                    selected = selectedChoice == choice,
                    label = voiceAnnouncerChoiceLabel(choice, option),
                    onSelect = { onChoiceChange(choice) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPreview,
                enabled = previewEnabled
            ) {
                Text(text = stringResource(R.string.audio_volume_test))
            }
        }
    }
}

@Composable
private fun VoiceRadioOption(
    selected: Boolean,
    label: String,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = settingsControlsEnabled(), onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            enabled = settingsControlsEnabled()
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun voiceAnnouncerChoiceLabel(
    choice: VoiceAnnouncerChoice,
    option: VoiceAnnouncerOption?
): String {
    val baseLabel = stringResource(
        when (choice) {
            VoiceAnnouncerChoice.SYSTEM_DEFAULT -> R.string.audio_voice_announcer_system
            VoiceAnnouncerChoice.MALE_1 -> R.string.audio_voice_announcer_male_1
            VoiceAnnouncerChoice.MALE_2 -> R.string.audio_voice_announcer_male_2
            VoiceAnnouncerChoice.MALE_3 -> R.string.audio_voice_announcer_male_3
            VoiceAnnouncerChoice.FEMALE_1 -> R.string.audio_voice_announcer_female_1
            VoiceAnnouncerChoice.FEMALE_2 -> R.string.audio_voice_announcer_female_2
            VoiceAnnouncerChoice.FEMALE_3 -> R.string.audio_voice_announcer_female_3
        }
    )
    val engineName = option?.engineVoiceName
    return when {
        engineName.isNullOrBlank() -> baseLabel
        option.available -> stringResource(R.string.audio_voice_announcer_named, baseLabel, engineName)
        else -> stringResource(R.string.audio_voice_announcer_unavailable, baseLabel)
    }
}
