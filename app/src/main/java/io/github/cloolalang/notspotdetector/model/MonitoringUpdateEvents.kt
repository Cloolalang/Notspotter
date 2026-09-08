package io.github.cloolalang.notspotdetector.model

data class MonitoringUpdateEvents(
    val cellChangeAnnouncement: String? = null,
    val technologyChangeAnnouncement: String? = null,
    val noSignalStateAnnouncement: String? = null,
    val limitedServiceStateAnnouncement: String? = null,
    val limitedServiceOperatorChangeAnnouncement: String? = null,
    val g2FallbackAnnouncement: String? = null,
    val deadzoneAnnouncement: String? = null,
    val tier5Announcement: String? = null,
    val searching2gStateEntered: Boolean = false
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
}
