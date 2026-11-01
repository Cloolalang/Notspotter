package io.github.cloolalang.notspotdetector.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.IOException

/** Opens a floor-plan image the user picked from Downloads. */
object FloorPlanLoader {
    const val MAX_SIDE_PX = 4096

    fun keepPermission(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // The picker grant still covers this visit.
        }
    }

    /**
     * Copies the picked file before decoding. A Downloads content stream often cannot
     * rewind, and [BitmapFactory.decodeStream] then reports a valid JPEG as unreadable.
     */
    fun decode(context: Context, uri: Uri): Bitmap? {
        val temp = try {
            File.createTempFile("floor-plan", ".img", context.cacheDir)
        } catch (_: IOException) {
            return null
        }
        return try {
            if (!copyToFile(context, uri, temp)) return null
            decodeCopiedFile(temp)
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        } finally {
            temp.delete()
        }
    }

    private fun copyToFile(context: Context, uri: Uri, target: File): Boolean {
        val input = context.contentResolver.openInputStream(uri) ?: return false
        input.use { source ->
            target.outputStream().use { dest -> source.copyTo(dest) }
        }
        return target.length() > 0L
    }

    private fun decodeCopiedFile(temp: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(temp.absolutePath, bounds)
        if (bounds.outWidth > 0 && bounds.outHeight > 0) {
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_SIDE_PX)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(temp.absolutePath, options)?.let { return it }
            BitmapFactory.decodeFile(temp.absolutePath)?.let { return it }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return try {
            val source = ImageDecoder.createSource(temp)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val sample = sampleSize(info.size.width, info.size.height, MAX_SIDE_PX)
                if (sample > 1) {
                    decoder.setTargetSize(
                        (info.size.width / sample).coerceAtLeast(1),
                        (info.size.height / sample).coerceAtLeast(1)
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } catch (_: IOException) {
            null
        }
    }

    fun sampleSize(width: Int, height: Int, maxSide: Int): Int {
        if (width <= 0 || height <= 0 || maxSide <= 0) return 1
        var sample = 1
        while (
            sample < 64 &&
            (width / (sample * 2) >= maxSide || height / (sample * 2) >= maxSide)
        ) {
            sample *= 2
        }
        return sample
    }
}
