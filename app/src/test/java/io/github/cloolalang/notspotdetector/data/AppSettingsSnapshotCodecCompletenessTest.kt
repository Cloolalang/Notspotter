package io.github.cloolalang.notspotdetector.data

import io.github.cloolalang.notspotdetector.model.AppSettingsSnapshot
import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.CellReselectBandNamingStyle
import io.github.cloolalang.notspotdetector.model.MockNetworkScenario
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.PingSettings
import io.github.cloolalang.notspotdetector.model.RxssStateFilterSettings
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

        assertExactKeys(settingsJson.getJSONObject("thresholds"), THRESHOLD_KEYS, "thresholds")
        assertExactKeys(settingsJson.getJSONObject("ping"), PING_KEYS, "ping")
        assertExactKeys(settingsJson.getJSONObject("monitoring"), MONITORING_KEYS, "monitoring")
        assertExactKeys(settingsJson.getJSONObject("passiveSignal"), PASSIVE_SIGNAL_KEYS, "passiveSignal")
        assertExactKeys(settingsJson.getJSONObject("passiveMock"), PASSIVE_MOCK_KEYS, "passiveMock")
        assertExactKeys(settingsJson.getJSONObject("audio"), AUDIO_KEYS, "audio")

        assertEquals(persistedPropertyNames(ThresholdSettings::class.java), THRESHOLD_KEYS)
        assertEquals(persistedPropertyNames(PingSettings::class.java), PING_KEYS)
        assertEquals(persistedPropertyNames(MonitoringSettings::class.java), MONITORING_KEYS)
        assertEquals(persistedPropertyNames(PassiveSignalSettings::class.java), PASSIVE_SIGNAL_KEYS)
        assertEquals(persistedPropertyNames(PassiveMockSettings::class.java), PASSIVE_MOCK_KEYS)
        assertEquals(audioJsonKeysFromModel(), AUDIO_KEYS)
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
        assertEquals(-97, decoded.passiveSignalSettings.g2WeakMaxDbm)
        assertEquals(-128, decoded.passiveSignalSettings.g2NoSignalRsrpDbm)
        assertEquals(-60, decoded.passiveSignalSettings.veryStrongRsrpMinDbm)
        assertEquals(-127, decoded.passiveSignalSettings.poorRsrpMinDbm)
        assertEquals(-140, decoded.passiveMockSettings.rsrpDbm)
    }

    @Test
    fun fullyCustomizedSnapshotExercisesEveryAdjustableField() {
        val defaults = AppSettingsSnapshot.defaults()
        val custom = fullyCustomizedSnapshot().normalized()
        val leftover = matchingDefaultFields(custom.thresholds, defaults.thresholds) +
            matchingDefaultFields(custom.pingSettings, defaults.pingSettings) +
            matchingDefaultFields(custom.monitoringSettings, defaults.monitoringSettings) +
            matchingDefaultFields(custom.passiveSignalSettings, defaults.passiveSignalSettings) +
            matchingDefaultFields(custom.passiveMockSettings, defaults.passiveMockSettings) +
            matchingDefaultFields(custom.audioVolumes, defaults.audioVolumes)
        val allowedSameAsDefault = setOf(
            "fairRsrpMinDbm",
            "goodRsrpMinDbm",
            "mildRsrpMinDbm",
            "passiveMeasurementIntervalMs",
            "passiveQuietUntilCritical"
        )
        val unexpected = leftover - allowedSameAsDefault
        assertEquals(emptySet<String>(), unexpected)
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

    private fun assertExactKeys(json: JSONObject, expectedKeys: Set<String>, section: String) {
        val actual = json.keys().asSequence().toSet()
        val missing = expectedKeys - actual
        val extra = actual - expectedKeys
        assertTrue("$section missing codec keys: $missing extra: $extra", missing.isEmpty() && extra.isEmpty())
    }

    private fun persistedPropertyNames(type: Class<*>): Set<String> {
        return type.declaredFields
            .asSequence()
            .filter { field ->
                !java.lang.reflect.Modifier.isStatic(field.modifiers) &&
                    !field.isSynthetic &&
                    field.name != "Companion" &&
                    !field.name.startsWith("$")
            }
            .map { it.name }
            .toSet()
    }

    private fun matchingDefaultFields(custom: Any, defaults: Any): Set<String> {
        return persistedPropertyNames(custom.javaClass)
            .filter { name ->
                val field = custom.javaClass.getDeclaredField(name)
                field.isAccessible = true
                field.get(custom) == field.get(defaults)
            }
            .toSet()
    }

    private fun audioJsonKeysFromModel(): Set<String> {
        val phraseObjects = setOf(
            "cellChangePhrases",
            "technologyChangeTo2gPhrases",
            "technologyChangeTo4gPhrases",
            "technologyChangeTo5gEndcPhrases",
            "signalLowPhrases",
            "noSignalPhrases",
            "limitedServicePhrases"
        )
        val scalarKeys = persistedPropertyNames(AudioVolumeSettings::class.java) - phraseObjects
        return scalarKeys + setOf(
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
            "limitedServiceSpeakBand",
            "limitedServiceSpeakHomeLimitedService",
            "limitedServiceSpeakVisitingLimitedService"
        )
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
                rsrpHistogramBinningMode = RsrpHistogramBinningMode.LEVEL,
                rsrpHistogramThreshold1Dbm = -90,
                rsrpHistogramThreshold2Dbm = -100,
                rsrpHistogramThreshold3Dbm = -120,
                fiveGFeaturesEnabled = true,
                showManualSelectOperatorButton = true,
                inhibit2g = true,
                keepScreenOnWhileMonitoring = false
            ),
            passiveSignalSettings = PassiveSignalSettings(
                noSignalRsrpDbm = -130,
                poorRsrpMinDbm = -127,
                fairRsrpMinDbm = -104,
                goodRsrpMinDbm = -99,
                mildRsrpMinDbm = -94,
                veryStrongRsrpMinDbm = -60,
                rsrqFairMinDb = -17,
                rsrqTierSoundEnabled = false,
                rsrqTierCoupledToSignalTier = true,
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
                poorTierClickIntervalMs = 910,
                fairTierClickIntervalMs = 1_120,
                goodTierClickIntervalMs = 1_330,
                mildTierClickIntervalMs = 1_540,
                veryStrongTierClickIntervalMs = 1_300,
                veryStrongTierSoundEnabled = false,
                mildTierSoundEnabled = false,
                goodTierSoundEnabled = false,
                fairTierSoundEnabled = false,
                poorTierSoundEnabled = false,
                criticalTierSoundEnabled = false,
                g2StrongTierClickIntervalMs = 1_700,
                g2StrongTierPulseDurationMs = 210,
                g2WeakTierClickIntervalMs = 480,
                g2WeakTierPulseDurationMs = 130,
                g2StrongTierSoundEnabled = false,
                g2WeakTierSoundEnabled = false,
                g2WeakMaxDbm = -97,
                g2NoSignalRsrpDbm = -128,
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
                searching2gTierSoundEnabled = true,
                searching2gTierPulseDurationMs = 240,
                limitedServiceTierClickIntervalMs = 880,
                limitedServiceTierSoundEnabled = false,
                limitedServiceTierPulseDurationMs = 260,
                limitedAlt2gTierClickIntervalMs = 610,
                limitedAlt2gTierSoundEnabled = false,
                limitedAlt2gTierPulseDurationMs = 270,
                wifiCallingTierClickIntervalMs = 590,
                wifiCallingTierSoundEnabled = false,
                wifiCallingTierPulseDurationMs = 280,
                lowSignalFilter = RxssStateFilterSettings(enabled = true, windowSeconds = 3, balancePercent = 70),
                noSignalFilter = RxssStateFilterSettings(enabled = false, windowSeconds = 4, balancePercent = 30),
                deadzoneFilter = RxssStateFilterSettings(enabled = true, windowSeconds = 5, balancePercent = 80),
                rsrqFilter = RxssStateFilterSettings(enabled = true, windowSeconds = 6, balancePercent = 20)
            ),
            passiveMockSettings = PassiveMockSettings(
                enabled = true,
                scenario = MockNetworkScenario.ALT_OPERATOR_2G,
                rsrpDbm = -140,
                rsrqDb = -16
            ),
            audioVolumes = AudioVolumeSettings(
                masterVoiceAnnouncementsEnabled = false,
                pingClickVolume = 0.4f,
                lowSignalClickVolume = 0.55f,
                signalPulseFrequencyHz = 810,
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
                cellChangeVoiceEnabled = false,
                cellChangeVoiceVolume = 0.35f,
                cellChangeSpeakBandEnabled = false,
                cellChangeBandNamingStyle = CellReselectBandNamingStyle.BAND_NUMBER,
                technologyChangeTo2gSoundEnabled = false,
                technologyChangeTo2gToneVolume = 0.41f,
                technologyChangeTo2gVoiceEnabled = false,
                technologyChangeTo2gPeriodicVoiceEnabled = false,
                technologyChangeTo2gVoiceVolume = 0.42f,
                technologyChangeTo4gSoundEnabled = false,
                technologyChangeTo4gToneVolume = 0.43f,
                technologyChangeTo4gVoiceEnabled = false,
                technologyChangeTo4gVoiceVolume = 0.44f,
                technologyChangeTo5gEndcSoundEnabled = false,
                technologyChangeTo5gEndcToneVolume = 0.46f,
                technologyChangeTo5gEndcVoiceEnabled = false,
                technologyChangeTo5gEndcVoiceVolume = 0.47f,
                tier5AnnouncerEnabled = false,
                tier5PeriodicVoiceEnabled = false,
                tier5AnnouncerVolume = 0.48f,
                voiceAnnouncerChoice = VoiceAnnouncerChoice.MALE_1,
                voiceAnnouncerEngineId = "engine-test",
                voiceSpeechRate = 1.4f,
                noSignalToneVolume = 0.49f,
                noSignalVibrationEnabled = false,
                noSignalVoiceEnabled = false,
                noSignalPeriodicVoiceEnabled = false,
                noSignalVoiceVolume = 0.51f,
                limitedServiceToneVolume = 0.52f,
                limitedServiceVoiceEnabled = false,
                limitedServicePeriodicVoiceEnabled = false,
                limitedServiceVoiceVolume = 0.53f,
                speakOperatorNameEnabled = true,
                speakTechnologyEnabled = true,
                cellChangePhrases = VoicePhraseOptions(speakOperatorName = false, speakTechnology = true, speakBand = true),
                technologyChangeTo2gPhrases = VoicePhraseOptions(speakOperatorName = true, speakTechnology = false, speakBand = true),
                technologyChangeTo4gPhrases = VoicePhraseOptions(speakOperatorName = false, speakTechnology = false, speakBand = false),
                technologyChangeTo5gEndcPhrases = VoicePhraseOptions(speakOperatorName = true, speakTechnology = true, speakBand = true),
                signalLowPhrases = VoicePhraseOptions(speakOperatorName = true, speakTechnology = false, speakBand = true),
                noSignalPhrases = VoicePhraseOptions(speakOperatorName = true, speakTechnology = false, speakBand = true),
                limitedServicePhrases = VoicePhraseOptions(
                    speakOperatorName = false,
                    speakTechnology = false,
                    speakBand = true,
                    speakHomeLimitedService = false,
                    speakVisitingLimitedService = false
                ),
                specialCellsDetectionEnabled = false,
                specialCellsVoiceEnabled = false,
                specialCellsSpeakType = false,
                specialCellsSpeakSite = false,
                specialCellsSpeakSector = false
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
            "fiveGFeaturesEnabled",
            "showManualSelectOperatorButton",
            "inhibit2g",
            "keepScreenOnWhileMonitoring"
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
            "g2WeakMaxDbm",
            "g2NoSignalRsrpDbm",
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
            "wifiCallingTierPulseDurationMs",
            "lowSignalFilter",
            "noSignalFilter",
            "deadzoneFilter",
            "rsrqFilter"
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
            "technologyChangeTo2gSoundEnabled",
            "technologyChangeTo2gToneVolume",
            "technologyChangeTo2gVoiceEnabled",
            "technologyChangeTo2gPeriodicVoiceEnabled",
            "technologyChangeTo2gVoiceVolume",
            "technologyChangeTo4gSoundEnabled",
            "technologyChangeTo4gToneVolume",
            "technologyChangeTo4gVoiceEnabled",
            "technologyChangeTo4gVoiceVolume",
            "technologyChangeTo5gEndcSoundEnabled",
            "technologyChangeTo5gEndcToneVolume",
            "technologyChangeTo5gEndcVoiceEnabled",
            "technologyChangeTo5gEndcVoiceVolume",
            "tier5AnnouncerEnabled",
            "tier5PeriodicVoiceEnabled",
            "tier5AnnouncerVolume",
            "voiceAnnouncerChoice",
            "voiceAnnouncerEngineId",
            "voiceSpeechRate",
            "noSignalToneVolume",
            "noSignalVibrationEnabled",
            "noSignalVoiceEnabled",
            "noSignalPeriodicVoiceEnabled",
            "noSignalVoiceVolume",
            "limitedServiceToneVolume",
            "limitedServiceVoiceEnabled",
            "limitedServicePeriodicVoiceEnabled",
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
            "limitedServiceSpeakBand",
            "limitedServiceSpeakHomeLimitedService",
            "limitedServiceSpeakVisitingLimitedService",
            "specialCellsDetectionEnabled",
            "specialCellsVoiceEnabled",
            "specialCellsSpeakType",
            "specialCellsSpeakSite",
            "specialCellsSpeakSector"
        )
    }
}
