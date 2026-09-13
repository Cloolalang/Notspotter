package io.github.cloolalang.notspotdetector.network

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager

/** Reads and, when Android allows, sets the phone’s mobile-data switch. */
object MobileDataControl {

    @SuppressLint("MissingPermission")
    fun isEnabled(context: Context, telephonyManager: TelephonyManager?): Boolean? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && telephonyManager != null) {
            runCatching { telephonyManager.isDataEnabled }.getOrNull()?.let { return it }
        }
        return runCatching {
            Settings.Global.getInt(context.contentResolver, "mobile_data") == 1
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    fun setEnabled(telephonyManager: TelephonyManager?, enabled: Boolean): Boolean {
        val manager = telephonyManager ?: return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                manager.setDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER, enabled)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                manager.setDataEnabled(enabled)
            } else {
                return false
            }
            true
        }.getOrDefault(false)
    }

    fun openSystemPanel(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        } else {
            Intent(Settings.ACTION_WIRELESS_SETTINGS)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
