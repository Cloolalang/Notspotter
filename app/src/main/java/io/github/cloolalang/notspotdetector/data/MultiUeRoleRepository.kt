package io.github.cloolalang.notspotdetector.data

import android.content.Context
import io.github.cloolalang.notspotdetector.model.MultiUeRole

/**
 * Role and last master address for this phone.
 * Kept out of settings profiles so loading a profile does not turn a slave into the master.
 */
class MultiUeRoleRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadRole(): MultiUeRole {
        val stored = prefs.getString(KEY_ROLE, null) ?: return MultiUeRole.OFF
        return MultiUeRole.entries.firstOrNull { it.name == stored } ?: MultiUeRole.OFF
    }

    fun saveRole(role: MultiUeRole) {
        prefs.edit().putString(KEY_ROLE, role.name).apply()
    }

    fun loadMasterAddress(): String? {
        return prefs.getString(KEY_MASTER_ADDRESS, null)?.takeIf { it.isNotBlank() }
    }

    fun saveMasterAddress(address: String) {
        prefs.edit().putString(KEY_MASTER_ADDRESS, address).apply()
    }

    fun loadMasterSyncSettings(): Boolean = prefs.getBoolean(KEY_MASTER_SYNC, false)

    fun saveMasterSyncSettings(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MASTER_SYNC, enabled).apply()
    }

    fun loadSlaveSyncSettings(): Boolean = prefs.getBoolean(KEY_SLAVE_SYNC, false)

    fun saveSlaveSyncSettings(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SLAVE_SYNC, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "notspot_multi_ue"
        private const val KEY_ROLE = "role"
        private const val KEY_MASTER_ADDRESS = "master_address"
        private const val KEY_MASTER_SYNC = "master_sync_settings"
        private const val KEY_SLAVE_SYNC = "slave_sync_settings"
    }
}
