package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader

object SignalStateAnnouncement {

    val LTE_NR_RADIO_TYPES = setOf(
        CellularSignalReader.RADIO_4G,
        CellularSignalReader.RADIO_5G,
        CellularSignalReader.RADIO_5G_ENDC
    )

    /** Spoken phrase order: operator → tech → band → signal state → service state (when not implicit 4G/5G full service). */
    internal const val PHRASE_SIGNAL_LOW = "signal low"
    internal const val PHRASE_NO_SIGNAL = "no signal"
    internal const val PHRASE_SIGNAL_RESTORED = "signal restored"
    internal const val PHRASE_LIMITED_SERVICE = "limited service"
    internal const val PHRASE_IN_SERVICE = "in-service"
    internal const val PHRASE_HOME_IN_SERVICE = "home in service"
    internal const val PHRASE_ROAMING_IN_SERVICE = "roaming in service"
    internal const val PHRASE_HOME_LIMITED_SERVICE = "home limited service"
    internal const val PHRASE_VISITING_LIMITED_SERVICE = "visiting limited service"
    internal const val PHRASE_HOME_OPERATOR_ROLE = "home"
    internal const val PHRASE_VISITED_OPERATOR_ROLE = "visited"
    internal const val PHRASE_DEADZONE = "dead zone, no service, no SOS calls"
    internal const val PHRASE_SEARCHING_2G = "searching 2 G"
    /** RXSS 31 — in service via WiFi calling only, no cellular RAT/RSRP. */
    internal const val PHRASE_WIFI_CALLING_NO_SIGNAL = "wifi calling, no cellular signal"
    internal const val PHRASE_CELLULAR_SIGNAL_RESTORED = "cellular signal restored"
    /** VA-20 — spoken every 30 s while mock network mode is on. */
    const val PHRASE_MOCK_NETWORK = "mock network"

    private const val PREVIEW_LTE_EARFCN = 6400

    fun isLteNrRadioAccessType(radioAccessType: String?): Boolean {
        return radioAccessType != null && radioAccessType in LTE_NR_RADIO_TYPES
    }

    fun previewTechnologyChange(
        networkOperatorName: String?,
        target: TechnologyChangeTarget = TechnologyChangeTarget.TO_4G,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        phrases: VoicePhraseOptions = VoicePhraseOptions(
            speakOperatorName = speakOperatorNameEnabled,
            speakTechnology = speakTechnologyEnabled
        ),
        bandNamingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT
    ): String {
        return formatTechnologyChange(
            target.radioAccessType,
            networkOperatorName,
            phrases.speakOperatorName,
            phrases.speakTechnology,
            phrases.speakBand,
            phrases.bandPhraseFor(PREVIEW_LTE_EARFCN, namingStyle = bandNamingStyle)
        )
    }

    fun previewTier5SignalLow(
        networkOperatorName: String?,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        phrases: VoicePhraseOptions = VoicePhraseOptions(
            speakOperatorName = speakOperatorNameEnabled,
            speakTechnology = speakTechnologyEnabled
        ),
        bandNamingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT
    ): String {
        return formatTier5SignalLowAnnouncement(
            networkOperatorName,
            CellularSignalReader.RADIO_4G,
            phrases.speakOperatorName,
            phrases.speakTechnology,
            phrases.speakBand,
            phrases.bandPhraseFor(PREVIEW_LTE_EARFCN, namingStyle = bandNamingStyle)
        )
    }

    fun previewNoSignal(
        networkOperatorName: String?,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        phrases: VoicePhraseOptions = VoicePhraseOptions(
            speakOperatorName = speakOperatorNameEnabled,
            speakTechnology = speakTechnologyEnabled
        ),
        bandNamingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT
    ): String {
        return formatNoSignalAnnouncement(
            networkOperatorName,
            CellularSignalReader.RADIO_4G,
            speakOperatorNameEnabled = phrases.speakOperatorName,
            speakTechnologyEnabled = phrases.speakTechnology,
            speakBandEnabled = phrases.speakBand,
            bandPhrase = phrases.bandPhraseFor(PREVIEW_LTE_EARFCN, namingStyle = bandNamingStyle)
        )
    }

    fun previewLimitedService(
        networkOperatorName: String?,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        phrases: VoicePhraseOptions = VoicePhraseOptions(
            speakOperatorName = speakOperatorNameEnabled,
            speakTechnology = speakTechnologyEnabled
        ),
        bandNamingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT
    ): String {
        return formatLimitedServiceAnnouncement(
            homeOperatorName = networkOperatorName,
            visitedOperatorName = "E E",
            radioAccessType = CellularSignalReader.RADIO_4G,
            speakOperatorNameEnabled = phrases.speakOperatorName,
            speakTechnologyEnabled = phrases.speakTechnology,
            speakBandEnabled = phrases.speakBand,
            bandPhrase = phrases.bandPhraseFor(PREVIEW_LTE_EARFCN, namingStyle = bandNamingStyle),
            speakHomeLimitedService = phrases.speakHomeLimitedService,
            speakVisitingLimitedService = phrases.speakVisitingLimitedService
        )
    }

    /**
     * Operator + tech + optional band (implicit 4G/5G full service — no service-state phrase).
     */
    fun formatTechnologyChange(
        radioAccessType: String,
        networkOperatorName: String?,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        speakBandEnabled: Boolean = false,
        bandPhrase: String? = null
    ): String {
        return joinAnnouncementParts(
            operatorNames = listOf(networkOperatorName),
            radioAccessType = radioAccessType,
            speakOperatorNameEnabled = speakOperatorNameEnabled,
            speakTechnologyEnabled = speakTechnologyEnabled,
            speakBandEnabled = speakBandEnabled,
            bandPhrase = bandPhrase
        )
    }

    fun formatTier5SignalLowAnnouncement(
        networkOperatorName: String?,
        radioAccessType: String?,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        speakBandEnabled: Boolean = false,
        bandPhrase: String? = null
    ): String {
        return formatCampedSignalStateAnnouncement(
            operatorSpeech = networkOperatorName?.let(NetworkOperatorSpeech::formatForSpeech),
            radioAccessType = radioAccessType,
            signalState = PHRASE_SIGNAL_LOW,
            speakOperatorNameEnabled = speakOperatorNameEnabled,
            speakTechnologyEnabled = speakTechnologyEnabled,
            speakBandEnabled = speakBandEnabled,
            bandPhrase = bandPhrase
        )
    }

    fun formatTier5SignalLowAnnouncement(
        stats: ConnectivityStats,
        lastKnownRadioAccessType: String? = null,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        phrases: VoicePhraseOptions = VoicePhraseOptions(
            speakOperatorName = speakOperatorNameEnabled,
            speakTechnology = speakTechnologyEnabled
        ),
        bandNamingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT
    ): String {
        return formatCampedSignalStateAnnouncement(
            stats = stats,
            lastKnownRadioAccessType = lastKnownRadioAccessType,
            signalState = PHRASE_SIGNAL_LOW,
            phrases = phrases,
            bandNamingStyle = bandNamingStyle
        )
    }

    fun formatNoSignalAnnouncement(
        networkOperatorName: String?,
        radioAccessType: String?,
        isWifiCallingActive: Boolean = false,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        speakBandEnabled: Boolean = false,
        bandPhrase: String? = null
    ): String {
        if (isWifiCallingActive) {
            return joinAnnouncementParts(
                operatorNames = listOf(networkOperatorName),
                serviceState = PHRASE_WIFI_CALLING_NO_SIGNAL,
                speakOperatorNameEnabled = speakOperatorNameEnabled
            )
        }
        return formatCampedSignalStateAnnouncement(
            operatorSpeech = networkOperatorName?.let(NetworkOperatorSpeech::formatForSpeech),
            radioAccessType = radioAccessType,
            signalState = PHRASE_NO_SIGNAL,
            speakOperatorNameEnabled = speakOperatorNameEnabled,
            speakTechnologyEnabled = speakTechnologyEnabled,
            speakBandEnabled = speakBandEnabled,
            bandPhrase = bandPhrase
        )
    }

    /** Operator → tech → signal state; camped visited PLMN uses `{operator} visited`. */
    internal fun formatCampedSignalStateAnnouncement(
        stats: ConnectivityStats,
        lastKnownRadioAccessType: String? = null,
        signalState: String,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        phrases: VoicePhraseOptions = VoicePhraseOptions(
            speakOperatorName = speakOperatorNameEnabled,
            speakTechnology = speakTechnologyEnabled
        ),
        serviceState: String? = null,
        bandNamingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT
    ): String {
        return formatCampedSignalStateAnnouncement(
            operatorSpeech = stats.formatCampedOperatorForSpeech(),
            radioAccessType = stats.resolveNoSignalAnnouncementRadioAccessType(lastKnownRadioAccessType),
            signalState = signalState,
            speakOperatorNameEnabled = phrases.speakOperatorName,
            speakTechnologyEnabled = phrases.speakTechnology,
            speakBandEnabled = phrases.speakBand,
            bandPhrase = phrases.bandPhraseFor(
                stats.lteEarfcn,
                stats.nrBand,
                stats.gsmEarfcn,
                namingStyle = bandNamingStyle
            ),
            serviceState = serviceState
        )
    }

    internal fun formatCampedSignalStateAnnouncement(
        operatorSpeech: String?,
        radioAccessType: String?,
        signalState: String,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        speakBandEnabled: Boolean = false,
        bandPhrase: String? = null,
        serviceState: String? = null
    ): String {
        val parts = mutableListOf<String>()
        if (speakOperatorNameEnabled) {
            operatorSpeech?.takeIf { it.isNotBlank() }?.let(parts::add)
        }
        if (speakTechnologyEnabled) {
            radioAccessType?.takeIf { it.isNotBlank() }?.let {
                parts.add(formatTechnologyForSpeech(it))
            }
        }
        if (speakBandEnabled) {
            bandPhrase?.takeIf { it.isNotBlank() }?.let(parts::add)
        }
        parts.add(signalState)
        serviceState?.takeIf { it.isNotBlank() }?.let(parts::add)
        return parts.joinToString(", ")
    }

    fun formatNoSignalChange(
        active: Boolean,
        networkOperatorName: String?,
        radioAccessType: String? = null,
        isWifiCallingActive: Boolean = false,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        phrases: VoicePhraseOptions = VoicePhraseOptions(
            speakOperatorName = speakOperatorNameEnabled,
            speakTechnology = speakTechnologyEnabled
        ),
        lteEarfcn: Int? = null,
        nrBand: Int? = null,
        gsmEarfcn: Int? = null,
        bandNamingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT
    ): String {
        val bandPhrase = phrases.bandPhraseFor(
            lteEarfcn,
            nrBand,
            gsmEarfcn,
            namingStyle = bandNamingStyle
        )
        if (active) {
            return formatNoSignalAnnouncement(
                networkOperatorName,
                radioAccessType,
                isWifiCallingActive,
                phrases.speakOperatorName,
                phrases.speakTechnology,
                phrases.speakBand,
                bandPhrase
            )
        }
        return formatSignalRestoredAnnouncement(
            networkOperatorName,
            radioAccessType,
            isWifiCallingActive,
            phrases.speakOperatorName,
            phrases.speakTechnology,
            phrases.speakBand,
            bandPhrase
        )
    }

    fun formatSignalRestoredAnnouncement(
        networkOperatorName: String?,
        radioAccessType: String?,
        isWifiCallingActive: Boolean = false,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        speakBandEnabled: Boolean = false,
        bandPhrase: String? = null
    ): String {
        if (isWifiCallingActive) {
            return joinAnnouncementParts(
                operatorNames = listOf(networkOperatorName),
                serviceState = PHRASE_CELLULAR_SIGNAL_RESTORED,
                speakOperatorNameEnabled = speakOperatorNameEnabled
            )
        }
        return joinAnnouncementParts(
            operatorNames = listOf(networkOperatorName),
            radioAccessType = radioAccessType,
            signalState = PHRASE_SIGNAL_RESTORED,
            speakOperatorNameEnabled = speakOperatorNameEnabled,
            speakTechnologyEnabled = speakTechnologyEnabled,
            speakBandEnabled = speakBandEnabled,
            bandPhrase = bandPhrase
        )
    }

    fun formatNoSignalChange(
        stats: ConnectivityStats,
        lastKnownRadioAccessType: String? = null,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        phrases: VoicePhraseOptions = VoicePhraseOptions(
            speakOperatorName = speakOperatorNameEnabled,
            speakTechnology = speakTechnologyEnabled
        ),
        bandNamingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT
    ): String? {
        return formatNoSignalChange(
            active = stats.noSignalActive,
            networkOperatorName = stats.networkOperatorName,
            radioAccessType = stats.resolveNoSignalAnnouncementRadioAccessType(lastKnownRadioAccessType),
            isWifiCallingActive = stats.isWifiCallingActive,
            phrases = phrases,
            lteEarfcn = stats.lteEarfcn,
            nrBand = stats.nrBand,
            gsmEarfcn = stats.gsmEarfcn,
            bandNamingStyle = bandNamingStyle
        )
    }

    fun formatNoSignalAnnouncement(
        stats: ConnectivityStats,
        lastKnownRadioAccessType: String? = null,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        phrases: VoicePhraseOptions = VoicePhraseOptions(
            speakOperatorName = speakOperatorNameEnabled,
            speakTechnology = speakTechnologyEnabled
        ),
        bandNamingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT
    ): String {
        if (stats.isWifiCallingActive) {
            return formatNoSignalAnnouncement(
                networkOperatorName = stats.networkOperatorName,
                radioAccessType = null,
                isWifiCallingActive = true,
                speakOperatorNameEnabled = phrases.speakOperatorName,
                speakTechnologyEnabled = phrases.speakTechnology
            )
        }
        return formatCampedSignalStateAnnouncement(
            stats = stats,
            lastKnownRadioAccessType = lastKnownRadioAccessType,
            signalState = PHRASE_NO_SIGNAL,
            phrases = phrases,
            serviceState = limitedServiceSpeechPhrase(
                isVisited = stats.resolveLimitedServiceVisitedOperatorName() != null,
                phrases = phrases
            ).takeIf { stats.isLimitedService },
            bandNamingStyle = bandNamingStyle
        )
    }

    fun formatSignalRestoredAnnouncement(
        stats: ConnectivityStats,
        lastKnownRadioAccessType: String? = null,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        phrases: VoicePhraseOptions = VoicePhraseOptions(
            speakOperatorName = speakOperatorNameEnabled,
            speakTechnology = speakTechnologyEnabled
        ),
        bandNamingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT
    ): String {
        if (stats.isWifiCallingActive) {
            return formatSignalRestoredAnnouncement(
                networkOperatorName = stats.networkOperatorName,
                radioAccessType = null,
                isWifiCallingActive = true,
                speakOperatorNameEnabled = phrases.speakOperatorName,
                speakTechnologyEnabled = phrases.speakTechnology
            )
        }
        return formatCampedSignalStateAnnouncement(
            stats = stats,
            lastKnownRadioAccessType = lastKnownRadioAccessType,
            signalState = PHRASE_SIGNAL_RESTORED,
            phrases = phrases,
            bandNamingStyle = bandNamingStyle
        )
    }

    fun formatLimitedServiceChange(
        stats: ConnectivityStats,
        lastKnownRadioAccessType: String? = null,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        phrases: VoicePhraseOptions = VoicePhraseOptions(
            speakOperatorName = speakOperatorNameEnabled,
            speakTechnology = speakTechnologyEnabled
        ),
        bandNamingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT
    ): String {
        if (!stats.isLimitedService && stats.shouldAnnounceInServiceAfterLimited()) {
            return formatInServiceAnnouncement(
                stats,
                lastKnownRadioAccessType,
                phrases,
                bandNamingStyle
            )
        }
        return formatLimitedServiceAnnouncement(
            stats,
            lastKnownRadioAccessType,
            phrases = phrases,
            bandNamingStyle = bandNamingStyle
        )
    }

    fun formatInServiceAnnouncement(
        stats: ConnectivityStats,
        lastKnownRadioAccessType: String? = null,
        phrases: VoicePhraseOptions = VoicePhraseOptions(),
        bandNamingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT
    ): String {
        return joinAnnouncementParts(
            operatorNames = listOf(
                stats.servingNetworkOperatorName ?: stats.networkOperatorName
            ),
            radioAccessType = stats.resolveNoSignalAnnouncementRadioAccessType(
                lastKnownRadioAccessType
            ),
            serviceState = inServiceSpeechPhrase(stats),
            speakOperatorNameEnabled = phrases.speakOperatorName,
            speakTechnologyEnabled = phrases.speakTechnology,
            speakBandEnabled = phrases.speakBand,
            bandPhrase = phrases.bandPhraseFor(
                stats.lteEarfcn,
                stats.nrBand,
                stats.gsmEarfcn,
                namingStyle = bandNamingStyle
            )
        )
    }

    fun previewInService(
        networkOperatorName: String?,
        phrases: VoicePhraseOptions = VoicePhraseOptions(),
        bandNamingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT,
        roaming: Boolean = false
    ): String {
        return joinAnnouncementParts(
            operatorNames = listOf(networkOperatorName),
            radioAccessType = CellularSignalReader.RADIO_4G,
            serviceState = if (roaming) PHRASE_ROAMING_IN_SERVICE else PHRASE_HOME_IN_SERVICE,
            speakOperatorNameEnabled = phrases.speakOperatorName,
            speakTechnologyEnabled = phrases.speakTechnology,
            speakBandEnabled = phrases.speakBand,
            bandPhrase = phrases.bandPhraseFor(PREVIEW_LTE_EARFCN, namingStyle = bandNamingStyle)
        )
    }

    fun isInServiceAnnouncement(message: String?): Boolean {
        val text = message ?: return false
        if (text.contains(PHRASE_LIMITED_SERVICE)) return false
        return text.contains(PHRASE_IN_SERVICE) ||
            text.contains(PHRASE_HOME_IN_SERVICE) ||
            text.contains(PHRASE_ROAMING_IN_SERVICE)
    }

    /** Home vs registered-roaming service phrase for VA-5 (2G and 4G/5G). */
    internal fun inServiceSpeechPhrase(stats: ConnectivityStats): String {
        return when {
            stats.isRegisteredSimRoaming() -> PHRASE_ROAMING_IN_SERVICE
            stats.resolveCampedVisitedOperatorName() == null -> PHRASE_HOME_IN_SERVICE
            else -> PHRASE_IN_SERVICE
        }
    }

    fun formatLimitedServiceAnnouncement(
        homeOperatorName: String?,
        visitedOperatorName: String?,
        radioAccessType: String?,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        speakBandEnabled: Boolean = false,
        bandPhrase: String? = null,
        speakHomeLimitedService: Boolean = VoicePhraseOptions.DEFAULT_SPEAK_HOME_LIMITED_SERVICE,
        speakVisitingLimitedService: Boolean = VoicePhraseOptions.DEFAULT_SPEAK_VISITING_LIMITED_SERVICE
    ): String {
        val parts = mutableListOf<String>()
        if (speakOperatorNameEnabled) {
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
        }
        if (speakTechnologyEnabled) {
            radioAccessType?.takeIf { it.isNotBlank() }?.let {
                parts.add(formatTechnologyForSpeech(it))
            }
        }
        if (speakBandEnabled) {
            bandPhrase?.takeIf { it.isNotBlank() }?.let(parts::add)
        }
        parts.add(
            limitedServiceSpeechPhrase(
                isVisited = visitedOperatorName != null,
                speakHomeLimitedService = speakHomeLimitedService,
                speakVisitingLimitedService = speakVisitingLimitedService
            )
        )
        return parts.joinToString(", ")
    }

    fun formatLimitedServiceAnnouncement(
        networkOperatorName: String?,
        radioAccessType: String?,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        speakBandEnabled: Boolean = false,
        bandPhrase: String? = null,
        speakHomeLimitedService: Boolean = VoicePhraseOptions.DEFAULT_SPEAK_HOME_LIMITED_SERVICE,
        speakVisitingLimitedService: Boolean = VoicePhraseOptions.DEFAULT_SPEAK_VISITING_LIMITED_SERVICE
    ): String {
        return formatLimitedServiceAnnouncement(
            homeOperatorName = networkOperatorName,
            visitedOperatorName = null,
            radioAccessType = radioAccessType,
            speakOperatorNameEnabled = speakOperatorNameEnabled,
            speakTechnologyEnabled = speakTechnologyEnabled,
            speakBandEnabled = speakBandEnabled,
            bandPhrase = bandPhrase,
            speakHomeLimitedService = speakHomeLimitedService,
            speakVisitingLimitedService = speakVisitingLimitedService
        )
    }

    fun formatLimitedServiceAnnouncement(
        stats: ConnectivityStats,
        lastKnownRadioAccessType: String? = null,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        phrases: VoicePhraseOptions = VoicePhraseOptions(
            speakOperatorName = speakOperatorNameEnabled,
            speakTechnology = speakTechnologyEnabled
        ),
        bandNamingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT
    ): String {
        return formatLimitedServiceAnnouncement(
            homeOperatorName = stats.homeNetworkOperatorName ?: stats.networkOperatorName,
            visitedOperatorName = stats.resolveLimitedServiceVisitedOperatorName(),
            radioAccessType = stats.resolveNoSignalAnnouncementRadioAccessType(lastKnownRadioAccessType),
            speakOperatorNameEnabled = phrases.speakOperatorName,
            speakTechnologyEnabled = phrases.speakTechnology,
            speakBandEnabled = phrases.speakBand,
            bandPhrase = phrases.bandPhraseFor(
                stats.lteEarfcn,
                stats.nrBand,
                stats.gsmEarfcn,
                namingStyle = bandNamingStyle
            ),
            speakHomeLimitedService = phrases.speakHomeLimitedService,
            speakVisitingLimitedService = phrases.speakVisitingLimitedService
        )
    }

    internal fun limitedServiceSpeechPhrase(
        isVisited: Boolean,
        phrases: VoicePhraseOptions
    ): String {
        return limitedServiceSpeechPhrase(
            isVisited = isVisited,
            speakHomeLimitedService = phrases.speakHomeLimitedService,
            speakVisitingLimitedService = phrases.speakVisitingLimitedService
        )
    }

    internal fun limitedServiceSpeechPhrase(
        isVisited: Boolean,
        speakHomeLimitedService: Boolean,
        speakVisitingLimitedService: Boolean
    ): String {
        return when {
            isVisited && speakVisitingLimitedService -> PHRASE_VISITING_LIMITED_SERVICE
            !isVisited && speakHomeLimitedService -> PHRASE_HOME_LIMITED_SERVICE
            else -> PHRASE_LIMITED_SERVICE
        }
    }

    /** Spoken when the phone camps on 2G after losing LTE/NR signal (operator + tech; 2G is not implicit full service). */
    fun formatG2CampedAnnouncement(
        networkOperatorName: String?,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        phrases: VoicePhraseOptions = VoicePhraseOptions(
            speakOperatorName = speakOperatorNameEnabled,
            speakTechnology = speakTechnologyEnabled
        ),
        lteEarfcn: Int? = null,
        nrBand: Int? = null,
        gsmEarfcn: Int? = null,
        bandNamingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT
    ): String {
        return joinAnnouncementParts(
            operatorNames = listOf(networkOperatorName),
            radioAccessType = CellularSignalReader.RADIO_2G,
            speakOperatorNameEnabled = phrases.speakOperatorName,
            speakTechnologyEnabled = phrases.speakTechnology,
            speakBandEnabled = phrases.speakBand,
            bandPhrase = phrases.bandPhraseFor(
                lteEarfcn,
                nrBand,
                gsmEarfcn,
                namingStyle = bandNamingStyle
            )
        )
    }

    /** Spoken after LTE/NR no signal while the phone is still scanning for 2G. */
    fun formatSearching2gAnnouncement(
        networkOperatorName: String?,
        lastKnownLteNrRadioAccessType: String?,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        phrases: VoicePhraseOptions = VoicePhraseOptions(
            speakOperatorName = speakOperatorNameEnabled,
            speakTechnology = speakTechnologyEnabled
        ),
        lteEarfcn: Int? = null,
        nrBand: Int? = null,
        gsmEarfcn: Int? = null,
        bandNamingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT
    ): String {
        return joinAnnouncementParts(
            operatorNames = listOf(networkOperatorName),
            radioAccessType = lastKnownLteNrRadioAccessType,
            signalState = PHRASE_NO_SIGNAL,
            serviceState = PHRASE_SEARCHING_2G,
            speakOperatorNameEnabled = phrases.speakOperatorName,
            speakTechnologyEnabled = phrases.speakTechnology,
            speakBandEnabled = phrases.speakBand,
            bandPhrase = phrases.bandPhraseFor(
                lteEarfcn,
                nrBand,
                gsmEarfcn,
                namingStyle = bandNamingStyle
            )
        )
    }

    fun formatDeadzoneAnnouncement(
        networkOperatorName: String?,
        speakOperatorNameEnabled: Boolean = true
    ): String {
        return joinAnnouncementParts(
            operatorNames = listOf(networkOperatorName),
            serviceState = PHRASE_DEADZONE,
            speakOperatorNameEnabled = speakOperatorNameEnabled
        )
    }

    /** VA-20 — reminder that metrics are simulated, not live radio. */
    fun formatMockNetworkAnnouncement(): String = PHRASE_MOCK_NETWORK

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
        serviceState: String? = null,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        speakBandEnabled: Boolean = false,
        bandPhrase: String? = null
    ): String {
        val parts = mutableListOf<String>()
        if (speakOperatorNameEnabled) {
            operatorNames.forEach { name ->
                NetworkOperatorSpeech.formatForSpeech(name)?.let { spoken ->
                    if (parts.none { it.equals(spoken, ignoreCase = true) }) {
                        parts.add(spoken)
                    }
                }
            }
        }
        if (speakTechnologyEnabled) {
            radioAccessType?.takeIf { it.isNotBlank() }?.let {
                parts.add(formatTechnologyForSpeech(it))
            }
        }
        if (speakBandEnabled) {
            bandPhrase?.takeIf { it.isNotBlank() }?.let(parts::add)
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
