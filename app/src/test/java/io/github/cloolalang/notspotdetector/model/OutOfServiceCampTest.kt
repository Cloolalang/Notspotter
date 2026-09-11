package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OutOfServiceCampTest {

    @Test
    fun reconcileOutOfServiceCamp_androidNoServiceIsAlwaysDeadzone() {
        val leftover = ConnectivityStats(
            isMonitoring = true,
            signalPermissionGranted = true,
            isLimitedService = true,
            networkServiceMode = NetworkServiceMode.OUT_OF_SERVICE,
            rsrpDbm = -110,
            radioAccessType = CellularSignalReader.RADIO_4G,
            lteEarfcn = 6_300,
            ltePci = 12,
            cellularAvailable = true
        )

        val first = leftover.reconcileOutOfServiceCamp()
        val second = leftover.reconcileOutOfServiceCamp()

        assertTrue(first.isCompleteNoService)
        assertFalse(first.isLimitedService)
        assertNull(first.rsrpDbm)
        assertEquals(SignalMeasurementTier.DEADZONE, first.resolveSignalMeasurementTier())
        assertEquals(SignalMeasurementTier.DEADZONE, second.resolveSignalMeasurementTier())
    }

    @Test
    fun reconcileOutOfServiceCamp_radioOffIsDeadzone() {
        val leftover = ConnectivityStats(
            isMonitoring = true,
            signalPermissionGranted = true,
            networkServiceMode = NetworkServiceMode.RADIO_OFF,
            rsrpDbm = -110,
            radioAccessType = CellularSignalReader.RADIO_4G,
            isWifiCallingActive = true
        )

        val reconciled = leftover.reconcileOutOfServiceCamp()

        assertTrue(reconciled.isCompleteNoService)
        assertFalse(reconciled.isWifiCallingActive)
        assertEquals(SignalMeasurementTier.DEADZONE, reconciled.resolveSignalMeasurementTier())
    }

    @Test
    fun reconcileOutOfServiceCamp_keepsLimitedServiceWhenAndroidReportsIt() {
        val limited = ConnectivityStats(
            isMonitoring = true,
            signalPermissionGranted = true,
            isLimitedService = true,
            networkServiceMode = NetworkServiceMode.LIMITED_SERVICE,
            rsrpDbm = -110,
            radioAccessType = CellularSignalReader.RADIO_4G,
            lteEarfcn = 6_300
        )

        val same = limited.reconcileOutOfServiceCamp()

        assertTrue(same.isLimitedService)
        assertEquals(-110, same.rsrpDbm)
        assertEquals(SignalMeasurementTier.LIMITED_SERVICE, same.resolveSignalMeasurementTier())
    }
}
