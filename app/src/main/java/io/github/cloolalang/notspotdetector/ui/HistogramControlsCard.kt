package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.coerceToHistogramWindowStep
import io.github.cloolalang.notspotdetector.ui.theme.Sushi
import kotlin.math.roundToInt

/**
 * Collapsible container for controls that affect the RSRP histogram (currently just the sample
 * window slider). Only rendered by the caller while monitoring/testing is running — see
 * [io.github.cloolalang.notspotdetector.ui.MonitorScreen].
 */
@Composable
fun HistogramControlsCard(
    windowMs: Long,
    onWindowChange: (Long) -> Unit,
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
                    text = stringResource(R.string.histogram_controls_title),
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
                    text = stringResource(R.string.histogram_controls_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                RsrpHistogramWindowSlider(
                    windowMs = windowMs.coerceToHistogramWindowStep(),
                    onWindowChange = onWindowChange
                )
            }
        }
    }
}

@Composable
private fun RsrpHistogramWindowSlider(
    windowMs: Long,
    onWindowChange: (Long) -> Unit
) {
    val minMs = MonitoringSettings.MIN_RSRP_HISTOGRAM_WINDOW_MS
    val stepMs = MonitoringSettings.RSRP_HISTOGRAM_WINDOW_STEP_MS
    val maxMs = MonitoringSettings.MAX_RSRP_HISTOGRAM_WINDOW_MS
    val stepCount = ((maxMs - minMs) / stepMs).toInt()
    val sliderValue = ((windowMs - minMs) / stepMs).toFloat().coerceIn(0f, stepCount.toFloat())

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.rsrp_histogram_window_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatHistogramWindow(windowMs),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { value ->
                onWindowChange(minMs + value.roundToInt() * stepMs)
            },
            valueRange = 0f..stepCount.toFloat(),
            steps = (stepCount - 1).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatHistogramWindow(minMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatHistogramWindow(maxMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatHistogramWindow(windowMs: Long): String {
    val totalSeconds = windowMs / 1_000L
    return when {
        totalSeconds < 60L -> "${totalSeconds}s"
        totalSeconds % 60L == 0L -> "${totalSeconds / 60L}m"
        else -> "${totalSeconds / 60L}m ${totalSeconds % 60L}s"
    }
}
