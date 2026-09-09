package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader

object CellIdentityAnnouncement {

    private val PREVIEW_IDENTITY: String
        get() = "channel ${SpeechDigits.format(6400)}, PCI ${SpeechDigits.format(123)}"

    fun previewText(networkOperatorName: String?): String {
        return formatAnnouncementBody(
            networkOperatorName = networkOperatorName,
            radioAccessType = CellularSignalReader.RADIO_4G,
            identityBody = PREVIEW_IDENTITY
        )
    }

    fun format(
        previous: CellIdentitySnapshot,
        next: CellIdentitySnapshot,
        radioAccessType: String?,
        networkOperatorName: String? = null,
        campedOnVisitedOperator: Boolean = false
    ): String {
        if (previous == next) return ""

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
            campedOnVisitedOperator = campedOnVisitedOperator
        )
    }

    private fun formatAnnouncementBody(
        networkOperatorName: String?,
        radioAccessType: String?,
        identityBody: String,
        campedOnVisitedOperator: Boolean = false
    ): String {
        val operatorSpoken = NetworkOperatorSpeech.formatForSpeech(networkOperatorName)?.let { spoken ->
            if (campedOnVisitedOperator) {
                "$spoken ${SignalStateAnnouncement.PHRASE_VISITED_OPERATOR_ROLE}"
            } else {
                spoken
            }
        }
        val detail = listOf("cell reselect", identityBody).filter { it.isNotBlank() }.joinToString(", ")
        return SignalStateAnnouncement.joinAnnouncementParts(
            operatorNames = listOf(operatorSpoken),
            radioAccessType = radioAccessType,
            serviceState = detail
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
