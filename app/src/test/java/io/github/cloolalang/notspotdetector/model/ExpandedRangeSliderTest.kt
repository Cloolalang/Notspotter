package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpandedRangeSliderTest {

    @Test
    fun midpointOfLowerTrack_mapsNearSplitNotMidRange() {
        val min = 10
        val split = 200
        val max = 5_000
        val atLowerEnd = ExpandedRangeSlider.valueFromPosition(0.55f, min, split, max)

        assertEquals(split, atLowerEnd)
        val linearMid = (min + max) / 2
        assertTrue(atLowerEnd < linearMid)
    }

    @Test
    fun positionAndValue_areInversesOnPulseScale() {
        val min = 10
        val split = 200
        val max = 5_000
        for (ms in listOf(10, 50, 100, 200, 1_000, 5_000)) {
            val position = ExpandedRangeSlider.positionFromValue(ms, min, split, max)
            val restored = ExpandedRangeSlider.valueFromPosition(position, min, split, max)
            assertEquals(ms, restored)
        }
    }

    @Test
    fun snapToStep_usesTenMsSteps() {
        assertEquals(50, ExpandedRangeSlider.snapToStep(54, 10, 5_000, 10))
        assertEquals(10, ExpandedRangeSlider.snapToStep(10, 10, 5_000, 10))
    }
}
