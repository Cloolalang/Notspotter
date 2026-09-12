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
import androidx.compose.material3.Switch
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
import io.github.cloolalang.notspotdetector.model.SpecialCell
import io.github.cloolalang.notspotdetector.model.SpecialCellCatalog
import io.github.cloolalang.notspotdetector.model.SpecialCellMatch
import io.github.cloolalang.notspotdetector.model.SpecialCellsImportResult
import io.github.cloolalang.notspotdetector.ui.theme.Sushi

data class SpecialCellsActions(
    val onImport: (onResult: (SpecialCellsImportResult) -> Unit) -> Unit,
    val onRestoreExample: () -> Unit,
    val onDetectionEnabledChange: (Boolean) -> Unit,
    val onVoiceEnabledChange: (Boolean) -> Unit,
    val onSpeakTypeChange: (Boolean) -> Unit,
    val onSpeakSiteChange: (Boolean) -> Unit,
    val onSpeakSectorChange: (Boolean) -> Unit,
    val onPreviewVoice: () -> Unit
)

@Composable
fun SpecialCellsCard(
    catalog: SpecialCellCatalog,
    match: SpecialCellMatch?,
    servingIdentity: String?,
    detectionEnabled: Boolean,
    voiceEnabled: Boolean,
    speakType: Boolean,
    speakSite: Boolean,
    speakSector: Boolean,
    previewEnabled: Boolean,
    actions: SpecialCellsActions,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var lastImportResult by rememberSaveable { mutableStateOf<SpecialCellsImportResult?>(null) }

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
                    text = stringResource(R.string.special_cells_title),
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
                    text = stringResource(R.string.special_cells_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        R.string.special_cells_loaded,
                        catalog.sourceLabel.ifBlank { stringResource(R.string.special_cells_unnamed) },
                        catalog.size
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = servingStatusText(servingIdentity, match, detectionEnabled),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (match != null) {
                        Sushi
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                if (catalog.cells.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.special_cells_list_heading),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    catalog.cells.forEach { cell ->
                        val isMatch = match?.cell === cell ||
                            (match != null && match.cell.matchKey == cell.matchKey &&
                                match.cell.site == cell.site && match.cell.sector == cell.sector)
                        Text(
                            text = formatLoadedCell(cell),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isMatch) FontWeight.Bold else FontWeight.Normal,
                            color = if (isMatch) Sushi else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (catalog.warnings.isNotEmpty()) {
                    Text(
                        text = catalog.warnings.joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                val controlsEnabled = settingsControlsEnabled()
                SettingsToggleRow(
                    label = stringResource(R.string.special_cells_detection_enabled),
                    checked = detectionEnabled,
                    enabled = controlsEnabled,
                    onCheckedChange = actions.onDetectionEnabledChange
                )
                SettingsToggleRow(
                    label = stringResource(R.string.special_cells_voice_enabled),
                    checked = voiceEnabled,
                    enabled = controlsEnabled,
                    onCheckedChange = actions.onVoiceEnabledChange
                )
                if (voiceEnabled) {
                    SettingsToggleRow(
                        label = stringResource(R.string.special_cells_speak_type),
                        checked = speakType,
                        enabled = controlsEnabled,
                        onCheckedChange = actions.onSpeakTypeChange
                    )
                    SettingsToggleRow(
                        label = stringResource(R.string.special_cells_speak_site),
                        checked = speakSite,
                        enabled = controlsEnabled,
                        onCheckedChange = actions.onSpeakSiteChange
                    )
                    SettingsToggleRow(
                        label = stringResource(R.string.special_cells_speak_sector),
                        checked = speakSector,
                        enabled = controlsEnabled,
                        onCheckedChange = actions.onSpeakSectorChange
                    )
                    OutlinedButton(
                        onClick = actions.onPreviewVoice,
                        enabled = previewEnabled && catalog.isNotEmpty() &&
                            (speakType || speakSite || speakSector),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.special_cells_test))
                    }
                }

                OutlinedButton(
                    onClick = {
                        actions.onImport { result -> lastImportResult = result }
                    },
                    enabled = controlsEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.special_cells_import))
                }
                OutlinedButton(
                    onClick = {
                        actions.onRestoreExample()
                        lastImportResult = null
                    },
                    enabled = controlsEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.special_cells_restore_example))
                }
                lastImportResult?.let { result ->
                    Text(
                        text = stringResource(importResultMessage(result)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (result == SpecialCellsImportResult.Imported) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
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
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

private fun importResultMessage(result: SpecialCellsImportResult): Int {
    return when (result) {
        SpecialCellsImportResult.Imported -> R.string.special_cells_imported
        SpecialCellsImportResult.InvalidFile -> R.string.special_cells_error_invalid_file
        SpecialCellsImportResult.EmptyFile -> R.string.special_cells_error_empty_file
        SpecialCellsImportResult.Failed -> R.string.special_cells_error_import_failed
    }
}

private fun SpecialCellCatalog.isNotEmpty(): Boolean = !isEmpty

private fun formatLoadedCell(cell: SpecialCell): String {
    return "${cell.site} · ${cell.type} · ${cell.rat.displayLabel} ${cell.sector} · ${cell.channel}/${cell.pci}"
}

@Composable
private fun servingStatusText(
    servingIdentity: String?,
    match: SpecialCellMatch?,
    detectionEnabled: Boolean
): String {
    if (!detectionEnabled) {
        return stringResource(R.string.special_cells_status_disabled)
    }
    if (servingIdentity.isNullOrBlank()) {
        return stringResource(R.string.special_cells_status_no_serving)
    }
    return if (match != null) {
        stringResource(
            R.string.special_cells_status_matched,
            servingIdentity,
            match.cell.displaySite,
            match.cell.sector
        )
    } else {
        stringResource(R.string.special_cells_status_no_match, servingIdentity)
    }
}
