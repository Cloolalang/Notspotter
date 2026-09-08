package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechDigitsTest {

    @Test
    fun format_separatesDigitsWithSpaces() {
        assertEquals("6 4 0 0", SpeechDigits.format(6400))
        assertEquals("1 2 3", SpeechDigits.format(123))
        assertEquals("4 2", SpeechDigits.format(42))
    }
}
