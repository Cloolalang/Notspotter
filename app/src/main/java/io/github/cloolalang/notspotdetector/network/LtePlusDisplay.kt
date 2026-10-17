package io.github.cloolalang.notspotdetector.network

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import java.util.concurrent.Executors

/**
 * Whether the status-bar data icon is the plus form of LTE (4G+ / LTE+).
 * [TelephonyManager.getDataNetworkType] only says LTE. The icon comes from
 * [TelephonyDisplayInfo], which is what System UI uses to choose 4G versus 4G+.
 */
object LtePlusDisplay {
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val callbackExecutor by lazy {
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "lte-plus-display").apply { isDaemon = true }
        }
    }
    private val lock = Any()
    /** [TelephonyManager.NETWORK_TYPE_LTE_CA], removed from newer SDKs. */
    private const val LTE_CA_NETWORK_TYPE = 19

    @Volatile
    private var ltePlus: Boolean = false
    private var desiredSubId: Int? = null
    private var desiredManager: TelephonyManager? = null
    private var registerQueued: Boolean = false
    private var registeredSubId: Int? = null
    private var registeredManager: TelephonyManager? = null
    private var registeredCallback: TelephonyCallback? = null
    private var registeredListener: PhoneStateListener? = null

    fun current(telephonyManager: TelephonyManager, subscriptionId: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        synchronized(lock) {
            desiredManager = telephonyManager
            desiredSubId = subscriptionId
            if (registeredSubId == subscriptionId && registeredManager != null) return ltePlus
            if (!registerQueued) {
                registerQueued = true
                mainHandler.post { drainRegistration() }
            }
        }
        return ltePlus
    }

    /**
     * True when the display info is LTE with the carrier-aggregation or LTE Advanced Pro
     * override, which is the plus icon. Other radios stay false.
     */
    fun isLtePlusIcon(networkType: Int, overrideNetworkType: Int): Boolean {
        val carrierAggregated = networkType == LTE_CA_NETWORK_TYPE
        val lte = networkType == TelephonyManager.NETWORK_TYPE_LTE || carrierAggregated
        if (!lte) return false
        if (carrierAggregated) return true
        return overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA ||
            overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO
    }

    private fun drainRegistration() {
        synchronized(lock) {
            registerQueued = false
            val telephonyManager = desiredManager ?: return
            val subscriptionId = desiredSubId ?: return
            if (registeredSubId == subscriptionId && registeredManager != null) return
            unregisterLocked()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                registerCallback(telephonyManager)
            } else {
                registerListener(telephonyManager)
            }
            registeredManager = telephonyManager
            registeredSubId = subscriptionId
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerCallback(telephonyManager: TelephonyManager) {
        val callback = object : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
            override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                ltePlus = isLtePlusIcon(
                    telephonyDisplayInfo.networkType,
                    telephonyDisplayInfo.overrideNetworkType
                )
            }
        }
        telephonyManager.registerTelephonyCallback(callbackExecutor, callback)
        registeredCallback = callback
    }

    @Suppress("DEPRECATION")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun registerListener(telephonyManager: TelephonyManager) {
        val listener = object : PhoneStateListener() {
            override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                ltePlus = isLtePlusIcon(
                    telephonyDisplayInfo.networkType,
                    telephonyDisplayInfo.overrideNetworkType
                )
            }
        }
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED)
        registeredListener = listener
    }

    @Suppress("DEPRECATION")
    private fun unregisterLocked() {
        val manager = registeredManager
        if (manager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                registeredCallback?.let { manager.unregisterTelephonyCallback(it) }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                registeredListener?.let { manager.listen(it, PhoneStateListener.LISTEN_NONE) }
            }
        }
        registeredCallback = null
        registeredListener = null
        registeredManager = null
        registeredSubId = null
        ltePlus = false
    }
}
