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
import androidx.compose.material3.RadioButton
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
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.SimSubscriptionOption
import io.github.cloolalang.notspotdetector.network.SimSubscriptionHelper
import io.github.cloolalang.notspotdetector.ui.theme.Sushi

@Composable
fun MonitoringSettingsCard(
    monitoringSettings: MonitoringSettings,
    simSubscriptions: List<SimSubscriptionOption>,
    phoneStatePermissionGranted: Boolean,
    onMonitor2gFallbackChange: (Boolean) -> Unit,
    onSubscriptionChange: (Int) -> Unit,
    onMobileDataEnabledChange: (Boolean) -> Unit = {},
    mobileDataEnabled: Boolean? = null,
    onShowManualSelectOperatorButtonChange: (Boolean) -> Unit = {},
    onKeepScreenOnWhileMonitoringChange: (Boolean) -> Unit = {},
    onInhibit2gChange: (Boolean) -> Unit = {},
    inhibit2gBusy: Boolean = false,
    inhibit2gFailed: Boolean = false,
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
                    text = stringResource(R.string.monitoring_settings_title),
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
                    text = stringResource(R.string.monitoring_settings_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SimSelectionSection(
                    selectedSubscriptionId = monitoringSettings.subscriptionId,
                    simSubscriptions = simSubscriptions,
                    phoneStatePermissionGranted = phoneStatePermissionGranted,
                    onSubscriptionChange = onSubscriptionChange
                )

                MobileDataToggleRow(
                    enabled = mobileDataEnabled,
                    onEnabledChange = onMobileDataEnabledChange
                )

                ManualSelectOperatorToggleRow(
                    enabled = monitoringSettings.showManualSelectOperatorButton,
                    onEnabledChange = onShowManualSelectOperatorButtonChange
                )

                KeepScreenOnToggleRow(
                    enabled = monitoringSettings.keepScreenOnWhileMonitoring,
                    onEnabledChange = onKeepScreenOnWhileMonitoringChange
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = monitoringSettings.monitor2gFallback,
                        onCheckedChange = onMonitor2gFallbackChange,
                        enabled = settingsControlsEnabled()
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.monitoring_2g_fallback),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.monitoring_2g_fallback_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Inhibit2gToggleRow(
                    enabled = monitoringSettings.inhibit2g,
                    busy = inhibit2gBusy,
                    failed = inhibit2gFailed,
                    onEnabledChange = onInhibit2gChange
                )
            }
        }
    }
}

@Composable
private fun MobileDataToggleRow(
    enabled: Boolean?,
    onEnabledChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.monitoring_mobile_data),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.monitoring_mobile_data_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = enabled == true,
            onCheckedChange = onEnabledChange,
            enabled = settingsControlsEnabled()
        )
    }
}

@Composable
private fun Inhibit2gToggleRow(
    enabled: Boolean,
    busy: Boolean,
    failed: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.monitoring_inhibit_2g),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.monitoring_inhibit_2g_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (failed) {
                Text(
                    text = stringResource(R.string.monitoring_inhibit_2g_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            enabled = settingsControlsEnabled() && !busy
        )
    }
}

@Composable
private fun KeepScreenOnToggleRow(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.monitoring_keep_screen_on),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.monitoring_keep_screen_on_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            enabled = settingsControlsEnabled()
        )
    }
}

@Composable
private fun ManualSelectOperatorToggleRow(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.monitoring_manual_select_operator),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.monitoring_manual_select_operator_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            enabled = settingsControlsEnabled()
        )
    }
}

@Composable
private fun SimSelectionSection(
    selectedSubscriptionId: Int,
    simSubscriptions: List<SimSubscriptionOption>,
    phoneStatePermissionGranted: Boolean,
    onSubscriptionChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.monitoring_sim_title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = stringResource(R.string.monitoring_sim_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!phoneStatePermissionGranted) {
            Text(
                text = stringResource(R.string.monitoring_sim_permission_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            return
        }

        SimRadioOption(
            selected = selectedSubscriptionId == MonitoringSettings.DEFAULT_SUBSCRIPTION_ID,
            label = stringResource(R.string.monitoring_sim_system_default),
            onSelect = { onSubscriptionChange(MonitoringSettings.DEFAULT_SUBSCRIPTION_ID) }
        )

        for (sim in simSubscriptions) {
            SimRadioOption(
                selected = selectedSubscriptionId == sim.subscriptionId,
                label = SimSubscriptionHelper.formatSimLabel(sim),
                onSelect = { onSubscriptionChange(sim.subscriptionId) }
            )
        }
    }
}

@Composable
private fun SimRadioOption(
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
