package io.github.cloolalang.notspotdetector.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import io.github.cloolalang.notspotdetector.model.AppSettingsSnapshot
import io.github.cloolalang.notspotdetector.model.ProfileExportOutcome
import io.github.cloolalang.notspotdetector.model.ProfileExportResult
import io.github.cloolalang.notspotdetector.model.ProfileImportResult
import io.github.cloolalang.notspotdetector.model.SettingsProfile
import io.github.cloolalang.notspotdetector.model.SettingsProfileSummary
import java.io.File
import java.util.UUID

class SettingsProfilesRepository(context: Context) {

    private val appContext = context.applicationContext
    private val legacyPrefs = appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
    private val profilesDir: File = resolveProfilesDir()

    init {
        migrateFromSharedPreferencesIfNeeded()
        migrateExternalProfileFilesIfNeeded()
    }

    fun profilesDirectory(): File = profilesDir

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
        return profileJsonFilesIn(profilesDir).mapNotNull(::readProfileFile)
    }

    fun findById(id: String): SettingsProfile? {
        return readProfileFile(profileFile(id))
    }

    fun exportProfileToDownloads(context: Context, id: String): ProfileExportOutcome {
        val profile = findById(id)
            ?: return ProfileExportOutcome(ProfileExportResult.ProfileNotFound)
        return SettingsProfileDownloadsExporter.exportProfile(context, profile)
    }

    fun profileFile(id: String): File = File(profilesDir, "$id$PROFILE_FILE_EXTENSION")

    fun saveProfile(name: String, settings: AppSettingsSnapshot): Boolean {
        val trimmedName = name.trim()
        val profile = SettingsProfile(
            id = UUID.randomUUID().toString(),
            name = trimmedName,
            savedAtMs = System.currentTimeMillis(),
            settings = settings.normalized()
        )
        return writeProfileFile(profile)
    }

    fun deleteProfile(id: String): Boolean {
        val file = profileFile(id)
        if (!file.exists()) return false
        return file.delete()
    }

    fun hasName(name: String, ignoreId: String? = null): Boolean {
        val target = name.trim().lowercase()
        return loadProfiles().any { profile ->
            profile.id != ignoreId && profile.name.trim().lowercase() == target
        }
    }

    fun importFromUri(uri: Uri): ProfileImportResult {
        if (loadProfiles().size >= MAX_PROFILES) return ProfileImportResult.TooManyProfiles
        val json = appContext.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader().readText()
        } ?: return ProfileImportResult.InvalidFile
        return importFromJson(json)
    }

    fun importFromJson(json: String): ProfileImportResult {
        if (loadProfiles().size >= MAX_PROFILES) return ProfileImportResult.TooManyProfiles
        val decoded = AppSettingsSnapshotCodec.decodeProfile(json) ?: return ProfileImportResult.InvalidFile
        val profile = SettingsProfile(
            id = UUID.randomUUID().toString(),
            name = uniqueImportName(decoded.name.trim()),
            savedAtMs = System.currentTimeMillis(),
            settings = decoded.settings.normalized()
        )
        return if (writeProfileFile(profile)) {
            ProfileImportResult.Imported
        } else {
            ProfileImportResult.Failed
        }
    }

    private fun resolveProfilesDir(): File {
        return File(appContext.filesDir, PROFILES_FOLDER_NAME).apply { mkdirs() }
    }

    private fun migrateExternalProfileFilesIfNeeded() {
        val externalDir = legacyExternalProfilesDir() ?: return
        for (file in profileJsonFilesIn(externalDir)) {
            val target = File(profilesDir, file.name)
            if (!target.exists()) {
                runCatching { file.copyTo(target, overwrite = false) }
            }
        }
    }

    private fun legacyExternalProfilesDir(): File? {
        val externalDocuments = appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return null
        val dir = File(externalDocuments, PROFILES_FOLDER_NAME)
        return dir.takeIf { it.isDirectory }
    }

    private fun migrateFromSharedPreferencesIfNeeded() {
        val legacyJson = legacyPrefs.getString(LEGACY_KEY_PROFILES_JSON, "") ?: ""
        if (legacyJson.isBlank()) return

        val profiles = runCatching {
            AppSettingsSnapshotCodec.decodeProfiles(legacyJson)
        }.getOrDefault(emptyList())
        for (profile in profiles) {
            val file = profileFile(profile.id)
            if (!file.exists()) {
                writeProfileFile(profile)
            }
        }
        legacyPrefs.edit().remove(LEGACY_KEY_PROFILES_JSON).apply()
    }

    private fun profileJsonFilesIn(dir: File): List<File> {
        return dir.listFiles { file ->
            file.isFile && file.extension.equals("json", ignoreCase = true)
        }?.toList().orEmpty()
    }

    private fun readProfileFile(file: File): SettingsProfile? {
        if (!file.exists() || !file.isFile) return null
        return runCatching {
            AppSettingsSnapshotCodec.decodeProfile(file.readText())
        }.getOrNull()
    }

    private fun writeProfileFile(profile: SettingsProfile): Boolean {
        return runCatching {
            profileFile(profile.id).writeText(AppSettingsSnapshotCodec.encodeProfile(profile))
            true
        }.getOrDefault(false)
    }

    private fun uniqueImportName(baseName: String): String {
        if (baseName.isBlank()) return "Imported profile"
        if (!hasName(baseName)) return baseName

        var suffix = 2
        while (true) {
            val candidate = "$baseName ($suffix)"
            if (!hasName(candidate)) return candidate
            suffix++
        }
    }

    companion object {
        private const val LEGACY_PREFS_NAME = "notspot_settings_profiles"
        private const val LEGACY_KEY_PROFILES_JSON = "profiles_json"
        private const val PROFILES_FOLDER_NAME = "profiles"
        private const val PROFILE_FILE_EXTENSION = ".json"

        const val PROFILE_MIME_TYPE = "application/json"
        const val MAX_PROFILES = 24
        const val MAX_NAME_LENGTH = 40
    }
}
