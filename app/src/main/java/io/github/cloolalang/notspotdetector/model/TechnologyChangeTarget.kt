package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader

/** Destination RAT for a momentary technology-change event (RXSS 28–30). */
enum class TechnologyChangeTarget(
    val rxssNumber: Int,
    val radioAccessType: String
) {
    TO_2G(Rxss.TECH_CHANGE_TO_2G, CellularSignalReader.RADIO_2G),
    TO_4G(Rxss.TECH_CHANGE_TO_4G, CellularSignalReader.RADIO_4G),
    TO_5G_ENDC(Rxss.TECH_CHANGE_TO_5G_ENDC, CellularSignalReader.RADIO_5G_ENDC);

    companion object {
        val thresholdPanelOrder = listOf(TO_2G, TO_4G, TO_5G_ENDC)

        fun fromRadioAccessType(radioAccessType: String?): TechnologyChangeTarget? {
            return when (radioAccessType) {
                CellularSignalReader.RADIO_2G -> TO_2G
                CellularSignalReader.RADIO_4G -> TO_4G
                CellularSignalReader.RADIO_5G_ENDC,
                CellularSignalReader.RADIO_5G -> TO_5G_ENDC
                else -> null
            }
        }
    }
}

data class TechnologyChangeAlertVolumes(
    val toneVolume: Float,
    val voiceEnabled: Boolean,
    val voiceVolume: Float
)
