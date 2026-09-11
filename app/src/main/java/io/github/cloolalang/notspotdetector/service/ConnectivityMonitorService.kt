package io.github.cloolalang.notspotdetector.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import io.github.cloolalang.notspotdetector.MainActivity
import io.github.cloolalang.notspotdetector.MonitorState
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.audio.AlertVibrator
import io.github.cloolalang.notspotdetector.audio.CellVoiceAnnouncer
import io.github.cloolalang.notspotdetector.audio.GeigerCounterPlayer
import io.github.cloolalang.notspotdetector.data.AudioVolumeSettingsRepository
import io.github.cloolalang.notspotdetector.data.PassiveMockSettingsRepository
import io.github.cloolalang.notspotdetector.data.PassiveSignalSettingsRepository
import io.github.cloolalang.notspotdetector.data.MonitoringSettingsRepository
import io.github.cloolalang.notspotdetector.data.PingSettingsRepository
import io.github.cloolalang.notspotdetector.data.ThresholdSettingsRepository
import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.MonitoringAnnouncement
import io.github.cloolalang.notspotdetector.model.MonitoringAnnouncementKind
import io.github.cloolalang.notspotdetector.model.RsrpHistogram
import io.github.cloolalang.notspotdetector.model.MonitoringUpdateEvents
import io.github.cloolalang.notspotdetector.model.SignalStateAnnouncement
import io.github.cloolalang.notspotdetector.model.TechnologyChangeTarget
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncementQueue
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerSelection
import io.github.cloolalang.notspotdetector.model.shouldPlayFlatline
import io.github.cloolalang.notspotdetector.model.isG2WeakSignal
import io.github.cloolalang.notspotdetector.model.shouldPlayG2NoSignalVoiceAnnouncements
import io.github.cloolalang.notspotdetector.model.shouldPlayNoSignalVoiceAnnouncements
import io.github.cloolalang.notspotdetector.model.shouldPlayPassiveSignalAndQualityAlerts
import io.github.cloolalang.notspotdetector.model.usesG2SignalTiers
import io.github.cloolalang.notspotdetector.model.shouldAllowG2CampedPeriodicVoice
import io.github.cloolalang.notspotdetector.model.shouldAllowG2WeakPeriodicVoice
import io.github.cloolalang.notspotdetector.model.shouldAllowLimitedServicePeriodicVoice
import io.github.cloolalang.notspotdetector.model.shouldPlayTier5StylePeriodicVoice
import io.github.cloolalang.notspotdetector.network.CellularPassiveSignalMonitor
import io.github.cloolalang.notspotdetector.network.CellularPingMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

class ConnectivityMonitorService : Service() {

    private var serviceJob = SupervisorJob()
    private var serviceScope = CoroutineScope(serviceJob + Dispatchers.Default)
    private lateinit var pingMonitor: CellularPingMonitor
    private lateinit var passiveSignalMonitor: CellularPassiveSignalMonitor
    private lateinit var geigerPlayer: GeigerCounterPlayer
    private lateinit var cellVoiceAnnouncer: CellVoiceAnnouncer
    private var wakeLock: PowerManager.WakeLock? = null
    private var isMonitoringActive = false
    private var isPassiveIdleMode = false
    private var isPassiveOnlyStart = false
    private var activePingTimeoutJob: Job? = null
    private var noSignalPeriodicAnnouncementJob: Job? = null
    private var g2ModePeriodicAnnouncementJob: Job? = null
    private var limitedServicePeriodicAnnouncementJob: Job? = null
    private var deadzonePeriodicAnnouncementJob: Job? = null
    private var tier5PeriodicAnnouncementJob: Job? = null
    private var searching2gAnnouncementJob: Job? = null
    private var rsrpHistogramSampleJob: Job? = null
    private val monitoringAlertMutex = Mutex()
    private val monitoringEventsHandlerMutex = Mutex()
    private val voiceQueue = VoiceAnnouncementQueue()
    private val periodicVoiceGeneration = AtomicInteger(0)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
        MonitorState.setThresholds(ThresholdSettingsRepository(this).load())
        MonitorState.setPingSettings(PingSettingsRepository(this).load())
        MonitorState.setMonitoringSettings(MonitoringSettingsRepository(this).load())
        MonitorState.setPassiveSignalSettings(PassiveSignalSettingsRepository(this).load())
        MonitorState.setPassiveMockSettings(PassiveMockSettingsRepository(this).load())
        MonitorState.setAudioVolumes(AudioVolumeSettingsRepository(this).load())
        pingMonitor = CellularPingMonitor(
            context = this,
            settingsProvider = { MonitorState.pingSettings.value },
            monitoringSettingsProvider = { MonitorState.monitoringSettings.value }
        )
        passiveSignalMonitor = CellularPassiveSignalMonitor(
            context = this,
            intervalProvider = {
                MonitorState.monitoringSettings.value.passiveMeasurementIntervalMs
            },
            monitoringSettingsProvider = { MonitorState.monitoringSettings.value },
            passiveSignalSettingsProvider = { MonitorState.passiveSignalSettings.value },
            passiveMockSettingsProvider = { MonitorState.passiveMockSettings.value },
            passiveIdleModeProvider = { isPassiveIdleMode },
            passiveOnlySessionProvider = { isPassiveOnlyStart }
        )
        geigerPlayer = GeigerCounterPlayer()
        cellVoiceAnnouncer = CellVoiceAnnouncer(this).also { announcer ->
            announcer.setVoiceSelectionProvider {
                VoiceAnnouncerSelection.fromSettings(MonitorState.audioVolumes.value)
            }
        }
    }

    private fun registerMonitoringEventsListener() {
        MonitorState.setMonitoringEventsListener { events ->
            if (!isMonitoringActive) return@setMonitoringEventsListener
            serviceScope.launch {
                monitoringEventsHandlerMutex.withLock {
                    handleMonitoringEvents(events)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopMonitoring()
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (isMonitoringActive) {
            return START_STICKY
        }

        isPassiveOnlyStart = intent?.getBooleanExtra(EXTRA_PASSIVE_ONLY, false) ?: false
        isMonitoringActive = true
        isPassiveIdleMode = false
        registerMonitoringEventsListener()
        MonitorState.setRunning(true)
        startRsrpHistogramSampler()
        acquireWakeLock()

        val notificationText = if (isPassiveOnlyStart) {
            getString(R.string.notification_passive_only)
        } else {
            getString(R.string.notification_starting)
        }
        val notification = buildNotification(notificationText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (isPassiveOnlyStart) {
            startPassiveOnlyMonitoring()
        } else {
            startActivePingMonitoring()
            scheduleActivePingTimeout()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopMonitoring()
        serviceScope.cancel()
        serviceJob.cancel()
        super.onDestroy()
    }

    /**
     * Called by the platform (API 35+) when this foreground service hits the `dataSync` type's
     * execution time limit (a total of ~6h per rolling 24h window) — most likely to be hit on a
     * long screen-locked stretch (e.g. overnight), since that's when nothing else brings the app
     * to the foreground to reset the budget. The system requires [stopSelf] within a few seconds
     * or it raises an ANR; simply restarting immediately is *not* possible — the OS throws
     * `ForegroundServiceStartNotAllowedException` for further `dataSync` starts until the app is
     * brought to the foreground again. So instead: stop cleanly and leave a notification telling
     * the user monitoring paused and to reopen the app to resume it.
     */
    override fun onTimeout(startId: Int, fgsType: Int) {
        runCatching { notifyMonitoringPausedByTimeLimit() }
        stopMonitoring()
        stopSelf(startId)
    }

    private fun notifyMonitoringPausedByTimeLimit() {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, NotificationChannels.MONITOR_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_paused_time_limit))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (isMonitoringActive) {
            val notification = buildNotification(MonitorState.stats.value.statusLabel(this))
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun startActivePingMonitoring() {
        pingMonitor.start(serviceScope) { stats ->
            handleStatsUpdate(stats)
        }
        startGeigerPlayer()
    }

    private fun startPassiveOnlyMonitoring() {
        MonitorState.beginPassiveOnlySession()
        passiveSignalMonitor.start(serviceScope) { stats ->
            handleStatsUpdate(stats)
        }
        startGeigerPlayer()
    }

    private fun startRsrpHistogramSampler() {
        rsrpHistogramSampleJob?.cancel()
        rsrpHistogramSampleJob = serviceScope.launch {
            while (isActive) {
                MonitorState.tickRsrpHistogramSample()
                delay(RsrpHistogram.SAMPLE_INTERVAL_MS)
            }
        }
    }

    private fun startGeigerPlayer() {
        geigerPlayer.start(
            scope = serviceScope,
            statsProvider = { MonitorState.stats.value },
            thresholdsProvider = { MonitorState.thresholds.value },
            audioVolumesProvider = { MonitorState.audioVolumes.value },
            monitoringSettingsProvider = { MonitorState.monitoringSettings.value },
            passiveSignalSettingsProvider = { MonitorState.passiveSignalSettings.value }
        )
    }

    private fun handleStatsUpdate(stats: io.github.cloolalang.notspotdetector.model.ConnectivityStats) {
        renewWakeLockIfNeeded()
        MonitorState.updateStats(stats)
    }

    private suspend fun handleMonitoringEvents(events: MonitoringUpdateEvents) {
        val incoming = events.immediateAnnouncements()
        if (incoming.isNotEmpty()) {
            val result = voiceQueue.enqueue(incoming)
            if (result.supersededServiceState) {
                periodicVoiceGeneration.incrementAndGet()
            }
            result.toneOnly.forEach { playSoundIcon(it) }
            if (result.interruptCurrent) {
                cellVoiceAnnouncer.stop()
            }
            serviceScope.launch {
                monitoringAlertMutex.withLock {
                    drainVoiceQueue()
                }
            }
        }
        val passiveSettings = MonitorState.passiveSignalSettings.value
        val monitoringSettings = MonitorState.monitoringSettings.value
        val playQualityAlerts = MonitorState.stats.value.shouldPlayPassiveSignalAndQualityAlerts(
            monitoringSettings,
            passiveSettings
        )
        val stats = MonitorState.stats.value
        if (events.searching2gStateEntered) {
            scheduleSearching2gAnnouncement()
        } else if (!stats.searching2gFallbackActive || stats.isOn2g || stats.isCompleteNoService) {
            searching2gAnnouncementJob?.cancel()
            searching2gAnnouncementJob = null
        }
        updateNoSignalPeriodicAnnouncements(
            forceRestart = events.noSignalStateChanged ||
                events.limitedVisitedNoSignalStateChanged
        )
        updateG2ModePeriodicAnnouncements(
            forceRestart = events.radioTechnologyChanged ||
                events.g2FallbackAnnounced ||
                events.g2PeriodicReset ||
                (events.noSignalStateChanged && stats.usesG2SignalTiers())
        )
        updateLimitedServicePeriodicAnnouncements()
        updateDeadzonePeriodicAnnouncements()
        updateTier5PeriodicAnnouncements(
            forceRestart = events.tier5Announced && playQualityAlerts,
            skipInitialDelay = events.tier5Immediate
        )
        updateNotification(MonitorState.stats.value.statusLabel(this))
    }

    private suspend fun drainVoiceQueue() {
        while (true) {
            val item = voiceQueue.startNext() ?: break
            playQueuedAnnouncement(item)
            voiceQueue.markFinished(item.id)
        }
    }

    private suspend fun playQueuedAnnouncement(item: VoiceAnnouncementQueue.Item) {
        val stillCurrent = { voiceQueue.isPlaying(item.id) }
        if (!stillCurrent()) return
        val announcement = item.announcement
        when (announcement.kind) {
            MonitoringAnnouncementKind.NO_SIGNAL_STATE ->
                playNoSignalAlertAwait(announcement.message, stillCurrent)
            MonitoringAnnouncementKind.DEADZONE ->
                playDeadzoneAlertAwait(announcement.message, stillCurrent)
            MonitoringAnnouncementKind.LIMITED_SERVICE_STATE,
            MonitoringAnnouncementKind.LIMITED_SERVICE_OPERATOR ->
                playLimitedServiceAlertAwait(announcement.message, stillCurrent)
            MonitoringAnnouncementKind.G2_FALLBACK ->
                playG2FallbackAlertAwait(announcement.message, stillCurrent)
            MonitoringAnnouncementKind.TIER5 ->
                playTier5VoiceAlertAwait(announcement.message, stillCurrent)
            MonitoringAnnouncementKind.TECHNOLOGY_CHANGE ->
                playTechnologyChangeAlertAwait(
                    announcement.message,
                    announcement.targetRadioAccessType,
                    stillCurrent
                )
            MonitoringAnnouncementKind.CELL_IDENTITY ->
                playCellChangeAlertAwait(announcement.message, stillCurrent)
        }
    }

    private fun scheduleSearching2gAnnouncement() {
        searching2gAnnouncementJob?.cancel()
        searching2gAnnouncementJob = serviceScope.launch {
            delay(SEARCHING_2G_ANNOUNCEMENT_DELAY_MS)
            val current = MonitorState.stats.value
            if (!MonitorState.shouldScheduleSearching2gAnnouncement(current)) return@launch
            MonitorState.markSearching2gAnnounced()
            playNoSignalAlert(MonitorState.formatSearching2gAnnouncement())
        }
    }

    private fun updateNoSignalPeriodicAnnouncements(forceRestart: Boolean = false) {
        val stats = MonitorState.stats.value
        val passiveSettings = MonitorState.passiveSignalSettings.value
        val shouldAnnounce = stats.shouldPlayNoSignalVoiceAnnouncements(passiveSettings)

        if (!shouldAnnounce) {
            noSignalPeriodicAnnouncementJob?.cancel()
            noSignalPeriodicAnnouncementJob = null
            return
        }

        if (forceRestart) {
            noSignalPeriodicAnnouncementJob?.cancel()
            noSignalPeriodicAnnouncementJob = null
        }
        if (noSignalPeriodicAnnouncementJob?.isActive == true) return

        noSignalPeriodicAnnouncementJob = serviceScope.launch {
            while (isActive) {
                delay(PERIODIC_ANNOUNCEMENT_MS)
                val current = MonitorState.stats.value
                val settings = MonitorState.passiveSignalSettings.value
                if (!current.shouldPlayNoSignalVoiceAnnouncements(settings)) {
                    break
                }
                playNoSignalAlert(MonitorState.formatNoSignalAnnouncement(current))
            }
        }
    }

    private fun updateG2ModePeriodicAnnouncements(forceRestart: Boolean = false) {
        val stats = MonitorState.stats.value
        val passiveSettings = MonitorState.passiveSignalSettings.value
        val shouldAnnounce = stats.isMonitoring &&
            !stats.isPassiveIdleMode &&
            stats.usesG2SignalTiers() &&
            (
                stats.shouldPlayG2NoSignalVoiceAnnouncements(passiveSettings) ||
                    stats.shouldAllowG2WeakPeriodicVoice(passiveSettings) ||
                    stats.shouldAllowG2CampedPeriodicVoice(passiveSettings)
                )

        if (!shouldAnnounce) {
            g2ModePeriodicAnnouncementJob?.cancel()
            g2ModePeriodicAnnouncementJob = null
            return
        }

        if (forceRestart) {
            g2ModePeriodicAnnouncementJob?.cancel()
            g2ModePeriodicAnnouncementJob = null
        }
        if (g2ModePeriodicAnnouncementJob?.isActive == true) return

        g2ModePeriodicAnnouncementJob = serviceScope.launch {
            var playImmediately = forceRestart
            while (isActive) {
                if (!playImmediately) {
                    delay(PERIODIC_ANNOUNCEMENT_MS)
                }
                playImmediately = false
                val current = MonitorState.stats.value
                val settings = MonitorState.passiveSignalSettings.value
                if (!current.isMonitoring ||
                    current.isPassiveIdleMode ||
                    !current.usesG2SignalTiers()
                ) {
                    break
                }
                val generation = periodicVoiceGeneration.get()
                monitoringAlertMutex.withLock {
                    if (periodicVoiceGeneration.get() != generation) {
                        return@withLock
                    }
                    playG2ModePeriodicAnnouncement(current, settings)
                }
            }
        }
    }

    private suspend fun playG2ModePeriodicAnnouncement(
        stats: io.github.cloolalang.notspotdetector.model.ConnectivityStats,
        passiveSettings: io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
    ) {
        when {
            stats.shouldPlayG2NoSignalVoiceAnnouncements(passiveSettings) ->
                playNoSignalAlertAwait(MonitorState.formatNoSignalAnnouncement(stats))
            stats.shouldAllowG2WeakPeriodicVoice(passiveSettings) ->
                playTier5VoiceAlertAwait(MonitorState.formatTier5Announcement(stats))
            stats.shouldAllowG2CampedPeriodicVoice(passiveSettings) -> {
                val volumes = MonitorState.audioVolumes.value
                playG2FallbackAlertAwait(
                    SignalStateAnnouncement.formatG2CampedAnnouncement(
                        stats.networkOperatorName,
                        phrases = volumes.technologyChangeTo2gPhrases,
                        lteEarfcn = stats.lteEarfcn,
                        nrBand = stats.nrBand
                    )
                )
            }
        }
    }

    private fun updateLimitedServicePeriodicAnnouncements() {
        val stats = MonitorState.stats.value
        val passiveSettings = MonitorState.passiveSignalSettings.value
        val shouldAnnounce = stats.shouldAllowLimitedServicePeriodicVoice(passiveSettings)

        if (!shouldAnnounce) {
            limitedServicePeriodicAnnouncementJob?.cancel()
            limitedServicePeriodicAnnouncementJob = null
            return
        }

        if (limitedServicePeriodicAnnouncementJob?.isActive == true) return

        limitedServicePeriodicAnnouncementJob = serviceScope.launch {
            while (isActive) {
                delay(PERIODIC_ANNOUNCEMENT_MS)
                val current = MonitorState.stats.value
                val settings = MonitorState.passiveSignalSettings.value
                if (!current.shouldAllowLimitedServicePeriodicVoice(settings)) {
                    break
                }
                playPeriodicLimitedServiceAlert(MonitorState.formatLimitedServiceAnnouncement(current))
            }
        }
    }

    private fun updateDeadzonePeriodicAnnouncements() {
        val stats = MonitorState.stats.value
        val shouldAnnounce = stats.isMonitoring &&
            !stats.isPassiveIdleMode &&
            stats.isCompleteNoService

        if (!shouldAnnounce) {
            deadzonePeriodicAnnouncementJob?.cancel()
            deadzonePeriodicAnnouncementJob = null
            return
        }

        if (deadzonePeriodicAnnouncementJob?.isActive == true) return

        deadzonePeriodicAnnouncementJob = serviceScope.launch {
            while (isActive) {
                delay(PERIODIC_ANNOUNCEMENT_MS)
                val current = MonitorState.stats.value
                if (!current.isMonitoring ||
                    current.isPassiveIdleMode ||
                    !current.isCompleteNoService
                ) {
                    break
                }
                playPeriodicDeadzoneAlert(MonitorState.formatDeadzoneAnnouncement(current))
            }
        }
    }

    private fun updateTier5PeriodicAnnouncements(
        forceRestart: Boolean = false,
        skipInitialDelay: Boolean = false
    ) {
        val stats = MonitorState.stats.value
        val passiveSettings = MonitorState.passiveSignalSettings.value
        val volumes = MonitorState.audioVolumes.value.normalized()
        val shouldAnnounce = stats.isMonitoring &&
            !stats.isPassiveIdleMode &&
            volumes.tier5AnnouncerEnabled &&
            stats.shouldPlayTier5StylePeriodicVoice(passiveSettings)

        if (!shouldAnnounce) {
            tier5PeriodicAnnouncementJob?.cancel()
            tier5PeriodicAnnouncementJob = null
            return
        }

        if (!forceRestart && tier5PeriodicAnnouncementJob?.isActive == true) return

        tier5PeriodicAnnouncementJob?.cancel()
        tier5PeriodicAnnouncementJob = serviceScope.launch {
            delay(
                if (skipInitialDelay) {
                    PERIODIC_ANNOUNCEMENT_MS
                } else {
                    TIER5_INITIAL_ANNOUNCEMENT_DELAY_MS
                }
            )
            while (isActive) {
                val current = MonitorState.stats.value
                val settings = MonitorState.passiveSignalSettings.value
                val audio = MonitorState.audioVolumes.value.normalized()
                if (!current.isMonitoring ||
                    current.isPassiveIdleMode ||
                    !audio.tier5AnnouncerEnabled ||
                    !current.shouldPlayTier5StylePeriodicVoice(settings)
                ) {
                    break
                }
                playTier5VoiceAlert(MonitorState.formatTier5Announcement(current))
                delay(PERIODIC_ANNOUNCEMENT_MS)
            }
        }
    }

    private suspend fun playAlertWithVoiceAwait(
        onPlayTone: () -> Unit,
        toneDurationMs: Int,
        announcement: String?,
        voiceEnabled: Boolean,
        voiceVolume: Float,
        stillCurrent: () -> Boolean = { true }
    ) {
        if (!stillCurrent()) return
        onPlayTone()
        delay(AudioVolumeSettings.voiceDelayAfterAlertTone(toneDurationMs))
        if (!stillCurrent()) return
        val masterVoiceEnabled = MonitorState.audioVolumes.value.masterVoiceAnnouncementsEnabled
        if (masterVoiceEnabled && voiceEnabled && !announcement.isNullOrBlank() && voiceVolume > 0f) {
            cellVoiceAnnouncer.speakAwait(announcement, voiceVolume)
        }
    }

    private suspend fun playCellChangeAlertAwait(
        announcement: String?,
        stillCurrent: () -> Boolean = { true }
    ) {
        val volumes = MonitorState.audioVolumes.value.normalized()
        playAlertWithVoiceAwait(
            onPlayTone = ::playCellChangeBell,
            toneDurationMs = GeigerCounterPlayer.CELL_CHANGE_BELL_DURATION_MS,
            announcement = announcement,
            voiceEnabled = volumes.cellChangeVoiceEnabled,
            voiceVolume = volumes.cellChangeVoiceVolume,
            stillCurrent = stillCurrent
        )
    }

    private suspend fun playTechnologyChangeAlertAwait(
        announcement: String?,
        targetRadioAccessType: String?,
        stillCurrent: () -> Boolean = { true }
    ) {
        val alertVolumes = MonitorState.audioVolumes.value
            .normalized()
            .technologyChangeAlertVolumes(targetRadioAccessType)
            ?: return
        playAlertWithVoiceAwait(
            onPlayTone = { geigerPlayer.playTechnologyChangeTone(alertVolumes.toneVolume) },
            toneDurationMs = GeigerCounterPlayer.TECHNOLOGY_CHANGE_TONE_DURATION_MS,
            announcement = announcement,
            voiceEnabled = alertVolumes.voiceEnabled,
            voiceVolume = alertVolumes.voiceVolume,
            stillCurrent = stillCurrent
        )
    }

    private suspend fun playNoSignalAlertAwait(
        announcement: String?,
        stillCurrent: () -> Boolean = { true }
    ) {
        val volumes = MonitorState.audioVolumes.value.normalized()
        if (volumes.noSignalVibrationEnabled) {
            AlertVibrator.buzzNoSignal(this)
        }
        playAlertWithVoiceAwait(
            onPlayTone = {
                geigerPlayer.previewNoSignalTone(volumes.noSignalToneVolume)
            },
            toneDurationMs = GeigerCounterPlayer.NO_SIGNAL_ALERT_TONE_DURATION_MS,
            announcement = announcement,
            voiceEnabled = volumes.noSignalVoiceEnabled,
            voiceVolume = volumes.noSignalVoiceVolume,
            stillCurrent = stillCurrent
        )
    }

    private fun playNoSignalAlert(announcement: String?) {
        playPeriodicAlert {
            val current = MonitorState.stats.value
            val settings = MonitorState.passiveSignalSettings.value
            when {
                current.shouldPlayNoSignalVoiceAnnouncements(settings) ->
                    playNoSignalAlertAwait(MonitorState.formatNoSignalAnnouncement(current))
                current.searching2gFallbackActive && !current.isOn2g && !current.isCompleteNoService ->
                    playNoSignalAlertAwait(announcement)
                else -> return@playPeriodicAlert
            }
        }
    }

    private fun playPeriodicLimitedServiceAlert(announcement: String?) {
        playPeriodicAlert {
            val current = MonitorState.stats.value
            val settings = MonitorState.passiveSignalSettings.value
            if (!current.shouldAllowLimitedServicePeriodicVoice(settings)) {
                return@playPeriodicAlert
            }
            playLimitedServiceAlertAwait(MonitorState.formatLimitedServiceAnnouncement(current))
        }
    }

    private fun playPeriodicDeadzoneAlert(announcement: String?) {
        playPeriodicAlert {
            val current = MonitorState.stats.value
            if (!current.isMonitoring || current.isPassiveIdleMode || !current.isCompleteNoService) {
                return@playPeriodicAlert
            }
            playDeadzoneAlertAwait(MonitorState.formatDeadzoneAnnouncement(current))
        }
    }

    private fun playPeriodicAlert(block: suspend () -> Unit) {
        val generation = periodicVoiceGeneration.get()
        serviceScope.launch {
            monitoringAlertMutex.withLock {
                if (periodicVoiceGeneration.get() != generation) return@withLock
                block()
            }
        }
    }

    private suspend fun playLimitedServiceAlertAwait(
        announcement: String?,
        stillCurrent: () -> Boolean = { true }
    ) {
        val volumes = MonitorState.audioVolumes.value.normalized()
        val toneMs = MonitorState.passiveSignalSettings.value.normalized().limitedServiceTierPulseDurationMs
        playAlertWithVoiceAwait(
            onPlayTone = {
                geigerPlayer.previewLimitedServiceTone(
                    volume = volumes.limitedServiceToneVolume,
                    lowFrequencyHz = volumes.limitedServiceTierPulseFrequencyHz,
                    highFrequencyHz = volumes.limitedServiceTwoToneHighFrequencyHz(),
                    toneMs = toneMs
                )
            },
            toneDurationMs = GeigerCounterPlayer.limitedServiceAlertToneDurationMs(toneMs),
            announcement = announcement,
            voiceEnabled = volumes.limitedServiceVoiceEnabled,
            voiceVolume = volumes.limitedServiceVoiceVolume,
            stillCurrent = stillCurrent
        )
    }

    private suspend fun playG2FallbackAlertAwait(
        announcement: String?,
        stillCurrent: () -> Boolean = { true }
    ) {
        val alertVolumes = MonitorState.audioVolumes.value
            .normalized()
            .technologyChangeAlertVolumes(TechnologyChangeTarget.TO_2G)
        playAlertWithVoiceAwait(
            onPlayTone = { geigerPlayer.playTechnologyChangeTone(alertVolumes.toneVolume) },
            toneDurationMs = GeigerCounterPlayer.TECHNOLOGY_CHANGE_TONE_DURATION_MS,
            announcement = announcement,
            voiceEnabled = alertVolumes.voiceEnabled,
            voiceVolume = alertVolumes.voiceVolume,
            stillCurrent = stillCurrent
        )
    }

    private suspend fun playDeadzoneAlertAwait(
        announcement: String?,
        stillCurrent: () -> Boolean = { true }
    ) {
        val volumes = MonitorState.audioVolumes.value.normalized()
        playAlertWithVoiceAwait(
            onPlayTone = {
                geigerPlayer.previewNoSignalTone(volumes.noSignalToneVolume)
            },
            toneDurationMs = GeigerCounterPlayer.NO_SIGNAL_ALERT_TONE_DURATION_MS,
            announcement = announcement,
            voiceEnabled = volumes.noSignalVoiceEnabled,
            voiceVolume = volumes.noSignalVoiceVolume,
            stillCurrent = stillCurrent
        )
    }

    private suspend fun playTier5VoiceAlertAwait(
        announcement: String?,
        stillCurrent: () -> Boolean = { true }
    ) {
        val volumes = MonitorState.audioVolumes.value.normalized()
        if (!volumes.masterVoiceAnnouncementsEnabled ||
            !volumes.tier5AnnouncerEnabled ||
            announcement.isNullOrBlank() ||
            volumes.tier5AnnouncerVolume <= 0f ||
            !stillCurrent()
        ) {
            return
        }
        cellVoiceAnnouncer.speakAwait(announcement, volumes.tier5AnnouncerVolume)
    }

    private fun playTier5VoiceAlert(announcement: String?) {
        playPeriodicAlert {
            val current = MonitorState.stats.value
            val settings = MonitorState.passiveSignalSettings.value
            val audio = MonitorState.audioVolumes.value.normalized()
            if (!current.isMonitoring ||
                current.isPassiveIdleMode ||
                !audio.tier5AnnouncerEnabled ||
                !current.shouldPlayTier5StylePeriodicVoice(settings)
            ) {
                return@playPeriodicAlert
            }
            playTier5VoiceAlertAwait(MonitorState.formatTier5Announcement(current))
        }
    }

    private fun playSoundIcon(announcement: MonitoringAnnouncement) {
        val volumes = MonitorState.audioVolumes.value.normalized()
        when (announcement.kind) {
            MonitoringAnnouncementKind.CELL_IDENTITY -> playCellChangeBell()
            MonitoringAnnouncementKind.TECHNOLOGY_CHANGE -> {
                val alertVolumes = volumes.technologyChangeAlertVolumes(
                    announcement.targetRadioAccessType
                ) ?: return
                geigerPlayer.playTechnologyChangeTone(alertVolumes.toneVolume)
            }
            MonitoringAnnouncementKind.G2_FALLBACK -> {
                val alertVolumes = volumes.technologyChangeAlertVolumes(TechnologyChangeTarget.TO_2G)
                geigerPlayer.playTechnologyChangeTone(alertVolumes.toneVolume)
            }
            MonitoringAnnouncementKind.NO_SIGNAL_STATE -> {
                if (volumes.noSignalVibrationEnabled) {
                    AlertVibrator.buzzNoSignal(this)
                }
                geigerPlayer.previewNoSignalTone(volumes.noSignalToneVolume)
            }
            MonitoringAnnouncementKind.DEADZONE ->
                geigerPlayer.previewNoSignalTone(volumes.noSignalToneVolume)
            MonitoringAnnouncementKind.LIMITED_SERVICE_STATE,
            MonitoringAnnouncementKind.LIMITED_SERVICE_OPERATOR -> {
                val toneMs = MonitorState.passiveSignalSettings.value
                    .normalized()
                    .limitedServiceTierPulseDurationMs
                geigerPlayer.previewLimitedServiceTone(
                    volume = volumes.limitedServiceToneVolume,
                    lowFrequencyHz = volumes.limitedServiceTierPulseFrequencyHz,
                    highFrequencyHz = volumes.limitedServiceTwoToneHighFrequencyHz(),
                    toneMs = toneMs
                )
            }
            MonitoringAnnouncementKind.TIER5 -> Unit
        }
    }

    private fun playCellChangeBell() {
        geigerPlayer.playCellChangeBell(
            MonitorState.audioVolumes.value.normalized().cellChangeBellVolume
        )
    }


    private fun scheduleActivePingTimeout() {
        activePingTimeoutJob?.cancel()
        activePingTimeoutJob = serviceScope.launch {
            delay(ACTIVE_PING_DURATION_MS)
            enterPassiveIdleMode()
        }
    }

    private fun enterPassiveIdleMode() {
        if (!isMonitoringActive || isPassiveIdleMode) return

        isPassiveIdleMode = true
        activePingTimeoutJob?.cancel()
        activePingTimeoutJob = null

        pingMonitor.stop()
        MonitorState.enterPassiveIdleMode()

        if (!isPassiveOnlyStart) {
            passiveSignalMonitor.start(serviceScope) { stats ->
                handleStatsUpdate(stats)
            }
        }

        updateNotification(getString(R.string.notification_passive_idle))
    }

    private fun stopMonitoring() {
        if (!isMonitoringActive && wakeLock == null) {
            return
        }
        isMonitoringActive = false
        isPassiveIdleMode = false
        isPassiveOnlyStart = false
        activePingTimeoutJob?.cancel()
        activePingTimeoutJob = null
        noSignalPeriodicAnnouncementJob?.cancel()
        noSignalPeriodicAnnouncementJob = null
        g2ModePeriodicAnnouncementJob?.cancel()
        g2ModePeriodicAnnouncementJob = null
        limitedServicePeriodicAnnouncementJob?.cancel()
        limitedServicePeriodicAnnouncementJob = null
        deadzonePeriodicAnnouncementJob?.cancel()
        deadzonePeriodicAnnouncementJob = null
        tier5PeriodicAnnouncementJob?.cancel()
        tier5PeriodicAnnouncementJob = null
        searching2gAnnouncementJob?.cancel()
        searching2gAnnouncementJob = null
        rsrpHistogramSampleJob?.cancel()
        rsrpHistogramSampleJob = null
        voiceQueue.clear()
        periodicVoiceGeneration.incrementAndGet()
        pingMonitor.stop()
        passiveSignalMonitor.stop()
        geigerPlayer.stop()
        cellVoiceAnnouncer.shutdown()
        MonitorState.setMonitoringEventsListener(null)
        releaseWakeLock()
        MonitorState.setRunning(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "NotspotDetector::ConnectivityMonitor"
        ).apply {
            setReferenceCounted(false)
        }
        renewWakeLockIfNeeded()
    }

    private fun renewWakeLockIfNeeded() {
        if (!isMonitoringActive) return
        val lock = wakeLock ?: return
        runCatching {
            lock.acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    private fun updateNotification(statusText: String) {
        if (!isMonitoringActive) return
        val notification = buildNotification(statusText)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ConnectivityMonitorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationChannels.MONITOR_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                getString(R.string.stop_monitoring),
                stopIntent
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        const val ACTION_STOP = "io.github.cloolalang.notspotdetector.action.STOP_MONITOR"
        const val EXTRA_PASSIVE_ONLY = "io.github.cloolalang.notspotdetector.extra.PASSIVE_ONLY"
        private const val NOTIFICATION_ID = 1001
        private const val ACTIVE_PING_DURATION_MS = 10 * 60 * 1000L
        private const val PERIODIC_ANNOUNCEMENT_MS = 30_000L
        private const val TIER5_INITIAL_ANNOUNCEMENT_DELAY_MS = 5_000L
        private const val SEARCHING_2G_ANNOUNCEMENT_DELAY_MS = 5_000L
        private const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L

        fun start(context: Context, passiveOnly: Boolean = false) {
            val intent = Intent(context, ConnectivityMonitorService::class.java)
                .putExtra(EXTRA_PASSIVE_ONLY, passiveOnly)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ConnectivityMonitorService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}

private fun io.github.cloolalang.notspotdetector.model.ConnectivityStats.statusLabel(
    context: Context
): String {
    return when (quality) {
        io.github.cloolalang.notspotdetector.model.ConnectionQuality.GOOD ->
            context.getString(R.string.quality_good)
        io.github.cloolalang.notspotdetector.model.ConnectionQuality.DEGRADED ->
            context.getString(R.string.quality_degraded)
        io.github.cloolalang.notspotdetector.model.ConnectionQuality.POOR ->
            context.getString(R.string.quality_poor)
        io.github.cloolalang.notspotdetector.model.ConnectionQuality.NO_CELLULAR ->
            context.getString(R.string.quality_no_cellular)
        io.github.cloolalang.notspotdetector.model.ConnectionQuality.PASSIVE_IDLE ->
            context.getString(R.string.quality_passive_idle)
        io.github.cloolalang.notspotdetector.model.ConnectionQuality.MONITORING_STOPPED ->
            context.getString(R.string.monitoring_stopped)
    }
}
