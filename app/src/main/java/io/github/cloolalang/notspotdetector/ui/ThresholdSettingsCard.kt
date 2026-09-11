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
import io.github.cloolalang.notspotdetector.ui.theme.Sushi
import io.github.cloolalang.notspotdetector.model.ThresholdSettings
import kotlin.math.roundToInt

@Composable
fun ThresholdSettingsCard(
    thresholds: ThresholdSettings,
    onGoodRttChange: (Long) -> Unit,
    onPoorRttChange: (Long) -> Unit,
    onPoorJitterChange: (Long) -> Unit,
    onPoorPacketLossChange: (Float) -> Unit,
    onSuppressClicksOnGoodChange: (Boolean) -> Unit,
    onGoodConnectionClicksPerPingChange: (Int) -> Unit,
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
                    text = stringResource(R.string.thresholds_title),
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
                    text = stringResource(R.string.thresholds_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ThresholdSlider(
                    label = stringResource(R.string.threshold_good_rtt),
                    value = thresholds.goodRttMs.toFloat(),
                    valueRange = ThresholdSettings.MIN_GOOD_RTT_MS.toFloat()..ThresholdSettings.MAX_GOOD_RTT_MS.toFloat(),
                    unit = "ms",
                    onValueChange = { onGoodRttChange(it.roundToInt().toLong()) }
                )

                ThresholdSlider(
                    label = stringResource(R.string.threshold_poor_rtt),
                    value = thresholds.poorRttMs.toFloat(),
                    valueRange = maxOf(
                        thresholds.goodRttMs,
                        ThresholdSettings.MIN_POOR_RTT_MS
                    ).toFloat()..ThresholdSettings.MAX_POOR_RTT_MS.toFloat(),
                    unit = "ms",
                    onValueChange = { onPoorRttChange(it.roundToInt().toLong()) }
                )

                ThresholdSlider(
                    label = stringResource(R.string.threshold_poor_jitter),
                    value = thresholds.poorJitterMs.toFloat(),
                    valueRange = ThresholdSettings.MIN_POOR_JITTER_MS.toFloat()..ThresholdSettings.MAX_POOR_JITTER_MS.toFloat(),
                    unit = "ms",
                    onValueChange = { onPoorJitterChange(it.roundToInt().toLong()) }
                )

                ThresholdSlider(
                    label = stringResource(R.string.threshold_poor_packet_loss),
                    value = thresholds.poorPacketLossPercent,
                    valueRange = ThresholdSettings.MIN_POOR_PACKET_LOSS_PERCENT..ThresholdSettings.MAX_POOR_PACKET_LOSS_PERCENT,
                    unit = "%",
                    onValueChange = { onPoorPacketLossChange(it) },
                    valueFormatter = { String.format("%.1f", it) },
                    steps = 20
                )

                ThresholdSlider(
                    label = stringResource(R.string.threshold_good_clicks_per_ping),
                    value = thresholds.goodConnectionClicksPerPing.toFloat(),
                    valueRange = ThresholdSettings.MIN_GOOD_CONNECTION_CLICKS_PER_PING.toFloat()..
                        ThresholdSettings.MAX_GOOD_CONNECTION_CLICKS_PER_PING.toFloat(),
                    unit = stringResource(R.string.threshold_good_clicks_per_ping_unit),
                    onValueChange = { onGoodConnectionClicksPerPingChange(it.roundToInt()) },
                    steps = ThresholdSettings.MAX_GOOD_CONNECTION_CLICKS_PER_PING -
                        ThresholdSettings.MIN_GOOD_CONNECTION_CLICKS_PER_PING - 1
                )

                Text(
                    text = stringResource(R.string.threshold_good_clicks_per_ping_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSuppressClicksOnGoodChange(!thresholds.suppressClicksOnGoodConnection)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = thresholds.suppressClicksOnGoodConnection,
                        onCheckedChange = onSuppressClicksOnGoodChange,
                        enabled = settingsControlsEnabled()
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.threshold_suppress_clicks_good),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(R.string.threshold_suppress_clicks_good_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = onReset,
                    enabled = settingsControlsEnabled(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.thresholds_reset))
                }
            }
        }
    }
}

@Composable
private fun ThresholdSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    onValueChange: (Float) -> Unit,
    valueFormatter: (Float) -> String = { it.roundToInt().toString() },
    steps: Int = 0
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${valueFormatter(value)} $unit",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            enabled = settingsControlsEnabled(),
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}
