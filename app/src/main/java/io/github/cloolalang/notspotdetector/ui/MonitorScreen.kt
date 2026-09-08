package io.github.cloolalang.notspotdetector.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.ConnectionQuality
import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.ConnectivityStats
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.NetworkServiceMode
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.PingSettings
import io.github.cloolalang.notspotdetector.model.RttSample
import io.github.cloolalang.notspotdetector.model.ProfileSaveResult
import io.github.cloolalang.notspotdetector.model.SettingsProfileSummary
import io.github.cloolalang.notspotdetector.model.SimSubscriptionOption
import io.github.cloolalang.notspotdetector.model.ThresholdSettings
import io.github.cloolalang.notspotdetector.model.receptionLevel
import io.github.cloolalang.notspotdetector.model.ReceptionLevel
import io.github.cloolalang.notspotdetector.model.shouldPlayFlatline

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
    isRunning: Boolean,
    isBatteryOptimizationDisabled: Boolean,
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
    onResetPassiveSignalSettings: () -> Unit,
    onSubscriptionChange: (Int) -> Unit,
    onPingClickVolumeChange: (Float) -> Unit,
    onLowSignalClickVolumeChange: (Float) -> Unit,
    onSignalPulseDurationChange: (Int) -> Unit,
    onCellChangeBellVolumeChange: (Float) -> Unit,
    onTechnologyChangeVolumeChange: (Float) -> Unit,
    onNoSignalToneVolumeChange: (Float) -> Unit,
    onLimitedServiceToneVolumeChange: (Float) -> Unit,
    onPassiveSoundSpeedChange: (Int) -> Unit,
    onPreviewPingClick: () -> Unit,
    onPreviewLowSignalClick: () -> Unit,
    onPreviewCellChangeBell: () -> Unit,
    onPreviewTechnologyChange: () -> Unit,
    onPreviewNoSignalTone: () -> Unit,
    onPreviewLimitedServiceTone: () -> Unit,
    onResetAudioVolumes: () -> Unit,
    onSaveSettingsProfile: (String) -> ProfileSaveResult,
    onLoadSettingsProfile: (String) -> Unit,
    onDeleteSettingsProfile: (String) -> Unit,
    onRequestBatteryExemption: () -> Unit,
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
                text = stringResource(R.string.app_name),
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

        Text(
            text = stringResource(R.string.tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isRunning) {
            BackgroundStatusCard(
                isBatteryOptimizationDisabled = isBatteryOptimizationDisabled,
                onRequestBatteryExemption = onRequestBatteryExemption
            )
        }

        GeigerIndicator(
            quality = stats.quality,
            severity = stats.severity,
            isFlatline = isRunning && !stats.isPassiveIdleMode &&
                stats.shouldPlayFlatline(passiveSignalSettings) && !stats.isLimitedService,
            isLimitedService = isRunning && !stats.isPassiveIdleMode && stats.isLimitedService,
            isActive = isRunning && !stats.isPassiveIdleMode && stats.cellularAvailable &&
                !stats.shouldPlayFlatline(passiveSignalSettings) && !stats.isLimitedService,
            receptionMode = isRunning && stats.isPassiveOnlySession,
            stats = stats,
            passiveSignalSettings = passiveSignalSettings
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
                onRequestCellIdentityPermission = onRequestCellIdentityPermission
            )
        }

        SettingsProfilesCard(
            profiles = settingsProfiles,
            onSaveProfile = onSaveSettingsProfile,
            onLoadProfile = onLoadSettingsProfile,
            onDeleteProfile = onDeleteSettingsProfile
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
            passiveSoundSpeed = monitoringSettings.passiveSoundSpeed,
            previewEnabled = !isRunning,
            onPingClickVolumeChange = onPingClickVolumeChange,
            onLowSignalClickVolumeChange = onLowSignalClickVolumeChange,
            onSignalPulseDurationChange = onSignalPulseDurationChange,
            onCellChangeBellVolumeChange = onCellChangeBellVolumeChange,
            onTechnologyChangeVolumeChange = onTechnologyChangeVolumeChange,
            onNoSignalToneVolumeChange = onNoSignalToneVolumeChange,
            onLimitedServiceToneVolumeChange = onLimitedServiceToneVolumeChange,
            onPassiveSoundSpeedChange = onPassiveSoundSpeedChange,
            onPreviewPingClick = onPreviewPingClick,
            onPreviewLowSignalClick = onPreviewLowSignalClick,
            onPreviewCellChangeBell = onPreviewCellChangeBell,
            onPreviewTechnologyChange = onPreviewTechnologyChange,
            onPreviewNoSignalTone = onPreviewNoSignalTone,
            onPreviewLimitedServiceTone = onPreviewLimitedServiceTone,
            onReset = onResetAudioVolumes
        )

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
                        if (passiveMockSettings.enabled) {
                            R.string.start_passive_mock
                        } else {
                            R.string.start_passive_only
                        }
                    )
                )
            }
        }

        if (!stats.cellularAvailable && isRunning) {
            Text(
                text = stringResource(R.string.waiting_for_cellular),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (isRunning) {
            Text(
                text = stringResource(R.string.background_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.size(8.dp))
    }
}

@Composable
private fun BackgroundStatusCard(
    isBatteryOptimizationDisabled: Boolean,
    onRequestBatteryExemption: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isBatteryOptimizationDisabled) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.background_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(
                    if (isBatteryOptimizationDisabled) {
                        R.string.background_status_ok
                    } else {
                        R.string.background_status_restricted
                    }
                ),
                style = MaterialTheme.typography.bodySmall
            )
            if (!isBatteryOptimizationDisabled) {
                OutlinedButton(
                    onClick = onRequestBatteryExemption,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.background_allow))
                }
            }
        }
    }
}

@Composable
private fun GeigerIndicator(
    quality: ConnectionQuality,
    severity: Float,
    isFlatline: Boolean,
    isLimitedService: Boolean,
    isActive: Boolean,
    receptionMode: Boolean = false,
    stats: ConnectivityStats = ConnectivityStats(),
    passiveSignalSettings: PassiveSignalSettings = PassiveSignalSettings()
) {
    val receptionLevel = if (receptionMode) stats.receptionLevel(passiveSignalSettings) else null

    val pulseSpeed = when {
        receptionLevel != null -> when (receptionLevel) {
            ReceptionLevel.GOOD -> 1800
            ReceptionLevel.FAIR -> 700
            ReceptionLevel.POOR -> 350
            ReceptionLevel.UNKNOWN -> 2000
        }
        isLimitedService -> 500
        isFlatline -> 400
        !isActive -> 2000
        quality == ConnectionQuality.POOR -> (400 - severity * 340).toInt().coerceAtLeast(80)
        quality == ConnectionQuality.DEGRADED -> (1200 - severity * 800).toInt().coerceAtLeast(300)
        else -> 1800
    }

    val infiniteTransition = rememberInfiniteTransition(label = "geiger_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = when {
            receptionLevel != null -> when (receptionLevel) {
                ReceptionLevel.GOOD -> 1.08f
                ReceptionLevel.FAIR -> 1.12f
                ReceptionLevel.POOR -> 1.18f
                ReceptionLevel.UNKNOWN -> 1f
            }
            isLimitedService -> 1.1f
            isFlatline -> 1.08f
            isActive -> 1.15f + severity * 0.2f
            else -> 1f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(pulseSpeed),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val indicatorColor = when {
        receptionLevel != null -> when (receptionLevel) {
            ReceptionLevel.GOOD -> Color(0xFF4CAF50)
            ReceptionLevel.FAIR -> Color(0xFFFFC107)
            ReceptionLevel.POOR -> Color(0xFFF44336)
            ReceptionLevel.UNKNOWN -> Color(0xFF9E9E9E)
        }
        isLimitedService -> Color(0xFFFF6F00)
        isFlatline -> Color(0xFFB71C1C)
        quality == ConnectionQuality.GOOD -> Color(0xFF4CAF50)
        quality == ConnectionQuality.DEGRADED -> Color(0xFFFFC107)
        quality == ConnectionQuality.POOR -> Color(0xFFF44336)
        quality == ConnectionQuality.NO_CELLULAR -> Color(0xFF9E9E9E)
        quality == ConnectionQuality.PASSIVE_IDLE -> Color(0xFF5C6BC0)
        else -> Color(0xFF607D8B)
    }

    val statusLabel = when {
        receptionLevel != null -> receptionLabel(receptionLevel)
        isLimitedService -> stringResource(R.string.quality_limited_service)
        isFlatline -> stringResource(R.string.quality_flatline)
        else -> qualityLabel(quality)
    }

    val hintText = if (receptionMode) {
        stringResource(R.string.reception_hint)
    } else {
        stringResource(R.string.geiger_hint)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale)
                    .background(indicatorColor.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(indicatorColor, CircleShape)
                )
            }

            Text(
                text = statusLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = indicatorColor
            )

            Text(
                text = hintText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricsCard(
    stats: ConnectivityStats,
    monitoringSettings: MonitoringSettings,
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
                value = formatSimMetric(stats, monitoringSettings)
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
        return stringResource(R.string.signal_unavailable)
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
        return stringResource(R.string.signal_unavailable)
    }
    return value.toString()
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun receptionLabel(level: ReceptionLevel): String {
    return stringResource(
        when (level) {
            ReceptionLevel.GOOD -> R.string.quality_reception_good
            ReceptionLevel.FAIR -> R.string.quality_reception_fair
            ReceptionLevel.POOR -> R.string.quality_reception_poor
            ReceptionLevel.UNKNOWN -> R.string.quality_reception_unknown
        }
    )
}

@Composable
private fun qualityLabel(quality: ConnectionQuality): String {
    return stringResource(
        when (quality) {
            ConnectionQuality.GOOD -> R.string.quality_good
            ConnectionQuality.DEGRADED -> R.string.quality_degraded
            ConnectionQuality.POOR -> R.string.quality_poor
            ConnectionQuality.NO_CELLULAR -> R.string.quality_no_cellular
            ConnectionQuality.PASSIVE_IDLE -> R.string.quality_passive_idle
            ConnectionQuality.MONITORING_STOPPED -> R.string.monitoring_stopped
        }
    )
}
