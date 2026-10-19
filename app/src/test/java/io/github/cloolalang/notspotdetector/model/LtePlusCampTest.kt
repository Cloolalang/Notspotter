package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LtePlusCampTest {

    @Test
    fun sameCellKeepsPlusAfterTheRawFlagDrops() {
        val camp = LtePlusCamp()
        assertTrue(camp.showPlus(radioIs4g = true, rawPlus = true, earfcn = 6300, pci = 328))
        assertTrue(camp.showPlus(radioIs4g = true, rawPlus = false, earfcn = 6300, pci = 328))
        assertTrue(camp.showPlus(radioIs4g = true, rawPlus = false, earfcn = null, pci = null))
    }

    @Test
    fun aDifferentCellUsesTheRawFlag() {
        val camp = LtePlusCamp()
        camp.showPlus(radioIs4g = true, rawPlus = true, earfcn = 6300, pci = 328)
        assertFalse(camp.showPlus(radioIs4g = true, rawPlus = false, earfcn = 3501, pci = 12))
        assertFalse(camp.showPlus(radioIs4g = true, rawPlus = false, earfcn = null, pci = null))
        assertTrue(camp.showPlus(radioIs4g = true, rawPlus = true, earfcn = 3501, pci = 12))
    }

    @Test
    fun leavingLteHidesPlusWithoutForgettingTheCell() {
        val camp = LtePlusCamp()
        camp.showPlus(radioIs4g = true, rawPlus = true, earfcn = 6300, pci = 328)
        assertFalse(camp.showPlus(radioIs4g = false, rawPlus = false, earfcn = null, pci = null))
        assertTrue(camp.showPlus(radioIs4g = true, rawPlus = false, earfcn = 6300, pci = 328))
    }
}
