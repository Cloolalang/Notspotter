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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.cloolalang.notspotdetector.MonitorState
import io.github.cloolalang.notspotdetector.model.MultiUeRole
import io.github.cloolalang.notspotdetector.multiue.MultiUeGraphClear
import io.github.cloolalang.notspotdetector.multiue.MultiUeState
import io.github.cloolalang.notspotdetector.multiue.MultiUeFloorPlan
import io.github.cloolalang.notspotdetector.multiue.MultiUeLinkService
import io.github.cloolalang.notspotdetector.multiue.MultiUeZoneSync
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.data.FloorPlanLoader
import io.github.cloolalang.notspotdetector.data.FloorPlanTransfer
import io.github.cloolalang.notspotdetector.data.WalkRouteImage
import io.github.cloolalang.notspotdetector.data.ZoneTestImageSaver
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
import io.github.cloolalang.notspotdetector.model.IntracellDominanceSegment
import io.github.cloolalang.notspotdetector.model.IntracellDominanceTape
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.PrimaryIntracellDominanceHistory
import io.github.cloolalang.notspotdetector.model.OperatorTitleStyle
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.PrimaryLayerDominance
import io.github.cloolalang.notspotdetector.model.primaryLayerDominance
import io.github.cloolalang.notspotdetector.model.coerceToTapeWindowStep
import io.github.cloolalang.notspotdetector.model.PrimaryCarrierHistory
import io.github.cloolalang.notspotdetector.model.PrimaryCarrierTape
import io.github.cloolalang.notspotdetector.model.PrimaryCarrierTapeEntry
import io.github.cloolalang.notspotdetector.model.RsrpBin
import io.github.cloolalang.notspotdetector.model.RsrpBinHistory
import io.github.cloolalang.notspotdetector.model.RsrpBinSegment
import io.github.cloolalang.notspotdetector.model.RsrpBinTape
import io.github.cloolalang.notspotdetector.model.RsrqBin
import io.github.cloolalang.notspotdetector.model.RsrqBinHistory
import io.github.cloolalang.notspotdetector.model.RsrqBinSegment
import io.github.cloolalang.notspotdetector.model.RsrqBinTape
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
import io.github.cloolalang.notspotdetector.model.TapeTimeline
import io.github.cloolalang.notspotdetector.model.TechnologyHistory
import io.github.cloolalang.notspotdetector.model.TechnologySegment
import io.github.cloolalang.notspotdetector.model.TechnologyTape
import io.github.cloolalang.notspotdetector.model.TimingAdvance
import io.github.cloolalang.notspotdetector.model.WalkLegendSwatch
import io.github.cloolalang.notspotdetector.model.WalkPin
import io.github.cloolalang.notspotdetector.model.WalkRoute
import io.github.cloolalang.notspotdetector.model.ZoneTest
import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import io.github.cloolalang.notspotdetector.model.coerceToHistogramWindowStep
import io.github.cloolalang.notspotdetector.ui.theme.BondiBlue
import io.github.cloolalang.notspotdetector.ui.theme.Sushi
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    passiveSignalSettings: PassiveSignalSettings,
    windowMs: Long,
    tapeWindowMs: Long,
    binningMode: RsrpHistogramBinningMode,
    thresholdsDbm: List<Int>,
    isActive: Boolean,
    onWindowChange: (Long) -> Unit,
    onTapeWindowChange: (Long) -> Unit,
    onBinningModeChange: (RsrpHistogramBinningMode) -> Unit,
    onThresholdChange: (Int, Int) -> Unit,
    onClearHistogram: () -> Unit,
    onClearKpiHistory: () -> Unit = {},
    walkRouteEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    var histogramExpanded by panelFold("histogram", defaultExpanded = false)
    var kpiHistoryExpanded by panelFold("kpi-history", defaultExpanded = false)
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val kpiHistory = viewModel<KpiHistoryViewModel>()
    var carrierTape by kpiHistory.carrierTape
    var technologyTape by kpiHistory.technologyTape
    var rsrpBinTape by kpiHistory.rsrpBinTape
    var rsrqBinTape by kpiHistory.rsrqBinTape
    var distanceTape by kpiHistory.distanceTape
    var intracellDominanceTape by kpiHistory.intracellDominanceTape
    var activeTestTape by kpiHistory.activeTestTape
    var aggregatedBandwidthTape by kpiHistory.aggregatedBandwidthTape
    val currentCarrier = PrimaryCarrierHistory.visitFromStats(stats)
    val currentTechnology = TechnologyHistory.technologyFromStats(stats)
    val sampleWindowMs = windowMs.coerceToHistogramWindowStep()
    val currentRsrpBin = RsrpBinHistory.classify(
        samples = samples,
        nowMs = nowMs,
        noSignal = TechnologyHistory.isNoSignalPeriod(stats),
        windowMs = sampleWindowMs
    )
    val currentRsrqBin = RsrqBinHistory.classify(
        samples = samples,
        nowMs = nowMs,
        noSignal = TechnologyHistory.isNoSignalPeriod(stats),
        windowMs = sampleWindowMs
    )
    val currentDistance = DistanceHistory.current(
        samples = samples,
        nowMs = nowMs,
        noSignal = TechnologyHistory.isNoSignalPeriod(stats)
    )
    val currentIntracellDominance = PrimaryIntracellDominanceHistory.current(stats, passiveSignalSettings)
    val currentActiveTest = ActiveTestHistory.current(stats)
    val currentAggregatedBandwidth = AggregatedBandwidthHistory.current(stats)
    // The tape clock holds still while monitoring is stopped, and that pause is left out of the scale.
    val testsRunning = isActive
    var tapePauseOffsetMs by kpiHistory.tapePauseOffsetMs
    var tapeHeldWallMs by kpiHistory.tapeHeldWallMs
    val heldWallMs = tapeHeldWallMs
    if (testsRunning && heldWallMs != null) {
        tapePauseOffsetMs += nowMs - heldWallMs
        tapeHeldWallMs = null
    } else if (!testsRunning && heldWallMs == null) {
        tapeHeldWallMs = nowMs
    }
    val tapeNowMs = (tapeHeldWallMs ?: nowMs) - tapePauseOffsetMs
    fun resetTapes(now: Long = tapeNowMs) {
        if (!testsRunning) {
            carrierTape = PrimaryCarrierTape()
            technologyTape = TechnologyTape()
            rsrpBinTape = RsrpBinTape()
            rsrqBinTape = RsrqBinTape()
            distanceTape = DistanceTape()
            intracellDominanceTape = IntracellDominanceTape()
            activeTestTape = ActiveTestTape()
            aggregatedBandwidthTape = AggregatedBandwidthTape()
            return
        }
        carrierTape = PrimaryCarrierHistory.record(PrimaryCarrierTape(), currentCarrier, now)
        technologyTape = TechnologyHistory.record(TechnologyTape(), currentTechnology, now)
        rsrpBinTape = RsrpBinHistory.record(RsrpBinTape(), currentRsrpBin, now)
        rsrqBinTape = RsrqBinHistory.record(RsrqBinTape(), currentRsrqBin, now)
        distanceTape = DistanceHistory.record(DistanceTape(), currentDistance, now)
        intracellDominanceTape = PrimaryIntracellDominanceHistory.record(
            IntracellDominanceTape(),
            currentIntracellDominance,
            now
        )
        activeTestTape = ActiveTestHistory.record(ActiveTestTape(), currentActiveTest, now)
        aggregatedBandwidthTape = AggregatedBandwidthHistory.record(
            AggregatedBandwidthTape(),
            currentAggregatedBandwidth,
            now
        )
    }
    val resetTapesNow = rememberUpdatedState<() -> Unit>({ resetTapes() })
    LaunchedEffect(Unit) {
        MultiUeGraphClear.tapes.collect { resetTapesNow.value.invoke() }
    }
    LaunchedEffect(currentCarrier, testsRunning) {
        if (!testsRunning) return@LaunchedEffect
        carrierTape = PrimaryCarrierHistory.record(carrierTape, currentCarrier, tapeNowMs)
    }
    LaunchedEffect(currentTechnology, testsRunning) {
        if (!testsRunning) return@LaunchedEffect
        technologyTape = TechnologyHistory.record(technologyTape, currentTechnology, tapeNowMs)
    }
    LaunchedEffect(currentRsrpBin, testsRunning) {
        if (!testsRunning) return@LaunchedEffect
        rsrpBinTape = RsrpBinHistory.record(rsrpBinTape, currentRsrpBin, tapeNowMs)
    }
    LaunchedEffect(currentRsrqBin, testsRunning) {
        if (!testsRunning) return@LaunchedEffect
        rsrqBinTape = RsrqBinHistory.record(rsrqBinTape, currentRsrqBin, tapeNowMs)
    }
    LaunchedEffect(currentDistance, testsRunning) {
        if (!testsRunning) return@LaunchedEffect
        distanceTape = DistanceHistory.record(distanceTape, currentDistance, tapeNowMs)
    }
    LaunchedEffect(currentIntracellDominance, testsRunning) {
        if (!testsRunning) return@LaunchedEffect
        intracellDominanceTape = PrimaryIntracellDominanceHistory.record(
            intracellDominanceTape,
            currentIntracellDominance,
            tapeNowMs
        )
    }
    LaunchedEffect(currentActiveTest, testsRunning) {
        if (!testsRunning) return@LaunchedEffect
        activeTestTape = ActiveTestHistory.record(activeTestTape, currentActiveTest, tapeNowMs)
    }
    LaunchedEffect(currentAggregatedBandwidth, testsRunning) {
        if (!testsRunning) return@LaunchedEffect
        aggregatedBandwidthTape = AggregatedBandwidthHistory.record(
            aggregatedBandwidthTape,
            currentAggregatedBandwidth,
            tapeNowMs
        )
    }

    LaunchedEffect(samples) {
        nowMs = System.currentTimeMillis()
    }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val elapsedMs = RsrpHistogram.elapsedSinceFirstSampleMs(
        samples = samples,
        nowMs = nowMs,
        windowMs = sampleWindowMs
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
                    windowMs = sampleWindowMs,
                    binningMode = binningMode,
                    thresholdsDbm = thresholdsDbm,
                    nowMs = nowMs,
                    elapsedMs = elapsedMs,
                    cellInfoAgeMs = stats.cellInfoAgeMs
                )

                OutlinedButton(
                    onClick = onClearHistogram,
                    enabled = samples.isNotEmpty() ||
                        carrierTape.entries.size > 1 ||
                        technologyTape.entries.size > 1 ||
                        rsrpBinTape.entries.size > 1 ||
                        rsrqBinTape.entries.size > 1 ||
                        distanceTape.entries.size > 1 ||
                        intracellDominanceTape.entries.size > 1 ||
                        activeTestTape.entries.size > 1 ||
                        aggregatedBandwidthTape.entries.size > 1,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.rsrp_histogram_clear))
                }

                HistogramSettingsPanel(
                    windowMs = sampleWindowMs,
                    tapeWindowMs = tapeWindowMs.coerceToTapeWindowStep(),
                    binningMode = binningMode,
                    thresholdsDbm = thresholdsDbm,
                    elapsedMs = elapsedMs,
                    onWindowChange = onWindowChange,
                    onTapeWindowChange = onTapeWindowChange,
                    onBinningModeChange = onBinningModeChange,
                    onThresholdChange = onThresholdChange
                )
            }
        }
    }
    val currentOperator = OperatorTitleStyle.resolveLabel(stats)
    var tapeOperator by kpiHistory.tapeOperator
    if (currentOperator != null && currentOperator != tapeOperator) {
        tapeOperator = currentOperator
    }
    ZoneTestPanel(
        isMonitoring = isActive,
        stats = stats,
        passiveSignalSettings = passiveSignalSettings,
        samples = samples,
        nowMs = nowMs,
        tapeNowMs = tapeNowMs,
        sampleWindowMs = sampleWindowMs,
        walkRouteEnabled = walkRouteEnabled
    )
    KpiHistoryPanel(
        expanded = kpiHistoryExpanded,
        onExpandedChange = { kpiHistoryExpanded = it },
        operatorLabel = tapeOperator,
        nowMs = tapeNowMs,
        carrierEntries = carrierTape.entries,
        reselectsPerMinute = stats.cellReselectsPerMinute,
        technologyEntries = technologyTape.entries,
        technologyChangesPerMinute = stats.technologyChangesPerMinute,
        rsrpBinEntries = rsrpBinTape.entries,
        rsrqBinEntries = rsrqBinTape.entries,
        distanceEntries = distanceTape.entries,
        intracellDominanceEntries = intracellDominanceTape.entries,
        activeTestEntries = activeTestTape.entries,
        aggregatedBandwidthEntries = aggregatedBandwidthTape.entries,
        tapeWindowMs = tapeWindowMs.coerceToTapeWindowStep(),
        onClearHistory = onClearKpiHistory
    )
    }
}

@Composable
private fun HistogramSettingsPanel(
    windowMs: Long,
    tapeWindowMs: Long,
    binningMode: RsrpHistogramBinningMode,
    thresholdsDbm: List<Int>,
    elapsedMs: Long?,
    onWindowChange: (Long) -> Unit,
    onTapeWindowChange: (Long) -> Unit,
    onBinningModeChange: (RsrpHistogramBinningMode) -> Unit,
    onThresholdChange: (Int, Int) -> Unit
) {
    var expanded by panelFold("histogram-settings", defaultExpanded = false)

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
                RsrpHistogramWindowControl(
                    windowMs = windowMs,
                    elapsedMs = elapsedMs,
                    onWindowChange = onWindowChange
                )
                TapeWindowControl(
                    windowMs = tapeWindowMs,
                    onWindowChange = onTapeWindowChange
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
    operatorLabel: String?,
    nowMs: Long,
    carrierEntries: List<PrimaryCarrierTapeEntry>,
    reselectsPerMinute: Int,
    technologyEntries: List<TechnologySegment>,
    technologyChangesPerMinute: Int,
    rsrpBinEntries: List<RsrpBinSegment>,
    rsrqBinEntries: List<RsrqBinSegment>,
    distanceEntries: List<DistanceSegment>,
    intracellDominanceEntries: List<IntracellDominanceSegment>,
    activeTestEntries: List<ActiveTestSegment>,
    aggregatedBandwidthEntries: List<AggregatedBandwidthSegment>,
    tapeWindowMs: Long,
    onClearHistory: () -> Unit
) {
    val tapeWindowStartMs = TapeTimeline.windowStartMs(nowMs, tapeWindowMs)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.kpi_history_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = Sushi,
                        fontWeight = FontWeight.Bold
                    )
                    if (operatorLabel != null) {
                        Text(
                            text = operatorLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
                RsrqBinHistoryBar(
                    entries = rsrqBinEntries,
                    nowMs = nowMs,
                    windowStartMs = tapeWindowStartMs
                )
                IntracellDominanceHistoryBar(
                    entries = intracellDominanceEntries,
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
                        technologyEntries.isNotEmpty() ||
                        rsrpBinEntries.isNotEmpty() ||
                        rsrqBinEntries.isNotEmpty() ||
                        intracellDominanceEntries.isNotEmpty() ||
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

private fun slavePinTime(now: Long, pins: List<WalkPin>): Long {
    val last = pins.lastOrNull()?.placedAtMs ?: return now
    return if (now > last) now else last + 1L
}

@Composable
private fun ZoneTestPanel(
    isMonitoring: Boolean,
    stats: ConnectivityStats,
    passiveSignalSettings: PassiveSignalSettings,
    samples: List<RsrpSample>,
    nowMs: Long,
    tapeNowMs: Long,
    sampleWindowMs: Long,
    walkRouteEnabled: Boolean
) {
    var expanded by panelFold("zone-test", defaultExpanded = false)
    val zoneTest = viewModel<ZoneTestViewModel>()
    var running by zoneTest.running
    var startMs by zoneTest.startMs
    var endMs by zoneTest.endMs
    var openSampleWindow by rememberSaveable { mutableStateOf(false) }
    var sampleWindowStartMs by zoneTest.sampleWindowStartMs
    var zoneName by zoneTest.zoneName
    var operatorLabel by zoneTest.operatorLabel
    var statusMessage by zoneTest.statusMessage
    var carrierTape by zoneTest.carrierTape
    var technologyTape by zoneTest.technologyTape
    var rsrpBinTape by zoneTest.rsrpBinTape
    var rsrqBinTape by zoneTest.rsrqBinTape
    var distanceTape by zoneTest.distanceTape
    var intracellDominanceTape by zoneTest.intracellDominanceTape
    var activeTestTape by zoneTest.activeTestTape
    var aggregatedBandwidthTape by zoneTest.aggregatedBandwidthTape
    val noSignal = TechnologyHistory.isNoSignalPeriod(stats)
    val currentCarrier = PrimaryCarrierHistory.visitFromStats(stats)
    val currentTechnology = TechnologyHistory.technologyFromStats(stats)
    val zoneSampleWindowMs = ZoneTest.sampleWindowMs(
        openForWholeTest = openSampleWindow && running,
        wallStartMs = sampleWindowStartMs,
        nowMs = nowMs,
        sliderWindowMs = sampleWindowMs
    )
    val zoneSampleRule = if (openSampleWindow && sampleWindowStartMs != null) {
        stringResource(R.string.zone_test_sample_window_rule)
    } else {
        null
    }
    val currentRsrpBin = RsrpBinHistory.classify(samples, nowMs, noSignal, zoneSampleWindowMs)
    val currentRsrqBin = RsrqBinHistory.classify(samples, nowMs, noSignal, zoneSampleWindowMs)
    LaunchedEffect(openSampleWindow, running, sampleWindowStartMs) {
        val retainFrom = if (openSampleWindow && running) sampleWindowStartMs else null
        MonitorState.setZoneTestSampleRetainFrom(retainFrom)
    }
    val currentDistance = DistanceHistory.current(samples, nowMs, noSignal)
    val currentIntracellDominance = PrimaryIntracellDominanceHistory.current(stats, passiveSignalSettings)
    val currentActiveTest = ActiveTestHistory.current(stats)
    val currentAggregatedBandwidth = AggregatedBandwidthHistory.current(stats)
    val liveOperator = OperatorTitleStyle.resolveLabel(stats)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var floorPlan by zoneTest.floorPlan
    var pins by zoneTest.pins
    val graphicsLayer = rememberGraphicsLayer()
    val savedTemplate = stringResource(R.string.zone_test_saved)
    val savedWithRouteTemplate = stringResource(R.string.zone_test_saved_with_route)
    val routeFailedTemplate = stringResource(R.string.zone_test_route_failed)
    val failedText = stringResource(R.string.zone_test_save_failed)
    val floorPlanFailed = rememberUpdatedState(stringResource(R.string.zone_test_floor_plan_failed))
    val routeLegend = listOf(
        WalkLegendSwatch(WalkRoute.rsrpRouteColor(RsrpBin.ABOVE_95), stringResource(R.string.rsrp_bin_above_95)),
        WalkLegendSwatch(WalkRoute.rsrpRouteColor(RsrpBin.ABOVE_105), stringResource(R.string.rsrp_bin_above_105)),
        WalkLegendSwatch(WalkRoute.rsrpRouteColor(RsrpBin.ABOVE_115), stringResource(R.string.rsrp_bin_above_115)),
        WalkLegendSwatch(WalkRoute.rsrpRouteColor(RsrpBin.BELOW_115), stringResource(R.string.rsrp_bin_below_115)),
        WalkLegendSwatch(WalkRoute.rsrpRouteColor(RsrpBin.NO_SIGNAL), stringResource(R.string.zone_test_no_signal))
    )
    val walkCaptionFallback = stringResource(R.string.zone_test_walk_caption)
    val routeLegendTitle = stringResource(R.string.zone_test_route_legend_title)
    val pickFloorPlan = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        FloorPlanLoader.keepPermission(context, uri)
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                FloorPlanLoader.decode(context, uri)
            }
            if (bitmap == null) {
                statusMessage = floorPlanFailed.value
            } else {
                floorPlan = bitmap
                pins = emptyList()
                statusMessage = null
            }
        }
    }

    fun freshTapes(atMs: Long) {
        carrierTape = PrimaryCarrierHistory.record(
            PrimaryCarrierTape(),
            currentCarrier,
            atMs,
            ZoneTest.RETAIN_MS
        )
        technologyTape = TechnologyHistory.record(
            TechnologyTape(),
            currentTechnology,
            atMs,
            ZoneTest.RETAIN_MS
        )
        rsrpBinTape = RsrpBinHistory.record(RsrpBinTape(), currentRsrpBin, atMs, ZoneTest.RETAIN_MS)
        rsrqBinTape = RsrqBinHistory.record(RsrqBinTape(), currentRsrqBin, atMs, ZoneTest.RETAIN_MS)
        distanceTape = DistanceHistory.record(DistanceTape(), currentDistance, atMs, ZoneTest.RETAIN_MS)
        intracellDominanceTape = PrimaryIntracellDominanceHistory.record(
            IntracellDominanceTape(),
            currentIntracellDominance,
            atMs,
            ZoneTest.RETAIN_MS
        )
        activeTestTape = ActiveTestHistory.record(
            ActiveTestTape(),
            currentActiveTest,
            atMs,
            ZoneTest.RETAIN_MS
        )
        aggregatedBandwidthTape = AggregatedBandwidthHistory.record(
            AggregatedBandwidthTape(),
            currentAggregatedBandwidth,
            atMs,
            ZoneTest.RETAIN_MS
        )
    }

    val link by MultiUeState.snapshot.collectAsStateWithLifecycle()
    val followsMaster = link.followsMaster
    val remoteZone by MultiUeZoneSync.state.collectAsStateWithLifecycle()
    fun walkKpi(): String? = if (walkRouteEnabled) ZoneTest.WALK_KPI else null
    val startZoneNow = rememberUpdatedState<() -> Unit>({
        val atMs = tapeNowMs
        startMs = atMs
        sampleWindowStartMs = nowMs
        endMs = null
        zoneName = ZoneTest.fileNamePrefix(liveOperator, walkKpi())
        statusMessage = null
        operatorLabel = liveOperator
        pins = emptyList()
        freshTapes(atMs)
        running = true
    })
    val stopZoneNow = rememberUpdatedState<() -> Unit>({
        running = false
        endMs = tapeNowMs
    })
    val clearZoneCharts = rememberUpdatedState<() -> Unit>({
        running = false
        startMs = null
        sampleWindowStartMs = null
        endMs = null
        statusMessage = null
        pins = emptyList()
        if (!followsMaster) {
            zoneName = ""
            operatorLabel = null
        }
        carrierTape = PrimaryCarrierTape()
        technologyTape = TechnologyTape()
        rsrpBinTape = RsrpBinTape()
        rsrqBinTape = RsrqBinTape()
        distanceTape = DistanceTape()
        intracellDominanceTape = IntracellDominanceTape()
        activeTestTape = ActiveTestTape()
        aggregatedBandwidthTape = AggregatedBandwidthTape()
    })
    val saveZoneJpeg = rememberUpdatedState<() -> Unit>({
        val started = startMs
        if (started == null) {
            statusMessage = failedText
            return@rememberUpdatedState
        }
        val markedEnd = if (running) tapeNowMs else endMs ?: tapeNowMs
        scope.launch {
            val bitmap = runCatching {
                val image = graphicsLayer.toImageBitmap()
                val raw = image.asAndroidBitmap()
                if (raw.config == Bitmap.Config.HARDWARE) {
                    raw.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    raw
                }
            }.getOrNull()
            if (bitmap == null) {
                statusMessage = failedText
                return@launch
            }
            val zonePath = ZoneTestImageSaver.save(
                context = context,
                bitmap = bitmap,
                name = zoneName,
                operator = operatorLabel
            )
            val plan = floorPlan
            val markedPins = pins.toList()
            var walkPath: String? = null
            if (walkRouteEnabled && plan != null && markedPins.isNotEmpty()) {
                val caption = listOfNotNull(
                    zoneName.trim().takeIf { it.isNotEmpty() },
                    operatorLabel
                ).joinToString(" · ").ifBlank { walkCaptionFallback }
                val routeBitmap = WalkRouteImage.render(
                    floorPlan = plan,
                    pins = markedPins,
                    segments = rsrpBinTape.entries,
                    zoneStartMs = started,
                    zoneEndMs = markedEnd,
                    caption = caption,
                    legendTitle = routeLegendTitle,
                    legend = routeLegend
                )
                if (routeBitmap != null) {
                    walkPath = ZoneTestImageSaver.saveJpeg(
                        context = context,
                        bitmap = routeBitmap,
                        fileName = ZoneTest.walkJpegFileName(zoneName, operatorLabel)
                    )
                    routeBitmap.recycle()
                }
            }
            statusMessage = when {
                zonePath != null && walkPath != null ->
                    savedWithRouteTemplate.format(zonePath, walkPath)
                zonePath != null && walkRouteEnabled && markedPins.isNotEmpty() ->
                    routeFailedTemplate.format(zonePath)
                zonePath != null -> savedTemplate.format(zonePath)
                walkPath != null -> savedTemplate.format(walkPath)
                else -> failedText
            }
        }
    })
    val remoteZoneName by MultiUeZoneSync.nameSuffix.collectAsStateWithLifecycle()
    if (link.role == MultiUeRole.MASTER) {
        LaunchedEffect(Unit) {
            snapshotFlow { running to openSampleWindow }
                .collect { (zoneRunning, wholeTest) ->
                    MultiUeZoneSync.set(zoneRunning, wholeTest)
                }
        }
        LaunchedEffect(liveOperator, operatorLabel, walkRouteEnabled) {
            snapshotFlow { zoneName }
                .collect { name ->
                    MultiUeZoneSync.setNameSuffix(
                        ZoneTest.sharedNameSuffix(name, operatorLabel ?: liveOperator, walkKpi())
                    )
                }
        }
        var planSent by remember { mutableStateOf(false) }
        LaunchedEffect(floorPlan) {
            val bitmap = floorPlan
            if (bitmap == null) {
                if (planSent) MultiUeFloorPlan.clearPlan()
                planSent = false
                return@LaunchedEffect
            }
            val jpeg = withContext(Dispatchers.IO) { FloorPlanTransfer.jpeg(bitmap) }
            if (jpeg.isEmpty()) return@LaunchedEffect
            MultiUeFloorPlan.setJpeg(jpeg)
            planSent = true
        }
        LaunchedEffect(Unit) {
            var previous = emptyList<Pair<Float, Float>>()
            snapshotFlow { pins.map { it.xFraction to it.yFraction } }
                .collect { next ->
                    MultiUeFloorPlan.publishPins(previous, next)
                    previous = next
                }
        }
    }
    if (followsMaster) {
        LaunchedEffect(remoteZone, isMonitoring) {
            val command = remoteZone ?: return@LaunchedEffect
            if (openSampleWindow != command.wholeTest) openSampleWindow = command.wholeTest
            if (command.running) {
                if (!running && isMonitoring) startZoneNow.value.invoke()
            } else if (running) {
                stopZoneNow.value.invoke()
            }
        }
        LaunchedEffect(remoteZoneName, operatorLabel, liveOperator, walkRouteEnabled) {
            val suffix = remoteZoneName ?: return@LaunchedEffect
            val next = ZoneTest.nameWithOperator(operatorLabel ?: liveOperator, suffix, walkKpi())
            if (zoneName != next) zoneName = next
        }
        val remotePlan by MultiUeFloorPlan.jpeg.collectAsStateWithLifecycle()
        val pinClock = rememberUpdatedState(if (running) tapeNowMs else endMs ?: tapeNowMs)
        LaunchedEffect(remotePlan) {
            val bytes = remotePlan ?: return@LaunchedEffect
            if (bytes.isEmpty()) {
                floorPlan = null
                return@LaunchedEffect
            }
            val bitmap = withContext(Dispatchers.IO) { FloorPlanTransfer.decode(bytes) } ?: return@LaunchedEffect
            floorPlan = bitmap
            if (!expanded) expanded = true
        }
        LaunchedEffect(Unit) {
            var previous = emptyList<MultiUeFloorPlan.PinPoint>()
            MultiUeFloorPlan.pins.collect { remote ->
                val local = pins
                pins = when {
                    remote.isEmpty() -> emptyList()
                    remote.size == previous.size + 1 && remote.dropLast(1) == previous -> {
                        val point = remote.last()
                        local + WalkPin(slavePinTime(pinClock.value, local), point.x, point.y)
                    }
                    remote.size + 1 == previous.size && previous.dropLast(1) == remote ->
                        local.dropLast(1)
                    else -> {
                        val now = pinClock.value
                        remote.mapIndexed { index, point ->
                            val existing = local.getOrNull(index)
                            if (
                                existing != null &&
                                existing.xFraction == point.x &&
                                existing.yFraction == point.y
                            ) {
                                existing
                            } else {
                                WalkPin(now + index, point.x, point.y)
                            }
                        }
                    }
                }
                previous = remote
            }
        }
        LaunchedEffect(Unit) {
            MultiUeZoneSync.clears.collect {
                clearZoneCharts.value.invoke()
            }
        }
        LaunchedEffect(Unit) {
            MultiUeZoneSync.saves.collect {
                if (!expanded) expanded = true
                withFrameNanos { }
                withFrameNanos { }
                saveZoneJpeg.value.invoke()
            }
        }
    }

    LaunchedEffect(isMonitoring) {
        if (!isMonitoring && running) {
            running = false
            endMs = tapeNowMs
        }
    }
    LaunchedEffect(liveOperator, running, walkRouteEnabled) {
        if (running && liveOperator != null) {
            operatorLabel = liveOperator
            if (!followsMaster && zoneName.isBlank()) {
                zoneName = ZoneTest.fileNamePrefix(liveOperator, walkKpi())
            }
        }
    }
    LaunchedEffect(running, startMs, operatorLabel, liveOperator, walkRouteEnabled) {
        if (!followsMaster && !running && startMs != null && zoneName.isBlank()) {
            val prefix = ZoneTest.fileNamePrefix(operatorLabel ?: liveOperator, walkKpi())
            if (prefix.isNotEmpty()) zoneName = prefix
        }
    }
    LaunchedEffect(currentCarrier, running) {
        if (!running) return@LaunchedEffect
        carrierTape = PrimaryCarrierHistory.record(carrierTape, currentCarrier, tapeNowMs, ZoneTest.RETAIN_MS)
    }
    LaunchedEffect(currentTechnology, running) {
        if (!running) return@LaunchedEffect
        technologyTape = TechnologyHistory.record(
            technologyTape,
            currentTechnology,
            tapeNowMs,
            ZoneTest.RETAIN_MS
        )
    }
    LaunchedEffect(currentRsrpBin, running) {
        if (!running) return@LaunchedEffect
        rsrpBinTape = RsrpBinHistory.record(rsrpBinTape, currentRsrpBin, tapeNowMs, ZoneTest.RETAIN_MS)
    }
    LaunchedEffect(currentRsrqBin, running) {
        if (!running) return@LaunchedEffect
        rsrqBinTape = RsrqBinHistory.record(rsrqBinTape, currentRsrqBin, tapeNowMs, ZoneTest.RETAIN_MS)
    }
    LaunchedEffect(currentDistance, running) {
        if (!running) return@LaunchedEffect
        distanceTape = DistanceHistory.record(distanceTape, currentDistance, tapeNowMs, ZoneTest.RETAIN_MS)
    }
    LaunchedEffect(currentIntracellDominance, running) {
        if (!running) return@LaunchedEffect
        intracellDominanceTape = PrimaryIntracellDominanceHistory.record(
            intracellDominanceTape,
            currentIntracellDominance,
            tapeNowMs,
            ZoneTest.RETAIN_MS
        )
    }
    LaunchedEffect(currentActiveTest, running) {
        if (!running) return@LaunchedEffect
        activeTestTape = ActiveTestHistory.record(
            activeTestTape,
            currentActiveTest,
            tapeNowMs,
            ZoneTest.RETAIN_MS
        )
    }
    LaunchedEffect(currentAggregatedBandwidth, running) {
        if (!running) return@LaunchedEffect
        aggregatedBandwidthTape = AggregatedBandwidthHistory.record(
            aggregatedBandwidthTape,
            currentAggregatedBandwidth,
            tapeNowMs,
            ZoneTest.RETAIN_MS
        )
    }

    val zoneStart = startMs
    val chartNowMs = if (running) tapeNowMs else endMs ?: tapeNowMs
    val chartTitle = stringResource(R.string.kpi_history_title)
    val zoneTickTimes = if (zoneStart != null && walkRouteEnabled) {
        WalkRoute.tickTimes(zoneStart, chartNowMs)
    } else {
        emptyList()
    }
    val markTimeMs = when {
        zoneStart == null -> null
        running -> tapeNowMs
        else -> endMs
    }

    Card(modifier = Modifier.fillMaxWidth()) {
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
                    text = stringResource(R.string.zone_test_title),
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
                text = stringResource(
                    when {
                        running -> R.string.zone_test_running
                        !isMonitoring && startMs == null -> R.string.zone_test_need_monitoring
                        walkRouteEnabled -> R.string.zone_test_hint_walk
                        else -> R.string.zone_test_hint
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (running) {
                Button(
                    onClick = { stopZoneNow.value.invoke() },
                    enabled = !followsMaster,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.zone_test_stop))
                }
            } else {
                Button(
                    onClick = { startZoneNow.value.invoke() },
                    enabled = isMonitoring && !followsMaster,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.zone_test_start))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.zone_test_open_sample_window),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.zone_test_open_sample_window_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = openSampleWindow,
                    onCheckedChange = { openSampleWindow = it },
                    enabled = !followsMaster
                )
            }
            if (walkRouteEnabled || (followsMaster && floorPlan != null)) {
            WalkRouteMap(
                modifier = Modifier.expandPastPadding(20.dp),
                floorPlan = floorPlan,
                pins = pins,
                rsrpSegments = rsrpBinTape.entries,
                zoneStartMs = zoneStart,
                markTimeMs = markTimeMs,
                liveBin = if (running) rsrpBinTape.entries.lastOrNull()?.bin else null,
                marksFromMaster = followsMaster,
                onChooseFloorPlan = { pickFloorPlan.launch(arrayOf("image/*")) },
                onMark = { pins = pins + it },
                onUndo = { if (pins.isNotEmpty()) pins = pins.dropLast(1) }
            )
            }
            if (!running && startMs != null) {
                OutlinedTextField(
                    value = zoneName,
                    onValueChange = { zoneName = it },
                    enabled = !followsMaster,
                    label = { Text(stringResource(R.string.zone_test_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (walkRouteEnabled && pins.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.zone_test_save_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        saveZoneJpeg.value.invoke()
                        if (link.role == MultiUeRole.MASTER) {
                            MultiUeLinkService.broadcastZoneSave(context)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(
                            if (walkRouteEnabled && pins.isNotEmpty()) {
                                R.string.zone_test_save_with_route
                            } else {
                                R.string.zone_test_save
                            }
                        )
                    )
                }
                OutlinedButton(
                    onClick = {
                        clearZoneCharts.value.invoke()
                        if (link.role == MultiUeRole.MASTER) {
                            MultiUeLinkService.broadcastZoneClear(context)
                        }
                    },
                    enabled = !followsMaster,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(
                            if (walkRouteEnabled && pins.isNotEmpty()) {
                                R.string.zone_test_clear_walk
                            } else {
                                R.string.zone_test_clear
                            }
                        )
                    )
                }
            }
            if (zoneStart != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawWithContent {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        }
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = chartTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = Sushi,
                        fontWeight = FontWeight.Bold
                    )
                    if (operatorLabel != null) {
                        Text(
                            text = operatorLabel!!,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (zoneName.isNotBlank()) {
                        Text(
                            text = zoneName.trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    PrimaryCarrierHistoryBar(
                        title = stringResource(R.string.rsrp_histogram_carrier_history_title),
                        detail = stringResource(
                            R.string.metric_cell_reselect_rate_value,
                            stats.cellReselectsPerMinute
                        ),
                        entries = carrierTape.entries,
                        nowMs = chartNowMs,
                        windowStartMs = zoneStart,
                        timeTickMs = zoneTickTimes
                    )
                    TechnologyHistoryBar(
                        entries = technologyTape.entries,
                        changesPerMinute = stats.technologyChangesPerMinute,
                        nowMs = chartNowMs,
                        windowStartMs = zoneStart,
                        timeTickMs = zoneTickTimes
                    )
                    RsrpBinHistoryBar(
                        entries = rsrpBinTape.entries,
                        nowMs = chartNowMs,
                        windowStartMs = zoneStart,
                        timeTickMs = zoneTickTimes,
                        ruleLabel = zoneSampleRule
                    )
                    RsrqBinHistoryBar(
                        entries = rsrqBinTape.entries,
                        nowMs = chartNowMs,
                        windowStartMs = zoneStart,
                        ruleLabel = zoneSampleRule,
                        timeTickMs = zoneTickTimes
                    )
                    IntracellDominanceHistoryBar(
                        entries = intracellDominanceTape.entries,
                        nowMs = chartNowMs,
                        windowStartMs = zoneStart,
                        timeTickMs = zoneTickTimes
                    )
                    DistanceHistoryBar(
                        entries = distanceTape.entries,
                        nowMs = chartNowMs,
                        windowStartMs = zoneStart,
                        timeTickMs = zoneTickTimes
                    )
                    ActiveTestHistoryBar(
                        entries = activeTestTape.entries,
                        nowMs = chartNowMs,
                        windowStartMs = zoneStart,
                        timeTickMs = zoneTickTimes
                    )
                    AggregatedBandwidthHistoryBar(
                        entries = aggregatedBandwidthTape.entries,
                        nowMs = chartNowMs,
                        windowStartMs = zoneStart,
                        timeTickMs = zoneTickTimes
                    )
                    ZoneTickLabelRow(
                        tickTimesMs = zoneTickTimes,
                        windowStartMs = zoneStart,
                        nowMs = chartNowMs
                    )
                }
            }
            if (statusMessage != null) {
                Text(
                    text = statusMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

/**
 * The live reading sits on one line beside the title. Letting it wrap changes the
 * card height each time a test updates that reading, so the panel jumps on a narrow
 * phone or a large font. The em dash and minus sign also fall back to a taller font
 * on some phones; turning font padding off keeps that line the same height.
 */
@Composable
private fun TapeSectionHeader(
    title: String,
    detail: String?,
    value: String
) {
    val titleStyle = MaterialTheme.typography.labelMedium.copy(
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
    val detailStyle = MaterialTheme.typography.labelSmall.copy(
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
    val density = LocalDensity.current
    val titleHeight = titleStyle.lineHeight.let { line ->
        if (line.isSp) with(density) { line.toDp() } else 16.dp
    }
    val detailHeight = detailStyle.lineHeight.let { line ->
        if (line.isSp) with(density) { line.toDp() } else 16.dp
    }
    // Lock the row. A fallback font for "—" or "−" is taller on some phones, and
    // that extra height was resizing the card on every reading change.
    val rowHeight = if (detail != null) titleHeight + 2.dp + detailHeight else titleHeight
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = titleStyle,
                fontWeight = FontWeight.SemiBold,
                color = Sushi,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = detailStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = value,
            style = titleStyle,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 168.dp)
        )
    }
}

@Composable
private fun PrimaryCarrierHistoryBar(
    title: String,
    detail: String?,
    entries: List<PrimaryCarrierTapeEntry>,
    nowMs: Long,
    windowStartMs: Long,
    timeTickMs: List<Long> = emptyList(),
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
    val dividerColor = MaterialTheme.colorScheme.surface
    val dividerWidth = with(LocalDensity.current) { 1.5.dp.toPx() }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TapeSectionHeader(title = title, detail = detail, value = valueText)
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
                if (index > 0 && left > 0f) {
                    drawRect(
                        color = dividerColor,
                        topLeft = Offset(left - dividerWidth / 2f, 0f),
                        size = Size(dividerWidth, size.height)
                    )
                }
            }
            drawZoneTimeTicks(timeTickMs, windowStartMs, nowMs)
        }
        TapeTimeScale(windowMs = (nowMs - windowStartMs).coerceAtLeast(0L))
    }
}

private val CarrierNeutralShades = listOf(
    Color(0xFF37474F),
    Color(0xFFB0BEC5),
    Color(0xFF607D8B),
    Color(0xFF263238),
    Color(0xFF90A4AE),
    Color(0xFF455A64),
    Color(0xFFCFD8DC),
    Color(0xFF546E7A)
)

private data class CarrierShadeStep(val hueOffset: Float, val saturation: Float, val value: Float)

/**
 * Cells on one band take these in order. Each step flips between dark and light
 * so a reselect between the first two or three cells reads as a clear change.
 */
private val CarrierShadeSteps = listOf(
    CarrierShadeStep(hueOffset = 0f, saturation = 0.90f, value = 0.48f),
    CarrierShadeStep(hueOffset = 14f, saturation = 0.40f, value = 0.95f),
    CarrierShadeStep(hueOffset = -14f, saturation = 0.95f, value = 0.72f),
    CarrierShadeStep(hueOffset = 8f, saturation = 0.65f, value = 0.34f),
    CarrierShadeStep(hueOffset = -8f, saturation = 0.70f, value = 0.88f),
    CarrierShadeStep(hueOffset = 20f, saturation = 0.55f, value = 0.58f),
    CarrierShadeStep(hueOffset = -20f, saturation = 0.25f, value = 0.98f),
    CarrierShadeStep(hueOffset = 4f, saturation = 0.95f, value = 0.40f)
)

private fun carrierTapeColor(entry: PrimaryCarrierTapeEntry): Color {
    if (entry.noSignal) return Color(0xFF90A4AE)
    val mhz = entry.pair?.nominalMhz
    val hue = PrimaryCarrierHistory.hueForMhz(mhz)
        ?: return CarrierNeutralShades[entry.shade.mod(CarrierNeutralShades.size)]
    val step = CarrierShadeSteps[entry.shade.mod(CarrierShadeSteps.size)]
    val hueSpread = if (mhz != null && mhz < 1_000) 0.6f else 1f
    val hue360 = (hue + step.hueOffset * hueSpread + 360f) % 360f
    return Color.hsv(hue360, step.saturation, step.value)
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
    timeTickMs: List<Long> = emptyList(),
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
        TapeSectionHeader(
            title = stringResource(R.string.rsrp_histogram_technology_history_title),
            detail = stringResource(
                R.string.metric_cell_reselect_rate_value,
                changesPerMinute
            ),
            value = valueText
        )
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
            drawZoneTimeTicks(timeTickMs, windowStartMs, nowMs)
        }
        TapeTimeScale(windowMs = (nowMs - windowStartMs).coerceAtLeast(0L))
    }
}

@Composable
private fun RsrpBinHistoryBar(
    entries: List<RsrpBinSegment>,
    nowMs: Long,
    windowStartMs: Long,
    timeTickMs: List<Long> = emptyList(),
    ruleLabel: String? = null,
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
        TapeSectionHeader(
            title = stringResource(R.string.rsrp_bin_history_title),
            detail = ruleLabel ?: stringResource(R.string.rsrp_bin_history_rule),
            value = valueText
        )
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
            drawZoneTimeTicks(timeTickMs, windowStartMs, nowMs)
        }
        TapeTimeScale(windowMs = (nowMs - windowStartMs).coerceAtLeast(0L))
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
private fun RsrqBinHistoryBar(
    entries: List<RsrqBinSegment>,
    nowMs: Long,
    windowStartMs: Long,
    timeTickMs: List<Long> = emptyList(),
    ruleLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val current = entries.lastOrNull()
    val valueText = current?.let { rsrqBinLabel(it.bin) }
        ?: stringResource(R.string.signal_unavailable)
    val segmentLabels = entries.map { rsrqBinLabel(it.bin) }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TapeSectionHeader(
            title = stringResource(R.string.rsrq_bin_history_title),
            detail = ruleLabel ?: stringResource(R.string.rsrp_bin_history_rule),
            value = valueText
        )
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
                val color = rsrqBinTapeColor(segment.bin)
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
            drawZoneTimeTicks(timeTickMs, windowStartMs, nowMs)
        }
        TapeTimeScale(windowMs = (nowMs - windowStartMs).coerceAtLeast(0L))
    }
}

@Composable
private fun rsrqBinLabel(bin: RsrqBin): String {
    return when (bin) {
        RsrqBin.ABOVE_15 -> stringResource(R.string.rsrq_bin_above_15)
        RsrqBin.FROM_17_TO_15 -> stringResource(R.string.rsrq_bin_from_17_to_15)
        RsrqBin.BELOW_17 -> stringResource(R.string.rsrq_bin_below_17)
        RsrqBin.NO_SIGNAL,
        RsrqBin.NO_RSRQ -> TechnologyHistory.LABEL_NO_SIGNAL
    }
}

private fun rsrqBinTapeColor(bin: RsrqBin): Color {
    return when (bin) {
        RsrqBin.ABOVE_15 -> MeanRsrpGreen
        RsrqBin.FROM_17_TO_15 -> MeanRsrpYellow
        RsrqBin.BELOW_17 -> MeanRsrpRed
        RsrqBin.NO_SIGNAL,
        RsrqBin.NO_RSRQ -> Color(0xFF90A4AE)
    }
}

@Composable
private fun IntracellDominanceHistoryBar(
    entries: List<IntracellDominanceSegment>,
    nowMs: Long,
    windowStartMs: Long,
    timeTickMs: List<Long> = emptyList(),
    modifier: Modifier = Modifier
) {
    val current = entries.lastOrNull()
    val valueText = current?.let { intracellDominanceLabel(it.gapDb) }
        ?: stringResource(R.string.signal_unavailable)
    val segmentLabels = entries.map { intracellDominanceLabel(it.gapDb) }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TapeSectionHeader(
            title = stringResource(R.string.intracell_dominance_history_title),
            detail = stringResource(R.string.intracell_dominance_history_rule),
            value = valueText
        )
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
                val color = intracellDominanceTapeColor(segment.gapDb)
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
            drawZoneTimeTicks(timeTickMs, windowStartMs, nowMs)
        }
        TapeTimeScale(windowMs = (nowMs - windowStartMs).coerceAtLeast(0L))
    }
}

@Composable
private fun intracellDominanceLabel(gapDb: Int?): String {
    val dominanceDb = gapDb ?: return TechnologyHistory.LABEL_NO_SIGNAL
    val band = when (primaryLayerDominance(dominanceDb)) {
        PrimaryLayerDominance.LOW -> stringResource(R.string.primary_layer_dominance_low)
        PrimaryLayerDominance.HIGH -> stringResource(R.string.primary_layer_dominance_high)
    }
    return stringResource(R.string.metric_primary_layer_dominance_value, dominanceDb, band)
}

private fun intracellDominanceTapeColor(gapDb: Int?): Color {
    return when (gapDb?.let(::primaryLayerDominance)) {
        null -> Color(0xFF90A4AE)
        PrimaryLayerDominance.LOW -> MeanRsrpRed
        PrimaryLayerDominance.HIGH -> MeanRsrpGreen
    }
}

@Composable
private fun DistanceHistoryBar(
    entries: List<DistanceSegment>,
    nowMs: Long,
    windowStartMs: Long,
    timeTickMs: List<Long> = emptyList(),
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
        TapeSectionHeader(
            title = stringResource(R.string.distance_history_title),
            detail = stringResource(R.string.distance_history_rule),
            value = valueText
        )
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
            drawZoneTimeTicks(timeTickMs, windowStartMs, nowMs)
        }
        TapeTimeScale(windowMs = (nowMs - windowStartMs).coerceAtLeast(0L))
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
    timeTickMs: List<Long> = emptyList(),
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
        TapeSectionHeader(
            title = stringResource(R.string.active_test_history_title),
            detail = stringResource(R.string.active_test_history_rule),
            value = valueText
        )
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
            drawZoneTimeTicks(timeTickMs, windowStartMs, nowMs)
        }
        TapeTimeScale(windowMs = (nowMs - windowStartMs).coerceAtLeast(0L))
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
    timeTickMs: List<Long> = emptyList(),
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
        TapeSectionHeader(
            title = stringResource(R.string.aggregated_bandwidth_history_title),
            detail = stringResource(R.string.aggregated_bandwidth_history_rule),
            value = valueText
        )
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
            drawZoneTimeTicks(timeTickMs, windowStartMs, nowMs)
        }
        TapeTimeScale(windowMs = (nowMs - windowStartMs).coerceAtLeast(0L))
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

private fun DrawScope.drawZoneTimeTicks(
    tickTimesMs: List<Long>,
    windowStartMs: Long,
    nowMs: Long
) {
    if (tickTimesMs.isEmpty()) return
    val spanMs = nowMs - windowStartMs
    if (spanMs <= 0L) return
    val span = spanMs.toFloat()
    for (tickMs in tickTimesMs) {
        val fraction = ((tickMs - windowStartMs).toFloat() / span).coerceIn(0f, 1f)
        val x = fraction * size.width
        drawLine(
            color = Color.White,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 4f
        )
        drawLine(
            color = Color.Black.copy(alpha = 0.85f),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 2f
        )
    }
}

@Composable
private fun ZoneTickLabelRow(
    tickTimesMs: List<Long>,
    windowStartMs: Long,
    nowMs: Long
) {
    if (tickTimesMs.isEmpty()) return
    val spanMs = (nowMs - windowStartMs).coerceAtLeast(1L)
    val labelStepMs = WalkRoute.labelStepMs(spanMs)
    val textMeasurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp)
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
    ) {
        var lastRight = -100f
        for (tickMs in tickTimesMs) {
            val elapsed = tickMs - windowStartMs
            if (elapsed % labelStepMs != 0L) continue
            val fraction = (elapsed.toFloat() / spanMs.toFloat()).coerceIn(0f, 1f)
            val layout = textMeasurer.measure(
                text = WalkRoute.elapsedLabel(elapsed),
                style = style.merge(TextStyle(color = color))
            )
            val x = (fraction * size.width - layout.size.width / 2f)
                .coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
            if (x < lastRight + 4f) continue
            drawText(textLayoutResult = layout, topLeft = Offset(x, 0f))
            lastRight = x + layout.size.width
        }
    }
}

@Composable
private fun TapeTimeScale(windowMs: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.rsrp_histogram_tape_ago, formatTapeWindow(windowMs)),
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
private fun TapeWindowControl(
    windowMs: Long,
    onWindowChange: (Long) -> Unit
) {
    DurationFigureControl(
        label = stringResource(R.string.tape_window_label),
        windowMs = windowMs,
        minMs = MonitoringSettings.MIN_TAPE_WINDOW_MS,
        maxMs = MonitoringSettings.MAX_TAPE_WINDOW_MS,
        hint = stringResource(R.string.tape_window_hint),
        formatWindow = { formatTapeWindow(it) },
        coerce = { it.coerceToTapeWindowStep() },
        onWindowChange = onWindowChange
    )
}

@Composable
internal fun formatTapeWindow(windowMs: Long): String {
    val totalSeconds = (windowMs / 1_000L).toInt().coerceAtLeast(0)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 && minutes == 0 && seconds == 0 ->
            stringResource(R.string.rsrp_histogram_tape_hours, hours)
        hours > 0 && seconds == 0 ->
            stringResource(R.string.tape_window_hours_minutes, hours, minutes)
        hours > 0 ->
            stringResource(R.string.tape_window_hours_minutes_seconds, hours, minutes, seconds)
        minutes > 0 && seconds == 0 ->
            stringResource(R.string.rsrp_histogram_tape_minutes, minutes)
        minutes > 0 ->
            stringResource(R.string.tape_window_minutes_seconds, minutes, seconds)
        else -> stringResource(R.string.rsrp_histogram_tape_seconds, seconds)
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
private fun RsrpHistogramWindowControl(
    windowMs: Long,
    elapsedMs: Long?,
    onWindowChange: (Long) -> Unit
) {
    val elapsedLabel = formatHistogramElapsedLabel(elapsedMs)
    val label = if (elapsedLabel != null) {
        "${stringResource(R.string.rsrp_histogram_window_label)} · $elapsedLabel"
    } else {
        stringResource(R.string.rsrp_histogram_window_label)
    }
    DurationFigureControl(
        label = label,
        windowMs = windowMs,
        minMs = MonitoringSettings.MIN_RSRP_HISTOGRAM_WINDOW_MS,
        maxMs = MonitoringSettings.MAX_RSRP_HISTOGRAM_WINDOW_MS,
        hint = stringResource(R.string.rsrp_histogram_window_hint),
        formatWindow = { formatHistogramWindow(it) },
        coerce = { it.coerceToHistogramWindowStep() },
        onWindowChange = onWindowChange
    )
}

@Composable
private fun DurationFigureControl(
    label: String,
    windowMs: Long,
    minMs: Long,
    maxMs: Long,
    hint: String,
    formatWindow: @Composable (Long) -> String,
    coerce: (Long) -> Long,
    onWindowChange: (Long) -> Unit
) {
    val enabled = settingsControlsEnabled()
    val parts = durationParts(windowMs)
    fun apply(hours: Int, minutes: Int, seconds: Int) {
        onWindowChange(coerce(durationMs(hours, minutes, seconds)))
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatWindow(windowMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        val minSeconds = (minMs / 1_000L).toInt()
        val maxSeconds = (maxMs / 1_000L).toInt()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DurationPartMenu(
                value = parts.hours,
                unit = stringResource(R.string.duration_hours_unit),
                options = hourOptions(minSeconds, maxSeconds),
                enabled = enabled,
                onSelect = { apply(it, parts.minutes, parts.seconds) },
                modifier = Modifier.weight(1f)
            )
            DurationPartMenu(
                value = parts.minutes,
                unit = stringResource(R.string.duration_minutes_unit),
                options = minuteOptions(parts.hours, minSeconds, maxSeconds),
                enabled = enabled,
                onSelect = { apply(parts.hours, it, parts.seconds) },
                modifier = Modifier.weight(1.15f)
            )
            DurationPartMenu(
                value = parts.seconds,
                unit = stringResource(R.string.duration_seconds_unit),
                options = secondOptions(parts.hours, parts.minutes, minSeconds, maxSeconds),
                enabled = enabled,
                onSelect = { apply(parts.hours, parts.minutes, it) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DurationStepButton(
                symbol = "− 1 s",
                enabled = enabled && windowMs > minMs,
                onClick = { onWindowChange(coerce(windowMs - 1_000L)) },
                modifier = Modifier.weight(1f)
            )
            DurationStepButton(
                symbol = "+ 1 s",
                enabled = enabled && windowMs < maxMs,
                onClick = { onWindowChange(coerce(windowMs + 1_000L)) },
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatWindow(minMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatWindow(maxMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DurationPartMenu(
    value: Int,
    unit: String,
    options: List<Int>,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(enabled) {
        if (!enabled) expanded = false
    }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "$value $unit ▾")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = "$option $unit") },
                    onClick = {
                        expanded = false
                        if (option != value) onSelect(option)
                    }
                )
            }
        }
    }
}

private fun hourOptions(minSeconds: Int, maxSeconds: Int): List<Int> {
    val maxHour = maxSeconds / 3_600
    return (0..maxHour).filter { hour ->
        rangesOverlap(hour * 3_600, hour * 3_600 + 3_599, minSeconds, maxSeconds)
    }
}

private fun minuteOptions(hours: Int, minSeconds: Int, maxSeconds: Int): List<Int> {
    return (0..59).filter { minute ->
        val start = hours * 3_600 + minute * 60
        rangesOverlap(start, start + 59, minSeconds, maxSeconds)
    }
}

private fun secondOptions(
    hours: Int,
    minutes: Int,
    minSeconds: Int,
    maxSeconds: Int
): List<Int> {
    val base = hours * 3_600 + minutes * 60
    val start = (minSeconds - base).coerceAtLeast(0)
    val end = (maxSeconds - base).coerceAtMost(59)
    if (end < start) return emptyList()
    return (start..end).toList()
}

private fun rangesOverlap(start: Int, end: Int, min: Int, max: Int): Boolean {
    return end >= min && start <= max
}

@Composable
private fun DurationStepButton(
    symbol: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        modifier = modifier
    ) {
        Text(text = symbol)
    }
}

private data class DurationParts(val hours: Int, val minutes: Int, val seconds: Int)

private fun durationParts(windowMs: Long): DurationParts {
    val totalSeconds = (windowMs / 1_000L).toInt().coerceAtLeast(0)
    return DurationParts(
        hours = totalSeconds / 3_600,
        minutes = (totalSeconds % 3_600) / 60,
        seconds = totalSeconds % 60
    )
}

private fun durationMs(hours: Int, minutes: Int, seconds: Int): Long {
    return (hours.coerceAtLeast(0) * 3_600L +
        minutes.coerceAtLeast(0) * 60L +
        seconds.coerceAtLeast(0)) * 1_000L
}

private fun formatHistogramWindow(windowMs: Long): String {
    val totalSeconds = (windowMs / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L && minutes == 0L && seconds == 0L -> "${hours} h"
        hours > 0L && seconds == 0L -> "${hours} h ${minutes} m"
        hours > 0L -> "${hours} h ${minutes} m ${seconds} s"
        minutes > 0L && seconds == 0L -> "${minutes} m"
        minutes > 0L -> "${minutes} m ${seconds} s"
        else -> "${seconds} s"
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
