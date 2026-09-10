package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.MockNetworkScenario
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import kotlin.math.roundToInt

@Composable
fun MockNetworkStateCard(
    mockSettings: PassiveMockSettings,
    onMockSettingsChange: (PassiveMockSettings) -> Unit,
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
                    text = stringResource(R.string.mock_network_state_title),
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
                text = stringResource(R.string.mock_network_state_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (expanded) {
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
                            text = stringResource(R.string.mock_network_state_enabled_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (mockSettings.enabled) {
                    Text(
                        text = stringResource(R.string.mock_network_state_scenario_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.mock_network_state_scenario_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        MockNetworkScenario.entries.forEach { scenario ->
                            MockScenarioRadioOption(
                                selected = mockSettings.scenario == scenario,
                                label = mockScenarioLabel(scenario),
                                hint = mockScenarioHint(scenario),
                                onSelect = { onMockSettingsChange(mockSettings.copy(scenario = scenario)) }
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.mock_network_state_signal_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (!mockSettings.scenario.appliesMockSignalStrength()) {
                        Text(
                            text = stringResource(R.string.mock_network_state_signal_inactive_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (mockSettings.scenario.usesG2SignalStrength()) {
                        Text(
                            text = stringResource(R.string.mock_network_state_2g_rx_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    MockSignalSlider(
                        label = stringResource(R.string.passive_mock_rsrp),
                        value = mockSettings.rsrpDbm,
                        valueRange = PassiveMockSettings.MIN_MOCK_RSRP_DBM..PassiveSignalSettings.MAX_RSRP_DBM,
                        unit = "dBm",
                        onValueChange = { onMockSettingsChange(mockSettings.copy(rsrpDbm = it)) }
                    )
                    MockSignalSlider(
                        label = stringResource(R.string.passive_mock_rsrq),
                        value = mockSettings.rsrqDb,
                        valueRange = PassiveSignalSettings.MIN_RSRQ_DB..PassiveSignalSettings.MAX_RSRQ_DB,
                        unit = "dB",
                        onValueChange = { onMockSettingsChange(mockSettings.copy(rsrqDb = it)) }
                    )

                    if (mockSettings.scenario == MockNetworkScenario.SEARCHING_2G) {
                        Text(
                            text = stringResource(R.string.mock_network_state_searching_requirement),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MockScenarioRadioOption(
    selected: Boolean,
    label: String,
    hint: String,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun mockScenarioLabel(scenario: MockNetworkScenario): String {
    return stringResource(
        when (scenario) {
            MockNetworkScenario.HOME_4G -> R.string.mock_scenario_home_4g
            MockNetworkScenario.HOME_2G -> R.string.mock_scenario_home_2g
            MockNetworkScenario.ALT_OPERATOR_4G -> R.string.mock_scenario_alt_4g
            MockNetworkScenario.ALT_OPERATOR_2G -> R.string.mock_scenario_alt_2g
            MockNetworkScenario.NO_SERVICE -> R.string.mock_scenario_no_service
            MockNetworkScenario.SEARCHING_2G -> R.string.mock_scenario_searching_2g
            MockNetworkScenario.HOME_5G_ENDC -> R.string.mock_scenario_home_5g_endc
        }
    )
}

@Composable
private fun mockScenarioHint(scenario: MockNetworkScenario): String {
    return stringResource(
        when (scenario) {
            MockNetworkScenario.HOME_4G -> R.string.mock_scenario_home_4g_hint
            MockNetworkScenario.HOME_2G -> R.string.mock_scenario_home_2g_hint
            MockNetworkScenario.ALT_OPERATOR_4G -> R.string.mock_scenario_alt_4g_hint
            MockNetworkScenario.ALT_OPERATOR_2G -> R.string.mock_scenario_alt_2g_hint
            MockNetworkScenario.NO_SERVICE -> R.string.mock_scenario_no_service_hint
            MockNetworkScenario.SEARCHING_2G -> R.string.mock_scenario_searching_2g_hint
            MockNetworkScenario.HOME_5G_ENDC -> R.string.mock_scenario_home_5g_endc_hint
        }
    )
}

@Composable
private fun MockSignalSlider(
    label: String,
    value: Int,
    valueRange: IntRange,
    unit: String,
    onValueChange: (Int) -> Unit
) {
    if (!TierSliderSupport.isValidIntRange(valueRange)) return

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
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.passive_signal_bound_value, value, unit),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { newValue -> onValueChange(newValue.roundToInt()) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = sliderSteps
        )
    }
}
