package io.github.cloolalang.notspotdetector

import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.CellIdentityAnnouncement
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
import io.github.cloolalang.notspotdetector.model.RsrpSample
import io.github.cloolalang.notspotdetector.model.ThresholdSettings
import io.github.cloolalang.notspotdetector.model.SignalStateAnnouncement
import io.github.cloolalang.notspotdetector.model.hasUsableSignalForMonitoring
import io.github.cloolalang.notspotdetector.model.NoSignalDebouncer
import io.github.cloolalang.notspotdetector.model.evaluateFlatlineCondition
import io.github.cloolalang.notspotdetector.model.limitedServiceAlternativeOperatorChanged
import io.github.cloolalang.notspotdetector.model.resolveLimitedServiceAlternativeOperatorName
import io.github.cloolalang.notspotdetector.model.resolveNoSignalAnnouncementRadioAccessType
import io.github.cloolalang.notspotdetector.network.CellularSignalReader
import io.github.cloolalang.notspotdetector.model.withStabilizedCellIdentity
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

    private val _rsrpHistory = MutableStateFlow<List<RsrpSample>>(emptyList())
    val rsrpHistory: StateFlow<List<RsrpSample>> = _rsrpHistory.asStateFlow()

    private var cellIdentityBaselineReady = false
    private var radioTechnologyBaselineReady = false
    private var noSignalBaselineReady = false
    private var limitedServiceBaselineReady = false
    private var stableCellIdentity = CellIdentitySnapshot()
    private val noSignalDebouncer = NoSignalDebouncer()
    private var lastKnownRadioAccessType: String? = null
    private var lteRatBeforeNoSignalEpisode: String? = null
    private var searching2gAnnounced = false
    private var deadzoneAnnouncedThisEpisode = false
    private var g2FallbackBaselineReady = false

    fun formatNoSignalAnnouncement(stats: ConnectivityStats = _stats.value): String {
        return SignalStateAnnouncement.formatNoSignalAnnouncement(
            stats,
            lastKnownRadioAccessType
        )
    }

    fun formatDeadzoneAnnouncement(stats: ConnectivityStats = _stats.value): String {
        return SignalStateAnnouncement.formatDeadzoneAnnouncement(stats.networkOperatorName)
    }

    fun formatLimitedServiceAnnouncement(stats: ConnectivityStats = _stats.value): String {
        return SignalStateAnnouncement.formatLimitedServiceAnnouncement(
            stats,
            lastKnownRadioAccessType
        )
    }

    fun formatSearching2gAnnouncement(): String {
        return SignalStateAnnouncement.formatSearching2gAnnouncement(
            _stats.value.networkOperatorName,
            lteRatBeforeNoSignalEpisode
        )
    }

    fun shouldScheduleSearching2gAnnouncement(stats: ConnectivityStats = _stats.value): Boolean {
        return stats.noSignalActive &&
            stats.monitor2gFallbackEnabled &&
            !stats.isOn2g &&
            !stats.isCompleteNoService &&
            SignalStateAnnouncement.isLteNrRadioAccessType(lteRatBeforeNoSignalEpisode) &&
            !searching2gAnnounced
    }

    fun markSearching2gAnnounced() {
        searching2gAnnounced = true
    }

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
        rememberRadioAccessType(stats)
        val passiveSettings = _passiveSignalSettings.value
        val previous = _stats.value
        val (displayStats, updatedIdentity) = stats.withStabilizedCellIdentity(stableCellIdentity)
        stableCellIdentity = updatedIdentity
        val enriched = if (displayStats.isPassiveIdleMode) {
            displayStats.copy(quality = ConnectionQuality.PASSIVE_IDLE, severity = 0f)
        } else {
            displayStats.withQuality(_thresholds.value, passiveSettings)
        }
        val (finalStats, events) = buildMonitoringEvents(previous, enriched, passiveSettings)
        _stats.value = finalStats
        recordRsrpSample(displayStats.rsrpDbm, stats.isMonitoring)
        if (!enriched.isPassiveIdleMode) {
            enriched.rttMs?.let { recordRttSample(it, enriched.lastPingTimestampMs) }
        }
        return events
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

    private fun recordRsrpSample(rsrpDbm: Int?, isMonitoring: Boolean) {
        if (!_isRunning.value || !isMonitoring || rsrpDbm == null) return
        val timestampMs = System.currentTimeMillis()
        val cutoff = timestampMs - RSRP_HISTORY_RETENTION_MS
        _rsrpHistory.value = (_rsrpHistory.value + RsrpSample(timestampMs, rsrpDbm))
            .filter { it.timestampMs >= cutoff }
    }

    fun pruneRsrpHistory(nowMs: Long = System.currentTimeMillis()) {
        val cutoff = nowMs - RSRP_HISTORY_RETENTION_MS
        _rsrpHistory.value = _rsrpHistory.value.filter { it.timestampMs >= cutoff }
    }

    fun recomputeStatsQuality() {
        val current = _stats.value
        if (current.isMonitoring) {
            val passiveSettings = _passiveSignalSettings.value
            val withQuality = current.withQuality(_thresholds.value, passiveSettings)
            _stats.value = applyNoSignalDebounce(withQuality, passiveSettings)
        }
    }

    fun refreshSignalMetrics(metrics: CellularRadioMetrics): MonitoringUpdateEvents {
        if (!_isRunning.value) {
            applyIdleSignalMetrics(metrics)
            return MonitoringUpdateEvents()
        }
        return updateMonitoringSignalMetrics(metrics)
    }

    private fun applyIdleSignalMetrics(metrics: CellularRadioMetrics) {
        val monitor2gFallback = _monitoringSettings.value.monitor2gFallback
        val passiveSettings = _passiveSignalSettings.value
        val hasSignal = metrics.hasUsableSignalForMonitoring(monitor2gFallback, passiveSettings)
        val merged = _stats.value.copy(
            isMonitoring = false,
            cellularAvailable = hasSignal,
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
        val (display, updatedIdentity) = merged.withStabilizedCellIdentity(stableCellIdentity)
        stableCellIdentity = updatedIdentity
        _stats.value = display.withQuality(_thresholds.value, passiveSettings)
    }

    fun updateSignalMetrics(metrics: CellularRadioMetrics): MonitoringUpdateEvents {
        return refreshSignalMetrics(metrics)
    }

    private fun updateMonitoringSignalMetrics(metrics: CellularRadioMetrics): MonitoringUpdateEvents {
        if (!_isRunning.value) return MonitoringUpdateEvents()

        rememberRadioAccessType(metrics)

        val monitor2gFallback = _monitoringSettings.value.monitor2gFallback
        val passiveSettings = _passiveSignalSettings.value
        val merged = _stats.value.copy(
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
        val (stabilized, updatedIdentity) = merged.withStabilizedCellIdentity(stableCellIdentity)
        stableCellIdentity = updatedIdentity
        val previous = _stats.value
        val next = if (stabilized.isMonitoring && !stabilized.isPassiveIdleMode) {
            stabilized.withQuality(_thresholds.value, passiveSettings)
        } else {
            stabilized
        }
        val (finalStats, events) = buildMonitoringEvents(previous, next, passiveSettings)
        _stats.value = finalStats
        return events
    }

    private fun applyNoSignalDebounce(
        stats: ConnectivityStats,
        passiveSettings: PassiveSignalSettings
    ): ConnectivityStats {
        if (!stats.isMonitoring) return stats.copy(noSignalActive = false)

        val debounceResult = noSignalDebouncer.update(stats.evaluateFlatlineCondition(passiveSettings))
        return stats.copy(noSignalActive = debounceResult.confirmedActive)
    }

    private fun buildMonitoringEvents(
        previous: ConnectivityStats,
        next: ConnectivityStats,
        passiveSettings: PassiveSignalSettings
    ): Pair<ConnectivityStats, MonitoringUpdateEvents> {
        val nextDebounced = applyNoSignalDebounce(next, passiveSettings)
        val networkOperatorName = nextDebounced.networkOperatorName
        val noSignalAnnouncement = consumeNoSignalStateChange(
            previousActive = previous.noSignalActive,
            nextActive = nextDebounced.noSignalActive,
            next = nextDebounced,
            networkOperatorName = networkOperatorName
        )
        val g2FallbackAnnouncement = consumeG2FallbackAnnouncement(
            previous = previous,
            next = nextDebounced,
            networkOperatorName = networkOperatorName
        )
        val events = MonitoringUpdateEvents(
            cellChangeAnnouncement = consumeCellIdentityChange(
                next = CellIdentitySnapshot.fromStats(nextDebounced),
                radioAccessType = nextDebounced.radioAccessType,
                networkOperatorName = networkOperatorName
            ),
            technologyChangeAnnouncement = consumeTechnologyChangeAnnouncement(
                nextType = nextDebounced.radioAccessType,
                networkOperatorName = networkOperatorName
            ),
            noSignalStateAnnouncement = noSignalAnnouncement,
            limitedServiceStateAnnouncement = consumeLimitedServiceStateChange(
                previous = previous,
                next = nextDebounced
            ),
            limitedServiceOperatorChangeAnnouncement = consumeLimitedServiceOperatorChange(
                previous = previous,
                next = nextDebounced
            ),
            g2FallbackAnnouncement = g2FallbackAnnouncement,
            deadzoneAnnouncement = consumeDeadzoneChange(
                previous = previous,
                next = nextDebounced,
                networkOperatorName = networkOperatorName
            ),
            searching2gStateEntered = shouldScheduleSearching2gAnnouncement(nextDebounced) &&
                !previous.noSignalActive &&
                nextDebounced.noSignalActive
        )
        return nextDebounced to events
    }

    private fun consumeTechnologyChangeAnnouncement(
        nextType: String?,
        networkOperatorName: String?
    ): String? {
        if (!_isRunning.value) return null

        if (!radioTechnologyBaselineReady) {
            if (!nextType.isNullOrBlank()) {
                radioTechnologyBaselineReady = true
            }
            return null
        }

        val previousType = _stats.value.radioAccessType?.takeIf { it.isNotBlank() }
            ?: if (_stats.value.noSignalActive) lastKnownRadioAccessType else null
        if (previousType == nextType) return null
        if (previousType.isNullOrBlank() || nextType.isNullOrBlank()) return null

        if (nextType == CellularSignalReader.RADIO_2G &&
            SignalStateAnnouncement.isLteNrRadioAccessType(previousType) &&
            _monitoringSettings.value.monitor2gFallback
        ) {
            return null
        }

        return SignalStateAnnouncement.formatTechnologyChange(nextType, networkOperatorName)
    }

    private fun consumeNoSignalStateChange(
        previousActive: Boolean,
        nextActive: Boolean,
        next: ConnectivityStats,
        networkOperatorName: String?
    ): String? {
        if (!_isRunning.value || !next.isMonitoring) return null

        if (!noSignalBaselineReady) {
            noSignalBaselineReady = true
            return null
        }

        if (previousActive == nextActive) return null

        if (nextActive) {
            beginNoSignalEpisode()
            return SignalStateAnnouncement.formatNoSignalChange(
                active = true,
                networkOperatorName = networkOperatorName,
                radioAccessType = next.resolveNoSignalAnnouncementRadioAccessType(lastKnownRadioAccessType)
            )
        }

        if (next.isOn2g && next.monitor2gFallbackEnabled && lteRatBeforeNoSignalEpisode != null) {
            endNoSignalEpisode()
            return null
        }

        endNoSignalEpisode()
        return SignalStateAnnouncement.formatNoSignalChange(
            active = false,
            networkOperatorName = networkOperatorName,
            radioAccessType = next.resolveNoSignalAnnouncementRadioAccessType(lastKnownRadioAccessType)
        )
    }

    private fun consumeG2FallbackAnnouncement(
        previous: ConnectivityStats,
        next: ConnectivityStats,
        networkOperatorName: String?
    ): String? {
        if (!_isRunning.value || !next.isMonitoring) return null
        if (!next.monitor2gFallbackEnabled) return null
        if (previous.isOn2g || !next.isOn2g) return null

        if (!g2FallbackBaselineReady) {
            g2FallbackBaselineReady = true
            return null
        }

        val fromLteNr = SignalStateAnnouncement.isLteNrRadioAccessType(lteRatBeforeNoSignalEpisode) ||
            SignalStateAnnouncement.isLteNrRadioAccessType(lastKnownRadioAccessType) ||
            SignalStateAnnouncement.isLteNrRadioAccessType(previous.radioAccessType)
        if (!fromLteNr) return null

        endNoSignalEpisode()
        return SignalStateAnnouncement.formatG2CampedAnnouncement(networkOperatorName)
    }

    private fun consumeDeadzoneChange(
        previous: ConnectivityStats,
        next: ConnectivityStats,
        networkOperatorName: String?
    ): String? {
        if (!_isRunning.value || !next.isMonitoring) return null
        if (previous.isCompleteNoService == next.isCompleteNoService) return null
        if (!next.isCompleteNoService) return null
        if (deadzoneAnnouncedThisEpisode) return null

        deadzoneAnnouncedThisEpisode = true
        searching2gAnnounced = true
        return SignalStateAnnouncement.formatDeadzoneAnnouncement(networkOperatorName)
    }

    private fun consumeLimitedServiceStateChange(
        previous: ConnectivityStats,
        next: ConnectivityStats
    ): String? {
        if (!_isRunning.value || !next.isMonitoring) return null

        val previousActive = previous.isLimitedService
        val nextActive = next.isLimitedService

        if (!limitedServiceBaselineReady) {
            limitedServiceBaselineReady = true
            return null
        }

        if (previousActive == nextActive) return null

        return SignalStateAnnouncement.formatLimitedServiceChange(
            active = nextActive,
            stats = next,
            lastKnownRadioAccessType = lastKnownRadioAccessType
        )
    }

    private fun consumeLimitedServiceOperatorChange(
        previous: ConnectivityStats,
        next: ConnectivityStats
    ): String? {
        if (!_isRunning.value || !next.isMonitoring) return null
        if (!limitedServiceBaselineReady) return null
        if (!next.limitedServiceAlternativeOperatorChanged(previous)) return null

        return SignalStateAnnouncement.formatLimitedServiceAnnouncement(
            stats = next,
            lastKnownRadioAccessType = lastKnownRadioAccessType
        )
    }

    private fun consumeCellIdentityChange(
        next: CellIdentitySnapshot,
        radioAccessType: String?,
        networkOperatorName: String?
    ): String? {
        if (!_isRunning.value || !next.hasAnyIdentity()) {
            return null
        }

        if (!cellIdentityBaselineReady) {
            cellIdentityBaselineReady = true
            return null
        }

        val previous = CellIdentitySnapshot.fromStats(_stats.value)
        if (previous == next) {
            return null
        }

        return CellIdentityAnnouncement.format(previous, next, radioAccessType, networkOperatorName)
    }

    private fun beginNoSignalEpisode() {
        if (SignalStateAnnouncement.isLteNrRadioAccessType(lastKnownRadioAccessType)) {
            lteRatBeforeNoSignalEpisode = lastKnownRadioAccessType
        }
        searching2gAnnounced = false
        deadzoneAnnouncedThisEpisode = false
    }

    private fun endNoSignalEpisode() {
        lteRatBeforeNoSignalEpisode = null
        searching2gAnnounced = false
        deadzoneAnnouncedThisEpisode = false
    }

    private fun resetCellIdentityTracking() {
        cellIdentityBaselineReady = false
        radioTechnologyBaselineReady = false
        noSignalBaselineReady = false
        limitedServiceBaselineReady = false
        g2FallbackBaselineReady = false
        stableCellIdentity = CellIdentitySnapshot()
        noSignalDebouncer.reset()
        lastKnownRadioAccessType = null
        lteRatBeforeNoSignalEpisode = null
        searching2gAnnounced = false
        deadzoneAnnouncedThisEpisode = false
    }

    private fun rememberRadioAccessType(stats: ConnectivityStats) {
        if (!stats.radioAccessType.isNullOrBlank()) {
            lastKnownRadioAccessType = stats.radioAccessType
            return
        }
        if (stats.isOn2g || stats.restrictedTo2gNetwork) {
            lastKnownRadioAccessType = CellularSignalReader.RADIO_2G
        }
    }

    private fun rememberRadioAccessType(metrics: CellularRadioMetrics) {
        rememberRadioAccessType(
            ConnectivityStats(
                radioAccessType = metrics.radioAccessType,
                isOn2g = metrics.isOn2g,
                restrictedTo2gNetwork = metrics.restrictedTo2gNetwork
            )
        )
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
            _rsrpHistory.value = emptyList()
        }
    }

    const val RTT_HISTORY_WINDOW_MS = 60_000L
    const val RSRP_HISTORY_RETENTION_MS = 300_000L
}
