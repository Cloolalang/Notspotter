package io.github.cloolalang.notspotdetector.model

/**
 * Dual-SIM phones often put both SIMs' registered cells in one [android.telephony.CellInfo]
 * list, and some OEM ServiceState lists mix EARFCN from one SIM with PCI from the other.
 * Matching must keep a coherent identity and prefer the subscription ServiceState channel.
 */
object ServingCellSelection {

    fun matchesRegisteredKeys(
        earfcn: Int?,
        pci: Int?,
        registeredEarfcn: Int?,
        registeredPci: Int?
    ): Boolean {
        if (registeredEarfcn != null && earfcn != null && registeredEarfcn != earfcn) {
            return false
        }
        if (registeredPci != null && pci != null && registeredPci != pci) {
            return false
        }
        return (registeredEarfcn != null && earfcn != null) ||
            (registeredPci != null && pci != null)
    }

    fun pickRegisteredIdentity(
        candidates: List<RegisteredIdentity>,
        expectedPlmns: Collection<String>
    ): RegisteredIdentity? {
        if (candidates.isEmpty()) return null
        val expected = expectedPlmns.mapNotNull { normalizePlmn(it) }.toSet()
        val matched = candidates.filter { candidate ->
            val plmn = normalizePlmn(candidate.plmn)
            plmn != null && plmn in expected
        }
        val pool = matched.ifEmpty { candidates }
        return pool.firstOrNull { it.earfcn != null || it.pci != null }
    }

    /**
     * ServiceState is the subscription serving identity. allCellInfo may still list a
     * removed-SIM cell (same PCI, wrong EARFCN) for many minutes.
     *
     * Same-PCI multi-carrier sites (Hill Farm 6300/328 and 3501/328) are the exception:
     * a camped CellInfo pair beats a stale or SCell ServiceState EARFCN.
     */
    fun resolveLteIdentity(
        rankedEarfcn: Int?,
        rankedPci: Int?,
        keyMatchEarfcn: Int?,
        keyMatchPci: Int?,
        registeredEarfcn: Int?,
        registeredPci: Int?,
        rankedConnectionRank: Int = 0,
        keyMatchConnectionRank: Int = 0
    ): Pair<Int?, Int?> {
        if (preferCampedPairOverRegistered(
                campedEarfcn = rankedEarfcn,
                campedPci = rankedPci,
                campedConnectionRank = rankedConnectionRank,
                registeredEarfcn = registeredEarfcn,
                registeredPci = registeredPci,
                keyMatchEarfcn = keyMatchEarfcn,
                keyMatchPci = keyMatchPci,
                keyMatchConnectionRank = keyMatchConnectionRank
            )
        ) {
            return rankedEarfcn to rankedPci
        }
        if (registeredEarfcn != null) {
            return registeredEarfcn to (registeredPci ?: keyMatchPci ?: rankedPci)
        }
        if (keyMatchEarfcn != null || keyMatchPci != null) {
            return keyMatchEarfcn to keyMatchPci
        }
        return rankedEarfcn to rankedPci
    }

    /**
     * True when CellInfo has a camped carrier on the same PCI as ServiceState but a different
     * EARFCN — typical after 2G↔4G or intra-site CA. Do not overlay the stale channel.
     */
    fun preferCampedPairOverRegistered(
        campedEarfcn: Int?,
        campedPci: Int?,
        campedConnectionRank: Int,
        registeredEarfcn: Int?,
        registeredPci: Int?,
        keyMatchEarfcn: Int? = null,
        keyMatchPci: Int? = null,
        keyMatchConnectionRank: Int = 0
    ): Boolean {
        if (campedEarfcn == null || campedPci == null) return false
        if (campedConnectionRank < 1) return false
        if (registeredEarfcn == null || registeredEarfcn == campedEarfcn) return false
        val samePci = registeredPci == null ||
            registeredPci == campedPci ||
            keyMatchPci == campedPci
        if (!samePci) return false
        return campedConnectionRank > keyMatchConnectionRank
    }

    fun isStaleCell(ageMs: Long?, newestAgeMs: Long?): Boolean {
        if (ageMs == null || ageMs < 0L) return false
        if (newestAgeMs != null) {
            return ageMs > newestAgeMs + STALE_SLACK_MS
        }
        return ageMs > ABSOLUTE_STALE_MS
    }

    /**
     * ServiceState often reports PCI 0 and no EARFCN when the serving identity is blank
     * (`ss=—/0`). PCI 0 is a real LTE PCI only when an EARFCN is also present.
     */
    fun usableRegisteredPair(earfcn: Int?, pci: Int?): Pair<Int?, Int?> {
        if (earfcn == null && (pci == null || pci == 0)) {
            return null to null
        }
        return earfcn to pci
    }

    /**
     * Registered / ServiceState-matched rows beat a neighbour that only looks better because
     * its PLMN is blank (UNKNOWN outranks an explicit 23410 vs 23415 mismatch).
     */
    fun beatsServingRank(
        matchesRegisteredKeys: Boolean,
        connectionRank: Int,
        plmnRank: Int,
        pciMatchesSignal: Boolean,
        otherMatchesRegisteredKeys: Boolean,
        otherConnectionRank: Int,
        otherPlmnRank: Int,
        otherPciMatchesSignal: Boolean
    ): Boolean {
        if (matchesRegisteredKeys != otherMatchesRegisteredKeys) return matchesRegisteredKeys
        if (connectionRank != otherConnectionRank) return connectionRank > otherConnectionRank
        if (plmnRank != otherPlmnRank) return plmnRank > otherPlmnRank
        if (pciMatchesSignal != otherPciMatchesSignal) return pciMatchesSignal
        return false
    }

    const val STALE_SLACK_MS = 10_000L
    const val ABSOLUTE_STALE_MS = 120_000L
    const val METRICS_STALE_MS = 15_000L

    fun isMetricsStale(newestAgeMs: Long?): Boolean {
        return newestAgeMs != null && newestAgeMs > METRICS_STALE_MS
    }

    /**
     * Whether CellInfo is new enough to show RSRP / RSRQ / 2G RX as a live measurement.
     * Visiting limited service is never treated as a live quality scan.
     * Home limited service and in-service use [METRICS_STALE_MS].
     */
    fun isSignalQualityFresh(
        newestAgeMs: Long?,
        isVisitedLimitedService: Boolean
    ): Boolean {
        if (isVisitedLimitedService) return false
        if (newestAgeMs != null) return newestAgeMs <= METRICS_STALE_MS
        return true
    }

    /**
     * Whether [android.telephony.SignalStrength] PCIs look like the serving CellInfo cell.
     * An empty PCI set means the modem omitted them — that is not a mismatch.
     */
    fun signalStrengthMatchesServingPcis(
        servingLtePci: Int?,
        servingNrPci: Int?,
        signalLtePcis: Set<Int>,
        signalNrPcis: Set<Int>
    ): Boolean {
        if (servingLtePci != null && servingLtePci in signalLtePcis) return true
        if (servingNrPci != null && servingNrPci in signalNrPcis) return true
        return signalLtePcis.isEmpty() && signalNrPcis.isEmpty()
    }

    /**
     * [android.telephony.SignalStrength] has no timestamp. Visiting limited service must not
     * keep a frozen SignalStrength RSRP/RSRQ/RX on screen. Home limited and in-service may
     * use it when CellInfo is fresh and the PCI matches (or there is no camped identity).
     */
    fun shouldUseSignalStrengthForServingMetrics(
        hasCampedIdentity: Boolean,
        signalMatchesServing: Boolean,
        signalQualityStale: Boolean,
        isVisitedLimitedService: Boolean
    ): Boolean {
        if (signalQualityStale || isVisitedLimitedService) return false
        return !hasCampedIdentity || signalMatchesServing
    }

    fun pickServingMetric(
        fromCell: Int?,
        fromSignal: Int?,
        useSignal: Boolean
    ): Int? {
        return fromCell ?: fromSignal.takeIf { useSignal }
    }

    fun normalizePlmn(value: String?): String? {
        val digits = value?.filter { it.isDigit() }.orEmpty()
        return digits.takeIf { it.length >= 5 }
    }

    data class RegisteredIdentity(
        val earfcn: Int?,
        val pci: Int?,
        val plmn: String? = null
    )
}
