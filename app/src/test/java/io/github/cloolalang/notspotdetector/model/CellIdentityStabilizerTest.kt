package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CellIdentityStabilizerTest {

    @Test
    fun coalesceWith_fillsMissingFieldsFromPrevious() {
        val current = CellIdentitySnapshot(lteEarfcn = 1_800, ltePci = null)
        val previous = CellIdentitySnapshot(lteEarfcn = 1_800, ltePci = 42)

        val merged = current.coalesceWith(previous)

        assertEquals(1_800, merged.lteEarfcn)
        assertEquals(42, merged.ltePci)
    }

    @Test
    fun coalesceWith_prefersCurrentNonNullValues() {
        val current = CellIdentitySnapshot(lteEarfcn = 1_900, ltePci = 77)
        val previous = CellIdentitySnapshot(lteEarfcn = 1_800, ltePci = 42)

        val merged = current.coalesceWith(previous)

        assertEquals(1_900, merged.lteEarfcn)
        assertEquals(77, merged.ltePci)
    }

    @Test
    fun withStabilizedCellIdentity_clearsOnCompleteNoService() {
        val stats = ConnectivityStats(
            isCompleteNoService = true,
            lteEarfcn = 1_800,
            ltePci = 42
        )
        val previous = CellIdentitySnapshot(lteEarfcn = 1_800, ltePci = 42)

        val (display, cache) = stats.withStabilizedCellIdentity(previous)

        assertNull(display.lteEarfcn)
        assertNull(display.ltePci)
        assertEquals(CellIdentitySnapshot(), cache)
    }

    @Test
    fun withStabilizedCellIdentity_clearsMetricsWhenNoSignalActive() {
        val stats = ConnectivityStats(
            isMonitoring = true,
            noSignalActive = true,
            rsrpDbm = -130,
            rsrqDb = -20,
            lteEarfcn = 1_800,
            ltePci = 42
        )
        val previous = CellIdentitySnapshot(lteEarfcn = 1_800, ltePci = 42)

        val (display, cache) = stats.withStabilizedCellIdentity(previous)

        assertNull(display.rsrpDbm)
        assertNull(display.rsrqDb)
        assertNull(display.lteEarfcn)
        assertNull(display.ltePci)
        assertEquals(CellIdentitySnapshot(), cache)
    }

    @Test
    fun withStabilizedCellIdentity_keepsIdentityWhenRsrpIsWeakButNotNoSignal() {
        val stats = ConnectivityStats(
            cellularAvailable = false,
            rsrpDbm = -130,
            lteEarfcn = 1_800,
            ltePci = null
        )
        val previous = CellIdentitySnapshot(lteEarfcn = 1_800, ltePci = 42)

        val (display, cache) = stats.withStabilizedCellIdentity(previous)

        assertEquals(1_800, display.lteEarfcn)
        assertEquals(42, display.ltePci)
        assertEquals(42, cache.ltePci)
    }
}
