package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader

object SignalStateAnnouncement {

    fun previewTechnologyChange(networkOperatorName: String?): String {
        return formatTechnologyChange(CellularSignalReader.RADIO_4G, networkOperatorName)
    }

    fun previewNoSignal(networkOperatorName: String?): String {
        return formatNoSignalChange(active = true, networkOperatorName)
    }

    fun previewLimitedService(networkOperatorName: String?): String {
        return formatLimitedServiceChange(active = true, networkOperatorName)
    }

    fun formatTechnologyChange(radioAccessType: String, networkOperatorName: String?): String {
        val tech = formatTechnologyForSpeech(radioAccessType)
        return prefixNetworkOperator("Technology change, $tech", networkOperatorName)
    }

    fun formatNoSignalChange(active: Boolean, networkOperatorName: String?): String {
        val message = if (active) "No signal" else "Signal restored"
        return prefixNetworkOperator(message, networkOperatorName)
    }

    fun formatLimitedServiceChange(active: Boolean, networkOperatorName: String?): String {
        val message = if (active) "Limited service" else "Full service"
        return prefixNetworkOperator(message, networkOperatorName)
    }

    fun formatTechnologyForSpeech(radioAccessType: String): String {
        return when (radioAccessType) {
            CellularSignalReader.RADIO_2G -> "2 G"
            CellularSignalReader.RADIO_4G -> "4 G"
            CellularSignalReader.RADIO_5G -> "5 G"
            CellularSignalReader.RADIO_5G_ENDC -> "5 G E N D C"
            else -> radioAccessType
                .replace("-", " ")
                .split(' ')
                .joinToString(" ") { word ->
                    word.toCharArray().joinToString(" ")
                }
        }
    }

    private fun prefixNetworkOperator(message: String, networkOperatorName: String?): String {
        val name = NetworkOperatorSpeech.formatForSpeech(networkOperatorName) ?: return message
        return "$name, $message"
    }
}
