package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CellReselectRateTest {

    @Test
    fun emptyWindowIsZero() {
        assertEquals(0, CellReselectRate.countPerMinute(emptyList(), nowMs = 60_000L))
    }

    @Test
    fun recordsReselectInsideWindow() {
        val timestamps = CellReselectRate.record(
            timestampsMs = emptyList(),
            nowMs = 10_000L,
            reselectOccurred = true
        )

        assertEquals(listOf(10_000L), timestamps)
        assertEquals(1, CellReselectRate.countPerMinute(timestamps, nowMs = 10_000L))
    }

    @Test
    fun countsThreeReselectsInTheSameMinute() {
        var timestamps = emptyList<Long>()
        timestamps = CellReselectRate.record(timestamps, nowMs = 1_000L, reselectOccurred = true)
        timestamps = CellReselectRate.record(timestamps, nowMs = 20_000L, reselectOccurred = true)
        timestamps = CellReselectRate.record(timestamps, nowMs = 40_000L, reselectOccurred = true)

        assertEquals(3, CellReselectRate.countPerMinute(timestamps, nowMs = 40_000L))
    }

    @Test
    fun dropsEventsOlderThanSixtySeconds() {
        val timestamps = listOf(1_000L, 40_000L)

        val pruned = CellReselectRate.record(
            timestampsMs = timestamps,
            nowMs = 61_001L,
            reselectOccurred = false
        )

        assertEquals(listOf(40_000L), pruned)
        assertEquals(1, CellReselectRate.countPerMinute(pruned, nowMs = 61_001L))
    }

    @Test
    fun keepsEventJustInsideTheWindow() {
        val timestamps = listOf(1L)

        assertEquals(1, CellReselectRate.countPerMinute(timestamps, nowMs = 60_000L))
    }

    @Test
    fun servingCellPciChangeIsAReselect() {
        val previous = CellIdentitySnapshot(lteEarfcn = 1_800, ltePci = 42)
        val next = CellIdentitySnapshot(lteEarfcn = 1_800, ltePci = 99)

        assertTrue(next.isServingCellReselectFrom(previous))
    }

    @Test
    fun firstCampIsNotAReselect() {
        val previous = CellIdentitySnapshot()
        val next = CellIdentitySnapshot(lteEarfcn = 1_800, ltePci = 42)

        assertFalse(next.isServingCellReselectFrom(previous))
    }

    @Test
    fun nrBandOnlyChangeIsNotAReselect() {
        val previous = CellIdentitySnapshot(nrEarfcn = 627_264, nrPci = 12, nrBand = 78)
        val next = CellIdentitySnapshot(nrEarfcn = 627_264, nrPci = 12, nrBand = 77)

        assertFalse(next.isServingCellReselectFrom(previous))
    }

    @Test
    fun gsmChannelChangeIsAReselect() {
        val previous = CellIdentitySnapshot(gsmEarfcn = 62, gsmBsic = 16)
        val next = CellIdentitySnapshot(gsmEarfcn = 70, gsmBsic = 16)

        assertTrue(next.isServingCellReselectFrom(previous))
    }
}
