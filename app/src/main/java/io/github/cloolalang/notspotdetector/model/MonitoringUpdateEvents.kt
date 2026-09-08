package io.github.cloolalang.notspotdetector.model

data class MonitoringUpdateEvents(
    val cellIdentityChanged: Boolean = false,
    val radioTechnologyChanged: Boolean = false
)
