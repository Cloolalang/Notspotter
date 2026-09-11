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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.ProfileExportOutcome
import io.github.cloolalang.notspotdetector.model.ProfileExportResult
import io.github.cloolalang.notspotdetector.model.ProfileImportResult
import io.github.cloolalang.notspotdetector.model.ProfileSaveResult
import io.github.cloolalang.notspotdetector.model.SettingsProfileSummary
import io.github.cloolalang.notspotdetector.ui.theme.Sushi
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsProfilesCard(
    profiles: List<SettingsProfileSummary>,
    onSaveProfile: (String) -> ProfileSaveResult,
    onLoadProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onImportProfile: (onResult: (ProfileImportResult) -> Unit) -> Unit,
    onShareProfile: (String) -> Unit,
    onExportProfileToDownloads: (String) -> ProfileExportOutcome,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var profileName by rememberSaveable { mutableStateOf("") }
    var lastSaveResult by rememberSaveable { mutableStateOf<ProfileSaveResult?>(null) }
    var lastImportResult by rememberSaveable { mutableStateOf<ProfileImportResult?>(null) }
    var lastExportOutcome by remember { mutableStateOf<ProfileExportOutcome?>(null) }

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
                    text = stringResource(R.string.settings_profiles_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = profileName,
                    onValueChange = {
                        profileName = it
                        lastSaveResult = null
                    },
                    enabled = settingsControlsEnabled(),
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.settings_profiles_name_label)) },
                    singleLine = true
                )

                OutlinedButton(
                    enabled = settingsControlsEnabled(),
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

                OutlinedButton(
                    enabled = settingsControlsEnabled(),
                    onClick = {
                        lastImportResult = null
                        onImportProfile { result ->
                            lastImportResult = result
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.settings_profiles_import))
                }

                importResultMessage(lastImportResult)?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (lastImportResult == ProfileImportResult.Imported) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }

                lastExportOutcome?.let { outcome ->
                    Text(
                        text = exportResultMessage(outcome),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (outcome.result == ProfileExportResult.Exported) {
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
                            onShare = { onShareProfile(profile.id) },
                            onExportToDownloads = {
                                lastExportOutcome = onExportProfileToDownloads(profile.id)
                            },
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
        ProfileSaveResult.Failed -> stringResource(R.string.settings_profiles_error_save_failed)
        null -> null
    }
}

@Composable
private fun importResultMessage(result: ProfileImportResult?): String? {
    return when (result) {
        ProfileImportResult.Imported -> stringResource(R.string.settings_profiles_imported)
        ProfileImportResult.InvalidFile -> stringResource(R.string.settings_profiles_error_invalid_file)
        ProfileImportResult.TooManyProfiles -> stringResource(R.string.settings_profiles_error_too_many)
        ProfileImportResult.Failed -> stringResource(R.string.settings_profiles_error_import_failed)
        null -> null
    }
}

@Composable
private fun exportResultMessage(outcome: ProfileExportOutcome): String {
    return when (outcome.result) {
        ProfileExportResult.Exported -> {
            val path = outcome.relativePath
            if (path.isNullOrBlank()) {
                stringResource(R.string.settings_profiles_exported)
            } else {
                stringResource(R.string.settings_profiles_exported_to, path)
            }
        }
        ProfileExportResult.ProfileNotFound -> stringResource(R.string.settings_profiles_error_export_not_found)
        ProfileExportResult.Failed -> stringResource(R.string.settings_profiles_error_export_failed)
    }
}

@Composable
private fun ProfileRow(
    profile: SettingsProfileSummary,
    savedAtLabel: String,
    onLoad: () -> Unit,
    onShare: () -> Unit,
    onExportToDownloads: () -> Unit,
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
                enabled = settingsControlsEnabled(),
                onClick = onLoad,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.settings_profiles_load))
            }
            OutlinedButton(
                enabled = settingsControlsEnabled(),
                onClick = onShare,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.settings_profiles_share))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                enabled = settingsControlsEnabled(),
                onClick = onExportToDownloads,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.settings_profiles_export_downloads))
            }
            OutlinedButton(
                enabled = settingsControlsEnabled(),
                onClick = onDelete,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.settings_profiles_delete))
            }
        }
    }
}
