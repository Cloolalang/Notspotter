package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MobileDataEnabledTest {

    @Test
    fun format_on() {
        assertEquals("Data on", formatMobileDataEnabledSuffix(true))
    }

    @Test
    fun format_off() {
        assertEquals("Data off", formatMobileDataEnabledSuffix(false))
    }

    @Test
    fun format_unknown() {
        assertNull(formatMobileDataEnabledSuffix(null))
    }
}
