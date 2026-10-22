package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveTestHistoryTest {

    @Test
    fun current_colorsPingTimeAndPacketLoss() {
        assertEquals(ActiveTestBand.FAST, visit(rttMs = 1).band)
        assertEquals(ActiveTestBand.FAST, visit(rttMs = 50).band)
        assertEquals(ActiveTestBand.OK, visit(rttMs = 51).band)
        assertEquals(ActiveTestBand.OK, visit(rttMs = 75).band)
        assertEquals(ActiveTestBand.SLOW, visit(rttMs = 76).band)
        assertEquals(ActiveTestBand.SLOW, visit(rttMs = 150).band)
        assertEquals(ActiveTestBand.VERY_SLOW, visit(rttMs = 151).band)
        assertEquals(ActiveTestBand.FAST, visit(rttMs = 40, packetLossPercent = 5f).band)
        assertEquals(ActiveTestBand.HIGH_LOSS, visit(rttMs = 40, packetLossPercent = 6f).band)
        assertEquals(ActiveTestBand.TOTAL_LOSS, visit(rttMs = null, packetLossPercent = 100f).band)
    }

    @Test
    fun current_noMeasurementWhenThereIsNoPingOrNoSignal() {
        assertNull(ActiveTestHistory.current(ConnectivityStats()))
        assertEquals(
            ActiveTestBand.NO_MEASUREMENT,
            ActiveTestHistory.current(
                ConnectivityStats(isMonitoring = true, isPassiveOnlySession = true, rttMs = 20, pingsSent = 2)
            )?.band
        )
        assertEquals(
            ActiveTestBand.NO_MEASUREMENT,
            ActiveTestHistory.current(ConnectivityStats(isMonitoring = true))?.band
        )
        assertEquals(
            ActiveTestBand.NO_MEASUREMENT,
            ActiveTestHistory.current(
                ConnectivityStats(isMonitoring = true, noSignalActive = true, rttMs = 20, pingsSent = 2)
            )?.band
        )
    }

    @Test
    fun label_showsPingTimeLossAndNoMeasurement() {
        assertEquals("42 ms", ActiveTestHistory.label(visit(rttMs = 42)))
        assertEquals("40 ms", ActiveTestHistory.label(visit(rttMs = 40, packetLossPercent = 8f)))
        assertEquals("100%", ActiveTestHistory.label(visit(rttMs = null, packetLossPercent = 100f)))
        assertEquals(
            "—",
            ActiveTestHistory.label(ActiveTestVisit(band = ActiveTestBand.NO_MEASUREMENT))
        )
    }

    @Test
    fun record_holdsAColorWhileThePingTimeUpdates() {
        val first = ActiveTestHistory.record(ActiveTestTape(), visit(rttMs = 40), nowMs = 1_000)
        val sameColor = ActiveTestHistory.record(first, visit(rttMs = 48), nowMs = 2_000)
        val slower = ActiveTestHistory.record(sameColor, visit(rttMs = 90), nowMs = 3_000)

        assertEquals(1, sameColor.entries.size)
        assertEquals(48L, sameColor.entries.single().rttMs)
        assertEquals(1_000L, sameColor.entries.single().startedAtMs)
        assertEquals(listOf(ActiveTestBand.FAST, ActiveTestBand.SLOW), slower.entries.map { it.band })
        assertEquals("48 ms", ActiveTestHistory.label(sameColor.entries.single()))
    }

    private fun visit(rttMs: Long?, packetLossPercent: Float = 0f): ActiveTestVisit {
        return ActiveTestHistory.current(
            ConnectivityStats(
                isMonitoring = true,
                rttMs = rttMs,
                packetLossPercent = packetLossPercent,
                pingsSent = 2
            )
        )!!
    }
}
