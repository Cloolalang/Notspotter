package io.github.cloolalang.notspotdetector.model

data class MonitoringUpdateEvents(
    val cellChangeAnnouncement: String? = null,
    val technologyChangeAnnouncement: String? = null,
    val technologyChangeTargetRadioAccessType: String? = null,
    val noSignalStateAnnouncement: String? = null,
    val limitedServiceStateAnnouncement: String? = null,
    val limitedServiceOperatorChangeAnnouncement: String? = null,
    val g2FallbackAnnouncement: String? = null,
    val deadzoneAnnouncement: String? = null,
    val tier5Announcement: String? = null,
    val searching2gStateEntered: Boolean = false,
    /** Restart the 2G periodic voice timer (e.g. flatline entered/exited on 2G). */
    val g2PeriodicReset: Boolean = false,
    /** Play tier 5 voice immediately (dead zone → tier 5; tier 10 → tier 6), then periodic repeats. */
    val tier5Immediate: Boolean = false,
    /** Entered RXSS 20 / 23 (visited limited-service no-signal overlay). */
    val limitedVisitedNoSignalStateChanged: Boolean = false
) {
    val cellIdentityChanged: Boolean
        get() = !cellChangeAnnouncement.isNullOrBlank()

    val radioTechnologyChanged: Boolean
        get() = !technologyChangeAnnouncement.isNullOrBlank()

    val noSignalStateChanged: Boolean
        get() = !noSignalStateAnnouncement.isNullOrBlank()

    val limitedServiceStateChanged: Boolean
        get() = !limitedServiceStateAnnouncement.isNullOrBlank()

    val limitedServiceOperatorChanged: Boolean
        get() = !limitedServiceOperatorChangeAnnouncement.isNullOrBlank()

    val g2FallbackAnnounced: Boolean
        get() = !g2FallbackAnnouncement.isNullOrBlank()

    val deadzoneAnnounced: Boolean
        get() = !deadzoneAnnouncement.isNullOrBlank()

    val tier5Announced: Boolean
        get() = !tier5Announcement.isNullOrBlank()

    /** Immediate voice alerts in [VoiceAnnouncement.immediatePlaybackOrder] (see VOICE_ANNOUNCEMENTS.md). */
    fun immediateAnnouncements(): List<MonitoringAnnouncement> {
        return VoiceAnnouncement.immediatePlaybackOrder.mapNotNull { kind ->
            immediateAnnouncementFor(kind)
        }
    }

    fun hasImmediateAnnouncements(): Boolean = immediateAnnouncements().isNotEmpty()

    private fun immediateAnnouncementFor(kind: MonitoringAnnouncementKind): MonitoringAnnouncement? {
        return when (kind) {
            MonitoringAnnouncementKind.DEADZONE -> deadzoneAnnounced.takeIf { it }?.let {
                MonitoringAnnouncement(kind, deadzoneAnnouncement)
            }
            MonitoringAnnouncementKind.NO_SIGNAL_STATE -> noSignalStateChanged.takeIf { it }?.let {
                MonitoringAnnouncement(kind, noSignalStateAnnouncement)
            }
            MonitoringAnnouncementKind.LIMITED_SERVICE_STATE -> limitedServiceStateChanged.takeIf { it }?.let {
                MonitoringAnnouncement(kind, limitedServiceStateAnnouncement)
            }
            MonitoringAnnouncementKind.TIER5 -> {
                if (tier5Immediate && tier5Announced) {
                    MonitoringAnnouncement(kind, tier5Announcement)
                } else {
                    null
                }
            }
            MonitoringAnnouncementKind.G2_FALLBACK -> g2FallbackAnnounced.takeIf { it }?.let {
                MonitoringAnnouncement(kind, g2FallbackAnnouncement)
            }
            MonitoringAnnouncementKind.LIMITED_SERVICE_OPERATOR -> limitedServiceOperatorChanged.takeIf { it }?.let {
                MonitoringAnnouncement(kind, limitedServiceOperatorChangeAnnouncement)
            }
            MonitoringAnnouncementKind.TECHNOLOGY_CHANGE -> radioTechnologyChanged.takeIf { it }?.let {
                MonitoringAnnouncement(
                    kind = kind,
                    message = technologyChangeAnnouncement,
                    targetRadioAccessType = technologyChangeTargetRadioAccessType
                )
            }
            MonitoringAnnouncementKind.CELL_IDENTITY -> cellIdentityChanged.takeIf { it }?.let {
                MonitoringAnnouncement(kind, cellChangeAnnouncement)
            }
        }
    }
}

enum class MonitoringAnnouncementKind {
    NO_SIGNAL_STATE,
    DEADZONE,
    LIMITED_SERVICE_STATE,
    LIMITED_SERVICE_OPERATOR,
    G2_FALLBACK,
    TIER5,
    TECHNOLOGY_CHANGE,
    CELL_IDENTITY;

    /** Playback priority — see [VoiceAnnouncement.priorityFor]. */
    val playbackPriority: Int
        get() = VoiceAnnouncement.priorityFor(this)

    /** Catalogue VA number(s) — see [VoiceAnnouncement.vaNumbersFor]. */
    val vaNumbers: List<Int>
        get() = VoiceAnnouncement.vaNumbersFor(this)

    /** RX Signal State catalogue number when this announcement maps to an RXSS row. */
    val rxssNumber: Int?
        get() = when (this) {
            CELL_IDENTITY -> Rxss.CELL_CHANGE
            else -> null
        }
}

data class MonitoringAnnouncement(
    val kind: MonitoringAnnouncementKind,
    val message: String?,
    val targetRadioAccessType: String? = null
)
