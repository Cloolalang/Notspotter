package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ZoneTestTest {

    @Test
    fun jpegFileName_includesNameAndOperator() {
        assertEquals(
            "Zone test - High street - Vodafone.jpg",
            ZoneTest.jpegFileName("High street", "Vodafone")
        )
    }

    @Test
    fun jpegFileName_stripsUnsafeCharacters() {
        assertEquals(
            "Zone test - site_1 - EE.jpg",
            ZoneTest.jpegFileName("site/1", "EE")
        )
    }

    @Test
    fun jpegFileName_blankNameUsesZone() {
        assertEquals("Zone test - zone.jpg", ZoneTest.jpegFileName("  ", null))
    }

    @Test
    fun sampleWindow_opensAcrossTheWholeRun() {
        assertEquals(30_000L, ZoneTest.sampleWindowMs(false, 1_000L, 50_000L, 30_000L))
        assertEquals(30_000L, ZoneTest.sampleWindowMs(true, null, 50_000L, 30_000L))
        assertEquals(49_000L, ZoneTest.sampleWindowMs(true, 1_000L, 50_000L, 30_000L))
    }

    @Test
    fun sharedNameSuffix_keepsEachOperatorsOwnPrefix() {
        assertEquals("site1", ZoneTest.sharedNameSuffix("Vodafone_site1", "Vodafone"))
        assertEquals("EE_site1", ZoneTest.nameWithOperator("EE", "site1"))
        assertEquals("EE_", ZoneTest.nameWithOperator("EE", ""))
        assertEquals("roof", ZoneTest.sharedNameSuffix("roof", "Vodafone"))
    }

    @Test
    fun walkFileName_startsWithOperatorThenRsrp() {
        assertEquals("VodafoneRSRP_", ZoneTest.fileNamePrefix("Vodafone", ZoneTest.WALK_KPI))
        assertEquals(
            "site1",
            ZoneTest.sharedNameSuffix("VodafoneRSRP_site1", "Vodafone", ZoneTest.WALK_KPI)
        )
        assertEquals("EERSRP_site1", ZoneTest.nameWithOperator("EE", "site1", ZoneTest.WALK_KPI))
        assertEquals(
            "VodafoneRSRP_High street.jpg",
            ZoneTest.walkJpegFileName("VodafoneRSRP_High street", "Vodafone")
        )
        assertEquals(
            "VodafoneRSRP_High street.jpg",
            ZoneTest.walkJpegFileName("High street", "Vodafone")
        )
    }
}
