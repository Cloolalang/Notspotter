package io.github.cloolalang.notspotdetector.model

/**
 * Per-VA phrase prefix toggles: operator, then technology, then band (when enabled).
 */
data class VoicePhraseOptions(
    val speakOperatorName: Boolean = DEFAULT_SPEAK_OPERATOR_NAME,
    val speakTechnology: Boolean = DEFAULT_SPEAK_TECHNOLOGY,
    val speakBand: Boolean = DEFAULT_SPEAK_BAND
) {
    fun withOperator(enabled: Boolean) = copy(speakOperatorName = enabled)
    fun withTechnology(enabled: Boolean) = copy(speakTechnology = enabled)
    fun withBand(enabled: Boolean) = copy(speakBand = enabled)

    fun bandPhraseFor(lteEarfcn: Int?, nrBand: Int? = null, gsmEarfcn: Int? = null): String? {
        if (!speakBand) return null
        return CellIdentityAnnouncement.prefixBandPhrase(
            lteEarfcn = lteEarfcn,
            nrBand = nrBand,
            gsmEarfcn = gsmEarfcn
        )
    }

    companion object {
        const val DEFAULT_SPEAK_OPERATOR_NAME = true
        const val DEFAULT_SPEAK_TECHNOLOGY = true
        const val DEFAULT_SPEAK_BAND = false
    }
}

enum class VoicePhraseGroup {
    CELL_CHANGE,
    TECH_CHANGE_TO_2G,
    TECH_CHANGE_TO_4G,
    TECH_CHANGE_TO_5G_ENDC,
    SIGNAL_LOW,
    NO_SIGNAL,
    LIMITED_SERVICE;

    companion object {
        fun forTechnologyChange(target: TechnologyChangeTarget): VoicePhraseGroup {
            return when (target) {
                TechnologyChangeTarget.TO_2G -> TECH_CHANGE_TO_2G
                TechnologyChangeTarget.TO_4G -> TECH_CHANGE_TO_4G
                TechnologyChangeTarget.TO_5G_ENDC -> TECH_CHANGE_TO_5G_ENDC
            }
        }
    }
}

enum class VoicePhraseFragment {
    OPERATOR,
    TECHNOLOGY,
    BAND
}
