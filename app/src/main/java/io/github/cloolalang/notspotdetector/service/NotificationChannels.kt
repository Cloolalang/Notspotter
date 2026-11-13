package io.github.cloolalang.notspotdetector.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import io.github.cloolalang.notspotdetector.R

object NotificationChannels {
    const val MONITOR_CHANNEL_ID = "connectivity_monitor"
    const val MULTI_UE_CHANNEL_ID = "multi_ue_link"
    const val CALL_TEST_CHANNEL_ID = "call_test"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            MONITOR_CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(false)
        }

        val multiUe = NotificationChannel(
            MULTI_UE_CHANNEL_ID,
            context.getString(R.string.notification_channel_multi_ue),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_multi_ue_description)
            setShowBadge(false)
        }

        val callTest = NotificationChannel(
            CALL_TEST_CHANNEL_ID,
            context.getString(R.string.notification_channel_call_test),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_call_test_description)
            setShowBadge(false)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        manager.createNotificationChannel(multiUe)
        manager.createNotificationChannel(callTest)
    }
}
