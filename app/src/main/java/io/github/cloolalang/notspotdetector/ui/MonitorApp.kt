package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.cloolalang.notspotdetector.BuildConfig
import io.github.cloolalang.notspotdetector.data.GithubAppUpdate
import io.github.cloolalang.notspotdetector.model.AppUpdateControls
import io.github.cloolalang.notspotdetector.model.AppUpdateStatus
import io.github.cloolalang.notspotdetector.model.ProfileImportResult
import io.github.cloolalang.notspotdetector.model.SpecialCellsImportResult
import io.github.cloolalang.notspotdetector.util.AppUpdateInstallResult
import io.github.cloolalang.notspotdetector.util.AppUpdateInstaller
import io.github.cloolalang.notspotdetector.viewmodel.MonitorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun MonitorApp(
    viewModel: MonitorViewModel,
    onRequestBatteryExemption: () -> Unit,
    onRequestCellIdentityPermission: () -> Unit,
    onImportSettingsProfile: (onResult: (ProfileImportResult) -> Unit) -> Unit,
    onShareSettingsProfile: (String) -> Unit,
    onShareRadioDebugLog: () -> Unit,
    onImportSpecialCells: (onResult: (SpecialCellsImportResult) -> Unit) -> Unit,
    onInstallAppUpdate: (File) -> AppUpdateInstallResult,
    onRequestCallPermission: () -> Unit,
    onRequestDialerRole: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val multiUe by viewModel.multiUe.collectAsStateWithLifecycle()
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
    val inhibit2gBusy by viewModel.inhibit2gBusy.collectAsStateWithLifecycle()
    val inhibit2gFailed by viewModel.inhibit2gFailed.collectAsStateWithLifecycle()
    val callTestSettings by viewModel.callTestSettings.collectAsStateWithLifecycle()
    val callTestStatus by viewModel.callTestStatus.collectAsStateWithLifecycle()

    var phoneStatePermissionGranted by remember {
        mutableStateOf(viewModel.phoneStatePermissionGranted)
    }
    var settingsUnlocked by remember { mutableStateOf(false) }
    val settingsUnlockIdleModifier = rememberSettingsUnlockIdleLock(
        unlocked = settingsUnlocked,
        onLock = { settingsUnlocked = false }
    )

    LaunchedEffect(settingsUnlocked) {
        if (!settingsUnlocked) {
            viewModel.disablePassiveMockMode()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        phoneStatePermissionGranted = viewModel.phoneStatePermissionGranted
        viewModel.reloadSettingsProfiles()
        viewModel.refreshCellularSignal()
        viewModel.refreshCarrierConfigSnapshot()
        viewModel.refreshMultiUeLink()
    }

    val context = LocalContext.current
    val updateScope = rememberCoroutineScope()
    var updateStatus by remember { mutableStateOf<AppUpdateStatus>(AppUpdateStatus.Idle) }
    var downloadedUpdate by remember { mutableStateOf<File?>(null) }
    var downloadedVersion by remember { mutableStateOf<String?>(null) }

    fun checkForUpdate(fromUser: Boolean) {
        val current = updateStatus
        if (current is AppUpdateStatus.Checking || current is AppUpdateStatus.Downloading) return
        updateScope.launch {
            if (fromUser) updateStatus = AppUpdateStatus.Checking
            val result = withContext(Dispatchers.IO) {
                GithubAppUpdate.check(BuildConfig.VERSION_NAME)
            }
            updateStatus = when (result) {
                AppUpdateStatus.UpToDate,
                is AppUpdateStatus.Failed -> if (fromUser) result else AppUpdateStatus.Idle
                else -> result
            }
        }
    }

    fun installDownloaded() {
        val file = downloadedUpdate ?: return
        val versionName = downloadedVersion ?: BuildConfig.VERSION_NAME
        updateStatus = when (onInstallAppUpdate(file)) {
            AppUpdateInstallResult.NeedPermission -> AppUpdateStatus.NeedPermission
            AppUpdateInstallResult.Failed ->
                AppUpdateStatus.Failed(AppUpdateStatus.Reason.Network)
            AppUpdateInstallResult.Started -> AppUpdateStatus.Ready(versionName)
        }
    }

    fun downloadAndInstall() {
        val available = updateStatus as? AppUpdateStatus.Available ?: return
        updateScope.launch {
            updateStatus = AppUpdateStatus.Downloading
            val destination = AppUpdateInstaller.updateFile(context.cacheDir)
            val saved = withContext(Dispatchers.IO) {
                GithubAppUpdate.download(available.apkUrl, destination)
            }
            if (!saved) {
                updateStatus = AppUpdateStatus.Failed(AppUpdateStatus.Reason.Network)
                return@launch
            }
            downloadedUpdate = destination
            downloadedVersion = available.versionName
            updateStatus = AppUpdateStatus.Ready(available.versionName)
            installDownloaded()
        }
    }

    LaunchedEffect(Unit) {
        checkForUpdate(fromUser = false)
    }

    KeepScreenOn(enabled = isRunning && monitoringSettings.keepScreenOnWhileMonitoring)

    LaunchedEffect(
        callTestStatus.needsCallPermission,
        callTestStatus.needsDialer,
        multiUe.followsMaster
    ) {
        if (!multiUe.followsMaster) return@LaunchedEffect
        when {
            callTestStatus.needsCallPermission -> onRequestCallPermission()
            callTestStatus.needsDialer -> onRequestDialerRole()
        }
    }

    fun ensureBackgroundMonitoringEnabled() {
        if (!viewModel.isBatteryOptimizationDisabled) {
            onRequestBatteryExemption()
        }
    }

    Scaffold(modifier = modifier.fillMaxSize().then(settingsUnlockIdleModifier)) { innerPadding ->
        MonitorScreen(
            stats = stats,
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
                ensureBackgroundMonitoringEnabled()
                viewModel.startPassiveOnlyMonitoring()
            },
            onStartActiveMode = {
                ensureBackgroundMonitoringEnabled()
                viewModel.startMonitoring()
            },
            onStop = viewModel::stopMonitoring,
            onStartCallTest = {
                ensureBackgroundMonitoringEnabled()
                when {
                    !viewModel.hasCallTestPermissions() -> onRequestCallPermission()
                    !viewModel.isDefaultDialer() -> onRequestDialerRole()
                    else -> viewModel.startCallTest()
                }
            },
            onStopCallTest = viewModel::stopCallTest,
            onPingAddressChange = viewModel::updatePingAddress,
            onPingsPerTestChange = viewModel::updatePingsPerTest,
            onTestIntervalChange = viewModel::updateTestIntervalMs,
            onActiveTimeLimitChange = viewModel::updateActiveTimeLimitEnabled,
            onMonitor2gFallbackChange = viewModel::updateMonitor2gFallback,
            onPassiveSignalSettingsChange = viewModel::updatePassiveSignalSettings,
            onPassiveMockSettingsChange = viewModel::updatePassiveMockSettings,
            onRsrpHistogramWindowChange = viewModel::updateRsrpHistogramWindowMs,
            onTapeWindowChange = viewModel::updateTapeWindowMs,
            onRsrpHistogramBinningModeChange = viewModel::updateRsrpHistogramBinningMode,
            onRsrpHistogramThresholdChange = viewModel::updateRsrpHistogramThresholdDbm,
            onClearRsrpHistogram = viewModel::clearGraphs,
            onClearKpiHistory = viewModel::clearKpiHistory,
            onApplyMacroDefaults = viewModel::applyMacroDefaults,
            onSubscriptionChange = viewModel::updateSelectedSubscription,
            onMobileDataEnabledChange = viewModel::setMobileDataEnabled,
            onShowManualSelectOperatorButtonChange = viewModel::updateShowManualSelectOperatorButton,
            onKeepScreenOnWhileMonitoringChange = viewModel::updateKeepScreenOnWhileMonitoring,
            onInhibit2gChange = viewModel::setInhibit2g,
            inhibit2gBusy = inhibit2gBusy,
            inhibit2gFailed = inhibit2gFailed,
            onOpenNetworkOperatorPicker = viewModel::openNetworkOperatorPicker,
            onVoiceAnnouncerChoiceChange = viewModel::updateVoiceAnnouncerChoice,
            onVoiceSpeechRateChange = viewModel::updateVoiceSpeechRate,
            onRefreshVoiceAnnouncerOptions = viewModel::refreshVoiceAnnouncerOptions,
            onPreviewVoiceAnnouncer = viewModel::previewVoiceAnnouncerSound,
            onPingClickVolumeChange = viewModel::updatePingClickVolume,
            onPingClickFrequencyChange = viewModel::updatePingClickFrequencyHz,
            onPingClickDurationChange = viewModel::updatePingClickDurationMs,
            onLowSignalClickVolumeChange = viewModel::updateLowSignalClickVolume,
            onSignalPulseFrequencyChange = viewModel::updateSignalPulseFrequencyHz,
            onVeryStrongTierPulseFrequencyChange = viewModel::updateVeryStrongTierPulseFrequencyHz,
            onG2StrongTierPulseFrequencyChange = viewModel::updateG2StrongTierPulseFrequencyHz,
            onG2WeakTierPulseFrequencyChange = viewModel::updateG2WeakTierPulseFrequencyHz,
            onSignalPulseDurationChange = viewModel::updateSignalPulseDurationMs,
            onMasterVoiceAnnouncementsEnabledChange = viewModel::updateMasterVoiceAnnouncementsEnabled,
            onMasterOtherSoundsEnabledChange = viewModel::updateMasterOtherSoundsEnabled,
            onVoicePhrasesChange = viewModel::updateVoicePhrases,
            onPreviewVoicePhrase = viewModel::previewVoicePhrase,
            onPeriodicVoiceRepeatChange = viewModel::updatePeriodicVoiceRepeat,
            onFiveGFeaturesEnabledChange = viewModel::updateFiveGFeaturesEnabled,
            settingsUnlocked = settingsUnlocked,
            onSettingsUnlockedChange = { settingsUnlocked = it },
            onAppBrandChange = viewModel::updateAppBrand,
            onCustomAppTitleChange = viewModel::updateCustomAppTitle,
            onClearAllCharts = viewModel::clearAllCharts,
            callTestSettings = callTestSettings,
            callTestStatus = callTestStatus,
            onCallNumberChange = viewModel::updateCallNumber,
            onCallDurationChange = viewModel::updateCallDurationSec,
            onCallDelayChange = viewModel::updateCallDelaySec,
            onWalkRouteEnabledChange = viewModel::updateWalkRouteEnabled,
            onCellChangeBellVolumeChange = viewModel::updateCellChangeBellVolume,
            onCellChangeVoiceEnabledChange = viewModel::updateCellChangeVoiceEnabled,
            onCellChangeVoiceVolumeChange = viewModel::updateCellChangeVoiceVolume,
            onCellChangeSpeakBandEnabledChange = viewModel::updateCellChangeSpeakBandEnabled,
            onCellChangeBandNamingStyleChange = viewModel::updateCellChangeBandNamingStyle,
            onCellReselectAlertScopeChange = viewModel::updateCellReselectAlertScope,
            onTimingAdvanceVoiceEnabledChange = viewModel::updateTimingAdvanceVoiceEnabled,
            onTimingAdvanceVoiceVolumeChange = viewModel::updateTimingAdvanceVoiceVolume,
            onPreviewTimingAdvanceVoice = viewModel::previewTimingAdvanceVoiceSound,
            onTechnologyChangeSoundEnabledChange = viewModel::updateTechnologyChangeSoundEnabled,
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
            onLevelRangeCPulseFrequencyChange = viewModel::updateLevelRangeCPulseFrequencyHz,
            onLevelRangeDPulseFrequencyChange = viewModel::updateLevelRangeDPulseFrequencyHz,
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
            onSaveDatedSettingsProfileToDownloads = viewModel::saveDatedSettingsProfileToDownloads,
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
            multiUe = multiUe,
            multiUeActions = MultiUeActions(
                onRoleChange = viewModel::updateMultiUeRole,
                onConnect = viewModel::connectMultiUeMaster,
                onLookForMaster = viewModel::lookForMultiUeMaster,
                onDisconnect = viewModel::disconnectMultiUeMaster,
                onRefresh = viewModel::refreshMultiUeLink,
                onMasterSyncSettingsChange = viewModel::updateMultiUeMasterSync,
                onSlaveSyncSettingsChange = viewModel::updateMultiUeSlaveSync
            ),
            appVersion = BuildConfig.VERSION_NAME,
            appUpdate = AppUpdateControls(
                status = updateStatus,
                onCheck = { checkForUpdate(fromUser = true) },
                onDownloadAndInstall = ::downloadAndInstall,
                onInstall = ::installDownloaded
            ),
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        val previous = view.keepScreenOn
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = previous }
    }
}
