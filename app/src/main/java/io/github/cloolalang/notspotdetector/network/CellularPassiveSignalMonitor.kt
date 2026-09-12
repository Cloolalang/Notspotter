package io.github.cloolalang.notspotdetector.network

import android.content.Context
import io.github.cloolalang.notspotdetector.model.ConnectivityStats
import io.github.cloolalang.notspotdetector.model.hasUsableSignalForMonitoring
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.toConnectivityStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CellularPassiveSignalMonitor(
    private val context: Context,
    private val intervalProvider: () -> Long,
    private val monitoringSettingsProvider: () -> MonitoringSettings,
    private val passiveSignalSettingsProvider: () -> PassiveSignalSettings,
    private val passiveMockSettingsProvider: () -> PassiveMockSettings,
    private val passiveIdleModeProvider: () -> Boolean,
    private val passiveOnlySessionProvider: () -> Boolean
) {
    private var monitorJob: Job? = null

    fun start(scope: CoroutineScope, onStatsUpdated: (ConnectivityStats) -> Unit) {
        stop()
        monitorJob = scope.launch {
            while (isActive) {
                emitStats(onStatsUpdated)
                delay(
                    intervalProvider().coerceIn(
                        MonitoringSettings.MIN_PASSIVE_MEASUREMENT_INTERVAL_MS,
                        MonitoringSettings.MAX_PASSIVE_MEASUREMENT_INTERVAL_MS
                    )
                )
            }
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private fun emitStats(onStatsUpdated: (ConnectivityStats) -> Unit) {
        val monitoring = monitoringSettingsProvider()
        val passiveSettings = passiveSignalSettingsProvider()
        val mockSettings = passiveMockSettingsProvider()
        val monitor2gFallback = monitoring.monitor2gFallback
        val passiveIdleMode = passiveIdleModeProvider()
        val passiveOnlySession = passiveOnlySessionProvider()

        if (mockSettings.enabled && passiveOnlySession) {
            onStatsUpdated(
                mockSettings.toConnectivityStats(
                    monitor2gFallback = monitor2gFallback,
                    passiveSettings = passiveSettings,
                    passiveIdleMode = passiveIdleMode,
                    passiveOnlySession = passiveOnlySession
                )
            )
            return
        }

        val radio = CellularSignalReader.read(
            context,
            monitor2gFallback,
            monitoring.subscriptionId
        )
        val hasSignal = radio.hasUsableSignalForMonitoring(monitor2gFallback, passiveSettings)

        onStatsUpdated(
            ConnectivityStats(
                isMonitoring = true,
                isPassiveIdleMode = passiveIdleMode,
                isPassiveOnlySession = passiveOnlySession,
                cellularAvailable = hasSignal,
                rsrpDbm = radio.rsrpDbm,
                rsrqDb = radio.rsrqDb,
                radioAccessType = radio.radioAccessType,
                lteEarfcn = radio.lteEarfcn,
                ltePci = radio.ltePci,
                nrEarfcn = radio.nrEarfcn,
                nrPci = radio.nrPci,
                nrBand = radio.nrBand,
                gsmEarfcn = radio.gsmEarfcn,
                gsmBsic = radio.gsmBsic,
                isOn2g = radio.isOn2g,
                networkModePreference = radio.networkModePreference,
                restrictedTo2gNetwork = radio.restrictedTo2gNetwork,
                isLimitedService = radio.isLimitedService,
                networkServiceMode = radio.networkServiceMode,
                isVoiceOnlyNoData = radio.isVoiceOnlyNoData,
                isWifiCallingActive = radio.isWifiCallingActive,
                hasLimitedServiceOnAnySim = radio.hasLimitedServiceOnAnySim,
                isCompleteNoService = radio.isCompleteNoService,
                hasHomeGsmSignal = radio.hasHomeGsmSignal,
                hasLteNrSignal = radio.hasLteNrSignal,
                monitor2gFallbackEnabled = monitor2gFallback,
                networkOperatorName = radio.networkOperatorName,
                homeNetworkOperatorName = radio.homeNetworkOperatorName,
                servingNetworkOperatorName = radio.servingNetworkOperatorName,
                plmn = radio.plmn,
                homePlmn = radio.homePlmn,
                subscriptionId = radio.subscriptionId,
                simSlotIndex = radio.simSlotIndex,
                simDisplayName = radio.simDisplayName,
                signalPermissionGranted = radio.permissionGranted,
                cellIdentityPermissionGranted = radio.cellIdentityPermissionGranted,
                lteLayerResilience = radio.lteLayerResilience
            )
        )
    }
}
