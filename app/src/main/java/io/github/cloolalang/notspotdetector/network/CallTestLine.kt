package io.github.cloolalang.notspotdetector.network

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.telecom.Call
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.app.role.RoleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CallTestLiveCall(
    val number: String,
    val state: Int,
    val hangUpAtElapsedMs: Long
)

/**
 * Calls this app owns while it is the phone app. [armHangup] ends the live call when the
 * call-test duration runs out, on the main thread, even if the test loop is in the background.
 */
object CallTestLine {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val calls = LinkedHashMap<Call, Call.Callback>()
    private var hangupGeneration = 0
    private var hangUpAtElapsedMs = 0L

    private val _live = MutableStateFlow<CallTestLiveCall?>(null)
    val live: StateFlow<CallTestLiveCall?> = _live.asStateFlow()

    @Volatile
    private var testActive = false

    fun isTestActive(): Boolean = testActive

    fun setTestActive(active: Boolean) {
        testActive = active
    }

    fun track(call: Call) {
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                if (state == Call.STATE_DISCONNECTED) {
                    untrack(call)
                } else {
                    publish()
                }
            }
        }
        synchronized(this) {
            calls[call] = callback
        }
        call.registerCallback(callback)
        publish()
    }

    fun untrack(call: Call) {
        val callback = synchronized(this) { calls.remove(call) }
        if (callback != null) {
            runCatching { call.unregisterCallback(callback) }
        }
        publish()
    }

    fun armHangup(delayMs: Long) {
        val token = synchronized(this) { ++hangupGeneration }
        val due = SystemClock.elapsedRealtime() + delayMs.coerceAtLeast(0L)
        synchronized(this) { hangUpAtElapsedMs = due }
        publish()
        mainHandler.postDelayed({
            if (token == hangupGeneration) disconnectAll()
        }, delayMs.coerceAtLeast(0L))
    }

    fun cancelHangup() {
        synchronized(this) {
            hangupGeneration++
            hangUpAtElapsedMs = 0L
        }
        publish()
    }

    /** Ends the live call. Returns true when this app is holding one. */
    fun hangUpNow(): Boolean {
        val hadCall = synchronized(this) {
            hangupGeneration++
            hangUpAtElapsedMs = 0L
            calls.isNotEmpty()
        }
        publish()
        mainHandler.post { disconnectAll() }
        return hadCall
    }

    fun answerRinging() {
        mainHandler.post {
            ringingCall()?.let { call ->
                runCatching { call.answer(VideoProfile.STATE_AUDIO_ONLY) }
            }
        }
    }

    fun rejectRinging() {
        mainHandler.post {
            ringingCall()?.let { call ->
                runCatching { call.reject(false, null) }
            }
        }
    }

    private fun ringingCall(): Call? {
        return synchronized(this) {
            calls.keys.firstOrNull { it.telecomState() == Call.STATE_RINGING }
        }
    }

    private fun disconnectAll() {
        val snapshot = synchronized(this) { calls.keys.toList() }
        snapshot.forEach { call ->
            val state = call.telecomState()
            if (!isEnding(state)) {
                runCatching { call.disconnect() }
            }
        }
    }

    private fun publish() {
        val snapshot = synchronized(this) {
            val call = calls.keys.lastOrNull { it.telecomState() != Call.STATE_DISCONNECTED }
            val due = hangUpAtElapsedMs
            call to due
        }
        val call = snapshot.first
        _live.value = if (call == null) {
            null
        } else {
            CallTestLiveCall(
                number = call.details.handle?.schemeSpecificPart.orEmpty(),
                state = call.telecomState(),
                hangUpAtElapsedMs = snapshot.second
            )
        }
    }
}

private fun isEnding(state: Int): Boolean {
    if (state == Call.STATE_DISCONNECTED) return true
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && state == Call.STATE_DISCONNECTING
}

internal fun Call.telecomState(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        details.state
    } else {
        @Suppress("DEPRECATION")
        state
    }
}

object CallTestDialer {
    fun isDefault(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
            return roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) &&
                roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
        }
        val telecom = context.getSystemService(TelecomManager::class.java) ?: return false
        return telecom.defaultDialerPackage == context.packageName
    }
}
