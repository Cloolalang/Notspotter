package io.github.cloolalang.notspotdetector.data

import io.github.cloolalang.notspotdetector.model.AppSettingsSnapshot
import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.PingSettings
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
                passiveMeasurementIntervalMs = 3_000L
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
                noisyRsrqPassiveClicks = true
            ),
            passiveMockSettings = PassiveMockSettings(enabled = true, rsrpDbm = -110, rsrqDb = -15),
            audioVolumes = AudioVolumeSettings(
                signalPulseFrequencyHz = 750,
                signalPulseDurationMs = 320,
                technologyChangeVoiceEnabled = true,
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
    fun normalizedSnapshotReconcilesTierIntervalsWithPulseDuration() {
        val snapshot = AppSettingsSnapshot(
            passiveSignalSettings = PassiveSignalSettings(criticalTierClickIntervalMs = 50),
            audioVolumes = AudioVolumeSettings(signalPulseDurationMs = 250)
        ).normalized()

        assertEquals(280, snapshot.passiveSignalSettings.criticalTierClickIntervalMs)
    }
}
