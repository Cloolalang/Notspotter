package io.github.cloolalang.notspotdetector.multiue

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class MultiUeRadioInfo(
    val available: Boolean,
    val enabled: Boolean,
    val localName: String
)

object MultiUePermissions {
    fun granted(context: Context): Boolean = missingBluetooth(context).isEmpty()

    fun missing(context: Context): List<String> {
        return buildList {
            addAll(missingBluetooth(context))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun missingBluetooth(context: Context): List<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return emptyList()
        return listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        ).filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
}

object MultiUeRadios {
    fun adapter(context: Context): BluetoothAdapter? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter
    }

    @SuppressLint("MissingPermission")
    fun read(context: Context): MultiUeRadioInfo {
        val radio = adapter(context) ?: return MultiUeRadioInfo(
            available = false,
            enabled = false,
            localName = ""
        )
        if (!MultiUePermissions.granted(context)) {
            return MultiUeRadioInfo(available = true, enabled = false, localName = "")
        }
        return try {
            MultiUeRadioInfo(
                available = true,
                enabled = radio.isEnabled,
                localName = radio.name?.takeIf { it.isNotBlank() }.orEmpty()
            )
        } catch (_: SecurityException) {
            MultiUeRadioInfo(available = true, enabled = false, localName = "")
        }
    }
}
