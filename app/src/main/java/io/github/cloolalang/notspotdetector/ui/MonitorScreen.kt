package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.CarrierConfigSnapshot
import io.github.cloolalang.notspotdetector.model.CellReselectBandNamingStyle
import io.github.cloolalang.notspotdetector.model.ConnectivityStats
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.LteLayerResilienceReading
import io.github.cloolalang.notspotdetector.model.NetworkModePreference
import io.github.cloolalang.notspotdetector.model.NetworkServiceMode
import io.github.cloolalang.notspotdetector.model.PrimaryLayerDominance
import io.github.cloolalang.notspotdetector.model.primaryLayerDominance
import io.github.cloolalang.notspotdetector.model.formatHomeOperatorDisplay
import io.github.cloolalang.notspotdetector.model.formatVisitedOperatorDisplay
import io.github.cloolalang.notspotdetector.model.shouldBlankStaleCellIdentity
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.SettingsCompatibility
import io.github.cloolalang.notspotdetector.model.PingSettings
import io.github.cloolalang.notspotdetector.model.RsrpHistogramBinningMode
import io.github.cloolalang.notspotdetector.model.RsrpSample
import io.github.cloolalang.notspotdetector.model.RttSample
import io.github.cloolalang.notspotdetector.model.ProfileExportOutcome
import io.github.cloolalang.notspotdetector.model.ProfileImportResult
import io.github.cloolalang.notspotdetector.model.ProfileSaveResult
import io.github.cloolalang.notspotdetector.model.SettingsProfileSummary
import io.github.cloolalang.notspotdetector.model.SimSubscriptionOption
import io.github.cloolalang.notspotdetector.model.TechnologyChangeTarget
import io.github.cloolalang.notspotdetector.model.ThresholdSettings
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerChoice
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerOption
import io.github.cloolalang.notspotdetector.model.VoicePhraseFragment
import io.github.cloolalang.notspotdetector.model.VoicePhraseGroup
import io.github.cloolalang.notspotdetector.model.VoicePhraseOptions
import io.github.cloolalang.notspotdetector.model.SignalMeasurementTier
import io.github.cloolalang.notspotdetector.model.RSRQ_POOR_TIER_NUMBER
import io.github.cloolalang.notspotdetector.model.isRsrqPoor
import io.github.cloolalang.notspotdetector.model.resolveLimitedServiceSignalOverlayRxss
import io.github.cloolalang.notspotdetector.model.resolveSignalMeasurementTier
import io.github.cloolalang.notspotdetector.ui.theme.BondiBlue
import io.github.cloolalang.notspotdetector.ui.theme.OnBondiBlue
import io.github.cloolalang.notspotdetector.ui.theme.Sushi

@Composable
fun MonitorScreen(
    stats: ConnectivityStats,
    thresholds: ThresholdSettings,
    pingSettings: PingSettings,
    monitoringSettings: MonitoringSettings,
    passiveSignalSettings: PassiveSignalSettings,
    passiveMockSettings: PassiveMockSettings,
    simSubscriptions: List<SimSubscriptionOption>,
    phoneStatePermissionGranted: Boolean,
    audioVolumes: AudioVolumeSettings,
    voiceAnnouncerOptions: List<VoiceAnnouncerOption>,
    settingsProfiles: List<SettingsProfileSummary>,
    rttHistory: List<RttSample>,
    rsrpHistory: List<RsrpSample>,
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onGoodRttChange: (Long) -> Unit,
    onPoorRttChange: (Long) -> Unit,
    onPoorJitterChange: (Long) -> Unit,
    onPoorPacketLossChange: (Float) -> Unit,
    onSuppressClicksOnGoodChange: (Boolean) -> Unit,
    onGoodConnectionClicksPerPingChange: (Int) -> Unit,
    onResetThresholds: () -> Unit,
    onPingAddressChange: (String) -> Unit,
    onPingsPerTestChange: (Int) -> Unit,
    onTestIntervalChange: (Long) -> Unit,
    onMonitor2gFallbackChange: (Boolean) -> Unit,
    onPassiveQuietUntilCriticalChange: (Boolean) -> Unit,
    onPassiveSignalSettingsChange: (PassiveSignalSettings) -> Unit,
    onPassiveMockSettingsChange: (PassiveMockSettings) -> Unit,
    onPassiveMeasurementIntervalChange: (Long) -> Unit,
    onRsrpHistogramWindowChange: (Long) -> Unit,
    onRsrpHistogramBinningModeChange: (RsrpHistogramBinningMode) -> Unit = {},
    onRsrpHistogramThresholdChange: (Int, Int) -> Unit = { _, _ -> },
    onClearRsrpHistogram: () -> Unit = {},
    onResetPassiveSignalSettings: () -> Unit,
    onSubscriptionChange: (Int) -> Unit,
    onVoiceAnnouncerChoiceChange: (VoiceAnnouncerChoice) -> Unit,
    onRefreshVoiceAnnouncerOptions: () -> Unit,
    onPreviewVoiceAnnouncer: () -> Unit,
    onPingClickVolumeChange: (Float) -> Unit,
    onLowSignalClickVolumeChange: (Float) -> Unit,
    onSignalPulseFrequencyChange: (Int) -> Unit,
    onVeryStrongTierPulseFrequencyChange: (Int) -> Unit,
    onG2StrongTierPulseFrequencyChange: (Int) -> Unit,
    onG2WeakTierPulseFrequencyChange: (Int) -> Unit,
    onSignalPulseDurationChange: (Int) -> Unit,
    onMasterVoiceAnnouncementsEnabledChange: (Boolean) -> Unit,
    onVoicePhrasesChange: (VoicePhraseGroup, VoicePhraseOptions) -> Unit = { _, _ -> },
    onPreviewVoicePhrase: (VoicePhraseGroup, VoicePhraseFragment) -> Unit = { _, _ -> },
    onFiveGFeaturesEnabledChange: (Boolean) -> Unit = {},
    settingsUnlocked: Boolean = false,
    onSettingsUnlockedChange: (Boolean) -> Unit = {},
    onCellChangeBellVolumeChange: (Float) -> Unit,
    onCellChangeVoiceEnabledChange: (Boolean) -> Unit,
    onCellChangeVoiceVolumeChange: (Float) -> Unit,
    onCellChangeSpeakBandEnabledChange: (Boolean) -> Unit = {},
    onCellChangeBandNamingStyleChange: (CellReselectBandNamingStyle) -> Unit = {},
    onTechnologyChangeToneVolumeChange: (TechnologyChangeTarget, Float) -> Unit,
    onTechnologyChangeVoiceEnabledChange: (TechnologyChangeTarget, Boolean) -> Unit,
    onTechnologyChangeVoiceVolumeChange: (TechnologyChangeTarget, Float) -> Unit,
    onTier5AnnouncerEnabledChange: (Boolean) -> Unit,
    onTier5AnnouncerVolumeChange: (Float) -> Unit,
    onNoSignalTierPulseFrequencyChange: (Int) -> Unit,
    onNoSignalToneVolumeChange: (Float) -> Unit,
    onNoSignalVibrationEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceVolumeChange: (Float) -> Unit,
    onLimitedServiceTierPulseFrequencyChange: (Int) -> Unit,
    onLimitedServiceTwoToneSpreadPercentChange: (Int) -> Unit,
    onLimitedServiceToneVolumeChange: (Float) -> Unit,
    onLimitedServiceVoiceEnabledChange: (Boolean) -> Unit,
    onLimitedServiceVoiceVolumeChange: (Float) -> Unit,
    onPreviewPingClick: () -> Unit,
    onPreviewLowSignalClick: (frequencyHz: Int, pulseDurationMs: Int) -> Unit,
    onPreviewSignalPulse: (volume: Float, frequencyHz: Int, pulseDurationMs: Int) -> Unit,
    onLevelRangeBcdClickVolumeChange: (Float) -> Unit,
    onLevelRangeBcdPulseFrequencyChange: (Int) -> Unit,
    onPreviewLevelRangeBcdClick: (frequencyHz: Int, pulseDurationMs: Int) -> Unit,
    onPreviewCellChangeBell: () -> Unit,
    onPreviewCellChangeVoice: () -> Unit,
    onPreviewTechnologyChangeTone: (TechnologyChangeTarget) -> Unit,
    onPreviewTechnologyChangeVoice: (TechnologyChangeTarget) -> Unit,
    onPreviewTier5Announcer: () -> Unit,
    onPreviewNoSignalTone: () -> Unit,
    onPreviewNoSignalVoice: () -> Unit,
    onPreviewLimitedServiceTone: () -> Unit,
    onPreviewLimitedServiceVoice: () -> Unit,
    onPreviewRsrqWhiteNoise: () -> Unit,
    onResetAudioVolumes: () -> Unit,
    onSaveSettingsProfile: (String) -> ProfileSaveResult,
    onLoadSettingsProfile: (String) -> Unit,
    onDeleteSettingsProfile: (String) -> Unit,
    onImportSettingsProfile: (onResult: (ProfileImportResult) -> Unit) -> Unit,
    onShareSettingsProfile: (String) -> Unit,
    onExportSettingsProfileToDownloads: (String) -> ProfileExportOutcome,
    onRequestCellIdentityPermission: () -> Unit,
    appVersion: String,
    carrierConfigSnapshot: CarrierConfigSnapshot = CarrierConfigSnapshot(),
    onRefreshCarrierConfig: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalSettingsUnlocked provides settingsUnlocked) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            HomeTitleBar(
                stats = stats,
                passiveSignalSettings = passiveSignalSettings,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.app_version, appVersion),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }

        MonitoringControlButtons(
            isRunning = isRunning,
            onStart = onStart,
            onStop = onStop
        )

        if (isRunning) {
            HistogramControlsCard(
                samples = rsrpHistory,
                windowMs = monitoringSettings.rsrpHistogramWindowMs,
                binningMode = monitoringSettings.rsrpHistogramBinningMode,
                thresholdsDbm = monitoringSettings.rsrpHistogramThresholdsDbm(),
                isActive = isRunning,
                onWindowChange = onRsrpHistogramWindowChange,
                onBinningModeChange = onRsrpHistogramBinningModeChange,
                onThresholdChange = onRsrpHistogramThresholdChange,
                onClearHistogram = onClearRsrpHistogram
            )

            if (stats.isPassiveIdleMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Text(
                        text = stringResource(
                            if (stats.isPassiveOnlySession) {
                                R.string.passive_only_idle_hint
                            } else {
                                R.string.passive_idle_hint
                            }
                        ),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (stats.isPassiveOnlySession && passiveMockSettings.enabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.passive_mock_active_hint),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!stats.isPassiveOnlySession) {
                RttGraphCard(
                    samples = rttHistory,
                    isActive = isRunning && !stats.isPassiveIdleMode
                )
            }
            MetricsCard(
                stats = stats,
                monitoringSettings = monitoringSettings,
                passiveSignalSettings = passiveSignalSettings,
                passiveMockSettings = passiveMockSettings,
                onRequestCellIdentityPermission = onRequestCellIdentityPermission
            )
        }

        TopLevelSectionCard(title = stringResource(R.string.top_level_passive_mode_monitoring)) {
            MasterVoiceAnnouncementsToggle(
                enabled = audioVolumes.masterVoiceAnnouncementsEnabled,
                onEnabledChange = onMasterVoiceAnnouncementsEnabledChange
            )

            MonitoringSettingsCard(
                monitoringSettings = monitoringSettings,
                passiveSignalSettings = passiveSignalSettings,
                simSubscriptions = simSubscriptions,
                phoneStatePermissionGranted = phoneStatePermissionGranted,
                onMonitor2gFallbackChange = onMonitor2gFallbackChange,
                onPassiveQuietUntilCriticalChange = onPassiveQuietUntilCriticalChange,
                onPassiveSignalSettingsChange = onPassiveSignalSettingsChange,
                onSubscriptionChange = onSubscriptionChange,
                onPassiveMeasurementIntervalChange = onPassiveMeasurementIntervalChange
            )

            PassiveSignalSettingsCard(
                settings = passiveSignalSettings,
                audioVolumes = audioVolumes,
                previewEnabled = !isRunning,
                passiveMeasurementIntervalMs = monitoringSettings.passiveMeasurementIntervalMs,
                signalPulseDurationMs = audioVolumes.signalPulseDurationMs,
                veryStrongTierPulseFrequencyHz = audioVolumes.veryStrongTierPulseFrequencyHz,
                g2StrongTierPulseFrequencyHz = audioVolumes.g2StrongTierPulseFrequencyHz,
                g2WeakTierPulseFrequencyHz = audioVolumes.g2WeakTierPulseFrequencyHz,
                onSettingsChange = onPassiveSignalSettingsChange,
                onVeryStrongTierPulseFrequencyChange = onVeryStrongTierPulseFrequencyChange,
                onG2StrongTierPulseFrequencyChange = onG2StrongTierPulseFrequencyChange,
                onG2WeakTierPulseFrequencyChange = onG2WeakTierPulseFrequencyChange,
                onTechnologyChangeToneVolumeChange = onTechnologyChangeToneVolumeChange,
                onTechnologyChangeVoiceEnabledChange = onTechnologyChangeVoiceEnabledChange,
                onTechnologyChangeVoiceVolumeChange = onTechnologyChangeVoiceVolumeChange,
                onTier5AnnouncerEnabledChange = onTier5AnnouncerEnabledChange,
                onTier5AnnouncerVolumeChange = onTier5AnnouncerVolumeChange,
                onNoSignalTierPulseFrequencyChange = onNoSignalTierPulseFrequencyChange,
                onNoSignalToneVolumeChange = onNoSignalToneVolumeChange,
                onNoSignalVibrationEnabledChange = onNoSignalVibrationEnabledChange,
                onNoSignalVoiceEnabledChange = onNoSignalVoiceEnabledChange,
                onNoSignalVoiceVolumeChange = onNoSignalVoiceVolumeChange,
                onLimitedServiceTierPulseFrequencyChange = onLimitedServiceTierPulseFrequencyChange,
                onLimitedServiceTwoToneSpreadPercentChange = onLimitedServiceTwoToneSpreadPercentChange,
                onLimitedServiceToneVolumeChange = onLimitedServiceToneVolumeChange,
                onLimitedServiceVoiceEnabledChange = onLimitedServiceVoiceEnabledChange,
                onLimitedServiceVoiceVolumeChange = onLimitedServiceVoiceVolumeChange,
                onPreviewTechnologyChangeTone = onPreviewTechnologyChangeTone,
                onPreviewTechnologyChangeVoice = onPreviewTechnologyChangeVoice,
                onPreviewTier5Announcer = onPreviewTier5Announcer,
                onPreviewNoSignalTone = onPreviewNoSignalTone,
                onPreviewNoSignalVoice = onPreviewNoSignalVoice,
                onPreviewLimitedServiceTone = onPreviewLimitedServiceTone,
                onPreviewLimitedServiceVoice = onPreviewLimitedServiceVoice,
                onLowSignalClickVolumeChange = onLowSignalClickVolumeChange,
                onSignalPulseDurationChange = onSignalPulseDurationChange,
                onPreviewLowSignalClick = onPreviewLowSignalClick,
                onPreviewSignalPulse = onPreviewSignalPulse,
                onLevelRangeBcdClickVolumeChange = onLevelRangeBcdClickVolumeChange,
                onLevelRangeBcdPulseFrequencyChange = onLevelRangeBcdPulseFrequencyChange,
                onPreviewLevelRangeBcdClick = onPreviewLevelRangeBcdClick,
                onPreviewRsrqWhiteNoise = onPreviewRsrqWhiteNoise,
                onSignalPulseFrequencyChange = onSignalPulseFrequencyChange,
                onCellChangeBellVolumeChange = onCellChangeBellVolumeChange,
                onCellChangeVoiceEnabledChange = onCellChangeVoiceEnabledChange,
                onCellChangeVoiceVolumeChange = onCellChangeVoiceVolumeChange,
                onPreviewCellChangeBell = onPreviewCellChangeBell,
                onPreviewCellChangeVoice = onPreviewCellChangeVoice,
                onCellChangeSpeakBandEnabledChange = onCellChangeSpeakBandEnabledChange,
                onCellChangeBandNamingStyleChange = onCellChangeBandNamingStyleChange,
                onReset = onResetPassiveSignalSettings,
                fiveGFeaturesEnabled = monitoringSettings.fiveGFeaturesEnabled,
                onVoicePhrasesChange = onVoicePhrasesChange,
                onPreviewVoicePhrase = onPreviewVoicePhrase
            )
        }

        TopLevelSectionCard(title = stringResource(R.string.top_level_general_app_settings)) {
            SettingsLockBar(
                unlocked = settingsUnlocked,
                onUnlockedChange = onSettingsUnlockedChange
            )

            SettingsProfilesCard(
                profiles = settingsProfiles,
                onSaveProfile = onSaveSettingsProfile,
                onLoadProfile = onLoadSettingsProfile,
                onDeleteProfile = onDeleteSettingsProfile,
                onImportProfile = onImportSettingsProfile,
                onShareProfile = onShareSettingsProfile,
                onExportProfileToDownloads = onExportSettingsProfileToDownloads
            )

            GlobalVoiceSettingsCard(
                voiceAnnouncerChoice = audioVolumes.voiceAnnouncerChoice,
                voiceAnnouncerOptions = voiceAnnouncerOptions,
                previewEnabled = !isRunning,
                onVoiceAnnouncerChoiceChange = onVoiceAnnouncerChoiceChange,
                onRefreshVoiceAnnouncerOptions = onRefreshVoiceAnnouncerOptions,
                onPreviewVoiceAnnouncer = onPreviewVoiceAnnouncer
            )
        }

        TopLevelSectionCard(title = stringResource(R.string.top_level_development_area)) {
            FeaturesUnderDevelopmentCard(
                fiveGFeaturesEnabled = monitoringSettings.fiveGFeaturesEnabled,
                onFiveGFeaturesEnabledChange = onFiveGFeaturesEnabledChange
            ) {
                PingSettingsCard(
                    pingSettings = pingSettings,
                    onAddressChange = onPingAddressChange,
                    onPingsPerTestChange = onPingsPerTestChange,
                    onTestIntervalChange = onTestIntervalChange
                )

                ThresholdSettingsCard(
                    thresholds = thresholds,
                    onGoodRttChange = onGoodRttChange,
                    onPoorRttChange = onPoorRttChange,
                    onPoorJitterChange = onPoorJitterChange,
                    onPoorPacketLossChange = onPoorPacketLossChange,
                    onSuppressClicksOnGoodChange = onSuppressClicksOnGoodChange,
                    onGoodConnectionClicksPerPingChange = onGoodConnectionClicksPerPingChange,
                    onReset = onResetThresholds
                )

                AudioVolumeSettingsCard(
                    audioVolumes = audioVolumes,
                    previewEnabled = !isRunning,
                    signalPulsePreviewRepeatIntervalMs = SettingsCompatibility.resolveTierClickIntervalMs(
                        configuredMs = passiveSignalSettings.levelRangeAbcdClickIntervalMs.toLong(),
                        signalPulseDurationMs = audioVolumes.signalPulseDurationMs,
                        isPassiveOnlySession = true
                    ),
                    onPingClickVolumeChange = onPingClickVolumeChange,
                    onLowSignalClickVolumeChange = onLowSignalClickVolumeChange,
                    onSignalPulseFrequencyChange = onSignalPulseFrequencyChange,
                    onSignalPulseDurationChange = onSignalPulseDurationChange,
                    onPreviewPingClick = onPreviewPingClick,
                    onPreviewLowSignalClick = onPreviewLowSignalClick,
                    onReset = onResetAudioVolumes
                )
            }

            MockNetworkStateCard(
                mockSettings = passiveMockSettings,
                fiveGFeaturesEnabled = monitoringSettings.fiveGFeaturesEnabled,
                onMockSettingsChange = onPassiveMockSettingsChange
            )

            CarrierConfigCard(
                snapshot = carrierConfigSnapshot,
                phoneStatePermissionGranted = phoneStatePermissionGranted,
                onRefresh = onRefreshCarrierConfig
            )
        }

        if (!stats.cellularAvailable && isRunning) {
            Text(
                text = stringResource(R.string.waiting_for_cellular),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.size(8.dp))
    }
    }
}

@Composable
private fun MasterVoiceAnnouncementsToggle(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.audio_master_voice_announcements),
                    style = MaterialTheme.typography.titleMedium,
                    color = Sushi,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.audio_master_voice_announcements_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                enabled = settingsControlsEnabled()
            )
        }
    }
}

@Composable
private fun MonitoringControlButtons(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    if (isRunning) {
        Button(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(text = stringResource(R.string.stop_monitoring))
        }
    } else {
        // "Start monitoring" now starts passive (signal-only) monitoring — active ping-based
        // testing is being reworked and is temporarily surfaced only as the disabled placeholder
        // button below.
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = BondiBlue,
                contentColor = OnBondiBlue
            )
        ) {
            Text(text = stringResource(R.string.start_monitoring))
        }
        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.start_active_mode_testing))
        }
    }
}

@Composable
private fun MetricsCard(
    stats: ConnectivityStats,
    monitoringSettings: MonitoringSettings,
    passiveSignalSettings: PassiveSignalSettings,
    passiveMockSettings: PassiveMockSettings,
    onRequestCellIdentityPermission: () -> Unit
) {
    val mockActive = stats.isPassiveOnlySession && passiveMockSettings.enabled

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(
                    if (mockActive) R.string.metrics_title_mock else R.string.metrics_title
                ),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleSmall,
                color = if (mockActive) MaterialTheme.colorScheme.error else Sushi,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            MetricRow(
                label = stringResource(R.string.metric_sim),
                value = formatSimMetric(stats, monitoringSettings),
                valueFontSize = 13.sp,
                valueSingleLine = true
            )
            MetricRow(
                label = stringResource(R.string.metric_home_operator),
                value = formatHomeOperator(stats)
            )
            stats.formatVisitedOperatorDisplay()?.let { visited ->
                MetricRow(
                    label = stringResource(R.string.metric_visited_operator),
                    value = visited
                )
            }
            MetricRow(
                label = stringResource(R.string.metric_network_mode),
                value = formatNetworkModePreference(stats)
            )
            MetricRow(
                label = stringResource(R.string.metric_service_state),
                value = formatServiceState(stats)
            )
            MetricRow(
                label = stringResource(R.string.metric_radio),
                value = formatRadioAccessType(stats)
            )
            CellIdentityMetrics(stats = stats, passiveSignalSettings = passiveSignalSettings)
            if (!stats.cellIdentityPermissionGranted) {
                Text(
                    text = stringResource(R.string.cell_identity_permission_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = onRequestCellIdentityPermission) {
                    Text(stringResource(R.string.cell_identity_grant))
                }
            }
            if (!stats.isPassiveOnlySession) {
                MetricRow(
                    label = stringResource(R.string.metric_rtt),
                    value = stats.rttMs?.let { "$it ms" } ?: "—"
                )
                MetricRow(
                    label = stringResource(R.string.metric_jitter),
                    value = "${stats.jitterMs} ms"
                )
                MetricRow(
                    label = stringResource(R.string.metric_packet_loss),
                    value = String.format("%.1f%%", stats.packetLossPercent)
                )
                MetricRow(
                    label = stringResource(R.string.metric_pings),
                    value = "${stats.pingsSent - stats.pingsFailed}/${stats.pingsSent}"
                )
            }
            MetricRow(
                label = when (stats.radioAccessType) {
                    CellularSignalReader.RADIO_5G_ENDC -> stringResource(R.string.metric_rsrp_lte)
                    CellularSignalReader.RADIO_2G -> stringResource(R.string.metric_signal_dbm_2g)
                    else -> stringResource(R.string.metric_rsrp)
                },
                value = formatSignalValue(
                    value = stats.rsrpDbm,
                    unit = "dBm",
                    permissionGranted = stats.signalPermissionGranted
                )
            )
            if (stats.radioAccessType != CellularSignalReader.RADIO_2G) {
                MetricRow(
                    label = if (stats.radioAccessType == CellularSignalReader.RADIO_5G_ENDC) {
                        stringResource(R.string.metric_rsrq_lte)
                    } else {
                        stringResource(R.string.metric_rsrq)
                    },
                    value = formatSignalValue(
                        value = stats.rsrqDb,
                        unit = "dB",
                        permissionGranted = stats.signalPermissionGranted
                    )
                )
            }
            SignalTierMetricRow(
                tier = stats.resolveSignalMeasurementTier(passiveSignalSettings),
                rsrqTierActive = stats.isRsrqPoor(passiveSignalSettings),
                limitedServiceSignalOverlayRxss = stats.resolveLimitedServiceSignalOverlayRxss(
                    passiveSignalSettings
                )
            )
        }
    }
}

@Composable
private fun CellIdentityMetrics(
    stats: ConnectivityStats,
    passiveSignalSettings: PassiveSignalSettings
) {
    val permissionGranted = stats.cellIdentityPermissionGranted
    // Home no-signal RXSS (dead zone, LTE/NR/2G no signal, searching 2G, WiFi calling, etc.) can
    // still carry a stale EARFCN/PCI/BSIC/band forward from CellIdentityStabilizer's coalescing —
    // that cell is no longer valid, so blank these fields out rather than show last-known values.
    // Limited-service camp (including visited 4G / RXSS 20) still has a current SOS cell.
    val staleNoSignal = stats.shouldBlankStaleCellIdentity(passiveSignalSettings)
    val lteEarfcn = stats.lteEarfcn.takeUnless { staleNoSignal }
    val ltePci = stats.ltePci.takeUnless { staleNoSignal }
    val nrEarfcn = stats.nrEarfcn.takeUnless { staleNoSignal }
    val nrPci = stats.nrPci.takeUnless { staleNoSignal }
    val nrBand = stats.nrBand.takeUnless { staleNoSignal }
    val gsmEarfcn = stats.gsmEarfcn.takeUnless { staleNoSignal }
    val gsmBsic = stats.gsmBsic.takeUnless { staleNoSignal }
    when (stats.radioAccessType) {
        CellularSignalReader.RADIO_5G_ENDC -> {
            MetricRow(
                label = stringResource(R.string.metric_lte_earfcn),
                value = formatCellIdentityValue(lteEarfcn, permissionGranted)
            )
            MetricRow(
                label = stringResource(R.string.metric_lte_pci),
                value = formatCellIdentityValue(ltePci, permissionGranted)
            )
            MetricRow(
                label = stringResource(R.string.metric_nr_arfcn),
                value = formatCellIdentityValue(nrEarfcn, permissionGranted)
            )
            MetricRow(
                label = stringResource(R.string.metric_nr_pci),
                value = formatCellIdentityValue(nrPci, permissionGranted)
            )
            MetricRow(
                label = stringResource(R.string.metric_nr_band),
                value = formatNrBandValue(nrBand, permissionGranted)
            )
        }
        CellularSignalReader.RADIO_5G -> {
            MetricRow(
                label = stringResource(R.string.metric_nr_arfcn),
                value = formatCellIdentityValue(nrEarfcn, permissionGranted)
            )
            MetricRow(
                label = stringResource(R.string.metric_nr_pci),
                value = formatCellIdentityValue(nrPci, permissionGranted)
            )
            MetricRow(
                label = stringResource(R.string.metric_nr_band),
                value = formatNrBandValue(nrBand, permissionGranted)
            )
        }
        CellularSignalReader.RADIO_2G -> {
            MetricRow(
                label = stringResource(R.string.metric_gsm_arfcn),
                value = formatCellIdentityValue(gsmEarfcn, permissionGranted)
            )
            MetricRow(
                label = stringResource(R.string.metric_gsm_bsic),
                value = formatCellIdentityValue(gsmBsic, permissionGranted)
            )
        }
        else -> {
            MetricRow(
                label = stringResource(R.string.metric_earfcn),
                value = formatCellIdentityValue(lteEarfcn, permissionGranted)
            )
            MetricRow(
                label = stringResource(R.string.metric_pci),
                value = formatCellIdentityValue(ltePci, permissionGranted)
            )
        }
    }

    // Neighbour-layer counts are not meaningful without a camped LTE cell — hide the same
    // leftover allCellInfo readings that no-signal / dead zone / WiFi calling already blank
    // for EARFCN/PCI.
    val lteLayerResilience = stats.lteLayerResilience.takeUnless {
        staleNoSignal || stats.isWifiCallingActive || stats.isCompleteNoService
    }
    // RSRP gap between the primary sector and the next-strongest sector on the *same* EARFCN
    // (see LteLayerResilienceReading.primaryLayerDominanceDb) — undefined ("—") when there's no
    // competing intra-channel sector detected to compare against.
    MetricRow(
        label = stringResource(R.string.metric_primary_layer_dominance),
        value = formatPrimaryLayerDominanceValue(lteLayerResilience, permissionGranted)
    )
    MetricRow(
        label = stringResource(R.string.metric_primary_layer_resilience),
        value = formatCellIdentityValue(lteLayerResilience?.primaryLayerCellCount, permissionGranted)
    )
    MetricRow(
        label = stringResource(R.string.metric_alternate_layer_resilience),
        value = if (!permissionGranted) {
            stringResource(R.string.cell_identity_permission_required)
        } else if (lteLayerResilience == null) {
            "—"
        } else {
            stringResource(
                R.string.metric_alternate_layer_resilience_value,
                lteLayerResilience.alternateLayerCellCount,
                lteLayerResilience.alternateLayerCount
            )
        }
    )
}

@Composable
private fun formatPrimaryLayerDominanceValue(
    lteLayerResilience: LteLayerResilienceReading?,
    permissionGranted: Boolean
): String {
    if (!permissionGranted) {
        return stringResource(R.string.cell_identity_permission_required)
    }
    val dominanceDb = lteLayerResilience?.primaryLayerDominanceDb ?: return "—"
    val dominance = lteLayerResilience.primaryLayerDominance() ?: return "—"
    val dominanceLabel = when (dominance) {
        PrimaryLayerDominance.LOW -> stringResource(R.string.primary_layer_dominance_low)
        PrimaryLayerDominance.HIGH -> stringResource(R.string.primary_layer_dominance_high)
    }
    return stringResource(R.string.metric_primary_layer_dominance_value, dominanceDb, dominanceLabel)
}

@Composable
private fun formatSimMetric(
    stats: ConnectivityStats,
    monitoringSettings: MonitoringSettings
): String {
    if (!stats.signalPermissionGranted) {
        return stringResource(R.string.signal_permission_required)
    }

    if (monitoringSettings.subscriptionId == MonitoringSettings.DEFAULT_SUBSCRIPTION_ID) {
        val resolved = stats.simDisplayName
        return if (resolved != null) {
            stringResource(R.string.monitoring_sim_system_default_resolved, resolved)
        } else {
            stringResource(R.string.monitoring_sim_system_default)
        }
    }

    return stats.simDisplayName ?: stringResource(R.string.signal_unavailable)
}

@Composable
private fun formatServiceState(stats: ConnectivityStats): String {
    if (!stats.signalPermissionGranted) {
        return stringResource(R.string.signal_permission_required)
    }
    return when (stats.networkServiceMode) {
        NetworkServiceMode.IN_SERVICE -> stringResource(R.string.service_state_in_service)
        NetworkServiceMode.LIMITED_SERVICE -> stringResource(R.string.service_state_limited)
        NetworkServiceMode.OUT_OF_SERVICE,
        NetworkServiceMode.RADIO_OFF -> stringResource(R.string.service_state_no_service)
        NetworkServiceMode.UNKNOWN -> stringResource(R.string.service_state_unknown)
    }
}

@Composable
private fun formatRadioAccessType(stats: ConnectivityStats): String {
    stats.radioAccessType?.let { return it }
    if (stats.isWifiCallingActive) {
        return stringResource(R.string.metric_radio_wifi_calling)
    }
    return stringResource(R.string.signal_unavailable)
}

@Composable
private fun formatHomeOperator(stats: ConnectivityStats): String {
    if (!stats.cellularAvailable && stats.formatHomeOperatorDisplay() == null) {
        return stringResource(R.string.network_waiting)
    }
    stats.formatHomeOperatorDisplay()?.let { return it }
    if (!stats.signalPermissionGranted) {
        return stringResource(R.string.network_permission_required)
    }
    return stringResource(R.string.network_cellular)
}

@Composable
private fun formatNetworkModePreference(stats: ConnectivityStats): String {
    if (!stats.signalPermissionGranted) {
        return stringResource(R.string.signal_permission_required)
    }
    return stringResource(
        when (stats.networkModePreference) {
            NetworkModePreference.ALL_TECHNOLOGIES -> R.string.network_mode_all_technologies
            NetworkModePreference.FORCED_2G -> R.string.network_mode_forced_2g
            NetworkModePreference.FORCED_LTE_NR -> R.string.network_mode_forced_lte_nr
            NetworkModePreference.FORCED_NR_ONLY -> R.string.network_mode_forced_nr
            NetworkModePreference.UNKNOWN -> R.string.network_mode_unknown
        }
    )
}

@Composable
private fun formatSignalValue(
    value: Int?,
    unit: String,
    permissionGranted: Boolean
): String {
    if (!permissionGranted) {
        return stringResource(R.string.signal_permission_required)
    }
    if (value == null) {
        return "—"
    }
    return "$value $unit"
}

@Composable
private fun formatCellIdentityValue(
    value: Int?,
    permissionGranted: Boolean
): String {
    if (!permissionGranted) {
        return stringResource(R.string.cell_identity_permission_required)
    }
    if (value == null) {
        return "—"
    }
    return value.toString()
}

/** Formats an NR band number as its 3GPP band name, e.g. 78 -> "n78". */
@Composable
private fun formatNrBandValue(
    value: Int?,
    permissionGranted: Boolean
): String {
    if (!permissionGranted) {
        return stringResource(R.string.cell_identity_permission_required)
    }
    if (value == null) {
        return "—"
    }
    return "n$value"
}

@Composable
private fun SignalTierMetricRow(
    tier: SignalMeasurementTier,
    rsrqTierActive: Boolean,
    limitedServiceSignalOverlayRxss: Int? = null
) {
    MetricRow(
        label = stringResource(R.string.metric_signal_tier),
        value = signalMeasurementTierLabel(tier, rsrqTierActive, limitedServiceSignalOverlayRxss),
        valueColor = signalMeasurementTierColor(tier, rsrqTierActive)
    )
}

@Composable
private fun signalMeasurementTierLabel(
    tier: SignalMeasurementTier,
    rsrqTierActive: Boolean,
    limitedServiceSignalOverlayRxss: Int? = null
): String = signalMeasurementDisplayLabel(tier, rsrqTierActive, limitedServiceSignalOverlayRxss)

@Composable
private fun signalMeasurementTierColor(
    tier: SignalMeasurementTier,
    rsrqTierActive: Boolean
): Color {
    if (rsrqTierActive && tier.rxssNumber == null) {
        return SignalTierColors.forMeasurementTier(SignalMeasurementTier.RSRQ_POOR)
    }
    return SignalTierColors.forMeasurementTier(tier)
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueFontSize: TextUnit = 18.sp,
    valueSingleLine: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = BondiBlue,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = value,
            modifier = if (valueSingleLine) {
                Modifier.weight(1f)
            } else {
                Modifier
            },
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            fontSize = valueFontSize,
            color = valueColor,
            fontWeight = if (valueColor != MaterialTheme.colorScheme.onSurface) {
                FontWeight.SemiBold
            } else {
                FontWeight.Normal
            },
            textAlign = if (valueSingleLine) TextAlign.End else TextAlign.Unspecified,
            maxLines = if (valueSingleLine) 1 else Int.MAX_VALUE,
            softWrap = !valueSingleLine,
            overflow = if (valueSingleLine) TextOverflow.Ellipsis else TextOverflow.Clip
        )
    }
}
