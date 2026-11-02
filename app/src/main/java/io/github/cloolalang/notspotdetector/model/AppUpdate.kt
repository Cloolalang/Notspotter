package io.github.cloolalang.notspotdetector.model

/** A published GitHub release that may replace this install. */
data class GithubRelease(
    val versionName: String,
    val apkUrl: String?
)

data class AppUpdateControls(
    val status: AppUpdateStatus = AppUpdateStatus.Idle,
    val onCheck: () -> Unit = {},
    val onDownloadAndInstall: () -> Unit = {},
    val onInstall: () -> Unit = {}
)

sealed class AppUpdateStatus {
    data object Idle : AppUpdateStatus()
    data object Checking : AppUpdateStatus()
    data object UpToDate : AppUpdateStatus()
    data class Available(val versionName: String, val apkUrl: String) : AppUpdateStatus()
    data object Downloading : AppUpdateStatus()
    data class Ready(val versionName: String) : AppUpdateStatus()
    data object NeedPermission : AppUpdateStatus()
    data class Failed(val reason: Reason) : AppUpdateStatus()

    enum class Reason {
        Network,
        NoRelease,
        NoApk
    }
}

object AppVersion {
    fun normalize(raw: String): String {
        return raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .substringBefore("+")
    }

    /** True when [candidate] is a newer dotted version than [installed]. */
    fun isNewer(candidate: String, installed: String): Boolean {
        val next = numbers(candidate)
        val current = numbers(installed)
        val length = maxOf(next.size, current.size)
        for (index in 0 until length) {
            val nextPart = next.getOrElse(index) { 0 }
            val currentPart = current.getOrElse(index) { 0 }
            if (nextPart != currentPart) return nextPart > currentPart
        }
        return false
    }

    private fun numbers(raw: String): List<Int> {
        return normalize(raw).split(".").map { part ->
            part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }
    }
}
