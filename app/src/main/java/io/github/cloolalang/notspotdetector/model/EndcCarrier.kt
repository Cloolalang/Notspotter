package io.github.cloolalang.notspotdetector.model

import io.github.cloolalang.notspotdetector.network.CellularSignalReader

/**
 * NR channel reported by the modem for a cell it is actually using.
 * On EN-DC, Samsung often leaves this out of the LTE [android.telephony.CellInfo] row.
 */
data class NrCarrierReading(
    val earfcn: Int? = null,
    val pci: Int? = null,
    val band: Int? = null
)

data class EndcCarrierFill(
    val radioAccessType: String?,
    val nrEarfcn: Int?,
    val nrPci: Int?,
    val nrBand: Int?
)

object EndcCarrier {
    /** 3GPP LTE EARFCN does not go above this. A higher downlink channel is NR-ARFCN. */
    const val LTE_EARFCN_MAX = 262_143

    /** [android.telephony.TelephonyManager.NETWORK_TYPE_NR]. */
    const val NETWORK_TYPE_NR = 20

    /** [android.telephony.TelephonyManager.NETWORK_TYPE_UNKNOWN]. */
    const val NETWORK_TYPE_UNKNOWN = 0

    /**
     * True when a modem channel is the NR leg. Samsung EN-DC often reports that leg with
     * network type unknown, or with an NR band, while the downlink channel is still the NR-ARFCN.
     */
    fun isNrDownlink(networkType: Int, downlinkChannel: Int, band: Int?): Boolean {
        if (networkType == NETWORK_TYPE_NR) return true
        if (downlinkChannel in (LTE_EARFCN_MAX + 1)..NR_ARFCN_MAX) return true
        if (band != null && (band in NR_ONLY_BANDS || band >= 257)) return true
        return networkType == NETWORK_TYPE_UNKNOWN &&
            downlinkChannel in NR_ARFCN_MIN..LTE_EARFCN_MAX
    }

    private const val NR_ARFCN_MIN = 123_400
    private const val NR_ARFCN_MAX = 3_279_165
    private val NR_ONLY_BANDS = setOf(77, 78, 79)
    /**
     * Signal strength on EN-DC is often LTE-only, because NR SS-RSRP is missing.
     * A cell reading that already found the NR leg should still be shown as EN-DC.
     */
    fun mergeRadioAccessType(fromSignal: String?, fromCell: String?): String? {
        if (fromCell == CellularSignalReader.RADIO_5G_ENDC &&
            (fromSignal == null ||
                fromSignal == CellularSignalReader.RADIO_4G ||
                fromSignal == CellularSignalReader.RADIO_2G)
        ) {
            return CellularSignalReader.RADIO_5G_ENDC
        }
        if (fromCell in LTE_NR_RADIO_TYPES &&
            (fromSignal == null || fromSignal == CellularSignalReader.RADIO_2G)
        ) {
            return fromCell
        }
        return fromSignal ?: fromCell
    }

    /**
     * Fills a missing NR EARFCN/PCI from the modem's own NR channel and labels an LTE camp
     * as EN-DC. An existing CellInfo NR identity is left as it is. 2G is left unchanged.
     */
    fun fillFromPhysicalChannel(
        radioAccessType: String?,
        hasLteCamp: Boolean,
        nrEarfcn: Int?,
        nrPci: Int?,
        nrBand: Int?,
        physical: NrCarrierReading?
    ): EndcCarrierFill {
        if (radioAccessType == CellularSignalReader.RADIO_2G ||
            physical == null ||
            (physical.earfcn == null && physical.pci == null)
        ) {
            return EndcCarrierFill(radioAccessType, nrEarfcn, nrPci, nrBand)
        }
        val earfcn = nrEarfcn ?: physical.earfcn
        val pci = nrPci ?: physical.pci
        val band = nrBand ?: physical.band
        val radio = when (radioAccessType) {
            CellularSignalReader.RADIO_5G,
            CellularSignalReader.RADIO_5G_ENDC -> radioAccessType
            else -> if (hasLteCamp || radioAccessType == CellularSignalReader.RADIO_4G) {
                CellularSignalReader.RADIO_5G_ENDC
            } else {
                CellularSignalReader.RADIO_5G
            }
        }
        return EndcCarrierFill(radio, earfcn, pci, band)
    }

    /**
     * NR leg state from a [android.telephony.NetworkRegistrationInfo] text dump, such as
     * `nrState=CONNECTED`. The getter is hidden, but the text is public on Android 11+.
     * Null when the dump does not carry an NR state.
     */
    fun nrStateFrom(registrationText: String): String? {
        return NR_STATE_PATTERN.find(registrationText)?.groupValues?.get(1)?.uppercase()
    }

    /**
     * Labels an LTE camp as EN-DC when the phone says the NR leg is up but the cell scan,
     * signal strength, and modem channel list all left it out. Any one sign is enough:
     * NR state connected, the status-bar 5G icon, or a data network type of NR.
     */
    fun promoteToEndc(
        radioAccessType: String?,
        nrState: String?,
        nrNsaIcon: Boolean,
        dataNetworkType: Int = NETWORK_TYPE_UNKNOWN
    ): String? {
        if (radioAccessType != CellularSignalReader.RADIO_4G) return radioAccessType
        val nrUp = nrState == NR_STATE_CONNECTED || nrNsaIcon || dataNetworkType == NETWORK_TYPE_NR
        return if (nrUp) CellularSignalReader.RADIO_5G_ENDC else radioAccessType
    }

    const val NR_STATE_CONNECTED = "CONNECTED"
    private val NR_STATE_PATTERN = Regex("""nrState\s*=\s*([A-Za-z_]+)""")

    private val LTE_NR_RADIO_TYPES = setOf(
        CellularSignalReader.RADIO_4G,
        CellularSignalReader.RADIO_5G,
        CellularSignalReader.RADIO_5G_ENDC
    )
}
