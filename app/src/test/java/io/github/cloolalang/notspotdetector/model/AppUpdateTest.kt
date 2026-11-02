package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.data.GithubAppUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateTest {

    @Test
    fun newerVersion_comparesNumbersNotText() {
        assertTrue(AppVersion.isNewer("2.10.0", "2.9.0"))
        assertTrue(AppVersion.isNewer("v2.151.0", "2.150.0"))
        assertFalse(AppVersion.isNewer("2.150.0", "2.150.0"))
        assertFalse(AppVersion.isNewer("2.149.0", "2.150.0"))
    }

    @Test
    fun parseRelease_readsTagAndApk() {
        val release = GithubAppUpdate.parseRelease(
            """
            {
              "tag_name": "v2.151.0",
              "assets": [
                {"name": "notes.txt", "browser_download_url": "https://github.com/Cloolalang/Notspotter/releases/download/v2.151.0/notes.txt"},
                {"name": "notspotter.apk", "browser_download_url": "https://github.com/Cloolalang/Notspotter/releases/download/v2.151.0/notspotter.apk"}
              ]
            }
            """.trimIndent()
        )
        assertEquals("2.151.0", release?.versionName)
        assertEquals(
            "https://github.com/Cloolalang/Notspotter/releases/download/v2.151.0/notspotter.apk",
            release?.apkUrl
        )
    }

    @Test
    fun parseRelease_withoutAnApk_hasNoDownload() {
        val release = GithubAppUpdate.parseRelease("""{"tag_name": "2.151.0", "assets": []}""")
        assertEquals("2.151.0", release?.versionName)
        assertNull(release?.apkUrl)
    }

    @Test
    fun downloadUrl_staysOnGithub() {
        assertTrue(
            GithubAppUpdate.isTrustedDownloadUrl(
                "https://github.com/Cloolalang/Notspotter/releases/download/v2.151.0/app.apk"
            )
        )
        assertFalse(GithubAppUpdate.isTrustedDownloadUrl("https://example.com/app.apk"))
    }
}
