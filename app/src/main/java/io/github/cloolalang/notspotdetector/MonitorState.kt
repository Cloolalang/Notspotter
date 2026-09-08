package io.github.cloolalang.notspotdetector

import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.CellIdentitySnapshot
import io.github.cloolalang.notspotdetector.model.CellularRadioMetrics
import io.github.cloolalang.notspotdetector.model.ConnectionQuality
import io.github.cloolalang.notspotdetector.model.ConnectivityStats
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.MonitoringUpdateEvents
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.PingSettings
import io.github.cloolalang.notspotdetector.model.RttSample
import io.github.cloolalang.notspotdetector.model.ThresholdSettings
import io.github.cloolalang.notspotdetector.model.withCellIdentityForDisplay
import io.github.cloolalang.notspotdetector.model.withQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MonitorState {
    private val _stats = MutableStateFlow(ConnectivityStats())
    val stats: StateFlow<ConnectivityStats> = _stats.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _thresholds = MutableStateFlow(ThresholdSettings())
    val thresholds: StateFlow<ThresholdSettings> = _thresholds.asStateFlow()

    private val _pingSettings = MutableStateFlow(PingSettings())
    val pingSettings: StateFlow<PingSettings> = _pingSettings.asStateFlow()

    private val _monitoringSettings = MutableStateFlow(MonitoringSettings())
    val monitoringSettings: StateFlow<MonitoringSettings> = _monitoringSettings.asStateFlow()

    private val _audioVolumes = MutableStateFlow(AudioVolumeSettings())
    val audioVolumes: StateFlow<AudioVolumeSettings> = _audioVolumes.asStateFlow()

    private val _passiveSignalSettings = MutableStateFlow(PassiveSignalSettings())
    val passiveSignalSettings: StateFlow<PassiveSignalSettings> = _passiveSignalSettings.asStateFlow()

    private val _passiveMockSettings = MutableStateFlow(PassiveMockSettings())
    val passiveMockSettings: StateFlow<PassiveMockSettings> = _passiveMockSettings.asStateFlow()

    private val _rttHistory = MutableStateFlow<List<RttSample>>(emptyList())
    val rttHistory: StateFlow<List<RttSample>> = _rttHistory.asStateFlow()

    private var cellIdentityBaselineReady = false
    private var radioTechnologyBaselineReady = false

    fun setPassiveSignalSettings(settings: PassiveSignalSettings) {
        _passiveSignalSettings.value = settings.normalized()
        recomputeStatsQuality()
    }

    fun setPassiveMockSettings(settings: PassiveMockSettings) {
        _passiveMockSettings.value = settings.normalized()
        recomputeStatsQuality()
    }

    fun setPingSettings(settings: PingSettings) {
        _pingSettings.value = settings.normalized()
    }

    fun setMonitoringSettings(settings: MonitoringSettings) {
        _monitoringSettings.value = settings
    }

    fun setAudioVolumes(settings: AudioVolumeSettings) {
        _audioVolumes.value = settings.normalized()
    }

    fun setThresholds(settings: ThresholdSettings) {
        val normalized = settings.normalized()
        _thresholds.value = normalized
        recomputeStatsQuality()
    }

    fun updateStats(stats: ConnectivityStats): MonitoringUpdateEvents {
        val passiveSettings = _passiveSignalSettings.value
        val displayStats = stats.withCellIdentityForDisplay(passiveSettings)
        val cellIdentityChanged = consumeCellIdentityChange(CellIdentitySnapshot.fromStats(displayStats))
        val radioTechnologyChanged = consumeRadioTechnologyChange(displayStats.radioAccessType)
        val enriched = if (displayStats.isPassiveIdleMode) {
            displayStats.copy(quality = ConnectionQuality.PASSIVE_IDLE, severity = 0f)
        } else {
            displayStats.withQuality(_thresholds.value, passiveSettings)
        }
        _stats.value = enriched
        if (!enriched.isPassiveIdleMode) {
            enriched.rttMs?.let { recordRttSample(it, enriched.lastPingTimestampMs) }
        }
        return MonitoringUpdateEvents(
            cellIdentityChanged = cellIdentityChanged,
            radioTechnologyChanged = radioTechnologyChanged
        )
    }

    private fun recordRttSample(rttMs: Long, timestampMs: Long) {
        if (!_isRunning.value || timestampMs <= 0L) return
        val cutoff = timestampMs - RTT_HISTORY_WINDOW_MS
        _rttHistory.value = (_rttHistory.value + RttSample(timestampMs, rttMs))
            .filter { it.timestampMs >= cutoff }
            .sortedBy { it.timestampMs }
    }

    fun pruneRttHistory(nowMs: Long = System.currentTimeMillis()) {
        val cutoff = nowMs - RTT_HISTORY_WINDOW_MS
        _rttHistory.value = _rttHistory.value.filter { it.timestampMs >= cutoff }
    }

    fun recomputeStatsQuality() {
        val current = _stats.value
        if (current.isMonitoring) {
            _stats.value = current.withQuality(_thresholds.value, _passiveSignalSettings.value)
        }
    }

    fun updateSignalMetrics(metrics: CellularRadioMetrics): MonitoringUpdateEvents {
        if (!_isRunning.value) return MonitoringUpdateEvents()

        val monitor2gFallback = _monitoringSettings.value.monitor2gFallback
        val passiveSettings = _passiveSignalSettings.value
        val displayMetrics = metrics.withCellIdentityForDisplay(monitor2gFallback, passiveSettings)
        val cellIdentityChanged = consumeCellIdentityChange(
            CellIdentitySnapshot.fromMetrics(displayMetrics)
        )
        val radioTechnologyChanged = consumeRadioTechnologyChange(metrics.radioAccessType)
        _stats.value = _stats.value.copy(
            rsrpDbm = metrics.rsrpDbm,
            rsrqDb = metrics.rsrqDb,
            radioAccessType = metrics.radioAccessType,
            lteEarfcn = displayMetrics.lteEarfcn,
            ltePci = displayMetrics.ltePci,
            nrEarfcn = displayMetrics.nrEarfcn,
            nrPci = displayMetrics.nrPci,
            gsmEarfcn = displayMetrics.gsmEarfcn,
            gsmBsic = displayMetrics.gsmBsic,
            isOn2g = metrics.isOn2g,
            isLimitedService = metrics.isLimitedService,
            networkServiceMode = metrics.networkServiceMode,
            hasLimitedServiceOnAnySim = metrics.hasLimitedServiceOnAnySim,
            isCompleteNoService = metrics.isCompleteNoService,
            hasHomeGsmSignal = metrics.hasHomeGsmSignal,
            hasLteNrSignal = metrics.hasLteNrSignal,
            monitor2gFallbackEnabled = monitor2gFallback,
            networkOperatorName = metrics.networkOperatorName,
            plmn = metrics.plmn,
            subscriptionId = metrics.subscriptionId,
            simSlotIndex = metrics.simSlotIndex,
            simDisplayName = metrics.simDisplayName,
            signalPermissionGranted = metrics.permissionGranted,
            cellIdentityPermissionGranted = metrics.cellIdentityPermissionGranted
        ).let { current ->
            val withQuality = if (current.isMonitoring && !current.isPassiveIdleMode) {
                current.withQuality(_thresholds.value, passiveSettings)
            } else {
                current
            }
            withQuality.withCellIdentityForDisplay(passiveSettings)
        }
        return MonitoringUpdateEvents(
            cellIdentityChanged = cellIdentityChanged,
            radioTechnologyChanged = radioTechnologyChanged
        )
    }

    private fun consumeRadioTechnologyChange(nextType: String?): Boolean {
        if (!_isRunning.value) return false

        if (!radioTechnologyBaselineReady) {
            if (!nextType.isNullOrBlank()) {
                radioTechnologyBaselineReady = true
            }
            return false
        }

        val previous = _stats.value.radioAccessType
        if (previous == nextType) return false
        if (previous.isNullOrBlank() || nextType.isNullOrBlank()) return false
        return true
    }

    private fun consumeCellIdentityChange(next: CellIdentitySnapshot): Boolean {
        if (!_isRunning.value || !next.hasAnyIdentity()) {
            return false
        }

        if (!cellIdentityBaselineReady) {
            cellIdentityBaselineReady = true
            return false
        }

        val previous = CellIdentitySnapshot.fromStats(_stats.value)
        if (previous == next) {
            return false
        }

        return true
    }

    private fun resetCellIdentityTracking() {
        cellIdentityBaselineReady = false
        radioTechnologyBaselineReady = false
    }

    fun enterPassiveIdleMode() {
        if (!_isRunning.value) return
        _stats.value = _stats.value.copy(
            isPassiveIdleMode = true,
            isMonitoring = true,
            rttMs = null,
            jitterMs = 0,
            packetLossPercent = 0f,
            quality = ConnectionQuality.PASSIVE_IDLE,
            severity = 0f
        )
    }

    fun beginPassiveOnlySession() {
        _stats.value = ConnectivityStats(
            isMonitoring = true,
            isPassiveOnlySession = true
        )
    }

    fun setRunning(running: Boolean) {
        _isRunning.value = running
        if (running) {
            resetCellIdentityTracking()
        } else {
            resetCellIdentityTracking()
            _stats.value = ConnectivityStats(isMonitoring = false)
            _rttHistory.value = emptyList()
        }
    }

    const val RTT_HISTORY_WINDOW_MS = 60_000L
}
