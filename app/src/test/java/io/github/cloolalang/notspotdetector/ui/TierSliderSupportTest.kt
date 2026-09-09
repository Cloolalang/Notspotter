package io.github.cloolalang.notspotdetector.ui

import io.github.cloolalang.notspotdetector.model.SettingsCompatibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TierSliderSupportTest {

    @Test
    fun tierClickSliderStepSize_keepsSliderStepsWithinLimit() {
        val minUiMs = 280
        val uiStepSize = TierSliderSupport.tierClickSliderStepSizeMs(minUiMs)
        val maxStep = TierSliderSupport.tierClickSliderMaxIndex(minUiMs, uiStepSize)

        assertTrue(maxStep > 0)
        assertTrue(
            TierSliderSupport.tierClickIndexSteps(0, maxStep) <=
                TierSliderSupport.MAX_TIER_CLICK_SLIDER_STEPS
        )
    }

    @Test
    fun tierClickSliderIndex_zeroMapsToMinUiMs() {
        val minUiMs = SettingsCompatibility.minTierClickIntervalUiMs(signalPulseDurationMs = 65)
        val uiStepSize = TierSliderSupport.tierClickSliderStepSizeMs(minUiMs)

        assertEquals(
            minUiMs,
            TierSliderSupport.tierClickSliderMsFromIndex(0, minUiMs, uiStepSize)
        )
    }

    @Test
    fun tierClickSliderIndexFromMs_roundTripsMinimum() {
        val minUiMs = SettingsCompatibility.minTierClickIntervalUiMs(signalPulseDurationMs = 10)
        val uiStepSize = TierSliderSupport.tierClickSliderStepSizeMs(minUiMs)

        assertEquals(
            0,
            TierSliderSupport.tierClickSliderIndexFromMs(minUiMs, minUiMs, uiStepSize)
        )
        assertEquals(
            minUiMs,
            TierSliderSupport.tierClickSliderMsFromIndex(0, minUiMs, uiStepSize)
        )
    }
}
