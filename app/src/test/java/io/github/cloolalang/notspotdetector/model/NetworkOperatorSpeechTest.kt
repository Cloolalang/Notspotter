package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkOperatorSpeechTest {

    @Test
    fun formatForSpeech_spellsShortUppercaseAcronyms() {
        assertEquals("E E", NetworkOperatorSpeech.formatForSpeech("EE"))
        assertEquals("O 2", NetworkOperatorSpeech.formatForSpeech("O2"))
    }

    @Test
    fun formatForSpeech_keepsLongOperatorNames() {
        assertEquals("Vodafone UK", NetworkOperatorSpeech.formatForSpeech("Vodafone UK"))
    }

    @Test
    fun formatForSpeech_returnsNullForBlank() {
        assertNull(NetworkOperatorSpeech.formatForSpeech("  "))
    }
}
