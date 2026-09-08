package io.github.cloolalang.notspotdetector.data

import android.content.Context
import io.github.cloolalang.notspotdetector.model.ThresholdSettings

class ThresholdSettingsRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): ThresholdSettings {
        return ThresholdSettings(
            goodRttMs = prefs.getLong(KEY_GOOD_RTT, ThresholdSettings.DEFAULT_GOOD_RTT_MS),
            poorRttMs = prefs.getLong(KEY_POOR_RTT, ThresholdSettings.DEFAULT_POOR_RTT_MS),
            poorJitterMs = prefs.getLong(KEY_POOR_JITTER, ThresholdSettings.DEFAULT_POOR_JITTER_MS),
            poorPacketLossPercent = prefs.getFloat(
                KEY_POOR_PACKET_LOSS,
                ThresholdSettings.DEFAULT_POOR_PACKET_LOSS_PERCENT
            ),
            suppressClicksOnGoodConnection = prefs.getBoolean(
                KEY_SUPPRESS_CLICKS_ON_GOOD,
                false
            ),
            goodConnectionClicksPerPing = prefs.getInt(
                KEY_GOOD_CLICKS_PER_PING,
                ThresholdSettings.DEFAULT_GOOD_CONNECTION_CLICKS_PER_PING
            )
        ).normalized()
    }

    fun save(settings: ThresholdSettings) {
        val normalized = settings.normalized()
        prefs.edit()
            .putLong(KEY_GOOD_RTT, normalized.goodRttMs)
            .putLong(KEY_POOR_RTT, normalized.poorRttMs)
            .putLong(KEY_POOR_JITTER, normalized.poorJitterMs)
            .putFloat(KEY_POOR_PACKET_LOSS, normalized.poorPacketLossPercent)
            .putBoolean(KEY_SUPPRESS_CLICKS_ON_GOOD, normalized.suppressClicksOnGoodConnection)
            .putInt(KEY_GOOD_CLICKS_PER_PING, normalized.goodConnectionClicksPerPing)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "notspot_thresholds"
        private const val KEY_GOOD_RTT = "good_rtt_ms"
        private const val KEY_POOR_RTT = "poor_rtt_ms"
        private const val KEY_POOR_JITTER = "poor_jitter_ms"
        private const val KEY_POOR_PACKET_LOSS = "poor_packet_loss_percent"
        private const val KEY_SUPPRESS_CLICKS_ON_GOOD = "suppress_clicks_on_good"
        private const val KEY_GOOD_CLICKS_PER_PING = "good_clicks_per_ping"
    }
}
