package io.github.cloolalang.notspotdetector.model

import android.telephony.TelephonyManager
import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkModePreferenceTest {

    private val twoGMask = (
        TelephonyManager.NETWORK_TYPE_BITMASK_GSM or
            TelephonyManager.NETWORK_TYPE_BITMASK_GPRS or
            TelephonyManager.NETWORK_TYPE_BITMASK_EDGE
        ).toLong()
    private val lteMask = (
        TelephonyManager.NETWORK_TYPE_BITMASK_LTE or
            TelephonyManager.NETWORK_TYPE_BITMASK_LTE_CA
        ).toLong()
    private val nrMask = TelephonyManager.NETWORK_TYPE_BITMASK_NR.toLong()

    @Test
    fun parseNetworkMode_zeroAllowed_isAllTechnologies() {
        assertEquals(NetworkModePreference.ALL_TECHNOLOGIES, parseNetworkModeFromAllowedBitmask(0L))
    }

    @Test
    fun parseNetworkMode_twoGOnly_isForced2g() {
        assertEquals(NetworkModePreference.FORCED_2G, parseNetworkModeFromAllowedBitmask(twoGMask))
    }

    @Test
    fun parseNetworkMode_lteOnly_isForcedLteNr() {
        assertEquals(NetworkModePreference.FORCED_LTE_NR, parseNetworkModeFromAllowedBitmask(lteMask))
    }

    @Test
    fun parseNetworkMode_nrOnly_isForcedNrOnly() {
        assertEquals(NetworkModePreference.FORCED_NR_ONLY, parseNetworkModeFromAllowedBitmask(nrMask))
    }

    @Test
    fun parseNetworkMode_lteAndNrWithout2g_isForcedLteNr() {
        assertEquals(
            NetworkModePreference.FORCED_LTE_NR,
            parseNetworkModeFromAllowedBitmask(lteMask or nrMask)
        )
    }

    @Test
    fun parsePreferredNetworkMode_gsmOnly_isForced2g() {
        assertEquals(NetworkModePreference.FORCED_2G, parsePreferredNetworkModeSetting(1))
    }

    @Test
    fun parsePreferredNetworkMode_lteOnly_isForcedLteNr() {
        assertEquals(NetworkModePreference.FORCED_LTE_NR, parsePreferredNetworkModeSetting(11))
    }

    @Test
    fun parsePreferredNetworkMode_nrOnly_isForcedNrOnly() {
        assertEquals(NetworkModePreference.FORCED_NR_ONLY, parsePreferredNetworkModeSetting(23))
    }

    @Test
    fun parsePreferredNetworkMode_nrLteGsm_isAllTechnologies() {
        assertEquals(NetworkModePreference.ALL_TECHNOLOGIES, parsePreferredNetworkModeSetting(26))
    }

    @Test
    fun parseNetworkMode_allBits_isAllTechnologies() {
        assertEquals(
            NetworkModePreference.ALL_TECHNOLOGIES,
            parseNetworkModeFromAllowedBitmask(twoGMask or lteMask or nrMask)
        )
    }

    @Test
    fun forcedLteNr_doesNotAllow2gFallbackScan() {
        assertFalse(NetworkModePreference.FORCED_LTE_NR.allows2gFallbackScan())
        assertFalse(NetworkModePreference.FORCED_NR_ONLY.allows2gFallbackScan())
        assertTrue(NetworkModePreference.ALL_TECHNOLOGIES.allows2gFallbackScan())
        assertTrue(NetworkModePreference.FORCED_2G.allows2gFallbackScan())
    }

    @Test
    fun computeSearching2gFallbackActive_requiresLteEpisodeAnd2gAllowed() {
        val base = ConnectivityStats(
            isMonitoring = true,
            noSignalActive = true,
            monitor2gFallbackEnabled = true,
            networkModePreference = NetworkModePreference.ALL_TECHNOLOGIES
        )

        assertTrue(
            computeSearching2gFallbackActive(base, CellularSignalReader.RADIO_4G)
        )
        assertFalse(
            computeSearching2gFallbackActive(
                base.copy(networkModePreference = NetworkModePreference.FORCED_LTE_NR),
                CellularSignalReader.RADIO_4G
            )
        )
        assertFalse(
            computeSearching2gFallbackActive(base, CellularSignalReader.RADIO_2G)
        )
        assertFalse(
            computeSearching2gFallbackActive(base.copy(isLimitedService = true), CellularSignalReader.RADIO_4G)
        )
    }

    @Test
    fun computeSearching2gFallbackActive_falseWhenWifiCallingActive() {
        val base = ConnectivityStats(
            isMonitoring = true,
            noSignalActive = true,
            monitor2gFallbackEnabled = true,
            networkModePreference = NetworkModePreference.ALL_TECHNOLOGIES
        )

        // Without WiFi calling, the conditions are satisfied (regression guard for the false
        // positive fixed by adding the isWifiCallingActive gate below).
        assertTrue(computeSearching2gFallbackActive(base, CellularSignalReader.RADIO_4G))

        assertFalse(
            computeSearching2gFallbackActive(
                base.copy(isWifiCallingActive = true),
                CellularSignalReader.RADIO_4G
            )
        )
    }
}
