package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class SettingsProfileNamesTest {

    @Test
    fun timestampName_usesFilesystemSafeDateAndTime() {
        val utc = TimeZone.getTimeZone("UTC")
        val calendar = Calendar.getInstance(utc)
        calendar.set(2026, Calendar.SEPTEMBER, 14, 12, 48, 22)
        calendar.set(Calendar.MILLISECOND, 0)

        assertEquals(
            "2026-09-14 12-48-22",
            SettingsProfileNames.timestampName(nowMs = calendar.timeInMillis, timeZone = utc)
        )
    }
}
