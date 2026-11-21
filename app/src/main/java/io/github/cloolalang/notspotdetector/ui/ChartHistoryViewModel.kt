package io.github.cloolalang.notspotdetector.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.github.cloolalang.notspotdetector.MonitorState
import io.github.cloolalang.notspotdetector.data.PedestrianDebugLogger
import io.github.cloolalang.notspotdetector.model.PedestrianDebugSnapshot
import io.github.cloolalang.notspotdetector.model.PedestrianLive
import io.github.cloolalang.notspotdetector.model.PedestrianPosition
import io.github.cloolalang.notspotdetector.model.PedestrianSample
import io.github.cloolalang.notspotdetector.model.PedestrianTrack
import io.github.cloolalang.notspotdetector.sensor.PedestrianTracker
import io.github.cloolalang.notspotdetector.model.ActiveTestTape
import io.github.cloolalang.notspotdetector.model.CallResultTape
import io.github.cloolalang.notspotdetector.model.AggregatedBandwidthTape
import io.github.cloolalang.notspotdetector.model.DistanceTape
import io.github.cloolalang.notspotdetector.model.IntracellDominanceTape
import io.github.cloolalang.notspotdetector.model.PrimaryCarrierTape
import io.github.cloolalang.notspotdetector.model.RsrpBinTape
import io.github.cloolalang.notspotdetector.model.RsrqBinTape
import io.github.cloolalang.notspotdetector.model.TechnologyTape
import io.github.cloolalang.notspotdetector.model.WalkPin

/**
 * KPI tapes for the main screen. The activity keeps this across rotation.
 * Clear history is the only reset.
 */
class KpiHistoryViewModel : ViewModel() {
    val carrierTape = mutableStateOf(PrimaryCarrierTape())
    val technologyTape = mutableStateOf(TechnologyTape())
    val rsrpBinTape = mutableStateOf(RsrpBinTape())
    val rsrqBinTape = mutableStateOf(RsrqBinTape())
    val distanceTape = mutableStateOf(DistanceTape())
    val intracellDominanceTape = mutableStateOf(IntracellDominanceTape())
    val activeTestTape = mutableStateOf(ActiveTestTape())
    val callResultTape = mutableStateOf(CallResultTape())
    val aggregatedBandwidthTape = mutableStateOf(AggregatedBandwidthTape())
    val tapePauseOffsetMs = mutableLongStateOf(0L)
    val tapeHeldWallMs = mutableStateOf<Long?>(null)
    val tapeOperator = mutableStateOf<String?>(null)
}

/**
 * Zone-test run, tapes, and walk marks. The activity keeps this across rotation.
 * Starting a new run replaces it. Leaving the screen drops the extra sample retention.
 */
class ZoneTestViewModel : ViewModel() {
    val running = mutableStateOf(false)
    /** When true, the level histogram and the zone RSRP/RSRQ tapes use the whole run. */
    val wholeTest = mutableStateOf(false)
    val startMs = mutableStateOf<Long?>(null)
    val endMs = mutableStateOf<Long?>(null)
    val sampleWindowStartMs = mutableStateOf<Long?>(null)
    val zoneName = mutableStateOf("")
    val operatorLabel = mutableStateOf<String?>(null)
    val statusMessage = mutableStateOf<String?>(null)
    val carrierTape = mutableStateOf(PrimaryCarrierTape())
    val technologyTape = mutableStateOf(TechnologyTape())
    val rsrpBinTape = mutableStateOf(RsrpBinTape())
    val rsrqBinTape = mutableStateOf(RsrqBinTape())
    val distanceTape = mutableStateOf(DistanceTape())
    val intracellDominanceTape = mutableStateOf(IntracellDominanceTape())
    val activeTestTape = mutableStateOf(ActiveTestTape())
    val callResultTape = mutableStateOf(CallResultTape())
    val aggregatedBandwidthTape = mutableStateOf(AggregatedBandwidthTape())
    val floorPlan = mutableStateOf<Bitmap?>(null)
    val pins = mutableStateOf<List<WalkPin>>(emptyList())
    val pedestrianSamples = mutableStateOf<List<PedestrianSample>>(emptyList())
    val pedestrianTracking = mutableStateOf(false)
    val pedestrianLive = mutableStateOf(PedestrianLive())
    val pedestrianCanUndo = mutableStateOf(false)

    private var pedestrianTracker: PedestrianTracker? = null
    private var pedestrianTapSteps = false
    private var pedestrianHeadingSnap = false
    private var pedestrianHeadingSnapDegrees = PedestrianTrack.DEFAULT_HEADING_SNAP_DEGREES

    /** Set by the zone-test panel. The main-screen button calls these. */
    var onStart: (() -> Unit)? = null
    var onStop: (() -> Unit)? = null

    fun startPedestrianAiming(context: Context) {
        if (pedestrianTracking.value) return
        val tracker = tracker(context)
        attachLive(tracker)
        tracker.headingSnap = pedestrianHeadingSnap
        tracker.headingSnapDegrees = pedestrianHeadingSnapDegrees
        tracker.startAiming()
    }

    fun startPedestrian(context: Context, strideMeters: Float, elapsedOffsetMs: Long = 0L) {
        val tracker = tracker(context)
        attachLive(tracker)
        tracker.tapSteps = pedestrianTapSteps
        tracker.headingSnap = pedestrianHeadingSnap
        tracker.headingSnapDegrees = pedestrianHeadingSnapDegrees
        tracker.strideMeters = PedestrianTrack.coerceStride(strideMeters)
        pedestrianSamples.value = emptyList()
        pedestrianTracking.value = true
        val offset = elapsedOffsetMs.coerceAtLeast(0L)
        tracker.start { sample ->
            storeSample(offset + sample.elapsedMs, PedestrianPosition(sample.eastMeters, sample.northMeters))
        }
    }

    fun resumePedestrian(context: Context, strideMeters: Float, elapsedNowMs: Long) {
        if (pedestrianSamples.value.isEmpty()) {
            startPedestrian(context, strideMeters, elapsedNowMs)
            return
        }
        val tracker = tracker(context)
        attachLive(tracker)
        tracker.tapSteps = pedestrianTapSteps
        tracker.headingSnap = pedestrianHeadingSnap
        tracker.headingSnapDegrees = pedestrianHeadingSnapDegrees
        tracker.strideMeters = PedestrianTrack.coerceStride(strideMeters)
        val last = pedestrianSamples.value.last()
        pedestrianTracking.value = true
        val offset = elapsedNowMs.coerceAtLeast(0L)
        tracker.resume(PedestrianPosition(last.eastMeters, last.northMeters)) { sample ->
            storeSample(offset + sample.elapsedMs, PedestrianPosition(sample.eastMeters, sample.northMeters))
        }
    }

    private fun storeSample(elapsedMs: Long, position: PedestrianPosition) {
        val before = pedestrianSamples.value.size
        val next = PedestrianTrack.appendSample(pedestrianSamples.value, elapsedMs, position)
        if (next.size == before) {
            PedestrianDebugLogger.record(
                PedestrianDebugSnapshot(
                    phase = "trail",
                    detail = "kept the previous mark, elapsed=${elapsedMs}ms is under 1s after the last mark",
                    suspect = false
                )
            )
        }
        pedestrianSamples.value = next
    }

    fun setPedestrianStride(strideMeters: Float) {
        pedestrianTracker?.strideMeters = PedestrianTrack.coerceStride(strideMeters)
    }

    fun setPedestrianTapSteps(enabled: Boolean) {
        pedestrianTapSteps = enabled
        pedestrianTracker?.tapSteps = enabled
    }

    fun setPedestrianHeadingSnap(enabled: Boolean, degrees: Float) {
        val coerced = PedestrianTrack.coerceHeadingSnapDegrees(degrees)
        val changedMode = pedestrianHeadingSnap != enabled
        pedestrianHeadingSnap = enabled
        pedestrianHeadingSnapDegrees = coerced
        val tracker = pedestrianTracker ?: return
        tracker.headingSnap = enabled
        tracker.headingSnapDegrees = coerced
        if (changedMode) tracker.realignHeadingSnap()
    }

    fun tapPedestrianStep() {
        pedestrianTracker?.manualStep()
    }

    fun undoPedestrianStep() {
        val retraction = pedestrianTracker?.undoLastStep() ?: return
        pedestrianSamples.value = PedestrianTrack.retractLastPosition(
            pedestrianSamples.value,
            retraction.from,
            retraction.to
        )
    }

    /** Removes the plotted walk. Monitoring and the compass keep going. */
    fun clearPedestrianPlot() {
        pedestrianTracker?.clearPlot()
        pedestrianSamples.value = emptyList()
        if (pedestrianTracker == null) {
            pedestrianLive.value = PedestrianLive()
            pedestrianCanUndo.value = false
        }
    }

    fun stopPedestrian() {
        pedestrianTracker?.stop()
        pedestrianTracking.value = false
    }

    fun clearPedestrianSamples() {
        pedestrianSamples.value = emptyList()
        pedestrianLive.value = PedestrianLive()
        pedestrianCanUndo.value = false
    }

    /** A linked phone has the master's path and no local compass or step count. */
    fun showRemoteWalk(samples: List<PedestrianSample>) {
        pedestrianSamples.value = samples
        pedestrianLive.value = if (samples.isEmpty()) {
            PedestrianLive()
        } else {
            PedestrianLive(distanceMeters = PedestrianTrack.walkedMeters(samples))
        }
        pedestrianCanUndo.value = false
    }

    private fun attachLive(tracker: PedestrianTracker) {
        tracker.onLive = { live ->
            if (pedestrianLive.value != live) pedestrianLive.value = live
            if (pedestrianCanUndo.value != live.canUndo) pedestrianCanUndo.value = live.canUndo
        }
    }

    override fun onCleared() {
        pedestrianTracker?.stop()
        MonitorState.setZoneTestSampleRetainFrom(null)
    }

    private fun tracker(context: Context): PedestrianTracker {
        return pedestrianTracker ?: PedestrianTracker(context.applicationContext).also {
            pedestrianTracker = it
        }
    }
}
