package io.github.cloolalang.notspotdetector.data

import android.content.Context
import io.github.cloolalang.notspotdetector.model.AppSettingsSnapshot
import io.github.cloolalang.notspotdetector.model.SettingsProfile
import io.github.cloolalang.notspotdetector.model.SettingsProfileSummary
import java.util.UUID

class SettingsProfilesRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun listSummaries(): List<SettingsProfileSummary> {
        return loadProfiles().map { profile ->
            SettingsProfileSummary(
                id = profile.id,
                name = profile.name,
                savedAtMs = profile.savedAtMs,
                differsFromDefaults = !profile.settings.isDefault()
            )
        }.sortedBy { it.name.lowercase() }
    }

    fun loadProfiles(): List<SettingsProfile> {
        return AppSettingsSnapshotCodec.decodeProfiles(prefs.getString(KEY_PROFILES_JSON, "") ?: "")
    }

    fun findById(id: String): SettingsProfile? {
        return loadProfiles().firstOrNull { it.id == id }
    }

    fun saveProfile(name: String, settings: AppSettingsSnapshot): SettingsProfile {
        val trimmedName = name.trim()
        val profile = SettingsProfile(
            id = UUID.randomUUID().toString(),
            name = trimmedName,
            savedAtMs = System.currentTimeMillis(),
            settings = settings.normalized()
        )
        val updated = loadProfiles() + profile
        persist(updated)
        return profile
    }

    fun deleteProfile(id: String): Boolean {
        val updated = loadProfiles().filterNot { it.id == id }
        if (updated.size == loadProfiles().size) return false
        persist(updated)
        return true
    }

    fun hasName(name: String, ignoreId: String? = null): Boolean {
        val target = name.trim().lowercase()
        return loadProfiles().any { profile ->
            profile.id != ignoreId && profile.name.trim().lowercase() == target
        }
    }

    private fun persist(profiles: List<SettingsProfile>) {
        prefs.edit()
            .putString(KEY_PROFILES_JSON, AppSettingsSnapshotCodec.encodeProfiles(profiles))
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "notspot_settings_profiles"
        private const val KEY_PROFILES_JSON = "profiles_json"
        const val MAX_PROFILES = 24
        const val MAX_NAME_LENGTH = 40
    }
}
