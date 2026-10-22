package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.ActiveTestBand
import io.github.cloolalang.notspotdetector.model.ActiveTestHistory
import io.github.cloolalang.notspotdetector.model.ActiveTestSegment
import io.github.cloolalang.notspotdetector.model.ActiveTestTape
import io.github.cloolalang.notspotdetector.model.AggregatedBandwidthBand
import io.github.cloolalang.notspotdetector.model.AggregatedBandwidthHistory
import io.github.cloolalang.notspotdetector.model.AggregatedBandwidthSegment
import io.github.cloolalang.notspotdetector.model.AggregatedBandwidthTape
import io.github.cloolalang.notspotdetector.model.ConnectivityStats
import io.github.cloolalang.notspotdetector.model.DistanceHistory
import io.github.cloolalang.notspotdetector.model.DistanceSegment
import io.github.cloolalang.notspotdetector.model.DistanceTape
import io.github.cloolalang.notspotdetector.model.DistanceVisit
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.PrimaryCarrierHistory
import io.github.cloolalang.notspotdetector.model.PrimaryCarrierTape
import io.github.cloolalang.notspotdetector.model.PrimaryCarrierTapeEntry
import io.github.cloolalang.notspotdetector.model.RsrpBin
import io.github.cloolalang.notspotdetector.model.RsrpBinHistory
import io.github.cloolalang.notspotdetector.model.RsrpBinSegment
import io.github.cloolalang.notspotdetector.model.RsrpBinTape
import io.github.cloolalang.notspotdetector.model.RsrpHistogram
import io.github.cloolalang.notspotdetector.model.RsrpHistogramBand
import io.github.cloolalang.notspotdetector.model.RsrpHistogramBin
import io.github.cloolalang.notspotdetector.model.RsrpHistogramBinKind
import io.github.cloolalang.notspotdetector.model.RsrpHistogramBinningMode
import io.github.cloolalang.notspotdetector.model.RsrpHistogramThresholdBarColor
import io.github.cloolalang.notspotdetector.model.RsrpMeanColorBand
import io.github.cloolalang.notspotdetector.model.RsrpSample
import io.github.cloolalang.notspotdetector.model.RsrpSampleTrend
import io.github.cloolalang.notspotdetector.model.ServingCellSelection
import io.github.cloolalang.notspotdetector.model.TapeAge
import io.github.cloolalang.notspotdetector.model.TapeAgeUnit
import io.github.cloolalang.notspotdetector.model.TapeTimeline
import io.github.cloolalang.notspotdetector.model.TechnologyHistory
import io.github.cloolalang.notspotdetector.model.TechnologySegment
import io.github.cloolalang.notspotdetector.model.TechnologyTape
import io.github.cloolalang.notspotdetector.model.TimingAdvance
import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import io.github.cloolalang.notspotdetector.model.coerceToHistogramWindowStep
import io.github.cloolalang.notspotdetector.ui.theme.BondiBlue
import io.github.cloolalang.notspotdetector.ui.theme.Sushi
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val HistogramBarAreaHeight = 80.dp
private val HistogramLabelAreaHeight = 34.dp
private val HistogramMinBarWidth = 12.dp
private val HistogramMaxBarWidth = 36.dp

/**
 * Level histogram chart, plus a separate collapsible KPI history panel for the tapes.
 * Mode, thresholds, and sample window live in a nested Histogram settings sub-panel.
 * Both stay on screen after monitoring stops so the last chart and tapes remain.
 */
@Composable
fun HistogramControlsCard(
    samples: List<RsrpSample>,
    stats: ConnectivityStats,
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
    var histogramExpanded by rememberSaveable { mutableStateOf(false) }
    var kpiHistoryExpanded by rememberSaveable { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var carrierTape by remember { mutableStateOf(PrimaryCarrierTape()) }
    var nrCarrierTape by remember { mutableStateOf(PrimaryCarrierTape()) }
    var technologyTape by remember { mutableStateOf(TechnologyTape()) }
    var rsrpBinTape by remember { mutableStateOf(RsrpBinTape()) }
    var distanceTape by remember { mutableStateOf(DistanceTape()) }
    var activeTestTape by remember { mutableStateOf(ActiveTestTape()) }
    var aggregatedBandwidthTape by remember { mutableStateOf(AggregatedBandwidthTape()) }
    val currentCarrier = PrimaryCarrierHistory.visitFromStats(stats)
    val currentNrCarrier = PrimaryCarrierHistory.nrVisitFromStats(stats)
    val currentTechnology = TechnologyHistory.technologyFromStats(stats)
    val currentRsrpBin = RsrpBinHistory.classify(
        samples = samples,
        nowMs = nowMs,
        noSignal = TechnologyHistory.isNoSignalPeriod(stats)
    )
    val currentDistance = DistanceHistory.current(
        samples = samples,
        nowMs = nowMs,
        noSignal = TechnologyHistory.isNoSignalPeriod(stats)
    )
    val currentActiveTest = ActiveTestHistory.current(stats)
    val currentAggregatedBandwidth = AggregatedBandwidthHistory.current(stats)
    fun resetTapes(now: Long = System.currentTimeMillis()) {
        carrierTape = PrimaryCarrierHistory.record(PrimaryCarrierTape(), currentCarrier, now)
        nrCarrierTape = PrimaryCarrierHistory.record(PrimaryCarrierTape(), currentNrCarrier, now)
        technologyTape = TechnologyHistory.record(TechnologyTape(), currentTechnology, now)
        rsrpBinTape = RsrpBinHistory.record(RsrpBinTape(), currentRsrpBin, now)
        distanceTape = DistanceHistory.record(DistanceTape(), currentDistance, now)
        activeTestTape = ActiveTestHistory.record(ActiveTestTape(), currentActiveTest, now)
        aggregatedBandwidthTape = AggregatedBandwidthHistory.record(
            AggregatedBandwidthTape(),
            currentAggregatedBandwidth,
            now
        )
    }
    LaunchedEffect(currentCarrier) {
        carrierTape = PrimaryCarrierHistory.record(
            carrierTape,
            currentCarrier,
            System.currentTimeMillis()
        )
    }
    LaunchedEffect(currentNrCarrier) {
        nrCarrierTape = PrimaryCarrierHistory.record(
            nrCarrierTape,
            currentNrCarrier,
            System.currentTimeMillis()
        )
    }
    LaunchedEffect(currentTechnology) {
        technologyTape = TechnologyHistory.record(
            technologyTape,
            currentTechnology,
            System.currentTimeMillis()
        )
    }
    LaunchedEffect(currentRsrpBin, isActive) {
        if (!isActive) return@LaunchedEffect
        rsrpBinTape = RsrpBinHistory.record(
            rsrpBinTape,
            currentRsrpBin,
            System.currentTimeMillis()
        )
    }
    LaunchedEffect(currentDistance, isActive) {
        if (!isActive) return@LaunchedEffect
        distanceTape = DistanceHistory.record(
            distanceTape,
            currentDistance,
            System.currentTimeMillis()
        )
    }
    LaunchedEffect(currentActiveTest, isActive) {
        if (!isActive) return@LaunchedEffect
        activeTestTape = ActiveTestHistory.record(
            activeTestTape,
            currentActiveTest,
            System.currentTimeMillis()
        )
    }
    LaunchedEffect(currentAggregatedBandwidth, isActive) {
        if (!isActive) return@LaunchedEffect
        aggregatedBandwidthTape = AggregatedBandwidthHistory.record(
            aggregatedBandwidthTape,
            currentAggregatedBandwidth,
            System.currentTimeMillis()
        )
    }

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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { histogramExpanded = !histogramExpanded },
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
                    text = if (histogramExpanded) "▲" else "▼",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (histogramExpanded) {
                RsrpHistogramDisplay(
                    samples = samples,
                    windowMs = windowMs.coerceToHistogramWindowStep(),
                    binningMode = binningMode,
                    thresholdsDbm = thresholdsDbm,
                    nowMs = nowMs,
                    elapsedMs = elapsedMs,
                    cellInfoAgeMs = stats.cellInfoAgeMs
                )

                OutlinedButton(
                    onClick = {
                        resetTapes()
                        onClearHistogram()
                    },
                    enabled = samples.isNotEmpty() ||
                        carrierTape.entries.size > 1 ||
                        nrCarrierTape.entries.size > 1 ||
                        technologyTape.entries.size > 1 ||
                        rsrpBinTape.entries.size > 1 ||
                        distanceTape.entries.size > 1 ||
                        activeTestTape.entries.size > 1 ||
                        aggregatedBandwidthTape.entries.size > 1,
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
    KpiHistoryPanel(
        expanded = kpiHistoryExpanded,
        onExpandedChange = { kpiHistoryExpanded = it },
        nowMs = nowMs,
        carrierEntries = carrierTape.entries,
        nrCarrierEntries = nrCarrierTape.entries,
        reselectsPerMinute = stats.cellReselectsPerMinute,
        technologyEntries = technologyTape.entries,
        technologyChangesPerMinute = stats.technologyChangesPerMinute,
        rsrpBinEntries = rsrpBinTape.entries,
        distanceEntries = distanceTape.entries,
        activeTestEntries = activeTestTape.entries,
        aggregatedBandwidthEntries = aggregatedBandwidthTape.entries,
        onClearHistory = { resetTapes() }
    )
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
    cellInfoAgeMs: Long?,
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
                rsrpDbm = RsrpHistogram.lastSampleRsrpDbm(samples, nowMs, windowMs)?.toDouble(),
                scanAgeMs = cellInfoAgeMs
            )
        }
    }
}

@Composable
private fun KpiHistoryPanel(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    nowMs: Long,
    carrierEntries: List<PrimaryCarrierTapeEntry>,
    nrCarrierEntries: List<PrimaryCarrierTapeEntry>,
    reselectsPerMinute: Int,
    technologyEntries: List<TechnologySegment>,
    technologyChangesPerMinute: Int,
    rsrpBinEntries: List<RsrpBinSegment>,
    distanceEntries: List<DistanceSegment>,
    activeTestEntries: List<ActiveTestSegment>,
    aggregatedBandwidthEntries: List<AggregatedBandwidthSegment>,
    onClearHistory: () -> Unit
) {
    val tapeWindowStartMs = TapeTimeline.windowStartMs(
        carrierEntries.map { it.startedAtMs } +
            nrCarrierEntries.map { it.startedAtMs } +
            technologyEntries.map { it.startedAtMs } +
            rsrpBinEntries.map { it.startedAtMs } +
            distanceEntries.map { it.startedAtMs } +
            activeTestEntries.map { it.startedAtMs } +
            aggregatedBandwidthEntries.map { it.startedAtMs }
    ) ?: nowMs
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.kpi_history_title),
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
                PrimaryCarrierHistoryBar(
                    title = stringResource(R.string.rsrp_histogram_carrier_history_title),
                    detail = stringResource(
                        R.string.metric_cell_reselect_rate_value,
                        reselectsPerMinute
                    ),
                    entries = carrierEntries,
                    nowMs = nowMs,
                    windowStartMs = tapeWindowStartMs
                )
                PrimaryCarrierHistoryBar(
                    title = stringResource(R.string.rsrp_histogram_nr_carrier_history_title),
                    detail = null,
                    entries = nrCarrierEntries,
                    nowMs = nowMs,
                    windowStartMs = tapeWindowStartMs
                )
                TechnologyHistoryBar(
                    entries = technologyEntries,
                    changesPerMinute = technologyChangesPerMinute,
                    nowMs = nowMs,
                    windowStartMs = tapeWindowStartMs
                )
                RsrpBinHistoryBar(
                    entries = rsrpBinEntries,
                    nowMs = nowMs,
                    windowStartMs = tapeWindowStartMs
                )
                DistanceHistoryBar(
                    entries = distanceEntries,
                    nowMs = nowMs,
                    windowStartMs = tapeWindowStartMs
                )
                ActiveTestHistoryBar(
                    entries = activeTestEntries,
                    nowMs = nowMs,
                    windowStartMs = tapeWindowStartMs
                )
                AggregatedBandwidthHistoryBar(
                    entries = aggregatedBandwidthEntries,
                    nowMs = nowMs,
                    windowStartMs = tapeWindowStartMs
                )
                OutlinedButton(
                    onClick = onClearHistory,
                    enabled = carrierEntries.isNotEmpty() ||
                        nrCarrierEntries.isNotEmpty() ||
                        technologyEntries.isNotEmpty() ||
                        rsrpBinEntries.isNotEmpty() ||
                        distanceEntries.isNotEmpty() ||
                        activeTestEntries.isNotEmpty() ||
                        aggregatedBandwidthEntries.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.kpi_history_clear))
                }
            }
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
                            stringResource(
                                R.string.rsrp_histogram_other_bin_label,
                                bin.labelDbm ?: RsrpHistogram.DEFAULT_THRESHOLD_3_DBM
                            )
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
    scanAgeMs: Long? = null,
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
        if (scanAgeMs != null && ServingCellSelection.isScanAgeNotable(scanAgeMs)) {
            Text(
                text = stringResource(
                    R.string.rsrp_histogram_last_scan_age,
                    ServingCellSelection.scanAgeSeconds(scanAgeMs)
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
private fun PrimaryCarrierHistoryBar(
    title: String,
    detail: String?,
    entries: List<PrimaryCarrierTapeEntry>,
    nowMs: Long,
    windowStartMs: Long,
    modifier: Modifier = Modifier
) {
    val current = entries.lastOrNull()
    val valueText = when {
        current == null -> stringResource(R.string.signal_unavailable)
        current.noSignal || current.pair == null -> stringResource(R.string.signal_unavailable)
        else -> stringResource(
            R.string.rsrp_histogram_carrier_history_pair,
            current.pair.channel,
            current.pair.pci
        )
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Sushi
                )
                if (detail != null) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(trackColor)
        ) {
            if (entries.isEmpty()) return@Canvas
            val canvasScope = this
            val bounds = tapeSegmentBounds(
                startsMs = entries.map { it.startedAtMs },
                windowStartMs = windowStartMs,
                nowMs = nowMs,
                tapeWidth = size.width
            )
            entries.forEachIndexed { index, entry ->
                val (left, segmentWidth) = bounds[index]
                if (segmentWidth <= 0f) return@forEachIndexed
                val color = carrierTapeColor(entry)
                val label = if (entry.noSignal || entry.pair == null) {
                    TechnologyHistory.LABEL_NO_SIGNAL
                } else {
                    "${entry.pair.channel}/${entry.pair.pci}"
                }
                canvasScope.clipRect(
                    left = left,
                    top = 0f,
                    right = left + segmentWidth,
                    bottom = size.height
                ) {
                    drawRect(
                        color = color,
                        topLeft = Offset(left, 0f),
                        size = Size(segmentWidth, size.height)
                    )
                    val layout = textMeasurer.measure(
                        text = label,
                        style = labelStyle.merge(TextStyle(color = carrierTapeLabelColor(color))),
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        maxLines = 1,
                        constraints = Constraints(
                            maxWidth = segmentWidth.roundToInt().coerceAtLeast(0)
                        )
                    )
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(
                            x = left + (segmentWidth - layout.size.width) / 2f,
                            y = (size.height - layout.size.height) / 2f
                        )
                    )
                }
            }
        }
        TapeTimeScale(
            oldestStartedAtMs = if (entries.isEmpty()) null else windowStartMs,
            nowMs = nowMs
        )
    }
}

private val CarrierNeutralShades = listOf(
    Color(0xFF263238),
    Color(0xFF37474F),
    Color(0xFF455A64),
    Color(0xFF546E7A),
    Color(0xFF607D8B),
    Color(0xFF78909C),
    Color(0xFF90A4AE),
    Color(0xFFB0BEC5)
)

private fun carrierTapeColor(entry: PrimaryCarrierTapeEntry): Color {
    if (entry.noSignal) return Color(0xFF90A4AE)
    val mhz = entry.pair?.nominalMhz
    val hue = PrimaryCarrierHistory.hueForMhz(mhz)
        ?: return CarrierNeutralShades[entry.shade.mod(CarrierNeutralShades.size)]
    val slot = entry.shade.mod(PrimaryCarrierHistory.SHADES_PER_BAND)
    val hueJitter = (slot - 3.5f) * if (mhz == 2600) 6f else 3f
    val value = (0.42f + slot * 0.065f).coerceAtMost(0.92f)
    val saturation = when {
        mhz != null && mhz < 1_000 -> 0.9f
        mhz == 2600 -> 0.8f
        else -> 0.72f
    }
    return Color.hsv((hue + hueJitter + 360f) % 360f, saturation, value)
}

private fun carrierTapeLabelColor(background: Color): Color {
    val luminance = (0.299f * background.red) + (0.587f * background.green) + (0.114f * background.blue)
    return if (luminance > 0.62f) Color.Black else Color.White
}

@Composable
private fun TechnologyHistoryBar(
    entries: List<TechnologySegment>,
    changesPerMinute: Int,
    nowMs: Long,
    windowStartMs: Long,
    modifier: Modifier = Modifier
) {
    val current = entries.lastOrNull()
    val valueText = current?.label ?: stringResource(R.string.signal_unavailable)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.rsrp_histogram_technology_history_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Sushi
                )
                Text(
                    text = stringResource(
                        R.string.metric_cell_reselect_rate_value,
                        changesPerMinute
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(trackColor)
        ) {
            if (entries.isEmpty()) return@Canvas
            val canvasScope = this
            val bounds = tapeSegmentBounds(
                startsMs = entries.map { it.startedAtMs },
                windowStartMs = windowStartMs,
                nowMs = nowMs,
                tapeWidth = size.width
            )
            entries.forEachIndexed { index, segment ->
                val (left, segmentWidth) = bounds[index]
                if (segmentWidth <= 0f) return@forEachIndexed
                val color = technologyTapeColor(segment.label)
                canvasScope.clipRect(
                    left = left,
                    top = 0f,
                    right = left + segmentWidth,
                    bottom = size.height
                ) {
                    drawRect(
                        color = color,
                        topLeft = Offset(left, 0f),
                        size = Size(segmentWidth, size.height)
                    )
                    val layout = textMeasurer.measure(
                        text = segment.label,
                        style = labelStyle.merge(TextStyle(color = carrierTapeLabelColor(color))),
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        maxLines = 1,
                        constraints = Constraints(
                            maxWidth = segmentWidth.roundToInt().coerceAtLeast(0)
                        )
                    )
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(
                            x = left + (segmentWidth - layout.size.width) / 2f,
                            y = (size.height - layout.size.height) / 2f
                        )
                    )
                }
            }
        }
        TapeTimeScale(
            oldestStartedAtMs = if (entries.isEmpty()) null else windowStartMs,
            nowMs = nowMs
        )
    }
}

@Composable
private fun RsrpBinHistoryBar(
    entries: List<RsrpBinSegment>,
    nowMs: Long,
    windowStartMs: Long,
    modifier: Modifier = Modifier
) {
    val current = entries.lastOrNull()
    val valueText = current?.let { rsrpBinLabel(it.bin) }
        ?: stringResource(R.string.signal_unavailable)
    val segmentLabels = entries.map { rsrpBinLabel(it.bin) }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.rsrp_bin_history_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Sushi
                )
                Text(
                    text = stringResource(R.string.rsrp_bin_history_rule),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(trackColor)
        ) {
            if (entries.isEmpty()) return@Canvas
            val canvasScope = this
            val bounds = tapeSegmentBounds(
                startsMs = entries.map { it.startedAtMs },
                windowStartMs = windowStartMs,
                nowMs = nowMs,
                tapeWidth = size.width
            )
            entries.forEachIndexed { index, segment ->
                val (left, segmentWidth) = bounds[index]
                if (segmentWidth <= 0f) return@forEachIndexed
                val color = rsrpBinTapeColor(segment.bin)
                val label = segmentLabels[index]
                canvasScope.clipRect(
                    left = left,
                    top = 0f,
                    right = left + segmentWidth,
                    bottom = size.height
                ) {
                    drawRect(
                        color = color,
                        topLeft = Offset(left, 0f),
                        size = Size(segmentWidth, size.height)
                    )
                    val layout = textMeasurer.measure(
                        text = label,
                        style = labelStyle.merge(TextStyle(color = carrierTapeLabelColor(color))),
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        maxLines = 1,
                        constraints = Constraints(
                            maxWidth = segmentWidth.roundToInt().coerceAtLeast(0)
                        )
                    )
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(
                            x = left + (segmentWidth - layout.size.width) / 2f,
                            y = (size.height - layout.size.height) / 2f
                        )
                    )
                }
            }
        }
        TapeTimeScale(
            oldestStartedAtMs = if (entries.isEmpty()) null else windowStartMs,
            nowMs = nowMs
        )
    }
}

@Composable
private fun rsrpBinLabel(bin: RsrpBin): String {
    return when (bin) {
        RsrpBin.ABOVE_95 -> stringResource(R.string.rsrp_bin_above_95)
        RsrpBin.ABOVE_105 -> stringResource(R.string.rsrp_bin_above_105)
        RsrpBin.ABOVE_115 -> stringResource(R.string.rsrp_bin_above_115)
        RsrpBin.BELOW_115 -> stringResource(R.string.rsrp_bin_below_115)
        RsrpBin.NO_SIGNAL -> TechnologyHistory.LABEL_NO_SIGNAL
    }
}

private fun rsrpBinTapeColor(bin: RsrpBin): Color {
    return when (bin) {
        RsrpBin.ABOVE_95 -> MeanRsrpLightBlue
        RsrpBin.ABOVE_105 -> MeanRsrpGreen
        RsrpBin.ABOVE_115 -> MeanRsrpYellow
        RsrpBin.BELOW_115 -> MeanRsrpRed
        RsrpBin.NO_SIGNAL -> Color(0xFF90A4AE)
    }
}

@Composable
private fun DistanceHistoryBar(
    entries: List<DistanceSegment>,
    nowMs: Long,
    windowStartMs: Long,
    modifier: Modifier = Modifier
) {
    val current = entries.lastOrNull()
    val valueText = current?.let { distanceLabel(it.metres) }
        ?: stringResource(R.string.signal_unavailable)
    val segmentLabels = entries.map { distanceLabel(it.metres) }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.distance_history_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Sushi
                )
                Text(
                    text = stringResource(R.string.distance_history_rule),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(trackColor)
        ) {
            if (entries.isEmpty()) return@Canvas
            val canvasScope = this
            val bounds = tapeSegmentBounds(
                startsMs = entries.map { it.startedAtMs },
                windowStartMs = windowStartMs,
                nowMs = nowMs,
                tapeWidth = size.width
            )
            entries.forEachIndexed { index, segment ->
                val (left, segmentWidth) = bounds[index]
                if (segmentWidth <= 0f) return@forEachIndexed
                val color = distanceTapeColor(segment.metres)
                val label = segmentLabels[index]
                canvasScope.clipRect(
                    left = left,
                    top = 0f,
                    right = left + segmentWidth,
                    bottom = size.height
                ) {
                    drawRect(
                        color = color,
                        topLeft = Offset(left, 0f),
                        size = Size(segmentWidth, size.height)
                    )
                    val layout = textMeasurer.measure(
                        text = label,
                        style = labelStyle.merge(TextStyle(color = carrierTapeLabelColor(color))),
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        maxLines = 1,
                        constraints = Constraints(
                            maxWidth = segmentWidth.roundToInt().coerceAtLeast(0)
                        )
                    )
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(
                            x = left + (segmentWidth - layout.size.width) / 2f,
                            y = (size.height - layout.size.height) / 2f
                        )
                    )
                }
            }
        }
        TapeTimeScale(
            oldestStartedAtMs = if (entries.isEmpty()) null else windowStartMs,
            nowMs = nowMs
        )
    }
}

private fun distanceLabel(metres: Int?): String {
    if (metres == null) return TechnologyHistory.LABEL_NO_SIGNAL
    return TimingAdvance.formatOneWayDistance(metres)
}

private fun distanceTapeColor(metres: Int?): Color {
    return when {
        metres == null -> Color(0xFF90A4AE)
        metres < 500 -> MeanRsrpLightBlue
        metres < 1_000 -> MeanRsrpGreen
        metres < 3_000 -> MeanRsrpYellow
        else -> MeanRsrpRed
    }
}

@Composable
private fun ActiveTestHistoryBar(
    entries: List<ActiveTestSegment>,
    nowMs: Long,
    windowStartMs: Long,
    modifier: Modifier = Modifier
) {
    val current = entries.lastOrNull()
    val valueText = current?.let { ActiveTestHistory.label(it) }
        ?: stringResource(R.string.signal_unavailable)
    val segmentLabels = entries.map { ActiveTestHistory.label(it) }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.active_test_history_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Sushi
                )
                Text(
                    text = stringResource(R.string.active_test_history_rule),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(trackColor)
        ) {
            if (entries.isEmpty()) return@Canvas
            val canvasScope = this
            val bounds = tapeSegmentBounds(
                startsMs = entries.map { it.startedAtMs },
                windowStartMs = windowStartMs,
                nowMs = nowMs,
                tapeWidth = size.width
            )
            entries.forEachIndexed { index, segment ->
                val (left, segmentWidth) = bounds[index]
                if (segmentWidth <= 0f) return@forEachIndexed
                val color = activeTestTapeColor(segment.band)
                val label = segmentLabels[index]
                canvasScope.clipRect(
                    left = left,
                    top = 0f,
                    right = left + segmentWidth,
                    bottom = size.height
                ) {
                    drawRect(
                        color = color,
                        topLeft = Offset(left, 0f),
                        size = Size(segmentWidth, size.height)
                    )
                    val layout = textMeasurer.measure(
                        text = label,
                        style = labelStyle.merge(TextStyle(color = carrierTapeLabelColor(color))),
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        maxLines = 1,
                        constraints = Constraints(
                            maxWidth = segmentWidth.roundToInt().coerceAtLeast(0)
                        )
                    )
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(
                            x = left + (segmentWidth - layout.size.width) / 2f,
                            y = (size.height - layout.size.height) / 2f
                        )
                    )
                }
            }
        }
        TapeTimeScale(
            oldestStartedAtMs = if (entries.isEmpty()) null else windowStartMs,
            nowMs = nowMs
        )
    }
}

private fun activeTestTapeColor(band: ActiveTestBand): Color {
    return when (band) {
        ActiveTestBand.NO_MEASUREMENT -> Color(0xFF90A4AE)
        ActiveTestBand.TOTAL_LOSS -> Color.Black
        ActiveTestBand.HIGH_LOSS,
        ActiveTestBand.VERY_SLOW -> MeanRsrpRed
        ActiveTestBand.SLOW -> MeanRsrpYellow
        ActiveTestBand.OK -> MeanRsrpGreen
        ActiveTestBand.FAST -> MeanRsrpLightBlue
    }
}

@Composable
private fun AggregatedBandwidthHistoryBar(
    entries: List<AggregatedBandwidthSegment>,
    nowMs: Long,
    windowStartMs: Long,
    modifier: Modifier = Modifier
) {
    val current = entries.lastOrNull()
    val valueText = current?.let { AggregatedBandwidthHistory.label(it) }
        ?: stringResource(R.string.signal_unavailable)
    val segmentLabels = entries.map { AggregatedBandwidthHistory.label(it) }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.aggregated_bandwidth_history_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Sushi
                )
                Text(
                    text = stringResource(R.string.aggregated_bandwidth_history_rule),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(trackColor)
        ) {
            if (entries.isEmpty()) return@Canvas
            val canvasScope = this
            val bounds = tapeSegmentBounds(
                startsMs = entries.map { it.startedAtMs },
                windowStartMs = windowStartMs,
                nowMs = nowMs,
                tapeWidth = size.width
            )
            entries.forEachIndexed { index, segment ->
                val (left, segmentWidth) = bounds[index]
                if (segmentWidth <= 0f) return@forEachIndexed
                val color = aggregatedBandwidthTapeColor(segment.band)
                val label = segmentLabels[index]
                canvasScope.clipRect(
                    left = left,
                    top = 0f,
                    right = left + segmentWidth,
                    bottom = size.height
                ) {
                    drawRect(
                        color = color,
                        topLeft = Offset(left, 0f),
                        size = Size(segmentWidth, size.height)
                    )
                    val layout = textMeasurer.measure(
                        text = label,
                        style = labelStyle.merge(TextStyle(color = carrierTapeLabelColor(color))),
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        maxLines = 1,
                        constraints = Constraints(
                            maxWidth = segmentWidth.roundToInt().coerceAtLeast(0)
                        )
                    )
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(
                            x = left + (segmentWidth - layout.size.width) / 2f,
                            y = (size.height - layout.size.height) / 2f
                        )
                    )
                }
            }
        }
        TapeTimeScale(
            oldestStartedAtMs = if (entries.isEmpty()) null else windowStartMs,
            nowMs = nowMs
        )
    }
}

private fun aggregatedBandwidthTapeColor(band: AggregatedBandwidthBand): Color {
    return when (band) {
        AggregatedBandwidthBand.NONE -> Color(0xFF90A4AE)
        AggregatedBandwidthBand.MHZ_5 -> MeanRsrpRed
        AggregatedBandwidthBand.MHZ_10 -> MeanRsrpYellow
        AggregatedBandwidthBand.MHZ_15 -> MeanRsrpOrange
        AggregatedBandwidthBand.MHZ_20 -> MeanRsrpGreen
        AggregatedBandwidthBand.BLUE_40 -> Color(0xFFB3E5FC)
        AggregatedBandwidthBand.BLUE_60 -> Color(0xFF81D4FA)
        AggregatedBandwidthBand.BLUE_100 -> Color(0xFF29B6F6)
        AggregatedBandwidthBand.BLUE_150 -> Color(0xFF0288D1)
        AggregatedBandwidthBand.BLUE_WIDE -> Color(0xFF01579B)
    }
}

private fun tapeSegmentBounds(
    startsMs: List<Long>,
    windowStartMs: Long,
    nowMs: Long,
    tapeWidth: Float
): List<Pair<Float, Float>> {
    return TapeTimeline.placements(startsMs, windowStartMs, nowMs).map { placement ->
        val width = (placement.endFraction - placement.startFraction) * tapeWidth
        // Now is the right edge. Older time runs to the left, on both tapes.
        val left = placement.startFraction * tapeWidth
        left to width
    }
}

private fun technologyTapeColor(technology: String): Color {
    if (technology == TechnologyHistory.LABEL_NO_SIGNAL) return Color(0xFF90A4AE)
    val hue = when (technology) {
        CellularSignalReader.RADIO_2G -> 28f
        CellularSignalReader.RADIO_4G -> 215f
        TechnologyHistory.LABEL_4G_PLUS -> 195f
        CellularSignalReader.RADIO_5G -> 278f
        CellularSignalReader.RADIO_5G_ENDC -> 168f
        else -> return Color(0xFF546E7A)
    }
    return Color.hsv(hue, 0.72f, 0.55f)
}

@Composable
private fun TapeTimeScale(
    oldestStartedAtMs: Long?,
    nowMs: Long
) {
    if (oldestStartedAtMs == null) return
    val age = tapeAgeText(oldestStartedAtMs, nowMs)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.rsrp_histogram_tape_ago, age),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.rsrp_histogram_tape_now),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun tapeAgeText(startedAtMs: Long, nowMs: Long): String {
    val amount = TapeAge.amount(startedAtMs, nowMs)
    return when (amount.unit) {
        TapeAgeUnit.SECONDS -> stringResource(R.string.rsrp_histogram_tape_seconds, amount.value.toInt())
        TapeAgeUnit.MINUTES -> stringResource(R.string.rsrp_histogram_tape_minutes, amount.value.toInt())
        TapeAgeUnit.HOURS -> stringResource(R.string.rsrp_histogram_tape_hours, amount.value.toInt())
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
    val controlsEnabled = settingsControlsEnabled()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = controlsEnabled, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            enabled = controlsEnabled
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
