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
import io.github.cloolalang.notspotdetector.viewmodel.MonitorViewModel

@Composable
fun MonitorApp(
    viewModel: MonitorViewModel,
    onRequestBatteryExemption: () -> Unit,
    onRequestCellIdentityPermission: () -> Unit,
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
    val settingsProfiles by viewModel.settingsProfiles.collectAsStateWithLifecycle()
    val rttHistory by viewModel.rttHistory.collectAsStateWithLifecycle()

    var batteryOptimizationDisabled by remember {
        mutableStateOf(viewModel.isBatteryOptimizationDisabled)
    }
    var phoneStatePermissionGranted by remember {
        mutableStateOf(viewModel.phoneStatePermissionGranted)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        batteryOptimizationDisabled = viewModel.isBatteryOptimizationDisabled
        phoneStatePermissionGranted = viewModel.phoneStatePermissionGranted
        viewModel.refreshCellularSignal()
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
            settingsProfiles = settingsProfiles,
            rttHistory = rttHistory,
            isRunning = isRunning,
            isBatteryOptimizationDisabled = batteryOptimizationDisabled,
            onStart = viewModel::startMonitoring,
            onStartPassiveOnly = viewModel::startPassiveOnlyMonitoring,
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
            onResetPassiveSignalSettings = viewModel::resetPassiveSignalSettings,
            onSubscriptionChange = viewModel::updateSelectedSubscription,
            onPingClickVolumeChange = viewModel::updatePingClickVolume,
            onLowSignalClickVolumeChange = viewModel::updateLowSignalClickVolume,
            onSignalPulseDurationChange = viewModel::updateSignalPulseDurationMs,
            onCellChangeBellVolumeChange = viewModel::updateCellChangeBellVolume,
            onTechnologyChangeVolumeChange = viewModel::updateTechnologyChangeVolume,
            onNoSignalToneVolumeChange = viewModel::updateNoSignalToneVolume,
            onLimitedServiceToneVolumeChange = viewModel::updateLimitedServiceToneVolume,
            onPassiveSoundSpeedChange = viewModel::updatePassiveSoundSpeed,
            onPreviewPingClick = viewModel::previewPingClickSound,
            onPreviewLowSignalClick = viewModel::previewLowSignalClickSound,
            onPreviewCellChangeBell = viewModel::previewCellChangeBellSound,
            onPreviewTechnologyChange = viewModel::previewTechnologyChangeSound,
            onPreviewNoSignalTone = viewModel::previewNoSignalToneSound,
            onPreviewLimitedServiceTone = viewModel::previewLimitedServiceToneSound,
            onResetAudioVolumes = viewModel::resetAudioVolumes,
            onSaveSettingsProfile = viewModel::saveSettingsProfile,
            onLoadSettingsProfile = viewModel::loadSettingsProfile,
            onDeleteSettingsProfile = viewModel::deleteSettingsProfile,
            onRequestBatteryExemption = onRequestBatteryExemption,
            onRequestCellIdentityPermission = onRequestCellIdentityPermission,
            appVersion = BuildConfig.VERSION_NAME,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
