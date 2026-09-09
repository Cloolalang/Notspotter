package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader

object SignalStateAnnouncement {

    val LTE_NR_RADIO_TYPES = setOf(
        CellularSignalReader.RADIO_4G,
        CellularSignalReader.RADIO_5G,
        CellularSignalReader.RADIO_5G_ENDC
    )

    /** Spoken phrase order: operator → tech → signal state → service state (when not implicit 4G/5G full service). */
    internal const val PHRASE_SIGNAL_LOW = "signal low"
    internal const val PHRASE_NO_SIGNAL = "no signal"
    internal const val PHRASE_SIGNAL_RESTORED = "signal restored"
    internal const val PHRASE_LIMITED_SERVICE = "limited service"
    internal const val PHRASE_HOME_OPERATOR_ROLE = "home"
    internal const val PHRASE_VISITED_OPERATOR_ROLE = "visited"
    internal const val PHRASE_DEADZONE = "deadzone, no service, no SOS calls"
    internal const val PHRASE_SEARCHING_2G = "searching 2 G"

    fun isLteNrRadioAccessType(radioAccessType: String?): Boolean {
        return radioAccessType != null && radioAccessType in LTE_NR_RADIO_TYPES
    }

    fun previewTechnologyChange(
        networkOperatorName: String?,
        target: TechnologyChangeTarget = TechnologyChangeTarget.TO_4G
    ): String {
        return formatTechnologyChange(target.radioAccessType, networkOperatorName)
    }

    fun previewTier5SignalLow(networkOperatorName: String?): String {
        return formatTier5SignalLowAnnouncement(networkOperatorName, CellularSignalReader.RADIO_4G)
    }

    fun previewNoSignal(networkOperatorName: String?): String {
        return formatNoSignalAnnouncement(networkOperatorName, CellularSignalReader.RADIO_4G)
    }

    fun previewLimitedService(networkOperatorName: String?): String {
        return formatLimitedServiceAnnouncement(
            homeOperatorName = networkOperatorName,
            visitedOperatorName = "E E",
            radioAccessType = CellularSignalReader.RADIO_4G
        )
    }

    /** Operator + tech only (implicit 4G/5G full service — no service-state phrase). */
    fun formatTechnologyChange(radioAccessType: String, networkOperatorName: String?): String {
        return joinAnnouncementParts(
            operatorNames = listOf(networkOperatorName),
            radioAccessType = radioAccessType
        )
    }

    fun formatTier5SignalLowAnnouncement(
        networkOperatorName: String?,
        radioAccessType: String?
    ): String {
        return formatCampedSignalStateAnnouncement(
            operatorSpeech = networkOperatorName?.let(NetworkOperatorSpeech::formatForSpeech),
            radioAccessType = radioAccessType,
            signalState = PHRASE_SIGNAL_LOW
        )
    }

    fun formatTier5SignalLowAnnouncement(
        stats: ConnectivityStats,
        lastKnownRadioAccessType: String? = null
    ): String {
        return formatCampedSignalStateAnnouncement(
            stats = stats,
            lastKnownRadioAccessType = lastKnownRadioAccessType,
            signalState = PHRASE_SIGNAL_LOW
        )
    }

    fun formatNoSignalAnnouncement(
        networkOperatorName: String?,
        radioAccessType: String?
    ): String {
        return formatCampedSignalStateAnnouncement(
            operatorSpeech = networkOperatorName?.let(NetworkOperatorSpeech::formatForSpeech),
            radioAccessType = radioAccessType,
            signalState = PHRASE_NO_SIGNAL
        )
    }

    /** Operator → tech → signal state; camped visited PLMN uses `{operator} visited`. */
    internal fun formatCampedSignalStateAnnouncement(
        stats: ConnectivityStats,
        lastKnownRadioAccessType: String? = null,
        signalState: String
    ): String {
        return formatCampedSignalStateAnnouncement(
            operatorSpeech = stats.formatCampedOperatorForSpeech(),
            radioAccessType = stats.resolveNoSignalAnnouncementRadioAccessType(lastKnownRadioAccessType),
            signalState = signalState
        )
    }

    internal fun formatCampedSignalStateAnnouncement(
        operatorSpeech: String?,
        radioAccessType: String?,
        signalState: String
    ): String {
        val parts = mutableListOf<String>()
        operatorSpeech?.takeIf { it.isNotBlank() }?.let(parts::add)
        radioAccessType?.takeIf { it.isNotBlank() }?.let {
            parts.add(formatTechnologyForSpeech(it))
        }
        parts.add(signalState)
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
        return joinAnnouncementParts(
            operatorNames = listOf(networkOperatorName),
            radioAccessType = radioAccessType,
            signalState = PHRASE_SIGNAL_RESTORED
        )
    }

    fun formatNoSignalChange(stats: ConnectivityStats, lastKnownRadioAccessType: String? = null): String? {
        return formatNoSignalChange(
            active = stats.noSignalActive,
            networkOperatorName = stats.networkOperatorName,
            radioAccessType = stats.resolveNoSignalAnnouncementRadioAccessType(lastKnownRadioAccessType)
        )
    }

    fun formatNoSignalAnnouncement(stats: ConnectivityStats, lastKnownRadioAccessType: String? = null): String {
        return formatCampedSignalStateAnnouncement(
            stats = stats,
            lastKnownRadioAccessType = lastKnownRadioAccessType,
            signalState = PHRASE_NO_SIGNAL
        )
    }

    fun formatSignalRestoredAnnouncement(
        stats: ConnectivityStats,
        lastKnownRadioAccessType: String? = null
    ): String {
        return formatCampedSignalStateAnnouncement(
            stats = stats,
            lastKnownRadioAccessType = lastKnownRadioAccessType,
            signalState = PHRASE_SIGNAL_RESTORED
        )
    }

    fun formatLimitedServiceChange(
        stats: ConnectivityStats,
        lastKnownRadioAccessType: String? = null
    ): String {
        return formatLimitedServiceAnnouncement(stats, lastKnownRadioAccessType)
    }

    fun formatLimitedServiceAnnouncement(
        homeOperatorName: String?,
        visitedOperatorName: String?,
        radioAccessType: String?
    ): String {
        val parts = mutableListOf<String>()
        if (visitedOperatorName != null) {
            NetworkOperatorSpeech.formatForSpeech(homeOperatorName)?.let {
                parts.add("$it $PHRASE_HOME_OPERATOR_ROLE")
            }
            NetworkOperatorSpeech.formatForSpeech(visitedOperatorName)?.let {
                parts.add("$it $PHRASE_VISITED_OPERATOR_ROLE")
            }
        } else {
            NetworkOperatorSpeech.formatForSpeech(homeOperatorName)?.let(parts::add)
        }
        radioAccessType?.takeIf { it.isNotBlank() }?.let {
            parts.add(formatTechnologyForSpeech(it))
        }
        parts.add(PHRASE_LIMITED_SERVICE)
        return parts.joinToString(", ")
    }

    fun formatLimitedServiceAnnouncement(
        networkOperatorName: String?,
        radioAccessType: String?
    ): String {
        return formatLimitedServiceAnnouncement(
            homeOperatorName = networkOperatorName,
            visitedOperatorName = null,
            radioAccessType = radioAccessType
        )
    }

    fun formatLimitedServiceAnnouncement(
        stats: ConnectivityStats,
        lastKnownRadioAccessType: String? = null
    ): String {
        return formatLimitedServiceAnnouncement(
            homeOperatorName = stats.homeNetworkOperatorName ?: stats.networkOperatorName,
            visitedOperatorName = stats.resolveLimitedServiceVisitedOperatorName(),
            radioAccessType = stats.resolveNoSignalAnnouncementRadioAccessType(lastKnownRadioAccessType)
        )
    }

    /** Spoken when the phone camps on 2G after losing LTE/NR signal (operator + tech; 2G is not implicit full service). */
    fun formatG2CampedAnnouncement(networkOperatorName: String?): String {
        return joinAnnouncementParts(
            operatorNames = listOf(networkOperatorName),
            radioAccessType = CellularSignalReader.RADIO_2G
        )
    }

    /** Spoken after LTE/NR no signal while the phone is still scanning for 2G. */
    fun formatSearching2gAnnouncement(
        networkOperatorName: String?,
        lastKnownLteNrRadioAccessType: String?
    ): String {
        return joinAnnouncementParts(
            operatorNames = listOf(networkOperatorName),
            radioAccessType = lastKnownLteNrRadioAccessType,
            signalState = PHRASE_NO_SIGNAL,
            serviceState = PHRASE_SEARCHING_2G
        )
    }

    fun formatDeadzoneAnnouncement(networkOperatorName: String?): String {
        return joinAnnouncementParts(
            operatorNames = listOf(networkOperatorName),
            serviceState = PHRASE_DEADZONE
        )
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

    /**
     * Builds `{operator}[, {alt}]`, `{tech}`, optional signal state, optional service state.
     * Tech is omitted for dead zone (no camped RAT). 4G/5G full service has no service-state phrase.
     */
    internal fun joinAnnouncementParts(
        operatorNames: List<String?>,
        radioAccessType: String? = null,
        signalState: String? = null,
        serviceState: String? = null
    ): String {
        val parts = mutableListOf<String>()
        operatorNames.forEach { name ->
            NetworkOperatorSpeech.formatForSpeech(name)?.let { spoken ->
                if (parts.none { it.equals(spoken, ignoreCase = true) }) {
                    parts.add(spoken)
                }
            }
        }
        radioAccessType?.takeIf { it.isNotBlank() }?.let {
            parts.add(formatTechnologyForSpeech(it))
        }
        signalState?.takeIf { it.isNotBlank() }?.let(parts::add)
        serviceState?.takeIf { it.isNotBlank() }?.let(parts::add)
        return parts.joinToString(", ")
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
