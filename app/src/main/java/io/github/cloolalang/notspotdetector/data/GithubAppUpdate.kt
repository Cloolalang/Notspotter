package io.github.cloolalang.notspotdetector.data

import io.github.cloolalang.notspotdetector.model.AppUpdateStatus
import io.github.cloolalang.notspotdetector.model.AppVersion
import io.github.cloolalang.notspotdetector.model.GithubRelease
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * Reads the latest GitHub release for this app and downloads its APK.
 * The phone's installer then replaces the installed build.
 */
object GithubAppUpdate {
    const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/Cloolalang/Notspotter/releases/latest"

    fun check(installedVersion: String): AppUpdateStatus {
        val body = httpGet(LATEST_RELEASE_URL) ?: return AppUpdateStatus.Failed(AppUpdateStatus.Reason.Network)
        if (body == NO_RELEASE) return AppUpdateStatus.Failed(AppUpdateStatus.Reason.NoRelease)
        val release = parseRelease(body) ?: return AppUpdateStatus.Failed(AppUpdateStatus.Reason.Network)
        if (!AppVersion.isNewer(release.versionName, installedVersion)) {
            return AppUpdateStatus.UpToDate
        }
        val apkUrl = release.apkUrl
        if (apkUrl.isNullOrBlank() || !isTrustedDownloadUrl(apkUrl)) {
            return AppUpdateStatus.Failed(AppUpdateStatus.Reason.NoApk)
        }
        return AppUpdateStatus.Available(release.versionName, apkUrl)
    }

    fun download(apkUrl: String, destination: File): Boolean {
        if (!isTrustedDownloadUrl(apkUrl)) return false
        val connection = open(
            apkUrl,
            readTimeoutMs = DOWNLOAD_TIMEOUT_MS,
            accept = "*/*"
        ) ?: return false
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return false
            destination.parentFile?.mkdirs()
            connection.inputStream.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            destination.length() > 0L
        } catch (_: Exception) {
            false
        } finally {
            connection.disconnect()
        }
    }

    fun parseRelease(body: String): GithubRelease? {
        return try {
            val json = JSONObject(body)
            val tag = json.optString("tag_name")
            if (tag.isBlank()) return null
            val assets = json.optJSONArray("assets")
            var apkUrl: String? = null
            if (assets != null) {
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    val name = asset.optString("name")
                    if (!name.endsWith(".apk", ignoreCase = true)) continue
                    apkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                    break
                }
            }
            GithubRelease(versionName = AppVersion.normalize(tag), apkUrl = apkUrl)
        } catch (_: Exception) {
            null
        }
    }

    fun isTrustedDownloadUrl(url: String): Boolean {
        val host = runCatching { URI(url).host?.lowercase() }.getOrNull() ?: return false
        return host == "github.com" ||
            host.endsWith(".github.com") ||
            host == "githubusercontent.com" ||
            host.endsWith(".githubusercontent.com")
    }

    private fun httpGet(url: String): String? {
        val connection = open(url, readTimeoutMs = API_TIMEOUT_MS, accept = GITHUB_ACCEPT) ?: return null
        return try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK ->
                    connection.inputStream.bufferedReader().use { it.readText() }
                HttpURLConnection.HTTP_NOT_FOUND -> NO_RELEASE
                else -> null
            }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String, readTimeoutMs: Int, accept: String): HttpURLConnection? {
        return try {
            (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = API_TIMEOUT_MS
                readTimeout = readTimeoutMs
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", accept)
            }
        } catch (_: Exception) {
            null
        }
    }

    private const val USER_AGENT = "Notspotter"
    private const val GITHUB_ACCEPT = "application/vnd.github+json"
    private const val API_TIMEOUT_MS = 15_000
    private const val DOWNLOAD_TIMEOUT_MS = 120_000
    private const val NO_RELEASE = ""
}
