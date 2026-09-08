package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.cloolalang.notspotdetector.BuildConfig
import io.github.cloolalang.notspotdetector.model.ProfileImportResult
import io.github.cloolalang.notspotdetector.viewmodel.MonitorViewModel

@Composable
fun MonitorApp(
    viewModel: MonitorViewModel,
    onRequestBatteryExemption: () -> Unit,
    onRequestCellIdentityPermission: () -> Unit,
    onImportSettingsProfile: (onResult: (ProfileImportResult) -> Unit) -> Unit,
    onShareSettingsProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val thresholds by viewModel.thresholds.collectAsStateWithLifecycle()
    val pingSettings by viewModel.pingSettings.collectAsStateWithLifecycle()
    val monitoringSettings by viewModel.monitoringSettings.collectAsStateWithLifecycle()
    val passiveSignalSettings by viewModel.passiveSignalSettings.collectAsStateWithLifecycle()
    val passiveMockSettings by viewModel.passiveMockSettings.collectAsStateWithLifecycle()
    val simSubscriptions by viewModel.simSubscriptions.collectAsStateWithLifecycle()
    val audioVolumes by viewModel.audioVolumes.collectAsStateWithLifecycle()
    val voiceAnnouncerOptions by viewModel.voiceAnnouncerOptions.collectAsStateWithLifecycle()
    val settingsProfiles by viewModel.settingsProfiles.collectAsStateWithLifecycle()
    val rttHistory by viewModel.rttHistory.collectAsStateWithLifecycle()
    val rsrpHistory by viewModel.rsrpHistory.collectAsStateWithLifecycle()

    var phoneStatePermissionGranted by remember {
        mutableStateOf(viewModel.phoneStatePermissionGranted)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        phoneStatePermissionGranted = viewModel.phoneStatePermissionGranted
        viewModel.reloadSettingsProfiles()
        viewModel.refreshCellularSignal()
    }

    fun ensureBackgroundMonitoringEnabled() {
        if (!viewModel.isBatteryOptimizationDisabled) {
            onRequestBatteryExemption()
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        MonitorScreen(
            stats = stats,
            thresholds = thresholds,
            pingSettings = pingSettings,
            monitoringSettings = monitoringSettings,
            passiveSignalSettings = passiveSignalSettings,
            passiveMockSettings = passiveMockSettings,
            simSubscriptions = simSubscriptions,
            phoneStatePermissionGranted = phoneStatePermissionGranted,
            audioVolumes = audioVolumes,
            voiceAnnouncerOptions = voiceAnnouncerOptions,
            settingsProfiles = settingsProfiles,
            rttHistory = rttHistory,
            rsrpHistory = rsrpHistory,
            isRunning = isRunning,
            onStart = {
                ensureBackgroundMonitoringEnabled()
                viewModel.startMonitoring()
            },
            onStartPassiveOnly = {
                ensureBackgroundMonitoringEnabled()
                viewModel.startPassiveOnlyMonitoring()
            },
            onStop = viewModel::stopMonitoring,
            onGoodRttChange = viewModel::updateGoodRttMs,
            onPoorRttChange = viewModel::updatePoorRttMs,
            onPoorJitterChange = viewModel::updatePoorJitterMs,
            onPoorPacketLossChange = viewModel::updatePoorPacketLossPercent,
            onSuppressClicksOnGoodChange = viewModel::updateSuppressClicksOnGoodConnection,
            onGoodConnectionClicksPerPingChange = viewModel::updateGoodConnectionClicksPerPing,
            onResetThresholds = viewModel::resetThresholds,
            onPingAddressChange = viewModel::updatePingAddress,
            onPingsPerTestChange = viewModel::updatePingsPerTest,
            onTestIntervalChange = viewModel::updateTestIntervalMs,
            onMonitor2gFallbackChange = viewModel::updateMonitor2gFallback,
            onPassiveQuietUntilCriticalChange = viewModel::updatePassiveQuietUntilCritical,
            onPassiveSignalSettingsChange = viewModel::updatePassiveSignalSettings,
            onPassiveMockSettingsChange = viewModel::updatePassiveMockSettings,
            onPassiveMeasurementIntervalChange = viewModel::updatePassiveMeasurementIntervalMs,
            onRsrpHistogramWindowChange = viewModel::updateRsrpHistogramWindowMs,
            onResetPassiveSignalSettings = viewModel::resetPassiveSignalSettings,
            onSubscriptionChange = viewModel::updateSelectedSubscription,
            onVoiceAnnouncerChoiceChange = viewModel::updateVoiceAnnouncerChoice,
            onRefreshVoiceAnnouncerOptions = viewModel::refreshVoiceAnnouncerOptions,
            onPreviewVoiceAnnouncer = viewModel::previewVoiceAnnouncerSound,
            onPingClickVolumeChange = viewModel::updatePingClickVolume,
            onLowSignalClickVolumeChange = viewModel::updateLowSignalClickVolume,
            onSignalPulseFrequencyChange = viewModel::updateSignalPulseFrequencyHz,
            onSignalPulseDurationChange = viewModel::updateSignalPulseDurationMs,
            onCellChangeBellVolumeChange = viewModel::updateCellChangeBellVolume,
            onCellChangeVoiceEnabledChange = viewModel::updateCellChangeVoiceEnabled,
            onCellChangeVoiceVolumeChange = viewModel::updateCellChangeVoiceVolume,
            onTechnologyChangeVolumeChange = viewModel::updateTechnologyChangeVolume,
            onTechnologyChangeVoiceEnabledChange = viewModel::updateTechnologyChangeVoiceEnabled,
            onTechnologyChangeVoiceVolumeChange = viewModel::updateTechnologyChangeVoiceVolume,
            onTier5AnnouncerEnabledChange = viewModel::updateTier5AnnouncerEnabled,
            onTier5AnnouncerVolumeChange = viewModel::updateTier5AnnouncerVolume,
            onNoSignalToneVolumeChange = viewModel::updateNoSignalToneVolume,
            onNoSignalVibrationEnabledChange = viewModel::updateNoSignalVibrationEnabled,
            onNoSignalVoiceEnabledChange = viewModel::updateNoSignalVoiceEnabled,
            onNoSignalVoiceVolumeChange = viewModel::updateNoSignalVoiceVolume,
            onLimitedServiceToneVolumeChange = viewModel::updateLimitedServiceToneVolume,
            onLimitedServiceVoiceEnabledChange = viewModel::updateLimitedServiceVoiceEnabled,
            onLimitedServiceVoiceVolumeChange = viewModel::updateLimitedServiceVoiceVolume,
            onPreviewPingClick = viewModel::previewPingClickSound,
            onPreviewLowSignalClick = viewModel::previewLowSignalClickSound,
            onPreviewCellChangeBell = viewModel::previewCellChangeBellSound,
            onPreviewCellChangeVoice = viewModel::previewCellChangeVoiceSound,
            onPreviewTechnologyChange = viewModel::previewTechnologyChangeSound,
            onPreviewTechnologyChangeVoice = viewModel::previewTechnologyChangeVoiceSound,
            onPreviewTier5Announcer = viewModel::previewTier5AnnouncerSound,
            onPreviewNoSignalTone = viewModel::previewNoSignalToneSound,
            onPreviewNoSignalVoice = viewModel::previewNoSignalVoiceSound,
            onPreviewLimitedServiceTone = viewModel::previewLimitedServiceToneSound,
            onPreviewLimitedServiceVoice = viewModel::previewLimitedServiceVoiceSound,
            onResetAudioVolumes = viewModel::resetAudioVolumes,
            onSaveSettingsProfile = viewModel::saveSettingsProfile,
            onLoadSettingsProfile = viewModel::loadSettingsProfile,
            onDeleteSettingsProfile = viewModel::deleteSettingsProfile,
            onImportSettingsProfile = onImportSettingsProfile,
            onShareSettingsProfile = onShareSettingsProfile,
            onExportSettingsProfileToDownloads = viewModel::exportSettingsProfileToDownloads,
            onRequestCellIdentityPermission = onRequestCellIdentityPermission,
            appVersion = BuildConfig.VERSION_NAME,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
