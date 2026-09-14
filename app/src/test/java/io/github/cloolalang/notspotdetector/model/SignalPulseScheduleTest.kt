package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SignalPulseScheduleTest {

    private val settings = PassiveSignalSettings()

    @Test
    fun rsrpBandChange_usesDistinctScheduleKeys() {
        val strong = lteStats(rsrpDbm = -70)
        val mid = lteStats(rsrpDbm = -90)
        val weak = lteStats(rsrpDbm = -128)

        val strongKey = strong.resolveSignalPulseScheduleKey(settings)
        val midKey = mid.resolveSignalPulseScheduleKey(settings)
        val weakKey = weak.resolveSignalPulseScheduleKey(settings)

        assertEquals(SignalPulsePath.RSRP_INTERVAL, strongKey.path)
        assertEquals(Rxss.SIGNAL_HIGH, strongKey.rxssNumber)
        assertEquals(Rxss.LEVEL_RANGE_A, midKey.rxssNumber)
        assertEquals(Rxss.SIGNAL_LOW, weakKey.rxssNumber)
        assertNotEquals(strongKey, midKey)
        assertNotEquals(midKey, weakKey)
    }

    @Test
    fun lowSignalFilterHold_keepsRxss5PulseKeyWhileRawIsRxss6() {
        val pending = lteStats(rsrpDbm = -128).copy(lowSignalActive = false)
        val confirmed = lteStats(rsrpDbm = -128).copy(lowSignalActive = true)

        assertEquals(Rxss.LEVEL_RANGE_D, pending.resolveSignalPulseScheduleKey(settings).rxssNumber)
        assertEquals(Rxss.SIGNAL_LOW, confirmed.resolveSignalPulseScheduleKey(settings).rxssNumber)
    }

    @Test
    fun lowSignalFilterHold_keepsRxss6PulseKeyWhileRawHasRecoveredToRxss5() {
        val pendingExit = lteStats(rsrpDbm = -124).copy(lowSignalActive = true)
        val confirmedExit = lteStats(rsrpDbm = -124).copy(lowSignalActive = false)

        assertEquals(Rxss.SIGNAL_LOW, pendingExit.resolveSignalPulseScheduleKey(settings).rxssNumber)
        assertEquals(Rxss.LEVEL_RANGE_D, confirmedExit.resolveSignalPulseScheduleKey(settings).rxssNumber)
    }

    @Test
    fun pendingNoSignal_keepsPreviousRsrpPulseKey() {
        val pendingFrom5 = lteStats(rsrpDbm = PassiveSignalSettings.MIN_RSRP_DBM).copy(
            cellularAvailable = false,
            heldInServiceSignalTier = SignalStrengthTier.POOR
        )
        val pendingFrom6 = lteStats(rsrpDbm = PassiveSignalSettings.MIN_RSRP_DBM).copy(
            cellularAvailable = false,
            heldInServiceSignalTier = SignalStrengthTier.CRITICAL
        )

        assertEquals(SignalPulsePath.RSRP_INTERVAL, pendingFrom5.resolveSignalPulseScheduleKey(settings).path)
        assertEquals(Rxss.LEVEL_RANGE_D, pendingFrom5.resolveSignalPulseScheduleKey(settings).rxssNumber)
        assertEquals(SignalPulsePath.RSRP_INTERVAL, pendingFrom6.resolveSignalPulseScheduleKey(settings).path)
        assertEquals(Rxss.SIGNAL_LOW, pendingFrom6.resolveSignalPulseScheduleKey(settings).rxssNumber)
    }

    @Test
    fun campChange_usesDistinctScheduleKeys() {
        val noSignal = ConnectivityStats(
            isMonitoring = true,
            noSignalActive = true,
            signalPermissionGranted = true,
            radioAccessType = CellularSignalReader.RADIO_4G
        )
        val deadzone = ConnectivityStats(
            isMonitoring = true,
            isCompleteNoService = true,
            signalPermissionGranted = true
        )

        val noSignalKey = noSignal.resolveSignalPulseScheduleKey(settings)
        val deadzoneKey = deadzone.resolveSignalPulseScheduleKey(settings)

        assertEquals(SignalPulsePath.NO_SIGNAL, noSignalKey.path)
        assertEquals(SignalPulsePath.DEADZONE, deadzoneKey.path)
        assertNotEquals(noSignalKey, deadzoneKey)
    }

    private fun lteStats(rsrpDbm: Int) = ConnectivityStats(
        isMonitoring = true,
        isPassiveOnlySession = true,
        cellularAvailable = true,
        signalPermissionGranted = true,
        rsrpDbm = rsrpDbm,
        rsrqDb = -10,
        radioAccessType = CellularSignalReader.RADIO_4G
    )
}
