package io.github.cloolalang.notspotdetector.data

import android.content.Context
import io.github.cloolalang.notspotdetector.model.CallTestSettings

class CallTestSettingsRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): CallTestSettings {
        return CallTestSettings(
            phoneNumber = prefs.getString(KEY_NUMBER, "") ?: "",
            callDurationSec = prefs.getInt(KEY_DURATION, CallTestSettings.DEFAULT_DURATION_SEC),
            delayBetweenCallsSec = prefs.getInt(KEY_DELAY, CallTestSettings.DEFAULT_DELAY_SEC),
            masterTimeoutEnabled = prefs.getBoolean(
                KEY_MASTER_TIMEOUT,
                CallTestSettings.DEFAULT_MASTER_TIMEOUT_ENABLED
            )
        ).normalized()
    }

    fun save(settings: CallTestSettings) {
        val normalized = settings.normalized()
        prefs.edit()
            .putString(KEY_NUMBER, normalized.phoneNumber)
            .putInt(KEY_DURATION, normalized.callDurationSec)
            .putInt(KEY_DELAY, normalized.delayBetweenCallsSec)
            .putBoolean(KEY_MASTER_TIMEOUT, normalized.masterTimeoutEnabled)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "notspot_call_test"
        private const val KEY_NUMBER = "phone_number"
        private const val KEY_DURATION = "call_duration_sec"
        private const val KEY_DELAY = "delay_between_calls_sec"
        private const val KEY_MASTER_TIMEOUT = "master_timeout_enabled"
    }
}
