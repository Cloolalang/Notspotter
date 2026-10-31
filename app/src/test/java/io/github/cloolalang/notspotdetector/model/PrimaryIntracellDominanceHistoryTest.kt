package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrimaryIntracellDominanceHistoryTest {

    @Test
    fun current_matchesTheCellMetricsGap() {
        val stats = monitoring(gapDb = 12)

        assertEquals(12, stats.primaryIntracellDominanceDb())
        assertEquals(IntracellDominanceVisit(12), PrimaryIntracellDominanceHistory.current(stats))
        assertEquals(PrimaryLayerDominance.HIGH, primaryLayerDominance(12))
        assertEquals(PrimaryLayerDominance.LOW, primaryLayerDominance(5))
    }

    @Test
    fun current_nullGapWhenTheMetricIsBlank() {
        val noComparison = ConnectivityStats(
            isMonitoring = true,
            lteLayerResilience = reading(gapDb = null)
        )
        assertEquals(IntracellDominanceVisit(null), PrimaryIntracellDominanceHistory.current(noComparison))
        assertEquals(
            IntracellDominanceVisit(null),
            PrimaryIntracellDominanceHistory.current(ConnectivityStats(isMonitoring = true))
        )
        assertEquals(
            IntracellDominanceVisit(null),
            PrimaryIntracellDominanceHistory.current(monitoring(gapDb = 12).copy(noSignalActive = true))
        )
        assertEquals(
            IntracellDominanceVisit(null),
            PrimaryIntracellDominanceHistory.current(monitoring(gapDb = 12).copy(isWifiCallingActive = true))
        )
        assertEquals(
            IntracellDominanceVisit(null),
            PrimaryIntracellDominanceHistory.current(
                monitoring(gapDb = 12).copy(radioAccessType = CellularSignalReader.RADIO_2G)
            )
        )
    }

    @Test
    fun current_holdsWhenMonitoringIsStopped() {
        assertNull(PrimaryIntracellDominanceHistory.current(monitoring(gapDb = 12).copy(isMonitoring = false)))
    }

    @Test
    fun record_keepsASegmentUntilTheGapChanges() {
        var tape = IntracellDominanceTape()
        tape = PrimaryIntracellDominanceHistory.record(tape, null)
        tape = PrimaryIntracellDominanceHistory.record(tape, IntracellDominanceVisit(4), nowMs = 1_000L)
        tape = PrimaryIntracellDominanceHistory.record(tape, IntracellDominanceVisit(4), nowMs = 2_000L)
        tape = PrimaryIntracellDominanceHistory.record(tape, IntracellDominanceVisit(null), nowMs = 3_000L)
        tape = PrimaryIntracellDominanceHistory.record(tape, IntracellDominanceVisit(18), nowMs = 4_000L)

        assertEquals(listOf(4, null, 18), tape.entries.map { it.gapDb })
        assertEquals(1_000L, tape.entries.first().startedAtMs)
    }

    private fun monitoring(gapDb: Int): ConnectivityStats {
        return ConnectivityStats(
            isMonitoring = true,
            lteLayerResilience = reading(gapDb)
        )
    }

    private fun reading(gapDb: Int?): LteLayerResilienceReading {
        return LteLayerResilienceReading(
            primaryLayerCellCount = 2,
            alternateLayerCount = 0,
            alternateLayerCellCount = 0,
            primaryLayerDominanceDb = gapDb
        )
    }
}
