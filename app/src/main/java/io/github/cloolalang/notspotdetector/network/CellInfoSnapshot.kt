package io.github.cloolalang.notspotdetector.network

import android.annotation.SuppressLint
import android.os.Build
import android.os.SystemClock
import android.telephony.CellInfo
import android.telephony.TelephonyManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * [TelephonyManager.getAllCellInfo] is often a cached snapshot. Walk tests then show a frozen
 * RSRP/RSRQ while ages climb. [TelephonyManager.requestCellInfoUpdate] asks the modem for a
 * fresh list; this helper waits briefly and falls back to the cache.
 */
object CellInfoSnapshot {

    private const val REFRESH_WAIT_MS = 350L
    private const val REUSE_MS = 400L
    private val executor = Executors.newSingleThreadExecutor()
    private val lock = Any()

    @Volatile
    private var latest: List<CellInfo>? = null

    @Volatile
    private var latestAtElapsedMs = 0L

    @SuppressLint("MissingPermission")
    fun read(telephonyManager: TelephonyManager): List<CellInfo> {
        val fallback = readCached(telephonyManager)
        val now = SystemClock.elapsedRealtime()
        val reused = synchronized(lock) {
            val held = latest
            if (held != null && now - latestAtElapsedMs in 0 until REUSE_MS) held else null
        }
        if (reused != null) return preferFresher(reused, fallback)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return fallback

        val latch = CountDownLatch(1)
        var updated: List<CellInfo>? = null
        try {
            telephonyManager.requestCellInfoUpdate(
                executor,
                object : TelephonyManager.CellInfoCallback() {
                    override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                        val copy = cellInfo.toList()
                        synchronized(lock) {
                            latest = copy
                            latestAtElapsedMs = SystemClock.elapsedRealtime()
                        }
                        updated = copy
                        latch.countDown()
                    }

                    override fun onError(errorCode: Int, detail: Throwable?) {
                        latch.countDown()
                    }
                }
            )
        } catch (_: SecurityException) {
            return preferFresher(latest, fallback)
        } catch (_: RuntimeException) {
            return preferFresher(latest, fallback)
        }
        runCatching { latch.await(REFRESH_WAIT_MS, TimeUnit.MILLISECONDS) }
        return preferFresher(updated ?: latest, fallback)
    }

    fun newestAgeMs(cellInfoList: List<CellInfo>): Long? {
        return cellInfoList.mapNotNull(::ageMs).minOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun readCached(telephonyManager: TelephonyManager): List<CellInfo> {
        return try {
            telephonyManager.allCellInfo.orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        } catch (_: RuntimeException) {
            emptyList()
        }
    }

    private fun preferFresher(preferred: List<CellInfo>?, fallback: List<CellInfo>): List<CellInfo> {
        if (preferred.isNullOrEmpty()) return fallback
        if (fallback.isEmpty()) return preferred
        val preferredAge = newestAgeMs(preferred)
        val fallbackAge = newestAgeMs(fallback)
        return if (preferredAge != null && fallbackAge != null && preferredAge <= fallbackAge) {
            preferred
        } else if (preferredAge != null && fallbackAge == null) {
            preferred
        } else {
            fallback
        }
    }

    private fun ageMs(info: CellInfo): Long? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val timestampMs = info.timestampMillis
            if (timestampMs <= 0L) null else SystemClock.elapsedRealtime() - timestampMs
        } else {
            @Suppress("DEPRECATION")
            val timestampNs = info.timeStamp
            if (timestampNs <= 0L) {
                null
            } else {
                SystemClock.elapsedRealtime() - timestampNs / 1_000_000L
            }
        }
    }
}
