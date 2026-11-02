package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Test

class HistogramSampleWindowTest {

    @Test
    fun sampleWindowStartsAtTenSecondsAndKeepsExistingSteps() {
        assertEquals(10_000L, MonitoringSettings.MIN_RSRP_HISTOGRAM_WINDOW_MS)
        assertEquals(10_000L, 10_000L.coerceToHistogramWindowStep())
        assertEquals(10_000L, 5_000L.coerceToHistogramWindowStep())
        assertEquals(300_000L, 300_000L.coerceToHistogramWindowStep())
        assertEquals(
            MonitoringSettings.MAX_RSRP_HISTOGRAM_WINDOW_MS,
            3_700_000L.coerceToHistogramWindowStep()
        )
    }
}
