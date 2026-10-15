package io.github.cloolalang.notspotdetector.model

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToLong

/**
 * Ping clicks run beside RXSS pulses. A good RTT, 45 ms and faster, clicks once per ping test.
 * Above that the gap shortens gradually. On a 5 second test that is about 2.5 seconds at 60 ms,
 * 1.25 seconds at 80 ms, 0.8 seconds at 100 ms, and 0.5 seconds at 120 ms, then faster until
 * about 10 clicks a second. During a ping test the only silence is 100% packet loss.
 */
object RttGeiger {
    /** At or below this RTT, click once per ping test. */
    const val ONCE_PER_TEST_RTT_MS = 45L
    /** About 10 clicks a second from RTT alone. Loss can click faster than this. */
    const val FAST_INTERVAL_MS = 100L
    /** At or above this loss, click speed doubles. */
    const val LOSS_DOUBLE_PERCENT = 5f
    /** At or above this loss, click speed doubles again. 100% loss is silence. */
    const val LOSS_DOUBLE_AGAIN_PERCENT = 10f

    /**
     * RTT that drives the click rate. Packet loss does not change this dose; it multiplies the
     * finished click speed. 100% loss produces no dose. Any other ping sample does.
     */
    fun doseMs(
        rttMs: Long?,
        packetLossPercent: Float,
        pingsSent: Int
    ): Long? {
        if (packetLossPercent >= 100f) return null
        if (rttMs == null && pingsSent <= 0) return null
        return rttMs ?: ONCE_PER_TEST_RTT_MS
    }

    /** 1 below 5% loss, 2 from 5%, 4 from 10%. */
    fun lossSpeedMultiplier(packetLossPercent: Float): Int {
        return when {
            packetLossPercent >= LOSS_DOUBLE_AGAIN_PERCENT -> 4
            packetLossPercent >= LOSS_DOUBLE_PERCENT -> 2
            else -> 1
        }
    }

    /** True when this RTT should tick about once per ping test, before loss speeds it up. */
    fun clicksOncePerTest(doseMs: Long): Boolean = doseMs <= ONCE_PER_TEST_RTT_MS

    /**
     * Milliseconds between clicks for [doseMs] when each ping test lasts [testIntervalMs].
     * At or below [ONCE_PER_TEST_RTT_MS] the gap matches the test. Above that the gap follows
     * the latency knots, scaled so a longer test keeps the same shape.
     */
    fun intervalMs(doseMs: Long, testIntervalMs: Long): Long {
        val testGap = testIntervalMs.coerceAtLeast(1L)
        if (doseMs <= ONCE_PER_TEST_RTT_MS) return testGap
        val scale = testGap / REFERENCE_TEST_MS
        val shaped = shapedIntervalMs(doseMs) * scale
        return shaped.roundToLong().coerceIn(FAST_INTERVAL_MS, testGap)
    }

    /**
     * Click gap on a 5 second test. Knots are the latencies where the rate should be obvious:
     * half the test at 60 ms, a quarter at 80 ms, then 0.8 s and 0.5 s. Past 120 ms the same
     * step continues until [FAST_INTERVAL_MS].
     */
    private fun shapedIntervalMs(doseMs: Long): Double {
        val last = curveRttMs.lastIndex
        if (doseMs >= curveRttMs[last]) {
            val span = (curveRttMs[last] - curveRttMs[last - 1]).toDouble()
            val ratio = curveIntervalMs[last] / curveIntervalMs[last - 1]
            val extra = (doseMs - curveRttMs[last]).toDouble()
            return curveIntervalMs[last] * exp(ln(ratio) / span * extra)
        }
        var segment = 0
        while (segment < last - 1 && doseMs >= curveRttMs[segment + 1]) segment++
        val span = (curveRttMs[segment + 1] - curveRttMs[segment]).toDouble()
        val t = (doseMs - curveRttMs[segment]) / span
        val logInterval = ln(curveIntervalMs[segment]) +
            t * (ln(curveIntervalMs[segment + 1]) - ln(curveIntervalMs[segment]))
        return exp(logInterval)
    }

    /**
     * Gap actually played. Loss of at least 5% halves it, and at least 10% halves it again.
     */
    fun clickIntervalMs(doseMs: Long, packetLossPercent: Float, testIntervalMs: Long): Long {
        val base = intervalMs(doseMs, testIntervalMs)
        return (base / lossSpeedMultiplier(packetLossPercent)).coerceAtLeast(1L)
    }

    /** Knots below are the gaps for a 5 second ping test, then scaled to the real interval. */
    private const val REFERENCE_TEST_MS = 5_000.0
    private val curveRttMs = longArrayOf(45L, 60L, 80L, 100L, 120L)
    private val curveIntervalMs = doubleArrayOf(5_000.0, 2_500.0, 1_250.0, 800.0, 500.0)
}

fun ConnectivityStats.shouldPlayRttGeiger(): Boolean {
    if (!isMonitoring || isPassiveIdleMode || isPassiveOnlySession) return false
    if (packetLossPercent >= 100f) return false
    return RttGeiger.doseMs(rttMs, packetLossPercent, pingsSent) != null
}
