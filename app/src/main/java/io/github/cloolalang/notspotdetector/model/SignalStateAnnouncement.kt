package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader

object SignalStateAnnouncement {

    val LTE_NR_RADIO_TYPES = setOf(
        CellularSignalReader.RADIO_4G,
        CellularSignalReader.RADIO_5G,
        CellularSignalReader.RADIO_5G_ENDC
    )

    fun isLteNrRadioAccessType(radioAccessType: String?): Boolean {
        return radioAccessType != null && radioAccessType in LTE_NR_RADIO_TYPES
    }

    fun previewTechnologyChange(networkOperatorName: String?): String {
        return formatTechnologyChange(CellularSignalReader.RADIO_4G, networkOperatorName)
    }

    fun previewNoSignal(networkOperatorName: String?): String {
        return formatNoSignalAnnouncement(networkOperatorName, CellularSignalReader.RADIO_4G)
    }

    fun previewLimitedService(networkOperatorName: String?): String {
        return formatLimitedServiceAnnouncement(
            homeOperatorName = networkOperatorName,
            alternativeOperatorName = "E E",
            radioAccessType = CellularSignalReader.RADIO_4G
        )
    }

    fun formatTechnologyChange(radioAccessType: String, networkOperatorName: String?): String {
        val tech = formatTechnologyForSpeech(radioAccessType)
        return prefixNetworkOperator("Technology change, $tech", networkOperatorName)
    }

    fun formatNoSignalAnnouncement(
        networkOperatorName: String?,
        radioAccessType: String?
    ): String {
        val parts = buildList {
            NetworkOperatorSpeech.formatForSpeech(networkOperatorName)?.let(::add)
            radioAccessType?.takeIf { it.isNotBlank() }?.let {
                add(formatTechnologyForSpeech(it))
            }
            add("no signal")
        }
        return parts.joinToString(", ")
    }

    fun formatNoSignalChange(
        active: Boolean,
        networkOperatorName: String?,
        radioAccessType: String? = null
    ): String {
        if (active) {
            return formatNoSignalAnnouncement(networkOperatorName, radioAccessType)
        }
        return formatSignalRestoredAnnouncement(networkOperatorName, radioAccessType)
    }

    fun formatSignalRestoredAnnouncement(
        networkOperatorName: String?,
        radioAccessType: String?
    ): String {
        val tech = radioAccessType?.takeIf { it.isNotBlank() }?.let { formatTechnologyForSpeech(it) }
        val message = if (tech != null) "Signal restored, $tech" else "Signal restored"
        return prefixNetworkOperator(message, networkOperatorName)
    }

    fun formatNoSignalChange(stats: ConnectivityStats, lastKnownRadioAccessType: String? = null): String? {
        return formatNoSignalChange(
            active = stats.noSignalActive,
            networkOperatorName = stats.networkOperatorName,
            radioAccessType = stats.resolveNoSignalAnnouncementRadioAccessType(lastKnownRadioAccessType)
        )
    }

    fun formatNoSignalAnnouncement(stats: ConnectivityStats, lastKnownRadioAccessType: String? = null): String {
        return formatNoSignalAnnouncement(
            networkOperatorName = stats.networkOperatorName,
            radioAccessType = stats.resolveNoSignalAnnouncementRadioAccessType(lastKnownRadioAccessType)
        )
    }

    fun formatLimitedServiceChange(
        active: Boolean,
        stats: ConnectivityStats,
        lastKnownRadioAccessType: String? = null
    ): String {
        val radioAccessType = stats.resolveNoSignalAnnouncementRadioAccessType(lastKnownRadioAccessType)
        if (active) {
            return formatLimitedServiceAnnouncement(stats, lastKnownRadioAccessType)
        }
        return formatFullServiceAnnouncement(
            homeOperatorName = stats.homeNetworkOperatorName ?: stats.networkOperatorName,
            radioAccessType = radioAccessType
        )
    }

    fun formatLimitedServiceChange(
        active: Boolean,
        networkOperatorName: String?,
        radioAccessType: String? = null
    ): String {
        if (active) {
            return formatLimitedServiceAnnouncement(
                homeOperatorName = networkOperatorName,
                alternativeOperatorName = null,
                radioAccessType = radioAccessType
            )
        }
        return formatFullServiceAnnouncement(networkOperatorName, radioAccessType)
    }

    fun formatFullServiceAnnouncement(
        homeOperatorName: String?,
        radioAccessType: String?
    ): String {
        val tech = radioAccessType?.takeIf { it.isNotBlank() }?.let { formatTechnologyForSpeech(it) }
        val message = if (tech != null) "Full service, $tech" else "Full service"
        return prefixNetworkOperator(message, homeOperatorName)
    }

    fun formatLimitedServiceAnnouncement(
        homeOperatorName: String?,
        alternativeOperatorName: String?,
        radioAccessType: String?
    ): String {
        val parts = buildLimitedServiceOperatorParts(homeOperatorName, alternativeOperatorName)
        val tech = radioAccessType?.takeIf { it.isNotBlank() }?.let { formatTechnologyForSpeech(it) }
        parts.add(if (tech != null) "Limited service, $tech" else "Limited service")
        return parts.joinToString(", ")
    }

    fun formatLimitedServiceAnnouncement(
        networkOperatorName: String?,
        radioAccessType: String?
    ): String {
        return formatLimitedServiceAnnouncement(
            homeOperatorName = networkOperatorName,
            alternativeOperatorName = null,
            radioAccessType = radioAccessType
        )
    }

    fun formatLimitedServiceAnnouncement(
        stats: ConnectivityStats,
        lastKnownRadioAccessType: String? = null
    ): String {
        return formatLimitedServiceAnnouncement(
            homeOperatorName = stats.homeNetworkOperatorName ?: stats.networkOperatorName,
            alternativeOperatorName = stats.resolveLimitedServiceAlternativeOperatorName(),
            radioAccessType = stats.resolveNoSignalAnnouncementRadioAccessType(lastKnownRadioAccessType)
        )
    }

    /** Spoken when the phone camps on 2G after losing LTE/NR signal. */
    fun formatG2CampedAnnouncement(networkOperatorName: String?): String {
        val parts = buildList {
            NetworkOperatorSpeech.formatForSpeech(networkOperatorName)?.let(::add)
            add(formatTechnologyForSpeech(CellularSignalReader.RADIO_2G))
        }
        return parts.joinToString(", ")
    }

    /** Spoken after LTE/NR no signal while the phone is still scanning for 2G. */
    fun formatSearching2gAnnouncement(
        networkOperatorName: String?,
        lastKnownLteNrRadioAccessType: String?
    ): String {
        val parts = buildList {
            NetworkOperatorSpeech.formatForSpeech(networkOperatorName)?.let(::add)
            lastKnownLteNrRadioAccessType?.takeIf { it.isNotBlank() }?.let {
                add(formatTechnologyForSpeech(it))
            }
            add("no signal")
            add("searching 2 G")
        }
        return parts.joinToString(", ")
    }

    fun formatDeadzoneAnnouncement(networkOperatorName: String?): String {
        return prefixNetworkOperator("all technologies dead zone, scanning", networkOperatorName)
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

    private fun buildLimitedServiceOperatorParts(
        homeOperatorName: String?,
        alternativeOperatorName: String?
    ): MutableList<String> {
        val parts = mutableListOf<String>()
        NetworkOperatorSpeech.formatForSpeech(homeOperatorName)?.let(parts::add)
        val alternative = NetworkOperatorSpeech.formatForSpeech(alternativeOperatorName)
        if (alternative != null && parts.none { it.equals(alternative, ignoreCase = true) }) {
            parts.add(alternative)
        }
        return parts
    }
}

/** Technology spoken in no-signal alerts when the radio omits [ConnectivityStats.radioAccessType] on 2G. */
fun ConnectivityStats.resolveNoSignalAnnouncementRadioAccessType(
    lastKnownRadioAccessType: String? = null
): String? {
    radioAccessType?.takeIf { it.isNotBlank() }?.let { return it }
    lastKnownRadioAccessType?.takeIf { it.isNotBlank() }?.let { return it }
    if (isOn2g || restrictedTo2gNetwork) return CellularSignalReader.RADIO_2G
    return null
}
