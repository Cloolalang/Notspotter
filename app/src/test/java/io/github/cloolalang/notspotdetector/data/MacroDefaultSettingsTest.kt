package io.github.cloolalang.notspotdetector.data

import io.github.cloolalang.notspotdetector.model.CellReselectBandNamingStyle
import io.github.cloolalang.notspotdetector.model.RsrpHistogramBinningMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MacroDefaultSettingsTest {

    @Test
    fun bundledMacroProfileDecodes() {
        val file = listOf(
            File("src/main/assets/macro-defaults.json"),
            File("app/src/main/assets/macro-defaults.json")
        ).first { it.exists() }
        val profile = AppSettingsSnapshotCodec.decodeProfile(file.readText())
        requireNotNull(profile)
        assertEquals("Macro", profile.name)

        val settings = profile.settings
        assertFalse(settings.isDefault())
        assertEquals(40L, settings.thresholds.goodRttMs)
        assertEquals(80L, settings.thresholds.poorRttMs)
        assertEquals(60L, settings.thresholds.poorJitterMs)
        assertEquals(5f, settings.thresholds.poorPacketLossPercent)
        assertEquals("8.8.8.8", settings.pingSettings.host)
        assertEquals(443, settings.pingSettings.port)
        assertEquals(2, settings.pingSettings.pingsPerTest)
        assertEquals(10_000L, settings.pingSettings.testIntervalMs)
        assertTrue(settings.monitoringSettings.monitor2gFallback)
        assertTrue(settings.monitoringSettings.fiveGFeaturesEnabled)
        assertEquals(300_000L, settings.monitoringSettings.rsrpHistogramWindowMs)
        assertEquals(
            RsrpHistogramBinningMode.THRESHOLD,
            settings.monitoringSettings.rsrpHistogramBinningMode
        )
        assertEquals(-130, settings.passiveSignalSettings.noSignalRsrpDbm)
        assertEquals(-123, settings.passiveSignalSettings.poorRsrpMinDbm)
        assertEquals(12_470, settings.passiveSignalSettings.noSignalTierClickIntervalMs)
        assertEquals(1_530, settings.passiveSignalSettings.deadzoneTierPulseDurationMs)
        assertFalse(settings.passiveSignalSettings.searching2gTierSoundEnabled)
        assertEquals(4_140, settings.audioVolumes.veryStrongTierPulseFrequencyHz)
        assertEquals(910, settings.audioVolumes.noSignalTierPulseFrequencyHz)
        assertEquals(
            CellReselectBandNamingStyle.MHZ_NICKNAME,
            settings.audioVolumes.cellChangeBandNamingStyle
        )
        assertTrue(settings.audioVolumes.masterVoiceAnnouncementsEnabled)
        assertFalse(settings.audioVolumes.technologyChangeTo4gPhrases.speakTechnology)
        assertFalse(settings.audioVolumes.limitedServicePhrases.speakTechnology)
    }
}
