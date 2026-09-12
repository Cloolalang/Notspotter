package io.github.cloolalang.notspotdetector

import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.ConnectivityStats
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.SpecialCell
import io.github.cloolalang.notspotdetector.model.SpecialCellAnnouncement
import io.github.cloolalang.notspotdetector.model.SpecialCellCatalog
import io.github.cloolalang.notspotdetector.model.SpecialCellRat
import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MonitorStateSpecialCellAnnouncementTest {

    private val passiveSettings = PassiveSignalSettings()
    private val catalog = SpecialCellCatalog(
        cells = listOf(
            SpecialCell(
                site = "Robin Hood",
                type = "Streetworks",
                mno = "Vodafone",
                rat = SpecialCellRat.G4,
                sector = "S2",
                channel = 6300,
                pci = 106,
                plmn = "23415"
            ),
            SpecialCell(
                site = "Hill Farm",
                type = "Macro",
                mno = "Vodafone",
                rat = SpecialCellRat.G4,
                sector = "S3",
                channel = 3501,
                pci = 328,
                plmn = "23415"
            )
        )
    )

    @Before
    fun setUp() {
        MonitorState.setRunning(false)
        MonitorState.setMonitoringSettings(MonitoringSettings(monitor2gFallback = true))
        MonitorState.setPassiveSignalSettings(passiveSettings)
        MonitorState.setAudioVolumes(AudioVolumeSettings())
        MonitorState.setSpecialCellCatalog(catalog)
        MonitorState.beginPassiveOnlySession()
        MonitorState.setRunning(true)
    }

    @After
    fun tearDown() {
        MonitorState.setRunning(false)
        MonitorState.setSpecialCellCatalog(SpecialCellCatalog())
        MonitorState.setAudioVolumes(AudioVolumeSettings())
    }

    @Test
    fun listedCellSpeaksSiteThenUnlistedCellSpeaksMacroCell() {
        val listed = camped4g(earfcn = 6300, pci = 106)
        val entry = MonitorState.updateStats(listed)
        assertEquals("streetworks, Robin Hood, sector two", entry.specialCellAnnouncement)

        assertNull(MonitorState.updateStats(listed).specialCellAnnouncement)

        val exit = MonitorState.updateStats(camped4g(earfcn = 6300, pci = 42))
        assertEquals(SpecialCellAnnouncement.EXIT_MACRO_CELL, exit.specialCellAnnouncement)
    }

    @Test
    fun nextListedCellSpeaksNewSiteNotMacroCell() {
        MonitorState.updateStats(camped4g(earfcn = 6300, pci = 106))

        val nextListed = MonitorState.updateStats(camped4g(earfcn = 3501, pci = 328))
        assertEquals("macro, Hill Farm, sector three", nextListed.specialCellAnnouncement)
    }

    @Test
    fun noSignalDoesNotSpeakMacroCell() {
        MonitorState.updateStats(camped4g(earfcn = 6300, pci = 106))

        val noSignal = camped4g(earfcn = 6300, pci = 106).copy(
            noSignalActive = true,
            rsrpDbm = null,
            rsrqDb = null
        )
        assertNull(MonitorState.updateStats(noSignal).specialCellAnnouncement)
    }

    private fun camped4g(earfcn: Int, pci: Int): ConnectivityStats {
        return ConnectivityStats(
            isMonitoring = true,
            isPassiveOnlySession = true,
            cellularAvailable = true,
            radioAccessType = CellularSignalReader.RADIO_4G,
            lteEarfcn = earfcn,
            ltePci = pci,
            rsrpDbm = -90,
            rsrqDb = -10,
            plmn = "23415",
            homePlmn = "23415",
            networkOperatorName = "Vodafone",
            signalPermissionGranted = true,
            cellIdentityPermissionGranted = true
        )
    }
}
