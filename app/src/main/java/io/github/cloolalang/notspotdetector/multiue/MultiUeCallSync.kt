package io.github.cloolalang.notspotdetector.multiue

import io.github.cloolalang.notspotdetector.model.CallTestSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Call-test number, duration, delay, master timeout, mute, and whether the test is running.
 * The master publishes them. A connected slave applies them on its own phone.
 */
object MultiUeCallSync {
    data class Config(
        val phoneNumber: String,
        val callDurationSec: Int,
        val delayBetweenCallsSec: Int,
        val masterTimeoutEnabled: Boolean,
        val muteCallAudio: Boolean
    )

    private val _config = MutableStateFlow<Config?>(null)
    val config: StateFlow<Config?> = _config.asStateFlow()

    private val _running = MutableStateFlow<Boolean?>(null)
    val running: StateFlow<Boolean?> = _running.asStateFlow()

    /**
     * A timeout or mute line can arrive before the number line. Held here until
     * the number line is applied, so that line does not invent an empty number.
     */
    private var pendingMasterTimeout: Boolean? = null
    private var pendingMuteCallAudio: Boolean? = null

    /** Master publish. The settings already include the timeout and mute. */
    fun setConfig(settings: CallTestSettings) {
        pendingMasterTimeout = null
        pendingMuteCallAudio = null
        publish(settings.normalized())
    }

    /** Slave received the number, duration, and delay. Keeps a timeout or mute that arrived first. */
    fun setSlaveConfig(phoneNumber: String, callDurationSec: Int, delayBetweenCallsSec: Int) {
        val current = _config.value
        val timeout = pendingMasterTimeout
            ?: current?.masterTimeoutEnabled
            ?: CallTestSettings.DEFAULT_MASTER_TIMEOUT_ENABLED
        val mute = pendingMuteCallAudio
            ?: current?.muteCallAudio
            ?: CallTestSettings.DEFAULT_MUTE_CALL_AUDIO
        pendingMasterTimeout = null
        pendingMuteCallAudio = null
        publish(
            CallTestSettings(
                phoneNumber = phoneNumber,
                callDurationSec = callDurationSec,
                delayBetweenCallsSec = delayBetweenCallsSec,
                masterTimeoutEnabled = timeout,
                muteCallAudio = mute
            ).normalized()
        )
    }

    fun setMasterTimeout(enabled: Boolean) {
        val current = _config.value
        if (current == null) {
            pendingMasterTimeout = enabled
            return
        }
        pendingMasterTimeout = null
        if (current.masterTimeoutEnabled == enabled) return
        _config.value = current.copy(masterTimeoutEnabled = enabled)
    }

    fun setMuteCallAudio(enabled: Boolean) {
        val current = _config.value
        if (current == null) {
            pendingMuteCallAudio = enabled
            return
        }
        pendingMuteCallAudio = null
        if (current.muteCallAudio == enabled) return
        _config.value = current.copy(muteCallAudio = enabled)
    }

    private fun publish(normalized: CallTestSettings) {
        val next = Config(
            phoneNumber = normalized.phoneNumber,
            callDurationSec = normalized.callDurationSec,
            delayBetweenCallsSec = normalized.delayBetweenCallsSec,
            masterTimeoutEnabled = normalized.masterTimeoutEnabled,
            muteCallAudio = normalized.muteCallAudio
        )
        if (_config.value == next) return
        _config.value = next
    }

    fun setRunning(running: Boolean) {
        if (_running.value == running) return
        _running.value = running
    }
}
