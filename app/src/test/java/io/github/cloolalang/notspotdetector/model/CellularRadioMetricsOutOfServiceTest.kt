package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CellularRadioMetricsOutOfServiceTest {

    @Test
    fun withoutCampedRadio_dropsStaleVisited4g() {
        val leftover = CellularRadioMetrics(
            rsrpDbm = -110,
            rsrqDb = -14,
            radioAccessType = CellularSignalReader.RADIO_4G,
            lteEarfcn = 6_300,
            ltePci = 12,
            isOn2g = false,
            isLimitedService = false,
            networkServiceMode = NetworkServiceMode.RADIO_OFF,
            hasLteNrSignal = true,
            servingNetworkOperatorName = "EE",
            plmn = "23430",
            homeNetworkOperatorName = "Vodafone UK",
            homePlmn = "23415",
            permissionGranted = true
        )

        val cleared = leftover.withoutCampedRadio()

        assertNull(cleared.rsrpDbm)
        assertNull(cleared.rsrqDb)
        assertNull(cleared.radioAccessType)
        assertNull(cleared.lteEarfcn)
        assertNull(cleared.ltePci)
        assertNull(cleared.servingNetworkOperatorName)
        assertNull(cleared.plmn)
        assertFalse(cleared.hasLteNrSignal)
        assertEquals("Vodafone UK", cleared.homeNetworkOperatorName)
        assertEquals("23415", cleared.homePlmn)
    }

    @Test
    fun resolveSignalMeasurementTier_radioOffAfterClearIsDeadzoneNotTier5() {
        val leftover = ConnectivityStats(
            isMonitoring = true,
            signalPermissionGranted = true,
            networkServiceMode = NetworkServiceMode.RADIO_OFF,
            rsrpDbm = -110,
            radioAccessType = CellularSignalReader.RADIO_4G,
            lteEarfcn = 6_300
        )
        val (cleared, _) = leftover.withStabilizedCellIdentity(
            CellIdentitySnapshot(lteEarfcn = 6_300, ltePci = 12)
        )
        val deadzone = cleared.copy(isCompleteNoService = true)

        assertNull(cleared.rsrpDbm)
        assertEquals(SignalMeasurementTier.DEADZONE, deadzone.resolveSignalMeasurementTier())
        assertTrue(leftover.shouldClearCellIdentity())
    }

    @Test
    fun shouldClearCellIdentity_falseForOutOfServiceOnceLimitedCampReturns() {
        val stats = ConnectivityStats(
            isMonitoring = true,
            signalPermissionGranted = true,
            isLimitedService = true,
            networkServiceMode = NetworkServiceMode.LIMITED_SERVICE,
            rsrpDbm = -110,
            radioAccessType = CellularSignalReader.RADIO_4G,
            lteEarfcn = 6_300
        )

        assertFalse(stats.shouldClearCellIdentity())
        assertEquals(
            SignalMeasurementTier.LIMITED_SERVICE,
            stats.resolveSignalMeasurementTier()
        )
    }
}
