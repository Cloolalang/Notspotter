package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceAnnouncementQueueTest {

    @Test
    fun noSignalThenRestoredKeepsOnlyRestored() {
        val queue = VoiceAnnouncementQueue()
        queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.NO_SIGNAL_STATE, "no signal")))
        queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.NO_SIGNAL_STATE, "signal restored")))

        assertEquals(listOf("signal restored"), queue.pendingMessages())
    }

    @Test
    fun flickerCollapsesToLatestServiceState() {
        val queue = VoiceAnnouncementQueue()
        queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.NO_SIGNAL_STATE, "no signal")))
        queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.LIMITED_SERVICE_STATE, "limited")))
        queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.DEADZONE, "deadzone")))
        queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.NO_SIGNAL_STATE, "signal restored")))

        assertEquals(
            listOf(MonitoringAnnouncementKind.NO_SIGNAL_STATE),
            queue.pendingKinds()
        )
        assertEquals(listOf("signal restored"), queue.pendingMessages())
    }

    @Test
    fun laterCellReselectReplacesEarlierOne() {
        val queue = VoiceAnnouncementQueue()
        queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.CELL_IDENTITY, "PCI 10")))
        queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.CELL_IDENTITY, "PCI 20")))

        assertEquals(listOf("PCI 20"), queue.pendingMessages())
    }

    @Test
    fun higherPriorityKeepsReselectBell() {
        val queue = VoiceAnnouncementQueue()
        queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.CELL_IDENTITY, "PCI 10")))
        val result = queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.DEADZONE, "deadzone")))

        assertEquals(listOf(MonitoringAnnouncementKind.DEADZONE), queue.pendingKinds())
        assertEquals(
            listOf(MonitoringAnnouncementKind.CELL_IDENTITY),
            result.toneOnly.map { it.kind }
        )
    }

    @Test
    fun signalLowKeepsReselectBellWhenSpeechBudgetIsUsed() {
        val queue = VoiceAnnouncementQueue()
        queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.CELL_IDENTITY, "PCI 10")))
        val result = queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.TIER5, "signal low")))

        assertEquals(listOf(MonitoringAnnouncementKind.TIER5), queue.pendingKinds())
        assertEquals(
            listOf(MonitoringAnnouncementKind.CELL_IDENTITY),
            result.toneOnly.map { it.kind }
        )
    }

    @Test
    fun noSignalDropsSignalLowAndKeepsReselectBell() {
        val queue = VoiceAnnouncementQueue()
        queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.CELL_IDENTITY, "PCI 10")))
        val result = queue.enqueue(
            listOf(
                announcement(MonitoringAnnouncementKind.NO_SIGNAL_STATE, "no signal"),
                announcement(MonitoringAnnouncementKind.TIER5, "signal low")
            )
        )

        assertEquals(
            listOf(MonitoringAnnouncementKind.NO_SIGNAL_STATE),
            queue.pendingKinds()
        )
        assertEquals(
            listOf(MonitoringAnnouncementKind.CELL_IDENTITY),
            result.toneOnly.map { it.kind }
        )
    }

    @Test
    fun reselectBehindLimitedServiceIsBellOnly() {
        val queue = VoiceAnnouncementQueue()
        queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.LIMITED_SERVICE_STATE, "limited")))
        val result = queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.CELL_IDENTITY, "PCI 10")))

        assertFalse(result.interruptCurrent)
        assertEquals(listOf(MonitoringAnnouncementKind.LIMITED_SERVICE_STATE), queue.pendingKinds())
        assertEquals(
            listOf(MonitoringAnnouncementKind.CELL_IDENTITY),
            result.toneOnly.map { it.kind }
        )
    }

    @Test
    fun overflowAfterLimitedServiceIsToneOnly() {
        val queue = VoiceAnnouncementQueue()
        val result = queue.enqueue(
            listOf(
                announcement(MonitoringAnnouncementKind.LIMITED_SERVICE_STATE, "limited"),
                announcement(MonitoringAnnouncementKind.TECHNOLOGY_CHANGE, "4 G"),
                announcement(MonitoringAnnouncementKind.CELL_IDENTITY, "PCI 10")
            )
        )

        assertEquals(
            listOf(MonitoringAnnouncementKind.LIMITED_SERVICE_STATE),
            queue.pendingKinds()
        )
        assertEquals(
            listOf(
                MonitoringAnnouncementKind.TECHNOLOGY_CHANGE,
                MonitoringAnnouncementKind.CELL_IDENTITY
            ),
            result.toneOnly.map { it.kind }
        )
    }

    @Test
    fun technologyChangeUsesBudgetAndReselectBecomesBell() {
        val queue = VoiceAnnouncementQueue()
        queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.CELL_IDENTITY, "PCI 10")))
        val result = queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.TECHNOLOGY_CHANGE, "4 G")))

        assertEquals(listOf(MonitoringAnnouncementKind.TECHNOLOGY_CHANGE), queue.pendingKinds())
        assertEquals(
            listOf(MonitoringAnnouncementKind.CELL_IDENTITY),
            result.toneOnly.map { it.kind }
        )
    }

    @Test
    fun cellReselectKeepsSpecialCellVoice() {
        val queue = VoiceAnnouncementQueue()
        val result = queue.enqueue(
            listOf(
                announcement(MonitoringAnnouncementKind.CELL_IDENTITY, "channel 6300, PCI 106"),
                announcement(MonitoringAnnouncementKind.SPECIAL_CELL, "streetworks, Robin Hood, sector two")
            )
        )

        assertEquals(
            listOf(
                MonitoringAnnouncementKind.CELL_IDENTITY,
                MonitoringAnnouncementKind.SPECIAL_CELL
            ),
            queue.pendingKinds()
        )
        assertTrue(result.toneOnly.isEmpty())
    }

    @Test
    fun isolatedReselectIsSpoken() {
        val queue = VoiceAnnouncementQueue()
        val result = queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.CELL_IDENTITY, "PCI 10")))

        assertEquals(listOf(MonitoringAnnouncementKind.CELL_IDENTITY), queue.pendingKinds())
        assertTrue(result.toneOnly.isEmpty())
    }

    @Test
    fun playingReselectIsInterruptedByDeadzone() {
        val queue = VoiceAnnouncementQueue()
        queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.CELL_IDENTITY, "PCI 10")))
        val playing = queue.startNext()!!

        val result = queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.DEADZONE, "deadzone")))

        assertTrue(result.interruptCurrent)
        assertFalse(queue.isPlaying(playing.id))
        assertEquals(listOf(MonitoringAnnouncementKind.DEADZONE), queue.pendingKinds())
    }

    @Test
    fun playingDeadzoneKeepsReselectBell() {
        val queue = VoiceAnnouncementQueue()
        queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.DEADZONE, "deadzone")))
        val playing = queue.startNext()!!

        val result = queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.CELL_IDENTITY, "PCI 10")))

        assertFalse(result.interruptCurrent)
        assertTrue(queue.isPlaying(playing.id))
        assertEquals(emptyList<MonitoringAnnouncementKind>(), queue.pendingKinds())
        assertEquals(
            listOf(MonitoringAnnouncementKind.CELL_IDENTITY),
            result.toneOnly.map { it.kind }
        )
    }

    @Test
    fun rapidReselectsKeepBellAndDropExtraVoice() {
        val queue = VoiceAnnouncementQueue()
        queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.CELL_IDENTITY, "PCI 10")))
        val playing = queue.startNext()!!

        val result = queue.enqueue(listOf(announcement(MonitoringAnnouncementKind.CELL_IDENTITY, "PCI 20")))

        assertTrue(result.interruptCurrent)
        assertFalse(queue.isPlaying(playing.id))
        assertTrue(queue.pendingKinds().isEmpty())
        assertEquals(listOf("PCI 20"), result.toneOnly.map { it.message })
    }

    @Test
    fun drainOrderFollowsPriorityWithinBudget() {
        val queue = VoiceAnnouncementQueue()
        val result = queue.enqueue(
            listOf(
                announcement(MonitoringAnnouncementKind.TECHNOLOGY_CHANGE, "4 G"),
                announcement(MonitoringAnnouncementKind.LIMITED_SERVICE_STATE, "limited"),
                announcement(MonitoringAnnouncementKind.TIER5, "signal low")
            )
        )

        assertEquals(
            listOf(MonitoringAnnouncementKind.LIMITED_SERVICE_STATE),
            queue.pendingKinds()
        )
        assertEquals(
            listOf(MonitoringAnnouncementKind.TECHNOLOGY_CHANGE),
            result.toneOnly.map { it.kind }
        )
    }

    private fun announcement(
        kind: MonitoringAnnouncementKind,
        message: String
    ): MonitoringAnnouncement {
        return MonitoringAnnouncement(kind, message)
    }
}
