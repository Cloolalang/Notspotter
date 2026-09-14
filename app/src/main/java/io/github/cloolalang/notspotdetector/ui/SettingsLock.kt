package io.github.cloolalang.notspotdetector.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.cloolalang.notspotdetector.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicLong

val LocalSettingsUnlocked = compositionLocalOf { false }

internal const val SETTINGS_UNLOCK_IDLE_TIMEOUT_MS = 10 * 60 * 1000L

@Composable
fun settingsControlsEnabled(): Boolean = LocalSettingsUnlocked.current

/**
 * Re-locks settings after [SETTINGS_UNLOCK_IDLE_TIMEOUT_MS] with no pointer activity in the app.
 * Returns a modifier that observes touches without consuming them.
 */
@Composable
fun rememberSettingsUnlockIdleLock(
    unlocked: Boolean,
    onLock: () -> Unit
): Modifier {
    val lastActivityElapsedMs = remember { AtomicLong(SystemClock.elapsedRealtime()) }
    LaunchedEffect(unlocked) {
        if (!unlocked) return@LaunchedEffect
        lastActivityElapsedMs.set(SystemClock.elapsedRealtime())
        while (isActive) {
            val remaining = SETTINGS_UNLOCK_IDLE_TIMEOUT_MS -
                (SystemClock.elapsedRealtime() - lastActivityElapsedMs.get())
            if (remaining <= 0L) {
                onLock()
                return@LaunchedEffect
            }
            delay(remaining)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (unlocked &&
            SystemClock.elapsedRealtime() - lastActivityElapsedMs.get() >= SETTINGS_UNLOCK_IDLE_TIMEOUT_MS
        ) {
            onLock()
        }
    }
    return Modifier.pointerInput(unlocked) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial)
                if (unlocked) {
                    lastActivityElapsedMs.set(SystemClock.elapsedRealtime())
                }
            }
        }
    }
}

@Composable
fun SettingsLockBar(
    unlocked: Boolean,
    onUnlockedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(
                    if (unlocked) R.string.settings_lock_unlocked_title else R.string.settings_lock_locked_title
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(
                    if (unlocked) R.string.settings_lock_unlocked_hint else R.string.settings_lock_locked_hint
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (unlocked) {
                OutlinedButton(onClick = {
                    password = ""
                    showError = false
                    onUnlockedChange(false)
                }) {
                    Text(text = stringResource(R.string.settings_lock_lock))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            showError = false
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.settings_lock_password_label)) },
                        singleLine = true,
                        isError = showError,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    OutlinedButton(onClick = {
                        if (password == SETTINGS_LOCK_PASSWORD) {
                            showError = false
                            password = ""
                            onUnlockedChange(true)
                        } else {
                            showError = true
                        }
                    }) {
                        Text(text = stringResource(R.string.settings_lock_unlock))
                    }
                }
                if (showError) {
                    Text(
                        text = stringResource(R.string.settings_lock_wrong_password),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private const val SETTINGS_LOCK_PASSWORD = "1234"
