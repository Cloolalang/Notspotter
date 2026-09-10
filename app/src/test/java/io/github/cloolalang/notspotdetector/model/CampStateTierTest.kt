package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CampStateTierTest {

    private val settings = PassiveSignalSettings()

    @Test
    fun resolveSignalMeasurementTier_mapsCampStatesToTiers10Through13() {
        val noSignal = ConnectivityStats(
            isMonitoring = true,
            noSignalActive = true,
            signalPermissionGranted = true
        )
        assertEquals(NO_SIGNAL_TIER_NUMBER, noSignal.resolveSignalMeasurementTier(settings).displayNumber)

        val searching = noSignal.copy(searching2gFallbackActive = true)
        assertEquals(SEARCHING_2G_TIER_NUMBER, searching.resolveSignalMeasurementTier(settings).displayNumber)

        val limited = ConnectivityStats(
            isMonitoring = true,
            isLimitedService = true,
            signalPermissionGranted = true,
            cellularAvailable = true,
            rsrpDbm = -95,
            rsrqDb = -12,
            homeNetworkOperatorName = "Home",
            servingNetworkOperatorName = "Alt",
            homePlmn = "23415",
            plmn = "23430",
            radioAccessType = io.github.cloolalang.notspotdetector.network.CellularSignalReader.RADIO_4G
        )
        assertEquals(LIMITED_SERVICE_TIER_NUMBER, limited.resolveSignalMeasurementTier(settings).displayNumber)

        val alt2g = limited.copy(
            isOn2g = true,
            monitor2gFallbackEnabled = true,
            hasHomeGsmSignal = false,
            radioAccessType = io.github.cloolalang.notspotdetector.network.CellularSignalReader.RADIO_2G
        )
        assertEquals(LIMITED_ALT_2G_TIER_NUMBER, alt2g.resolveSignalMeasurementTier(settings).displayNumber)
    }

    @Test
    fun shouldPlayNoSignalVoiceAnnouncements_includesTier10CampState() {
        val stats = ConnectivityStats(
            isMonitoring = true,
            noSignalActive = true,
            signalPermissionGranted = true,
            radioAccessType = io.github.cloolalang.notspotdetector.network.CellularSignalReader.RADIO_4G
        )
        assertTrue(stats.shouldPlayNoSignalVoiceAnnouncements(settings))
        assertFalse(stats.shouldPlayFlatline(settings))
    }

    @Test
    fun home2gNoSignalUsesG2NoSignalCampTierNotTier10() {
        val stats = ConnectivityStats(
            isMonitoring = true,
            isOn2g = true,
            monitor2gFallbackEnabled = true,
            noSignalActive = true,
            signalPermissionGranted = true
        )
        assertFalse(stats.shouldPlayNoSignalCampTier(settings))
        assertTrue(stats.shouldPlayG2NoSignalCampTier(settings))
        assertFalse(stats.shouldPlayFlatline(settings))
        assertEquals(G2_NO_SIGNAL_TIER_NUMBER, stats.resolveSignalMeasurementTier(settings).displayNumber)
    }

    @Test
    fun shouldPlayNoSignalCampTier_suppressesLegacyFlatline() {
        val stats = ConnectivityStats(
            isMonitoring = true,
            noSignalActive = true,
            signalPermissionGranted = true
        )
        assertTrue(stats.shouldPlayNoSignalCampTier(settings))
        assertFalse(stats.shouldPlayFlatline(settings))
        assertTrue(stats.shouldPlayFlatline(settings.copy(noSignalTierSoundEnabled = false)))
    }

    @Test
    fun wifiCallingNoSignal_usesDedicatedCampTierNotTier10() {
        val stats = ConnectivityStats(
            isMonitoring = true,
            isWifiCallingActive = true,
            noSignalActive = true,
            signalPermissionGranted = true
        )
        assertFalse(stats.shouldPlayNoSignalCampTier(settings))
        assertTrue(stats.shouldPlayWifiCallingNoSignalCampTier(settings))
        assertFalse(stats.shouldPlayFlatline(settings))
        assertTrue(stats.shouldPlayNoSignalVoiceAnnouncements(settings))
        assertEquals(Rxss.WIFI_CALLING_NO_SIGNAL, stats.resolveSignalMeasurementTier(settings).displayNumber)
    }

    @Test
    fun wifiCallingNoSignal_soundDisabledFallsBackToFlatlineButKeepsVoice() {
        val stats = ConnectivityStats(
            isMonitoring = true,
            isWifiCallingActive = true,
            noSignalActive = true,
            signalPermissionGranted = true
        )
        val disabled = settings.copy(wifiCallingTierSoundEnabled = false)
        assertFalse(stats.shouldPlayWifiCallingNoSignalCampTier(disabled))
        assertTrue(stats.shouldPlayFlatline(disabled))
        assertTrue(stats.shouldPlayNoSignalVoiceAnnouncements(disabled))
    }

    @Test
    fun clickVolumeForTier_usesLimitedServiceToneVolumeForCampTiers() {
        val volumes = AudioVolumeSettings(
            lowSignalClickVolume = 0.3f,
            noSignalToneVolume = 0.55f,
            limitedServiceToneVolume = 0.65f
        ).normalized()

        assertEquals(0.65f, volumes.clickVolumeForTier(SignalStrengthTier.LIMITED_SERVICE))
        assertEquals(0.65f, volumes.clickVolumeForTier(SignalStrengthTier.LIMITED_ALT_2G))
        assertEquals(0.55f, volumes.clickVolumeForTier(SignalStrengthTier.DEADZONE))
    }

    @Test
    fun pulseFrequencyHzForTier_usesLimitedServiceTierSetting() {
        val volumes = AudioVolumeSettings(
            signalPulseFrequencyHz = 500,
            limitedServiceTierPulseFrequencyHz = 445
        )

        assertEquals(445, volumes.pulseFrequencyHzForTier(SignalStrengthTier.LIMITED_SERVICE))
        assertEquals(445, volumes.pulseFrequencyHzForTier(SignalStrengthTier.LIMITED_ALT_2G))
        assertEquals(500, volumes.pulseFrequencyHzForTier(SignalStrengthTier.CRITICAL))
    }

    @Test
    fun limitedServiceWithSignalOverlay_prefersOverlayPulseOverCampTier() {
        val stats = ConnectivityStats(
            isMonitoring = true,
            isLimitedService = true,
            signalPermissionGranted = true,
            cellularAvailable = true,
            rsrpDbm = -95,
            rsrqDb = -12,
            radioAccessType = io.github.cloolalang.notspotdetector.network.CellularSignalReader.RADIO_4G,
            homeNetworkOperatorName = "Vodafone",
            servingNetworkOperatorName = "EE",
            homePlmn = "23415",
            plmn = "23430"
        )
        assertTrue(stats.shouldPlayLimitedServiceSignalOverlay(settings))
        assertFalse(stats.shouldPlayLimitedServiceCampTier(settings))
        assertFalse(stats.shouldPlayLimitedServiceTone())
    }
}
