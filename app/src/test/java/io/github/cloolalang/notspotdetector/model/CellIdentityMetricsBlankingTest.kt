package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CellIdentityMetricsBlankingTest {

    private val settings = PassiveSignalSettings()

    @Test
    fun shouldBlankStaleCellIdentity_falseDuringLimitedServiceEvenWithoutRsrp() {
        val stats = ConnectivityStats(
            isLimitedService = true,
            radioAccessType = CellularSignalReader.RADIO_4G,
            rsrpDbm = null,
            lteEarfcn = 6300,
            ltePci = 123
        )

        assertFalse(stats.shouldBlankStaleCellIdentity(settings))
    }

    @Test
    fun shouldBlankStaleCellIdentity_trueForHomeNoSignal() {
        val stats = ConnectivityStats(
            noSignalActive = true,
            radioAccessType = CellularSignalReader.RADIO_4G,
            lteEarfcn = 6300,
            ltePci = 123
        )

        assertTrue(stats.shouldBlankStaleCellIdentity(settings))
    }
}
