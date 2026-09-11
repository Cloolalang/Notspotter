package io.github.cloolalang.notspotdetector.data

import io.github.cloolalang.notspotdetector.model.AppSettingsSnapshot
import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.PingSettings
import io.github.cloolalang.notspotdetector.model.RsrpHistogramBinningMode
import io.github.cloolalang.notspotdetector.model.SettingsProfile
import io.github.cloolalang.notspotdetector.model.ThresholdSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsSnapshotCodecTest {

    @Test
    fun singleProfileRoundTripUsesShareableWrapper() {
        val profile = SettingsProfile(
            id = "share-profile",
            name = "Shared preset",
            savedAtMs = 9_876L,
            settings = AppSettingsSnapshot(
                pingSettings = PingSettings(host = "8.8.8.8")
            ).normalized()
        )

        val encoded = AppSettingsSnapshotCodec.encodeProfile(profile)
        val decoded = AppSettingsSnapshotCodec.decodeProfile(encoded)

        requireNotNull(decoded)
        assertEquals(profile.id, decoded.id)
        assertEquals(profile.name, decoded.name)
        assertEquals(profile.settings, decoded.settings)
    }

    @Test
    fun roundTripPreservesRecentlyAddedSettings() {
        val snapshot = AppSettingsSnapshot(
            thresholds = ThresholdSettings(goodConnectionClicksPerPing = 3),
            pingSettings = PingSettings(host = "1.1.1.1", pingsPerTest = 4),
            monitoringSettings = MonitoringSettings(
                passiveMeasurementIntervalMs = 3_000L,
                rsrpHistogramWindowMs = 120_000L,
                rsrpHistogramBinningMode = RsrpHistogramBinningMode.THRESHOLD,
                rsrpHistogramThreshold1Dbm = -88,
                rsrpHistogramThreshold2Dbm = -102,
                rsrpHistogramThreshold3Dbm = -118
            ),
            passiveSignalSettings = PassiveSignalSettings(
                criticalTierClickIntervalMs = 400,
                poorTierClickIntervalMs = 600,
                fairTierClickIntervalMs = 1_300,
                goodTierClickIntervalMs = 2_100,
                mildTierClickIntervalMs = 2_600,
                mildRsrpMinDbm = -92,
                veryStrongRsrpMinDbm = -78,
                veryStrongTierClickIntervalMs = 1_400,
                g2StrongTierClickIntervalMs = 1_800,
                g2StrongTierPulseDurationMs = 200,
                g2WeakTierClickIntervalMs = 450,
                g2WeakTierPulseDurationMs = 120,
                g2StrongTierSoundEnabled = false,
                g2WeakTierSoundEnabled = true,
                deadzoneTierClickIntervalMs = 320,
                deadzoneTierSoundEnabled = false,
                deadzoneTierPulseDurationMs = 180,
                rsrqTierSoundEnabled = true,
                rsrqTierCoupledToSignalTier = true
            ),
            passiveMockSettings = PassiveMockSettings(enabled = true, rsrpDbm = -110, rsrqDb = -15),
            audioVolumes = AudioVolumeSettings(
                signalPulseFrequencyHz = 750,
                veryStrongTierPulseFrequencyHz = 880,
                g2StrongTierPulseFrequencyHz = 640,
                g2WeakTierPulseFrequencyHz = 520,
                signalPulseDurationMs = 320,
                technologyChangeTo4gVoiceEnabled = true,
                noSignalVoiceEnabled = true,
                limitedServiceVoiceEnabled = true
            )
        ).normalized()

        val encoded = AppSettingsSnapshotCodec.encodeProfiles(
            listOf(
                SettingsProfile(
                    id = "test-profile",
                    name = "Field test",
                    savedAtMs = 1_234L,
                    settings = snapshot
                )
            )
        )
        val decoded = AppSettingsSnapshotCodec.decodeProfiles(encoded).single().settings

        assertEquals(snapshot, decoded)
    }

    @Test
    fun roundTripPreservesHistogramBinSettings() {
        val snapshot = AppSettingsSnapshot(
            monitoringSettings = MonitoringSettings(
                rsrpHistogramWindowMs = 150_000L,
                rsrpHistogramBinningMode = RsrpHistogramBinningMode.THRESHOLD,
                rsrpHistogramThreshold1Dbm = -90,
                rsrpHistogramThreshold2Dbm = -100,
                rsrpHistogramThreshold3Dbm = -120
            )
        ).normalized()

        val encoded = AppSettingsSnapshotCodec.encodeProfile(
            SettingsProfile(
                id = "histogram-bins",
                name = "Histogram bins",
                savedAtMs = 7L,
                settings = snapshot
            )
        )
        val decoded = AppSettingsSnapshotCodec.decodeProfile(encoded)

        requireNotNull(decoded)
        val monitoring = decoded.settings.monitoringSettings
        assertEquals(150_000L, monitoring.rsrpHistogramWindowMs)
        assertEquals(RsrpHistogramBinningMode.THRESHOLD, monitoring.rsrpHistogramBinningMode)
        assertEquals(-90, monitoring.rsrpHistogramThreshold1Dbm)
        assertEquals(-100, monitoring.rsrpHistogramThreshold2Dbm)
        assertEquals(-120, monitoring.rsrpHistogramThreshold3Dbm)
        assertEquals(snapshot.monitoringSettings, monitoring)
    }

    @Test
    fun normalizedSnapshotReconcilesTierIntervalsWithPulseDuration() {
        val snapshot = AppSettingsSnapshot(
            passiveSignalSettings = PassiveSignalSettings(criticalTierClickIntervalMs = 50),
            audioVolumes = AudioVolumeSettings(signalPulseDurationMs = 250)
        ).normalized()

        assertEquals(280, snapshot.passiveSignalSettings.criticalTierClickIntervalMs)
    }

    @Test
    fun decodingLegacyProfileWithoutPerTierDurationKeysSeedsFromFormerlySharedAudioDurations() {
        // Pre-fix exported profile: no mildTierPulseDurationMs / criticalTierPulseDurationMs keys at all —
        // only the (now-legacy) shared audio-level durations that used to appear to control them.
        val legacyJson = """
            {
              "schemaVersion": 1,
              "profile": {
                "id": "legacy",
                "name": "Legacy export",
                "savedAtMs": 1,
                "settings": {
                  "audio": {
                    "signalPulseDurationMs": 222,
                    "levelRangeBcdPulseDurationMs": 333
                  },
                  "passiveSignal": {}
                }
              }
            }
        """.trimIndent()

        val decoded = AppSettingsSnapshotCodec.decodeProfile(legacyJson)

        requireNotNull(decoded)
        assertEquals(222, decoded.settings.passiveSignalSettings.criticalTierPulseDurationMs)
        assertEquals(333, decoded.settings.passiveSignalSettings.mildTierPulseDurationMs)
        assertEquals(333, decoded.settings.passiveSignalSettings.goodTierPulseDurationMs)
        assertEquals(333, decoded.settings.passiveSignalSettings.fairTierPulseDurationMs)
        assertEquals(333, decoded.settings.passiveSignalSettings.poorTierPulseDurationMs)
    }
}
