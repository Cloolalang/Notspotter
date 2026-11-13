@file:Suppress("DEPRECATION")

package io.github.cloolalang.notspotdetector.network

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import io.github.cloolalang.notspotdetector.model.CallResultBand
import io.github.cloolalang.notspotdetector.model.CallTestPhase
import io.github.cloolalang.notspotdetector.model.CallTestScore
import io.github.cloolalang.notspotdetector.model.CallTestSettings
import io.github.cloolalang.notspotdetector.model.CallTestStatus
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.atomic.AtomicInteger

/**
 * Places the configured number in a loop and scores each attempt.
 *
 * A success is the call staying off-hook for the requested duration. Off-hook starts when this
 * phone begins the call, so it does not prove the far end answered. Ordinary apps cannot always
 * hang up; when the call stays up, the loop waits until it returns to idle before the next one.
 */
class CallTestRunner(context: Context) {

    private val appContext = context.applicationContext
    private val callState = AtomicInteger(TelephonyManager.CALL_STATE_IDLE)
    private var telephonyCallback: TelephonyCallback? = null
    private var phoneStateListener: PhoneStateListener? = null

    suspend fun run(
        settings: () -> CallTestSettings,
        onStatus: (CallTestStatus) -> Unit
    ) {
        var attempts = 0
        var successes = 0
        var failures = 0
        var blocked = false
        var lastOutcome: CallResultBand? = null

        fun status(running: Boolean, phase: CallTestPhase, markBlocked: Boolean = false): CallTestStatus {
            return CallTestStatus(
                running = running,
                phase = phase,
                attempts = attempts,
                successes = successes,
                failures = failures,
                blocked = markBlocked,
                lastOutcome = lastOutcome
            )
        }

        try {
            if (!startListening()) {
                blocked = true
                onStatus(status(running = false, phase = CallTestPhase.IDLE, markBlocked = true))
                return
            }
            CallTestLine.setTestActive(true)
            while (currentCoroutineContext().isActive) {
                val current = settings().normalized()
                val number = CallTestScore.dialableNumber(current.phoneNumber)
                if (number == null) {
                    onStatus(status(running = true, phase = CallTestPhase.IDLE))
                    delay(400)
                    continue
                }
                if (callState.get() != TelephonyManager.CALL_STATE_IDLE) {
                    onStatus(status(running = true, phase = CallTestPhase.WAITING_FOR_HANGUP))
                    hangUp()
                    awaitCallState(TelephonyManager.CALL_STATE_IDLE, CallTestSettings.HANGUP_WAIT_MS)
                    if (!currentCoroutineContext().isActive) break
                    if (callState.get() != TelephonyManager.CALL_STATE_IDLE) {
                        delay(500)
                        continue
                    }
                }

                onStatus(status(running = true, phase = CallTestPhase.DIALING))
                val placed = runCatching { placeCall(number) }.isSuccess
                if (!currentCoroutineContext().isActive) break
                if (!placed) {
                    attempts++
                    failures++
                    lastOutcome = CallResultBand.FAIL
                    onStatus(status(running = true, phase = CallTestPhase.GAP))
                    delay(gapMs(settings))
                    continue
                }

                val connected = awaitCallState(
                    TelephonyManager.CALL_STATE_OFFHOOK,
                    CallTestSettings.SETUP_TIMEOUT_MS
                )
                if (!currentCoroutineContext().isActive) break
                if (!connected) {
                    attempts++
                    failures++
                    lastOutcome = CallResultBand.FAIL
                    hangUp()
                    awaitCallState(TelephonyManager.CALL_STATE_IDLE, CallTestSettings.HANGUP_WAIT_MS)
                    if (!currentCoroutineContext().isActive) break
                    onStatus(status(running = true, phase = CallTestPhase.GAP))
                    delay(gapMs(settings))
                    continue
                }

                onStatus(status(running = true, phase = CallTestPhase.IN_CALL))
                val held = holdOffHook(current.callDurationSec * 1_000L)
                if (!currentCoroutineContext().isActive) break
                attempts++
                if (held) {
                    successes++
                    lastOutcome = CallResultBand.SUCCESS
                } else {
                    failures++
                    lastOutcome = CallResultBand.FAIL
                }
                onStatus(status(running = true, phase = CallTestPhase.GAP))
                hangUp()
                val idle = awaitCallState(
                    TelephonyManager.CALL_STATE_IDLE,
                    CallTestSettings.HANGUP_WAIT_MS
                )
                if (!currentCoroutineContext().isActive) break
                if (!idle) {
                    onStatus(status(running = true, phase = CallTestPhase.WAITING_FOR_HANGUP))
                    while (
                        currentCoroutineContext().isActive &&
                        callState.get() != TelephonyManager.CALL_STATE_IDLE
                    ) {
                        delay(500)
                    }
                    if (!currentCoroutineContext().isActive) break
                }
                onStatus(status(running = true, phase = CallTestPhase.GAP))
                delay(gapMs(settings))
            }
        } finally {
            hangUp()
            stopListening()
            CallTestLine.setTestActive(false)
            onStatus(
                if (blocked) {
                    status(running = false, phase = CallTestPhase.IDLE, markBlocked = true)
                } else {
                    status(running = false, phase = CallTestPhase.IDLE)
                }
            )
        }
    }

    private fun gapMs(settings: () -> CallTestSettings): Long {
        return settings().normalized().delayBetweenCallsSec * 1_000L
    }

    private fun placeCall(number: String) {
        val intent = Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(intent)
    }

    private suspend fun awaitCallState(target: Int, timeoutMs: Long): Boolean {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (currentCoroutineContext().isActive) {
            if (callState.get() == target) return true
            if (SystemClock.elapsedRealtime() >= deadline) return false
            delay(200)
        }
        return false
    }

    private suspend fun holdOffHook(durationMs: Long): Boolean {
        val deadline = SystemClock.elapsedRealtime() + durationMs
        CallTestLine.armHangup(durationMs)
        while (currentCoroutineContext().isActive) {
            if (SystemClock.elapsedRealtime() >= deadline) return true
            if (callState.get() != TelephonyManager.CALL_STATE_OFFHOOK) {
                CallTestLine.cancelHangup()
                return false
            }
            delay(200)
        }
        return false
    }

    private fun startListening(): Boolean {
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val telephony = appContext.getSystemService(TelephonyManager::class.java) ?: return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        callState.set(state)
                    }
                }
                telephonyCallback = callback
                telephony.registerTelephonyCallback(appContext.mainExecutor, callback)
            } else {
                listenLegacy(telephony)
            }
            true
        }.getOrDefault(false)
    }

    @Suppress("DEPRECATION")
    private fun listenLegacy(telephony: TelephonyManager) {
        val listener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                callState.set(state)
            }
        }
        phoneStateListener = listener
        telephony.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun stopListening() {
        val telephony = appContext.getSystemService(TelephonyManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { callback ->
                runCatching { telephony.unregisterTelephonyCallback(callback) }
            }
            telephonyCallback = null
        } else {
            @Suppress("DEPRECATION")
            phoneStateListener?.let { listener ->
                runCatching { telephony.listen(listener, PhoneStateListener.LISTEN_NONE) }
            }
            phoneStateListener = null
        }
    }

    private fun hangUp() {
        CallTestLine.hangUpNow()
        CallHangup.endCall(appContext)
    }
}

/**
 * Best-effort hang-up. Current Android builds often refuse this for an app that is not the
 * default dialer. The caller waits for idle when it does not work.
 */
internal object CallHangup {
    fun endCall(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val telecom = context.getSystemService(TelecomManager::class.java)
            if (telecom != null) {
                val ended = runCatching {
                    val method = TelecomManager::class.java.getDeclaredMethod("endCall")
                    method.isAccessible = true
                    method.invoke(telecom) as? Boolean ?: false
                }.getOrDefault(false)
                if (ended) return true
            }
        }
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) ?: return false
        return runCatching {
            val getITelephony = telephony.javaClass.getDeclaredMethod("getITelephony")
            getITelephony.isAccessible = true
            val binder = getITelephony.invoke(telephony) ?: return false
            val endCall = binder.javaClass.getDeclaredMethod("endCall")
            endCall.isAccessible = true
            endCall.invoke(binder) as? Boolean ?: false
        }.getOrDefault(false)
    }
}
