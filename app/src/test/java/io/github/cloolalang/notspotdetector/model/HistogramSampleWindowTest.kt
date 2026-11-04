package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HistogramSampleWindowTest {

    @Test
    fun sampleWindowAndTapeScaleStartAtOneSecond() {
        assertEquals(1_000L, MonitoringSettings.MIN_RSRP_HISTOGRAM_WINDOW_MS)
        assertEquals(1_000L, MonitoringSettings.MIN_TAPE_WINDOW_MS)
        assertEquals(1_000L, 0L.coerceToHistogramWindowStep())
        assertEquals(1_000L, 1_500L.coerceToHistogramWindowStep())
        assertEquals(2_000L, 2_000L.coerceToHistogramWindowStep())
        assertEquals(300_000L, 300_000L.coerceToHistogramWindowStep())
        assertEquals(
            MonitoringSettings.MAX_RSRP_HISTOGRAM_WINDOW_MS,
            3_700_000L.coerceToHistogramWindowStep()
        )
        assertEquals(1_000L, 0L.coerceToTapeWindowStep())
        assertEquals(90_000L, 90_000L.coerceToTapeWindowStep())
        assertEquals(
            MonitoringSettings.MAX_TAPE_WINDOW_MS,
            8_000_000L.coerceToTapeWindowStep()
        )
    }
}
