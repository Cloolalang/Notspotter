package io.github.cloolalang.notspotdetector

import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.CellIdentityAnnouncement
import io.github.cloolalang.notspotdetector.model.CellIdentitySnapshot
import io.github.cloolalang.notspotdetector.model.CellularRadioMetrics
import io.github.cloolalang.notspotdetector.model.ConnectionQuality
import io.github.cloolalang.notspotdetector.model.ConnectivityStats
import io.github.cloolalang.notspotdetector.model.hidingFiveGIfDisabled
import io.github.cloolalang.notspotdetector.model.TechnologyChangeTarget
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.MonitoringUpdateEvents
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.toConnectivityStats
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.PingSettings
import io.github.cloolalang.notspotdetector.model.RttSample
import io.github.cloolalang.notspotdetector.model.RsrpSample
import io.github.cloolalang.notspotdetector.model.ThresholdSettings
import io.github.cloolalang.notspotdetector.model.SignalStateAnnouncement
import io.github.cloolalang.notspotdetector.model.hasUsableSignalForMonitoring
import io.github.cloolalang.notspotdetector.model.LteLayerResilienceDebouncer
import io.github.cloolalang.notspotdetector.model.NoSignalDebouncer
import io.github.cloolalang.notspotdetector.model.MockNetworkScenario
import io.github.cloolalang.notspotdetector.model.computeSearching2gFallbackActive
import io.github.cloolalang.notspotdetector.model.evaluateFlatlineCondition
import io.github.cloolalang.notspotdetector.model.isG2FlatlineActive
import io.github.cloolalang.notspotdetector.model.isG2WeakSignal
import io.github.cloolalang.notspotdetector.model.isLimitedServiceAlt2g
import io.github.cloolalang.notspotdetector.model.isLimitedServiceNoSignalCamp
import io.github.cloolalang.notspotdetector.model.isSignalLowVoiceCamp
import io.github.cloolalang.notspotdetector.model.isTier5PoorSignal
import io.github.cloolalang.notspotdetector.model.isTier6CriticalSignal
import io.github.cloolalang.notspotdetector.model.shouldAllowCellReselectVoice
import io.github.cloolalang.notspotdetector.model.shouldPlayG2NoSignalVoiceAnnouncements
import io.github.cloolalang.notspotdetector.model.shouldSuppressSignalRestoredForWeakSignalRecovery
import io.github.cloolalang.notspotdetector.model.usesG2SignalTiers
import io.github.cloolalang.notspotdetector.model.limitedServiceVisitedOperatorChanged
import io.github.cloolalang.notspotdetector.model.resolveCampedVisitedOperatorName
import io.github.cloolalang.notspotdetector.model.resolveLimitedServiceVisitedOperatorName
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
    private val lteLayerResilienceDebouncer = LteLayerResilienceDebouncer()
    private var lastKnownRadioAccessType: String? = null
    /** Camped RAT the current histogram belongs to; a change wipes the sample window. */
    private var histogramRadioAccessType: String? = null
    private var lteRatBeforeNoSignalEpisode: String? = null
    private var searching2gAnnounced = false
    private var deadzoneAnnouncedThisEpisode = false
    /** Suppresses a duplicate “Signal restored” when [noSignalActive] clears after dead-zone exit. */
    private var deadzoneRecoveryExitHandled = false
    private var g2FallbackBaselineReady = false
    private var tier5BaselineReady = false
    private var monitoringEventsListener: ((MonitoringUpdateEvents) -> Unit)? = null
    private val monitorLock = Any()

    fun setMonitoringEventsListener(listener: ((MonitoringUpdateEvents) -> Unit)?) {
        synchronized(monitorLock) {
            monitoringEventsListener = listener
        }
    }

    private fun dispatchMonitoringEvents(events: MonitoringUpdateEvents) {
        val listener = synchronized(monitorLock) { monitoringEventsListener }
        listener?.invoke(events)
    }

    fun formatNoSignalAnnouncement(stats: ConnectivityStats = _stats.value): String {
        val volumes = _audioVolumes.value
        return SignalStateAnnouncement.formatNoSignalAnnouncement(
            stats,
            lastKnownRadioAccessType,
            phrases = volumes.noSignalPhrases
        )
    }

    fun formatSignalRestoredAnnouncement(stats: ConnectivityStats = _stats.value): String {
        val volumes = _audioVolumes.value
        return SignalStateAnnouncement.formatSignalRestoredAnnouncement(
            stats,
            lastKnownRadioAccessType,
            phrases = volumes.noSignalPhrases
        )
    }

    fun formatDeadzoneAnnouncement(stats: ConnectivityStats = _stats.value): String {
        return SignalStateAnnouncement.formatDeadzoneAnnouncement(
            stats.networkOperatorName,
            _audioVolumes.value.noSignalPhrases.speakOperatorName
        )
    }

    fun formatTier5Announcement(stats: ConnectivityStats = _stats.value): String {
        val volumes = _audioVolumes.value
        return SignalStateAnnouncement.formatTier5SignalLowAnnouncement(
            stats,
            lastKnownRadioAccessType,
            phrases = volumes.signalLowPhrases
        )
    }

    fun formatLimitedServiceAnnouncement(stats: ConnectivityStats = _stats.value): String {
        val volumes = _audioVolumes.value
        return SignalStateAnnouncement.formatLimitedServiceAnnouncement(
            stats,
            lastKnownRadioAccessType,
            phrases = volumes.limitedServicePhrases
        )
    }

    fun formatSearching2gAnnouncement(): String {
        val volumes = _audioVolumes.value
        val stats = _stats.value
        return SignalStateAnnouncement.formatSearching2gAnnouncement(
            stats.networkOperatorName,
            lteRatBeforeNoSignalEpisode,
            phrases = volumes.noSignalPhrases,
            lteEarfcn = stats.lteEarfcn,
            nrBand = stats.nrBand
        )
    }

    fun shouldScheduleSearching2gAnnouncement(stats: ConnectivityStats = _stats.value): Boolean {
        return stats.searching2gFallbackActive && !searching2gAnnounced
    }

    fun markSearching2gAnnounced() {
        synchronized(monitorLock) {
            searching2gAnnounced = true
        }
    }

    fun setPassiveSignalSettings(settings: PassiveSignalSettings) {
        _passiveSignalSettings.value = settings.normalized()
        recomputeStatsQuality()
    }

    fun setPassiveMockSettings(settings: PassiveMockSettings) {
        _passiveMockSettings.value = settings.normalized()
        recomputeStatsQuality()
        pushMockStatsIfActive()
    }

    /** Applies mock RSRP/RSRQ immediately during passive-only mock monitoring. */
    fun pushMockStatsIfActive(): Boolean {
        val mock = _passiveMockSettings.value
        val currentStats = _stats.value
        if (!mock.enabled || !_isRunning.value || !currentStats.isPassiveOnlySession) {
            return false
        }
        val mockStats = mock.toConnectivityStats(
            monitor2gFallback = _monitoringSettings.value.monitor2gFallback,
            passiveSettings = _passiveSignalSettings.value,
            passiveIdleMode = currentStats.isPassiveIdleMode,
            passiveOnlySession = true
        ).hidingFiveGIfDisabled(_monitoringSettings.value.fiveGFeaturesEnabled)
        // Two polls so no-signal debounce confirms immediately after scenario changes.
        updateStats(mockStats)
        updateStats(mockStats)
        return true
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

    fun updateStats(rawStats: ConnectivityStats): MonitoringUpdateEvents {
        val events = synchronized(monitorLock) {
            val remapped = rawStats.hidingFiveGIfDisabled(_monitoringSettings.value.fiveGFeaturesEnabled)
            val previousKnownRat = lastKnownRadioAccessType
            rememberRadioAccessType(remapped)
            val passiveSettings = _passiveSignalSettings.value
            val previous = _stats.value
            // CellularPingMonitor/CellularPassiveSignalMonitor build ConnectivityStats directly
            // from a fresh CellularRadioMetrics reading each poll — debounce the raw reading
            // here (the single funnel point for the actual running-monitor path) rather than in
            // those readers, so a single flickering neighbour reading doesn't make the displayed
            // "4G layers detected" values jump around.
            val stats = remapped.copy(
                lteLayerResilience = lteLayerResilienceDebouncer.update(
                    remapped.lteLayerResilience
                ).confirmedReading
            )
            val (displayStats, updatedIdentity) = stats.withStabilizedCellIdentity(stableCellIdentity)
            stableCellIdentity = updatedIdentity
            val enriched = if (displayStats.isPassiveIdleMode) {
                displayStats.copy(quality = ConnectionQuality.PASSIVE_IDLE, severity = 0f)
            } else {
                displayStats.withQuality(_thresholds.value, passiveSettings)
            }
            val (finalStats, events) = buildMonitoringEvents(
                previous,
                enriched,
                passiveSettings,
                previousKnownRat
            )
            _stats.value = finalStats
            clearRsrpHistogramIfTechnologyChanged(finalStats.radioAccessType)
            if (!enriched.isPassiveIdleMode) {
                enriched.rttMs?.let { recordRttSample(it, enriched.lastPingTimestampMs) }
            }
            events
        }
        dispatchMonitoringEvents(events)
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

    /**
     * Records one occupancy sample of the latest RSRP. Called on a 1 s tick so a longer sample
     * window actually accumulates more counts instead of only storing one point per radio poll.
     */
    fun tickRsrpHistogramSample() {
        synchronized(monitorLock) {
            val stats = _stats.value
            recordRsrpSample(stats.rsrpDbm, stats.isMonitoring, stats.radioAccessType)
        }
    }

    private fun recordRsrpSample(
        rsrpDbm: Int?,
        isMonitoring: Boolean,
        radioAccessType: String?
    ) {
        // rsrpDbm may be null (e.g. a no-signal state) — still recorded so the histogram's
        // "no signal" bin reflects how often that happened within the window, rather than
        // silently dropping those polls.
        if (!_isRunning.value || !isMonitoring) return
        clearRsrpHistogramIfTechnologyChanged(radioAccessType)
        val timestampMs = System.currentTimeMillis()
        val retainMs = rsrpHistoryRetentionMs()
        val cutoff = timestampMs - retainMs
        _rsrpHistory.value = (_rsrpHistory.value + RsrpSample(timestampMs, rsrpDbm))
            .filter { it.timestampMs >= cutoff }
    }

    /**
     * Drops the rolling histogram when the camped RAT changes so 2G / 4G / 5G / EN-DC
     * measurements never share one window. A blank or missing type (no-signal) does not
     * count as a change — those N/A samples stay with the last camped technology.
     */
    private fun clearRsrpHistogramIfTechnologyChanged(radioAccessType: String?) {
        val currentTech = radioAccessType?.takeIf { it.isNotBlank() } ?: return
        val previousTech = histogramRadioAccessType
        if (previousTech != null && previousTech != currentTech) {
            _rsrpHistory.value = emptyList()
        }
        histogramRadioAccessType = currentTech
    }

    /** Empties the live histogram so sampling restarts from the next tick. */
    fun clearRsrpHistogram() {
        synchronized(monitorLock) {
            _rsrpHistory.value = emptyList()
        }
    }

    fun pruneRsrpHistory(nowMs: Long = System.currentTimeMillis()) {
        val cutoff = nowMs - rsrpHistoryRetentionMs()
        _rsrpHistory.value = _rsrpHistory.value.filter { it.timestampMs >= cutoff }
    }

    fun recomputeStatsQuality() {
        synchronized(monitorLock) {
            val current = _stats.value
            if (current.isMonitoring) {
                val passiveSettings = _passiveSignalSettings.value
                val withQuality = current.withQuality(_thresholds.value, passiveSettings)
                _stats.value = applyNoSignalDebounce(withQuality, passiveSettings)
            }
        }
    }

    fun refreshSignalMetrics(metrics: CellularRadioMetrics): MonitoringUpdateEvents {
        val events = synchronized(monitorLock) {
            if (!_isRunning.value) {
                applyIdleSignalMetrics(metrics)
                MonitoringUpdateEvents()
            } else {
                updateMonitoringSignalMetrics(metrics)
            }
        }
        dispatchMonitoringEvents(events)
        return events
    }

    private fun applyIdleSignalMetrics(metrics: CellularRadioMetrics) {
        val monitor2gFallback = _monitoringSettings.value.monitor2gFallback
        val passiveSettings = _passiveSignalSettings.value
        val hasSignal = metrics.hasUsableSignalForMonitoring(monitor2gFallback, passiveSettings)
        val lteLayerResilience = lteLayerResilienceDebouncer.update(
            metrics.lteLayerResilience
        ).confirmedReading
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
            nrBand = metrics.nrBand,
            gsmEarfcn = metrics.gsmEarfcn,
            gsmBsic = metrics.gsmBsic,
            isOn2g = metrics.isOn2g,
            networkModePreference = metrics.networkModePreference,
            restrictedTo2gNetwork = metrics.restrictedTo2gNetwork,
            isLimitedService = metrics.isLimitedService,
            networkServiceMode = metrics.networkServiceMode,
            isWifiCallingActive = metrics.isWifiCallingActive,
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
            cellIdentityPermissionGranted = metrics.cellIdentityPermissionGranted,
            lteLayerResilience = lteLayerResilience
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

        val previousKnownRat = lastKnownRadioAccessType
        rememberRadioAccessType(metrics)

        val monitor2gFallback = _monitoringSettings.value.monitor2gFallback
        val passiveSettings = _passiveSignalSettings.value
        val lteLayerResilience = lteLayerResilienceDebouncer.update(
            metrics.lteLayerResilience
        ).confirmedReading
        val merged = _stats.value.copy(
            rsrpDbm = metrics.rsrpDbm,
            rsrqDb = metrics.rsrqDb,
            radioAccessType = metrics.radioAccessType,
            lteEarfcn = metrics.lteEarfcn,
            ltePci = metrics.ltePci,
            nrEarfcn = metrics.nrEarfcn,
            nrPci = metrics.nrPci,
            nrBand = metrics.nrBand,
            gsmEarfcn = metrics.gsmEarfcn,
            gsmBsic = metrics.gsmBsic,
            isOn2g = metrics.isOn2g,
            networkModePreference = metrics.networkModePreference,
            restrictedTo2gNetwork = metrics.restrictedTo2gNetwork,
            isLimitedService = metrics.isLimitedService,
            networkServiceMode = metrics.networkServiceMode,
            isWifiCallingActive = metrics.isWifiCallingActive,
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
            cellIdentityPermissionGranted = metrics.cellIdentityPermissionGranted,
            lteLayerResilience = lteLayerResilience
        )
        val (stabilized, updatedIdentity) = merged.withStabilizedCellIdentity(stableCellIdentity)
        stableCellIdentity = updatedIdentity
        val previous = _stats.value
        val next = if (stabilized.isMonitoring && !stabilized.isPassiveIdleMode) {
            stabilized.withQuality(_thresholds.value, passiveSettings)
        } else {
            stabilized
        }
        val (finalStats, events) = buildMonitoringEvents(
            previous,
            next,
            passiveSettings,
            previousKnownRat
        )
        _stats.value = finalStats
        return events
    }

    private fun applyNoSignalDebounce(
        stats: ConnectivityStats,
        passiveSettings: PassiveSignalSettings
    ): ConnectivityStats {
        if (!stats.isMonitoring) return stats.copy(noSignalActive = false)
        if (stats.isLimitedService) {
            noSignalDebouncer.reset()
            return stats.copy(noSignalActive = false)
        }

        val debounceResult = noSignalDebouncer.update(stats.evaluateFlatlineCondition(passiveSettings))
        return stats.copy(noSignalActive = debounceResult.confirmedActive)
    }

    private fun buildMonitoringEvents(
        previous: ConnectivityStats,
        next: ConnectivityStats,
        passiveSettings: PassiveSignalSettings,
        previousKnownRat: String?
    ): Pair<ConnectivityStats, MonitoringUpdateEvents> {
        val nextDebounced = applyNoSignalDebounce(next, passiveSettings).let { debounced ->
            debounced.copy(
                searching2gFallbackActive = resolveSearching2gFallbackActive(debounced)
            )
        }
        val networkOperatorName = nextDebounced.networkOperatorName
        val suppressSignalRestored = shouldSuppressSignalRestoredForWeakSignalRecovery(
            previous = previous,
            next = nextDebounced,
            previousNoSignalActive = previous.noSignalActive,
            nextNoSignalActive = nextDebounced.noSignalActive,
            settings = passiveSettings
        )
        val noSignalAnnouncement = consumeNoSignalStateChange(
            previousActive = previous.noSignalActive,
            nextActive = nextDebounced.noSignalActive,
            next = nextDebounced,
            networkOperatorName = networkOperatorName,
            suppressSignalRestored = suppressSignalRestored
        ) ?: consumeLimitedVisitedNoSignalChange(
            previous = previous,
            next = nextDebounced,
            passiveSettings = passiveSettings,
            suppressSignalRestored = suppressSignalRestored
        ) ?: consumeCompleteNoServiceExitAnnouncement(
            previous = previous,
            next = nextDebounced,
            suppressSignalRestored = suppressSignalRestored,
            networkOperatorName = networkOperatorName
        )
        val g2FallbackAnnouncement = consumeG2FallbackAnnouncement(
            previous = previous,
            next = nextDebounced,
            networkOperatorName = networkOperatorName
        )
        val g2PeriodicReset = previous.shouldPlayG2NoSignalVoiceAnnouncements(passiveSettings) !=
            nextDebounced.shouldPlayG2NoSignalVoiceAnnouncements(passiveSettings) ||
            previous.isG2FlatlineActive(passiveSettings) !=
            nextDebounced.isG2FlatlineActive(passiveSettings) ||
            previous.isG2WeakSignal(passiveSettings) !=
            nextDebounced.isG2WeakSignal(passiveSettings) ||
            (previous.isLimitedServiceAlt2g() &&
                previous.isSignalLowVoiceCamp(passiveSettings) !=
                nextDebounced.isSignalLowVoiceCamp(passiveSettings))
        val tier5Announcement = consumeTier5StateChange(
            previous = previous,
            next = nextDebounced,
            passiveSettings = passiveSettings
        )
        val tier5Immediate = tier5Announcement != null && (
            suppressSignalRestored ||
                previous.isLimitedServiceNoSignalCamp(passiveSettings)
            )
        val technologyChangeAnnouncement = consumeTechnologyChangeAnnouncement(
            previous = previous,
            next = nextDebounced,
            previousKnownRat = previousKnownRat,
            networkOperatorName = networkOperatorName
        )
        val events = MonitoringUpdateEvents(
            cellChangeAnnouncement = consumeCellIdentityChange(
                next = CellIdentitySnapshot.fromStats(nextDebounced),
                stats = nextDebounced,
                passiveSettings = passiveSettings,
                radioAccessType = nextDebounced.radioAccessType,
                networkOperatorName = networkOperatorName
            ),
            technologyChangeAnnouncement = technologyChangeAnnouncement,
            technologyChangeTargetRadioAccessType = nextDebounced.radioAccessType?.takeIf {
                technologyChangeAnnouncement != null
            },
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
            tier5Announcement = tier5Announcement,
            searching2gStateEntered = shouldScheduleSearching2gAnnouncement(nextDebounced) &&
                !previous.noSignalActive &&
                nextDebounced.noSignalActive,
            g2PeriodicReset = g2PeriodicReset,
            tier5Immediate = tier5Immediate,
            limitedVisitedNoSignalStateChanged = _isRunning.value &&
                nextDebounced.isMonitoring &&
                previous.isLimitedServiceNoSignalCamp(passiveSettings) !=
                nextDebounced.isLimitedServiceNoSignalCamp(passiveSettings)
        )
        return nextDebounced to events
    }

    private fun consumeTechnologyChangeAnnouncement(
        previous: ConnectivityStats,
        next: ConnectivityStats,
        previousKnownRat: String?,
        networkOperatorName: String?
    ): String? {
        if (!_isRunning.value) return null
        val nextType = next.radioAccessType?.takeIf { it.isNotBlank() }

        if (!radioTechnologyBaselineReady) {
            if (!nextType.isNullOrBlank()) {
                radioTechnologyBaselineReady = true
            }
            return null
        }

        val previousType = previous.radioAccessType?.takeIf { it.isNotBlank() }
            ?: previousKnownRat?.takeIf { it.isNotBlank() }
        if (previousType == nextType) return null
        if (previousType.isNullOrBlank() || nextType.isNullOrBlank()) return null

        if (nextType == CellularSignalReader.RADIO_2G &&
            SignalStateAnnouncement.isLteNrRadioAccessType(previousType) &&
            _monitoringSettings.value.monitor2gFallback &&
            previous.noSignalActive
        ) {
            // After a no-signal episode, the 2G camped voice replaces the generic technology change.
            return null
        }

        val volumes = _audioVolumes.value
        val target = TechnologyChangeTarget.fromRadioAccessType(nextType)
        val phrases = if (target != null) {
            volumes.phrasesForTechnologyChange(target)
        } else {
            volumes.technologyChangeTo4gPhrases
        }
        return SignalStateAnnouncement.formatTechnologyChange(
            nextType,
            networkOperatorName,
            phrases.speakOperatorName,
            speakTechnologyEnabled = true,
            phrases.speakBand,
            phrases.bandPhraseFor(next.lteEarfcn, next.nrBand)
        )
    }

    private fun consumeTier5StateChange(
        previous: ConnectivityStats,
        next: ConnectivityStats,
        passiveSettings: PassiveSignalSettings
    ): String? {
        if (!_isRunning.value || !next.isMonitoring || next.isPassiveIdleMode) return null

        val wasSignalLow = previous.isInSignalLowVoiceCamp(passiveSettings)
        val isSignalLow = next.isImmediateSignalLowVoiceEntry(previous, passiveSettings)

        if (!tier5BaselineReady) {
            tier5BaselineReady = true
            return if (isSignalLow) formatTier5Announcement(next) else null
        }

        if (!wasSignalLow && isSignalLow) {
            // Tier 10 → tier 5 uses “Signal restored”; dead zone → tier 5 uses “signal low”.
            if (previous.noSignalActive &&
                !next.noSignalActive &&
                !previous.isCompleteNoService &&
                !next.isLimitedService
            ) {
                return null
            }
            return formatTier5Announcement(next)
        }
        if (previous.noSignalActive &&
            !next.noSignalActive &&
            !previous.isCompleteNoService &&
            next.isImmediateSignalLowVoiceEntry(previous, passiveSettings)
        ) {
            return formatTier5Announcement(next)
        }
        return null
    }

    private fun ConnectivityStats.isInSignalLowVoiceCamp(
        passiveSettings: PassiveSignalSettings
    ): Boolean {
        if (isLimitedService) return isSignalLowVoiceCamp(passiveSettings)
        return isTier6CriticalSignal(passiveSettings)
    }

    private fun ConnectivityStats.isImmediateSignalLowVoiceEntry(
        previous: ConnectivityStats,
        passiveSettings: PassiveSignalSettings
    ): Boolean {
        if (isLimitedService) return isSignalLowVoiceCamp(passiveSettings)
        if (previous.isCompleteNoService) {
            return isTier5PoorSignal(passiveSettings) || isTier6CriticalSignal(passiveSettings)
        }
        return isTier6CriticalSignal(passiveSettings)
    }

    private fun consumeLimitedVisitedNoSignalChange(
        previous: ConnectivityStats,
        next: ConnectivityStats,
        passiveSettings: PassiveSignalSettings,
        suppressSignalRestored: Boolean = false
    ): String? {
        if (!_isRunning.value || !next.isMonitoring) return null
        val wasNoSignal = previous.isLimitedServiceNoSignalCamp(passiveSettings)
        val isNoSignal = next.isLimitedServiceNoSignalCamp(passiveSettings)
        if (wasNoSignal == isNoSignal) return null

        if (isNoSignal) {
            return formatNoSignalAnnouncement(next)
        }

        if (suppressSignalRestored || next.isImmediateSignalLowVoiceEntry(previous, passiveSettings)) {
            return null
        }
        return formatSignalRestoredAnnouncement(next)
    }

    private fun consumeNoSignalStateChange(
        previousActive: Boolean,
        nextActive: Boolean,
        next: ConnectivityStats,
        networkOperatorName: String?,
        suppressSignalRestored: Boolean = false
    ): String? {
        if (!_isRunning.value || !next.isMonitoring) return null

        if (!noSignalBaselineReady) {
            noSignalBaselineReady = true
            return null
        }

        if (previousActive == nextActive) return null

        if (nextActive) {
            if (next.isCompleteNoService) {
                // RXSS 0 dead zone — VA-3 replaces VA-1 no-signal entry (including debounced entry).
                return null
            }
            beginNoSignalEpisode()
            if (next.usesG2SignalTiers()) {
                // RXSS 15 — defer entry voice to the 2G periodic timer (same as no-signal repeats).
                return null
            }
            val volumes = _audioVolumes.value
            return SignalStateAnnouncement.formatNoSignalChange(
                active = true,
                networkOperatorName = networkOperatorName,
                radioAccessType = next.resolveNoSignalAnnouncementRadioAccessType(lastKnownRadioAccessType),
                isWifiCallingActive = next.isWifiCallingActive,
                phrases = volumes.noSignalPhrases,
                lteEarfcn = next.lteEarfcn,
                nrBand = next.nrBand
            )
        }

        if (deadzoneRecoveryExitHandled) {
            deadzoneRecoveryExitHandled = false
            endNoSignalEpisode()
            return null
        }

        if (next.isOn2g && next.monitor2gFallbackEnabled && lteRatBeforeNoSignalEpisode != null) {
            endNoSignalEpisode()
            return null
        }

        endNoSignalEpisode()
        if (suppressSignalRestored) {
            return null
        }
        val volumes = _audioVolumes.value
        return SignalStateAnnouncement.formatNoSignalChange(
            active = false,
            networkOperatorName = networkOperatorName,
            radioAccessType = next.resolveNoSignalAnnouncementRadioAccessType(lastKnownRadioAccessType),
            isWifiCallingActive = next.isWifiCallingActive,
            phrases = volumes.noSignalPhrases,
            lteEarfcn = next.lteEarfcn,
            nrBand = next.nrBand
        )
    }

    private fun consumeCompleteNoServiceExitAnnouncement(
        previous: ConnectivityStats,
        next: ConnectivityStats,
        suppressSignalRestored: Boolean,
        networkOperatorName: String?
    ): String? {
        if (!_isRunning.value || !next.isMonitoring) return null
        if (!noSignalBaselineReady) return null
        if (!previous.isCompleteNoService || next.isCompleteNoService) return null

        deadzoneRecoveryExitHandled = true
        if (suppressSignalRestored) {
            return null
        }
        val volumes = _audioVolumes.value
        return SignalStateAnnouncement.formatSignalRestoredAnnouncement(
            networkOperatorName = networkOperatorName,
            radioAccessType = next.resolveNoSignalAnnouncementRadioAccessType(lastKnownRadioAccessType),
            speakOperatorNameEnabled = volumes.noSignalPhrases.speakOperatorName,
            speakTechnologyEnabled = volumes.noSignalPhrases.speakTechnology,
            speakBandEnabled = volumes.noSignalPhrases.speakBand,
            bandPhrase = volumes.noSignalPhrases.bandPhraseFor(next.lteEarfcn, next.nrBand)
        )
    }

    private fun consumeG2FallbackAnnouncement(
        previous: ConnectivityStats,
        next: ConnectivityStats,
        networkOperatorName: String?
    ): String? {
        if (!_isRunning.value || !next.isMonitoring) return null
        if (!next.monitor2gFallbackEnabled) return null
        if (previous.isOn2g || !next.isOn2g) {
            if (!next.isOn2g && !g2FallbackBaselineReady) {
                g2FallbackBaselineReady = true
            }
            return null
        }
        if (!previous.noSignalActive && lteRatBeforeNoSignalEpisode == null) {
            return null
        }

        if (!g2FallbackBaselineReady) {
            g2FallbackBaselineReady = true
            return null
        }

        val fromLteNr = SignalStateAnnouncement.isLteNrRadioAccessType(lteRatBeforeNoSignalEpisode) ||
            SignalStateAnnouncement.isLteNrRadioAccessType(lastKnownRadioAccessType) ||
            SignalStateAnnouncement.isLteNrRadioAccessType(previous.radioAccessType)
        if (!fromLteNr) return null

        endNoSignalEpisode()
        val volumes = _audioVolumes.value
        return SignalStateAnnouncement.formatG2CampedAnnouncement(
            networkOperatorName,
            phrases = volumes.technologyChangeTo2gPhrases,
            lteEarfcn = next.lteEarfcn,
            nrBand = next.nrBand
        )
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
        return SignalStateAnnouncement.formatDeadzoneAnnouncement(
            networkOperatorName,
            _audioVolumes.value.noSignalPhrases.speakOperatorName
        )
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
        // No exit voice: camp on 4G/5G without limited/no-signal/dead zone is implicit full service.
        if (!nextActive) return null

        return SignalStateAnnouncement.formatLimitedServiceChange(
            stats = next,
            lastKnownRadioAccessType = lastKnownRadioAccessType,
            phrases = _audioVolumes.value.limitedServicePhrases
        )
    }

    private fun consumeLimitedServiceOperatorChange(
        previous: ConnectivityStats,
        next: ConnectivityStats
    ): String? {
        if (!_isRunning.value || !next.isMonitoring) return null
        if (!limitedServiceBaselineReady) return null
        if (!next.limitedServiceVisitedOperatorChanged(previous)) return null

        return SignalStateAnnouncement.formatLimitedServiceAnnouncement(
            stats = next,
            lastKnownRadioAccessType = lastKnownRadioAccessType,
            phrases = _audioVolumes.value.limitedServicePhrases
        )
    }

    private fun consumeCellIdentityChange(
        next: CellIdentitySnapshot,
        stats: ConnectivityStats,
        passiveSettings: PassiveSignalSettings,
        radioAccessType: String?,
        networkOperatorName: String?
    ): String? {
        if (!_isRunning.value || !next.hasAnyIdentity()) {
            return null
        }
        if (!stats.shouldAllowCellReselectVoice(passiveSettings)) {
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

        val volumes = _audioVolumes.value
        return CellIdentityAnnouncement.format(
            previous = previous,
            next = next,
            radioAccessType = radioAccessType,
            networkOperatorName = networkOperatorName,
            campedOnVisitedOperator = stats.resolveCampedVisitedOperatorName() != null,
            speakBandEnabled = volumes.cellChangeSpeakBandEnabled,
            bandNamingStyle = volumes.cellChangeBandNamingStyle,
            prefixPhrases = volumes.cellChangePhrases
        )
    }

    private fun beginNoSignalEpisode() {
        if (SignalStateAnnouncement.isLteNrRadioAccessType(lastKnownRadioAccessType)) {
            lteRatBeforeNoSignalEpisode = lastKnownRadioAccessType
        }
        searching2gAnnounced = false
        deadzoneAnnouncedThisEpisode = false
        deadzoneRecoveryExitHandled = false
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
        tier5BaselineReady = false
        stableCellIdentity = CellIdentitySnapshot()
        noSignalDebouncer.reset()
        lteLayerResilienceDebouncer.reset()
        lastKnownRadioAccessType = null
        lteRatBeforeNoSignalEpisode = null
        searching2gAnnounced = false
        deadzoneAnnouncedThisEpisode = false
        deadzoneRecoveryExitHandled = false
    }

    private fun resolveSearching2gFallbackActive(debounced: ConnectivityStats): Boolean {
        val mock = _passiveMockSettings.value
        if (mock.enabled && debounced.isPassiveOnlySession) {
            when (mock.scenario) {
                MockNetworkScenario.SEARCHING_2G -> {
                    if (lteRatBeforeNoSignalEpisode == null) {
                        lteRatBeforeNoSignalEpisode = CellularSignalReader.RADIO_4G
                    }
                    return computeSearching2gFallbackActive(debounced, lteRatBeforeNoSignalEpisode)
                }
                MockNetworkScenario.HOME_4G, MockNetworkScenario.ALT_OPERATOR_4G,
                MockNetworkScenario.HOME_5G_ENDC -> {
                    if (!debounced.noSignalActive) {
                        lteRatBeforeNoSignalEpisode = null
                    }
                    // Mock LTE camp stays on tier 10 (no signal), not tier 11 (searching 2G).
                    return false
                }
                else -> {
                    if (!debounced.noSignalActive) {
                        lteRatBeforeNoSignalEpisode = null
                    }
                }
            }
        }
        return computeSearching2gFallbackActive(debounced, lteRatBeforeNoSignalEpisode)
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
        synchronized(monitorLock) {
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
    }

    fun beginPassiveOnlySession() {
        synchronized(monitorLock) {
            _stats.value = ConnectivityStats(
                isMonitoring = true,
                isPassiveOnlySession = true
            )
        }
    }

    fun setRunning(running: Boolean) {
        synchronized(monitorLock) {
            _isRunning.value = running
            if (running) {
                resetCellIdentityTracking()
            } else {
                monitoringEventsListener = null
                resetCellIdentityTracking()
                _stats.value = ConnectivityStats(isMonitoring = false)
                _rttHistory.value = emptyList()
                _rsrpHistory.value = emptyList()
                histogramRadioAccessType = null
            }
        }
    }

    const val RTT_HISTORY_WINDOW_MS = 60_000L
    const val RSRP_HISTORY_RETENTION_MS = MonitoringSettings.MAX_RSRP_HISTOGRAM_WINDOW_MS

    private fun rsrpHistoryRetentionMs(): Long {
        return maxOf(
            RSRP_HISTORY_RETENTION_MS,
            _monitoringSettings.value.rsrpHistogramWindowMs
        )
    }
}
