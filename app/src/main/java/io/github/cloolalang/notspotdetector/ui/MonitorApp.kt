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
import io.github.cloolalang.notspotdetector.model.SpecialCellsImportResult
import io.github.cloolalang.notspotdetector.viewmodel.MonitorViewModel

@Composable
fun MonitorApp(
    viewModel: MonitorViewModel,
    onRequestBatteryExemption: () -> Unit,
    onRequestCellIdentityPermission: () -> Unit,
    onImportSettingsProfile: (onResult: (ProfileImportResult) -> Unit) -> Unit,
    onShareSettingsProfile: (String) -> Unit,
    onShareRadioDebugLog: () -> Unit,
    onImportSpecialCells: (onResult: (SpecialCellsImportResult) -> Unit) -> Unit,
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
    val specialCellCatalog by viewModel.specialCellCatalog.collectAsStateWithLifecycle()
    val specialCellMatch by viewModel.specialCellMatch.collectAsStateWithLifecycle()
    val radioDebugEnabled by viewModel.radioDebugEnabled.collectAsStateWithLifecycle()
    val radioDebugSnapshot by viewModel.radioDebugSnapshot.collectAsStateWithLifecycle()
    val radioDebugLineCount by viewModel.radioDebugLineCount.collectAsStateWithLifecycle()
    val rttHistory by viewModel.rttHistory.collectAsStateWithLifecycle()
    val rsrpHistory by viewModel.rsrpHistory.collectAsStateWithLifecycle()
    val carrierConfigSnapshot by viewModel.carrierConfigSnapshot.collectAsStateWithLifecycle()

    var phoneStatePermissionGranted by remember {
        mutableStateOf(viewModel.phoneStatePermissionGranted)
    }
    var settingsUnlocked by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        phoneStatePermissionGranted = viewModel.phoneStatePermissionGranted
        viewModel.reloadSettingsProfiles()
        viewModel.refreshCellularSignal()
        viewModel.refreshCarrierConfigSnapshot()
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
            specialCellCatalog = specialCellCatalog,
            specialCellMatch = specialCellMatch,
            rttHistory = rttHistory,
            rsrpHistory = rsrpHistory,
            isRunning = isRunning,
            onStart = {
                // "Start monitoring" starts passive (signal-only) monitoring — see
                // MonitoringControlButtons for the disabled active-mode-testing placeholder.
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
            onRsrpHistogramBinningModeChange = viewModel::updateRsrpHistogramBinningMode,
            onRsrpHistogramThresholdChange = viewModel::updateRsrpHistogramThresholdDbm,
            onClearRsrpHistogram = viewModel::clearRsrpHistogram,
            onResetPassiveSignalSettings = viewModel::resetPassiveSignalSettings,
            onSubscriptionChange = viewModel::updateSelectedSubscription,
            onVoiceAnnouncerChoiceChange = viewModel::updateVoiceAnnouncerChoice,
            onVoiceSpeechRateChange = viewModel::updateVoiceSpeechRate,
            onRefreshVoiceAnnouncerOptions = viewModel::refreshVoiceAnnouncerOptions,
            onPreviewVoiceAnnouncer = viewModel::previewVoiceAnnouncerSound,
            onPingClickVolumeChange = viewModel::updatePingClickVolume,
            onLowSignalClickVolumeChange = viewModel::updateLowSignalClickVolume,
            onSignalPulseFrequencyChange = viewModel::updateSignalPulseFrequencyHz,
            onVeryStrongTierPulseFrequencyChange = viewModel::updateVeryStrongTierPulseFrequencyHz,
            onG2StrongTierPulseFrequencyChange = viewModel::updateG2StrongTierPulseFrequencyHz,
            onG2WeakTierPulseFrequencyChange = viewModel::updateG2WeakTierPulseFrequencyHz,
            onSignalPulseDurationChange = viewModel::updateSignalPulseDurationMs,
            onMasterVoiceAnnouncementsEnabledChange = viewModel::updateMasterVoiceAnnouncementsEnabled,
            onVoicePhrasesChange = viewModel::updateVoicePhrases,
            onPreviewVoicePhrase = viewModel::previewVoicePhrase,
            onPeriodicVoiceRepeatChange = viewModel::updatePeriodicVoiceRepeat,
            onFiveGFeaturesEnabledChange = viewModel::updateFiveGFeaturesEnabled,
            settingsUnlocked = settingsUnlocked,
            onSettingsUnlockedChange = { settingsUnlocked = it },
            onCellChangeBellVolumeChange = viewModel::updateCellChangeBellVolume,
            onCellChangeVoiceEnabledChange = viewModel::updateCellChangeVoiceEnabled,
            onCellChangeVoiceVolumeChange = viewModel::updateCellChangeVoiceVolume,
            onCellChangeSpeakBandEnabledChange = viewModel::updateCellChangeSpeakBandEnabled,
            onCellChangeBandNamingStyleChange = viewModel::updateCellChangeBandNamingStyle,
            onTechnologyChangeToneVolumeChange = viewModel::updateTechnologyChangeToneVolume,
            onTechnologyChangeVoiceEnabledChange = viewModel::updateTechnologyChangeVoiceEnabled,
            onTechnologyChangeVoiceVolumeChange = viewModel::updateTechnologyChangeVoiceVolume,
            onTier5AnnouncerEnabledChange = viewModel::updateTier5AnnouncerEnabled,
            onTier5AnnouncerVolumeChange = viewModel::updateTier5AnnouncerVolume,
            onNoSignalTierPulseFrequencyChange = viewModel::updateNoSignalTierPulseFrequencyHz,
            onNoSignalToneVolumeChange = viewModel::updateNoSignalToneVolume,
            onNoSignalVibrationEnabledChange = viewModel::updateNoSignalVibrationEnabled,
            onNoSignalVoiceEnabledChange = viewModel::updateNoSignalVoiceEnabled,
            onNoSignalVoiceVolumeChange = viewModel::updateNoSignalVoiceVolume,
            onLimitedServiceTierPulseFrequencyChange = viewModel::updateLimitedServiceTierPulseFrequencyHz,
            onLimitedServiceTwoToneSpreadPercentChange = viewModel::updateLimitedServiceTwoToneSpreadPercent,
            onLimitedServiceToneVolumeChange = viewModel::updateLimitedServiceToneVolume,
            onLimitedServiceVoiceEnabledChange = viewModel::updateLimitedServiceVoiceEnabled,
            onLimitedServiceVoiceVolumeChange = viewModel::updateLimitedServiceVoiceVolume,
            onPreviewPingClick = viewModel::previewPingClickSound,
            onPreviewLowSignalClick = viewModel::previewLowSignalClickSound,
            onPreviewSignalPulse = viewModel::previewSignalPulseSound,
            onLevelRangeBcdClickVolumeChange = viewModel::updateLevelRangeBcdClickVolume,
            onLevelRangeBcdPulseFrequencyChange = viewModel::updateLevelRangeBcdPulseFrequencyHz,
            onPreviewLevelRangeBcdClick = viewModel::previewLevelRangeBcdClickSound,
            onPreviewCellChangeBell = viewModel::previewCellChangeBellSound,
            onPreviewCellChangeVoice = viewModel::previewCellChangeVoiceSound,
            onPreviewTechnologyChangeTone = viewModel::previewTechnologyChangeSound,
            onPreviewTechnologyChangeVoice = viewModel::previewTechnologyChangeVoiceSound,
            onPreviewTier5Announcer = viewModel::previewTier5AnnouncerSound,
            onPreviewNoSignalTone = viewModel::previewNoSignalToneSound,
            onPreviewNoSignalVoice = viewModel::previewNoSignalVoiceSound,
            onPreviewLimitedServiceTone = viewModel::previewLimitedServiceToneSound,
            onPreviewLimitedServiceVoice = viewModel::previewLimitedServiceVoiceSound,
            onPreviewRsrqWhiteNoise = viewModel::previewRsrqWhiteNoiseSound,
            onResetAudioVolumes = viewModel::resetAudioVolumes,
            onSaveSettingsProfile = viewModel::saveSettingsProfile,
            onLoadSettingsProfile = viewModel::loadSettingsProfile,
            onDeleteSettingsProfile = viewModel::deleteSettingsProfile,
            onImportSettingsProfile = onImportSettingsProfile,
            onShareSettingsProfile = onShareSettingsProfile,
            onExportSettingsProfileToDownloads = viewModel::exportSettingsProfileToDownloads,
            radioDebugEnabled = radioDebugEnabled,
            radioDebugLineCount = radioDebugLineCount,
            radioDebugSnapshot = radioDebugSnapshot,
            onRadioDebugEnabledChange = viewModel::updateRadioDebugEnabled,
            onShareRadioDebugLog = onShareRadioDebugLog,
            onClearRadioDebugLog = viewModel::clearRadioDebugLog,
            specialCellsActions = SpecialCellsActions(
                onImport = onImportSpecialCells,
                onRestoreExample = viewModel::restoreExampleSpecialCells,
                onDetectionEnabledChange = viewModel::updateSpecialCellsDetectionEnabled,
                onVoiceEnabledChange = viewModel::updateSpecialCellsVoiceEnabled,
                onSpeakTypeChange = viewModel::updateSpecialCellsSpeakType,
                onSpeakSiteChange = viewModel::updateSpecialCellsSpeakSite,
                onSpeakSectorChange = viewModel::updateSpecialCellsSpeakSector,
                onPreviewVoice = viewModel::previewSpecialCellVoice
            ),
            onRequestCellIdentityPermission = onRequestCellIdentityPermission,
            carrierConfigSnapshot = carrierConfigSnapshot,
            onRefreshCarrierConfig = viewModel::refreshCarrierConfigSnapshot,
            appVersion = BuildConfig.VERSION_NAME,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
