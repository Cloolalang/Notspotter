package io.github.cloolalang.notspotdetector.model

data class ThresholdSettings(
    val goodRttMs: Long = DEFAULT_GOOD_RTT_MS,
    val poorRttMs: Long = DEFAULT_POOR_RTT_MS,
    val poorJitterMs: Long = DEFAULT_POOR_JITTER_MS,
    val poorPacketLossPercent: Float = DEFAULT_POOR_PACKET_LOSS_PERCENT,
    val suppressClicksOnGoodConnection: Boolean = false,
    val goodConnectionClicksPerPing: Int = DEFAULT_GOOD_CONNECTION_CLICKS_PER_PING
) {
    fun normalized(): ThresholdSettings {
        val goodRtt = goodRttMs.coerceIn(MIN_GOOD_RTT_MS, MAX_GOOD_RTT_MS)
        val poorRtt = poorRttMs.coerceIn(
            maxOf(MIN_POOR_RTT_MS, goodRtt + MIN_RTT_GAP_MS),
            MAX_POOR_RTT_MS
        )
        return copy(
            goodRttMs = goodRtt,
            poorRttMs = poorRtt,
            poorJitterMs = poorJitterMs.coerceIn(MIN_POOR_JITTER_MS, MAX_POOR_JITTER_MS),
            poorPacketLossPercent = poorPacketLossPercent.coerceIn(
                MIN_POOR_PACKET_LOSS_PERCENT,
                MAX_POOR_PACKET_LOSS_PERCENT
            ),
            goodConnectionClicksPerPing = goodConnectionClicksPerPing.coerceIn(
                MIN_GOOD_CONNECTION_CLICKS_PER_PING,
                MAX_GOOD_CONNECTION_CLICKS_PER_PING
            )
        )
    }

    companion object {
        const val DEFAULT_GOOD_RTT_MS = 40L
        const val DEFAULT_POOR_RTT_MS = 80L
        const val DEFAULT_POOR_JITTER_MS = 60L
        const val DEFAULT_POOR_PACKET_LOSS_PERCENT = 5f
        const val DEFAULT_GOOD_CONNECTION_CLICKS_PER_PING = 1

        const val MIN_GOOD_RTT_MS = 20L
        const val MAX_GOOD_RTT_MS = 60L
        const val MIN_POOR_RTT_MS = 60L
        const val MAX_POOR_RTT_MS = 100L
        const val MIN_RTT_GAP_MS = 0L
        const val MIN_POOR_JITTER_MS = 20L
        const val MAX_POOR_JITTER_MS = 100L
        const val MIN_POOR_PACKET_LOSS_PERCENT = 0f
        const val MAX_POOR_PACKET_LOSS_PERCENT = 10f
        const val MIN_GOOD_CONNECTION_CLICKS_PER_PING = 1
        const val MAX_GOOD_CONNECTION_CLICKS_PER_PING = 10
    }
}

fun ConnectivityStats.severity(
    thresholds: ThresholdSettings,
    passiveSettings: PassiveSignalSettings = PassiveSignalSettings()
): Float {
    if (isPassiveIdleMode) return 0f
    if (isLimitedService) return 1f
    if (isOn2g && !monitor2gFallbackEnabled) return 1f
    if (!cellularAvailable) return 1f
    if (isPassiveOnlySession || (isOn2g && monitor2gFallbackEnabled)) {
        return signalOnlySeverity(passiveSettings)
    }
    if (rttMs == null && pingsSent > 0) return 1f

    val settings = thresholds.normalized()
    val rttRange = (settings.poorRttMs - settings.goodRttMs).coerceAtLeast(1L)

    val rttSeverity = rttMs?.let {
        ((it - settings.goodRttMs).toFloat() / rttRange).coerceIn(0f, 1f)
    } ?: 0f

    val lossSeverity = if (settings.poorPacketLossPercent <= 0f) {
        if (packetLossPercent > 0f) 1f else 0f
    } else {
        (packetLossPercent / settings.poorPacketLossPercent).coerceIn(0f, 1f)
    }

    val jitterSeverity = (jitterMs.toFloat() / settings.poorJitterMs).coerceIn(0f, 1f)

    return maxOf(rttSeverity, lossSeverity, jitterSeverity)
}

fun ConnectivityStats.withQuality(
    thresholds: ThresholdSettings,
    passiveSettings: PassiveSignalSettings = PassiveSignalSettings()
): ConnectivityStats {
    if (isMonitoring && !isPassiveIdleMode && isLimitedService) {
        return copy(quality = ConnectionQuality.POOR, severity = 1f)
    }

    if (isMonitoring && !isPassiveIdleMode && isOn2g && !monitor2gFallbackEnabled) {
        return copy(quality = ConnectionQuality.POOR, severity = 1f)
    }

    val sev = severity(thresholds, passiveSettings)
    return copy(
        quality = when {
            !isMonitoring -> ConnectionQuality.MONITORING_STOPPED
            isPassiveIdleMode -> ConnectionQuality.PASSIVE_IDLE
            !cellularAvailable -> ConnectionQuality.NO_CELLULAR
            sev < 0.25f -> ConnectionQuality.GOOD
            sev < 0.6f -> ConnectionQuality.DEGRADED
            else -> ConnectionQuality.POOR
        },
        severity = sev
    )
}

private fun ConnectivityStats.signalOnlySeverity(
    passiveSettings: PassiveSignalSettings
): Float {
    val tier = resolveSignalStrengthTier(passiveSettings) ?: return 0.1f
    return when (tier) {
        SignalStrengthTier.MILD -> 0.15f
        SignalStrengthTier.GOOD -> 0.3f
        SignalStrengthTier.FAIR -> 0.45f
        SignalStrengthTier.POOR -> 0.75f
        SignalStrengthTier.CRITICAL -> 0.95f
        SignalStrengthTier.G2_STRONG -> 0.25f
        SignalStrengthTier.G2_WEAK -> 0.8f
        SignalStrengthTier.DEADZONE -> 1f
    }
}

fun ConnectivityStats.shouldSuppressGeigerClicks(
    thresholds: ThresholdSettings,
    passiveSettings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!thresholds.suppressClicksOnGoodConnection) return false
    if (!isMonitoring || !cellularAvailable || shouldPlayFlatline(passiveSettings)) return false
    return quality == ConnectionQuality.GOOD
}

fun ConnectivityStats.hasExtremeLatency(): Boolean {
    return (rttMs != null && rttMs > EXTREME_LATENCY_MS) || jitterMs > EXTREME_LATENCY_MS
}

const val EXTREME_LATENCY_MS = 1_000L
