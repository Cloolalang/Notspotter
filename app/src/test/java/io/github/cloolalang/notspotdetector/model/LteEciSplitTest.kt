package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LteEciSplitTest {

    @Test
    fun fromEci_dividesBy256() {
        val split = LteEciSplit.fromEci(1_234_567)

        assertEquals(4822, split?.enbId)
        assertEquals(135, split?.cellId)
    }

    @Test
    fun fromEci_remainderIsLogicalCellId() {
        assertEquals(0, LteEciSplit.fromEci(0)?.enbId)
        assertEquals(0, LteEciSplit.fromEci(0)?.cellId)
        assertEquals(0, LteEciSplit.fromEci(255)?.enbId)
        assertEquals(255, LteEciSplit.fromEci(255)?.cellId)
        assertEquals(1, LteEciSplit.fromEci(256)?.enbId)
        assertEquals(0, LteEciSplit.fromEci(256)?.cellId)
    }

    @Test
    fun fromEci_rejectsMissingAndOutOfRange() {
        assertNull(LteEciSplit.fromEci(null))
        assertNull(LteEciSplit.fromEci(-1))
        assertNull(LteEciSplit.fromEci(0x10000000))
    }
}
