package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioDebugSnapshotTest {

    @Test
    fun flags_vmo2ChosenOnVodafoneSim_marksPlmnAndSsMismatch() {
        val flags = RadioDebugSnapshot.flags(
            chosenLteEarfcn = 6400,
            chosenPlmn = "23410",
            expectedPlmn = "23415",
            homePlmn = "23415",
            serviceStateLteEarfcn = 6300,
            registeredLteCount = 2,
            registeredPlmns = listOf("23415", "23410")
        )

        assertTrue(flags.contains("plmn-mismatch"))
        assertTrue(flags.contains("ss-earfcn-mismatch"))
        assertTrue(flags.contains("multi-registered"))
        assertTrue(flags.contains("multi-plmn"))
    }

    @Test
    fun flags_matchingVodafoneCell_isClean() {
        val flags = RadioDebugSnapshot.flags(
            chosenLteEarfcn = 6300,
            chosenPlmn = "234-15",
            expectedPlmn = "23415",
            homePlmn = "23415",
            serviceStateLteEarfcn = 6300,
            registeredLteCount = 1,
            registeredPlmns = listOf("23415"),
            chosenLtePci = 106,
            serviceStateLtePci = 106
        )

        assertTrue(flags.isEmpty())
    }

    @Test
    fun flags_neighbourPciOnSameEarfcn_marksSsPciMismatch() {
        val flags = RadioDebugSnapshot.flags(
            chosenLteEarfcn = 6300,
            chosenPlmn = "23415",
            expectedPlmn = "23415",
            homePlmn = "23415",
            serviceStateLteEarfcn = 6300,
            registeredLteCount = 1,
            registeredPlmns = listOf("23410"),
            chosenLtePci = 107,
            serviceStateLtePci = 106
        )

        assertTrue(flags.contains("ss-pci-mismatch"))
        assertFalse(flags.contains("ss-earfcn-mismatch"))
    }

    @Test
    fun logLine_includesChosenAndCellDump() {
        val snapshot = RadioDebugSnapshot(
            chosenRat = "4G",
            chosenLteEarfcn = 6400,
            chosenLtePci = 118,
            chosenRsrp = -95,
            chosenRsrq = -12,
            chosenPlmn = "23410",
            expectedPlmn = "23415",
            homePlmn = "23415",
            serviceStateLte = "6300/107",
            signalLtePcis = "107",
            dualSim = true,
            simLabel = "Vodafone",
            registeredLteCount = 2,
            cells = "LR:6400/118/-95/23410/a1200 LP:6300/107/-88/23415/a400",
            flags = listOf("plmn-mismatch", "ss-earfcn-mismatch")
        )

        val line = snapshot.logLine("19:32:01.400")
        assertTrue(line.contains("chosen=6400/118"))
        assertTrue(line.contains("ss=6300/107"))
        assertTrue(line.contains("LP:6300/107"))
        assertFalse(snapshot.bannerLine().contains("ok"))
        assertEquals("6400/118", RadioDebugSnapshot.formatEarfcnPci(6400, 118))
    }
}
