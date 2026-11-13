package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.CallTestPhase
import io.github.cloolalang.notspotdetector.model.CallTestScore
import io.github.cloolalang.notspotdetector.model.CallTestSettings
import io.github.cloolalang.notspotdetector.model.CallTestStatus
import io.github.cloolalang.notspotdetector.ui.theme.CallTestIndigo
import kotlin.math.roundToInt

@Composable
fun CallTestCard(
    settings: CallTestSettings,
    status: CallTestStatus,
    controlsEnabled: Boolean = true,
    onNumberChange: (String) -> Unit,
    onDurationChange: (Int) -> Unit,
    onDelayChange: (Int) -> Unit,
    onMasterTimeoutChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by panelFold("call-test", defaultExpanded = true)
    val dialable = CallTestScore.dialableNumber(settings.phoneNumber) != null

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.call_test_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = CallTestIndigo,
                        fontWeight = FontWeight.Bold
                    )
                    if (status.attempts > 0) {
                        Text(
                            text = stringResource(
                                R.string.call_test_stats,
                                status.attempts,
                                status.successes,
                                status.failures,
                                status.successRatePercent
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (expanded) {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.call_test_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = settings.phoneNumber,
                    onValueChange = { onNumberChange(it.take(CallTestSettings.MAX_NUMBER_CHARS)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.call_test_number_label)) },
                    supportingText = {
                        Text(
                            stringResource(
                                if (settings.phoneNumber.isBlank() || dialable) {
                                    R.string.call_test_number_hint
                                } else {
                                    R.string.call_test_number_invalid
                                }
                            )
                        )
                    },
                    enabled = controlsEnabled,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                CallTestSecondsSlider(
                    label = stringResource(R.string.call_test_duration_label),
                    valueSeconds = settings.callDurationSec,
                    hint = stringResource(R.string.call_test_duration_hint),
                    enabled = controlsEnabled,
                    onValueChange = onDurationChange
                )
                CallTestSecondsSlider(
                    label = stringResource(R.string.call_test_delay_label),
                    valueSeconds = settings.delayBetweenCallsSec,
                    hint = stringResource(R.string.call_test_delay_hint),
                    enabled = controlsEnabled,
                    onValueChange = onDelayChange
                )
                CallTestMasterTimeoutSwitch(
                    enabled = settings.masterTimeoutEnabled,
                    controlsEnabled = controlsEnabled,
                    onEnabledChange = onMasterTimeoutChange
                )
                if (!controlsEnabled) {
                    Text(
                        text = stringResource(R.string.call_test_follows_master),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (status.needsDialer) {
                    Text(
                        text = stringResource(R.string.call_test_needs_dialer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (status.blocked) {
                    Text(
                        text = stringResource(R.string.call_test_phone_state_needed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (status.running) {
                    Text(
                        text = phaseLabel(status.phase),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CallTestIndigo,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun CallTestMasterTimeoutSwitch(
    enabled: Boolean,
    controlsEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.call_test_master_timeout_label),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(
                    R.string.call_test_master_timeout_value,
                    CallTestSettings.MASTER_TIMEOUT_MINUTES
                ),
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) {
                    CallTestIndigo
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.Medium
            )
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                enabled = controlsEnabled
            )
        }
        Text(
            text = if (enabled) {
                stringResource(
                    R.string.call_test_master_timeout_hint_on,
                    CallTestSettings.MASTER_TIMEOUT_MINUTES
                )
            } else {
                stringResource(R.string.call_test_master_timeout_hint_off)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun phaseLabel(phase: CallTestPhase): String {
    return when (phase) {
        CallTestPhase.DIALING -> stringResource(R.string.call_test_phase_dialing)
        CallTestPhase.IN_CALL -> stringResource(R.string.call_test_phase_in_call)
        CallTestPhase.GAP -> stringResource(R.string.call_test_phase_gap)
        CallTestPhase.WAITING_FOR_HANGUP -> stringResource(R.string.call_test_phase_hangup)
        CallTestPhase.IDLE -> stringResource(R.string.call_test_phase_idle)
    }
}

@Composable
private fun CallTestSecondsSlider(
    label: String,
    valueSeconds: Int,
    hint: String,
    enabled: Boolean,
    onValueChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = stringResource(R.string.call_test_seconds_value, valueSeconds),
                style = MaterialTheme.typography.bodyMedium,
                color = CallTestIndigo,
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            value = valueSeconds.toFloat(),
            onValueChange = { raw ->
                val snapped = (raw / 5f).roundToInt() * 5
                onValueChange(snapped.coerceIn(CallTestSettings.MIN_SECONDS, CallTestSettings.MAX_SECONDS))
            },
            valueRange = CallTestSettings.MIN_SECONDS.toFloat()..CallTestSettings.MAX_SECONDS.toFloat(),
            steps = (CallTestSettings.MAX_SECONDS - CallTestSettings.MIN_SECONDS) / 5 - 1,
            enabled = enabled
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
