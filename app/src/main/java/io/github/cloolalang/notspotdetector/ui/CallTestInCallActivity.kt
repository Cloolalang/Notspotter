package io.github.cloolalang.notspotdetector.ui

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.telecom.Call
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.network.CallTestLine
import io.github.cloolalang.notspotdetector.ui.theme.CallTestIndigo
import io.github.cloolalang.notspotdetector.ui.theme.CallTestStop
import io.github.cloolalang.notspotdetector.ui.theme.OnCallTestIndigo
import io.github.cloolalang.notspotdetector.ui.theme.NotspotDetectorTheme
import io.github.cloolalang.notspotdetector.ui.theme.OnCallTestStop
import androidx.compose.material3.ButtonDefaults
import kotlinx.coroutines.delay

class CallTestInCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        setContent {
            NotspotDetectorTheme {
                CallTestInCallScreen(onFinished = { finish() })
            }
        }
    }
}

@Composable
private fun CallTestInCallScreen(onFinished: () -> Unit) {
    val live by CallTestLine.live.collectAsStateWithLifecycle()
    val call = live
    LaunchedEffect(call) {
        if (call == null) onFinished()
    }
    if (call == null) return

    var nowElapsed by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(call.hangUpAtElapsedMs) {
        while (true) {
            nowElapsed = SystemClock.elapsedRealtime()
            delay(250)
        }
    }
    val remainingSec = if (call.hangUpAtElapsedMs > nowElapsed) {
        ((call.hangUpAtElapsedMs - nowElapsed) / 1_000L).toInt() + 1
    } else {
        null
    }
    val ringing = call.state == Call.STATE_RINGING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.call_test_incall_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = call.number.ifBlank { stringResource(R.string.call_test_incall_unknown) },
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = inCallStateLabel(call.state),
            style = MaterialTheme.typography.bodyLarge
        )
        if (remainingSec != null) {
            Text(
                text = stringResource(R.string.call_test_incall_hangup_in, remainingSec),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
        if (ringing) {
            Button(
                onClick = { CallTestLine.answerRinging() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.call_test_incall_answer))
            }
            OutlinedButton(
                onClick = { CallTestLine.rejectRinging() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.call_test_incall_reject))
            }
        } else {
            Button(
                onClick = { CallTestLine.hangUpNow() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CallTestStop,
                    contentColor = OnCallTestStop
                )
            ) {
                Text(stringResource(R.string.call_test_incall_hangup))
            }
        }
        OutlinedButton(
            onClick = onFinished,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.call_test_incall_back))
        }
    }
}

@Composable
private fun inCallStateLabel(state: Int): String {
    val res = when (state) {
        Call.STATE_DIALING, Call.STATE_CONNECTING -> R.string.call_test_phase_dialing
        Call.STATE_RINGING -> R.string.call_test_incall_ringing
        Call.STATE_ACTIVE -> R.string.call_test_phase_in_call
        Call.STATE_HOLDING -> R.string.call_test_incall_holding
        Call.STATE_DISCONNECTING -> R.string.call_test_incall_ending
        else -> R.string.call_test_phase_in_call
    }
    return stringResource(res)
}

/**
 * One row on the main screen while a call test is up, so the charts stay in view.
 * A call outside a test still uses the full call screen.
 */
@Composable
fun CallTestMiniBar(modifier: Modifier = Modifier) {
    val live by CallTestLine.live.collectAsStateWithLifecycle()
    val call = live
    if (call == null || !CallTestLine.isTestActive()) return

    var nowElapsed by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(call.hangUpAtElapsedMs) {
        while (true) {
            nowElapsed = SystemClock.elapsedRealtime()
            delay(250)
        }
    }
    val remainingSec = if (call.hangUpAtElapsedMs > nowElapsed) {
        ((call.hangUpAtElapsedMs - nowElapsed) / 1_000L).toInt() + 1
    } else {
        null
    }
    val ringing = call.state == Call.STATE_RINGING
    val detail = when {
        ringing -> stringResource(R.string.call_test_incall_ringing)
        remainingSec != null -> stringResource(R.string.call_test_incall_hangup_in, remainingSec)
        else -> inCallStateLabel(call.state)
    }
    val number = call.number.ifBlank { stringResource(R.string.call_test_incall_unknown) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CallTestIndigo, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$number · $detail",
            modifier = Modifier.weight(1f),
            color = OnCallTestIndigo,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (ringing) {
            TextButton(onClick = { CallTestLine.answerRinging() }) {
                Text(stringResource(R.string.call_test_incall_answer), color = OnCallTestIndigo)
            }
            TextButton(onClick = { CallTestLine.rejectRinging() }) {
                Text(stringResource(R.string.call_test_incall_reject), color = OnCallTestIndigo)
            }
        } else {
            TextButton(onClick = { CallTestLine.hangUpNow() }) {
                Text(stringResource(R.string.call_test_incall_hangup), color = OnCallTestIndigo)
            }
        }
    }
}
