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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import io.github.cloolalang.notspotdetector.model.ConnectivityStats
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.NetworkServiceMode
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.PingSettings
import io.github.cloolalang.notspotdetector.model.RsrpSample
import io.github.cloolalang.notspotdetector.model.RttSample
import io.github.cloolalang.notspotdetector.model.ProfileImportResult
import io.github.cloolalang.notspotdetector.model.ProfileSaveResult
import io.github.cloolalang.notspotdetector.model.SettingsProfileSummary
import io.github.cloolalang.notspotdetector.model.SimSubscriptionOption
import io.github.cloolalang.notspotdetector.model.ThresholdSettings
import io.github.cloolalang.notspotdetector.model.SignalMeasurementTier
import io.github.cloolalang.notspotdetector.model.resolveSignalMeasurementTier

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
    settingsProfiles: List<SettingsProfileSummary>,
    rttHistory: List<RttSample>,
    rsrpHistory: List<RsrpSample>,
    isRunning: Boolean,
    onStart: () -> Unit,
    onStartPassiveOnly: () -> Unit,
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
    onResetPassiveSignalSettings: () -> Unit,
    onSubscriptionChange: (Int) -> Unit,
    onPingClickVolumeChange: (Float) -> Unit,
    onLowSignalClickVolumeChange: (Float) -> Unit,
    onSignalPulseFrequencyChange: (Int) -> Unit,
    onSignalPulseDurationChange: (Int) -> Unit,
    onCellChangeBellVolumeChange: (Float) -> Unit,
    onCellChangeVoiceEnabledChange: (Boolean) -> Unit,
    onCellChangeVoiceVolumeChange: (Float) -> Unit,
    onTechnologyChangeVolumeChange: (Float) -> Unit,
    onTechnologyChangeVoiceEnabledChange: (Boolean) -> Unit,
    onTechnologyChangeVoiceVolumeChange: (Float) -> Unit,
    onNoSignalToneVolumeChange: (Float) -> Unit,
    onNoSignalVoiceEnabledChange: (Boolean) -> Unit,
    onNoSignalVoiceVolumeChange: (Float) -> Unit,
    onLimitedServiceToneVolumeChange: (Float) -> Unit,
    onLimitedServiceVoiceEnabledChange: (Boolean) -> Unit,
    onLimitedServiceVoiceVolumeChange: (Float) -> Unit,
    onPreviewPingClick: () -> Unit,
    onPreviewLowSignalClick: () -> Unit,
    onPreviewCellChangeBell: () -> Unit,
    onPreviewCellChangeVoice: () -> Unit,
    onPreviewTechnologyChange: () -> Unit,
    onPreviewTechnologyChangeVoice: () -> Unit,
    onPreviewNoSignalTone: () -> Unit,
    onPreviewNoSignalVoice: () -> Unit,
    onPreviewLimitedServiceTone: () -> Unit,
    onPreviewLimitedServiceVoice: () -> Unit,
    onResetAudioVolumes: () -> Unit,
    onSaveSettingsProfile: (String) -> ProfileSaveResult,
    onLoadSettingsProfile: (String) -> Unit,
    onDeleteSettingsProfile: (String) -> Unit,
    onImportSettingsProfile: (onResult: (ProfileImportResult) -> Unit) -> Unit,
    onShareSettingsProfile: (String) -> Unit,
    onRequestCellIdentityPermission: () -> Unit,
    appVersion: String,
    modifier: Modifier = Modifier
) {
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
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
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
            passiveMockEnabled = passiveMockSettings.enabled,
            onStart = onStart,
            onStartPassiveOnly = onStartPassiveOnly,
            onStop = onStop
        )

        RsrpHistogramCard(
            samples = rsrpHistory,
            windowMs = monitoringSettings.rsrpHistogramWindowMs,
            isActive = isRunning,
            onWindowChange = onRsrpHistogramWindowChange
        )

        if (isRunning) {
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
            } else if (stats.isPassiveOnlySession) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Text(
                        text = stringResource(
                            if (passiveMockSettings.enabled) {
                                R.string.passive_mock_active_hint
                            } else {
                                R.string.passive_only_active_hint
                            }
                        ),
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
                onRequestCellIdentityPermission = onRequestCellIdentityPermission
            )
        }

        SettingsProfilesCard(
            profiles = settingsProfiles,
            onSaveProfile = onSaveSettingsProfile,
            onLoadProfile = onLoadSettingsProfile,
            onDeleteProfile = onDeleteSettingsProfile,
            onImportProfile = onImportSettingsProfile,
            onShareProfile = onShareSettingsProfile
        )

        PingSettingsCard(
            pingSettings = pingSettings,
            onAddressChange = onPingAddressChange,
            onPingsPerTestChange = onPingsPerTestChange,
            onTestIntervalChange = onTestIntervalChange
        )

        MonitoringSettingsCard(
            monitoringSettings = monitoringSettings,
            passiveSignalSettings = passiveSignalSettings,
            simSubscriptions = simSubscriptions,
            phoneStatePermissionGranted = phoneStatePermissionGranted,
            onMonitor2gFallbackChange = onMonitor2gFallbackChange,
            onPassiveQuietUntilCriticalChange = onPassiveQuietUntilCriticalChange,
            onPassiveSignalSettingsChange = onPassiveSignalSettingsChange,
            onSubscriptionChange = onSubscriptionChange
        )

        PassiveSignalSettingsCard(
            settings = passiveSignalSettings,
            mockSettings = passiveMockSettings,
            passiveMeasurementIntervalMs = monitoringSettings.passiveMeasurementIntervalMs,
            signalPulseDurationMs = audioVolumes.signalPulseDurationMs,
            onSettingsChange = onPassiveSignalSettingsChange,
            onMockSettingsChange = onPassiveMockSettingsChange,
            onPassiveMeasurementIntervalChange = onPassiveMeasurementIntervalChange,
            onReset = onResetPassiveSignalSettings
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
            onPingClickVolumeChange = onPingClickVolumeChange,
            onLowSignalClickVolumeChange = onLowSignalClickVolumeChange,
            onSignalPulseFrequencyChange = onSignalPulseFrequencyChange,
            onSignalPulseDurationChange = onSignalPulseDurationChange,
            onCellChangeBellVolumeChange = onCellChangeBellVolumeChange,
            onCellChangeVoiceEnabledChange = onCellChangeVoiceEnabledChange,
            onCellChangeVoiceVolumeChange = onCellChangeVoiceVolumeChange,
            onTechnologyChangeVolumeChange = onTechnologyChangeVolumeChange,
            onTechnologyChangeVoiceEnabledChange = onTechnologyChangeVoiceEnabledChange,
            onTechnologyChangeVoiceVolumeChange = onTechnologyChangeVoiceVolumeChange,
            onNoSignalToneVolumeChange = onNoSignalToneVolumeChange,
            onNoSignalVoiceEnabledChange = onNoSignalVoiceEnabledChange,
            onNoSignalVoiceVolumeChange = onNoSignalVoiceVolumeChange,
            onLimitedServiceToneVolumeChange = onLimitedServiceToneVolumeChange,
            onLimitedServiceVoiceEnabledChange = onLimitedServiceVoiceEnabledChange,
            onLimitedServiceVoiceVolumeChange = onLimitedServiceVoiceVolumeChange,
            onPreviewPingClick = onPreviewPingClick,
            onPreviewLowSignalClick = onPreviewLowSignalClick,
            onPreviewCellChangeBell = onPreviewCellChangeBell,
            onPreviewCellChangeVoice = onPreviewCellChangeVoice,
            onPreviewTechnologyChange = onPreviewTechnologyChange,
            onPreviewTechnologyChangeVoice = onPreviewTechnologyChangeVoice,
            onPreviewNoSignalTone = onPreviewNoSignalTone,
            onPreviewNoSignalVoice = onPreviewNoSignalVoice,
            onPreviewLimitedServiceTone = onPreviewLimitedServiceTone,
            onPreviewLimitedServiceVoice = onPreviewLimitedServiceVoice,
            onReset = onResetAudioVolumes
        )

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

@Composable
private fun MonitoringControlButtons(
    isRunning: Boolean,
    passiveMockEnabled: Boolean,
    onStart: () -> Unit,
    onStartPassiveOnly: () -> Unit,
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
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.start_monitoring))
        }
        OutlinedButton(
            onClick = onStartPassiveOnly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(
                    if (passiveMockEnabled) {
                        R.string.start_passive_mock
                    } else {
                        R.string.start_passive_only
                    }
                )
            )
        }
    }
}

@Composable
private fun MetricsCard(
    stats: ConnectivityStats,
    monitoringSettings: MonitoringSettings,
    passiveSignalSettings: PassiveSignalSettings,
    onRequestCellIdentityPermission: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.metrics_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            MetricRow(
                label = stringResource(R.string.metric_sim),
                value = formatSimMetric(stats, monitoringSettings),
                valueFontSize = 13.sp,
                valueSingleLine = true
            )
            MetricRow(
                label = stringResource(R.string.metric_network),
                value = formatNetworkOperator(stats)
            )
            MetricRow(
                label = stringResource(R.string.metric_service_state),
                value = formatServiceState(stats)
            )
            MetricRow(
                label = stringResource(R.string.metric_radio),
                value = stats.radioAccessType ?: stringResource(R.string.signal_unavailable)
            )
            CellIdentityMetrics(stats = stats)
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
                tier = stats.resolveSignalMeasurementTier(passiveSignalSettings)
            )
        }
    }
}

@Composable
private fun CellIdentityMetrics(stats: ConnectivityStats) {
    val permissionGranted = stats.cellIdentityPermissionGranted
    when (stats.radioAccessType) {
        CellularSignalReader.RADIO_5G_ENDC -> {
            MetricRow(
                label = stringResource(R.string.metric_lte_earfcn),
                value = formatCellIdentityValue(stats.lteEarfcn, permissionGranted)
            )
            MetricRow(
                label = stringResource(R.string.metric_lte_pci),
                value = formatCellIdentityValue(stats.ltePci, permissionGranted)
            )
            MetricRow(
                label = stringResource(R.string.metric_nr_arfcn),
                value = formatCellIdentityValue(stats.nrEarfcn, permissionGranted)
            )
            MetricRow(
                label = stringResource(R.string.metric_nr_pci),
                value = formatCellIdentityValue(stats.nrPci, permissionGranted)
            )
        }
        CellularSignalReader.RADIO_5G -> {
            MetricRow(
                label = stringResource(R.string.metric_nr_arfcn),
                value = formatCellIdentityValue(stats.nrEarfcn, permissionGranted)
            )
            MetricRow(
                label = stringResource(R.string.metric_nr_pci),
                value = formatCellIdentityValue(stats.nrPci, permissionGranted)
            )
        }
        CellularSignalReader.RADIO_2G -> {
            MetricRow(
                label = stringResource(R.string.metric_gsm_arfcn),
                value = formatCellIdentityValue(stats.gsmEarfcn, permissionGranted)
            )
            MetricRow(
                label = stringResource(R.string.metric_gsm_bsic),
                value = formatCellIdentityValue(stats.gsmBsic, permissionGranted)
            )
        }
        else -> {
            MetricRow(
                label = stringResource(R.string.metric_earfcn),
                value = formatCellIdentityValue(stats.lteEarfcn, permissionGranted)
            )
            MetricRow(
                label = stringResource(R.string.metric_pci),
                value = formatCellIdentityValue(stats.ltePci, permissionGranted)
            )
        }
    }
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
        NetworkServiceMode.OUT_OF_SERVICE -> stringResource(R.string.service_state_no_service)
        NetworkServiceMode.UNKNOWN -> stringResource(R.string.service_state_unknown)
    }
}

@Composable
private fun formatNetworkOperator(stats: ConnectivityStats): String {
    if (!stats.cellularAvailable) {
        return stringResource(R.string.network_waiting)
    }
    if (!stats.networkOperatorName.isNullOrBlank()) {
        return stats.networkOperatorName
    }
    if (!stats.plmn.isNullOrBlank()) {
        return stats.plmn
    }
    if (!stats.signalPermissionGranted) {
        return stringResource(R.string.network_permission_required)
    }
    return stringResource(R.string.network_cellular)
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

@Composable
private fun SignalTierMetricRow(tier: SignalMeasurementTier) {
    MetricRow(
        label = stringResource(R.string.metric_signal_tier),
        value = signalMeasurementTierLabel(tier),
        valueColor = signalMeasurementTierColor(tier)
    )
}

@Composable
private fun signalMeasurementTierLabel(tier: SignalMeasurementTier): String {
    tier.displayNumber?.let { number ->
        return stringResource(R.string.signal_tier_number, number)
    }
    return stringResource(
        when (tier) {
            SignalMeasurementTier.NO_SIGNAL -> R.string.signal_tier_no_signal
            SignalMeasurementTier.LIMITED_SERVICE -> R.string.signal_tier_limited_service
            SignalMeasurementTier.PERMISSION_REQUIRED -> R.string.signal_permission_required
            SignalMeasurementTier.UNAVAILABLE -> R.string.signal_tier_unavailable
            else -> R.string.signal_tier_unavailable
        }
    )
}

@Composable
private fun signalMeasurementTierColor(tier: SignalMeasurementTier): Color {
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
