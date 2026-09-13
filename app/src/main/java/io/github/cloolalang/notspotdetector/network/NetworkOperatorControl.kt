package io.github.cloolalang.notspotdetector.network

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager

/**
 * Changes the phone’s network-operator list (automatic vs a locked PLMN).
 *
 * Selecting a specific operator needs [android.Manifest.permission.MODIFY_PHONE_STATE],
 * which a normal app cannot get. The working control is Android’s Network operators
 * page (search / pick a network).
 */
object NetworkOperatorControl {

    @SuppressLint("MissingPermission")
    fun setAutomatic(telephonyManager: TelephonyManager?): Boolean {
        val manager = telephonyManager ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return runCatching {
            manager.setNetworkSelectionModeAutomatic()
            true
        }.getOrDefault(false)
    }

    fun openSystemSettings(context: Context, subscriptionId: Int?) {
        for (intent in networkOperatorPageIntents(subscriptionId)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(intent) }.isSuccess) {
                return
            }
        }
    }

    /**
     * Deepest Network operators / Choose network screens first. The public
     * [Settings.ACTION_NETWORK_OPERATOR_SETTINGS] action often stops at the parent
     * SIM / mobile-network page.
     */
    private fun networkOperatorPageIntents(subscriptionId: Int?): List<Intent> {
        return listOf(
            component("com.android.phone", "com.android.phone.NetworkSetting"),
            component("com.android.phone", "com.android.phone.settings.NetworkSetting"),
            component(
                "com.samsung.android.app.telephonyui",
                "com.samsung.android.app.telephonyui.netsettings.ui.NetSettingsActivity"
            ),
            component(
                "com.android.settings",
                "com.android.settings.Settings\$NetworkSelectSettingsActivity"
            ),
            Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS),
            Intent(Settings.ACTION_DATA_ROAMING_SETTINGS),
            Intent(Settings.ACTION_WIRELESS_SETTINGS)
        ).map { it.withSubscriptionId(subscriptionId) }
    }

    private fun component(packageName: String, className: String): Intent {
        return Intent().setClassName(packageName, className)
    }

    private fun Intent.withSubscriptionId(subscriptionId: Int?): Intent {
        if (subscriptionId != null && subscriptionId >= 0) {
            putExtra(Settings.EXTRA_SUB_ID, subscriptionId)
            putExtra("subscription", subscriptionId)
            putExtra("sub_id", subscriptionId)
        }
        return this
    }
}
