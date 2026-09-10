package io.github.cloolalang.notspotdetector.model

/**
 * One row of the "Carrier config (UE behavior)" research panel — see
 * [io.github.cloolalang.notspotdetector.network.CarrierConfigReader].
 */
data class CarrierConfigEntry(
    val category: String,
    val label: String,
    val key: String,
    val value: String
)

/**
 * Snapshot of a curated set of [android.telephony.CarrierConfigManager] keys that affect actual
 * UE (device) behavior — VoLTE, WiFi calling, VoNR, NR/5G availability, network selection, and
 * emergency calling — as opposed to purely cosmetic/display/icon keys. This is a research/
 * diagnostics feature only; nothing elsewhere in the app reads or depends on this snapshot.
 */
data class CarrierConfigSnapshot(
    val available: Boolean = false,
    val subscriptionId: Int = -1,
    val carrierName: String? = null,
    val entries: List<CarrierConfigEntry> = emptyList()
)
