package io.github.cloolalang.notspotdetector.model

/**
 * Downlink bandwidth of the carriers the modem is using.
 * [android.telephony.PhysicalChannelConfig] labels each carrier primary or secondary.
 * Values are kHz. A carrier of 20 MHz is 20_000.
 */
data class CarrierBandwidths(
    val primaryDownlinkKhz: List<Int> = emptyList(),
    val secondaryDownlinkKhz: List<Int> = emptyList()
) {
    fun hasAny(): Boolean = primaryDownlinkKhz.isNotEmpty() || secondaryDownlinkKhz.isNotEmpty()
}

/** One reported carrier. [connectionStatus] uses the Android primary/secondary/none values. */
data class ChannelBandwidth(
    val connectionStatus: Int,
    val downlinkKhz: Int,
    val radio: CarrierRadio = CarrierRadio.UNKNOWN
)

enum class CarrierRadio {
    LTE,
    NR,
    UNKNOWN
}

object CarrierBandwidth {
    /** [android.telephony.PhysicalChannelConfig.CONNECTION_PRIMARY_SERVING]. */
    const val CONNECTION_PRIMARY = 1

    /** [android.telephony.PhysicalChannelConfig.CONNECTION_SECONDARY_SERVING]. */
    const val CONNECTION_SECONDARY = 2

    /** [android.telephony.CellInfo.CELL_CONNECTION_NONE]. A neighbour, not a carrier in use. */
    const val CONNECTION_NONE = 0

    fun isReportedKhz(khz: Int): Boolean = khz in MIN_KHZ..MAX_KHZ

    /**
     * Primary serving carriers stay on the primary row, including EN-DC when both the LTE
     * anchor and the NR leg are marked primary. A carrier with an unknown status fills a
     * missing primary, then any further unknown carriers are shown as secondary.
     * Neighbours ([CONNECTION_NONE]) are left out.
     */
    fun fromChannels(channels: List<ChannelBandwidth>): CarrierBandwidths {
        val reported = channels.filter {
            it.connectionStatus != CONNECTION_NONE && isReportedKhz(it.downlinkKhz)
        }
        val primary = reported
            .filter { it.connectionStatus == CONNECTION_PRIMARY }
            .map { it.downlinkKhz }
            .distinct()
        val secondary = reported
            .filter { it.connectionStatus == CONNECTION_SECONDARY }
            .map { it.downlinkKhz }
            .distinct()
        val unknown = reported
            .filter {
                it.connectionStatus != CONNECTION_PRIMARY &&
                    it.connectionStatus != CONNECTION_SECONDARY
            }
            .map { it.downlinkKhz }
            .distinct()
        val primaryOut = primary.ifEmpty { unknown.take(1) }
        val leftoverUnknown = if (primary.isEmpty()) unknown.drop(1) else unknown
        return CarrierBandwidths(
            primaryDownlinkKhz = primaryOut,
            secondaryDownlinkKhz = (secondary + leftoverUnknown).distinct()
        )
    }

    /**
     * Physical-channel bandwidth wins where it is labeled. LTE cell identity fills gaps.
     * An unlabeled [android.telephony.ServiceState.getCellBandwidths] array is used only
     * when neither source named a carrier.
     */
    fun merge(
        physical: CarrierBandwidths,
        lteCell: CarrierBandwidths,
        unlabeledKhz: List<Int>
    ): CarrierBandwidths {
        val primary = (physical.primaryDownlinkKhz + lteCell.primaryDownlinkKhz).distinct()
        val secondary = (physical.secondaryDownlinkKhz + lteCell.secondaryDownlinkKhz).distinct()
        if (primary.isNotEmpty() || secondary.isNotEmpty()) {
            return CarrierBandwidths(primary, secondary)
        }
        return fromChannels(
            unlabeledKhz.map {
                ChannelBandwidth(connectionStatus = CONNECTION_UNKNOWN, downlinkKhz = it)
            }
        )
    }

    /** 20_000 kHz is "20 MHz". 1_400 kHz is "1.4 MHz". */
    fun formatMhz(khz: Int): String {
        val whole = khz / 1_000
        val remainder = khz % 1_000
        if (remainder == 0) return "$whole MHz"
        return "$whole.${remainder / 100} MHz"
    }

    fun formatList(khz: List<Int>): String? {
        if (khz.isEmpty()) return null
        return khz.joinToString(", ") { formatMhz(it) }
    }

    /**
     * Sum of the primary and secondary widths shown in cellular metrics.
     * A width the modem repeats while data is moving counts once. A secondary
     * width that is only on the LTE cell still counts when the modem report
     * lists the primary alone. An unlabeled list is used only when neither
     * source named a carrier.
     */
    fun aggregatedKhz(
        physical: List<ChannelBandwidth>,
        lteCell: List<ChannelBandwidth>,
        unlabeledKhz: List<Int>
    ): Int? {
        val merged = merge(
            physical = fromChannels(physical),
            lteCell = fromChannels(lteCell),
            unlabeledKhz = unlabeledKhz
        )
        val total = (merged.primaryDownlinkKhz + merged.secondaryDownlinkKhz).sum()
        return total.takeIf { it > 0 }
    }

    private const val CONNECTION_UNKNOWN = -1
    private const val MIN_KHZ = 200
    private const val MAX_KHZ = 400_000
}
