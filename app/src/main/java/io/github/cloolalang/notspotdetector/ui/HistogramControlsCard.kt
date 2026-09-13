package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.RsrpHistogram
import io.github.cloolalang.notspotdetector.model.RsrpHistogramBand
import io.github.cloolalang.notspotdetector.model.RsrpHistogramBin
import io.github.cloolalang.notspotdetector.model.RsrpHistogramBinKind
import io.github.cloolalang.notspotdetector.model.RsrpHistogramBinningMode
import io.github.cloolalang.notspotdetector.model.RsrpHistogramThresholdBarColor
import io.github.cloolalang.notspotdetector.model.RsrpMeanColorBand
import io.github.cloolalang.notspotdetector.model.RsrpSample
import io.github.cloolalang.notspotdetector.model.RsrpSampleTrend
import io.github.cloolalang.notspotdetector.model.coerceToHistogramWindowStep
import io.github.cloolalang.notspotdetector.ui.theme.BondiBlue
import io.github.cloolalang.notspotdetector.ui.theme.Sushi
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private val HistogramBarAreaHeight = 80.dp
private val HistogramLabelAreaHeight = 34.dp
private val HistogramMinBarWidth = 12.dp
private val HistogramMaxBarWidth = 36.dp

/**
 * Collapsible "Level histogram" panel: shows one live RSRP histogram (5 dB bins or threshold
 * bins). Mode, thresholds, and sample window live in a nested Histogram settings sub-panel.
 * Only rendered by the caller while monitoring/testing is running — see
 * [io.github.cloolalang.notspotdetector.ui.MonitorScreen].
 */
@Composable
fun HistogramControlsCard(
    samples: List<RsrpSample>,
    windowMs: Long,
    binningMode: RsrpHistogramBinningMode,
    thresholdsDbm: List<Int>,
    isActive: Boolean,
    onWindowChange: (Long) -> Unit,
    onBinningModeChange: (RsrpHistogramBinningMode) -> Unit,
    onThresholdChange: (Int, Int) -> Unit,
    onClearHistogram: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(samples) {
        nowMs = System.currentTimeMillis()
    }
    LaunchedEffect(isActive) {
        while (isActive) {
            nowMs = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val elapsedMs = RsrpHistogram.elapsedSinceFirstSampleMs(
        samples = samples,
        nowMs = nowMs,
        windowMs = windowMs.coerceToHistogramWindowStep()
    )

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
                RsrpHistogramDisplay(
                    samples = samples,
                    windowMs = windowMs.coerceToHistogramWindowStep(),
                    binningMode = binningMode,
                    thresholdsDbm = thresholdsDbm,
                    nowMs = nowMs,
                    elapsedMs = elapsedMs
                )

                OutlinedButton(
                    onClick = onClearHistogram,
                    enabled = samples.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.rsrp_histogram_clear))
                }

                HistogramSettingsPanel(
                    windowMs = windowMs.coerceToHistogramWindowStep(),
                    binningMode = binningMode,
                    thresholdsDbm = thresholdsDbm,
                    elapsedMs = elapsedMs,
                    onWindowChange = onWindowChange,
                    onBinningModeChange = onBinningModeChange,
                    onThresholdChange = onThresholdChange
                )
            }
        }
    }
}

@Composable
private fun HistogramSettingsPanel(
    windowMs: Long,
    binningMode: RsrpHistogramBinningMode,
    thresholdsDbm: List<Int>,
    elapsedMs: Long?,
    onWindowChange: (Long) -> Unit,
    onBinningModeChange: (RsrpHistogramBinningMode) -> Unit,
    onThresholdChange: (Int, Int) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.histogram_settings_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Sushi
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Text(
                    text = stringResource(R.string.histogram_settings_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HistogramBinningModeSelector(
                    selected = binningMode,
                    onSelect = onBinningModeChange
                )
                Text(
                    text = stringResource(
                        if (binningMode == RsrpHistogramBinningMode.THRESHOLD) {
                            R.string.rsrp_histogram_threshold_summary
                        } else {
                            R.string.rsrp_histogram_summary
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (binningMode == RsrpHistogramBinningMode.THRESHOLD) {
                    thresholdsDbm.forEachIndexed { index, dbm ->
                        HistogramThresholdSlider(
                            index = index,
                            thresholdDbm = dbm,
                            onThresholdChange = { onThresholdChange(index, it) }
                        )
                    }
                }
                RsrpHistogramWindowSlider(
                    windowMs = windowMs,
                    elapsedMs = elapsedMs,
                    onWindowChange = onWindowChange
                )
            }
        }
    }
}

@Composable
private fun RsrpHistogramDisplay(
    samples: List<RsrpSample>,
    windowMs: Long,
    binningMode: RsrpHistogramBinningMode,
    thresholdsDbm: List<Int>,
    nowMs: Long,
    elapsedMs: Long?,
    modifier: Modifier = Modifier
) {
    val bins = if (binningMode == RsrpHistogramBinningMode.THRESHOLD) {
        RsrpHistogram.buildThresholdBins(samples, nowMs, windowMs, thresholdsDbm)
    } else {
        RsrpHistogram.buildBins(samples, nowMs, windowMs)
    }
    val displayedBins = if (binningMode == RsrpHistogramBinningMode.THRESHOLD) {
        bins.withIndex().map { IndexedValue(it.index, it.value) }
    } else {
        RsrpHistogram.activeBins(bins)
    }
    val totalSamples = RsrpHistogram.totalSamples(samples, nowMs, windowMs)
    val maxCount = if (binningMode == RsrpHistogramBinningMode.THRESHOLD) {
        totalSamples.coerceAtLeast(1)
    } else {
        displayedBins.maxOfOrNull { it.value.count }?.coerceAtLeast(1) ?: 1
    }
    val windowStats = RsrpHistogram.windowStats(samples, nowMs, windowMs)
    val sampleTrend = RsrpHistogram.sampleTrend(samples, nowMs, windowMs)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
                displayedBins.forEach { indexedBin ->
                    RsrpHistogramBarColumn(
                        bin = indexedBin.value,
                        totalSamples = totalSamples,
                        maxCount = maxCount,
                        barColor = histogramBarColor(
                            bin = indexedBin.value,
                            totalSamples = totalSamples,
                            thresholdMode = binningMode == RsrpHistogramBinningMode.THRESHOLD
                        ),
                        thresholdMode = binningMode == RsrpHistogramBinningMode.THRESHOLD,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (windowStats != null) {
                    SigmaHistogramBarColumn(
                        stdevDbm = windowStats.stdevDbm,
                        modifier = Modifier.width(40.dp)
                    )
                }
            }
            val countText = if (binningMode == RsrpHistogramBinningMode.THRESHOLD) {
                stringResource(R.string.rsrp_histogram_sample_count, totalSamples)
            } else {
                stringResource(
                    R.string.rsrp_histogram_sample_count_active,
                    totalSamples,
                    displayedBins.size
                )
            }
            val elapsedLabel = formatHistogramElapsedLabel(elapsedMs)
            Text(
                text = if (elapsedLabel != null) "$countText · $elapsedLabel" else countText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (windowStats != null) {
                Text(
                    text = stringResource(
                        R.string.rsrp_histogram_window_stats,
                        RsrpHistogram.formatWindowStat(windowStats.meanDbm),
                        RsrpHistogram.formatWindowStat(windowStats.medianDbm),
                        RsrpHistogram.formatWindowStat(windowStats.stdevDbm)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalRsrpBar(
                    title = stringResource(R.string.rsrp_histogram_mean_title),
                    rsrpDbm = windowStats.meanDbm,
                    trend = sampleTrend
                )
            }
            HorizontalRsrpBar(
                title = stringResource(R.string.rsrp_histogram_last_title),
                rsrpDbm = RsrpHistogram.lastSampleRsrpDbm(samples, nowMs, windowMs)?.toDouble()
            )
        }
    }
}

@Composable
private fun RsrpHistogramBarColumn(
    bin: RsrpHistogramBin,
    totalSamples: Int,
    maxCount: Int,
    barColor: Color,
    thresholdMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val thresholdLabel = thresholdMode && bin.kind == RsrpHistogramBinKind.SIGNAL
    val wrappedLabel = thresholdLabel ||
        bin.kind == RsrpHistogramBinKind.OTHER
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
                    text = bin.count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        R.string.rsrp_histogram_bin_percent,
                        histogramBinPercent(bin.count, totalSamples)
                    ),
                    style = MaterialTheme.typography.labelSmall,
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
                val fillFraction = if (thresholdMode) {
                    RsrpHistogram.thresholdBarFillFraction(
                        RsrpHistogram.occupancyPercent(bin.count, totalSamples)
                    )
                } else {
                    bin.count.toFloat() / maxCount.toFloat()
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fillFraction)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(barColor)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (wrappedLabel) 40.dp else HistogramLabelAreaHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (bin.kind) {
                        RsrpHistogramBinKind.OTHER ->
                            stringResource(R.string.rsrp_histogram_other_bin_label)
                        RsrpHistogramBinKind.NO_SIGNAL ->
                            stringResource(R.string.rsrp_histogram_null_bin_label)
                        RsrpHistogramBinKind.SIGNAL -> if (thresholdLabel) {
                            stringResource(
                                R.string.rsrp_histogram_threshold_bin_label,
                                bin.labelDbm ?: 0
                            )
                        } else {
                            bin.labelDbm?.let { dbm ->
                                stringResource(R.string.rsrp_histogram_bin_label, dbm)
                            }.orEmpty()
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = if (wrappedLabel) 11.sp else 10.sp,
                    maxLines = if (wrappedLabel) 2 else 1,
                    softWrap = wrappedLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = if (wrappedLabel) Modifier else Modifier.rotate(-90f)
                )
            }
        }
    }
}

@Composable
private fun SigmaHistogramBarColumn(
    stdevDbm: Double,
    modifier: Modifier = Modifier
) {
    val fillFraction = RsrpHistogram.sigmaBarFillFraction(stdevDbm)
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
                    text = RsrpHistogram.formatWindowStat(stdevDbm),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.rsrp_histogram_sigma_unit),
                    style = MaterialTheme.typography.labelSmall,
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
                        .fillMaxHeight(fillFraction)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(BondiBlue)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.rsrp_histogram_sigma_label),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    maxLines = 2,
                    softWrap = true,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HorizontalRsrpBar(
    title: String,
    rsrpDbm: Double?,
    trend: RsrpSampleTrend? = null,
    modifier: Modifier = Modifier
) {
    val fraction = rsrpDbm?.let { RsrpHistogram.meanBarFillFraction(it) } ?: 0f
    val markerColor = rsrpDbm?.let { meanRsrpBandColor(RsrpHistogram.meanColorBand(it)) }
    val valueText = if (rsrpDbm != null) {
        stringResource(
            R.string.rsrp_histogram_mean_value,
            RsrpHistogram.formatWindowStat(rsrpDbm)
        )
    } else {
        stringResource(R.string.rsrp_histogram_null_bin_label)
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Sushi
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                RsrpTrendIcon(trend = trend)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            contentAlignment = Alignment.CenterStart
        ) {
            if (markerColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .background(markerColor)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.rsrp_histogram_mean_min),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.rsrp_histogram_mean_max),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RsrpTrendIcon(trend: RsrpSampleTrend?) {
    val (drawable, tint, description) = when (trend) {
        RsrpSampleTrend.UP -> Triple(
            R.drawable.ic_rsrp_trend_up,
            Sushi,
            stringResource(R.string.rsrp_histogram_trend_up)
        )
        RsrpSampleTrend.DOWN -> Triple(
            R.drawable.ic_rsrp_trend_down,
            ThresholdRed,
            stringResource(R.string.rsrp_histogram_trend_down)
        )
        RsrpSampleTrend.FLAT -> Triple(
            R.drawable.ic_rsrp_trend_flat,
            MaterialTheme.colorScheme.onSurfaceVariant,
            stringResource(R.string.rsrp_histogram_trend_flat)
        )
        null -> return
    }
    Image(
        painter = painterResource(drawable),
        contentDescription = description,
        colorFilter = ColorFilter.tint(tint),
        modifier = Modifier.size(18.dp)
    )
}

private val MeanRsrpRed = Color(0xFFFF1744)
private val MeanRsrpOrange = Color(0xFFFF9800)
private val MeanRsrpYellow = Color(0xFFFFEB3B)
private val MeanRsrpGreen = Color(0xFF66BB6A)
private val MeanRsrpLightBlue = Color(0xFF81D4FA)
private val MeanRsrpLighterBlue = Color(0xFFB3E5FC)

private fun meanRsrpBandColor(band: RsrpMeanColorBand): Color {
    return when (band) {
        RsrpMeanColorBand.RED -> MeanRsrpRed
        RsrpMeanColorBand.ORANGE -> MeanRsrpOrange
        RsrpMeanColorBand.YELLOW -> MeanRsrpYellow
        RsrpMeanColorBand.GREEN -> MeanRsrpGreen
        RsrpMeanColorBand.LIGHT_BLUE -> MeanRsrpLightBlue
        RsrpMeanColorBand.LIGHTER_BLUE -> MeanRsrpLighterBlue
    }
}

/** Grey — used for the "no signal" bin, distinct from any signal-strength band. */
private val NullBinColor = Color(0xFF9E9E9E)
/** Slate — measured RSRP that missed every threshold floor. */
private val OtherBinColor = Color(0xFF78909C)

private val ThresholdGreen = Color(0xFF66BB6A)
private val ThresholdOrange = Color(0xFFFF9800)
private val ThresholdYellow = Color(0xFFFFEB3B)
private val ThresholdRed = Color(0xFFFF1744)

private fun histogramBarColor(
    bin: RsrpHistogramBin,
    totalSamples: Int,
    thresholdMode: Boolean
): Color {
    if (thresholdMode) {
        return when (RsrpHistogram.thresholdBarColor(bin, totalSamples)) {
            RsrpHistogramThresholdBarColor.GREEN -> ThresholdGreen
            RsrpHistogramThresholdBarColor.ORANGE -> ThresholdOrange
            RsrpHistogramThresholdBarColor.YELLOW -> ThresholdYellow
            RsrpHistogramThresholdBarColor.RED -> ThresholdRed
        }
    }
    return when (bin.kind) {
        RsrpHistogramBinKind.NO_SIGNAL -> NullBinColor
        RsrpHistogramBinKind.OTHER -> OtherBinColor
        RsrpHistogramBinKind.SIGNAL -> when (RsrpHistogram.bandForLabelDbm(bin.labelDbm)) {
            null -> NullBinColor
            RsrpHistogramBand.EXCELLENT -> Color(0xFF81D4FA)
            RsrpHistogramBand.GOOD -> Color(0xFF66BB6A)
            RsrpHistogramBand.FAIR -> Color(0xFFFFEB3B)
            RsrpHistogramBand.POOR -> Color(0xFFFF9800)
            RsrpHistogramBand.CRITICAL -> Color(0xFFFF1744)
        }
    }
}

private fun histogramBinPercent(count: Int, totalSamples: Int): Int {
    return RsrpHistogram.occupancyPercent(count, totalSamples)
}

@Composable
private fun HistogramBinningModeSelector(
    selected: RsrpHistogramBinningMode,
    onSelect: (RsrpHistogramBinningMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        HistogramBinningModeOption(
            selected = selected == RsrpHistogramBinningMode.LEVEL,
            label = stringResource(R.string.rsrp_histogram_mode_level),
            onSelect = { onSelect(RsrpHistogramBinningMode.LEVEL) }
        )
        HistogramBinningModeOption(
            selected = selected == RsrpHistogramBinningMode.THRESHOLD,
            label = stringResource(R.string.rsrp_histogram_mode_threshold),
            onSelect = { onSelect(RsrpHistogramBinningMode.THRESHOLD) }
        )
    }
}

@Composable
private fun HistogramBinningModeOption(
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

@Composable
private fun HistogramThresholdSlider(
    index: Int,
    thresholdDbm: Int,
    onThresholdChange: (Int) -> Unit
) {
    val minDbm = RsrpHistogram.MIN_THRESHOLD_DBM
    val maxDbm = RsrpHistogram.MAX_THRESHOLD_DBM
    val clamped = thresholdDbm.coerceIn(minDbm, maxDbm)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.rsrp_histogram_threshold_slider_label, index + 1),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.rsrp_histogram_threshold_slider_value, clamped),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            enabled = settingsControlsEnabled(),
            value = clamped.toFloat(),
            onValueChange = { onThresholdChange(it.roundToInt()) },
            valueRange = minDbm.toFloat()..maxDbm.toFloat(),
            steps = (maxDbm - minDbm - 1).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RsrpHistogramWindowSlider(
    windowMs: Long,
    elapsedMs: Long?,
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
            val elapsedLabel = formatHistogramElapsedLabel(elapsedMs)
            Text(
                text = if (elapsedLabel != null) {
                    "${stringResource(R.string.rsrp_histogram_window_label)} · $elapsedLabel"
                } else {
                    stringResource(R.string.rsrp_histogram_window_label)
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatHistogramWindow(windowMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            enabled = settingsControlsEnabled(),
            value = sliderValue,
            onValueChange = { value ->
                onWindowChange(minMs + value.roundToInt() * stepMs)
            },
            valueRange = 0f..stepCount.toFloat(),
            steps = (stepCount - 1).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.rsrp_histogram_window_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        totalSeconds < 60L -> "${totalSeconds} s"
        totalSeconds % 60L == 0L -> "${totalSeconds / 60L} m"
        else -> "${totalSeconds / 60L} m ${totalSeconds % 60L} s"
    }
}

@Composable
private fun formatHistogramElapsedLabel(elapsedMs: Long?): String? {
    if (elapsedMs == null) return null
    val totalSeconds = elapsedMs / 1_000L
    return stringResource(
        R.string.rsrp_histogram_elapsed,
        (totalSeconds / 60L).toInt(),
        (totalSeconds % 60L).toInt()
    )
}
