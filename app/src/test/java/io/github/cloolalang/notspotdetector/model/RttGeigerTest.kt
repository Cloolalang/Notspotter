package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RttGeigerTest {

    @Test
    fun goodRttClicksOncePerPingTest() {
        assertEquals(TEST_INTERVAL_MS, intervalFor(rttMs = 10))
        assertEquals(TEST_INTERVAL_MS, intervalFor(rttMs = 25))
        assertEquals(TEST_INTERVAL_MS, intervalFor(rttMs = 40))
        assertEquals(TEST_INTERVAL_MS, intervalFor(rttMs = 45))
        assertTrue(RttGeiger.clicksOncePerTest(45))
        assertFalse(RttGeiger.clicksOncePerTest(46))
    }

    @Test
    fun aFewMillisecondsAboveGoodDoesNotJumpToOneSecond() {
        val at52 = intervalFor(rttMs = 52)
        assertTrue(at52 > 3_000L)
        assertEquals(3_618L, at52)
        assertTrue(intervalFor(rttMs = 46) > 4_500L)
    }

    @Test
    fun latencyKnotsMatchTheAttentionCurve() {
        assertEquals(2_500L, intervalFor(rttMs = 60))
        assertEquals(1_250L, intervalFor(rttMs = 80))
        assertEquals(800L, intervalFor(rttMs = 100))
        assertEquals(500L, intervalFor(rttMs = 120))
        assertTrue(intervalFor(rttMs = 140) < 500L)
        assertEquals(RttGeiger.FAST_INTERVAL_MS, intervalFor(rttMs = 300))
        assertEquals(RttGeiger.FAST_INTERVAL_MS, intervalFor(rttMs = 600))
    }

    @Test
    fun longerTestsKeepTheSameShape() {
        val dose = RttGeiger.doseMs(rttMs = 40L, packetLossPercent = 0f, pingsSent = 1)!!
        assertEquals(10_000L, RttGeiger.clickIntervalMs(dose, packetLossPercent = 0f, testIntervalMs = 10_000L))
        val atSixty = RttGeiger.doseMs(rttMs = 60L, packetLossPercent = 0f, pingsSent = 1)!!
        assertEquals(5_000L, RttGeiger.clickIntervalMs(atSixty, packetLossPercent = 0f, testIntervalMs = 10_000L))
    }

    @Test
    fun timedOutPingWithNoMeasuredLossStillClicks() {
        assertEquals(
            RttGeiger.ONCE_PER_TEST_RTT_MS,
            RttGeiger.doseMs(
                rttMs = null,
                packetLossPercent = 0f,
                pingsSent = 1
            )
        )
    }

    @Test
    fun totalPacketLossProducesNoDose() {
        assertNull(RttGeiger.doseMs(rttMs = 40L, packetLossPercent = 100f, pingsSent = 4))
        assertNull(RttGeiger.doseMs(rttMs = null, packetLossPercent = 100f, pingsSent = 4))
    }

    @Test
    fun noPingSampleProducesNoDose() {
        assertNull(
            RttGeiger.doseMs(
                rttMs = null,
                packetLossPercent = 0f,
                pingsSent = 0
            )
        )
    }

    @Test
    fun packetLossDoublesClickSpeedAtFivePercentAndAgainAtTen() {
        val atZero = intervalFor(rttMs = 25, packetLossPercent = 0f)
        val justUnder = intervalFor(rttMs = 25, packetLossPercent = 4.9f)
        val atFive = intervalFor(rttMs = 25, packetLossPercent = 5f)
        val atTen = intervalFor(rttMs = 25, packetLossPercent = 10f)
        val aboveTen = intervalFor(rttMs = 25, packetLossPercent = 40f)

        assertEquals(TEST_INTERVAL_MS, atZero)
        assertEquals(atZero, justUnder)
        assertEquals(atZero / 2, atFive)
        assertEquals(atZero / 4, atTen)
        assertEquals(atTen, aboveTen)
    }

    @Test
    fun packetLossDoublesTheRttRateInsteadOfReplacingIt() {
        val rttOnly = intervalFor(rttMs = 400, packetLossPercent = 0f)
        val both = intervalFor(rttMs = 400, packetLossPercent = 5f)

        assertEquals(
            RttGeiger.doseMs(400L, packetLossPercent = 0f, pingsSent = 1),
            RttGeiger.doseMs(400L, packetLossPercent = 5f, pingsSent = 1)
        )
        assertEquals(rttOnly / 2, both)
    }

    @Test
    fun geigerPlaysDuringActivePingAndStaysQuietInPassiveSessions() {
        assertTrue(
            ConnectivityStats(
                isMonitoring = true,
                cellularAvailable = true,
                rttMs = 27L,
                pingsSent = 1
            ).shouldPlayRttGeiger()
        )
        assertFalse(
            ConnectivityStats(
                isMonitoring = false,
                isPassiveOnlySession = true,
                cellularAvailable = true,
                rttMs = 160L,
                pingsSent = 1
            ).shouldPlayRttGeiger()
        )
        assertFalse(
            ConnectivityStats(
                isMonitoring = true,
                isPassiveIdleMode = true,
                cellularAvailable = true,
                rttMs = 160L,
                pingsSent = 1
            ).shouldPlayRttGeiger()
        )
        assertFalse(
            ConnectivityStats(isMonitoring = true, cellularAvailable = true).shouldPlayRttGeiger()
        )
    }

    @Test
    fun geigerKeepsClickingDuringAPingTestUnlessPacketLossIsTotal() {
        val inService = ConnectivityStats(
            isMonitoring = true,
            cellularAvailable = true,
            rttMs = 40L,
            pingsSent = 4
        )
        assertTrue(inService.copy(cellularAvailable = false).shouldPlayRttGeiger())
        assertTrue(inService.copy(isCompleteNoService = true).shouldPlayRttGeiger())
        assertTrue(inService.copy(noSignalActive = true).shouldPlayRttGeiger())
        assertTrue(inService.copy(deadzoneActive = true).shouldPlayRttGeiger())
        assertFalse(
            inService.copy(packetLossPercent = 100f, pingsFailed = 4).shouldPlayRttGeiger()
        )
    }

    private fun intervalFor(rttMs: Long, packetLossPercent: Float = 0f): Long {
        val dose = RttGeiger.doseMs(rttMs, packetLossPercent = packetLossPercent, pingsSent = 1)!!
        return RttGeiger.clickIntervalMs(dose, packetLossPercent, testIntervalMs = TEST_INTERVAL_MS)
    }

    private companion object {
        const val TEST_INTERVAL_MS = 5_000L
    }
}
