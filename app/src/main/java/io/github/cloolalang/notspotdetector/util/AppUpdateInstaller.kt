package io.github.cloolalang.notspotdetector.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

enum class AppUpdateInstallResult {
    Started,
    NeedPermission,
    Failed
}

object AppUpdateInstaller {
    fun updateFile(cacheDir: File): File {
        return File(File(cacheDir, "updates"), "notspotter-update.apk")
    }

    /**
     * Opens the system installer for [apk]. Android asks before it replaces this install.
     * The new file must be a newer build of the same app, signed with the same key.
     */
    fun install(activity: Activity, apk: File): AppUpdateInstallResult {
        if (!apk.isFile || apk.length() == 0L) return AppUpdateInstallResult.Failed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            val settings = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${activity.packageName}")
            )
            return try {
                activity.startActivity(settings)
                AppUpdateInstallResult.NeedPermission
            } catch (_: ActivityNotFoundException) {
                AppUpdateInstallResult.Failed
            }
        }
        val uri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            apk
        )
        val install = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            activity.startActivity(install)
            AppUpdateInstallResult.Started
        } catch (_: ActivityNotFoundException) {
            AppUpdateInstallResult.Failed
        }
    }

    private const val APK_MIME = "application/vnd.android.package-archive"
}
