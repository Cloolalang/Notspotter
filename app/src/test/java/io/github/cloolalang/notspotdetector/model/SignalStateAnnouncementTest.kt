package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.Assert.assertEquals
import org.junit.Test

class SignalStateAnnouncementTest {

    @Test
    fun formatTechnologyChange_includesOperatorAndSpokenRat() {
        val announcement = SignalStateAnnouncement.formatTechnologyChange(
            CellularSignalReader.RADIO_4G,
            "EE"
        )
        assertEquals("E E, Technology change, 4 G", announcement)
    }

    @Test
    fun formatNoSignalChange_announcesEnterAndExit() {
        assertEquals(
            "4 G, no signal",
            SignalStateAnnouncement.formatNoSignalChange(
                active = true,
                networkOperatorName = null,
                radioAccessType = CellularSignalReader.RADIO_4G
            )
        )
        assertEquals(
            "E E, 4 G, no signal",
            SignalStateAnnouncement.formatNoSignalChange(
                active = true,
                networkOperatorName = "EE",
                radioAccessType = CellularSignalReader.RADIO_4G
            )
        )
        assertEquals(
            "E E, Signal restored",
            SignalStateAnnouncement.formatNoSignalChange(active = false, networkOperatorName = "EE")
        )
    }

    @Test
    fun formatLimitedServiceChange_announcesEnterAndExit() {
        assertEquals(
            "Limited service",
            SignalStateAnnouncement.formatLimitedServiceChange(active = true, networkOperatorName = null)
        )
        assertEquals(
            "E E, Full service",
            SignalStateAnnouncement.formatLimitedServiceChange(active = false, networkOperatorName = "EE")
        )
    }
}
