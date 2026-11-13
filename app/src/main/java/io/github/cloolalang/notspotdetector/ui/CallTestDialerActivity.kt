package io.github.cloolalang.notspotdetector.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.CallTestScore
import io.github.cloolalang.notspotdetector.ui.theme.CallTestIndigo
import io.github.cloolalang.notspotdetector.ui.theme.NotspotDetectorTheme
import io.github.cloolalang.notspotdetector.ui.theme.OnCallTestIndigo
import androidx.compose.material3.ButtonDefaults

/**
 * Required so Android will accept NotSpotter as the phone app. That role is what lets a
 * call test hang up when the duration ends.
 */
class CallTestDialerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preset = intent?.data?.schemeSpecificPart.orEmpty()
        setContent {
            NotspotDetectorTheme {
                CallTestDialerScreen(preset = preset, onCall = { number -> place(number) })
            }
        }
    }

    private fun place(number: String) {
        val dialable = CallTestScore.dialableNumber(number) ?: return
        startActivity(
            Intent(Intent.ACTION_CALL, Uri.fromParts("tel", dialable, null))
        )
    }
}

@Composable
private fun CallTestDialerScreen(preset: String, onCall: (String) -> Unit) {
    var number by rememberSaveable { mutableStateOf(preset) }
    val dialable = CallTestScore.dialableNumber(number) != null
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.call_test_dialer_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.call_test_dialer_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = number,
            onValueChange = { number = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.call_test_number_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        Button(
            onClick = { onCall(number) },
            enabled = dialable,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = CallTestIndigo,
                contentColor = OnCallTestIndigo
            )
        ) {
            Text(stringResource(R.string.call_test_dialer_call))
        }
    }
}
