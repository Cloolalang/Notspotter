package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LteLayerResilienceTest {

    @Test
    fun pciOnlyNeighboursCountOnThePrimaryLayer() {
        val reading = LteLayerResilience.fromDetectedCells(
            cells = listOf(
                DetectedLteCell(earfcn = 1_800, pci = 42, rsrpDbm = -80, isRegistered = true),
                DetectedLteCell(earfcn = null, pci = 87, rsrpDbm = -92),
                DetectedLteCell(earfcn = null, pci = 101, rsrpDbm = -98),
                DetectedLteCell(earfcn = null, pci = 210, rsrpDbm = -105)
            ),
            primaryEarfcn = 1_800,
            primaryPci = 42
        )

        assertEquals(4, reading.primaryLayerCellCount)
        assertEquals(0, reading.alternateLayerCount)
        assertEquals(12, reading.primaryLayerDominanceDb)
    }

    @Test
    fun interFrequencyLayerStaysAlternate() {
        val reading = LteLayerResilience.fromDetectedCells(
            cells = listOf(
                DetectedLteCell(earfcn = 1_800, pci = 42, rsrpDbm = -80, isRegistered = true),
                DetectedLteCell(earfcn = null, pci = 87, rsrpDbm = -90),
                DetectedLteCell(earfcn = 3_500, pci = 12, rsrpDbm = -110),
                DetectedLteCell(earfcn = 3_500, pci = 19, rsrpDbm = -112)
            ),
            primaryEarfcn = 1_800
        )

        assertEquals(2, reading.primaryLayerCellCount)
        assertEquals(1, reading.alternateLayerCount)
        assertEquals(2, reading.alternateLayerCellCount)
        assertEquals(10, reading.primaryLayerDominanceDb)
    }

    @Test
    fun duplicatePciOnTheSameLayerCountsOnce() {
        val reading = LteLayerResilience.fromDetectedCells(
            cells = listOf(
                DetectedLteCell(earfcn = 1_800, pci = 42, rsrpDbm = -80, isRegistered = true),
                DetectedLteCell(earfcn = 1_800, pci = 42, rsrpDbm = -81),
                DetectedLteCell(earfcn = null, pci = 42, rsrpDbm = -82)
            ),
            primaryEarfcn = 1_800,
            primaryPci = 42
        )

        assertEquals(1, reading.primaryLayerCellCount)
        assertNull(reading.primaryLayerDominanceDb)
    }

    @Test
    fun missingPrimaryEarfcnRecoversFromRegisteredCell() {
        val reading = LteLayerResilience.fromDetectedCells(
            cells = listOf(
                DetectedLteCell(earfcn = 1_800, pci = 42, rsrpDbm = -80, isRegistered = true),
                DetectedLteCell(earfcn = null, pci = 87, rsrpDbm = -91)
            ),
            primaryEarfcn = null,
            primaryPci = 42
        )

        assertEquals(2, reading.primaryLayerCellCount)
        assertEquals(11, reading.primaryLayerDominanceDb)
    }

    @Test
    fun campedIdentityWithNoMatchingCellStillCountsServingSector() {
        val reading = LteLayerResilience.fromDetectedCells(
            cells = emptyList(),
            primaryEarfcn = 1_800,
            primaryPci = 42
        )

        assertEquals(1, reading.primaryLayerCellCount)
        assertEquals(0, reading.alternateLayerCount)
        assertNull(reading.primaryLayerDominanceDb)
    }

    @Test
    fun unknownPlmnNeighboursAreCounted() {
        assertTrue(
            LteLayerResilience.shouldCountNeighbour(
                isPlmnMismatch = false,
                isRegistered = false,
                acceptRegisteredPlmnMismatch = false
            )
        )
    }

    @Test
    fun unregisteredOtherPlmnIsExcluded() {
        assertFalse(
            LteLayerResilience.shouldCountNeighbour(
                isPlmnMismatch = true,
                isRegistered = false,
                acceptRegisteredPlmnMismatch = false
            )
        )
    }

    @Test
    fun registeredMismatchIsCountedOnlyWhenAccepted() {
        assertFalse(
            LteLayerResilience.shouldCountNeighbour(
                isPlmnMismatch = true,
                isRegistered = true,
                acceptRegisteredPlmnMismatch = false
            )
        )
        assertTrue(
            LteLayerResilience.shouldCountNeighbour(
                isPlmnMismatch = true,
                isRegistered = true,
                acceptRegisteredPlmnMismatch = true
            )
        )
    }

    @Test
    fun noLteCellsAndNoServingIdentityIsZero() {
        val reading = LteLayerResilience.fromDetectedCells(
            cells = emptyList(),
            primaryEarfcn = null,
            primaryPci = null
        )

        assertEquals(0, reading.primaryLayerCellCount)
        assertNull(reading.primaryLayerDominanceDb)
    }
}
