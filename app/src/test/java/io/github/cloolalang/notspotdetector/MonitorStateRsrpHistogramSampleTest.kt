package io.github.cloolalang.notspotdetector

import io.github.cloolalang.notspotdetector.model.ConnectivityStats
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MonitorStateRsrpHistogramSampleTest {

    @Before
    fun setUp() {
        MonitorState.setRunning(false)
        MonitorState.setMonitoringSettings(MonitoringSettings())
        MonitorState.setPassiveSignalSettings(PassiveSignalSettings())
        MonitorState.setRunning(true)
        MonitorState.beginPassiveOnlySession()
        MonitorState.updateStats(lteStats(-95))
    }

    @After
    fun tearDown() {
        MonitorState.setRunning(false)
    }

    @Test
    fun tickRsrpHistogramSample_appendsCurrentRsrp() {
        MonitorState.tickRsrpHistogramSample()
        MonitorState.tickRsrpHistogramSample()

        val history = MonitorState.rsrpHistory.value
        assertEquals(2, history.size)
        assertTrue(history.all { it.rsrpDbm == -95 })
    }

    @Test
    fun tickRsrpHistogramSample_ignoredWhenNotRunning() {
        MonitorState.setRunning(false)
        MonitorState.tickRsrpHistogramSample()

        assertTrue(MonitorState.rsrpHistory.value.isEmpty())
    }

    @Test
    fun histogramClearsWhenTechnologyChanges() {
        MonitorState.tickRsrpHistogramSample()
        MonitorState.tickRsrpHistogramSample()
        assertEquals(2, MonitorState.rsrpHistory.value.size)

        MonitorState.updateStats(lteStats(-80, CellularSignalReader.RADIO_5G))

        assertTrue(MonitorState.rsrpHistory.value.isEmpty())
        MonitorState.tickRsrpHistogramSample()
        val history = MonitorState.rsrpHistory.value
        assertEquals(1, history.size)
        assertEquals(-80, history.single().rsrpDbm)
    }

    @Test
    fun clearRsrpHistogram_emptiesHistory() {
        MonitorState.tickRsrpHistogramSample()
        MonitorState.tickRsrpHistogramSample()
        assertEquals(2, MonitorState.rsrpHistory.value.size)

        MonitorState.clearRsrpHistogram()

        assertTrue(MonitorState.rsrpHistory.value.isEmpty())
    }

    @Test
    fun histogramKeepsSamplesWhenTechnologyStaysTheSame() {
        MonitorState.tickRsrpHistogramSample()
        MonitorState.updateStats(lteStats(-100))
        MonitorState.tickRsrpHistogramSample()

        assertEquals(2, MonitorState.rsrpHistory.value.size)
    }

    @Test
    fun histogramDoesNotClearWhenRadioTypeGoesBlank() {
        MonitorState.tickRsrpHistogramSample()
        MonitorState.updateStats(
            ConnectivityStats(
                isMonitoring = true,
                isPassiveOnlySession = true,
                rsrpDbm = null,
                radioAccessType = null
            )
        )
        MonitorState.tickRsrpHistogramSample()

        assertEquals(2, MonitorState.rsrpHistory.value.size)
        assertEquals(-95, MonitorState.rsrpHistory.value.first().rsrpDbm)
        assertEquals(null, MonitorState.rsrpHistory.value.last().rsrpDbm)
    }

    private fun lteStats(
        rsrpDbm: Int?,
        radioAccessType: String = CellularSignalReader.RADIO_4G
    ) = ConnectivityStats(
        isMonitoring = true,
        isPassiveOnlySession = true,
        rsrpDbm = rsrpDbm,
        radioAccessType = radioAccessType
    )
}
