package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlatlineConditionTest {

    @Test
    fun evaluateFlatlineCondition_2gFallbackWithGsmSignal_isNotFlatline() {
        val stats = ConnectivityStats(
            isOn2g = true,
            monitor2gFallbackEnabled = true,
            hasLteNrSignal = false,
            hasHomeGsmSignal = true,
            cellularAvailable = true,
            rsrpDbm = -85,
            radioAccessType = "2G",
            signalPermissionGranted = true
        )

        assertFalse(stats.evaluateFlatlineCondition())
    }

    @Test
    fun evaluateFlatlineCondition_2gFallbackWithoutGsmSignal_isFlatline() {
        val stats = ConnectivityStats(
            isOn2g = true,
            monitor2gFallbackEnabled = true,
            hasLteNrSignal = false,
            hasHomeGsmSignal = false,
            cellularAvailable = false,
            rsrpDbm = null,
            signalPermissionGranted = true
        )

        assertTrue(stats.evaluateFlatlineCondition())
    }

    @Test
    fun evaluateFlatlineCondition_2gWithoutFallback_isFlatline() {
        val stats = ConnectivityStats(
            isOn2g = true,
            monitor2gFallbackEnabled = false,
            hasHomeGsmSignal = true,
            rsrpDbm = -85,
            cellularAvailable = true
        )

        assertTrue(stats.evaluateFlatlineCondition())
    }
}
