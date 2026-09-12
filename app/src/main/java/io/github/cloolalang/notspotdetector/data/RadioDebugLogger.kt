package io.github.cloolalang.notspotdetector.data

import android.content.Context
import io.github.cloolalang.notspotdetector.model.RadioDebugSnapshot
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Rolling on-device radio log for drive tests. Dedupes identical consecutive samples,
 * but writes a heartbeat so a stuck-wrong EARFCN still has timestamps.
 */
class RadioDebugLogger(context: Context) {

    private val appContext = context.applicationContext
    private val logDir = File(appContext.filesDir, LOG_DIR_NAME)
    private val logFile = File(logDir, LOG_FILE_NAME)
    private val lock = Any()
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.UK)

    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _lastSnapshot = MutableStateFlow<RadioDebugSnapshot?>(null)
    val lastSnapshot: StateFlow<RadioDebugSnapshot?> = _lastSnapshot.asStateFlow()

    private val _lineCount = MutableStateFlow(0)
    val lineCount: StateFlow<Int> = _lineCount.asStateFlow()

    @Volatile
    private var lastBody: String? = null
    @Volatile
    private var lastWriteElapsedMs: Long = 0L

    init {
        logDir.mkdirs()
        _lineCount.value = countLines()
    }

    val isEnabled: Boolean
        get() = _enabled.value

    fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
        if (enabled) {
            instance = this
            synchronized(lock) {
                appendUnlocked("# radio debug enabled ${timeFormat.format(Date())}")
            }
        } else if (instance === this) {
            synchronized(lock) {
                appendUnlocked("# radio debug disabled ${timeFormat.format(Date())}")
            }
        }
    }

    fun record(snapshot: RadioDebugSnapshot) {
        if (!_enabled.value) return
        _lastSnapshot.value = snapshot
        val body = snapshot.bodyForDedupe()
        val now = System.currentTimeMillis()
        synchronized(lock) {
            val same = body == lastBody
            if (same && now - lastWriteElapsedMs < HEARTBEAT_MS) {
                return
            }
            val prefix = if (same) "still " else ""
            appendUnlocked(prefix + snapshot.logLine(timeFormat.format(Date())))
            lastBody = body
            lastWriteElapsedMs = now
        }
    }

    fun clear() {
        synchronized(lock) {
            if (logFile.exists()) {
                logFile.writeText("")
            }
            lastBody = null
            lastWriteElapsedMs = 0L
            _lineCount.value = 0
        }
        _lastSnapshot.value = null
    }

    fun fileForShare(): File? {
        synchronized(lock) {
            if (!logFile.exists() || logFile.length() == 0L) return null
            return logFile
        }
    }

    private fun appendUnlocked(line: String) {
        logDir.mkdirs()
        logFile.appendText(line + "\n")
        trimIfNeededUnlocked()
        _lineCount.value = countLines()
    }

    private fun trimIfNeededUnlocked() {
        if (!logFile.exists() || logFile.length() <= MAX_FILE_BYTES) return
        val text = logFile.readText()
        val keepFrom = (text.length - KEEP_FILE_BYTES).coerceAtLeast(0)
        val trimmed = text.substring(keepFrom).substringAfter('\n', text)
        logFile.writeText(trimmed)
    }

    private fun countLines(): Int {
        if (!logFile.exists()) return 0
        return logFile.useLines { it.count() }
    }

    companion object {
        const val LOG_DIR_NAME = "radio-debug"
        const val LOG_FILE_NAME = "radio-debug.txt"
        const val SHARE_MIME_TYPE = "text/plain"
        private const val HEARTBEAT_MS = 15_000L
        private const val MAX_FILE_BYTES = 400_000L
        private const val KEEP_FILE_BYTES = 200_000

        @Volatile
        var instance: RadioDebugLogger? = null
            private set

        fun isEnabled(): Boolean = instance?.isEnabled == true

        fun record(snapshot: RadioDebugSnapshot) {
            instance?.record(snapshot)
        }
    }
}
