package io.github.cloolalang.notspotdetector.network

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.SimSubscriptionOption

object SimSubscriptionHelper {

    @SuppressLint("MissingPermission")
    fun listActiveSubscriptions(context: Context): List<SimSubscriptionOption> {
        if (!CellularSignalReader.hasPhoneStatePermission(context)) {
            return emptyList()
        }

        return try {
            val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
                ?: return emptyList()
            val active = subscriptionManager.activeSubscriptionInfoList ?: return emptyList()

            active
                .sortedBy { it.simSlotIndex }
                .map { info ->
                    val carrier = info.carrierName?.toString()?.takeIf { it.isNotBlank() }
                    val display = info.displayName?.toString()?.takeIf { it.isNotBlank() }
                        ?: carrier
                        ?: "SIM ${info.simSlotIndex + 1}"
                    SimSubscriptionOption(
                        subscriptionId = info.subscriptionId,
                        slotIndex = info.simSlotIndex,
                        displayName = display,
                        carrierName = carrier
                    )
                }
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: RuntimeException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun resolveSubscriptionLabel(
        context: Context,
        subscriptionId: Int
    ): String? {
        if (!CellularSignalReader.hasPhoneStatePermission(context)) {
            return null
        }

        val effectiveId = resolveEffectiveSubscriptionId(context, subscriptionId) ?: return null

        return listActiveSubscriptions(context)
            .firstOrNull { it.subscriptionId == effectiveId }
            ?.let { formatSimLabel(it) }
    }

    fun resolveCarrierName(context: Context, subscriptionId: Int): String? {
        if (!CellularSignalReader.hasPhoneStatePermission(context)) {
            return null
        }

        val effectiveId = resolveEffectiveSubscriptionId(context, subscriptionId) ?: return null
        return listActiveSubscriptions(context)
            .firstOrNull { it.subscriptionId == effectiveId }
            ?.carrierName
    }

    fun resolveDisplayName(context: Context, subscriptionId: Int): String? {
        if (!CellularSignalReader.hasPhoneStatePermission(context)) {
            return null
        }

        val effectiveId = resolveEffectiveSubscriptionId(context, subscriptionId) ?: return null
        return listActiveSubscriptions(context)
            .firstOrNull { it.subscriptionId == effectiveId }
            ?.displayName
    }

    fun resolveSimCarrierId(telephonyManager: TelephonyManager): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return runCatching {
            telephonyManager.simCarrierId.takeIf { it > 0 }
        }.getOrNull()
    }

    fun resolveSimSpecificCarrierId(telephonyManager: TelephonyManager): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            telephonyManager.simSpecificCarrierId.takeIf { it > 0 }
        }.getOrNull()
    }

    fun resolveSimCarrierIdName(telephonyManager: TelephonyManager): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return runCatching {
            telephonyManager.simCarrierIdName?.toString()?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun resolveSimSpecificCarrierIdName(telephonyManager: TelephonyManager): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            telephonyManager.simSpecificCarrierIdName?.toString()?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    fun resolveSlotIndex(
        context: Context,
        subscriptionId: Int
    ): Int? {
        if (!CellularSignalReader.hasPhoneStatePermission(context)) {
            return null
        }

        val effectiveId = resolveEffectiveSubscriptionId(context, subscriptionId) ?: return null

        return listActiveSubscriptions(context)
            .firstOrNull { it.subscriptionId == effectiveId }
            ?.slotIndex
    }

    @SuppressLint("MissingPermission")
    fun resolveEffectiveSubscriptionId(context: Context, subscriptionId: Int): Int? {
        if (subscriptionId != MonitoringSettings.DEFAULT_SUBSCRIPTION_ID) {
            return subscriptionId
        }
        return defaultDataSubscriptionId(context)
    }

    fun telephonyManagerFor(context: Context, subscriptionId: Int): TelephonyManager? {
        val base = context.getSystemService(TelephonyManager::class.java) ?: return null
        val effectiveSubId = resolveEffectiveSubscriptionId(context, subscriptionId)
        return if (effectiveSubId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            base.createForSubscriptionId(effectiveSubId)
        } else {
            base
        }
    }

    fun formatSimLabel(option: SimSubscriptionOption): String {
        return "SIM ${option.slotIndex + 1} · ${option.displayName}"
    }

    @SuppressLint("MissingPermission")
    private fun defaultDataSubscriptionId(context: Context): Int? {
        if (!CellularSignalReader.hasPhoneStatePermission(context)) {
            return null
        }
        val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
            ?: return null
        val defaultId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            defaultDataSubscriptionIdApi24(subscriptionManager)
        } else {
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        }
        return defaultId.takeIf { it != SubscriptionManager.INVALID_SUBSCRIPTION_ID }
    }

    private fun defaultDataSubscriptionIdApi24(subscriptionManager: SubscriptionManager): Int {
        return runCatching {
            SubscriptionManager::class.java.getMethod("getDefaultDataSubscriptionId")
                .invoke(subscriptionManager) as Int
        }.getOrDefault(SubscriptionManager.INVALID_SUBSCRIPTION_ID)
    }
}
