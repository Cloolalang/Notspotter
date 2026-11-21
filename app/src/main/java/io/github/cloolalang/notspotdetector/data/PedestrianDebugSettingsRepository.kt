package io.github.cloolalang.notspotdetector.data

import android.content.Context

class PedestrianDebugSettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun saveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "pedestrian_debug_settings"
        private const val KEY_ENABLED = "pedestrian_debug_enabled"
    }
}
