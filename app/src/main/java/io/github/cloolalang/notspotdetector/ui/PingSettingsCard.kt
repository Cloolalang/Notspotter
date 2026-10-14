package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.PingSettings
import io.github.cloolalang.notspotdetector.ui.theme.Sushi
import kotlin.math.roundToInt

@Composable
fun PingSettingsCard(
    pingSettings: PingSettings,
    onAddressChange: (String) -> Unit,
    onPingsPerTestChange: (Int) -> Unit,
    onTestIntervalChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var addressInput by rememberSaveable { mutableStateOf(pingSettings.displayAddress) }
    var addressFocused by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(pingSettings.displayAddress) {
        if (!addressFocused) {
            addressInput = pingSettings.displayAddress
        }
    }

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
                    text = stringResource(R.string.ping_settings_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Sushi,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Text(
                    text = stringResource(R.string.ping_settings_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = addressInput,
                    onValueChange = { addressInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            addressFocused = focusState.isFocused
                            if (!focusState.isFocused) {
                                onAddressChange(addressInput)
                            }
                        },
                    label = { Text(stringResource(R.string.ping_address_label)) },
                    supportingText = { Text(stringResource(R.string.ping_address_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onAddressChange(addressInput) }
                    )
                )

                PingCountSlider(
                    value = pingSettings.pingsPerTest,
                    onValueChange = onPingsPerTestChange
                )

                TestIntervalSlider(
                    valueSeconds = (pingSettings.testIntervalMs / 1_000L).toInt(),
                    onValueChange = { onTestIntervalChange(it * 1_000L) }
                )
            }
        }
    }
}

@Composable
private fun TestIntervalSlider(
    valueSeconds: Int,
    onValueChange: (Int) -> Unit
) {
    val minSeconds = (PingSettings.MIN_TEST_INTERVAL_MS / 1_000).toInt()
    val maxSeconds = (PingSettings.MAX_TEST_INTERVAL_MS / 1_000).toInt()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.test_interval_label),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.test_interval_value, valueSeconds),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            value = valueSeconds.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = minSeconds.toFloat()..maxSeconds.toFloat(),
            steps = (maxSeconds - minSeconds) / 5 - 1
        )
        Text(
            text = stringResource(R.string.test_interval_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PingCountSlider(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.pings_per_test_label),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = if (value == 1) {
                    stringResource(R.string.pings_per_test_single)
                } else {
                    stringResource(R.string.pings_per_test_multiple, value)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = PingSettings.MIN_PINGS_PER_TEST.toFloat()..PingSettings.MAX_PINGS_PER_TEST.toFloat(),
            steps = PingSettings.MAX_PINGS_PER_TEST - PingSettings.MIN_PINGS_PER_TEST - 1
        )
        Text(
            text = stringResource(R.string.pings_per_test_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
