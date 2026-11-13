package io.github.cloolalang.notspotdetector.ui

import android.graphics.Bitmap
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.github.cloolalang.notspotdetector.MonitorState
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

    override fun onCleared() {
        MonitorState.setZoneTestSampleRetainFrom(null)
    }
}
