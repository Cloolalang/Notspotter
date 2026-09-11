package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerChoice
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerOption
import io.github.cloolalang.notspotdetector.ui.theme.Sushi

/**
 * "Global voice settings" panel — combines voice announcer selection (which TTS voice is used)
 * with the app-wide options that apply to every spoken announcement (cell reselect, technology
 * change, no-signal, limited service, tier 5, dead zone, searching 2G, 2G camped). Formerly split
 * across the voice-announcer picker in "Alert sound settings" and the standalone
 * "VA — Voice announcement preferences" panel; both now live here.
 */
@Composable
fun GlobalVoiceSettingsCard(
    voiceAnnouncerChoice: VoiceAnnouncerChoice,
    voiceAnnouncerOptions: List<VoiceAnnouncerOption>,
    previewEnabled: Boolean,
    speakOperatorNameEnabled: Boolean,
    speakTechnologyEnabled: Boolean,
    onVoiceAnnouncerChoiceChange: (VoiceAnnouncerChoice) -> Unit,
    onRefreshVoiceAnnouncerOptions: () -> Unit,
    onPreviewVoiceAnnouncer: () -> Unit,
    onSpeakOperatorNameEnabledChange: (Boolean) -> Unit,
    onSpeakTechnologyEnabledChange: (Boolean) -> Unit,
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

                VoiceAnnouncementPreferenceOption(
                    checked = speakOperatorNameEnabled,
                    onCheckedChange = onSpeakOperatorNameEnabledChange,
                    title = stringResource(R.string.voice_announcement_speak_operator_name),
                    hint = stringResource(R.string.voice_announcement_speak_operator_name_hint)
                )
                VoiceAnnouncementPreferenceOption(
                    checked = speakTechnologyEnabled,
                    onCheckedChange = onSpeakTechnologyEnabledChange,
                    title = stringResource(R.string.voice_announcement_speak_technology),
                    hint = stringResource(R.string.voice_announcement_speak_technology_hint)
                )
            }
        }
    }
}

@Composable
private fun VoiceAnnouncementPreferenceOption(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    hint: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
