package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.RsrpHistogram
import io.github.cloolalang.notspotdetector.model.RsrpHistogramBand
import io.github.cloolalang.notspotdetector.model.RsrpSample
import io.github.cloolalang.notspotdetector.model.coerceToHistogramWindowStep
import io.github.cloolalang.notspotdetector.ui.theme.Sushi
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private val HistogramBarAreaHeight = 132.dp
private val HistogramLabelAreaHeight = 34.dp
private val HistogramMinBarWidth = 12.dp
private val HistogramMaxBarWidth = 36.dp

@Composable
fun RsrpHistogramCard(
    samples: List<RsrpSample>,
    windowMs: Long,
    isActive: Boolean,
    onWindowChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val coercedWindowMs = windowMs.coerceToHistogramWindowStep()

    LaunchedEffect(isActive) {
        while (isActive) {
            nowMs = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val bins = RsrpHistogram.buildBins(samples, nowMs, coercedWindowMs)
    val activeBins = RsrpHistogram.activeBins(bins)
    val totalSamples = RsrpHistogram.totalSamples(samples, nowMs, coercedWindowMs)
    val maxCount = activeBins.maxOfOrNull { it.value.count }?.coerceAtLeast(1) ?: 1

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.rsrp_histogram_title),
                style = MaterialTheme.typography.titleSmall,
                color = Sushi,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.rsrp_histogram_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (totalSamples == 0) {
                Text(
                    text = stringResource(R.string.rsrp_histogram_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    activeBins.forEach { indexedBin ->
                        RsrpHistogramBarColumn(
                            labelDbm = indexedBin.value.labelDbm,
                            count = indexedBin.value.count,
                            totalSamples = totalSamples,
                            maxCount = maxCount,
                            barColor = histogramBarColor(indexedBin.value.labelDbm),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(
                    text = stringResource(
                        R.string.rsrp_histogram_sample_count_active,
                        totalSamples,
                        activeBins.size
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // The sample-window control only makes sense while samples are actively being
            // collected; hide it when monitoring/testing isn't running.
            if (isActive) {
                RsrpHistogramWindowSlider(
                    windowMs = coercedWindowMs,
                    onWindowChange = onWindowChange
                )
            }
        }
    }
}

@Composable
private fun RsrpHistogramBarColumn(
    labelDbm: Int?,
    count: Int,
    totalSamples: Int,
    maxCount: Int,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.widthIn(min = 28.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val barWidth = (maxWidth * 0.42f).coerceIn(HistogramMinBarWidth, HistogramMaxBarWidth)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        R.string.rsrp_histogram_bin_percent,
                        histogramBinPercent(count, totalSamples)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(HistogramBarAreaHeight)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(count.toFloat() / maxCount.toFloat())
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(barColor)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HistogramLabelAreaHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelDbm?.toString() ?: stringResource(R.string.rsrp_histogram_null_bin_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(-90f)
                )
            }
        }
    }
}

/** Grey — used for the "no signal" bin (null label), distinct from any signal-strength band. */
private val NullBinColor = Color(0xFF9E9E9E)

private fun histogramBarColor(labelDbm: Int?): Color {
    return when (RsrpHistogram.bandForLabelDbm(labelDbm)) {
        null -> NullBinColor
        RsrpHistogramBand.EXCELLENT -> Color(0xFF81D4FA)
        RsrpHistogramBand.GOOD -> Color(0xFF66BB6A)
        RsrpHistogramBand.FAIR -> Color(0xFFFFEB3B)
        RsrpHistogramBand.POOR -> Color(0xFFFF9800)
        RsrpHistogramBand.CRITICAL -> Color(0xFFFF1744)
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

private fun histogramBinPercent(count: Int, totalSamples: Int): Int {
    if (totalSamples <= 0) return 0
    return ((count * 100f) / totalSamples).roundToInt()
}
