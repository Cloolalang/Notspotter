package io.github.cloolalang.notspotdetector.model

enum class NetworkServiceMode {
    IN_SERVICE,
    LIMITED_SERVICE,
    OUT_OF_SERVICE,
    /** Airplane mode / radio powered off ([android.telephony.ServiceState.STATE_POWER_OFF]). */
    RADIO_OFF,
    UNKNOWN
}

fun NetworkServiceMode.isRadioPoweredOff(): Boolean = this == NetworkServiceMode.RADIO_OFF

fun NetworkServiceMode.isNoCellularService(): Boolean {
    return this == NetworkServiceMode.OUT_OF_SERVICE || this == NetworkServiceMode.RADIO_OFF
}
