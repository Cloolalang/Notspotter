package io.github.cloolalang.notspotdetector.data

import android.content.Context

class RadioDebugSettingsRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun saveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    private companion object {
        const val PREFS_NAME = "radio_debug_settings"
        const val KEY_ENABLED = "radio_debug_enabled"
    }
}
