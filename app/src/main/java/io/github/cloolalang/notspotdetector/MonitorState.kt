package io.github.cloolalang.notspotdetector

import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.CellIdentityAnnouncement
import io.github.cloolalang.notspotdetector.model.CellIdentitySnapshot
import io.github.cloolalang.notspotdetector.model.CellReselectRate
import io.github.cloolalang.notspotdetector.model.TechnologyHistory
import io.github.cloolalang.notspotdetector.model.CellularRadioMetrics
import io.github.cloolalang.notspotdetector.model.ConnectionQuality
import io.github.cloolalang.notspotdetector.model.ConnectivityStats
import io.github.cloolalang.notspotdetector.model.ServingCellSelection
import io.github.cloolalang.notspotdetector.model.hidingFiveGIfDisabled
import io.github.cloolalang.notspotdetector.model.TechnologyChangeTarget
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.MonitoringUpdateEvents
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.toConnectivityStats
import io.github.cloolalang.notspotdetector.model.withMockPingApplied
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.PingSettings
import io.github.cloolalang.notspotdetector.model.RttSample
import io.github.cloolalang.notspotdetector.model.RsrpSample
import io.github.cloolalang.notspotdetector.model.ThresholdSettings
import io.github.cloolalang.notspotdetector.model.SignalStrengthTier
import io.github.cloolalang.notspotdetector.model.SignalStateAnnouncement
import io.github.cloolalang.notspotdetector.model.currentInServicePulseTier
import io.github.cloolalang.notspotdetector.model.SpecialCellAnnouncement
import io.github.cloolalang.notspotdetector.model.SpecialCellCatalog
import io.github.cloolalang.notspotdetector.model.SpecialCellMatch
import io.github.cloolalang.notspotdetector.model.SpecialCellMatcher
import io.github.cloolalang.notspotdetector.model.shouldBlankStaleCellIdentity
import io.github.cloolalang.notspotdetector.model.hasUsableSignalForMonitoring
import io.github.cloolalang.notspotdetector.model.LteLayerResilienceDebouncer
import io.github.cloolalang.notspotdetector.model.RollingTriggerFilter
import io.github.cloolalang.notspotdetector.model.RxssStateFilterSettings
import io.github.cloolalang.notspotdetector.model.MockNetworkScenario
import io.github.cloolalang.notspotdetector.model.LteLayerResilienceReading
import io.github.cloolalang.notspotdetector.model.computeSearching2gFallbackActive
import io.github.cloolalang.notspotdetector.model.isRadioPoweredOff
import io.github.cloolalang.notspotdetector.model.reconcileOutOfServiceCamp
import io.github.cloolalang.notspotdetector.model.evaluateFlatlineCondition
import io.github.cloolalang.notspotdetector.model.evaluateRsrqPoor
import io.github.cloolalang.notspotdetector.model.isDeadzoneConfirmed
import io.github.cloolalang.notspotdetector.model.isG2FlatlineActive
import io.github.cloolalang.notspotdetector.model.isG2WeakSignal
import io.github.cloolalang.notspotdetector.model.isLimitedServiceAlt2g
import io.github.cloolalang.notspotdetector.model.isLimitedServiceVisited
import io.github.cloolalang.notspotdetector.model.isLimitedServiceNoSignalCamp
import io.github.cloolalang.notspotdetector.model.isSignalLowVoiceCamp
import io.github.cloolalang.notspotdetector.model.isRawTier5PoorSignal
import io.github.cloolalang.notspotdetector.model.isTier4FairSignal
import io.github.cloolalang.notspotdetector.model.isTier5PoorSignal
import io.github.cloolalang.notspotdetector.model.isTier6CriticalSignal
import io.github.cloolalang.notspotdetector.model.resolveSignalStrengthTier
import io.github.cloolalang.notspotdetector.model.shouldAllowCellReselectVoice
import io.github.cloolalang.notspotdetector.model.LIMITED_SERVICE_VOICE_HOLD
import io.github.cloolalang.notspotdetector.model.isRegisteredSimRoaming
import io.github.cloolalang.notspotdetector.model.shouldAnnounceInServiceAfterLimited
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

    private val _specialCellCatalog = MutableStateFlow(SpecialCellCatalog())
    val specialCellCatalog: StateFlow<SpecialCellCatalog> = _specialCellCatalog.asStateFlow()

    private val _specialCellMatch = MutableStateFlow<SpecialCellMatch?>(null)
    val specialCellMatch: StateFlow<SpecialCellMatch?> = _specialCellMatch.asStateFlow()

    private val _rttHistory = MutableStateFlow<List<RttSample>>(emptyList())
    val rttHistory: StateFlow<List<RttSample>> = _rttHistory.asStateFlow()

    private val _rsrpHistory = MutableStateFlow<List<RsrpSample>>(emptyList())
    val rsrpHistory: StateFlow<List<RsrpSample>> = _rsrpHistory.asStateFlow()

    private var lastSpokenSpecialCellKey: String? = null
    private var specialCellMatchHoldUntilMs: Long = 0L
    private var cellIdentityBaselineReady = false
    private val cellReselectTimestamps = mutableListOf<Long>()
    private val technologyChangeTimestamps = mutableListOf<Long>()
    private var radioTechnologyBaselineReady = false
    /** Last technology that was spoken, or the baseline when monitoring started. */
    private var announcedRadioAccessType: String? = null
    private var pendingLteFamilyRat: String? = null
    private var pendingLteFamilyPolls: Int = 0

    /**
     * Consecutive polls of a new 4G / 5G / EN-DC type before that technology is spoken.
     * The displayed type updates on the first poll, so this counts against [announcedRadioAccessType].
     */
    private const val LTE_FAMILY_RAT_VOICE_HOLD_POLLS = 3

    /**
     * Polls a new home/roaming role must last before VA-5 while already in service.
     * A reselection in good coverage often reports another PLMN on the new cell, or
     * blips the roaming flag, for a few seconds without a real roam. Same wait as
     * limited-service voice entry (~4 s at 1 Hz).
     */
    private const val IN_SERVICE_ROAMING_VOICE_HOLD_POLLS = 4
    private var noSignalBaselineReady = false
    private var limitedServiceBaselineReady = false
    private val limitedServiceVoiceFilter = RollingTriggerFilter()
    /** Roaming role already accepted for VA-5. Null until the first in-service sample. */
    private var stableInServiceRoaming: Boolean? = null
    private var pendingInServiceRoaming: Boolean? = null
    private var pendingInServiceRoamingPolls: Int = 0
    private var stableCellIdentity = CellIdentitySnapshot()
    private val noSignalFilter = RollingTriggerFilter()
    private val deadzoneFilter = RollingTriggerFilter()
    private val lowSignalFilter = RollingTriggerFilter()
    private val levelRangeCFilter = RollingTriggerFilter()
    private val levelRangeDFilter = RollingTriggerFilter()
    private val rsrqFilter = RollingTriggerFilter()
    private var heldInServiceSignalTier: SignalStrengthTier? = null
    private var heldFairNeighborTier: SignalStrengthTier? = null
    private var heldPoorNeighborTier: SignalStrengthTier? = null
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
            phrases = volumes.noSignalPhrases,
            bandNamingStyle = volumes.cellChangeBandNamingStyle
        )
    }

    fun formatSignalRestoredAnnouncement(stats: ConnectivityStats = _stats.value): String {
        val volumes = _audioVolumes.value
        return SignalStateAnnouncement.formatSignalRestoredAnnouncement(
            stats,
            lastKnownRadioAccessType,
            phrases = volumes.noSignalPhrases,
            bandNamingStyle = volumes.cellChangeBandNamingStyle
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
            phrases = volumes.signalLowPhrases,
            bandNamingStyle = volumes.cellChangeBandNamingStyle
        )
    }

    fun formatLimitedServiceAnnouncement(stats: ConnectivityStats = _stats.value): String {
        val volumes = _audioVolumes.value
        return SignalStateAnnouncement.formatLimitedServiceAnnouncement(
            stats,
            lastKnownRadioAccessType,
            phrases = volumes.limitedServicePhrases,
            bandNamingStyle = volumes.cellChangeBandNamingStyle
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
            nrBand = stats.nrBand,
            gsmEarfcn = stats.gsmEarfcn,
            bandNamingStyle = volumes.cellChangeBandNamingStyle
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
        val previousMock = _passiveMockSettings.value
        val normalized = settings.normalized()
        _passiveMockSettings.value = normalized
        if (pushMockStatsIfActive()) return
        val pingChanged = previousMock.pingMockEnabled != normalized.pingMockEnabled ||
            previousMock.pingRttMs != normalized.pingRttMs ||
            previousMock.packetLossPercent != normalized.packetLossPercent
        if (!pingChanged) {
            recomputeStatsQuality()
            return
        }
        if (normalized.pingMockEnabled) {
            pulseMockPing()
        } else if (_stats.value.mockPingActive) {
            updateStats(_stats.value)
        } else {
            recomputeStatsQuality()
        }
    }

    /** Publishes one mock ping so active-mode clicks and the chart follow the sliders. */
    fun pulseMockPing() {
        val current = _stats.value
        val mock = _passiveMockSettings.value
        if (!mock.pingMockEnabled || !_isRunning.value || !current.isMonitoring ||
            current.isPassiveOnlySession || current.isPassiveIdleMode
        ) {
            return
        }
        updateStats(current, mockPingTick = true)
    }

    /**
     * Applies mock RSRP/RSRQ immediately during passive-only mock monitoring.
     *
     * The displayed radio metrics update at once, but RXSS state filters do not consume a
     * sample here — those stay on the 1 Hz measurement cycle so dragging the mock slider
     * cannot burn through the flicker wait.
     */
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
        updateStats(mockStats, recordFilterSample = false)
        return true
    }

    fun setPingSettings(settings: PingSettings) {
        _pingSettings.value = settings.normalized()
    }

    fun setMonitoringSettings(settings: MonitoringSettings) {
        _monitoringSettings.value = settings
        publishSpecialCellMatch(_stats.value)
    }

    fun setAudioVolumes(settings: AudioVolumeSettings) {
        _audioVolumes.value = settings.normalized()
        publishSpecialCellMatch(_stats.value)
    }

    fun setSpecialCellCatalog(catalog: SpecialCellCatalog) {
        _specialCellCatalog.value = catalog
        lastSpokenSpecialCellKey = null
        publishSpecialCellMatch(_stats.value)
    }

    fun setThresholds(settings: ThresholdSettings) {
        val normalized = settings.normalized()
        _thresholds.value = normalized
        recomputeStatsQuality()
    }

    fun updateStats(
        rawStats: ConnectivityStats,
        recordFilterSample: Boolean = true,
        mockPingTick: Boolean = false
    ): MonitoringUpdateEvents {
        val events = synchronized(monitorLock) {
            val remapped = rawStats.hidingFiveGIfDisabled(_monitoringSettings.value.fiveGFeaturesEnabled)
            val previousKnownRat = lastKnownRadioAccessType
            val passiveSettings = _passiveSignalSettings.value
            val previous = _stats.value
            val reconciled = remapped.reconcileOutOfServiceCamp()
            rememberRadioAccessType(reconciled)
            // CellularPingMonitor/CellularPassiveSignalMonitor build ConnectivityStats directly
            // from a fresh CellularRadioMetrics reading each poll — debounce the raw reading
            // here (the single funnel point for the actual running-monitor path) rather than in
            // those readers, so a single flickering neighbour reading doesn't make the displayed
            // "4G layers detected" values jump around.
            val stats = reconciled.copy(
                lteLayerResilience = confirmedLayerResilience(
                    reconciled.lteLayerResilience,
                    reconciled.networkServiceMode.isRadioPoweredOff(),
                    reconciled.isCompleteNoService
                )
            )
            val (stabilizedStats, updatedIdentity) = stats.withStabilizedCellIdentity(stableCellIdentity)
            stableCellIdentity = updatedIdentity
            val displayStats = stabilizedStats.withMockPingApplied(
                mock = _passiveMockSettings.value,
                previous = previous,
                tick = mockPingTick
            )
            val enriched = if (displayStats.isPassiveIdleMode) {
                displayStats.copy(quality = ConnectionQuality.PASSIVE_IDLE, severity = 0f)
            } else {
                displayStats.withQuality(_thresholds.value, passiveSettings)
            }
            val (finalStats, events) = buildMonitoringEvents(
                previous,
                enriched,
                passiveSettings,
                previousKnownRat,
                recordFilterSample = recordFilterSample
            )
            _stats.value = finalStats
            publishSpecialCellMatch(finalStats)
            clearRsrpHistogramIfTechnologyChanged(finalStats.radioAccessType)
            if (!enriched.isPassiveIdleMode) {
                recordRttSample(
                    rttMs = enriched.rttMs,
                    jitterMs = enriched.jitterMs,
                    packetLossPercent = enriched.packetLossPercent,
                    timestampMs = enriched.lastPingTimestampMs,
                    pingsSent = enriched.pingsSent
                )
            }
            events
        }
        dispatchMonitoringEvents(events)
        return events
    }

    private fun recordRttSample(
        rttMs: Long?,
        jitterMs: Long,
        packetLossPercent: Float,
        timestampMs: Long,
        pingsSent: Int
    ) {
        if (!_isRunning.value || timestampMs <= 0L || pingsSent <= 0) return
        val cutoff = timestampMs - RTT_HISTORY_WINDOW_MS
        val sample = RttSample(timestampMs, rttMs, jitterMs, packetLossPercent)
        val history = _rttHistory.value.filter { it.timestampMs >= cutoff }
        val replaced = if (history.lastOrNull()?.timestampMs == timestampMs) {
            history.dropLast(1) + sample
        } else {
            history + sample
        }
        _rttHistory.value = replaced.sortedBy { it.timestampMs }
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
            val scanStalled = !stats.isLimitedServiceVisited() &&
                ServingCellSelection.isScanAgeNotable(stats.cellInfoAgeMs)
            recordRsrpSample(
                rsrpDbm = if (scanStalled) null else stats.rsrpDbm,
                isMonitoring = stats.isMonitoring,
                radioAccessType = stats.radioAccessType,
                signalQualityFresh = stats.signalQualityFresh && !stats.isLimitedServiceVisited(),
                recordStaleScan = !stats.isLimitedServiceVisited() &&
                    ServingCellSelection.isScanAgeStale(stats.cellInfoAgeMs)
            )
        }
    }

    private fun recordRsrpSample(
        rsrpDbm: Int?,
        isMonitoring: Boolean,
        radioAccessType: String?,
        signalQualityFresh: Boolean,
        recordStaleScan: Boolean = false
    ) {
        // Skip frozen CellInfo leftovers so the histogram only counts live scans.
        // Null RSRP on a fresh read is still recorded for the "no signal" bin.
        // A scan whose timestamp has stopped is recorded as N/A, including after it goes stale.
        if (!_isRunning.value || !isMonitoring) return
        if (!signalQualityFresh && !recordStaleScan) return
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
                _stats.value = applyRxssStateFilters(
                    withQuality,
                    passiveSettings,
                    recordSample = false
                )
                publishSpecialCellMatch(_stats.value)
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
        val previous = _stats.value
        val merged = previous.copy(
            isMonitoring = false,
            cellularAvailable = false,
            rsrpDbm = metrics.rsrpDbm,
            rsrqDb = metrics.rsrqDb,
            lteSinrDb = metrics.lteSinrDb,
            nrSinrDb = metrics.nrSinrDb,
            timingAdvance = metrics.timingAdvance,
            radioAccessType = metrics.radioAccessType,
            ltePlusIcon = metrics.ltePlusIcon,
            lteEarfcn = metrics.lteEarfcn,
            ltePci = metrics.ltePci,
            lteEci = metrics.lteEci,
            lteTac = metrics.lteTac,
            nrEarfcn = metrics.nrEarfcn,
            nrPci = metrics.nrPci,
            nrNci = metrics.nrNci,
            nrBand = metrics.nrBand,
            gsmEarfcn = metrics.gsmEarfcn,
            gsmBsic = metrics.gsmBsic,
            isOn2g = metrics.isOn2g,
            networkModePreference = metrics.networkModePreference,
            restrictedTo2gNetwork = metrics.restrictedTo2gNetwork,
            isLimitedService = metrics.isLimitedService,
            networkServiceMode = metrics.networkServiceMode,
            isVoiceOnlyNoData = metrics.isVoiceOnlyNoData,
            isNetworkRoaming = metrics.isNetworkRoaming,
            isWifiCallingActive = metrics.isWifiCallingActive,
            hasLimitedServiceOnAnySim = metrics.hasLimitedServiceOnAnySim,
            isCompleteNoService = metrics.isCompleteNoService,
            hasHomeGsmSignal = metrics.hasHomeGsmSignal,
            hasLteNrSignal = metrics.hasLteNrSignal,
            monitor2gFallbackEnabled = monitor2gFallback,
            networkOperatorName = metrics.networkOperatorName,
            homeNetworkOperatorName = metrics.homeNetworkOperatorName,
            virtualNetworkOperatorName = metrics.virtualNetworkOperatorName,
            servingNetworkOperatorName = metrics.servingNetworkOperatorName,
            plmn = metrics.plmn,
            homePlmn = metrics.homePlmn,
            subscriptionId = metrics.subscriptionId,
            simSlotIndex = metrics.simSlotIndex,
            simDisplayName = metrics.simDisplayName,
            simOperatorSelectionMode = metrics.simOperatorSelectionMode,
            manualSimOperatorName = metrics.manualSimOperatorName,
            mobileDataEnabled = metrics.mobileDataEnabled,
            selectedApn = metrics.selectedApn,
            signalPermissionGranted = metrics.permissionGranted,
            cellIdentityPermissionGranted = metrics.cellIdentityPermissionGranted,
            lteLayerResilience = metrics.lteLayerResilience,
            signalQualityFresh = metrics.signalQualityFresh,
            cellInfoAgeMs = metrics.cellInfoAgeMs
        )
        val reconciled = merged.reconcileOutOfServiceCamp()
        val displayReady = reconciled.copy(
            cellularAvailable = if (reconciled.isCompleteNoService) {
                false
            } else {
                metrics.hasUsableSignalForMonitoring(monitor2gFallback, passiveSettings)
            },
            lteLayerResilience = confirmedLayerResilience(
                reconciled.lteLayerResilience,
                reconciled.networkServiceMode.isRadioPoweredOff(),
                reconciled.isCompleteNoService
            )
        )
        val (display, updatedIdentity) = displayReady.withStabilizedCellIdentity(stableCellIdentity)
        stableCellIdentity = updatedIdentity
        _stats.value = display.withQuality(_thresholds.value, passiveSettings)
        publishSpecialCellMatch(_stats.value)
    }

    fun updateSignalMetrics(metrics: CellularRadioMetrics): MonitoringUpdateEvents {
        return refreshSignalMetrics(metrics)
    }

    private fun updateMonitoringSignalMetrics(metrics: CellularRadioMetrics): MonitoringUpdateEvents {
        if (!_isRunning.value) return MonitoringUpdateEvents()

        val previousKnownRat = lastKnownRadioAccessType
        val monitor2gFallback = _monitoringSettings.value.monitor2gFallback
        val passiveSettings = _passiveSignalSettings.value
        val previous = _stats.value
        val merged = previous.copy(
            rsrpDbm = metrics.rsrpDbm,
            rsrqDb = metrics.rsrqDb,
            lteSinrDb = metrics.lteSinrDb,
            nrSinrDb = metrics.nrSinrDb,
            timingAdvance = metrics.timingAdvance,
            radioAccessType = metrics.radioAccessType,
            ltePlusIcon = metrics.ltePlusIcon,
            lteEarfcn = metrics.lteEarfcn,
            ltePci = metrics.ltePci,
            lteEci = metrics.lteEci,
            lteTac = metrics.lteTac,
            nrEarfcn = metrics.nrEarfcn,
            nrPci = metrics.nrPci,
            nrNci = metrics.nrNci,
            nrBand = metrics.nrBand,
            gsmEarfcn = metrics.gsmEarfcn,
            gsmBsic = metrics.gsmBsic,
            isOn2g = metrics.isOn2g,
            networkModePreference = metrics.networkModePreference,
            restrictedTo2gNetwork = metrics.restrictedTo2gNetwork,
            isLimitedService = metrics.isLimitedService,
            networkServiceMode = metrics.networkServiceMode,
            isVoiceOnlyNoData = metrics.isVoiceOnlyNoData,
            isNetworkRoaming = metrics.isNetworkRoaming,
            isWifiCallingActive = metrics.isWifiCallingActive,
            hasLimitedServiceOnAnySim = metrics.hasLimitedServiceOnAnySim,
            isCompleteNoService = metrics.isCompleteNoService,
            hasHomeGsmSignal = metrics.hasHomeGsmSignal,
            hasLteNrSignal = metrics.hasLteNrSignal,
            monitor2gFallbackEnabled = monitor2gFallback,
            networkOperatorName = metrics.networkOperatorName,
            homeNetworkOperatorName = metrics.homeNetworkOperatorName,
            virtualNetworkOperatorName = metrics.virtualNetworkOperatorName,
            servingNetworkOperatorName = metrics.servingNetworkOperatorName,
            plmn = metrics.plmn,
            homePlmn = metrics.homePlmn,
            subscriptionId = metrics.subscriptionId,
            simSlotIndex = metrics.simSlotIndex,
            simDisplayName = metrics.simDisplayName,
            simOperatorSelectionMode = metrics.simOperatorSelectionMode,
            manualSimOperatorName = metrics.manualSimOperatorName,
            mobileDataEnabled = metrics.mobileDataEnabled,
            selectedApn = metrics.selectedApn,
            signalPermissionGranted = metrics.permissionGranted,
            cellIdentityPermissionGranted = metrics.cellIdentityPermissionGranted,
            lteLayerResilience = metrics.lteLayerResilience,
            signalQualityFresh = metrics.signalQualityFresh,
            cellInfoAgeMs = metrics.cellInfoAgeMs
        )
        val reconciled = merged.reconcileOutOfServiceCamp()
        rememberRadioAccessType(reconciled)
        val withLayers = reconciled.copy(
            lteLayerResilience = confirmedLayerResilience(
                reconciled.lteLayerResilience,
                reconciled.networkServiceMode.isRadioPoweredOff(),
                reconciled.isCompleteNoService
            )
        )
        val (stabilized, updatedIdentity) = withLayers.withStabilizedCellIdentity(stableCellIdentity)
        stableCellIdentity = updatedIdentity
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
        publishSpecialCellMatch(finalStats)
        return events
    }

    private fun applyRxssStateFilters(
        stats: ConnectivityStats,
        passiveSettings: PassiveSignalSettings,
        recordSample: Boolean = true
    ): ConnectivityStats {
        if (!stats.isMonitoring) {
            noSignalFilter.reset()
            deadzoneFilter.reset()
            lowSignalFilter.reset()
            levelRangeCFilter.reset()
            levelRangeDFilter.reset()
            rsrqFilter.reset()
            limitedServiceVoiceFilter.reset()
            heldInServiceSignalTier = null
            heldFairNeighborTier = null
            heldPoorNeighborTier = null
            return stats.copy(
                noSignalActive = false,
                deadzoneActive = false,
                lowSignalActive = false,
                levelRangeCActive = false,
                levelRangeDActive = false,
                rsrqPoorActive = false,
                heldInServiceSignalTier = null,
                heldFairNeighborTier = null,
                heldPoorNeighborTier = null
            )
        }

        val noSignalRaw = if (stats.isLimitedService) {
            if (recordSample) noSignalFilter.reset()
            false
        } else {
            stats.evaluateFlatlineCondition(passiveSettings)
        }
        val deadzoneRaw = stats.isCompleteNoService
        val lowSignalRaw = stats.copy(lowSignalActive = null).let { raw ->
            raw.isTier6CriticalSignal(passiveSettings) || raw.isG2WeakSignal(passiveSettings)
        }
        val levelRangeCRaw = stats.copy(levelRangeCActive = null).isTier4FairSignal(passiveSettings)
        val levelRangeDRaw = stats.isRawTier5PoorSignal(passiveSettings)
        val rsrqRaw = stats.copy(rsrqPoorActive = null).evaluateRsrqPoor(passiveSettings)

        val filtered = stats.copy(
            noSignalActive = rollingConfirmed(
                noSignalFilter,
                noSignalRaw,
                passiveSettings.noSignalFilter,
                recordSample
            ),
            deadzoneActive = rollingConfirmed(
                deadzoneFilter,
                deadzoneRaw,
                passiveSettings.deadzoneFilter,
                recordSample
            ),
            lowSignalActive = rollingConfirmed(
                lowSignalFilter,
                lowSignalRaw,
                passiveSettings.lowSignalFilter,
                recordSample
            ),
            levelRangeCActive = rollingConfirmed(
                levelRangeCFilter,
                levelRangeCRaw,
                passiveSettings.levelRangeCFilter,
                recordSample
            ),
            levelRangeDActive = rollingConfirmed(
                levelRangeDFilter,
                levelRangeDRaw,
                passiveSettings.levelRangeDFilter,
                recordSample
            ),
            rsrqPoorActive = rollingConfirmed(
                rsrqFilter,
                rsrqRaw,
                passiveSettings.rsrqFilter,
                recordSample
            )
        )
        val nextPoorNeighbor = nextHeldPoorNeighborTier(filtered, passiveSettings)
        heldPoorNeighborTier = nextPoorNeighbor
        val nextFairNeighbor = nextHeldFairNeighborTier(
            filtered.copy(heldPoorNeighborTier = nextPoorNeighbor),
            passiveSettings
        )
        heldFairNeighborTier = nextFairNeighbor
        val withNeighbors = filtered.copy(
            heldPoorNeighborTier = nextPoorNeighbor,
            heldFairNeighborTier = nextFairNeighbor
        )
        val nextHeld = nextHeldInServiceSignalTier(withNeighbors, passiveSettings)
        heldInServiceSignalTier = nextHeld
        return withNeighbors.copy(heldInServiceSignalTier = nextHeld)
    }

    /**
     * Remember the in-service band on either side of RXSS 4 while the Level Range C filter
     * is still waiting to enter.
     */
    private fun nextHeldFairNeighborTier(
        stats: ConnectivityStats,
        settings: PassiveSignalSettings
    ): SignalStrengthTier? {
        if (!stats.isMonitoring || stats.usesG2SignalTiers()) return null
        val raw = settings.resolveSignalStrengthTier(stats.rsrpDbm)
        val holdingFair = stats.levelRangeCActive == true ||
            (stats.levelRangeCActive == false && raw == SignalStrengthTier.FAIR)
        if (holdingFair) return heldFairNeighborTier
        val afterLow = when (stats.lowSignalActive) {
            true -> SignalStrengthTier.CRITICAL
            false -> if (raw == SignalStrengthTier.CRITICAL) SignalStrengthTier.POOR else raw
            null -> raw
        }
        val displayed = when {
            stats.lowSignalActive == true -> afterLow
            stats.levelRangeDActive == true -> SignalStrengthTier.POOR
            else -> afterLow
        }
        return displayed?.takeIf { it.isInServiceRsrpPulseBand() && it != SignalStrengthTier.FAIR }
    }

    /**
     * Remember the in-service band on either side of RXSS 5 while the Level Range D filter
     * is still waiting to enter.
     */
    private fun nextHeldPoorNeighborTier(
        stats: ConnectivityStats,
        settings: PassiveSignalSettings
    ): SignalStrengthTier? {
        if (!stats.isMonitoring || stats.usesG2SignalTiers()) return null
        val raw = settings.resolveSignalStrengthTier(stats.rsrpDbm)
        val holdingPoor = stats.levelRangeDActive == true ||
            (stats.levelRangeDActive == false && raw == SignalStrengthTier.POOR)
        if (holdingPoor) return heldPoorNeighborTier
        val afterLow = when (stats.lowSignalActive) {
            true -> SignalStrengthTier.CRITICAL
            false -> if (raw == SignalStrengthTier.CRITICAL) SignalStrengthTier.POOR else raw
            null -> raw
        }
        return afterLow?.takeIf { it.isInServiceRsrpPulseBand() && it != SignalStrengthTier.POOR }
    }

    /**
     * Remember the audible in-service RSRP band while the no-signal filter is still waiting.
     * Raw RSRP has already dropped off the map, but RXSS 5/6 must keep pulsing until RXSS 10
     * (or 15) is confirmed.
     */
    private fun nextHeldInServiceSignalTier(
        stats: ConnectivityStats,
        settings: PassiveSignalSettings
    ): SignalStrengthTier? {
        if (!stats.isMonitoring || stats.isDeadzoneConfirmed() || stats.isCompleteNoService) {
            return null
        }
        if (stats.noSignalActive) return null
        if (stats.isLimitedService && stats.isLimitedServiceNoSignalCamp(settings)) return null
        val current = stats.currentInServicePulseTier(settings)
        return current ?: heldInServiceSignalTier
    }

    /**
     * When [recordSample] is false (mock slider / settings recompute), keep the last confirmed
     * trigger and do not count this as a 1 Hz measurement. Inactive filters still adopt [rawActive]
     * immediately so turning a filter off takes effect at once.
     */
    private fun rollingConfirmed(
        filter: RollingTriggerFilter,
        rawActive: Boolean,
        settings: RxssStateFilterSettings,
        recordSample: Boolean
    ): Boolean {
        if (!recordSample && settings.normalized().isActive) {
            return filter.confirmedActive
        }
        return filter.update(rawActive, settings).confirmedActive
    }

    private fun buildMonitoringEvents(
        previous: ConnectivityStats,
        next: ConnectivityStats,
        passiveSettings: PassiveSignalSettings,
        previousKnownRat: String?,
        recordFilterSample: Boolean = true
    ): Pair<ConnectivityStats, MonitoringUpdateEvents> {
        val nextDebounced = applyRxssStateFilters(
            next,
            passiveSettings,
            recordSample = recordFilterSample
        ).let { debounced ->
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
        val previousIdentity = CellIdentitySnapshot.fromStats(previous)
        val nextIdentity = CellIdentitySnapshot.fromStats(nextDebounced)
        val hadCellIdentityBaseline = cellIdentityBaselineReady
        val cellChangeAnnouncement = consumeCellIdentityChange(
            next = nextIdentity,
            stats = nextDebounced,
            passiveSettings = passiveSettings,
            radioAccessType = nextDebounced.radioAccessType,
            networkOperatorName = networkOperatorName
        )
        val reselectOccurred = hadCellIdentityBaseline &&
            _isRunning.value &&
            nextDebounced.isMonitoring &&
            nextDebounced.shouldAllowCellReselectVoice(passiveSettings) &&
            nextIdentity.isServingCellReselectFrom(previousIdentity)
        val technologyChanged = !previous.radioAccessType.isNullOrBlank() &&
            !nextDebounced.radioAccessType.isNullOrBlank() &&
            previous.radioAccessType != nextDebounced.radioAccessType
        val technologyLabelChanged = run {
            val previousLabel = TechnologyHistory.technologyFromStats(previous)
            val nextLabel = TechnologyHistory.technologyFromStats(nextDebounced)
            !previousLabel.isNullOrBlank() &&
                !nextLabel.isNullOrBlank() &&
                previousLabel != nextLabel
        }
        val nowMs = System.currentTimeMillis()
        if (reselectOccurred || technologyChanged) {
            specialCellMatchHoldUntilMs = nowMs + SpecialCellMatcher.reselectHoldMs(
                _monitoringSettings.value.passiveMeasurementIntervalMs
            )
        }
        val updatedTimestamps = CellReselectRate.record(
            timestampsMs = cellReselectTimestamps,
            nowMs = nowMs,
            reselectOccurred = reselectOccurred
        )
        cellReselectTimestamps.clear()
        cellReselectTimestamps.addAll(updatedTimestamps)
        val updatedTechnologyTimestamps = CellReselectRate.record(
            timestampsMs = technologyChangeTimestamps,
            nowMs = nowMs,
            reselectOccurred = technologyLabelChanged
        )
        technologyChangeTimestamps.clear()
        technologyChangeTimestamps.addAll(updatedTechnologyTimestamps)
        val nextWithRate = nextDebounced.copy(
            cellReselectsPerMinute = CellReselectRate.countPerMinute(cellReselectTimestamps, nowMs),
            technologyChangesPerMinute = CellReselectRate.countPerMinute(
                technologyChangeTimestamps,
                nowMs
            )
        )
        val events = MonitoringUpdateEvents(
            cellChangeAnnouncement = cellChangeAnnouncement,
            specialCellAnnouncement = consumeSpecialCellAnnouncement(
                stats = nextDebounced,
                passiveSettings = passiveSettings,
                nowMs = nowMs
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
        return nextWithRate to events
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
                announcedRadioAccessType = nextType
            }
            return null
        }

        val previousType = previous.radioAccessType?.takeIf { it.isNotBlank() }
            ?: previousKnownRat?.takeIf { it.isNotBlank() }
        if (previousType.isNullOrBlank() || nextType.isNullOrBlank()) {
            pendingLteFamilyRat = null
            pendingLteFamilyPolls = 0
            return null
        }

        if (nextType == CellularSignalReader.RADIO_2G &&
            SignalStateAnnouncement.isLteNrRadioAccessType(previousType) &&
            _monitoringSettings.value.monitor2gFallback &&
            previous.noSignalActive
        ) {
            // After a no-signal episode, the 2G camped voice replaces the generic technology change.
            pendingLteFamilyRat = null
            pendingLteFamilyPolls = 0
            announcedRadioAccessType = nextType
            return null
        }

        if (nextType == announcedRadioAccessType) {
            pendingLteFamilyRat = null
            pendingLteFamilyPolls = 0
            return null
        }

        val lteFamilyChange = SignalStateAnnouncement.isLteNrRadioAccessType(announcedRadioAccessType) &&
            SignalStateAnnouncement.isLteNrRadioAccessType(nextType)
        if (lteFamilyChange) {
            if (pendingLteFamilyRat != nextType) {
                pendingLteFamilyRat = nextType
                pendingLteFamilyPolls = 1
                return null
            }
            pendingLteFamilyPolls++
            if (pendingLteFamilyPolls < LTE_FAMILY_RAT_VOICE_HOLD_POLLS) return null
            pendingLteFamilyRat = null
            pendingLteFamilyPolls = 0
        } else {
            pendingLteFamilyRat = null
            pendingLteFamilyPolls = 0
        }
        announcedRadioAccessType = nextType

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
            phrases.bandPhraseFor(
                next.lteEarfcn,
                next.nrBand,
                next.gsmEarfcn,
                namingStyle = volumes.cellChangeBandNamingStyle
            )
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
                !previous.isDeadzoneConfirmed() &&
                !next.isLimitedService
            ) {
                return null
            }
            return formatTier5Announcement(next)
        }
        if (previous.noSignalActive &&
            !next.noSignalActive &&
            !previous.isDeadzoneConfirmed() &&
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
        if (previous.isDeadzoneConfirmed()) {
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
                nrBand = next.nrBand,
                gsmEarfcn = next.gsmEarfcn,
                bandNamingStyle = volumes.cellChangeBandNamingStyle
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
            nrBand = next.nrBand,
            gsmEarfcn = next.gsmEarfcn,
            bandNamingStyle = volumes.cellChangeBandNamingStyle
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
        if (!previous.isDeadzoneConfirmed() || next.isDeadzoneConfirmed()) return null

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
            bandPhrase = volumes.noSignalPhrases.bandPhraseFor(
                next.lteEarfcn,
                next.nrBand,
                next.gsmEarfcn,
                namingStyle = volumes.cellChangeBandNamingStyle
            )
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
            nrBand = next.nrBand,
            gsmEarfcn = next.gsmEarfcn,
            bandNamingStyle = volumes.cellChangeBandNamingStyle
        )
    }

    private fun consumeDeadzoneChange(
        previous: ConnectivityStats,
        next: ConnectivityStats,
        networkOperatorName: String?
    ): String? {
        if (!_isRunning.value || !next.isMonitoring) return null
        if (previous.isDeadzoneConfirmed() == next.isDeadzoneConfirmed()) return null
        if (!next.isDeadzoneConfirmed()) return null
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
        if (!limitedServiceBaselineReady) {
            limitedServiceBaselineReady = true
            limitedServiceVoiceFilter.reset()
            limitedServiceVoiceFilter.update(
                rawActive = next.isLimitedService,
                settings = RxssStateFilterSettings.INACTIVE
            )
            rememberStableInServiceRoaming(next)
            return null
        }

        val previousConfirmed = limitedServiceVoiceFilter.confirmedActive
        val confirmed = limitedServiceVoiceFilter.update(
            rawActive = next.isLimitedService,
            settings = LIMITED_SERVICE_VOICE_HOLD
        ).confirmedActive

        if (!_isRunning.value || !next.isMonitoring) return null

        if (previousConfirmed != confirmed) {
            if (!confirmed && !next.shouldAnnounceInServiceAfterLimited()) return null
            if (!confirmed) {
                rememberStableInServiceRoaming(next)
            } else {
                clearPendingInServiceRoaming()
            }
            return SignalStateAnnouncement.formatLimitedServiceChange(
                stats = next,
                lastKnownRadioAccessType = lastKnownRadioAccessType,
                phrases = _audioVolumes.value.limitedServicePhrases,
                bandNamingStyle = _audioVolumes.value.cellChangeBandNamingStyle
            )
        }

        if (previousConfirmed ||
            !previous.shouldAnnounceInServiceAfterLimited() ||
            !next.shouldAnnounceInServiceAfterLimited()
        ) {
            if (!next.shouldAnnounceInServiceAfterLimited()) {
                clearPendingInServiceRoaming()
            }
            return null
        }

        return consumeInServiceRoamingChange(next)
    }

    /**
     * VA-5 while already in service follows the registered roaming flag only.
     * A different camped PLMN or operator name on the new cell is normal during
     * reselection and must not speak "in service" on its own.
     */
    private fun consumeInServiceRoamingChange(next: ConnectivityStats): String? {
        val roaming = next.isRegisteredSimRoaming()
        val stable = stableInServiceRoaming
        if (stable == null) {
            stableInServiceRoaming = roaming
            return null
        }
        if (roaming == stable) {
            clearPendingInServiceRoaming()
            return null
        }
        if (pendingInServiceRoaming != roaming) {
            pendingInServiceRoaming = roaming
            pendingInServiceRoamingPolls = 1
            return null
        }
        pendingInServiceRoamingPolls++
        if (pendingInServiceRoamingPolls < IN_SERVICE_ROAMING_VOICE_HOLD_POLLS) return null

        stableInServiceRoaming = roaming
        clearPendingInServiceRoaming()
        return SignalStateAnnouncement.formatInServiceAnnouncement(
            stats = next,
            lastKnownRadioAccessType = lastKnownRadioAccessType,
            phrases = _audioVolumes.value.limitedServicePhrases,
            bandNamingStyle = _audioVolumes.value.cellChangeBandNamingStyle
        )
    }

    private fun rememberStableInServiceRoaming(stats: ConnectivityStats) {
        if (!stats.shouldAnnounceInServiceAfterLimited()) return
        stableInServiceRoaming = stats.isRegisteredSimRoaming()
        clearPendingInServiceRoaming()
    }

    private fun clearPendingInServiceRoaming() {
        pendingInServiceRoaming = null
        pendingInServiceRoamingPolls = 0
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
            phrases = _audioVolumes.value.limitedServicePhrases,
            bandNamingStyle = _audioVolumes.value.cellChangeBandNamingStyle
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

    private fun consumeSpecialCellAnnouncement(
        stats: ConnectivityStats,
        passiveSettings: PassiveSignalSettings,
        nowMs: Long = System.currentTimeMillis()
    ): String? {
        publishSpecialCellMatch(stats, passiveSettings)
        if (!_isRunning.value || !stats.isMonitoring) return null
        val volumes = _audioVolumes.value
        if (!volumes.allowsSpecialCellVoice()) {
            lastSpokenSpecialCellKey = null
            return null
        }
        val match = _specialCellMatch.value
        val key = match?.cell?.matchKey
        if (key != null && key == lastSpokenSpecialCellKey) return null
        if (match != null) {
            val spoken = SpecialCellAnnouncement.format(
                cell = match.cell,
                speakType = volumes.specialCellsSpeakType,
                speakSite = volumes.specialCellsSpeakSite,
                speakSector = volumes.specialCellsSpeakSector
            ).takeIf { it.isNotBlank() } ?: return null
            lastSpokenSpecialCellKey = key
            specialCellMatchHoldUntilMs = 0L
            return spoken
        }
        if (nowMs < specialCellMatchHoldUntilMs) return null
        val leavingListedCell = lastSpokenSpecialCellKey != null
        lastSpokenSpecialCellKey = null
        if (!leavingListedCell) return null
        if (!SpecialCellMatcher.hasServingIdentity(stats)) return null
        if (stats.shouldBlankStaleCellIdentity(passiveSettings)) return null
        return SpecialCellAnnouncement.EXIT_UNKNOWN_CELL
    }

    private fun publishSpecialCellMatch(
        stats: ConnectivityStats,
        passiveSettings: PassiveSignalSettings = _passiveSignalSettings.value
    ) {
        val volumes = _audioVolumes.value
        _specialCellMatch.value = if (!volumes.specialCellsDetectionEnabled) {
            null
        } else {
            SpecialCellMatcher.match(
                stats = stats,
                catalog = _specialCellCatalog.value,
                fiveGFeaturesEnabled = _monitoringSettings.value.fiveGFeaturesEnabled,
                blankStaleIdentity = stats.shouldBlankStaleCellIdentity(passiveSettings)
            )
        }
    }

    internal fun expireSpecialCellReselectHoldForTest() {
        specialCellMatchHoldUntilMs = 0L
    }

    private fun resetCellIdentityTracking() {
        lastSpokenSpecialCellKey = null
        specialCellMatchHoldUntilMs = 0L
        cellReselectTimestamps.clear()
        technologyChangeTimestamps.clear()
        cellIdentityBaselineReady = false
        radioTechnologyBaselineReady = false
        announcedRadioAccessType = null
        pendingLteFamilyRat = null
        pendingLteFamilyPolls = 0
        noSignalBaselineReady = false
        limitedServiceBaselineReady = false
        limitedServiceVoiceFilter.reset()
        stableInServiceRoaming = null
        clearPendingInServiceRoaming()
        g2FallbackBaselineReady = false
        tier5BaselineReady = false
        stableCellIdentity = CellIdentitySnapshot()
        noSignalFilter.reset()
        deadzoneFilter.reset()
        lowSignalFilter.reset()
        levelRangeCFilter.reset()
        levelRangeDFilter.reset()
        rsrqFilter.reset()
        heldInServiceSignalTier = null
        heldFairNeighborTier = null
        heldPoorNeighborTier = null
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
                else -> {
                    if (mock.scenario.usesLteNrSignalStrength() && !debounced.noSignalActive) {
                        lteRatBeforeNoSignalEpisode = null
                    }
                    if (mock.scenario.usesLteNrSignalStrength()) {
                        // Mock LTE/NR camp stays on tier 10 (no signal), not tier 11 (searching 2G).
                        return false
                    }
                    if (!debounced.noSignalActive) {
                        lteRatBeforeNoSignalEpisode = null
                    }
                }
            }
        }
        return computeSearching2gFallbackActive(debounced, lteRatBeforeNoSignalEpisode)
    }

    private fun confirmedLayerResilience(
        rawReading: LteLayerResilienceReading?,
        radioOff: Boolean,
        completeNoService: Boolean
    ): LteLayerResilienceReading? {
        if (radioOff || completeNoService) {
            lteLayerResilienceDebouncer.reset()
            return null
        }
        return lteLayerResilienceDebouncer.update(rawReading).confirmedReading
    }

    private fun rememberRadioAccessType(stats: ConnectivityStats) {
        if (stats.networkServiceMode.isRadioPoweredOff() || stats.isCompleteNoService) {
            lastKnownRadioAccessType = null
            return
        }
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
                ltePlusIcon = metrics.ltePlusIcon,
                isOn2g = metrics.isOn2g,
                restrictedTo2gNetwork = metrics.restrictedTo2gNetwork,
                networkServiceMode = metrics.networkServiceMode,
                isCompleteNoService = metrics.isCompleteNoService
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
                publishSpecialCellMatch(_stats.value)
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
