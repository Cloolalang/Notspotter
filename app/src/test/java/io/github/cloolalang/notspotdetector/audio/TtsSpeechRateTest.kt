package io.github.cloolalang.notspotdetector.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsSpeechRateTest {

    @Test
    fun candidates_forFiveX_tryEngineFallbacks() {
        assertEquals(
            listOf(5.0f, 3.0f, 2.0f, 1.5f, 1.2f),
            TtsSpeechRate.candidates(5.0f)
        )
    }

    @Test
    fun candidates_forNormalRate_doNotOfferFasterFallbacks() {
        assertEquals(listOf(1.2f), TtsSpeechRate.candidates(1.2f))
    }

    @Test
    fun audioFocusHold_isLongerAtHighRate() {
        val slow = TtsSpeechRate.audioFocusHoldMs(1.2f)
        val fast = TtsSpeechRate.audioFocusHoldMs(5.0f)
        assertTrue(fast > slow)
        assertFalse(fast < 200L)
    }
}
