package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LteLayerResilienceReadingTest {

    private fun reading(dominanceDb: Int?) = LteLayerResilienceReading(
        primaryLayerCellCount = 2,
        alternateLayerCount = 2,
        alternateLayerCellCount = 3,
        primaryLayerDominanceDb = dominanceDb
    )

    @Test
    fun gapBelowThresholdIsLowDominance() {
        assertEquals(PrimaryLayerDominance.LOW, reading(0).primaryLayerDominance())
        assertEquals(PrimaryLayerDominance.LOW, reading(5).primaryLayerDominance())
    }

    @Test
    fun gapAtOrAboveThresholdIsHighDominance() {
        assertEquals(PrimaryLayerDominance.HIGH, reading(PRIMARY_LAYER_LOW_DOMINANCE_THRESHOLD_DB).primaryLayerDominance())
        assertEquals(PrimaryLayerDominance.HIGH, reading(20).primaryLayerDominance())
    }

    @Test
    fun negativeGapIsLowDominance() {
        // A negative gap means the "next highest" intra-channel sector is actually stronger than
        // the one flagged as serving (e.g. an imminent reselection) — still "low" dominance.
        assertEquals(PrimaryLayerDominance.LOW, reading(-3).primaryLayerDominance())
    }

    @Test
    fun nullDominanceDbIsUndefined() {
        assertNull(reading(null).primaryLayerDominance())
    }
}
