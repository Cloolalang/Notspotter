package io.github.cloolalang.notspotdetector.model

/**
 * Full user-configurable settings bundle stored in a named profile.
 *
 * Captured on save: [ThresholdSettings], [PingSettings], [MonitoringSettings],
 * [PassiveSignalSettings], [PassiveMockSettings] (enabled, scenario, rsrpDbm, rsrqDb),
 * and [AudioVolumeSettings].
 *
 * Not captured: monitoring session state, live [ConnectivityStats], or permissions.
 * See [SETTINGS_PROFILES.md] and [AppSettingsSnapshotCodec].
 */
data class AppSettingsSnapshot(
    val thresholds: ThresholdSettings = ThresholdSettings(),
    val pingSettings: PingSettings = PingSettings(),
    val monitoringSettings: MonitoringSettings = MonitoringSettings(),
    val passiveSignalSettings: PassiveSignalSettings = PassiveSignalSettings(),
    val passiveMockSettings: PassiveMockSettings = PassiveMockSettings(),
    val audioVolumes: AudioVolumeSettings = AudioVolumeSettings()
) {
    fun normalized(): AppSettingsSnapshot {
        val normalizedAudio = audioVolumes.normalized()
        return copy(
            thresholds = thresholds.normalized(),
            pingSettings = pingSettings.normalized(),
            monitoringSettings = monitoringSettings.normalized(),
            passiveSignalSettings = SettingsCompatibility.normalizePassiveSignalSettings(
                passiveSignalSettings.normalized(),
                normalizedAudio.signalPulseDurationMs,
                normalizedAudio.levelRangeBcdPulseDurationMs
            ),
            passiveMockSettings = passiveMockSettings.normalized(),
            audioVolumes = normalizedAudio
        )
    }

    fun isDefault(): Boolean = this == defaults()

    companion object {
        fun defaults(): AppSettingsSnapshot = AppSettingsSnapshot().normalized()
    }
}

data class SettingsProfile(
    val id: String,
    val name: String,
    val savedAtMs: Long,
    val settings: AppSettingsSnapshot
)

data class SettingsProfileSummary(
    val id: String,
    val name: String,
    val savedAtMs: Long,
    val differsFromDefaults: Boolean
)

enum class ProfileSaveResult {
    Saved,
    EmptyName,
    DuplicateName,
    MatchesDefaults,
    TooManyProfiles,
    NameTooLong,
    Failed
}

enum class ProfileImportResult {
    Imported,
    InvalidFile,
    TooManyProfiles,
    Failed
}
