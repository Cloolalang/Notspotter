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
     */
    fun resolveLteIdentity(
        rankedEarfcn: Int?,
        rankedPci: Int?,
        keyMatchEarfcn: Int?,
        keyMatchPci: Int?,
        registeredEarfcn: Int?,
        registeredPci: Int?
    ): Pair<Int?, Int?> {
        if (registeredEarfcn != null) {
            return registeredEarfcn to (registeredPci ?: keyMatchPci ?: rankedPci)
        }
        if (keyMatchEarfcn != null || keyMatchPci != null) {
            return keyMatchEarfcn to keyMatchPci
        }
        return rankedEarfcn to rankedPci
    }

    fun isStaleCell(ageMs: Long?, newestAgeMs: Long?): Boolean {
        if (ageMs == null || ageMs < 0L) return false
        if (newestAgeMs != null) {
            return ageMs > newestAgeMs + STALE_SLACK_MS
        }
        return ageMs > ABSOLUTE_STALE_MS
    }

    const val STALE_SLACK_MS = 10_000L
    const val ABSOLUTE_STALE_MS = 120_000L

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
