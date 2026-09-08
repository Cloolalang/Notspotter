package io.github.cloolalang.notspotdetector.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.cloolalang.notspotdetector.MonitorState
import io.github.cloolalang.notspotdetector.data.AudioVolumeSettingsRepository
import io.github.cloolalang.notspotdetector.data.MonitoringSettingsRepository
import io.github.cloolalang.notspotdetector.data.PassiveMockSettingsRepository
import io.github.cloolalang.notspotdetector.data.PassiveSignalSettingsRepository
import io.github.cloolalang.notspotdetector.data.PingSettingsRepository
import io.github.cloolalang.notspotdetector.data.SettingsProfilesRepository
import io.github.cloolalang.notspotdetector.data.ThresholdSettingsRepository
import io.github.cloolalang.notspotdetector.model.AppSettingsSnapshot
import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.ProfileImportResult
import io.github.cloolalang.notspotdetector.model.ProfileSaveResult
import io.github.cloolalang.notspotdetector.model.SettingsCompatibility
import io.github.cloolalang.notspotdetector.model.SettingsProfileSummary
import io.github.cloolalang.notspotdetector.model.toConnectivityStats
import io.github.cloolalang.notspotdetector.model.PingSettings
import io.github.cloolalang.notspotdetector.model.SimSubscriptionOption
import io.github.cloolalang.notspotdetector.model.ThresholdSettings
import java.io.File
import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import io.github.cloolalang.notspotdetector.network.SimSubscriptionHelper
import io.github.cloolalang.notspotdetector.audio.CellVoiceAnnouncer
import io.github.cloolalang.notspotdetector.audio.GeigerCounterPlayer
import io.github.cloolalang.notspotdetector.model.CellIdentityAnnouncement
import io.github.cloolalang.notspotdetector.model.SignalStateAnnouncement
import io.github.cloolalang.notspotdetector.service.ConnectivityMonitorService
import io.github.cloolalang.notspotdetector.util.BackgroundHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MonitorViewModel(application: Application) : AndroidViewModel(application) {

    private val thresholdRepository = ThresholdSettingsRepository(application)
    private val pingSettingsRepository = PingSettingsRepository(application)
    private val monitoringSettingsRepository = MonitoringSettingsRepository(application)
    private val audioVolumeRepository = AudioVolumeSettingsRepository(application)
    private val passiveSignalSettingsRepository = PassiveSignalSettingsRepository(application)
    private val passiveMockSettingsRepository = PassiveMockSettingsRepository(application)
    private val settingsProfilesRepository = SettingsProfilesRepository(application)
    private val alertSoundPreview = GeigerCounterPlayer()
    private val cellVoiceAnnouncer = CellVoiceAnnouncer(application)

    private val _settingsProfiles = MutableStateFlow(settingsProfilesRepository.listSummaries())
    val settingsProfiles: StateFlow<List<SettingsProfileSummary>> = _settingsProfiles.asStateFlow()

    private val _simSubscriptions = MutableStateFlow<List<SimSubscriptionOption>>(emptyList())
    val simSubscriptions: StateFlow<List<SimSubscriptionOption>> = _simSubscriptions.asStateFlow()

    init {
        MonitorState.setThresholds(thresholdRepository.load())
        MonitorState.setPingSettings(pingSettingsRepository.load())
        MonitorState.setMonitoringSettings(monitoringSettingsRepository.load())
        MonitorState.setAudioVolumes(audioVolumeRepository.load())
        MonitorState.setPassiveSignalSettings(passiveSignalSettingsRepository.load())
        MonitorState.setPassiveMockSettings(passiveMockSettingsRepository.load())
        reconcilePassiveTierClickIntervals(MonitorState.audioVolumes.value.signalPulseDurationMs)
        refreshSimSubscriptions()
        refreshCellularSignal()
    }

    val stats = MonitorState.stats.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MonitorState.stats.value
    )

    val isRunning = MonitorState.isRunning.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MonitorState.isRunning.value
    )

    val thresholds = MonitorState.thresholds.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MonitorState.thresholds.value
    )

    val pingSettings = MonitorState.pingSettings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MonitorState.pingSettings.value
    )

    val monitoringSettings = MonitorState.monitoringSettings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MonitorState.monitoringSettings.value
    )

    val audioVolumes = MonitorState.audioVolumes.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MonitorState.audioVolumes.value
    )

    val passiveSignalSettings = MonitorState.passiveSignalSettings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MonitorState.passiveSignalSettings.value
    )

    val passiveMockSettings = MonitorState.passiveMockSettings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MonitorState.passiveMockSettings.value
    )

    val rttHistory = MonitorState.rttHistory.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MonitorState.rttHistory.value
    )

    val isBatteryOptimizationDisabled: Boolean
        get() = BackgroundHelper.isIgnoringBatteryOptimizations(getApplication())

    val phoneStatePermissionGranted: Boolean
        get() = CellularSignalReader.hasPhoneStatePermission(getApplication())

    fun startMonitoring() {
        ConnectivityMonitorService.start(getApplication(), passiveOnly = false)
    }

    fun startPassiveOnlyMonitoring() {
        ConnectivityMonitorService.start(getApplication(), passiveOnly = true)
    }

    fun stopMonitoring() {
        ConnectivityMonitorService.stop(getApplication())
    }

    fun updateGoodRttMs(value: Long) {
        updateThresholds(thresholds.value.copy(goodRttMs = value))
    }

    fun updatePoorRttMs(value: Long) {
        updateThresholds(thresholds.value.copy(poorRttMs = value))
    }

    fun updatePoorJitterMs(value: Long) {
        updateThresholds(thresholds.value.copy(poorJitterMs = value))
    }

    fun updatePoorPacketLossPercent(value: Float) {
        updateThresholds(thresholds.value.copy(poorPacketLossPercent = value))
    }

    fun updateSuppressClicksOnGoodConnection(enabled: Boolean) {
        updateThresholds(thresholds.value.copy(suppressClicksOnGoodConnection = enabled))
    }

    fun updateGoodConnectionClicksPerPing(value: Int) {
        updateThresholds(thresholds.value.copy(goodConnectionClicksPerPing = value))
    }

    fun resetThresholds() {
        updateThresholds(ThresholdSettings())
    }

    fun updatePingAddress(input: String) {
        val (host, port) = PingSettings.parseAddress(input)
        updatePingSettings(pingSettings.value.copy(host = host, port = port))
    }

    fun updatePingsPerTest(value: Int) {
        updatePingSettings(pingSettings.value.copy(pingsPerTest = value))
    }

    fun updateTestIntervalMs(value: Long) {
        updatePingSettings(pingSettings.value.copy(testIntervalMs = value))
    }

    fun updateMonitor2gFallback(enabled: Boolean) {
        updateMonitoringSettings(monitoringSettings.value.copy(monitor2gFallback = enabled))
        refreshCellularSignal()
    }

    fun updatePassiveQuietUntilCritical(enabled: Boolean) {
        updateMonitoringSettings(monitoringSettings.value.copy(passiveQuietUntilCritical = enabled))
    }

    fun updatePassiveMeasurementIntervalMs(value: Long) {
        updateMonitoringSettings(monitoringSettings.value.copy(passiveMeasurementIntervalMs = value))
    }

    fun updateSelectedSubscription(subscriptionId: Int) {
        val wasRunning = isRunning.value
        val passiveOnly = stats.value.isPassiveOnlySession
        updateMonitoringSettings(monitoringSettings.value.copy(subscriptionId = subscriptionId))
        refreshCellularSignal()
        if (wasRunning) {
            stopMonitoring()
            if (passiveOnly) {
                startPassiveOnlyMonitoring()
            } else {
                startMonitoring()
            }
        }
    }

    fun refreshSimSubscriptions() {
        _simSubscriptions.value = SimSubscriptionHelper.listActiveSubscriptions(getApplication())
    }

    fun updatePingClickVolume(value: Float) {
        updateAudioVolumes(audioVolumes.value.copy(pingClickVolume = value))
    }

    fun updateLowSignalClickVolume(value: Float) {
        updateAudioVolumes(audioVolumes.value.copy(lowSignalClickVolume = value))
    }

    fun updateSignalPulseFrequencyHz(value: Int) {
        updateAudioVolumes(audioVolumes.value.copy(signalPulseFrequencyHz = value))
    }

    fun updateSignalPulseDurationMs(value: Int) {
        val normalizedAudio = audioVolumes.value.copy(signalPulseDurationMs = value).normalized()
        updateAudioVolumes(normalizedAudio)
        reconcilePassiveTierClickIntervals(normalizedAudio.signalPulseDurationMs)
    }

    fun updateCellChangeBellVolume(value: Float) {
        updateAudioVolumes(audioVolumes.value.copy(cellChangeBellVolume = value))
    }

    fun updateCellChangeVoiceEnabled(enabled: Boolean) {
        updateAudioVolumes(audioVolumes.value.copy(cellChangeVoiceEnabled = enabled))
    }

    fun updateCellChangeVoiceVolume(value: Float) {
        updateAudioVolumes(audioVolumes.value.copy(cellChangeVoiceVolume = value))
    }

    fun updateTechnologyChangeVolume(value: Float) {
        updateAudioVolumes(audioVolumes.value.copy(technologyChangeVolume = value))
    }

    fun updateTechnologyChangeVoiceEnabled(enabled: Boolean) {
        updateAudioVolumes(audioVolumes.value.copy(technologyChangeVoiceEnabled = enabled))
    }

    fun updateTechnologyChangeVoiceVolume(value: Float) {
        updateAudioVolumes(audioVolumes.value.copy(technologyChangeVoiceVolume = value))
    }

    fun updateNoSignalToneVolume(value: Float) {
        updateAudioVolumes(audioVolumes.value.copy(noSignalToneVolume = value))
    }

    fun updateNoSignalVoiceEnabled(enabled: Boolean) {
        updateAudioVolumes(audioVolumes.value.copy(noSignalVoiceEnabled = enabled))
    }

    fun updateNoSignalVoiceVolume(value: Float) {
        updateAudioVolumes(audioVolumes.value.copy(noSignalVoiceVolume = value))
    }

    fun updateLimitedServiceToneVolume(value: Float) {
        updateAudioVolumes(audioVolumes.value.copy(limitedServiceToneVolume = value))
    }

    fun updateLimitedServiceVoiceEnabled(enabled: Boolean) {
        updateAudioVolumes(audioVolumes.value.copy(limitedServiceVoiceEnabled = enabled))
    }

    fun updateLimitedServiceVoiceVolume(value: Float) {
        updateAudioVolumes(audioVolumes.value.copy(limitedServiceVoiceVolume = value))
    }

    fun resetAudioVolumes() {
        updateAudioVolumes(AudioVolumeSettings())
    }

    fun previewPingClickSound() {
        if (isRunning.value) return
        val volumes = audioVolumes.value.normalized()
        alertSoundPreview.previewPingClick(volumes.pingClickVolume)
    }

    fun previewLowSignalClickSound() {
        if (isRunning.value) return
        val volumes = audioVolumes.value.normalized()
        alertSoundPreview.previewLowSignalClick(
            volumes.lowSignalClickVolume,
            volumes.signalPulseDurationMs,
            volumes.signalPulseFrequencyHz
        )
    }

    fun previewCellChangeBellSound() {
        if (isRunning.value) return
        alertSoundPreview.playCellChangeBell(audioVolumes.value.normalized().cellChangeBellVolume)
    }

    fun previewCellChangeVoiceSound() {
        if (isRunning.value) return
        val volumes = audioVolumes.value.normalized()
        if (!volumes.cellChangeVoiceEnabled) return
        previewAlertWithVoice(
            onPlayTone = { alertSoundPreview.playCellChangeBell(volumes.cellChangeBellVolume) },
            toneDurationMs = GeigerCounterPlayer.CELL_CHANGE_BELL_DURATION_MS,
            announcement = CellIdentityAnnouncement.previewText(readCurrentOperatorName()),
            voiceVolume = volumes.cellChangeVoiceVolume
        )
    }

    fun previewTechnologyChangeSound() {
        if (isRunning.value) return
        alertSoundPreview.playTechnologyChangeTone(audioVolumes.value.normalized().technologyChangeVolume)
    }

    fun previewTechnologyChangeVoiceSound() {
        if (isRunning.value) return
        val volumes = audioVolumes.value.normalized()
        if (!volumes.technologyChangeVoiceEnabled) return
        previewAlertWithVoice(
            onPlayTone = { alertSoundPreview.playTechnologyChangeTone(volumes.technologyChangeVolume) },
            toneDurationMs = GeigerCounterPlayer.TECHNOLOGY_CHANGE_TONE_DURATION_MS,
            announcement = SignalStateAnnouncement.previewTechnologyChange(readCurrentOperatorName()),
            voiceVolume = volumes.technologyChangeVoiceVolume
        )
    }

    fun previewNoSignalToneSound() {
        if (isRunning.value) return
        alertSoundPreview.previewNoSignalTone(audioVolumes.value.normalized().noSignalToneVolume)
    }

    fun previewNoSignalVoiceSound() {
        if (isRunning.value) return
        val volumes = audioVolumes.value.normalized()
        if (!volumes.noSignalVoiceEnabled) return
        previewAlertWithVoice(
            onPlayTone = { alertSoundPreview.previewNoSignalTone(volumes.noSignalToneVolume) },
            toneDurationMs = GeigerCounterPlayer.NO_SIGNAL_ALERT_TONE_DURATION_MS,
            announcement = SignalStateAnnouncement.previewNoSignal(readCurrentOperatorName()),
            voiceVolume = volumes.noSignalVoiceVolume
        )
    }

    fun previewLimitedServiceToneSound() {
        if (isRunning.value) return
        alertSoundPreview.previewLimitedServiceTone(audioVolumes.value.normalized().limitedServiceToneVolume)
    }

    fun previewLimitedServiceVoiceSound() {
        if (isRunning.value) return
        val volumes = audioVolumes.value.normalized()
        if (!volumes.limitedServiceVoiceEnabled) return
        previewAlertWithVoice(
            onPlayTone = { alertSoundPreview.previewLimitedServiceTone(volumes.limitedServiceToneVolume) },
            toneDurationMs = GeigerCounterPlayer.LIMITED_SERVICE_ALERT_TONE_DURATION_MS,
            announcement = SignalStateAnnouncement.previewLimitedService(readCurrentOperatorName()),
            voiceVolume = volumes.limitedServiceVoiceVolume
        )
    }

    private fun previewAlertWithVoice(
        onPlayTone: () -> Unit,
        toneDurationMs: Int,
        announcement: String,
        voiceVolume: Float
    ) {
        onPlayTone()
        if (announcement.isBlank() || voiceVolume <= 0f) return
        viewModelScope.launch {
            delay(AudioVolumeSettings.voiceDelayAfterAlertTone(toneDurationMs))
            cellVoiceAnnouncer.speak(announcement, voiceVolume)
        }
    }

    private fun readCurrentOperatorName(): String? {
        val monitoring = monitoringSettings.value
        return CellularSignalReader.read(
            getApplication(),
            monitoring.monitor2gFallback,
            monitoring.subscriptionId
        ).networkOperatorName
    }

    fun saveSettingsProfile(name: String): ProfileSaveResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return ProfileSaveResult.EmptyName
        if (trimmed.length > SettingsProfilesRepository.MAX_NAME_LENGTH) return ProfileSaveResult.NameTooLong
        if (settingsProfilesRepository.hasName(trimmed)) return ProfileSaveResult.DuplicateName
        if (settingsProfilesRepository.loadProfiles().size >= SettingsProfilesRepository.MAX_PROFILES) {
            return ProfileSaveResult.TooManyProfiles
        }
        val snapshot = captureCurrentSettingsSnapshot()
        if (snapshot.isDefault()) return ProfileSaveResult.MatchesDefaults
        settingsProfilesRepository.saveProfile(trimmed, snapshot)
        refreshSettingsProfiles()
        return ProfileSaveResult.Saved
    }

    fun loadSettingsProfile(id: String) {
        val profile = settingsProfilesRepository.findById(id) ?: return
        val wasRunning = isRunning.value
        val passiveOnly = stats.value.isPassiveOnlySession
        val previousSubscription = monitoringSettings.value.subscriptionId
        applySettingsSnapshot(profile.settings)
        refreshCellularSignal()
        applyMockSignalIfActive()
        if (wasRunning && previousSubscription != monitoringSettings.value.subscriptionId) {
            stopMonitoring()
            if (passiveOnly) {
                startPassiveOnlyMonitoring()
            } else {
                startMonitoring()
            }
        }
    }

    fun deleteSettingsProfile(id: String) {
        if (settingsProfilesRepository.deleteProfile(id)) {
            refreshSettingsProfiles()
        }
    }

    fun exportSettingsProfile(id: String): File? {
        val file = settingsProfilesRepository.profileFile(id)
        return file.takeIf { it.exists() }
    }

    fun profileShareLabel(id: String): String? {
        return settingsProfilesRepository.findById(id)?.name
    }

    fun importSettingsProfile(uri: Uri): ProfileImportResult {
        val result = settingsProfilesRepository.importFromUri(uri)
        if (result == ProfileImportResult.Imported) {
            refreshSettingsProfiles()
        }
        return result
    }

    fun updatePassiveSignalSettings(settings: PassiveSignalSettings) {
        val normalized = SettingsCompatibility.normalizePassiveSignalSettings(
            settings,
            MonitorState.audioVolumes.value.signalPulseDurationMs
        )
        passiveSignalSettingsRepository.save(normalized)
        MonitorState.setPassiveSignalSettings(normalized)
    }

    fun resetPassiveSignalSettings() {
        updatePassiveSignalSettings(PassiveSignalSettings())
    }

    fun updatePassiveMockSettings(settings: PassiveMockSettings) {
        val normalized = settings.normalized()
        passiveMockSettingsRepository.save(normalized)
        MonitorState.setPassiveMockSettings(normalized)
        applyMockSignalIfActive()
    }

    fun refreshCellularSignal() {
        refreshSimSubscriptions()
        if (applyMockSignalIfActive()) return
        MonitorState.updateSignalMetrics(
            CellularSignalReader.read(
                getApplication(),
                MonitorState.monitoringSettings.value.monitor2gFallback,
                MonitorState.monitoringSettings.value.subscriptionId
            )
        )
    }

    private fun applyMockSignalIfActive(): Boolean {
        val mock = MonitorState.passiveMockSettings.value
        if (!mock.enabled || !isRunning.value || !stats.value.isPassiveOnlySession) return false
        MonitorState.updateStats(
            mock.toConnectivityStats(
                monitor2gFallback = MonitorState.monitoringSettings.value.monitor2gFallback,
                passiveSettings = MonitorState.passiveSignalSettings.value,
                passiveIdleMode = stats.value.isPassiveIdleMode,
                passiveOnlySession = true
            )
        )
        return true
    }

    private fun updateThresholds(settings: ThresholdSettings) {
        val normalized = settings.normalized()
        thresholdRepository.save(normalized)
        MonitorState.setThresholds(normalized)
    }

    private fun updatePingSettings(settings: PingSettings) {
        val normalized = settings.normalized()
        pingSettingsRepository.save(normalized)
        MonitorState.setPingSettings(normalized)
    }

    private fun updateMonitoringSettings(settings: MonitoringSettings) {
        val normalized = settings.normalized()
        monitoringSettingsRepository.save(normalized)
        MonitorState.setMonitoringSettings(normalized)
    }

    private fun updateAudioVolumes(settings: AudioVolumeSettings) {
        val normalized = settings.normalized()
        audioVolumeRepository.save(normalized)
        MonitorState.setAudioVolumes(normalized)
    }

    private fun reconcilePassiveTierClickIntervals(signalPulseDurationMs: Int) {
        val reconciled = SettingsCompatibility.normalizePassiveSignalSettings(
            MonitorState.passiveSignalSettings.value,
            signalPulseDurationMs
        )
        if (reconciled != MonitorState.passiveSignalSettings.value) {
            passiveSignalSettingsRepository.save(reconciled)
            MonitorState.setPassiveSignalSettings(reconciled)
        }
    }

    private fun captureCurrentSettingsSnapshot(): AppSettingsSnapshot {
        return AppSettingsSnapshot(
            thresholds = thresholds.value,
            pingSettings = pingSettings.value,
            monitoringSettings = monitoringSettings.value,
            passiveSignalSettings = passiveSignalSettings.value,
            passiveMockSettings = passiveMockSettings.value,
            audioVolumes = audioVolumes.value
        ).normalized()
    }

    private fun applySettingsSnapshot(snapshot: AppSettingsSnapshot) {
        val normalized = snapshot.normalized()
        updateThresholds(normalized.thresholds)
        updatePingSettings(normalized.pingSettings)
        updateMonitoringSettings(normalized.monitoringSettings)
        updatePassiveSignalSettings(normalized.passiveSignalSettings)
        updatePassiveMockSettings(normalized.passiveMockSettings)
        updateAudioVolumes(normalized.audioVolumes)
    }

    private fun refreshSettingsProfiles() {
        _settingsProfiles.value = settingsProfilesRepository.listSummaries()
    }

    override fun onCleared() {
        cellVoiceAnnouncer.shutdown()
        super.onCleared()
    }
}
