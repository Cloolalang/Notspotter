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

    @Volatile
    private var nrNsaIcon: Boolean = false

    /** Last raw display info, for the radio debug log. -1 until the first callback. */
    @Volatile
    var displayNetworkType: Int = -1
        private set

    @Volatile
    var displayOverrideType: Int = -1
        private set
    private var desiredSubId: Int? = null
    private var desiredManager: TelephonyManager? = null
    private var registerQueued: Boolean = false
    private var registeredSubId: Int? = null
    private var registeredManager: TelephonyManager? = null
    private var registeredCallback: TelephonyCallback? = null
    private var registeredListener: PhoneStateListener? = null

    fun current(telephonyManager: TelephonyManager, subscriptionId: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        ensureRegistered(telephonyManager, subscriptionId)
        return ltePlus
    }

    /** True while the status bar shows the 5G icon over an LTE anchor (EN-DC). */
    fun currentNrNsaIcon(telephonyManager: TelephonyManager, subscriptionId: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        ensureRegistered(telephonyManager, subscriptionId)
        return nrNsaIcon
    }

    private fun ensureRegistered(telephonyManager: TelephonyManager, subscriptionId: Int) {
        synchronized(lock) {
            desiredManager = telephonyManager
            desiredSubId = subscriptionId
            if (registeredSubId == subscriptionId && registeredManager != null) return
            if (!registerQueued) {
                registerQueued = true
                mainHandler.post { drainRegistration() }
            }
        }
    }

    /**
     * The 5G icon on an EN-DC anchor. Most phones send LTE with an NR override; some Samsung
     * builds send the network type as NR instead, so the override counts on any network type.
     */
    fun isNrNsaIcon(networkType: Int, overrideNetworkType: Int): Boolean {
        return overrideNetworkType in NR_OVERRIDE_TYPES
    }

    private fun onDisplayInfo(telephonyDisplayInfo: TelephonyDisplayInfo) {
        displayNetworkType = telephonyDisplayInfo.networkType
        displayOverrideType = telephonyDisplayInfo.overrideNetworkType
        ltePlus = isLtePlusIcon(
            telephonyDisplayInfo.networkType,
            telephonyDisplayInfo.overrideNetworkType
        )
        nrNsaIcon = isNrNsaIcon(
            telephonyDisplayInfo.networkType,
            telephonyDisplayInfo.overrideNetworkType
        )
    }

    /** NR_NSA (3), NR_NSA_MMWAVE (4, deprecated), NR_ADVANCED (5). */
    private val NR_OVERRIDE_TYPES = setOf(3, 4, 5)

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
                onDisplayInfo(telephonyDisplayInfo)
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
                onDisplayInfo(telephonyDisplayInfo)
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
        nrNsaIcon = false
        displayNetworkType = -1
        displayOverrideType = -1
    }
}
