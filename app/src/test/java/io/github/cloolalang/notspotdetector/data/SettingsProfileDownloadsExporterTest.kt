package io.github.cloolalang.notspotdetector.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsProfileDownloadsExporterTest {

    @Test
    fun sanitizeProfileFileName_replacesIllegalCharacters() {
        assertEquals("Field test", SettingsProfileDownloadsExporter.sanitizeProfileFileName("Field test"))
        assertEquals("A_B", SettingsProfileDownloadsExporter.sanitizeProfileFileName("A/B"))
        assertEquals("profile", SettingsProfileDownloadsExporter.sanitizeProfileFileName("   "))
    }
}
