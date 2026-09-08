package io.github.cloolalang.notspotdetector.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TelephonyNetworkSpecifier
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.cloolalang.notspotdetector.model.ConnectivityStats
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.PingSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import kotlin.math.abs

class CellularPingMonitor(
    private val context: Context,
    private val settingsProvider: () -> PingSettings,
    private val monitoringSettingsProvider: () -> MonitoringSettings,
    private val sampleWindowSize: Int = 20,
    private val connectTimeoutMs: Int = 5_000
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var pingJob: Job? = null
    private var activeNetwork: Network? = null

    private val rttSamples = ArrayDeque<Long>()
    private val pingCycleStats = ArrayDeque<PingCycleStats>()
    private var pingsSent = 0
    private var pingsFailed = 0

    fun start(scope: CoroutineScope, onStatsUpdated: (ConnectivityStats) -> Unit) {
        stop()

        val subscriptionId = monitoringSettingsProvider().subscriptionId
        val request = buildNetworkRequest(subscriptionId)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                activeNetwork = network
                startPingLoop(scope, network, onStatsUpdated)
            }

            override fun onLost(network: Network) {
                if (activeNetwork == network) {
                    activeNetwork = null
                    pingJob?.cancel()
                    pingJob = null
                    emitStats(onStatsUpdated, cellularAvailable = false)
                }
            }

            override fun onUnavailable() {
                activeNetwork = null
                emitStats(onStatsUpdated, cellularAvailable = false)
            }
        }

        networkCallback = callback
        connectivityManager.requestNetwork(request, callback)
        emitStats(onStatsUpdated, cellularAvailable = activeNetwork != null)
    }

    fun stop() {
        pingJob?.cancel()
        pingJob = null
        networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
        networkCallback = null
        activeNetwork = null
        rttSamples.clear()
        pingCycleStats.clear()
        pingsSent = 0
        pingsFailed = 0
    }

    private fun startPingLoop(
        scope: CoroutineScope,
        network: Network,
        onStatsUpdated: (ConnectivityStats) -> Unit
    ) {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive) {
                val settings = settingsProvider()
                val monitoring = monitoringSettingsProvider()
                val radio = CellularSignalReader.read(
                    context,
                    monitoring.monitor2gFallback,
                    monitoring.subscriptionId
                )

                if (radio.isOn2g || radio.isLimitedService) {
                    emitStats(
                        onStatsUpdated = onStatsUpdated,
                        cellularAvailable = activeNetwork != null,
                        latestRtt = null,
                        radio = radio,
                        monitor2gFallback = monitoring.monitor2gFallback
                    )
                    delay(settings.testIntervalMs)
                    continue
                }

                val testResult = runPingTest(network, settings)
                emitStats(
                    onStatsUpdated = onStatsUpdated,
                    cellularAvailable = true,
                    latestRtt = testResult.averageRttMs,
                    radio = radio,
                    monitor2gFallback = monitoring.monitor2gFallback
                )
                delay(settings.testIntervalMs)
            }
        }
    }

    private suspend fun runPingTest(network: Network, settings: PingSettings): PingTestResult {
        val rttResults = mutableListOf<Long>()
        var failures = 0

        repeat(settings.totalPingsPerTest) { index ->
            val rtt = measureRtt(network, settings.host, settings.port)
            if (index < PingSettings.RRC_WARMUP_PINGS) {
                // First probe warms up RRC / bearer setup and is excluded from metrics.
                return@repeat
            }
            if (rtt != null) {
                rttResults.add(rtt)
            } else {
                failures++
            }
        }

        pingCycleStats.addLast(
            PingCycleStats(
                measuredSent = settings.pingsPerTest,
                measuredFailed = failures
            )
        )
        while (pingCycleStats.size > sampleWindowSize) {
            pingCycleStats.removeFirst()
        }
        pruneStaleLossCycles()
        updateRollingPingTotals()

        val averageRtt = rttResults.takeIf { it.isNotEmpty() }?.average()?.toLong()
        if (averageRtt != null) {
            addRttSample(averageRtt)
        }

        return PingTestResult(
            averageRttMs = averageRtt,
            individualRtts = rttResults
        )
    }

    private suspend fun measureRtt(network: Network, host: String, port: Int): Long? =
        withContext(Dispatchers.IO) {
            try {
                val start = System.nanoTime()
                network.socketFactory.createSocket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), connectTimeoutMs)
                }
                (System.nanoTime() - start) / 1_000_000
            } catch (_: Exception) {
                null
            }
        }

    private fun addRttSample(averageRtt: Long) {
        if (rttSamples.isNotEmpty()) {
            val windowMax = rttSamples.max()
            if (averageRtt * 2 < windowMax) {
                val staleThreshold = maxOf(averageRtt * 3, STALE_RTT_FLOOR_MS)
                while (rttSamples.isNotEmpty() && rttSamples.first() > staleThreshold) {
                    rttSamples.removeFirst()
                }
            }
        }
        rttSamples.addLast(averageRtt)
        while (rttSamples.size > sampleWindowSize) {
            rttSamples.removeFirst()
        }
    }

    private fun computeJitter(): Long {
        val recent = rttSamples.takeLast(JITTER_WINDOW_SIZE)
        if (recent.size < 2) return 0
        return recent.zipWithNext { a, b -> abs(b - a) }.average().toLong()
    }

    private fun activeLossWindow(): List<PingCycleStats> {
        return pingCycleStats.takeLast(PACKET_LOSS_WINDOW_SIZE)
    }

    private fun pruneStaleLossCycles() {
        if (pingCycleStats.size < RECOVERY_CYCLES) return
        val recent = pingCycleStats.takeLast(RECOVERY_CYCLES)
        if (recent.all { it.measuredFailed == 0 }) {
            pingCycleStats.clear()
            pingCycleStats.addAll(recent)
        }
    }

    private fun updateRollingPingTotals() {
        val window = activeLossWindow()
        pingsSent = window.sumOf { it.measuredSent }
        pingsFailed = window.sumOf { it.measuredFailed }
    }

    private fun computePacketLoss(): Float {
        val window = activeLossWindow()
        val sent = window.sumOf { it.measuredSent }
        if (sent == 0) return 0f
        return (window.sumOf { it.measuredFailed }.toFloat() / sent) * 100f
    }

    private fun emitStats(
        onStatsUpdated: (ConnectivityStats) -> Unit,
        cellularAvailable: Boolean,
        latestRtt: Long? = rttSamples.lastOrNull(),
        radio: io.github.cloolalang.notspotdetector.model.CellularRadioMetrics? = null,
        monitor2gFallback: Boolean = monitoringSettingsProvider().monitor2gFallback,
        subscriptionId: Int = monitoringSettingsProvider().subscriptionId
    ) {
        val metrics = radio ?: CellularSignalReader.read(context, monitor2gFallback, subscriptionId)
        val skipPingMetrics = metrics.isOn2g || metrics.isLimitedService
        val base = ConnectivityStats(
            rttMs = if (skipPingMetrics) null else latestRtt,
            jitterMs = if (skipPingMetrics) 0 else computeJitter(),
            packetLossPercent = if (skipPingMetrics) 0f else computePacketLoss(),
            pingsSent = if (skipPingMetrics) 0 else pingsSent,
            pingsFailed = if (skipPingMetrics) 0 else pingsFailed,
            cellularAvailable = cellularAvailable,
            isMonitoring = true,
            lastPingTimestampMs = if (skipPingMetrics) 0L else System.currentTimeMillis(),
            rsrpDbm = metrics.rsrpDbm,
            rsrqDb = metrics.rsrqDb,
            radioAccessType = metrics.radioAccessType,
            lteEarfcn = metrics.lteEarfcn,
            ltePci = metrics.ltePci,
            nrEarfcn = metrics.nrEarfcn,
            nrPci = metrics.nrPci,
            gsmEarfcn = metrics.gsmEarfcn,
            gsmBsic = metrics.gsmBsic,
            isOn2g = metrics.isOn2g,
            restrictedTo2gNetwork = metrics.restrictedTo2gNetwork,
            isLimitedService = metrics.isLimitedService,
            networkServiceMode = metrics.networkServiceMode,
            hasLimitedServiceOnAnySim = metrics.hasLimitedServiceOnAnySim,
            isCompleteNoService = metrics.isCompleteNoService,
            hasHomeGsmSignal = metrics.hasHomeGsmSignal,
            hasLteNrSignal = metrics.hasLteNrSignal,
            monitor2gFallbackEnabled = monitor2gFallback,
            networkOperatorName = metrics.networkOperatorName,
            homeNetworkOperatorName = metrics.homeNetworkOperatorName,
            servingNetworkOperatorName = metrics.servingNetworkOperatorName,
            plmn = metrics.plmn,
            homePlmn = metrics.homePlmn,
            subscriptionId = metrics.subscriptionId,
            simSlotIndex = metrics.simSlotIndex,
            simDisplayName = metrics.simDisplayName,
            signalPermissionGranted = metrics.permissionGranted,
            cellIdentityPermissionGranted = metrics.cellIdentityPermissionGranted
        )
        onStatsUpdated(base)
    }

    private fun buildNetworkRequest(subscriptionId: Int): NetworkRequest {
        val builder = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            subscriptionId != MonitoringSettings.DEFAULT_SUBSCRIPTION_ID
        ) {
            return buildSubscriptionNetworkRequest(subscriptionId)
        }

        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun buildSubscriptionNetworkRequest(subscriptionId: Int): NetworkRequest {
        return NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(
                TelephonyNetworkSpecifier.Builder()
                    .setSubscriptionId(subscriptionId)
                    .build()
            )
            .build()
    }

    private data class PingTestResult(
        val averageRttMs: Long?,
        val individualRtts: List<Long>
    )

    private data class PingCycleStats(
        val measuredSent: Int,
        val measuredFailed: Int
    )

    companion object {
        private const val JITTER_WINDOW_SIZE = 5
        private const val PACKET_LOSS_WINDOW_SIZE = 5
        private const val RECOVERY_CYCLES = 3
        private const val STALE_RTT_FLOOR_MS = 300L
    }
}
