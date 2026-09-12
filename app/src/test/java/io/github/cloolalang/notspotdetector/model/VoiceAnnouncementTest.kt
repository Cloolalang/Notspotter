package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceAnnouncementTest {

    @Test
    fun immediatePlaybackOrder_matchesConfirmedPriorities() {
        val priorities = VoiceAnnouncement.immediatePlaybackOrder.map { VoiceAnnouncement.priorityFor(it) }
        assertEquals(listOf(1, 2, 4, 5, 6, 8, 8, 9, 10), priorities)
    }

    @Test
    fun immediatePlaybackOrder_isMonotonicNonDecreasingPriority() {
        val priorities = VoiceAnnouncement.immediatePlaybackOrder.map { VoiceAnnouncement.priorityFor(it) }
        assertTrue(priorities.zipWithNext().all { (a, b) -> a <= b })
    }

    @Test
    fun specialCellIsLowestImmediatePriority() {
        assertEquals(
            10,
            VoiceAnnouncement.priorityFor(MonitoringAnnouncementKind.SPECIAL_CELL)
        )
    }
}
