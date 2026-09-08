package io.github.cloolalang.notspotdetector.model

/**
 * Full app settings bundle stored in a named profile.
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
        return copy(
            thresholds = thresholds.normalized(),
            pingSettings = pingSettings.normalized(),
            monitoringSettings = monitoringSettings.normalized(),
            passiveSignalSettings = passiveSignalSettings.normalized(),
            passiveMockSettings = passiveMockSettings.normalized(),
            audioVolumes = audioVolumes.normalized()
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
    NameTooLong
}
