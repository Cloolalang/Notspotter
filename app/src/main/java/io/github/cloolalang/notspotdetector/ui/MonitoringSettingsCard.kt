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
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.SimSubscriptionOption
import io.github.cloolalang.notspotdetector.network.SimSubscriptionHelper
import kotlin.math.roundToInt

@Composable
fun MonitoringSettingsCard(
    monitoringSettings: MonitoringSettings,
    passiveSignalSettings: PassiveSignalSettings,
    simSubscriptions: List<SimSubscriptionOption>,
    phoneStatePermissionGranted: Boolean,
    onMonitor2gFallbackChange: (Boolean) -> Unit,
    onPassiveQuietUntilCriticalChange: (Boolean) -> Unit,
    onPassiveSignalSettingsChange: (PassiveSignalSettings) -> Unit,
    onSubscriptionChange: (Int) -> Unit,
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
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = stringResource(R.string.monitoring_settings_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (expanded) {
                SimSelectionSection(
                    selectedSubscriptionId = monitoringSettings.subscriptionId,
                    simSubscriptions = simSubscriptions,
                    phoneStatePermissionGranted = phoneStatePermissionGranted,
                    onSubscriptionChange = onSubscriptionChange
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = monitoringSettings.passiveQuietUntilCritical,
                        onCheckedChange = onPassiveQuietUntilCriticalChange
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.monitoring_passive_quiet_alerts),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(
                                R.string.monitoring_passive_quiet_alerts_hint,
                                passiveSignalSettings.quietAlertRsrqDb,
                                passiveSignalSettings.quietAlertRsrpMaxDbm
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                QuietAlertThresholdSliders(
                    settings = passiveSignalSettings,
                    onSettingsChange = onPassiveSignalSettingsChange
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = monitoringSettings.monitor2gFallback,
                        onCheckedChange = onMonitor2gFallbackChange
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
            }
        }
    }
}

@Composable
private fun QuietAlertThresholdSliders(
    settings: PassiveSignalSettings,
    onSettingsChange: (PassiveSignalSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.monitoring_quiet_alert_thresholds_title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = stringResource(R.string.monitoring_quiet_alert_thresholds_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        QuietAlertSlider(
            label = stringResource(R.string.monitoring_quiet_alert_rsrp),
            value = settings.quietAlertRsrpMaxDbm,
            valueRange = PassiveSignalSettings.MIN_RSRP_DBM..PassiveSignalSettings.MAX_RSRP_DBM,
            unit = "dBm",
            onValueChange = { onSettingsChange(settings.copy(quietAlertRsrpMaxDbm = it)) }
        )
        QuietAlertSlider(
            label = stringResource(R.string.monitoring_quiet_alert_rsrq),
            value = settings.quietAlertRsrqDb,
            valueRange = PassiveSignalSettings.MIN_RSRQ_DB..PassiveSignalSettings.MAX_RSRQ_DB,
            unit = "dB",
            onValueChange = { onSettingsChange(settings.copy(quietAlertRsrqDb = it)) }
        )
    }
}

@Composable
private fun QuietAlertSlider(
    label: String,
    value: Int,
    valueRange: IntRange,
    unit: String,
    onValueChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.passive_signal_bound_value, value, unit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            value = value.toFloat().coerceIn(valueRange.first.toFloat(), valueRange.last.toFloat()),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = (valueRange.last - valueRange.first).coerceAtMost(76)
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
            .clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}
