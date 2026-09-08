package io.github.cloolalang.notspotdetector.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import io.github.cloolalang.notspotdetector.model.ProfileExportOutcome
import io.github.cloolalang.notspotdetector.model.ProfileExportResult
import io.github.cloolalang.notspotdetector.model.SettingsProfile
import java.io.File
import java.io.IOException

object SettingsProfileDownloadsExporter {

    private const val DOWNLOADS_SUBFOLDER = "NotSpotter"
    private const val FILE_PREFIX = "NotSpotter - "

    fun exportProfile(context: Context, profile: SettingsProfile): ProfileExportOutcome {
        val json = AppSettingsSnapshotCodec.encodeProfile(profile)
        val fileName = buildFileName(profile.name)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportWithMediaStore(context, fileName, json)
        } else {
            exportLegacy(fileName, json)
        }
    }

    fun sanitizeProfileFileName(profileName: String): String {
        return profileName
            .trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .take(MAX_PROFILE_NAME_LENGTH)
            .ifBlank { "profile" }
    }

    private fun buildFileName(profileName: String): String {
        return "$FILE_PREFIX${sanitizeProfileFileName(profileName)}.json"
    }

    private fun exportWithMediaStore(
        context: Context,
        fileName: String,
        json: String
    ): ProfileExportOutcome {
        val resolver = context.contentResolver
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOADS_SUBFOLDER"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, SettingsProfilesRepository.PROFILE_MIME_TYPE)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: return ProfileExportOutcome(ProfileExportResult.Failed)

        return try {
            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
            } ?: throw IOException("Could not open Downloads output stream")

            val published = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(uri, published, null, null)
            ProfileExportOutcome(
                result = ProfileExportResult.Exported,
                relativePath = "$relativePath/$fileName"
            )
        } catch (_: IOException) {
            resolver.delete(uri, null, null)
            ProfileExportOutcome(ProfileExportResult.Failed)
        }
    }

    @Suppress("DEPRECATION")
    private fun exportLegacy(fileName: String, json: String): ProfileExportOutcome {
        val downloadsRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetDir = File(downloadsRoot, DOWNLOADS_SUBFOLDER)
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            return ProfileExportOutcome(ProfileExportResult.Failed)
        }

        val targetFile = File(targetDir, fileName)
        return try {
            targetFile.writeText(json, Charsets.UTF_8)
            ProfileExportOutcome(
                result = ProfileExportResult.Exported,
                relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOADS_SUBFOLDER/$fileName"
            )
        } catch (_: IOException) {
            ProfileExportOutcome(ProfileExportResult.Failed)
        }
    }

    private const val MAX_PROFILE_NAME_LENGTH = 80
}
