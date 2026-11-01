package io.github.cloolalang.notspotdetector.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import io.github.cloolalang.notspotdetector.model.ZoneTest
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ZoneTestImageSaver {

    private const val DOWNLOADS_SUBFOLDER = "NotSpotter"

    fun save(
        context: Context,
        bitmap: Bitmap,
        name: String,
        operator: String?
    ): String? {
        return saveJpeg(context, bitmap, ZoneTest.jpegFileName(name, operator))
    }

    fun saveJpeg(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(context, bitmap, fileName)
        } else {
            saveLegacy(bitmap, fileName)
        }
    }

    private fun saveWithMediaStore(
        context: Context,
        bitmap: Bitmap,
        fileName: String
    ): String? {
        val resolver = context.contentResolver
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOADS_SUBFOLDER"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)) {
                    throw IOException("JPEG compress failed")
                }
            } ?: throw IOException("Could not open Downloads output stream")
            val published = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(uri, published, null, null)
            "$relativePath/$fileName"
        } catch (_: IOException) {
            resolver.delete(uri, null, null)
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(bitmap: Bitmap, fileName: String): String? {
        val downloadsRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetDir = File(downloadsRoot, DOWNLOADS_SUBFOLDER)
        if (!targetDir.exists() && !targetDir.mkdirs()) return null
        val target = File(targetDir, fileName)
        return try {
            FileOutputStream(target).use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)) {
                    throw IOException("JPEG compress failed")
                }
            }
            "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOADS_SUBFOLDER/$fileName"
        } catch (_: IOException) {
            null
        }
    }
}
