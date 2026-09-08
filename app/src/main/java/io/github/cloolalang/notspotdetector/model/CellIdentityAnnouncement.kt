package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader

object CellIdentityAnnouncement {

    private val PREVIEW_BODY: String
        get() = "Cell reselect, channel ${SpeechDigits.format(6400)}, PCI ${SpeechDigits.format(123)}"

    fun previewText(networkOperatorName: String?): String {
        return prefixNetworkOperator(PREVIEW_BODY, networkOperatorName)
    }

    fun format(
        previous: CellIdentitySnapshot,
        next: CellIdentitySnapshot,
        radioAccessType: String?,
        networkOperatorName: String? = null
    ): String {
        if (previous == next) return ""

        val parts = mutableListOf("Cell reselect")
        val lteChanged = lteIdentityChanged(previous, next)
        val nrChanged = nrIdentityChanged(previous, next)
        val gsmChanged = gsmIdentityChanged(previous, next)
        val useLtePrefix = radioAccessType == CellularSignalReader.RADIO_5G_ENDC ||
            (lteChanged && nrChanged)

        if (lteChanged && hasLteIdentity(next)) {
            parts.add(formatRatIdentity(useLtePrefix, next.lteEarfcn, next.ltePci))
        }
        if (nrChanged && hasNrIdentity(next)) {
            parts.add(formatRatIdentity(prefix = true, next.nrEarfcn, next.nrPci, ratLabel = "NR"))
        }
        if (gsmChanged && hasGsmIdentity(next)) {
            parts.add(formatGsmIdentity(next.gsmEarfcn, next.gsmBsic))
        }

        if (parts.size == 1) {
            appendFallbackIdentity(parts, next, radioAccessType)
        }

        return prefixNetworkOperator(parts.joinToString(", "), networkOperatorName)
    }

    private fun prefixNetworkOperator(message: String, networkOperatorName: String?): String {
        val name = NetworkOperatorSpeech.formatForSpeech(networkOperatorName) ?: return message
        return "$name, $message"
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
            earfcn?.let { append("ARFCN ${SpeechDigits.format(it)}") }
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
