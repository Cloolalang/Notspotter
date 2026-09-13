package io.github.cloolalang.notspotdetector.network

import io.github.cloolalang.notspotdetector.model.NetworkModePreference
import io.github.cloolalang.notspotdetector.model.parseNetworkModeFromAllowedBitmask
import io.github.cloolalang.notspotdetector.model.parsePreferredNetworkModeSetting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkModeControlTest {

    @Test
    fun inhibitBitmaskDrops2gAndKeepsLteNr() {
        val inhibit = NetworkModeControl.inhibit2gBitmask()
        val twoG = NetworkModeControl.twoGNetworkBitmask()

        assertEquals(0L, inhibit and twoG)
        assertEquals(
            NetworkModePreference.FORCED_LTE_NR,
            parseNetworkModeFromAllowedBitmask(inhibit)
        )
        assertEquals(
            NetworkModePreference.ALL_TECHNOLOGIES,
            parseNetworkModeFromAllowedBitmask(NetworkModeControl.allTechnologiesBitmask())
        )
    }

    @Test
    fun preferredModesMatchExistingParser() {
        assertEquals(
            NetworkModePreference.FORCED_LTE_NR,
            parsePreferredNetworkModeSetting(NetworkModeControl.preferredNetworkMode(true))
        )
        assertEquals(
            NetworkModePreference.ALL_TECHNOLOGIES,
            parsePreferredNetworkModeSetting(NetworkModeControl.preferredNetworkMode(false))
        )
    }

    @Test
    fun shellCommandsIncludeTelephonyAndSettingsWrites() {
        val commands = NetworkModeControl.shellCommands(inhibit2g = true, subscriptionId = 3)

        assertTrue(commands.any { it.startsWith("cmd phone set-allowed-network-types-for-reason") })
        assertTrue(commands.contains("settings put global preferred_network_mode 28"))
        assertTrue(commands.contains("settings put global preferred_network_mode3 28"))
        assertTrue(commands.contains("settings put global preferred_network_mode_3 28"))
        assertFalse(commands.any { it.contains(NetworkModeControl.twoGNetworkBitmask().toString()) })
    }
}
