package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TechnologyHistoryTest {

    @Test
    fun record_keepsLastFourAndIgnoresARepeatOrABlank() {
        var tape = TechnologyTape()
        tape = TechnologyHistory.record(tape, null)
        assertEquals(emptyList<String>(), tape.entries.map { it.label })

        listOf(
            CellularSignalReader.RADIO_4G,
            CellularSignalReader.RADIO_4G,
            CellularSignalReader.RADIO_5G_ENDC,
            CellularSignalReader.RADIO_5G,
            CellularSignalReader.RADIO_2G,
            CellularSignalReader.RADIO_4G
        ).forEach { tape = TechnologyHistory.record(tape, it) }

        assertEquals(
            listOf(
                CellularSignalReader.RADIO_5G_ENDC,
                CellularSignalReader.RADIO_5G,
                CellularSignalReader.RADIO_2G,
                CellularSignalReader.RADIO_4G
            ),
            tape.entries.map { it.label }
        )
        val held = TechnologyHistory.record(tape, null)
        assertEquals(tape, held)
    }

    @Test
    fun technologyFromStats_usesTheRadioType() {
        assertEquals(
            CellularSignalReader.RADIO_2G,
            TechnologyHistory.technologyFromStats(
                ConnectivityStats(radioAccessType = CellularSignalReader.RADIO_2G)
            )
        )
        assertNull(TechnologyHistory.technologyFromStats(ConnectivityStats()))
        assertEquals(
            TechnologyHistory.LABEL_4G_PLUS,
            TechnologyHistory.technologyFromStats(
                ConnectivityStats(
                    radioAccessType = CellularSignalReader.RADIO_4G,
                    ltePlusIcon = true
                )
            )
        )
        assertEquals(
            CellularSignalReader.RADIO_4G,
            TechnologyHistory.technologyFromStats(
                ConnectivityStats(radioAccessType = CellularSignalReader.RADIO_4G)
            )
        )
    }

    @Test
    fun noSignalWhileMonitoringIsAGreySegmentBetweenTechnologies() {
        assertEquals(
            TechnologyHistory.LABEL_NO_SIGNAL,
            TechnologyHistory.technologyFromStats(
                ConnectivityStats(
                    isMonitoring = true,
                    noSignalActive = true,
                    radioAccessType = CellularSignalReader.RADIO_4G
                )
            )
        )
        assertTrue(
            TechnologyHistory.isNoSignalPeriod(
                ConnectivityStats(isMonitoring = true, isCompleteNoService = true)
            )
        )
        var tape = TechnologyTape()
        tape = TechnologyHistory.record(tape, CellularSignalReader.RADIO_4G, nowMs = 1_000L)
        tape = TechnologyHistory.record(tape, TechnologyHistory.LABEL_NO_SIGNAL, nowMs = 5_000L)
        tape = TechnologyHistory.record(tape, CellularSignalReader.RADIO_2G, nowMs = 9_000L)
        assertEquals(
            listOf(
                CellularSignalReader.RADIO_4G,
                TechnologyHistory.LABEL_NO_SIGNAL,
                CellularSignalReader.RADIO_2G
            ),
            tape.entries.map { it.label }
        )
        assertEquals(5_000L, tape.entries[1].startedAtMs)
    }

    @Test
    fun tapeAgeUsesSecondsThenMinutesThenHours() {
        assertEquals(TapeAgeAmount(45, TapeAgeUnit.SECONDS), TapeAge.amount(0L, 45_000L))
        assertEquals(TapeAgeAmount(2, TapeAgeUnit.MINUTES), TapeAge.amount(0L, 120_000L))
        assertEquals(TapeAgeAmount(3, TapeAgeUnit.HOURS), TapeAge.amount(0L, 3 * 3_600_000L))
    }

    @Test
    fun segmentDuration_holdsStillAtZeroAndFollowsTheChangeRate() {
        assertNull(TechnologyHistory.segmentDurationMs(0))
        assertEquals(60_000L, TechnologyHistory.segmentDurationMs(1))
        assertEquals(15_000L, TechnologyHistory.segmentDurationMs(4))
    }
}
