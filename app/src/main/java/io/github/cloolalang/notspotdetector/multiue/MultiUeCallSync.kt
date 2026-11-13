package io.github.cloolalang.notspotdetector.multiue

import io.github.cloolalang.notspotdetector.model.CallTestSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Call-test number, duration, delay, and whether the test is running.
 * The master publishes them. A connected slave applies them on its own phone.
 */
object MultiUeCallSync {
    data class Config(
        val phoneNumber: String,
        val callDurationSec: Int,
        val delayBetweenCallsSec: Int
    )

    private val _config = MutableStateFlow<Config?>(null)
    val config: StateFlow<Config?> = _config.asStateFlow()

    private val _running = MutableStateFlow<Boolean?>(null)
    val running: StateFlow<Boolean?> = _running.asStateFlow()

    fun setConfig(settings: CallTestSettings) {
        val normalized = settings.normalized()
        val next = Config(
            phoneNumber = normalized.phoneNumber,
            callDurationSec = normalized.callDurationSec,
            delayBetweenCallsSec = normalized.delayBetweenCallsSec
        )
        if (_config.value == next) return
        _config.value = next
    }

    fun setRunning(running: Boolean) {
        if (_running.value == running) return
        _running.value = running
    }
}
