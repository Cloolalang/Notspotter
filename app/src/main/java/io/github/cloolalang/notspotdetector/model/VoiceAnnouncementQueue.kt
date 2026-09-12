package io.github.cloolalang.notspotdetector.model

/**
 * Immediate VA queue. Same-kind phrases replace each other, service-state VAs
 * (dead zone / no-signal / limited) collapse to the latest, and spoken backlog is
 * kept to about [SPEECH_BUDGET_MS]. Overflow events keep their alert tone/bell
 * and drop TTS so reselections stay audible in low signal. Known-cell voice
 * (VA-19) is always kept — it has no bell of its own.
 */
class VoiceAnnouncementQueue {

    data class Item(
        val id: Long,
        val announcement: MonitoringAnnouncement
    )

    data class EnqueueResult(
        val interruptCurrent: Boolean = false,
        val supersededServiceState: Boolean = false,
        val toneOnly: List<MonitoringAnnouncement> = emptyList()
    )

    private val lock = Any()
    private val pending = ArrayDeque<Item>()
    private var playing: Item? = null
    private var nextId = 1L

    fun enqueue(announcements: List<MonitoringAnnouncement>): EnqueueResult {
        if (announcements.isEmpty()) return EnqueueResult()
        synchronized(lock) {
            var interruptCurrent = false
            var supersededServiceState = false
            val toneOnly = mutableListOf<MonitoringAnnouncement>()
            for (announcement in announcements) {
                if (announcement.message.isNullOrBlank()) continue
                val presentKinds = presentKindsLocked()
                if (shouldDropIncoming(announcement.kind, presentKinds)) continue

                val current = playing
                if (current != null &&
                    current.announcement.kind == MonitoringAnnouncementKind.CELL_IDENTITY &&
                    announcement.kind == MonitoringAnnouncementKind.CELL_IDENTITY
                ) {
                    interruptCurrent = true
                    playing = null
                    toneOnly += announcement
                    continue
                }

                pending.removeAll { existing ->
                    val drop = shouldSupersede(existing.announcement.kind, announcement.kind)
                    if (drop &&
                        existing.announcement.kind != announcement.kind &&
                        hasSoundIcon(existing.announcement.kind)
                    ) {
                        toneOnly += existing.announcement
                    }
                    drop
                }
                if (current != null && shouldInterruptPlaying(current.announcement.kind, announcement.kind)) {
                    interruptCurrent = true
                    playing = null
                }
                if (announcement.kind in SERVICE_STATE_KINDS) {
                    supersededServiceState = true
                }
                pending.addLast(Item(nextId++, announcement))
            }
            sortPendingLocked()
            applySpeechBudgetLocked(toneOnly)
            return EnqueueResult(
                interruptCurrent = interruptCurrent,
                supersededServiceState = supersededServiceState,
                toneOnly = toneOnly.toList()
            )
        }
    }

    fun startNext(): Item? = synchronized(lock) {
        if (playing != null) return@synchronized playing
        val next = pending.removeFirstOrNull() ?: return@synchronized null
        playing = next
        next
    }

    fun markFinished(id: Long) {
        synchronized(lock) {
            if (playing?.id == id) {
                playing = null
            }
        }
    }

    fun isPlaying(id: Long): Boolean = synchronized(lock) { playing?.id == id }

    fun clear() {
        synchronized(lock) {
            pending.clear()
            playing = null
        }
    }

    fun pendingKinds(): List<MonitoringAnnouncementKind> = synchronized(lock) {
        pending.map { it.announcement.kind }
    }

    fun pendingMessages(): List<String?> = synchronized(lock) {
        pending.map { it.announcement.message }
    }

    private fun presentKindsLocked(): List<MonitoringAnnouncementKind> {
        return buildList {
            playing?.let { add(it.announcement.kind) }
            addAll(pending.map { it.announcement.kind })
        }
    }

    private fun sortPendingLocked() {
        val sorted = pending.sortedWith(
            compareBy<Item> { it.announcement.kind.playbackPriority }
                .thenBy { VoiceAnnouncement.immediatePlaybackOrder.indexOf(it.announcement.kind) }
        )
        pending.clear()
        pending.addAll(sorted)
    }

    private fun applySpeechBudgetLocked(toneOnly: MutableList<MonitoringAnnouncement>) {
        var remaining = SPEECH_BUDGET_MS - remainingPlayingMsLocked()
        val keep = ArrayDeque<Item>()
        for (item in pending) {
            val kind = item.announcement.kind
            val cost = estimatedSpokenMs(kind)
            if (kind == MonitoringAnnouncementKind.SPECIAL_CELL || cost <= remaining) {
                keep.addLast(item)
                remaining -= cost
            } else if (hasSoundIcon(kind)) {
                toneOnly += item.announcement
            }
        }
        pending.clear()
        pending.addAll(keep)
    }

    private fun remainingPlayingMsLocked(): Long {
        val current = playing ?: return 0L
        return estimatedSpokenMs(current.announcement.kind)
    }

    companion object {
        const val SPEECH_BUDGET_MS = 5_000L
        const val ESTIMATED_SPEECH_MS = 3_000L
        private const val ALERT_VOICE_GAP_MS = 50L

        val SERVICE_STATE_KINDS = setOf(
            MonitoringAnnouncementKind.DEADZONE,
            MonitoringAnnouncementKind.NO_SIGNAL_STATE,
            MonitoringAnnouncementKind.LIMITED_SERVICE_STATE
        )

        fun hasSoundIcon(kind: MonitoringAnnouncementKind): Boolean {
            return kind != MonitoringAnnouncementKind.TIER5 &&
                kind != MonitoringAnnouncementKind.SPECIAL_CELL
        }

        fun estimatedSpokenMs(kind: MonitoringAnnouncementKind): Long {
            val toneMs = when (kind) {
                MonitoringAnnouncementKind.CELL_IDENTITY -> 450L
                MonitoringAnnouncementKind.TECHNOLOGY_CHANGE,
                MonitoringAnnouncementKind.G2_FALLBACK -> 300L
                MonitoringAnnouncementKind.NO_SIGNAL_STATE,
                MonitoringAnnouncementKind.DEADZONE -> 250L
                MonitoringAnnouncementKind.LIMITED_SERVICE_STATE,
                MonitoringAnnouncementKind.LIMITED_SERVICE_OPERATOR -> 800L
                MonitoringAnnouncementKind.TIER5,
                MonitoringAnnouncementKind.SPECIAL_CELL -> 0L
            }
            return toneMs + ALERT_VOICE_GAP_MS + ESTIMATED_SPEECH_MS
        }

        fun shouldSupersede(
            existing: MonitoringAnnouncementKind,
            incoming: MonitoringAnnouncementKind
        ): Boolean {
            if (existing == incoming) return true
            if (existing in SERVICE_STATE_KINDS && incoming in SERVICE_STATE_KINDS) return true
            if (existing == MonitoringAnnouncementKind.CELL_IDENTITY &&
                incoming.playbackPriority < existing.playbackPriority
            ) {
                return true
            }
            if (incoming == MonitoringAnnouncementKind.DEADZONE &&
                existing in CAMP_DEPENDENT_KINDS
            ) {
                return true
            }
            if (existing == MonitoringAnnouncementKind.TIER5 &&
                incoming in setOf(
                    MonitoringAnnouncementKind.DEADZONE,
                    MonitoringAnnouncementKind.NO_SIGNAL_STATE
                )
            ) {
                return true
            }
            return false
        }

        fun shouldDropIncoming(
            incoming: MonitoringAnnouncementKind,
            present: Collection<MonitoringAnnouncementKind>
        ): Boolean {
            if (incoming == MonitoringAnnouncementKind.TIER5 &&
                present.any {
                    it == MonitoringAnnouncementKind.DEADZONE ||
                        it == MonitoringAnnouncementKind.NO_SIGNAL_STATE
                }
            ) {
                return true
            }
            return false
        }

        fun shouldInterruptPlaying(
            playing: MonitoringAnnouncementKind,
            incoming: MonitoringAnnouncementKind
        ): Boolean = shouldSupersede(playing, incoming)

        private val CAMP_DEPENDENT_KINDS = setOf(
            MonitoringAnnouncementKind.TIER5,
            MonitoringAnnouncementKind.G2_FALLBACK,
            MonitoringAnnouncementKind.LIMITED_SERVICE_OPERATOR,
            MonitoringAnnouncementKind.TECHNOLOGY_CHANGE,
            MonitoringAnnouncementKind.CELL_IDENTITY,
            MonitoringAnnouncementKind.SPECIAL_CELL
        )
    }
}
