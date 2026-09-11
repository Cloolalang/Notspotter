package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MonitoringAnnouncementPlaybackOrderTest {

    @Test
    fun serviceStateAnnouncementsPlayBeforeTechnologyAndCellReselect() {
        val events = MonitoringUpdateEvents(
            cellChangeAnnouncement = "Cell reselect",
            technologyChangeAnnouncement = "Technology change, 4 G",
            limitedServiceStateAnnouncement = "Vodafone, Limited service, 4 G",
            limitedServiceOperatorChangeAnnouncement = "Vodafone home, E E visited, Limited service, 4 G"
        )

        assertEquals(
            listOf(
                MonitoringAnnouncementKind.LIMITED_SERVICE_STATE,
                MonitoringAnnouncementKind.LIMITED_SERVICE_OPERATOR,
                MonitoringAnnouncementKind.TECHNOLOGY_CHANGE,
                MonitoringAnnouncementKind.CELL_IDENTITY
            ),
            events.immediateAnnouncements().map { it.kind }
        )
    }

    @Test
    fun noSignalAndRestoredPlayBeforeLimitedAndFullService() {
        val events = MonitoringUpdateEvents(
            noSignalStateAnnouncement = "Vodafone, 4 G, no signal",
            limitedServiceStateAnnouncement = "Vodafone, Full service, 4 G",
            cellChangeAnnouncement = "Cell reselect",
            technologyChangeAnnouncement = "Technology change, 4 G"
        )

        assertEquals(
            listOf(
                MonitoringAnnouncementKind.NO_SIGNAL_STATE,
                MonitoringAnnouncementKind.LIMITED_SERVICE_STATE,
                MonitoringAnnouncementKind.TECHNOLOGY_CHANGE,
                MonitoringAnnouncementKind.CELL_IDENTITY
            ),
            events.immediateAnnouncements().map { it.kind }
        )
    }

    @Test
    fun deadzoneDoesNotQueueNoSignalOnSamePoll() {
        val events = MonitoringUpdateEvents(
            deadzoneAnnouncement = "Vodafone, dead zone, no service, no SOS calls",
            limitedServiceStateAnnouncement = "Vodafone, Full service, 4 G"
        )

        assertEquals(
            listOf(
                MonitoringAnnouncementKind.DEADZONE,
                MonitoringAnnouncementKind.LIMITED_SERVICE_STATE
            ),
            events.immediateAnnouncements().map { it.kind }
        )
    }

    @Test
    fun home4gToAlt4gStyleTransitionOrdersLimitedServiceBeforeCellReselect() {
        val events = MonitoringUpdateEvents(
            cellChangeAnnouncement = "Cell reselect, 4 G",
            limitedServiceStateAnnouncement = "Vodafone home, E E visited, Limited service, 4 G"
        )

        assertEquals(
            listOf(
                MonitoringAnnouncementKind.LIMITED_SERVICE_STATE,
                MonitoringAnnouncementKind.CELL_IDENTITY
            ),
            events.immediateAnnouncements().map { it.kind }
        )
    }

    @Test
    fun g2FallbackPlaysAfterServiceStateAndBeforeTechnologyChange() {
        val events = MonitoringUpdateEvents(
            noSignalStateAnnouncement = "Vodafone, Signal restored, 2 G",
            g2FallbackAnnouncement = "Vodafone, 2 G",
            technologyChangeAnnouncement = "Technology change, 2 G",
            cellChangeAnnouncement = "Cell reselect, 2 G"
        )

        assertEquals(
            listOf(
                MonitoringAnnouncementKind.NO_SIGNAL_STATE,
                MonitoringAnnouncementKind.G2_FALLBACK,
                MonitoringAnnouncementKind.TECHNOLOGY_CHANGE,
                MonitoringAnnouncementKind.CELL_IDENTITY
            ),
            events.immediateAnnouncements().map { it.kind }
        )
    }

    @Test
    fun tier5ImmediatePlaysAfterLimitedServiceAndBeforeG2AndOperatorChange() {
        val events = MonitoringUpdateEvents(
            limitedServiceStateAnnouncement = "Vodafone, Limited service, 4 G",
            tier5Announcement = "Vodafone, 4 G, signal low",
            tier5Immediate = true,
            limitedServiceOperatorChangeAnnouncement = "Vodafone home, E E visited, Limited service, 4 G",
            g2FallbackAnnouncement = "Vodafone, 2 G"
        )

        assertEquals(
            listOf(
                MonitoringAnnouncementKind.LIMITED_SERVICE_STATE,
                MonitoringAnnouncementKind.TIER5,
                MonitoringAnnouncementKind.G2_FALLBACK,
                MonitoringAnnouncementKind.LIMITED_SERVICE_OPERATOR
            ),
            events.immediateAnnouncements().map { it.kind }
        )
    }
}
