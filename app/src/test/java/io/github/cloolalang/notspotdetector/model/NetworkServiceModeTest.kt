package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkServiceModeTest {

    @Test
    fun outOfServiceWithoutVoiceCampIsDeadzone() {
        assertEquals(
            NetworkServiceMode.OUT_OF_SERVICE,
            resolveNetworkServiceMode(
                serviceState = SERVICE_STATE_OUT_OF_SERVICE,
                circuitSwitchedRegistered = false,
                emergencyCamp = false
            )
        )
    }

    @Test
    fun outOfServiceWithEmergency2gCampIsLimited() {
        assertEquals(
            NetworkServiceMode.LIMITED_SERVICE,
            resolveNetworkServiceMode(
                serviceState = SERVICE_STATE_OUT_OF_SERVICE,
                circuitSwitchedRegistered = false,
                emergencyCamp = true
            )
        )
    }

    @Test
    fun outOfServiceWithHome2gVoiceIsInService() {
        assertEquals(
            NetworkServiceMode.IN_SERVICE,
            resolveNetworkServiceMode(
                serviceState = SERVICE_STATE_OUT_OF_SERVICE,
                circuitSwitchedRegistered = true,
                emergencyCamp = false
            )
        )
    }

    @Test
    fun leftoverPsLteDoesNotOverrideOutOfService() {
        assertEquals(
            NetworkServiceMode.OUT_OF_SERVICE,
            resolveNetworkServiceMode(
                serviceState = SERVICE_STATE_OUT_OF_SERVICE,
                circuitSwitchedRegistered = false,
                emergencyCamp = false
            )
        )
    }

    @Test
    fun powerOffStaysRadioOffEvenWithStaleEmergencyHint() {
        assertEquals(
            NetworkServiceMode.RADIO_OFF,
            resolveNetworkServiceMode(
                serviceState = SERVICE_STATE_POWER_OFF,
                circuitSwitchedRegistered = true,
                emergencyCamp = true
            )
        )
    }

    @Test
    fun voiceOnlyWhenCampedWithoutPacketData() {
        assertTrue(
            isVoiceOnlyNoData(
                serviceMode = NetworkServiceMode.LIMITED_SERVICE,
                packetSwitchedRegistered = false
            )
        )
        assertTrue(
            isVoiceOnlyNoData(
                serviceMode = NetworkServiceMode.IN_SERVICE,
                packetSwitchedRegistered = false
            )
        )
        assertFalse(
            isVoiceOnlyNoData(
                serviceMode = NetworkServiceMode.IN_SERVICE,
                packetSwitchedRegistered = true
            )
        )
        assertFalse(
            isVoiceOnlyNoData(
                serviceMode = NetworkServiceMode.OUT_OF_SERVICE,
                packetSwitchedRegistered = false
            )
        )
    }

    @Test
    fun emergencyOnlyStateIsLimited() {
        assertEquals(
            NetworkServiceMode.LIMITED_SERVICE,
            resolveNetworkServiceMode(
                serviceState = SERVICE_STATE_EMERGENCY_ONLY,
                circuitSwitchedRegistered = false,
                emergencyCamp = false
            )
        )
    }
}
