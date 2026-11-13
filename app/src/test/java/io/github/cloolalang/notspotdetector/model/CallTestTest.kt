package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallTestTest {

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
