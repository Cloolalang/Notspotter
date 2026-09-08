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
import io.github.cloolalang.notspotdetector.audio.CellVoiceAnnouncer
import io.github.cloolalang.notspotdetector.audio.GeigerCounterPlayer
import io.github.cloolalang.notspotdetector.data.AudioVolumeSettingsRepository
import io.github.cloolalang.notspotdetector.data.PassiveMockSettingsRepository
import io.github.cloolalang.notspotdetector.data.PassiveSignalSettingsRepository
import io.github.cloolalang.notspotdetector.data.MonitoringSettingsRepository
import io.github.cloolalang.notspotdetector.data.PingSettingsRepository
import io.github.cloolalang.notspotdetector.data.ThresholdSettingsRepository
import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.SignalStateAnnouncement
import io.github.cloolalang.notspotdetector.model.shouldPlayFlatline
import io.github.cloolalang.notspotdetector.model.shouldPlayPassiveSignalAndQualityAlerts
import io.github.cloolalang.notspotdetector.model.usesG2SignalTiers
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
    private var searching2gAnnouncementJob: Job? = null

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
        cellVoiceAnnouncer = CellVoiceAnnouncer(this)
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
        MonitorState.setRunning(true)
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
        val events = MonitorState.updateStats(stats)
        val passiveSettings = MonitorState.passiveSignalSettings.value
        val monitoringSettings = MonitorState.monitoringSettings.value
        val playQualityAlerts = MonitorState.stats.value.shouldPlayPassiveSignalAndQualityAlerts(
            monitoringSettings,
            passiveSettings
        )
        if (events.cellIdentityChanged && playQualityAlerts) {
            playCellChangeAlert(events.cellChangeAnnouncement)
        }
        val stats = MonitorState.stats.value
        if (events.radioTechnologyChanged && (
                playQualityAlerts ||
                    (
                        isPassiveOnlyStart &&
                            stats.noSignalActive &&
                            monitoringSettings.monitor2gFallback
                        )
                )
        ) {
            playTechnologyChangeAlert(events.technologyChangeAnnouncement)
        }
        if (events.limitedServiceStateChanged) {
            playLimitedServiceAlert(events.limitedServiceStateAnnouncement)
        }
        if (events.limitedServiceOperatorChanged) {
            playLimitedServiceAlert(events.limitedServiceOperatorChangeAnnouncement)
        }
        if (events.noSignalStateChanged) {
            playNoSignalAlert(events.noSignalStateAnnouncement)
        }
        if (events.g2FallbackAnnounced) {
            playG2FallbackAlert(events.g2FallbackAnnouncement)
        }
        if (events.deadzoneAnnounced) {
            playDeadzoneAlert(events.deadzoneAnnouncement)
        }
        if (events.searching2gStateEntered) {
            scheduleSearching2gAnnouncement()
        } else if (!stats.noSignalActive || stats.isOn2g || stats.isCompleteNoService) {
            searching2gAnnouncementJob?.cancel()
            searching2gAnnouncementJob = null
        }
        updateNoSignalPeriodicAnnouncements()
        updateG2ModePeriodicAnnouncements()
        updateLimitedServicePeriodicAnnouncements()
        updateDeadzonePeriodicAnnouncements()
        updateNotification(MonitorState.stats.value.statusLabel(this))
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

    private fun updateNoSignalPeriodicAnnouncements() {
        val stats = MonitorState.stats.value
        val passiveSettings = MonitorState.passiveSignalSettings.value
        val shouldAnnounce = stats.isMonitoring &&
            !stats.isPassiveIdleMode &&
            stats.shouldPlayFlatline(passiveSettings) &&
            !stats.usesG2SignalTiers() &&
            !stats.isCompleteNoService

        if (!shouldAnnounce) {
            noSignalPeriodicAnnouncementJob?.cancel()
            noSignalPeriodicAnnouncementJob = null
            return
        }

        if (noSignalPeriodicAnnouncementJob?.isActive == true) return

        noSignalPeriodicAnnouncementJob = serviceScope.launch {
            while (isActive) {
                delay(PERIODIC_ANNOUNCEMENT_MS)
                val current = MonitorState.stats.value
                val settings = MonitorState.passiveSignalSettings.value
                if (!current.isMonitoring ||
                    current.isPassiveIdleMode ||
                    !current.shouldPlayFlatline(settings) ||
                    current.usesG2SignalTiers() ||
                    current.isCompleteNoService
                ) {
                    break
                }
                playNoSignalAlert(MonitorState.formatNoSignalAnnouncement(current))
            }
        }
    }

    private fun updateG2ModePeriodicAnnouncements() {
        val stats = MonitorState.stats.value
        val passiveSettings = MonitorState.passiveSignalSettings.value
        val shouldAnnounce = stats.isMonitoring &&
            !stats.isPassiveIdleMode &&
            stats.usesG2SignalTiers()

        if (!shouldAnnounce) {
            g2ModePeriodicAnnouncementJob?.cancel()
            g2ModePeriodicAnnouncementJob = null
            return
        }

        if (g2ModePeriodicAnnouncementJob?.isActive == true) return

        g2ModePeriodicAnnouncementJob = serviceScope.launch {
            while (isActive) {
                delay(PERIODIC_ANNOUNCEMENT_MS)
                val current = MonitorState.stats.value
                val settings = MonitorState.passiveSignalSettings.value
                if (!current.isMonitoring ||
                    current.isPassiveIdleMode ||
                    !current.usesG2SignalTiers()
                ) {
                    break
                }
                if (current.shouldPlayFlatline(settings)) {
                    playNoSignalAlert(MonitorState.formatNoSignalAnnouncement(current))
                } else {
                    playG2FallbackAlert(
                        SignalStateAnnouncement.formatG2CampedAnnouncement(current.networkOperatorName)
                    )
                }
            }
        }
    }

    private fun updateLimitedServicePeriodicAnnouncements() {
        val stats = MonitorState.stats.value
        val shouldAnnounce = stats.isMonitoring &&
            !stats.isPassiveIdleMode &&
            stats.isLimitedService

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
                if (!current.isMonitoring ||
                    current.isPassiveIdleMode ||
                    !current.isLimitedService
                ) {
                    break
                }
                playLimitedServiceAlert(MonitorState.formatLimitedServiceAnnouncement(current))
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
                playDeadzoneAlert(MonitorState.formatDeadzoneAnnouncement(current))
            }
        }
    }

    private fun playAlertWithVoice(
        onPlayTone: () -> Unit,
        toneDurationMs: Int,
        announcement: String?,
        voiceEnabled: Boolean,
        voiceVolume: Float
    ) {
        onPlayTone()
        if (!voiceEnabled || announcement.isNullOrBlank() || voiceVolume <= 0f) return
        serviceScope.launch {
            delay(AudioVolumeSettings.voiceDelayAfterAlertTone(toneDurationMs))
            cellVoiceAnnouncer.speak(announcement, voiceVolume)
        }
    }

    private fun playCellChangeAlert(announcement: String?) {
        val volumes = MonitorState.audioVolumes.value.normalized()
        playAlertWithVoice(
            onPlayTone = ::playCellChangeBell,
            toneDurationMs = GeigerCounterPlayer.CELL_CHANGE_BELL_DURATION_MS,
            announcement = announcement,
            voiceEnabled = volumes.cellChangeVoiceEnabled,
            voiceVolume = volumes.cellChangeVoiceVolume
        )
    }

    private fun playTechnologyChangeAlert(announcement: String?) {
        val volumes = MonitorState.audioVolumes.value.normalized()
        playAlertWithVoice(
            onPlayTone = ::playTechnologyChangeTone,
            toneDurationMs = GeigerCounterPlayer.TECHNOLOGY_CHANGE_TONE_DURATION_MS,
            announcement = announcement,
            voiceEnabled = volumes.technologyChangeVoiceEnabled,
            voiceVolume = volumes.technologyChangeVoiceVolume
        )
    }

    private fun playNoSignalAlert(announcement: String?) {
        val volumes = MonitorState.audioVolumes.value.normalized()
        playAlertWithVoice(
            onPlayTone = {
                geigerPlayer.previewNoSignalTone(volumes.noSignalToneVolume)
            },
            toneDurationMs = GeigerCounterPlayer.NO_SIGNAL_ALERT_TONE_DURATION_MS,
            announcement = announcement,
            voiceEnabled = volumes.noSignalVoiceEnabled,
            voiceVolume = volumes.noSignalVoiceVolume
        )
    }

    private fun playLimitedServiceAlert(announcement: String?) {
        val volumes = MonitorState.audioVolumes.value.normalized()
        playAlertWithVoice(
            onPlayTone = {
                geigerPlayer.previewLimitedServiceTone(volumes.limitedServiceToneVolume)
            },
            toneDurationMs = GeigerCounterPlayer.LIMITED_SERVICE_ALERT_TONE_DURATION_MS,
            announcement = announcement,
            voiceEnabled = volumes.limitedServiceVoiceEnabled,
            voiceVolume = volumes.limitedServiceVoiceVolume
        )
    }

    private fun playG2FallbackAlert(announcement: String?) {
        val volumes = MonitorState.audioVolumes.value.normalized()
        playAlertWithVoice(
            onPlayTone = ::playTechnologyChangeTone,
            toneDurationMs = GeigerCounterPlayer.TECHNOLOGY_CHANGE_TONE_DURATION_MS,
            announcement = announcement,
            voiceEnabled = volumes.technologyChangeVoiceEnabled,
            voiceVolume = volumes.technologyChangeVoiceVolume
        )
    }

    private fun playDeadzoneAlert(announcement: String?) {
        val volumes = MonitorState.audioVolumes.value.normalized()
        playAlertWithVoice(
            onPlayTone = {
                geigerPlayer.previewNoSignalTone(volumes.noSignalToneVolume)
            },
            toneDurationMs = GeigerCounterPlayer.NO_SIGNAL_ALERT_TONE_DURATION_MS,
            announcement = announcement,
            voiceEnabled = volumes.noSignalVoiceEnabled,
            voiceVolume = volumes.noSignalVoiceVolume
        )
    }

    private fun playCellChangeBell() {
        geigerPlayer.playCellChangeBell(
            MonitorState.audioVolumes.value.normalized().cellChangeBellVolume
        )
    }

    private fun playTechnologyChangeTone() {
        geigerPlayer.playTechnologyChangeTone(
            MonitorState.audioVolumes.value.normalized().technologyChangeVolume
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
        searching2gAnnouncementJob?.cancel()
        searching2gAnnouncementJob = null
        pingMonitor.stop()
        passiveSignalMonitor.stop()
        geigerPlayer.stop()
        cellVoiceAnnouncer.shutdown()
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
            acquire()
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
        private const val SEARCHING_2G_ANNOUNCEMENT_DELAY_MS = 5_000L

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
