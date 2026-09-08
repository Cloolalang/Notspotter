package io.github.cloolalang.notspotdetector.model

data class MonitoringUpdateEvents(
    val cellChangeAnnouncement: String? = null,
    val technologyChangeAnnouncement: String? = null,
    val noSignalStateAnnouncement: String? = null,
    val limitedServiceStateAnnouncement: String? = null
) {
    val cellIdentityChanged: Boolean
        get() = !cellChangeAnnouncement.isNullOrBlank()

    val radioTechnologyChanged: Boolean
        get() = !technologyChangeAnnouncement.isNullOrBlank()

    val noSignalStateChanged: Boolean
        get() = !noSignalStateAnnouncement.isNullOrBlank()

    val limitedServiceStateChanged: Boolean
        get() = !limitedServiceStateAnnouncement.isNullOrBlank()
}
