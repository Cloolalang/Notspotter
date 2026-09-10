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
import io.github.cloolalang.notspotdetector.model.CarrierConfigEntry
import io.github.cloolalang.notspotdetector.model.CarrierConfigSnapshot
import io.github.cloolalang.notspotdetector.ui.theme.Sushi

/**
 * Research/diagnostics card — read-only dump of a curated set of `CarrierConfigManager` keys
 * that affect actual UE (device) behavior (VoLTE, WiFi calling, VoNR, NR/5G availability,
 * network selection, emergency). Display only for now; exporting to a file is a future step.
 */
@Composable
fun CarrierConfigCard(
    snapshot: CarrierConfigSnapshot,
    phoneStatePermissionGranted: Boolean,
    onRefresh: () -> Unit,
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
                    text = stringResource(R.string.carrier_config_title),
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

            Text(
                text = stringResource(R.string.carrier_config_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (expanded) {
                LaunchedEffect(Unit) {
                    onRefresh()
                }

                if (!phoneStatePermissionGranted) {
                    Text(
                        text = stringResource(R.string.carrier_config_permission_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (!snapshot.available) {
                    Text(
                        text = stringResource(R.string.carrier_config_unavailable_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val carrierName = snapshot.carrierName
                    Text(
                        text = if (carrierName != null) {
                            stringResource(
                                R.string.carrier_config_carrier_label,
                                carrierName,
                                snapshot.subscriptionId
                            )
                        } else {
                            stringResource(R.string.carrier_config_sub_only_label, snapshot.subscriptionId)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )

                    snapshot.entries
                        .groupBy { it.category }
                        .forEach { (category, entries) ->
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Sushi,
                                fontWeight = FontWeight.Bold
                            )
                            entries.forEach { entry -> CarrierConfigEntryRow(entry) }
                        }
                }

                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.carrier_config_refresh))
                }
            }
        }
    }
}

@Composable
private fun CarrierConfigEntryRow(entry: CarrierConfigEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.padding(end = 12.dp)) {
            Text(
                text = entry.label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = entry.key,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = entry.value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
