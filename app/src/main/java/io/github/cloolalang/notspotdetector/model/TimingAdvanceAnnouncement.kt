package io.github.cloolalang.notspotdetector.model

/**
 * Voice announcement for the approximate distance to the primary cell.
 * Optional operator, technology, and band prefixes follow the other announcements.
 */
object TimingAdvanceAnnouncement {
    private val PREVIEW_READING = TimingAdvanceReading(42, TimingAdvanceKind.LTE)

    fun format(stats: ConnectivityStats, volumes: AudioVolumeSettings): String? {
        val distance = stats.displayedTimingAdvance()?.distanceSpeech() ?: return null
        return spoken(stats, volumes, distance)
    }

    fun preview(
        networkOperatorName: String?,
        phrases: VoicePhraseOptions,
        bandNamingStyle: CellReselectBandNamingStyle
    ): String {
        return SignalStateAnnouncement.joinAnnouncementParts(
            operatorNames = listOf(networkOperatorName),
            radioAccessType = io.github.cloolalang.notspotdetector.network.CellularSignalReader.RADIO_4G,
            serviceState = PREVIEW_READING.distanceSpeech(),
            speakOperatorNameEnabled = phrases.speakOperatorName,
            speakTechnologyEnabled = phrases.speakTechnology,
            speakBandEnabled = phrases.speakBand,
            bandPhrase = phrases.bandPhraseFor(lteEarfcn = 6400, namingStyle = bandNamingStyle)
        )
    }

    private fun spoken(
        stats: ConnectivityStats,
        volumes: AudioVolumeSettings,
        distance: String
    ): String {
        val phrases = volumes.timingAdvancePhrases
        return SignalStateAnnouncement.joinAnnouncementParts(
            operatorNames = listOf(stats.networkOperatorName),
            radioAccessType = stats.radioAccessType,
            serviceState = distance,
            speakOperatorNameEnabled = phrases.speakOperatorName,
            speakTechnologyEnabled = phrases.speakTechnology,
            speakBandEnabled = phrases.speakBand,
            bandPhrase = phrases.bandPhraseFor(
                lteEarfcn = stats.lteEarfcn,
                nrBand = stats.nrBand,
                gsmEarfcn = stats.gsmEarfcn,
                namingStyle = volumes.cellChangeBandNamingStyle
            )
        )
    }
}
