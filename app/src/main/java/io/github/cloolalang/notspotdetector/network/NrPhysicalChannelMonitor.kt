package io.github.cloolalang.notspotdetector.network

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.CellInfo
import android.telephony.PhysicalChannelConfig
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import io.github.cloolalang.notspotdetector.model.CarrierBandwidth
import io.github.cloolalang.notspotdetector.model.CarrierRadio
import io.github.cloolalang.notspotdetector.model.ChannelBandwidth
import io.github.cloolalang.notspotdetector.model.EndcCarrier
import io.github.cloolalang.notspotdetector.model.NrCarrierReading
import java.util.concurrent.Executors

/**
 * NR EARFCN and PCI for a channel the modem is using. Current SDKs have no public getter;
 * the value arrives from a hidden [TelephonyManager] method or from
 * [TelephonyCallback.PhysicalChannelConfigListener].
 */
object NrPhysicalChannelMonitor {
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val callbackExecutor by lazy {
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "nr-physical-channel").apply { isDaemon = true }
        }
    }
    private val lock = Any()

    @Volatile
    private var reading: NrCarrierReading? = null

    @Volatile
    private var bandwidthChannels: List<ChannelBandwidth> = emptyList()
    private var desiredManager: TelephonyManager? = null
    private var desiredSubId: Int? = null
    private var registerQueued: Boolean = false
    private var registeredSubId: Int? = null
    private var registeredManager: TelephonyManager? = null
    private var registeredCallback: TelephonyCallback? = null

    fun current(telephonyManager: TelephonyManager, subscriptionId: Int): NrCarrierReading? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        refresh(telephonyManager, subscriptionId)
        return reading
    }

    /** Carriers from the same modem report, including LTE and NR, before they are split for display. */
    fun currentChannels(telephonyManager: TelephonyManager, subscriptionId: Int): List<ChannelBandwidth> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return emptyList()
        refresh(telephonyManager, subscriptionId)
        return bandwidthChannels
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun refresh(telephonyManager: TelephonyManager, subscriptionId: Int) {
        val immediate = hiddenConfigs(telephonyManager)
        if (immediate != null) apply(immediate)
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

    @RequiresApi(Build.VERSION_CODES.S)
    fun readingFrom(configs: List<PhysicalChannelConfig>): NrCarrierReading? {
        val nr = configs.mapNotNull { config ->
            val band = bandOf(config)
            if (EndcCarrier.isNrDownlink(config.networkType, config.downlinkChannelNumber, band)) {
                config to band
            } else {
                null
            }
        }
        if (nr.isEmpty()) return null
        val serving = nr.filter { (config, _) ->
            config.connectionStatus == PhysicalChannelConfig.CONNECTION_PRIMARY_SERVING ||
                config.connectionStatus == PhysicalChannelConfig.CONNECTION_SECONDARY_SERVING
        }
        val chosen = (serving.ifEmpty { nr }).firstOrNull { (config, _) ->
            validNrPci(config.physicalCellId) || validNrArfcn(config.downlinkChannelNumber)
        } ?: return null
        val config = chosen.first
        return NrCarrierReading(
            earfcn = config.downlinkChannelNumber.takeIf { validNrArfcn(it) },
            pci = config.physicalCellId.takeIf { validNrPci(it) },
            band = chosen.second
        )
    }

    private fun drainRegistration() {
        synchronized(lock) {
            registerQueued = false
            val telephonyManager = desiredManager ?: return
            val subscriptionId = desiredSubId ?: return
            if (registeredSubId == subscriptionId && registeredManager != null) return
            unregisterLocked()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && registerCallback(telephonyManager)) {
                registeredManager = telephonyManager
                registeredSubId = subscriptionId
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun hiddenConfigs(telephonyManager: TelephonyManager): List<PhysicalChannelConfig>? {
        val configs = runCatching {
            val method = telephonyManager.javaClass.methods.firstOrNull { candidate ->
                candidate.parameterCount == 0 && candidate.name in HIDDEN_CONFIG_METHODS
            } ?: telephonyManager.javaClass.declaredMethods.firstOrNull { candidate ->
                candidate.parameterCount == 0 && candidate.name in HIDDEN_CONFIG_METHODS
            }?.also { it.isAccessible = true }
            method?.invoke(telephonyManager) as? List<*>
        }.getOrNull() ?: return null
        val typed = configs.filterIsInstance<PhysicalChannelConfig>()
        if (typed.isEmpty()) return null
        return typed
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun apply(configs: List<PhysicalChannelConfig>) {
        val next = readingFrom(configs)
        if (next != null) reading = next
        val channels = configs.map { config ->
            ChannelBandwidth(
                connectionStatus = config.connectionStatus,
                downlinkKhz = reportedDownlinkKhz(config),
                radio = radioOf(config)
            )
        }
        val nextBandwidths = CarrierBandwidth.fromChannels(channels)
        if (nextBandwidths.hasAny()) bandwidthChannels = channels
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun radioOf(config: PhysicalChannelConfig): CarrierRadio {
        val band = bandOf(config)
        if (EndcCarrier.isNrDownlink(config.networkType, config.downlinkChannelNumber, band)) {
            return CarrierRadio.NR
        }
        if (config.networkType == TelephonyManager.NETWORK_TYPE_LTE ||
            config.networkType == LTE_CA_NETWORK_TYPE
        ) {
            return CarrierRadio.LTE
        }
        return CarrierRadio.UNKNOWN
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun reportedDownlinkKhz(config: PhysicalChannelConfig): Int {
        val downlink = config.cellBandwidthDownlinkKhz
        if (CarrierBandwidth.isReportedKhz(downlink)) return downlink
        return config.cellBandwidthUplinkKhz
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerCallback(telephonyManager: TelephonyManager): Boolean {
        val callback = object : TelephonyCallback(), TelephonyCallback.PhysicalChannelConfigListener {
            override fun onPhysicalChannelConfigChanged(configs: List<PhysicalChannelConfig>) {
                apply(configs)
            }
        }
        val registered = runCatching {
            telephonyManager.registerTelephonyCallback(callbackExecutor, callback)
        }.isSuccess
        if (registered) registeredCallback = callback
        return registered
    }

    private fun unregisterLocked() {
        val manager = registeredManager
        val callback = registeredCallback
        if (manager != null && callback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { manager.unregisterTelephonyCallback(callback) }
        }
        registeredCallback = null
        registeredManager = null
        registeredSubId = null
        bandwidthChannels = emptyList()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun bandOf(config: PhysicalChannelConfig): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        return config.band.takeIf { isValidIdentity(it) && it in 1..512 }
    }

    private fun validNrPci(pci: Int): Boolean = isValidIdentity(pci) && pci in 0..1007

    private fun validNrArfcn(arfcn: Int): Boolean = isValidIdentity(arfcn) && arfcn in 0..3_279_165

    private fun isValidIdentity(value: Int): Boolean {
        return value != CellInfo.UNAVAILABLE && value != Int.MAX_VALUE
    }

    private val HIDDEN_CONFIG_METHODS = setOf("getPhysicalChannelConfig", "getPhysicalChannelConfigs")

    /** [android.telephony.TelephonyManager.NETWORK_TYPE_LTE_CA], removed from newer SDKs. */
    private const val LTE_CA_NETWORK_TYPE = 19
}
