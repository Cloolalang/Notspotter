package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockPingOverlayTest {

    private val monitoring = ConnectivityStats(
        isMonitoring = true,
        cellularAvailable = true,
        rttMs = 30L,
        jitterMs = 4L,
        packetLossPercent = 0f,
        pingsSent = 2,
        lastPingTimestampMs = 1_000L
    )

    @Test
    fun mockPingReplacesRttLossAndMarksTheSession() {
        val mock = PassiveMockSettings(
            pingMockEnabled = true,
            pingRttMs = 90,
            packetLossPercent = 8
        )
        val overlaid = monitoring.withMockPingApplied(
            mock = mock,
            previous = monitoring,
            tick = false
        )

        assertEquals(90L, overlaid.rttMs)
        assertEquals(0L, overlaid.jitterMs)
        assertEquals(8f, overlaid.packetLossPercent)
        assertEquals(1_000L, overlaid.lastPingTimestampMs)
        assertTrue(overlaid.mockPingActive)
        assertEquals(ConnectionQuality.POOR, overlaid.withQuality(ThresholdSettings()).quality)
    }

    @Test
    fun tickAdvancesThePingTimestamp() {
        val mock = PassiveMockSettings(pingMockEnabled = true, pingRttMs = 20)
        val previous = monitoring.copy(mockPingActive = true)
        val overlaid = monitoring.withMockPingApplied(mock, previous, tick = true)

        assertTrue(overlaid.lastPingTimestampMs > previous.lastPingTimestampMs)
        assertEquals(ConnectionQuality.GOOD, overlaid.withQuality(ThresholdSettings()).quality)
    }

    @Test
    fun turningMockPingOffClearsTheOverlay() {
        val previous = monitoring.copy(mockPingActive = true, rttMs = 200L, packetLossPercent = 40f)
        val cleared = previous.withMockPingApplied(
            mock = PassiveMockSettings(pingMockEnabled = false),
            previous = previous,
            tick = false
        )

        assertEquals(null, cleared.rttMs)
        assertEquals(0f, cleared.packetLossPercent)
        assertEquals(0, cleared.pingsSent)
        assertFalse(cleared.mockPingActive)
    }

    @Test
    fun passiveOnlySessionIgnoresMockPing() {
        val passive = monitoring.copy(isPassiveOnlySession = true)
        val overlaid = passive.withMockPingApplied(
            mock = PassiveMockSettings(pingMockEnabled = true, pingRttMs = 400),
            previous = passive,
            tick = true
        )

        assertEquals(30L, overlaid.rttMs)
        assertFalse(overlaid.mockPingActive)
    }
}
