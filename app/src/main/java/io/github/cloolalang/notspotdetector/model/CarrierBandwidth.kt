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
    val radio: CarrierRadio = CarrierRadio.UNKNOWN,
    /** Downlink EARFCN or NR-ARFCN, when the report named the channel. */
    val channelNumber: Int? = null,
    /** True for the cell the phone is camped on. Neighbours stay false. */
    val registered: Boolean = false
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
        val reported = dedupe(
            channels.filter {
                it.connectionStatus != CONNECTION_NONE && isReportedKhz(it.downlinkKhz)
            }
        )
        val markedPrimary = reported.filter {
            it.connectionStatus == CONNECTION_PRIMARY && it.radio != CarrierRadio.NR
        }
        val lteLike = reported.filter { it.radio != CarrierRadio.NR }
        val primaryCarriers = when {
            markedPrimary.isNotEmpty() -> markedPrimary
            lteLike.isNotEmpty() -> listOf(lteLike.first())
            else -> emptyList()
        }
        val secondaryCarriers = reported.filter { it !in primaryCarriers }
        return CarrierBandwidths(
            primaryDownlinkKhz = primaryCarriers.map { it.downlinkKhz },
            secondaryDownlinkKhz = secondaryCarriers.map { it.downlinkKhz }
        )
    }

    /**
     * The modem carrier on the camped EARFCN is the primary, even when the phone
     * labelled a different channel as primary. Cell-identity bandwidth for that
     * same channel is often the wider value this phone showed before a band lock.
     */
    fun campedFirst(
        channels: List<ChannelBandwidth>,
        campedChannel: Int?
    ): List<ChannelBandwidth> {
        if (campedChannel == null || campedChannel <= 0) return channels
        val match = channels.firstOrNull {
            it.channelNumber == campedChannel &&
                it.connectionStatus != CONNECTION_NONE &&
                isReportedKhz(it.downlinkKhz)
        } ?: return channels
        val others = channels.filter { it.channelNumber != campedChannel }.map { channel ->
            if (channel.connectionStatus == CONNECTION_PRIMARY) {
                channel.copy(connectionStatus = CONNECTION_SECONDARY)
            } else {
                channel
            }
        }
        return listOf(match.copy(connectionStatus = CONNECTION_PRIMARY)) + others
    }

    /** A physical-channel list has no neighbours. Status 0 there means unlabelled, not unused. */
    fun physicalConnectionStatus(status: Int): Int {
        return if (status == CONNECTION_NONE) CONNECTION_UNKNOWN else status
    }

    /**
     * Carriers the phone is actually using from a cell scan. Labelled primary and
     * secondary cells count. Otherwise only the registered cell. A scan full of
     * unlabelled neighbours is not a carrier list.
     */
    fun lteServingChannels(channels: List<ChannelBandwidth>): List<ChannelBandwidth> {
        val valid = channels.filter {
            it.connectionStatus != CONNECTION_NONE && isReportedKhz(it.downlinkKhz)
        }
        val registered = valid.filter { it.registered }
        if (registered.isNotEmpty()) {
            val campedChannels = registered.mapNotNull { it.channelNumber }.toSet()
            val camped = registered.map { it.copy(connectionStatus = CONNECTION_PRIMARY) }
            val extra = valid.filter {
                !it.registered &&
                    it.connectionStatus == CONNECTION_SECONDARY &&
                    (it.channelNumber == null || it.channelNumber !in campedChannels)
            }
            return camped + extra
        }
        val labeled = valid.filter {
            it.connectionStatus == CONNECTION_PRIMARY || it.connectionStatus == CONNECTION_SECONDARY
        }
        if (labeled.isNotEmpty()) return labeled
        return if (valid.size == 1) valid else emptyList()
    }

    /**
     * The modem's physical-channel downlink list is the carrier list when it names one.
     * A missing primary is filled from the LTE cell. Cell secondaries are not added on
     * top of a modem report. The LTE cell, then an unlabeled list, is used only when
     * the modem named no carrier.
     */
    fun merge(
        physical: CarrierBandwidths,
        lteCell: CarrierBandwidths,
        unlabeledKhz: List<Int>
    ): CarrierBandwidths {
        if (physical.hasAny()) {
            return CarrierBandwidths(
                primaryDownlinkKhz = physical.primaryDownlinkKhz.ifEmpty { lteCell.primaryDownlinkKhz },
                secondaryDownlinkKhz = physical.secondaryDownlinkKhz
            )
        }
        if (unlabeledKhz.size == 1 && isReportedKhz(unlabeledKhz.first())) {
            return CarrierBandwidths(primaryDownlinkKhz = unlabeledKhz)
        }
        if (lteCell.hasAny()) return lteCell
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
     * The same downlink channel counts once. A modem report is not padded with
     * extra widths from the cell scan.
     */
    fun aggregatedKhz(
        physical: List<ChannelBandwidth>,
        lteCell: List<ChannelBandwidth>,
        unlabeledKhz: List<Int>,
        servingChannel: Int? = null
    ): Int? {
        val merged = merge(
            physical = fromChannels(campedFirst(physical, servingChannel)),
            lteCell = fromChannels(lteServingChannels(lteCell)),
            unlabeledKhz = unlabeledKhz
        )
        val total = (merged.primaryDownlinkKhz + merged.secondaryDownlinkKhz).sum()
        return total.takeIf { it > 0 }
    }

    /**
     * One entry per downlink channel. Two carriers of the same width on different
     * channels both stay. Repeats of one channel, or an unlabelled repeat of the
     * same width, count once.
     */
    private fun dedupe(channels: List<ChannelBandwidth>): List<ChannelBandwidth> {
        val seenChannels = mutableSetOf<String>()
        val seenWidths = mutableSetOf<String>()
        val unique = mutableListOf<ChannelBandwidth>()
        for (channel in channels) {
            val number = channel.channelNumber?.takeIf { it > 0 }
            if (number != null) {
                if (seenChannels.add("${channel.radio}:$number")) unique.add(channel)
            } else {
                val key = "${channel.radio}:${channel.connectionStatus}:${channel.downlinkKhz}"
                if (seenWidths.add(key)) unique.add(channel)
            }
        }
        return unique
    }

    private const val CONNECTION_UNKNOWN = -1
    private const val MIN_KHZ = 200
    private const val MAX_KHZ = 400_000
}
