package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import kotlin.math.roundToInt

@Composable
fun PassiveSignalSettingsCard(
    settings: PassiveSignalSettings,
    mockSettings: PassiveMockSettings,
    passiveMeasurementIntervalMs: Long,
    onSettingsChange: (PassiveSignalSettings) -> Unit,
    onMockSettingsChange: (PassiveMockSettings) -> Unit,
    onPassiveMeasurementIntervalChange: (Long) -> Unit,
    onReset: () -> Unit,
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
                    text = stringResource(R.string.passive_signal_settings_title),
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
                text = stringResource(R.string.passive_signal_settings_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (expanded) {
                PassiveMeasurementIntervalSlider(
                    intervalMs = passiveMeasurementIntervalMs,
                    onIntervalChange = onPassiveMeasurementIntervalChange
                )

                PassiveMockSection(
                    mockSettings = mockSettings,
                    onMockSettingsChange = onMockSettingsChange
                )

                Text(
                    text = stringResource(R.string.passive_signal_rsrp_section),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(
                        R.string.passive_signal_rsrp_section_hint,
                        PassiveSignalSettings.MIN_RSRP_DBM,
                        PassiveSignalSettings.MAX_RSRP_DBM
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                RsrpBoundarySliders(
                    settings = settings,
                    onSettingsChange = onSettingsChange
                )

                Text(
                    text = stringResource(R.string.passive_signal_rsrq_section),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(
                        R.string.passive_signal_rsrq_section_hint,
                        PassiveSignalSettings.RSRQ_FAIR_MIN_DB,
                        PassiveSignalSettings.RSRQ_FAIR_MAX_DB
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                RsrqBoundarySliders(
                    settings = settings,
                    onSettingsChange = onSettingsChange
                )

                NoisyRsrqPassiveClicksOption(
                    settings = settings,
                    onSettingsChange = onSettingsChange
                )

                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.passive_signal_settings_reset))
                }
            }
        }
    }
}

@Composable
private fun PassiveMeasurementIntervalSlider(
    intervalMs: Long,
    onIntervalChange: (Long) -> Unit
) {
    val minSeconds = (MonitoringSettings.MIN_PASSIVE_MEASUREMENT_INTERVAL_MS / 1_000).toInt()
    val maxSeconds = (MonitoringSettings.MAX_PASSIVE_MEASUREMENT_INTERVAL_MS / 1_000).toInt()
    val valueSeconds = (intervalMs / 1_000L).toInt().coerceIn(minSeconds, maxSeconds)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.passive_measurement_interval_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.passive_measurement_interval_value, valueSeconds),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = stringResource(R.string.passive_measurement_interval_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = valueSeconds.toFloat(),
            onValueChange = { onIntervalChange(it.roundToInt() * 1_000L) },
            valueRange = minSeconds.toFloat()..maxSeconds.toFloat(),
            steps = maxSeconds - minSeconds - 1
        )
    }
}

@Composable
private fun PassiveMockSection(
    mockSettings: PassiveMockSettings,
    onMockSettingsChange: (PassiveMockSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = mockSettings.enabled,
                onCheckedChange = { onMockSettingsChange(mockSettings.copy(enabled = it)) }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.passive_mock_enabled),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.passive_mock_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (mockSettings.enabled) {
            BoundarySlider(
                label = stringResource(R.string.passive_mock_rsrp),
                rangeLabel = stringResource(
                    R.string.passive_signal_bound_value,
                    mockSettings.rsrpDbm,
                    "dBm"
                ),
                value = mockSettings.rsrpDbm,
                valueRange = PassiveSignalSettings.MIN_RSRP_DBM..PassiveSignalSettings.MAX_RSRP_DBM,
                onValueChange = { onMockSettingsChange(mockSettings.copy(rsrpDbm = it)) }
            )
            BoundarySlider(
                label = stringResource(R.string.passive_mock_rsrq),
                rangeLabel = stringResource(
                    R.string.passive_signal_bound_value,
                    mockSettings.rsrqDb,
                    "dB"
                ),
                value = mockSettings.rsrqDb,
                valueRange = PassiveSignalSettings.MIN_RSRQ_DB..PassiveSignalSettings.MAX_RSRQ_DB,
                unit = "dB",
                onValueChange = { onMockSettingsChange(mockSettings.copy(rsrqDb = it)) }
            )
        }
    }
}

@Composable
private fun RsrpBoundarySliders(
    settings: PassiveSignalSettings,
    onSettingsChange: (PassiveSignalSettings) -> Unit
) {
    val gap = PassiveSignalSettings.MIN_RSRP_BAND_GAP_DBM
    val maxMild = PassiveSignalSettings.VERY_STRONG_RSRP_DBM - gap

    BoundarySlider(
        label = stringResource(R.string.passive_signal_boundary_no_signal),
        rangeLabel = stringResource(
            R.string.passive_signal_rsrp_band_range,
            PassiveSignalSettings.MIN_RSRP_DBM,
            settings.noSignalRsrpDbm
        ),
        value = settings.noSignalRsrpDbm,
        valueRange = PassiveSignalSettings.MIN_RSRP_DBM..(settings.poorRsrpMinDbm - gap),
        onValueChange = { onSettingsChange(settings.copy(noSignalRsrpDbm = it)) }
    )
    BoundarySlider(
        label = stringResource(R.string.passive_signal_boundary_poor),
        rangeLabel = stringResource(
            R.string.passive_signal_rsrp_band_range,
            settings.poorRsrpMinDbm + gap,
            settings.fairRsrpMinDbm
        ),
        value = settings.poorRsrpMinDbm,
        valueRange = (settings.noSignalRsrpDbm + gap)..(settings.fairRsrpMinDbm - gap),
        onValueChange = { onSettingsChange(settings.copy(poorRsrpMinDbm = it)) }
    )
    BoundarySlider(
        label = stringResource(R.string.passive_signal_boundary_fair),
        rangeLabel = stringResource(
            R.string.passive_signal_rsrp_band_range,
            settings.fairRsrpMinDbm + gap,
            settings.goodRsrpMinDbm
        ),
        value = settings.fairRsrpMinDbm,
        valueRange = (settings.poorRsrpMinDbm + gap)..(settings.goodRsrpMinDbm - gap),
        onValueChange = { onSettingsChange(settings.copy(fairRsrpMinDbm = it)) }
    )
    BoundarySlider(
        label = stringResource(R.string.passive_signal_boundary_good),
        rangeLabel = stringResource(
            R.string.passive_signal_rsrp_band_range,
            settings.goodRsrpMinDbm + gap,
            settings.mildRsrpMinDbm
        ),
        value = settings.goodRsrpMinDbm,
        valueRange = (settings.fairRsrpMinDbm + gap)..(settings.mildRsrpMinDbm - gap),
        onValueChange = { onSettingsChange(settings.copy(goodRsrpMinDbm = it)) }
    )
    BoundarySlider(
        label = stringResource(R.string.passive_signal_boundary_mild),
        rangeLabel = stringResource(
            R.string.passive_signal_rsrp_band_range,
            settings.mildRsrpMinDbm + gap,
            PassiveSignalSettings.VERY_STRONG_RSRP_DBM
        ),
        value = settings.mildRsrpMinDbm,
        valueRange = (settings.goodRsrpMinDbm + gap)..maxMild,
        onValueChange = { onSettingsChange(settings.copy(mildRsrpMinDbm = it)) }
    )
    FixedRsrpBand(
        label = stringResource(R.string.passive_signal_boundary_very_strong),
        rangeLabel = stringResource(
            R.string.passive_signal_rsrp_band_range_open,
            PassiveSignalSettings.VERY_STRONG_RSRP_DBM + gap,
            PassiveSignalSettings.MAX_RSRP_DBM
        ),
        thresholdLabel = stringResource(
            R.string.passive_signal_bound_value,
            PassiveSignalSettings.VERY_STRONG_RSRP_DBM,
            "dBm"
        )
    )
}

@Composable
private fun FixedRsrpBand(
    label: String,
    rangeLabel: String,
    thresholdLabel: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = thresholdLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = rangeLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.passive_signal_very_strong_fixed_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RsrqBoundarySliders(
    settings: PassiveSignalSettings,
    onSettingsChange: (PassiveSignalSettings) -> Unit
) {
    BoundarySlider(
        label = stringResource(R.string.passive_signal_boundary_rsrq_fair),
        rangeLabel = stringResource(
            R.string.passive_signal_rsrq_single_band_hint,
            PassiveSignalSettings.RSRQ_FAIR_MIN_DB,
            settings.rsrqFairMinDb - PassiveSignalSettings.MIN_RSRQ_BAND_GAP_DB,
            settings.rsrqFairMinDb,
            PassiveSignalSettings.RSRQ_FAIR_MAX_DB
        ),
        value = settings.rsrqFairMinDb,
        valueRange = PassiveSignalSettings.RSRQ_FAIR_MIN_DB..PassiveSignalSettings.RSRQ_FAIR_MAX_DB,
        unit = "dB",
        onValueChange = { onSettingsChange(settings.copy(rsrqFairMinDb = it)) }
    )
}

@Composable
private fun NoisyRsrqPassiveClicksOption(
    settings: PassiveSignalSettings,
    onSettingsChange: (PassiveSignalSettings) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = settings.noisyRsrqPassiveClicks,
            onCheckedChange = { onSettingsChange(settings.copy(noisyRsrqPassiveClicks = it)) }
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.passive_signal_noisy_rsrq_clicks),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(
                    R.string.passive_signal_noisy_rsrq_clicks_hint,
                    settings.rsrqFairMinDb
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BoundarySlider(
    label: String,
    rangeLabel: String,
    value: Int,
    valueRange: IntRange,
    unit: String = "dBm",
    onValueChange: (Int) -> Unit
) {
    if (valueRange.first > valueRange.last) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.passive_signal_bound_value, value, unit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = rangeLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = value.toFloat().coerceIn(valueRange.first.toFloat(), valueRange.last.toFloat()),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = (valueRange.last - valueRange.first).coerceAtMost(76)
        )
    }
}
