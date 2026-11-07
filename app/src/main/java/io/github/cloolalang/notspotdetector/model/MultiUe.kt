package io.github.cloolalang.notspotdetector.model

import java.util.UUID

enum class MultiUeRole {
    OFF,
    MASTER,
    SLAVE
}

enum class MultiUeLinkPhase {
    IDLE,
    CONNECTING,
    RETRYING,
    FAILED,
    MASTER_FULL
}

data class MultiUePeer(
    val address: String,
    val name: String
)

data class MultiUeLinkSnapshot(
    val role: MultiUeRole = MultiUeRole.OFF,
    val bluetoothAvailable: Boolean = true,
    val bluetoothEnabled: Boolean = false,
    val permissionGranted: Boolean = false,
    val localName: String = "",
    val connectedSlaves: List<MultiUePeer> = emptyList(),
    val connectedMaster: MultiUePeer? = null,
    val savedMasterAddress: String? = null,
    val paired: List<MultiUePeer> = emptyList(),
    val nearby: List<MultiUePeer> = emptyList(),
    val discovering: Boolean = false,
    val phase: MultiUeLinkPhase = MultiUeLinkPhase.IDLE,
    val masterSyncSettings: Boolean = false,
    val slaveSyncSettings: Boolean = false
) {
    /** Slave is linked, so monitoring start, stop, and active mode come from the master. */
    val followsMaster: Boolean
        get() = role == MultiUeRole.SLAVE && connectedMaster != null
}

enum class MultiUeRunMode {
    STOPPED,
    PASSIVE,
    ACTIVE
}

sealed interface MultiUeMessage {
    data object Hello : MultiUeMessage
    data class Passive(val on: Boolean) : MultiUeMessage
    data class Session(val mode: MultiUeRunMode) : MultiUeMessage
    data object Clear : MultiUeMessage
    data object ClearHistory : MultiUeMessage
    data class Panels(val open: Map<String, Boolean>) : MultiUeMessage
    data class Scroll(val fraction: Int) : MultiUeMessage
    data class Zone(val running: Boolean, val wholeTest: Boolean) : MultiUeMessage
    data class ZoneName(val suffix: String) : MultiUeMessage
    data object Full : MultiUeMessage
    data class Sync(val on: Boolean) : MultiUeMessage
    data class Settings(val payload: String) : MultiUeMessage
}

object MultiUeProtocol {
    const val MAX_SLAVES = 4
    const val SCROLL_SCALE = 10_000
    const val SERVICE_NAME = "Notspotter Multi-UE"
    val SERVICE_UUID: UUID = UUID.fromString("6f0e2a44-8b1d-4c3e-9f57-1a2b3c4d5e6f")

    fun hello(): String = "HELLO 1"

    fun passive(on: Boolean): String = if (on) "PASSIVE 1" else "PASSIVE 0"

    fun session(mode: MultiUeRunMode): String = when (mode) {
        MultiUeRunMode.STOPPED -> "SESSION 0"
        MultiUeRunMode.PASSIVE -> "SESSION P"
        MultiUeRunMode.ACTIVE -> "SESSION A"
    }

    fun clearGraphs(): String = "CLEAR"

    fun clearHistory(): String = "HISTORY"

    fun zone(running: Boolean, wholeTest: Boolean): String {
        return "ZONE ${if (running) 1 else 0} ${if (wholeTest) 1 else 0}"
    }

    fun zoneName(suffix: String): String {
        val safe = suffix.replace("\r", " ").replace("\n", " ")
        return ZONE_NAME_PREFIX + safe
    }

    fun panels(open: Map<String, Boolean>): String {
        if (open.isEmpty()) return "PANELS -"
        val body = open.entries
            .sortedBy { it.key }
            .joinToString(",") { (key, value) -> "$key=${if (value) "1" else "0"}" }
        return "PANELS $body"
    }

    fun scroll(fraction: Int): String = "SCROLL ${fraction.coerceIn(0, SCROLL_SCALE)}"

    fun full(): String = "FULL"

    fun sync(on: Boolean): String = if (on) "SYNC 1" else "SYNC 0"

    fun settings(payload: String): String = "SETTINGS $payload"

    fun parse(line: String): MultiUeMessage? {
        val trimmed = line.trim()
        if (trimmed.startsWith(SETTINGS_PREFIX)) {
            val payload = trimmed.removePrefix(SETTINGS_PREFIX).trim()
            return payload.takeIf { it.isNotEmpty() }?.let(MultiUeMessage::Settings)
        }
        if (trimmed.startsWith(PANELS_PREFIX)) {
            return parsePanels(trimmed.removePrefix(PANELS_PREFIX).trim())?.let(MultiUeMessage::Panels)
        }
        if (trimmed == "ZONENAME" || trimmed.startsWith(ZONE_NAME_PREFIX)) {
            val suffix = if (trimmed == "ZONENAME") "" else trimmed.removePrefix(ZONE_NAME_PREFIX)
            return MultiUeMessage.ZoneName(suffix)
        }
        val parts = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when (parts.firstOrNull()) {
            "HELLO" -> MultiUeMessage.Hello
            "PASSIVE" -> when (parts.getOrNull(1)) {
                "1" -> MultiUeMessage.Passive(on = true)
                "0" -> MultiUeMessage.Passive(on = false)
                else -> null
            }
            "SESSION" -> when (parts.getOrNull(1)) {
                "0" -> MultiUeMessage.Session(MultiUeRunMode.STOPPED)
                "P" -> MultiUeMessage.Session(MultiUeRunMode.PASSIVE)
                "A" -> MultiUeMessage.Session(MultiUeRunMode.ACTIVE)
                else -> null
            }
            "CLEAR" -> MultiUeMessage.Clear
            "HISTORY" -> MultiUeMessage.ClearHistory
            "ZONE" -> {
                val running = parts.getOrNull(1)
                val wholeTest = parts.getOrNull(2)
                if ((running == "0" || running == "1") && (wholeTest == "0" || wholeTest == "1")) {
                    MultiUeMessage.Zone(running == "1", wholeTest == "1")
                } else {
                    null
                }
            }
            "SCROLL" -> parts.getOrNull(1)?.toIntOrNull()
                ?.takeIf { it in 0..SCROLL_SCALE }
                ?.let(MultiUeMessage::Scroll)
            "SYNC" -> when (parts.getOrNull(1)) {
                "1" -> MultiUeMessage.Sync(on = true)
                "0" -> MultiUeMessage.Sync(on = false)
                else -> null
            }
            "FULL" -> MultiUeMessage.Full
            else -> null
        }
    }

    private fun parsePanels(body: String): Map<String, Boolean>? {
        if (body == "-") return emptyMap()
        if (body.isEmpty()) return null
        val open = linkedMapOf<String, Boolean>()
        for (part in body.split(',')) {
            val bits = part.split('=')
            if (bits.size != 2 || !PANEL_KEY.matches(bits[0])) return null
            val value = when (bits[1]) {
                "1" -> true
                "0" -> false
                else -> return null
            }
            open[bits[0]] = value
        }
        return open
    }

    private const val SETTINGS_PREFIX = "SETTINGS "
    private const val PANELS_PREFIX = "PANELS "
    private const val ZONE_NAME_PREFIX = "ZONENAME "
    private val PANEL_KEY = Regex("[a-z0-9-]+")
}

/**
 * What a slave should do with one passive on/off command.
 * [stopFirst] then [startPassive] switches an active-mode session over to passive monitoring.
 */
data class MultiUePassivePlan(
    val stopFirst: Boolean = false,
    val startPassive: Boolean = false,
    val stop: Boolean = false
)

fun multiUePassivePlan(
    isRunning: Boolean,
    passiveSession: Boolean,
    commandOn: Boolean
): MultiUePassivePlan {
    return when {
        commandOn && isRunning && passiveSession -> MultiUePassivePlan()
        commandOn && isRunning -> MultiUePassivePlan(stopFirst = true, startPassive = true)
        commandOn -> MultiUePassivePlan(startPassive = true)
        isRunning -> MultiUePassivePlan(stop = true)
        else -> MultiUePassivePlan()
    }
}

/**
 * What a slave should do with one monitoring command from the master.
 * Switching between passive and active stops the running session, then starts the other.
 */
data class MultiUeRunPlan(
    val stopFirst: Boolean = false,
    val startPassive: Boolean = false,
    val startActive: Boolean = false
)

fun multiUeRunPlan(
    isRunning: Boolean,
    isMonitoring: Boolean,
    passiveSession: Boolean,
    command: MultiUeRunMode
): MultiUeRunPlan {
    if (command == MultiUeRunMode.STOPPED) {
        return if (isRunning) MultiUeRunPlan(stopFirst = true) else MultiUeRunPlan()
    }
    val current = when {
        isRunning && isMonitoring && passiveSession -> MultiUeRunMode.PASSIVE
        isRunning && isMonitoring -> MultiUeRunMode.ACTIVE
        else -> MultiUeRunMode.STOPPED
    }
    if (current == command) return MultiUeRunPlan()
    return when (command) {
        MultiUeRunMode.PASSIVE -> MultiUeRunPlan(stopFirst = isRunning, startPassive = true)
        MultiUeRunMode.ACTIVE -> MultiUeRunPlan(stopFirst = isRunning, startActive = true)
        MultiUeRunMode.STOPPED -> MultiUeRunPlan(stopFirst = isRunning)
    }
}
