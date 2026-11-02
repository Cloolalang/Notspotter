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
    fun walkJpegFileName_usesTheSameNameAndOperator() {
        assertEquals(
            "Zone walk - High street - Vodafone.jpg",
            ZoneTest.walkJpegFileName("High street", "Vodafone")
        )
    }
}
