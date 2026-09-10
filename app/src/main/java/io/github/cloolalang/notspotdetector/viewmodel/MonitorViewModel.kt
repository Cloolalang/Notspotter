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
import io.github.cloolalang.notspotdetector.model.ProfileExportOutcome
import io.github.cloolalang.notspotdetector.model.ProfileImportResult
import io.github.cloolalang.notspotdetector.model.ProfileSaveResult
import io.github.cloolalang.notspotdetector.model.SettingsCompatibility
import io.github.cloolalang.notspotdetector.model.CellReselectBandNamingStyle
import io.github.cloolalang.notspotdetector.model.TechnologyChangeTarget
import io.github.cloolalang.notspotdetector.model.SettingsProfileSummary
import io.github.cloolalang.notspotdetector.model.PingSettings
import io.github.cloolalang.notspotdetector.model.SimSubscriptionOption
import io.github.cloolalang.notspotdetector.model.ThresholdSettings
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerChoice
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerOption
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerSelection
import io.github.cloolalang.notspotdetector.R
import java.io.File
import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import io.github.cloolalang.notspotdetector.network.SimSubscriptionHelper
import io.github.cloolalang.notspotdetector.audio.CellVoiceAnnouncer
import io.github.cloolalang.notspotdetector.audio.AlertVibrator
import io.github.cloolalang.notspotdetector.audio.GeigerCounterPlayer
import io.github.cloolalang.notspotdetector.model.CellIdentityAnnouncement
import io.github.cloolalang.notspotdetector.model.SignalStateAnnouncement
import io.github.cloolalang.notspotdetector.service.ConnectivityMonitorService
import io.github.cloolalang.notspotdetector.util.BackgroundHelper
import kotlinx.coroutines.Dispatchers
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

    private val _voiceAnnouncerOptions = MutableStateFlow(defaultVoiceAnnouncerOptions())
    val voiceAnnouncerOptions: StateFlow<List<VoiceAnnouncerOption>> = _voiceAnnouncerOptions.asStateFlow()

    private val _settingsProfiles = MutableStateFlow(settingsProfilesRepository.listSummaries())
    val settingsProfiles: StateFlow<List<SettingsProfileSummary>> = _settingsProfiles.asStateFlow()

    private val _simSubscriptions = MutableStateFlow<List<SimSubscriptionOption>>(emptyList())
    val simSubscriptions: StateFlow<List<SimSubscriptionOption>> = _simSubscriptions.asStateFlow()

    init {
        MonitorState.setThresholds(thresholdRepository.load())
        MonitorState.setPingSettings(pingSettingsRepository.load())
        MonitorState.setMonitoringSettings(monitoringSettingsRepository.load())
        val loadedAudioVolumes = audioVolumeRepository.load()
        MonitorState.setAudioVolumes(loadedAudioVolumes)
        MonitorState.setPassiveSignalSettings(
            passiveSignalSettingsRepository.load(
                legacySignalPulseDurationMs = loadedAudioVolumes.signalPulseDurationMs,
                legacyLevelRangeBcdPulseDurationMs = loadedAudioVolumes.levelRangeBcdPulseDurationMs
            )
        )
        MonitorState.setPassiveMockSettings(passiveMockSettingsRepository.load())
        reconcilePassiveTierClickIntervals(MonitorState.audioVolumes.value)
        cellVoiceAnnouncer.setVoiceSelectionProvider {
            VoiceAnnouncerSelection.fromSettings(MonitorState.audioVolumes.value)
        }
        cellVoiceAnnouncer.setOnReadyListener { refreshVoiceAnnouncerOptions() }
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

    val rsrpHistory = MonitorState.rsrpHistory.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MonitorState.rsrpHistory.value
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

    fun updateRsrpHistogramWindowMs(value: Long) {
        updateMonitoringSettings(monitoringSettings.value.copy(rsrpHistogramWindowMs = value))
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

    fun updateNoSignalTierPulseFrequencyHz(value: Int) {
        updateAudioVolumes(audioVolumes.value.copy(noSignalTierPulseFrequencyHz = value))
    }

    fun updateLevelRangeBcdPulseFrequencyHz(value: Int) {
        updateAudioVolumes(audioVolumes.value.copy(levelRangeBcdPulseFrequencyHz = value))
    }

    fun updateLevelRangeBcdClickVolume(value: Float) {
        updateAudioVolumes(audioVolumes.value.copy(levelRangeBcdClickVolume = value))
    }

    fun updateVeryStrongTierPulseFrequencyHz(value: Int) {
        updateAudioVolumes(audioVolumes.value.copy(veryStrongTierPulseFrequencyHz = value))
    }

    fun updateG2StrongTierPulseFrequencyHz(value: Int) {
        updateAudioVolumes(audioVolumes.value.copy(g2StrongTierPulseFrequencyHz = value))
    }

    fun updateG2WeakTierPulseFrequencyHz(value: Int) {
        updateAudioVolumes(audioVolumes.value.copy(g2WeakTierPulseFrequencyHz = value))
    }

    fun updateSignalPulseDurationMs(value: Int) {
        val normalizedAudio = audioVolumes.value.copy(signalPulseDurationMs = value).normalized()
        updateAudioVolumes(normalizedAudio)
        reconcilePassiveTierClickIntervals(normalizedAudio)
    }

    fun updateMasterVoiceAnnouncementsEnabled(enabled: Boolean) {
        updateAudioVolumes(audioVolumes.value.copy(masterVoiceAnnouncementsEnabled = enabled))
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

    fun updateCellChangeSpeakBandEnabled(enabled: Boolean) {
        updateAudioVolumes(audioVolumes.value.copy(cellChangeSpeakBandEnabled = enabled))
    }

    fun updateCellChangeBandNamingStyle(style: CellReselectBandNamingStyle) {
        updateAudioVolumes(audioVolumes.value.copy(cellChangeBandNamingStyle = style))
    }

    fun updateTechnologyChangeToneVolume(target: TechnologyChangeTarget, value: Float) {
        updateAudioVolumes(audioVolumes.value.withTechnologyChangeToneVolume(target, value))
    }

    fun updateTechnologyChangeVoiceEnabled(target: TechnologyChangeTarget, enabled: Boolean) {
        updateAudioVolumes(audioVolumes.value.withTechnologyChangeVoiceEnabled(target, enabled))
    }

    fun updateTechnologyChangeVoiceVolume(target: TechnologyChangeTarget, value: Float) {
        updateAudioVolumes(audioVolumes.value.withTechnologyChangeVoiceVolume(target, value))
    }

    fun updateTier5AnnouncerEnabled(enabled: Boolean) {
        updateAudioVolumes(audioVolumes.value.copy(tier5AnnouncerEnabled = enabled))
    }

    fun updateTier5AnnouncerVolume(value: Float) {
        updateAudioVolumes(audioVolumes.value.copy(tier5AnnouncerVolume = value))
    }

    fun updateVoiceAnnouncerChoice(choice: VoiceAnnouncerChoice) {
        val engineVoiceId = if (choice == VoiceAnnouncerChoice.SYSTEM_DEFAULT) {
            null
        } else {
            cellVoiceAnnouncer.getResolvedOptions()
                .find { it.choice == choice }
                ?.engineVoiceId
        }
        updateAudioVolumes(
            audioVolumes.value.copy(
                voiceAnnouncerChoice = choice,
                voiceAnnouncerEngineId = engineVoiceId
            )
        )
    }

    fun refreshVoiceAnnouncerOptions() {
        val options = cellVoiceAnnouncer.getResolvedOptions()
        _voiceAnnouncerOptions.value = options
        val current = audioVolumes.value
        if (current.voiceAnnouncerChoice == VoiceAnnouncerChoice.SYSTEM_DEFAULT) {
            if (current.voiceAnnouncerEngineId != null) {
                updateAudioVolumes(current.copy(voiceAnnouncerEngineId = null))
            }
            return
        }
        val resolvedEngineId = options.find { it.choice == current.voiceAnnouncerChoice }?.engineVoiceId
        if (resolvedEngineId != null && resolvedEngineId != current.voiceAnnouncerEngineId) {
            updateAudioVolumes(current.copy(voiceAnnouncerEngineId = resolvedEngineId))
        }
    }

    fun updateNoSignalToneVolume(value: Float) {
        updateAudioVolumes(audioVolumes.value.copy(noSignalToneVolume = value))
    }

    fun updateNoSignalVibrationEnabled(enabled: Boolean) {
        updateAudioVolumes(audioVolumes.value.copy(noSignalVibrationEnabled = enabled))
    }

    fun updateNoSignalVoiceEnabled(enabled: Boolean) {
        updateAudioVolumes(audioVolumes.value.copy(noSignalVoiceEnabled = enabled))
    }

    fun updateNoSignalVoiceVolume(value: Float) {
        updateAudioVolumes(audioVolumes.value.copy(noSignalVoiceVolume = value))
    }

    fun updateLimitedServiceTierPulseFrequencyHz(value: Int) {
        updateAudioVolumes(audioVolumes.value.copy(limitedServiceTierPulseFrequencyHz = value))
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

    fun previewLowSignalClickSound(frequencyHz: Int, pulseDurationMs: Int) {
        previewSignalPulseSound(
            volume = audioVolumes.value.normalized().lowSignalClickVolume,
            frequencyHz = frequencyHz,
            pulseDurationMs = pulseDurationMs
        )
    }

    fun previewSignalPulseSound(volume: Float, frequencyHz: Int, pulseDurationMs: Int) {
        if (isRunning.value) return
        alertSoundPreview.previewLowSignalClick(volume, pulseDurationMs, frequencyHz)
    }

    fun previewLevelRangeBcdClickSound(frequencyHz: Int, pulseDurationMs: Int) {
        if (isRunning.value) return
        val volumes = audioVolumes.value.normalized()
        alertSoundPreview.previewLowSignalClick(
            volumes.levelRangeBcdClickVolume,
            pulseDurationMs,
            frequencyHz
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
            announcement = CellIdentityAnnouncement.previewText(
                readCurrentOperatorName(),
                speakBandEnabled = volumes.cellChangeSpeakBandEnabled,
                bandNamingStyle = volumes.cellChangeBandNamingStyle
            ),
            voiceVolume = volumes.cellChangeVoiceVolume
        )
    }

    fun previewTechnologyChangeSound(target: TechnologyChangeTarget) {
        if (isRunning.value) return
        val alertVolumes = audioVolumes.value.normalized().technologyChangeAlertVolumes(target)
        alertSoundPreview.playTechnologyChangeTone(alertVolumes.toneVolume)
    }

    fun previewTechnologyChangeVoiceSound(target: TechnologyChangeTarget) {
        if (isRunning.value) return
        val volumes = audioVolumes.value.normalized()
        val alertVolumes = volumes.technologyChangeAlertVolumes(target)
        previewAlertWithVoice(
            onPlayTone = { alertSoundPreview.playTechnologyChangeTone(alertVolumes.toneVolume) },
            toneDurationMs = GeigerCounterPlayer.TECHNOLOGY_CHANGE_TONE_DURATION_MS,
            announcement = SignalStateAnnouncement.previewTechnologyChange(readCurrentOperatorName(), target),
            voiceVolume = alertVolumes.voiceVolume
        )
    }

    fun previewTier5AnnouncerSound() {
        if (isRunning.value) return
        val volumes = audioVolumes.value.normalized()
        if (volumes.tier5AnnouncerVolume <= 0f) return
        val announcement = SignalStateAnnouncement.previewTier5SignalLow(readCurrentOperatorName())
        if (announcement.isBlank()) return
        viewModelScope.launch {
            cellVoiceAnnouncer.speak(announcement, volumes.tier5AnnouncerVolume)
        }
    }

    fun previewVoiceAnnouncerSound() {
        if (isRunning.value) return
        val volumes = audioVolumes.value.normalized()
        cellVoiceAnnouncer.stop()
        cellVoiceAnnouncer.speak(
            getApplication<Application>().getString(R.string.audio_voice_announcer_preview),
            AudioVolumeSettings.DEFAULT_VOLUME,
            VoiceAnnouncerSelection.fromSettings(volumes)
        )
    }

    fun previewNoSignalToneSound() {
        if (isRunning.value) return
        val volumes = audioVolumes.value.normalized()
        if (volumes.noSignalVibrationEnabled) {
            AlertVibrator.buzzNoSignal(getApplication())
        }
        alertSoundPreview.previewNoSignalTone(volumes.noSignalToneVolume)
    }

    fun previewNoSignalVoiceSound() {
        if (isRunning.value) return
        val volumes = audioVolumes.value.normalized()
        if (!volumes.noSignalVoiceEnabled) return
        if (volumes.noSignalVibrationEnabled) {
            AlertVibrator.buzzNoSignal(getApplication())
        }
        previewAlertWithVoice(
            onPlayTone = { alertSoundPreview.previewNoSignalTone(volumes.noSignalToneVolume) },
            toneDurationMs = GeigerCounterPlayer.NO_SIGNAL_ALERT_TONE_DURATION_MS,
            announcement = SignalStateAnnouncement.previewNoSignal(readCurrentOperatorName()),
            voiceVolume = volumes.noSignalVoiceVolume
        )
    }

    fun previewRsrqWhiteNoiseSound() {
        if (isRunning.value) return
        val volumes = audioVolumes.value.normalized()
        val passive = passiveSignalSettings.value
        alertSoundPreview.previewRsrqWhiteNoise(
            clickVolume = volumes.lowSignalClickVolume,
            whiteNoiseMix = passive.rsrqTierWhiteNoiseVolume,
            pulseDurationMs = passive.rsrqTierPulseDurationMs
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
        if (!settingsProfilesRepository.saveProfile(trimmed, snapshot)) {
            return ProfileSaveResult.Failed
        }
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

    fun exportSettingsProfileToDownloads(id: String): ProfileExportOutcome {
        return settingsProfilesRepository.exportProfileToDownloads(getApplication(), id)
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
    }

    fun refreshCellularSignal() {
        refreshSimSubscriptions()
        if (applyMockSignalIfActive()) return
        if (MonitorState.isRunning.value) return

        viewModelScope.launch(Dispatchers.IO) {
            val metrics = CellularSignalReader.read(
                getApplication(),
                MonitorState.monitoringSettings.value.monitor2gFallback,
                MonitorState.monitoringSettings.value.subscriptionId
            )
            MonitorState.updateSignalMetrics(metrics)
        }
    }

    private fun applyMockSignalIfActive(): Boolean {
        return MonitorState.pushMockStatsIfActive()
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

    private fun reconcilePassiveTierClickIntervals(audioVolumes: AudioVolumeSettings) {
        val reconciled = SettingsCompatibility.normalizePassiveSignalSettings(
            MonitorState.passiveSignalSettings.value,
            audioVolumes.signalPulseDurationMs
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

    fun reloadSettingsProfiles() {
        refreshSettingsProfiles()
    }

    override fun onCleared() {
        cellVoiceAnnouncer.shutdown()
        super.onCleared()
    }

    private fun defaultVoiceAnnouncerOptions(): List<VoiceAnnouncerOption> {
        return VoiceAnnouncerChoice.selectableChoices.map { choice ->
            VoiceAnnouncerOption(
                choice = choice,
                engineVoiceId = null,
                engineVoiceName = null,
                available = choice == VoiceAnnouncerChoice.SYSTEM_DEFAULT
            )
        }
    }
}
