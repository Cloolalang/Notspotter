package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallTestTest {

    @Test
    fun masterTimeout_defaultsOnForThirtyMinutes() {
        assertTrue(CallTestSettings().masterTimeoutEnabled)
        assertFalse(CallTestSettings().muteCallAudio)
        assertEquals(30, CallTestSettings.MASTER_TIMEOUT_MINUTES)
        assertEquals(30L * 60L * 1000L, CallTestSettings.MASTER_TIMEOUT_MS)
    }

    @Test
    fun ratePercent_isZeroUntilThereIsAnAttempt() {
        assertEquals(0, CallTestScore.ratePercent(0, 0))
        assertEquals(0, CallTestScore.ratePercent(1, 0))
    }

    @Test
    fun ratePercent_usesWholeAttempts() {
        assertEquals(75, CallTestScore.ratePercent(3, 4))
        assertEquals(100, CallTestScore.ratePercent(2, 2))
        assertEquals(0, CallTestScore.ratePercent(0, 5))
    }

    @Test
    fun dialableNumber_keepsALeadingPlusAndDropsFormatting() {
        assertEquals("+353871234567", CallTestScore.dialableNumber(" +353 87-123 (4567) "))
    }

    @Test
    fun dialableNumber_rejectsLettersAndShortNumbers() {
        assertNull(CallTestScore.dialableNumber("abc"))
        assertNull(CallTestScore.dialableNumber("12"))
        assertNull(CallTestScore.dialableNumber(""))
        assertNull(CallTestScore.dialableNumber("+44 office"))
    }
}
