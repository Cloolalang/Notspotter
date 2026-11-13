package io.github.cloolalang.notspotdetector.service

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
import io.github.cloolalang.notspotdetector.R

/**
 * Keeps the process awake while the in-call screen is in front, so the call loop can finish
 * the duration and the gap before the next call.
 */
class CallTestService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationChannels.create(this)
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        if (intent?.action == ACTION_STOP) {
            stopKeepAlive()
            return START_NOT_STICKY
        }
        acquireWakeLock()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    private fun stopKeepAlive() {
        releaseWakeLock()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "notspotdetector:call-test"
        ).apply {
            setReferenceCounted(false)
            acquire(MAX_WAKE_LOCK_MS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) lock.release()
        }
        wakeLock = null
    }

    private fun buildNotification() = NotificationCompat.Builder(this, NotificationChannels.CALL_TEST_CHANNEL_ID)
        .setContentTitle(getString(R.string.notification_call_test_title))
        .setContentText(getString(R.string.notification_call_test_text))
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentIntent(openAppIntent())
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .build()

    private fun openAppIntent(): PendingIntent {
        val open = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_STOP = "io.github.cloolalang.notspotdetector.action.STOP_CALL_TEST"
        private const val NOTIFICATION_ID = 1003
        private const val MAX_WAKE_LOCK_MS = 2 * 60 * 60 * 1000L

        fun start(context: Context) {
            val intent = Intent(context, CallTestService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallTestService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
