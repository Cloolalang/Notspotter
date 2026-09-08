package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.SettingsCompatibility
import io.github.cloolalang.notspotdetector.model.SignalStrengthTier
import io.github.cloolalang.notspotdetector.model.VERY_STRONG_TIER_NUMBER
import kotlin.math.roundToInt

@Composable
fun PassiveSignalSettingsCard(
    settings: PassiveSignalSettings,
    mockSettings: PassiveMockSettings,
    passiveMeasurementIntervalMs: Long,
    signalPulseDurationMs: Int,
    onSettingsChange: (PassiveSignalSettings) -> Unit,
    onMockSettingsChange: (PassiveMockSettings) -> Unit,
    onPassiveMeasurementIntervalChange: (Long) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.passive_signal_settings_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = stringResource(R.string.passive_signal_settings_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (expanded) {
                PassiveMeasurementIntervalSlider(
                    intervalMs = passiveMeasurementIntervalMs,
                    onIntervalChange = onPassiveMeasurementIntervalChange
                )

                PassiveMockSection(
                    mockSettings = mockSettings,
                    onMockSettingsChange = onMockSettingsChange
                )

                Text(
                    text = stringResource(R.string.passive_signal_rsrp_section),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(
                        R.string.passive_signal_rsrp_section_hint,
                        PassiveSignalSettings.MIN_RSRP_DBM,
                        PassiveSignalSettings.MAX_RSRP_DBM
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(R.string.passive_signal_rsrp_order_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                RsrpTierSettings(
                    settings = settings,
                    signalPulseDurationMs = signalPulseDurationMs,
                    passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                    onSettingsChange = onSettingsChange
                )

                Text(
                    text = stringResource(R.string.passive_signal_rsrq_section),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(
                        R.string.passive_signal_rsrq_section_hint,
                        PassiveSignalSettings.RSRQ_FAIR_MIN_DB,
                        PassiveSignalSettings.RSRQ_FAIR_MAX_DB
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                RsrqBoundarySliders(
                    settings = settings,
                    onSettingsChange = onSettingsChange
                )

                NoisyRsrqPassiveClicksOption(
                    settings = settings,
                    onSettingsChange = onSettingsChange
                )

                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.passive_signal_settings_reset))
                }
            }
        }
    }
}

@Composable
private fun TierClickSpeedSlider(
    label: String,
    intervalMs: Int,
    signalPulseDurationMs: Int,
    passiveMeasurementIntervalMs: Long,
    accentColor: Color,
    onIntervalChange: (Int) -> Unit
) {
    val minUiMs = SettingsCompatibility.minTierClickIntervalUiMs(signalPulseDurationMs)
    val stepSize = PassiveSignalSettings.TIER_CLICK_INTERVAL_STEP_MS
    val minStep = minUiMs / stepSize
    val maxStep = PassiveSignalSettings.MAX_TIER_CLICK_INTERVAL_MS / stepSize
    val coercedIntervalMs = intervalMs.coerceIn(minUiMs, PassiveSignalSettings.MAX_TIER_CLICK_INTERVAL_MS)
    val step = (coercedIntervalMs / stepSize).coerceIn(minStep, maxStep)
    val passiveOnlyEffectiveMs = SettingsCompatibility.resolveTierClickIntervalMs(
        configuredMs = coercedIntervalMs.toLong(),
        signalPulseDurationMs = signalPulseDurationMs,
        isPassiveOnlySession = true
    )
    val pingAndPassiveEffectiveMs = SettingsCompatibility.resolveTierClickIntervalMs(
        configuredMs = coercedIntervalMs.toLong(),
        signalPulseDurationMs = signalPulseDurationMs,
        isPassiveOnlySession = false
    )
    val showPulseDurationNote = intervalMs < minUiMs
    val showMeasurementWarning = SettingsCompatibility.isTierIntervalMuchSlowerThanMeasurement(
        tierIntervalMs = coercedIntervalMs,
        measurementIntervalMs = passiveMeasurementIntervalMs
    )
    if (!TierSliderSupport.isValidClickIndexRange(minStep, maxStep)) return

    val sliderSteps = TierSliderSupport.tierClickIndexSteps(minStep, maxStep)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = accentColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.passive_signal_tier_click_value, coercedIntervalMs),
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                fontWeight = FontWeight.Medium
            )
        }
        if (showPulseDurationNote) {
            Text(
                text = stringResource(
                    R.string.passive_signal_tier_click_effective,
                    passiveOnlyEffectiveMs
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        Text(
            text = stringResource(
                R.string.passive_signal_tier_click_ping_mode,
                pingAndPassiveEffectiveMs
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (showMeasurementWarning) {
            Text(
                text = stringResource(
                    R.string.passive_signal_tier_click_measurement_warning,
                    passiveMeasurementIntervalMs / 1_000L
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        Slider(
            value = step.toFloat().coerceIn(minStep.toFloat(), maxStep.toFloat()),
            onValueChange = {
                val selectedMs = (it.roundToInt() * stepSize).coerceIn(minUiMs, PassiveSignalSettings.MAX_TIER_CLICK_INTERVAL_MS)
                onIntervalChange(selectedMs)
            },
            valueRange = minStep.toFloat()..maxStep.toFloat(),
            steps = sliderSteps,
            colors = tierSliderColors(accentColor)
        )
    }
}

@Composable
private fun PassiveMeasurementIntervalSlider(
    intervalMs: Long,
    onIntervalChange: (Long) -> Unit
) {
    val minSeconds = (MonitoringSettings.MIN_PASSIVE_MEASUREMENT_INTERVAL_MS / 1_000).toInt()
    val maxSeconds = (MonitoringSettings.MAX_PASSIVE_MEASUREMENT_INTERVAL_MS / 1_000).toInt()
    val valueSeconds = (intervalMs / 1_000L).toInt().coerceIn(minSeconds, maxSeconds)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.passive_measurement_interval_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.passive_measurement_interval_value, valueSeconds),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = stringResource(R.string.passive_measurement_interval_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = valueSeconds.toFloat(),
            onValueChange = { onIntervalChange(it.roundToInt() * 1_000L) },
            valueRange = minSeconds.toFloat()..maxSeconds.toFloat(),
            steps = maxSeconds - minSeconds - 1
        )
    }
}

@Composable
private fun PassiveMockSection(
    mockSettings: PassiveMockSettings,
    onMockSettingsChange: (PassiveMockSettings) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = mockSettings.enabled,
                onCheckedChange = { onMockSettingsChange(mockSettings.copy(enabled = it)) }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.passive_mock_enabled),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.passive_mock_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (mockSettings.enabled) {
            BoundarySlider(
                label = stringResource(R.string.passive_mock_rsrp),
                rangeLabel = stringResource(
                    R.string.passive_signal_bound_value,
                    mockSettings.rsrpDbm,
                    "dBm"
                ),
                value = mockSettings.rsrpDbm,
                valueRange = PassiveSignalSettings.MIN_RSRP_DBM..PassiveSignalSettings.MAX_RSRP_DBM,
                onValueChange = { onMockSettingsChange(mockSettings.copy(rsrpDbm = it)) }
            )
            BoundarySlider(
                label = stringResource(R.string.passive_mock_rsrq),
                rangeLabel = stringResource(
                    R.string.passive_signal_bound_value,
                    mockSettings.rsrqDb,
                    "dB"
                ),
                value = mockSettings.rsrqDb,
                valueRange = PassiveSignalSettings.MIN_RSRQ_DB..PassiveSignalSettings.MAX_RSRQ_DB,
                unit = "dB",
                onValueChange = { onMockSettingsChange(mockSettings.copy(rsrqDb = it)) }
            )
        }
    }
}

@Composable
private fun RsrpTierSettings(
    settings: PassiveSignalSettings,
    signalPulseDurationMs: Int,
    passiveMeasurementIntervalMs: Long,
    onSettingsChange: (PassiveSignalSettings) -> Unit
) {
    val gap = PassiveSignalSettings.MIN_RSRP_BAND_GAP_DBM
    val maxMild = settings.veryStrongRsrpMinDbm - gap

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TierSettingSection(
            tierNumber = VERY_STRONG_TIER_NUMBER,
            accentColor = SignalTierColors.forTierNumber(VERY_STRONG_TIER_NUMBER)
        ) {
            TierClickSpeedSlider(
                label = stringResource(R.string.passive_signal_tier_click_interval, VERY_STRONG_TIER_NUMBER),
                intervalMs = settings.veryStrongTierClickIntervalMs,
                signalPulseDurationMs = signalPulseDurationMs,
                passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                accentColor = SignalTierColors.forTierNumber(VERY_STRONG_TIER_NUMBER),
                onIntervalChange = { onSettingsChange(settings.copy(veryStrongTierClickIntervalMs = it)) }
            )
            BoundarySlider(
                label = stringResource(R.string.passive_signal_boundary_tier_above, VERY_STRONG_TIER_NUMBER),
                rangeLabel = stringResource(
                    R.string.passive_signal_rsrp_band_range_open,
                    settings.veryStrongRsrpMinDbm + gap,
                    PassiveSignalSettings.MAX_RSRP_DBM
                ),
                value = settings.veryStrongRsrpMinDbm,
                valueRange = (settings.mildRsrpMinDbm + gap)..PassiveSignalSettings.MAX_VERY_STRONG_RSRP_DBM,
                accentColor = SignalTierColors.forTierNumber(VERY_STRONG_TIER_NUMBER),
                onValueChange = { onSettingsChange(settings.copy(veryStrongRsrpMinDbm = it)) }
            )
            Text(
                text = stringResource(R.string.passive_signal_very_strong_hint),
                style = MaterialTheme.typography.bodySmall,
                color = SignalTierColors.tier1
            )
        }

        TierSettingSection(
            tierNumber = SignalStrengthTier.MILD.displayNumber,
            accentColor = SignalTierColors.forStrengthTier(SignalStrengthTier.MILD)
        ) {
            TierClickSpeedSlider(
                label = stringResource(
                    R.string.passive_signal_tier_click_interval,
                    SignalStrengthTier.MILD.displayNumber
                ),
                intervalMs = settings.mildTierClickIntervalMs,
                signalPulseDurationMs = signalPulseDurationMs,
                passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                accentColor = SignalTierColors.forStrengthTier(SignalStrengthTier.MILD),
                onIntervalChange = { onSettingsChange(settings.copy(mildTierClickIntervalMs = it)) }
            )
            BoundarySlider(
                label = stringResource(
                    R.string.passive_signal_boundary_tier_above,
                    SignalStrengthTier.MILD.displayNumber
                ),
                rangeLabel = stringResource(
                    R.string.passive_signal_rsrp_band_range,
                    settings.mildRsrpMinDbm + gap,
                    settings.veryStrongRsrpMinDbm
                ),
                value = settings.mildRsrpMinDbm,
                valueRange = (settings.goodRsrpMinDbm + gap)..maxMild,
                accentColor = SignalTierColors.forStrengthTier(SignalStrengthTier.MILD),
                onValueChange = { onSettingsChange(settings.copy(mildRsrpMinDbm = it)) }
            )
        }

        TierSettingSection(
            tierNumber = SignalStrengthTier.GOOD.displayNumber,
            accentColor = SignalTierColors.forStrengthTier(SignalStrengthTier.GOOD)
        ) {
            TierClickSpeedSlider(
                label = stringResource(
                    R.string.passive_signal_tier_click_interval,
                    SignalStrengthTier.GOOD.displayNumber
                ),
                intervalMs = settings.goodTierClickIntervalMs,
                signalPulseDurationMs = signalPulseDurationMs,
                passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                accentColor = SignalTierColors.forStrengthTier(SignalStrengthTier.GOOD),
                onIntervalChange = { onSettingsChange(settings.copy(goodTierClickIntervalMs = it)) }
            )
            BoundarySlider(
                label = stringResource(
                    R.string.passive_signal_boundary_tier_above,
                    SignalStrengthTier.GOOD.displayNumber
                ),
                rangeLabel = stringResource(
                    R.string.passive_signal_rsrp_band_range,
                    settings.goodRsrpMinDbm + gap,
                    settings.mildRsrpMinDbm
                ),
                value = settings.goodRsrpMinDbm,
                valueRange = (settings.fairRsrpMinDbm + gap)..(settings.mildRsrpMinDbm - gap),
                accentColor = SignalTierColors.forStrengthTier(SignalStrengthTier.GOOD),
                onValueChange = { onSettingsChange(settings.copy(goodRsrpMinDbm = it)) }
            )
        }

        TierSettingSection(
            tierNumber = SignalStrengthTier.FAIR.displayNumber,
            accentColor = SignalTierColors.forStrengthTier(SignalStrengthTier.FAIR)
        ) {
            TierClickSpeedSlider(
                label = stringResource(
                    R.string.passive_signal_tier_click_interval,
                    SignalStrengthTier.FAIR.displayNumber
                ),
                intervalMs = settings.fairTierClickIntervalMs,
                signalPulseDurationMs = signalPulseDurationMs,
                passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                accentColor = SignalTierColors.forStrengthTier(SignalStrengthTier.FAIR),
                onIntervalChange = { onSettingsChange(settings.copy(fairTierClickIntervalMs = it)) }
            )
            BoundarySlider(
                label = stringResource(
                    R.string.passive_signal_boundary_tier_above,
                    SignalStrengthTier.FAIR.displayNumber
                ),
                rangeLabel = stringResource(
                    R.string.passive_signal_rsrp_band_range,
                    settings.fairRsrpMinDbm + gap,
                    settings.goodRsrpMinDbm
                ),
                value = settings.fairRsrpMinDbm,
                valueRange = (settings.poorRsrpMinDbm + gap)..(settings.goodRsrpMinDbm - gap),
                accentColor = SignalTierColors.forStrengthTier(SignalStrengthTier.FAIR),
                onValueChange = { onSettingsChange(settings.copy(fairRsrpMinDbm = it)) }
            )
        }

        TierSettingSection(
            tierNumber = SignalStrengthTier.POOR.displayNumber,
            accentColor = SignalTierColors.forStrengthTier(SignalStrengthTier.POOR)
        ) {
            TierClickSpeedSlider(
                label = stringResource(
                    R.string.passive_signal_tier_click_interval,
                    SignalStrengthTier.POOR.displayNumber
                ),
                intervalMs = settings.poorTierClickIntervalMs,
                signalPulseDurationMs = signalPulseDurationMs,
                passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                accentColor = SignalTierColors.forStrengthTier(SignalStrengthTier.POOR),
                onIntervalChange = { onSettingsChange(settings.copy(poorTierClickIntervalMs = it)) }
            )
            BoundarySlider(
                label = stringResource(
                    R.string.passive_signal_boundary_tier_above,
                    SignalStrengthTier.POOR.displayNumber
                ),
                rangeLabel = stringResource(
                    R.string.passive_signal_rsrp_band_range,
                    settings.poorRsrpMinDbm + gap,
                    settings.fairRsrpMinDbm
                ),
                value = settings.poorRsrpMinDbm,
                valueRange = (settings.noSignalRsrpDbm + gap)..(settings.fairRsrpMinDbm - gap),
                accentColor = SignalTierColors.forStrengthTier(SignalStrengthTier.POOR),
                onValueChange = { onSettingsChange(settings.copy(poorRsrpMinDbm = it)) }
            )
        }

        TierSettingSection(
            tierNumber = SignalStrengthTier.CRITICAL.displayNumber,
            accentColor = SignalTierColors.forStrengthTier(SignalStrengthTier.CRITICAL)
        ) {
            TierClickSpeedSlider(
                label = stringResource(
                    R.string.passive_signal_tier_click_interval,
                    SignalStrengthTier.CRITICAL.displayNumber
                ),
                intervalMs = settings.criticalTierClickIntervalMs,
                signalPulseDurationMs = signalPulseDurationMs,
                passiveMeasurementIntervalMs = passiveMeasurementIntervalMs,
                accentColor = SignalTierColors.forStrengthTier(SignalStrengthTier.CRITICAL),
                onIntervalChange = { onSettingsChange(settings.copy(criticalTierClickIntervalMs = it)) }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.passive_signal_no_signal_section),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = SignalTierColors.noSignal
            )
            BoundarySlider(
                label = stringResource(R.string.passive_signal_boundary_no_signal),
                rangeLabel = stringResource(
                    R.string.passive_signal_rsrp_band_range,
                    PassiveSignalSettings.MIN_RSRP_DBM,
                    settings.noSignalRsrpDbm
                ),
                value = settings.noSignalRsrpDbm,
                valueRange = PassiveSignalSettings.MIN_RSRP_DBM..(settings.poorRsrpMinDbm - gap),
                accentColor = SignalTierColors.noSignal,
                onValueChange = { onSettingsChange(settings.copy(noSignalRsrpDbm = it)) }
            )
        }
    }
}

@Composable
private fun TierSettingSection(
    tierNumber: Int,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.passive_signal_tier_section, tierNumber),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = accentColor
        )
        content()
    }
}

@Composable
private fun RsrqBoundarySliders(
    settings: PassiveSignalSettings,
    onSettingsChange: (PassiveSignalSettings) -> Unit
) {
    BoundarySlider(
        label = stringResource(R.string.passive_signal_boundary_rsrq_fair),
        rangeLabel = stringResource(
            R.string.passive_signal_rsrq_single_band_hint,
            PassiveSignalSettings.RSRQ_FAIR_MIN_DB,
            settings.rsrqFairMinDb - PassiveSignalSettings.MIN_RSRQ_BAND_GAP_DB,
            settings.rsrqFairMinDb,
            PassiveSignalSettings.RSRQ_FAIR_MAX_DB
        ),
        value = settings.rsrqFairMinDb,
        valueRange = PassiveSignalSettings.RSRQ_FAIR_MIN_DB..PassiveSignalSettings.RSRQ_FAIR_MAX_DB,
        unit = "dB",
        accentColor = SignalTierColors.forStrengthTier(SignalStrengthTier.CRITICAL),
        onValueChange = { onSettingsChange(settings.copy(rsrqFairMinDb = it)) }
    )
}

@Composable
private fun NoisyRsrqPassiveClicksOption(
    settings: PassiveSignalSettings,
    onSettingsChange: (PassiveSignalSettings) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = settings.noisyRsrqPassiveClicks,
            onCheckedChange = { onSettingsChange(settings.copy(noisyRsrqPassiveClicks = it)) }
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.passive_signal_noisy_rsrq_clicks),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(
                    R.string.passive_signal_noisy_rsrq_clicks_hint,
                    settings.rsrqFairMinDb
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BoundarySlider(
    label: String,
    rangeLabel: String,
    value: Int,
    valueRange: IntRange,
    accentColor: Color? = null,
    unit: String = "dBm",
    onValueChange: (Int) -> Unit
) {
    if (!TierSliderSupport.isValidIntRange(valueRange)) return

    val controlColor = accentColor ?: MaterialTheme.colorScheme.primary
    val sliderSteps = TierSliderSupport.discreteIntRangeSteps(valueRange)
    val sliderValue = value.toFloat().coerceIn(valueRange.first.toFloat(), valueRange.last.toFloat())

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = controlColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.passive_signal_bound_value, value, unit),
                style = MaterialTheme.typography.bodySmall,
                color = controlColor,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = rangeLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = sliderValue,
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = sliderSteps,
            colors = tierSliderColors(controlColor)
        )
    }
}
