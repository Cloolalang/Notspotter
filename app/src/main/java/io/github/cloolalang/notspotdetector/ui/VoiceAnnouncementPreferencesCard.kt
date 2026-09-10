package io.github.cloolalang.notspotdetector.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.ui.theme.Sushi

/**
 * Global VA (voice announcement) panel — two app-wide toggles that apply to every spoken
 * announcement (cell reselect, technology change, no-signal, limited service, tier 5, dead zone,
 * searching 2G, 2G camped), not just a single RXSS tier. See [io.github.cloolalang.notspotdetector.model.AudioVolumeSettings.speakOperatorNameEnabled]
 * and [io.github.cloolalang.notspotdetector.model.AudioVolumeSettings.speakTechnologyEnabled].
 */
@Composable
fun VoiceAnnouncementPreferencesCard(
    speakOperatorNameEnabled: Boolean,
    speakTechnologyEnabled: Boolean,
    onSpeakOperatorNameEnabledChange: (Boolean) -> Unit,
    onSpeakTechnologyEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.voice_announcement_preferences_title),
                style = MaterialTheme.typography.titleSmall,
                color = Sushi,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.voice_announcement_preferences_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
