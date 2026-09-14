package io.github.cloolalang.notspotdetector.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object SettingsProfileNames {
    private const val TIMESTAMP_PATTERN = "yyyy-MM-dd HH-mm-ss"

    fun timestampName(
        nowMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val format = SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US)
        format.timeZone = timeZone
        return format.format(Date(nowMs))
    }
}
