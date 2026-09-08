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
import androidx.compose.material3.OutlinedTextField
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
import io.github.cloolalang.notspotdetector.model.ProfileSaveResult
import io.github.cloolalang.notspotdetector.model.SettingsProfileSummary
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsProfilesCard(
    profiles: List<SettingsProfileSummary>,
    onSaveProfile: (String) -> ProfileSaveResult,
    onLoadProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var profileName by rememberSaveable { mutableStateOf("") }
    var lastSaveResult by rememberSaveable { mutableStateOf<ProfileSaveResult?>(null) }

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
                    text = stringResource(R.string.settings_profiles_title),
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
                text = stringResource(R.string.settings_profiles_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (expanded) {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = {
                        profileName = it
                        lastSaveResult = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.settings_profiles_name_label)) },
                    singleLine = true
                )

                OutlinedButton(
                    onClick = {
                        val result = onSaveProfile(profileName)
                        lastSaveResult = result
                        if (result == ProfileSaveResult.Saved) {
                            profileName = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.settings_profiles_save))
                }

                saveResultMessage(lastSaveResult)?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (lastSaveResult == ProfileSaveResult.Saved) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }

                if (profiles.isEmpty()) {
                    Text(
                        text = stringResource(R.string.settings_profiles_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    profiles.forEach { profile ->
                        ProfileRow(
                            profile = profile,
                            savedAtLabel = dateFormat.format(Date(profile.savedAtMs)),
                            onLoad = { onLoadProfile(profile.id) },
                            onDelete = { onDeleteProfile(profile.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun saveResultMessage(result: ProfileSaveResult?): String? {
    return when (result) {
        ProfileSaveResult.Saved -> stringResource(R.string.settings_profiles_saved)
        ProfileSaveResult.EmptyName -> stringResource(R.string.settings_profiles_error_empty_name)
        ProfileSaveResult.DuplicateName -> stringResource(R.string.settings_profiles_error_duplicate_name)
        ProfileSaveResult.MatchesDefaults -> stringResource(R.string.settings_profiles_error_matches_defaults)
        ProfileSaveResult.TooManyProfiles -> stringResource(R.string.settings_profiles_error_too_many)
        ProfileSaveResult.NameTooLong -> stringResource(R.string.settings_profiles_error_name_too_long)
        null -> null
    }
}

@Composable
private fun ProfileRow(
    profile: SettingsProfileSummary,
    savedAtLabel: String,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = profile.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = stringResource(R.string.settings_profiles_saved_at, savedAtLabel),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onLoad,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.settings_profiles_load))
            }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.settings_profiles_delete))
            }
        }
    }
}
