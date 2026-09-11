package io.github.cloolalang.notspotdetector.data

import io.github.cloolalang.notspotdetector.model.AppSettingsSnapshot
import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.CellReselectBandNamingStyle
import io.github.cloolalang.notspotdetector.model.MockNetworkScenario
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.PingSettings
import io.github.cloolalang.notspotdetector.model.RsrpHistogramBinningMode
import io.github.cloolalang.notspotdetector.model.SettingsProfile
import io.github.cloolalang.notspotdetector.model.ThresholdSettings
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerChoice
import io.github.cloolalang.notspotdetector.model.VoicePhraseOptions
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsSnapshotCodecCompletenessTest {

    @Test
    fun encodedJsonIncludesEverySettableField() {
        val settingsJson = encodeSettingsJson(fullyCustomizedSnapshot().normalized())

        assertContainsAll(
            settingsJson.getJSONObject("thresholds"),
            THRESHOLD_KEYS,
            "thresholds"
        )
        assertContainsAll(settingsJson.getJSONObject("ping"), PING_KEYS, "ping")
        assertContainsAll(settingsJson.getJSONObject("monitoring"), MONITORING_KEYS, "monitoring")
        assertContainsAll(settingsJson.getJSONObject("passiveSignal"), PASSIVE_SIGNAL_KEYS, "passiveSignal")
        assertContainsAll(settingsJson.getJSONObject("passiveMock"), PASSIVE_MOCK_KEYS, "passiveMock")
        assertContainsAll(settingsJson.getJSONObject("audio"), AUDIO_KEYS, "audio")
    }

    @Test
    fun roundTripPreservesWifiCallingMockScenario() {
        val snapshot = fullyCustomizedSnapshot().copy(
            passiveMockSettings = PassiveMockSettings(
                enabled = true,
                scenario = MockNetworkScenario.WIFI_CALLING,
                rsrpDbm = -112,
                rsrqDb = -16
            )
        ).normalized()

        val encoded = AppSettingsSnapshotCodec.encodeProfile(
            SettingsProfile(id = "wifi-calling", name = "WiFi calling", savedAtMs = 1L, settings = snapshot)
        )
        val decoded = AppSettingsSnapshotCodec.decodeProfile(encoded)

        assertEquals(MockNetworkScenario.WIFI_CALLING, decoded?.settings?.passiveMockSettings?.scenario)
        assertEquals(snapshot, decoded?.settings)
    }

    @Test
    fun roundTripPreservesFullyCustomizedSnapshot() {
        val snapshot = fullyCustomizedSnapshot().normalized()
        val encoded = AppSettingsSnapshotCodec.encodeProfiles(
            listOf(
                SettingsProfile(
                    id = "full-coverage",
                    name = "Full coverage",
                    savedAtMs = 42L,
                    settings = snapshot
                )
            )
        )
        val decoded = AppSettingsSnapshotCodec.decodeProfiles(encoded).single().settings
        assertEquals(snapshot, decoded)
    }

    private fun encodeSettingsJson(snapshot: AppSettingsSnapshot): JSONObject {
        val encoded = AppSettingsSnapshotCodec.encodeProfile(
            SettingsProfile(
                id = "audit",
                name = "Audit",
                savedAtMs = 0L,
                settings = snapshot
            )
        )
        return JSONObject(encoded)
            .getJSONObject("profile")
            .getJSONObject("settings")
    }

    private fun assertContainsAll(json: JSONObject, expectedKeys: Set<String>, section: String) {
        val missing = expectedKeys.filterNot(json::has)
        assertTrue("$section missing codec keys: $missing", missing.isEmpty())
    }

    private fun fullyCustomizedSnapshot(): AppSettingsSnapshot {
        return AppSettingsSnapshot(
            thresholds = ThresholdSettings(
                goodRttMs = 25L,
                poorRttMs = 90L,
                poorJitterMs = 75L,
                poorPacketLossPercent = 7f,
                suppressClicksOnGoodConnection = true,
                goodConnectionClicksPerPing = 4
            ),
            pingSettings = PingSettings(
                host = "1.0.0.1",
                port = 53,
                pingsPerTest = 5,
                testIntervalMs = 20_000L
            ),
            monitoringSettings = MonitoringSettings(
                monitor2gFallback = false,
                subscriptionId = 3,
                passiveQuietUntilCritical = true,
                passiveMeasurementIntervalMs = 4_000L,
                rsrpHistogramWindowMs = 90_000L,
                rsrpHistogramBinningMode = RsrpHistogramBinningMode.THRESHOLD,
                rsrpHistogramThreshold1Dbm = -90,
                rsrpHistogramThreshold2Dbm = -100,
                rsrpHistogramThreshold3Dbm = -120
            ),
            passiveSignalSettings = PassiveSignalSettings(
                poorRsrpMinDbm = -118,
                fairRsrpMinDbm = -104,
                goodRsrpMinDbm = -99,
                mildRsrpMinDbm = -94,
                veryStrongRsrpMinDbm = -79,
                rsrqFairMinDb = -17,
                rsrqTierSoundEnabled = true,
                rsrqTierCoupledToSignalTier = false,
                rsrqTierWhiteNoiseVolume = 0.55f,
                rsrqTierClickIntervalMs = 900,
                rsrqTierPulseDurationMs = 180,
                quietAlertRsrqDb = -19,
                quietAlertRsrpMaxDbm = -104,
                criticalTierClickIntervalMs = 350,
                criticalTierPulseDurationMs = 170,
                mildTierPulseDurationMs = 160,
                goodTierPulseDurationMs = 165,
                fairTierPulseDurationMs = 175,
                poorTierPulseDurationMs = 185,
                levelRangeAbcdClickIntervalMs = 1_400,
                veryStrongTierClickIntervalMs = 1_300,
                veryStrongTierSoundEnabled = false,
                mildTierSoundEnabled = false,
                goodTierSoundEnabled = true,
                fairTierSoundEnabled = true,
                poorTierSoundEnabled = false,
                criticalTierSoundEnabled = true,
                g2StrongTierClickIntervalMs = 1_700,
                g2StrongTierPulseDurationMs = 210,
                g2WeakTierClickIntervalMs = 480,
                g2WeakTierPulseDurationMs = 130,
                g2StrongTierSoundEnabled = false,
                g2WeakTierSoundEnabled = true,
                g2NoSignalTierClickIntervalMs = 650,
                g2NoSignalTierSoundEnabled = false,
                g2NoSignalTierPulseDurationMs = 220,
                deadzoneTierClickIntervalMs = 330,
                deadzoneTierSoundEnabled = false,
                deadzoneTierPulseDurationMs = 190,
                noSignalTierClickIntervalMs = 640,
                noSignalTierSoundEnabled = false,
                noSignalTierPulseDurationMs = 230,
                searching2gTierClickIntervalMs = 620,
                searching2gTierSoundEnabled = false,
                searching2gTierPulseDurationMs = 240,
                limitedServiceTierClickIntervalMs = 880,
                limitedServiceTierSoundEnabled = false,
                limitedServiceTierPulseDurationMs = 260,
                limitedAlt2gTierClickIntervalMs = 610,
                limitedAlt2gTierSoundEnabled = false,
                limitedAlt2gTierPulseDurationMs = 270,
                wifiCallingTierClickIntervalMs = 590,
                wifiCallingTierSoundEnabled = false,
                wifiCallingTierPulseDurationMs = 280
            ),
            passiveMockSettings = PassiveMockSettings(
                enabled = true,
                scenario = MockNetworkScenario.ALT_OPERATOR_2G,
                rsrpDbm = -112,
                rsrqDb = -16
            ),
            audioVolumes = AudioVolumeSettings(
                masterVoiceAnnouncementsEnabled = false,
                pingClickVolume = 0.4f,
                lowSignalClickVolume = 0.55f,
                signalPulseFrequencyHz = 720,
                noSignalTierPulseFrequencyHz = 540,
                limitedServiceTierPulseFrequencyHz = 475,
                limitedServiceTwoToneSpreadPercent = 22,
                levelRangeBcdPulseFrequencyHz = 680,
                veryStrongTierPulseFrequencyHz = 860,
                g2StrongTierPulseFrequencyHz = 630,
                g2WeakTierPulseFrequencyHz = 510,
                signalPulseDurationMs = 310,
                levelRangeBcdPulseDurationMs = 290,
                levelRangeBcdClickVolume = 0.62f,
                cellChangeBellVolume = 0.45f,
                cellChangeVoiceEnabled = true,
                cellChangeVoiceVolume = 0.5f,
                cellChangeSpeakBandEnabled = true,
                cellChangeBandNamingStyle = CellReselectBandNamingStyle.MHZ_NICKNAME,
                technologyChangeTo2gToneVolume = 0.41f,
                technologyChangeTo2gVoiceEnabled = true,
                technologyChangeTo2gVoiceVolume = 0.42f,
                technologyChangeTo4gToneVolume = 0.43f,
                technologyChangeTo4gVoiceEnabled = true,
                technologyChangeTo4gVoiceVolume = 0.44f,
                technologyChangeTo5gEndcToneVolume = 0.46f,
                technologyChangeTo5gEndcVoiceEnabled = true,
                technologyChangeTo5gEndcVoiceVolume = 0.47f,
                tier5AnnouncerEnabled = true,
                tier5AnnouncerVolume = 0.48f,
                voiceAnnouncerChoice = VoiceAnnouncerChoice.SYSTEM_DEFAULT,
                voiceAnnouncerEngineId = "engine-test",
                noSignalToneVolume = 0.49f,
                noSignalVibrationEnabled = true,
                noSignalVoiceEnabled = true,
                noSignalVoiceVolume = 0.51f,
                limitedServiceToneVolume = 0.52f,
                limitedServiceVoiceEnabled = true,
                limitedServiceVoiceVolume = 0.53f,
                speakOperatorNameEnabled = false,
                speakTechnologyEnabled = false,
                cellChangePhrases = VoicePhraseOptions(speakOperatorName = false, speakTechnology = true, speakBand = true),
                technologyChangeTo2gPhrases = VoicePhraseOptions(speakOperatorName = true, speakTechnology = false, speakBand = true),
                technologyChangeTo4gPhrases = VoicePhraseOptions(speakOperatorName = false, speakTechnology = false, speakBand = false),
                technologyChangeTo5gEndcPhrases = VoicePhraseOptions(speakOperatorName = true, speakTechnology = true, speakBand = true),
                signalLowPhrases = VoicePhraseOptions(speakOperatorName = false, speakTechnology = true, speakBand = false),
                noSignalPhrases = VoicePhraseOptions(speakOperatorName = true, speakTechnology = false, speakBand = true),
                limitedServicePhrases = VoicePhraseOptions(speakOperatorName = false, speakTechnology = false, speakBand = true)
            )
        )
    }

    companion object {
        private val THRESHOLD_KEYS = setOf(
            "goodRttMs",
            "poorRttMs",
            "poorJitterMs",
            "poorPacketLossPercent",
            "suppressClicksOnGoodConnection",
            "goodConnectionClicksPerPing"
        )

        private val PING_KEYS = setOf(
            "host",
            "port",
            "pingsPerTest",
            "testIntervalMs"
        )

        private val MONITORING_KEYS = setOf(
            "monitor2gFallback",
            "subscriptionId",
            "passiveQuietUntilCritical",
            "passiveMeasurementIntervalMs",
            "rsrpHistogramWindowMs",
            "rsrpHistogramBinningMode",
            "rsrpHistogramThreshold1Dbm",
            "rsrpHistogramThreshold2Dbm",
            "rsrpHistogramThreshold3Dbm",
            "fiveGFeaturesEnabled"
        )

        private val PASSIVE_MOCK_KEYS = setOf(
            "enabled",
            "scenario",
            "rsrpDbm",
            "rsrqDb"
        )

        private val PASSIVE_SIGNAL_KEYS = setOf(
            "noSignalRsrpDbm",
            "poorRsrpMinDbm",
            "fairRsrpMinDbm",
            "goodRsrpMinDbm",
            "mildRsrpMinDbm",
            "veryStrongRsrpMinDbm",
            "rsrqFairMinDb",
            "rsrqTierSoundEnabled",
            "rsrqTierCoupledToSignalTier",
            "rsrqTierWhiteNoiseVolume",
            "rsrqTierClickIntervalMs",
            "rsrqTierPulseDurationMs",
            "quietAlertRsrqDb",
            "quietAlertRsrpMaxDbm",
            "criticalTierClickIntervalMs",
            "criticalTierPulseDurationMs",
            "mildTierPulseDurationMs",
            "goodTierPulseDurationMs",
            "fairTierPulseDurationMs",
            "poorTierPulseDurationMs",
            "levelRangeAbcdClickIntervalMs",
            "poorTierClickIntervalMs",
            "fairTierClickIntervalMs",
            "goodTierClickIntervalMs",
            "mildTierClickIntervalMs",
            "veryStrongTierClickIntervalMs",
            "veryStrongTierSoundEnabled",
            "mildTierSoundEnabled",
            "goodTierSoundEnabled",
            "fairTierSoundEnabled",
            "poorTierSoundEnabled",
            "criticalTierSoundEnabled",
            "g2StrongTierClickIntervalMs",
            "g2StrongTierPulseDurationMs",
            "g2WeakTierClickIntervalMs",
            "g2WeakTierPulseDurationMs",
            "g2StrongTierSoundEnabled",
            "g2WeakTierSoundEnabled",
            "g2NoSignalTierClickIntervalMs",
            "g2NoSignalTierSoundEnabled",
            "g2NoSignalTierPulseDurationMs",
            "deadzoneTierClickIntervalMs",
            "deadzoneTierSoundEnabled",
            "deadzoneTierPulseDurationMs",
            "noSignalTierClickIntervalMs",
            "noSignalTierSoundEnabled",
            "noSignalTierPulseDurationMs",
            "searching2gTierClickIntervalMs",
            "searching2gTierSoundEnabled",
            "searching2gTierPulseDurationMs",
            "limitedServiceTierClickIntervalMs",
            "limitedServiceTierSoundEnabled",
            "limitedServiceTierPulseDurationMs",
            "limitedAlt2gTierClickIntervalMs",
            "limitedAlt2gTierSoundEnabled",
            "limitedAlt2gTierPulseDurationMs",
            "wifiCallingTierClickIntervalMs",
            "wifiCallingTierSoundEnabled",
            "wifiCallingTierPulseDurationMs"
        )

        private val AUDIO_KEYS = setOf(
            "masterVoiceAnnouncementsEnabled",
            "pingClickVolume",
            "lowSignalClickVolume",
            "signalPulseFrequencyHz",
            "noSignalTierPulseFrequencyHz",
            "limitedServiceTierPulseFrequencyHz",
            "limitedServiceTwoToneSpreadPercent",
            "levelRangeBcdPulseFrequencyHz",
            "veryStrongTierPulseFrequencyHz",
            "g2StrongTierPulseFrequencyHz",
            "g2WeakTierPulseFrequencyHz",
            "signalPulseDurationMs",
            "levelRangeBcdPulseDurationMs",
            "levelRangeBcdClickVolume",
            "cellChangeBellVolume",
            "cellChangeVoiceEnabled",
            "cellChangeVoiceVolume",
            "cellChangeSpeakBandEnabled",
            "cellChangeBandNamingStyle",
            "technologyChangeTo2gToneVolume",
            "technologyChangeTo2gVoiceEnabled",
            "technologyChangeTo2gVoiceVolume",
            "technologyChangeTo4gToneVolume",
            "technologyChangeTo4gVoiceEnabled",
            "technologyChangeTo4gVoiceVolume",
            "technologyChangeTo5gEndcToneVolume",
            "technologyChangeTo5gEndcVoiceEnabled",
            "technologyChangeTo5gEndcVoiceVolume",
            "tier5AnnouncerEnabled",
            "tier5AnnouncerVolume",
            "voiceAnnouncerChoice",
            "voiceAnnouncerEngineId",
            "noSignalToneVolume",
            "noSignalVibrationEnabled",
            "noSignalVoiceEnabled",
            "noSignalVoiceVolume",
            "limitedServiceToneVolume",
            "limitedServiceVoiceEnabled",
            "limitedServiceVoiceVolume",
            "speakOperatorNameEnabled",
            "speakTechnologyEnabled",
            "cellChangeSpeakOperatorName",
            "cellChangeSpeakTechnology",
            "cellChangeSpeakBand",
            "technologyChangeTo2gSpeakOperatorName",
            "technologyChangeTo2gSpeakTechnology",
            "technologyChangeTo2gSpeakBand",
            "technologyChangeTo4gSpeakOperatorName",
            "technologyChangeTo4gSpeakTechnology",
            "technologyChangeTo4gSpeakBand",
            "technologyChangeTo5gEndcSpeakOperatorName",
            "technologyChangeTo5gEndcSpeakTechnology",
            "technologyChangeTo5gEndcSpeakBand",
            "signalLowSpeakOperatorName",
            "signalLowSpeakTechnology",
            "signalLowSpeakBand",
            "noSignalSpeakOperatorName",
            "noSignalSpeakTechnology",
            "noSignalSpeakBand",
            "limitedServiceSpeakOperatorName",
            "limitedServiceSpeakTechnology",
            "limitedServiceSpeakBand"
        )
    }
}
