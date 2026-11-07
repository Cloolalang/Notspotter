package io.github.cloolalang.notspotdetector.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** A smaller JPEG of a floor plan, small enough to send over Bluetooth. */
object FloorPlanTransfer {
    const val MAX_SIDE_PX = 1280
    const val JPEG_QUALITY = 70
    private const val MAX_BYTES = 700_000

    fun jpeg(source: Bitmap): ByteArray {
        if (source.width <= 0 || source.height <= 0) return ByteArray(0)
        val hardware = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            source.config == Bitmap.Config.HARDWARE
        val software = if (hardware) {
            source.copy(Bitmap.Config.ARGB_8888, false) ?: return ByteArray(0)
        } else {
            source
        }
        return try {
            var side = MAX_SIDE_PX
            var quality = JPEG_QUALITY
            var bytes = encode(software, side, quality)
            while (bytes.size > MAX_BYTES && (side > 480 || quality > 40)) {
                if (quality > 40) quality -= 10 else side = (side * 3) / 4
                bytes = encode(software, side, quality)
            }
            bytes
        } finally {
            if (software !== source) software.recycle()
        }
    }

    fun decode(jpeg: ByteArray): Bitmap? {
        if (jpeg.isEmpty()) return null
        return BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
    }

    private fun encode(source: Bitmap, maxSide: Int, quality: Int): ByteArray {
        val scaled = scale(source, maxSide)
        return try {
            val stream = ByteArrayOutputStream()
            val wrote = scaled.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            if (wrote) stream.toByteArray() else ByteArray(0)
        } finally {
            if (scaled !== source) scaled.recycle()
        }
    }

    private fun scale(source: Bitmap, maxSide: Int): Bitmap {
        val largest = max(source.width, source.height)
        if (largest <= maxSide || largest <= 0) return source
        val factor = maxSide.toFloat() / largest.toFloat()
        val width = (source.width * factor).toInt().coerceAtLeast(1)
        val height = (source.height * factor).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }
}
