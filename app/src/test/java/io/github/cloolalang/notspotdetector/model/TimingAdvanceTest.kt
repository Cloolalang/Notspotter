package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimingAdvanceTest {

    @Test
    fun lteIndexRejectsUnavailableAndConvertsToDistance() {
        assertNull(TimingAdvance.takeLteIndex(Int.MAX_VALUE))
        assertNull(TimingAdvance.takeLteIndex(-1))
        assertNull(TimingAdvance.takeLteIndex(1_283))
        val reading = TimingAdvance.takeLteIndex(42)
        assertEquals(TimingAdvanceKind.LTE, reading!!.kind)
        assertEquals(3_281, reading.oneWayMetres())
        assertEquals("42 · 3.3 km", reading.formatDisplay())
        assertEquals("three point three kilometres", reading.distanceSpeech())
    }

    @Test
    fun gsmAndNrUseTheirOwnUnits() {
        assertEquals(554, TimingAdvance.takeGsmIndex(1)!!.oneWayMetres())
        assertNull(TimingAdvance.takeGsmIndex(220))
        val nr = TimingAdvance.takeNrMicros(20)
        assertEquals(3_000, nr!!.oneWayMetres())
        assertEquals("20 μs · 3.0 km", nr.formatDisplay())
        assertEquals("three kilometres", nr.distanceSpeech())
        assertEquals("zero metres", TimingAdvance.takeGsmIndex(0)!!.distanceSpeech())
        assertEquals("five hundred fifty four metres", TimingAdvance.takeGsmIndex(1)!!.distanceSpeech())
        assertNull(TimingAdvance.takeNrMicros(Int.MAX_VALUE))
    }
}
