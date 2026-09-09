package io.github.cloolalang.notspotdetector.model

/**
 * Stable voice announcement IDs (**VA-1** … **VA-18**) and playback priorities.
 * Phrase templates: [SignalStateAnnouncement] · doc: VOICE_ANNOUNCEMENTS.md
 */
object VoiceAnnouncement {

    const val VA_1_NO_SIGNAL_ENTRY = 1
    const val VA_2_SIGNAL_RESTORED = 2
    const val VA_3_DEADZONE_ENTRY = 3
    const val VA_4_LIMITED_SERVICE_ENTRY = 4
    /** Retired — limited-service exit; camp on 4G/5G is implicit full service. */
    const val VA_5_RETIRED = 5
    const val VA_6_LIMITED_SERVICE_OPERATOR = 6
    const val VA_7_G2_FALLBACK = 7
    const val VA_8_SIGNAL_LOW_IMMEDIATE = 8
    const val VA_9_TECHNOLOGY_CHANGE = 9
    const val VA_10_CELL_RESELECT = 10
    const val VA_11_SEARCHING_2G = 11
    const val VA_12_NO_SIGNAL_REPEAT = 12
    const val VA_13_DEADZONE_REPEAT = 13
    const val VA_14_LIMITED_SERVICE_REPEAT = 14
    const val VA_15_SIGNAL_LOW_REPEAT = 15
    const val VA_16_G2_CAMPED_REPEAT = 16
    const val VA_17_G2_NO_SIGNAL_REPEAT = 17
    const val VA_18_G2_WEAK_REPEAT = 18

    /** Lower number speaks first when several announcements queue together. */
    fun priorityFor(kind: MonitoringAnnouncementKind): Int = when (kind) {
        MonitoringAnnouncementKind.DEADZONE -> 1
        MonitoringAnnouncementKind.NO_SIGNAL_STATE -> 2
        MonitoringAnnouncementKind.LIMITED_SERVICE_STATE -> 4
        MonitoringAnnouncementKind.TIER5 -> 5
        MonitoringAnnouncementKind.G2_FALLBACK -> 6
        MonitoringAnnouncementKind.LIMITED_SERVICE_OPERATOR -> 8
        MonitoringAnnouncementKind.TECHNOLOGY_CHANGE -> 8
        MonitoringAnnouncementKind.CELL_IDENTITY -> 9
    }

    /** Maps an immediate [MonitoringAnnouncementKind] to catalogue VA number(s). */
    fun vaNumbersFor(kind: MonitoringAnnouncementKind): List<Int> = when (kind) {
        MonitoringAnnouncementKind.NO_SIGNAL_STATE -> listOf(VA_1_NO_SIGNAL_ENTRY, VA_2_SIGNAL_RESTORED)
        MonitoringAnnouncementKind.DEADZONE -> listOf(VA_3_DEADZONE_ENTRY)
        MonitoringAnnouncementKind.LIMITED_SERVICE_STATE -> listOf(VA_4_LIMITED_SERVICE_ENTRY)
        MonitoringAnnouncementKind.TIER5 -> listOf(VA_8_SIGNAL_LOW_IMMEDIATE)
        MonitoringAnnouncementKind.G2_FALLBACK -> listOf(VA_7_G2_FALLBACK)
        MonitoringAnnouncementKind.LIMITED_SERVICE_OPERATOR -> listOf(VA_6_LIMITED_SERVICE_OPERATOR)
        MonitoringAnnouncementKind.TECHNOLOGY_CHANGE -> listOf(VA_9_TECHNOLOGY_CHANGE)
        MonitoringAnnouncementKind.CELL_IDENTITY -> listOf(VA_10_CELL_RESELECT)
    }

    /** Same-poll immediate playback order (lowest [priorityFor] first; tie-break by list order). */
    val immediatePlaybackOrder: List<MonitoringAnnouncementKind> = listOf(
        MonitoringAnnouncementKind.DEADZONE,
        MonitoringAnnouncementKind.NO_SIGNAL_STATE,
        MonitoringAnnouncementKind.LIMITED_SERVICE_STATE,
        MonitoringAnnouncementKind.TIER5,
        MonitoringAnnouncementKind.G2_FALLBACK,
        MonitoringAnnouncementKind.LIMITED_SERVICE_OPERATOR,
        MonitoringAnnouncementKind.TECHNOLOGY_CHANGE,
        MonitoringAnnouncementKind.CELL_IDENTITY
    )
}
