package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader

object CellIdentityAnnouncement {

    private const val PREVIEW_LTE_EARFCN = 6400

    private val PREVIEW_IDENTITY: String
        get() = "channel ${SpeechDigits.format(PREVIEW_LTE_EARFCN)}, PCI ${SpeechDigits.format(123)}"

    fun previewText(
        networkOperatorName: String?,
        speakBandEnabled: Boolean = false,
        bandNamingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        prefixPhrases: VoicePhraseOptions = VoicePhraseOptions(
            speakOperatorName = speakOperatorNameEnabled,
            speakTechnology = speakTechnologyEnabled
        )
    ): String {
        val bandPhrase = if (speakBandEnabled) formatBandPhrase(PREVIEW_LTE_EARFCN, bandNamingStyle) else null
        return formatAnnouncementBody(
            networkOperatorName = networkOperatorName,
            radioAccessType = CellularSignalReader.RADIO_4G,
            identityBody = bandPhrase ?: PREVIEW_IDENTITY,
            includeCellReselectPrefix = bandPhrase == null,
            speakOperatorNameEnabled = prefixPhrases.speakOperatorName,
            speakTechnologyEnabled = prefixPhrases.speakTechnology,
            prefixSpeakBandEnabled = prefixPhrases.speakBand,
            prefixBandPhrase = prefixPhrases.bandPhraseFor(PREVIEW_LTE_EARFCN)
        )
    }

    fun format(
        previous: CellIdentitySnapshot,
        next: CellIdentitySnapshot,
        radioAccessType: String?,
        networkOperatorName: String? = null,
        campedOnVisitedOperator: Boolean = false,
        speakBandEnabled: Boolean = false,
        bandNamingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        prefixPhrases: VoicePhraseOptions = VoicePhraseOptions(
            speakOperatorName = speakOperatorNameEnabled,
            speakTechnology = speakTechnologyEnabled
        )
    ): String {
        if (previous == next) return ""

        val bandPhrase = if (speakBandEnabled) formatBandPhrase(next.lteEarfcn, bandNamingStyle) else null
        if (bandPhrase != null) {
            return formatAnnouncementBody(
                networkOperatorName = networkOperatorName,
                radioAccessType = radioAccessType,
                identityBody = bandPhrase,
                campedOnVisitedOperator = campedOnVisitedOperator,
                includeCellReselectPrefix = false,
                speakOperatorNameEnabled = prefixPhrases.speakOperatorName,
                speakTechnologyEnabled = prefixPhrases.speakTechnology,
                prefixSpeakBandEnabled = prefixPhrases.speakBand,
                prefixBandPhrase = prefixPhrases.bandPhraseFor(next.lteEarfcn, next.nrBand)
            )
        }

        val identityParts = mutableListOf<String>()
        val lteChanged = lteIdentityChanged(previous, next)
        val nrChanged = nrIdentityChanged(previous, next)
        val gsmChanged = gsmIdentityChanged(previous, next)
        val useLtePrefix = radioAccessType == CellularSignalReader.RADIO_5G_ENDC ||
            (lteChanged && nrChanged)

        if (lteChanged && hasLteIdentity(next)) {
            identityParts.add(formatRatIdentity(useLtePrefix, next.lteEarfcn, next.ltePci))
        }
        if (nrChanged && hasNrIdentity(next)) {
            identityParts.add(formatRatIdentity(prefix = true, next.nrEarfcn, next.nrPci, ratLabel = "NR"))
        }
        if (gsmChanged && hasGsmIdentity(next)) {
            identityParts.add(formatGsmIdentity(next.gsmEarfcn, next.gsmBsic))
        }

        if (identityParts.isEmpty()) {
            appendFallbackIdentity(identityParts, next, radioAccessType)
        }

        return formatAnnouncementBody(
            networkOperatorName = networkOperatorName,
            radioAccessType = radioAccessType,
            identityBody = identityParts.joinToString(", "),
            campedOnVisitedOperator = campedOnVisitedOperator,
            speakOperatorNameEnabled = prefixPhrases.speakOperatorName,
            speakTechnologyEnabled = prefixPhrases.speakTechnology,
            prefixSpeakBandEnabled = prefixPhrases.speakBand,
            prefixBandPhrase = prefixPhrases.bandPhraseFor(next.lteEarfcn, next.nrBand)
        )
    }

    /**
     * RXSS 9 alternative phrasing — "band, &lt;number in words&gt;" (option A, e.g. "band, twenty")
     * or "band, &lt;MHz nickname in words, no "L" prefix&gt;" (option B, e.g. "band, eight hundred"
     * for L800, "band, twenty-six hundred" for L2600), derived from the LTE EARFCN. Numbers are
     * spoken as whole words (not digit-by-digit) so they read naturally, and a comma after "band"
     * forces a short TTS pause — without it, "band" run straight into a number can be
     * clipped/mumbled by some TTS engines (e.g. sounding like "bunt"). Returns null when there is
     * no LTE EARFCN to map (2G-only or NR-only reselect), so callers fall back to the normal
     * channel/PCI phrasing.
     */
    fun formatBandPhrase(
        lteEarfcn: Int?,
        namingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.DEFAULT
    ): String? {
        val earfcn = lteEarfcn ?: return null
        val bandInfo = EutraBand.forEarfcn(earfcn) ?: return null
        val spokenBand = when (namingStyle) {
            CellReselectBandNamingStyle.BAND_NUMBER -> NumberWords.toWords(bandInfo.band)
            CellReselectBandNamingStyle.MHZ_NICKNAME -> formatMhzNicknameForSpeech(bandInfo.mhzNickname)
        }
        return "band, $spokenBand"
    }

    /**
     * Prefix band phrase spoken after operator and technology: "band, twenty" (LTE EARFCN) or
     * "band, seventy eight" (NR operating band).
     */
    fun prefixBandPhrase(
        lteEarfcn: Int?,
        nrBand: Int? = null,
        namingStyle: CellReselectBandNamingStyle = CellReselectBandNamingStyle.BAND_NUMBER
    ): String? {
        formatBandPhrase(lteEarfcn, namingStyle)?.let { return it }
        val band = nrBand ?: return null
        return "band, ${NumberWords.toWords(band)}"
    }

    /**
     * Converts a nickname like "L800" or "L2600" into natural spoken hundreds — "eight hundred" or
     * "twenty-six hundred" — dropping the leading "L" (it reads better without it, e.g. "band,
     * eight hundred" rather than "band, L eight hundred").
     */
    private fun formatMhzNicknameForSpeech(nickname: String): String {
        val letterPart = nickname.takeWhile { it.isLetter() }
        val digitPart = nickname.drop(letterPart.length)
        return digitPart.toIntOrNull()?.let { NumberWords.toHundredsWords(it) } ?: digitPart
    }

    private fun formatAnnouncementBody(
        networkOperatorName: String?,
        radioAccessType: String?,
        identityBody: String,
        campedOnVisitedOperator: Boolean = false,
        includeCellReselectPrefix: Boolean = true,
        speakOperatorNameEnabled: Boolean = true,
        speakTechnologyEnabled: Boolean = true,
        prefixSpeakBandEnabled: Boolean = false,
        prefixBandPhrase: String? = null
    ): String {
        val operatorSpoken = NetworkOperatorSpeech.formatForSpeech(networkOperatorName)?.let { spoken ->
            if (campedOnVisitedOperator) {
                "$spoken ${SignalStateAnnouncement.PHRASE_VISITED_OPERATOR_ROLE}"
            } else {
                spoken
            }
        }
        val detail = if (includeCellReselectPrefix) {
            listOf("cell reselect", identityBody).filter { it.isNotBlank() }.joinToString(", ")
        } else {
            identityBody
        }
        return SignalStateAnnouncement.joinAnnouncementParts(
            operatorNames = listOf(operatorSpoken),
            radioAccessType = radioAccessType,
            serviceState = detail,
            speakOperatorNameEnabled = speakOperatorNameEnabled,
            speakTechnologyEnabled = speakTechnologyEnabled,
            speakBandEnabled = prefixSpeakBandEnabled,
            bandPhrase = prefixBandPhrase
        )
    }

    private fun appendFallbackIdentity(
        parts: MutableList<String>,
        next: CellIdentitySnapshot,
        radioAccessType: String?
    ) {
        when {
            hasNrIdentity(next) && radioAccessType?.contains("5G") == true -> {
                val prefixNr = radioAccessType == CellularSignalReader.RADIO_5G_ENDC
                parts.add(formatRatIdentity(prefixNr, next.nrEarfcn, next.nrPci, ratLabel = "NR"))
            }
            hasLteIdentity(next) -> {
                parts.add(formatRatIdentity(prefix = false, next.lteEarfcn, next.ltePci))
            }
            hasGsmIdentity(next) -> {
                parts.add(formatGsmIdentity(next.gsmEarfcn, next.gsmBsic))
            }
        }
    }

    private fun formatRatIdentity(
        prefix: Boolean,
        earfcn: Int?,
        pci: Int?,
        ratLabel: String = "LTE"
    ): String {
        return buildString {
            if (prefix) {
                append(ratLabel)
                append(' ')
            }
            earfcn?.let {
                append("channel ${SpeechDigits.format(it)}")
            }
            pci?.let {
                if (earfcn != null) append(", ")
                append("PCI ${SpeechDigits.format(it)}")
            }
        }
    }

    private fun formatGsmIdentity(earfcn: Int?, bsic: Int?): String {
        return buildString {
            earfcn?.let { append("channel ${SpeechDigits.format(it)}") }
            bsic?.let {
                if (earfcn != null) append(", ")
                append("BSIC ${SpeechDigits.format(it)}")
            }
        }
    }

    private fun lteIdentityChanged(previous: CellIdentitySnapshot, next: CellIdentitySnapshot): Boolean {
        return previous.lteEarfcn != next.lteEarfcn || previous.ltePci != next.ltePci
    }

    private fun nrIdentityChanged(previous: CellIdentitySnapshot, next: CellIdentitySnapshot): Boolean {
        return previous.nrEarfcn != next.nrEarfcn || previous.nrPci != next.nrPci
    }

    private fun gsmIdentityChanged(previous: CellIdentitySnapshot, next: CellIdentitySnapshot): Boolean {
        return previous.gsmEarfcn != next.gsmEarfcn || previous.gsmBsic != next.gsmBsic
    }

    private fun hasLteIdentity(snapshot: CellIdentitySnapshot): Boolean {
        return snapshot.lteEarfcn != null || snapshot.ltePci != null
    }

    private fun hasNrIdentity(snapshot: CellIdentitySnapshot): Boolean {
        return snapshot.nrEarfcn != null || snapshot.nrPci != null
    }

    private fun hasGsmIdentity(snapshot: CellIdentitySnapshot): Boolean {
        return snapshot.gsmEarfcn != null || snapshot.gsmBsic != null
    }
}
