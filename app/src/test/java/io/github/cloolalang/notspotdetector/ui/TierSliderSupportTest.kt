package io.github.cloolalang.notspotdetector.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class TierSliderSupportTest {

    @Test
    fun tierClickSliderStepSize_keepsSliderStepsWithinLimit() {
        val minUiMs = 280
        val uiStepSize = TierSliderSupport.tierClickSliderStepSizeMs(minUiMs)
        val minStep = TierSliderSupport.tierClickSliderIndex(minUiMs, minUiMs, uiStepSize)
        val maxStep = 20_000 / uiStepSize

        assertTrue(minStep < maxStep)
        assertTrue(
            TierSliderSupport.tierClickIndexSteps(minStep, maxStep) <=
                TierSliderSupport.MAX_TIER_CLICK_SLIDER_STEPS
        )
    }
}
