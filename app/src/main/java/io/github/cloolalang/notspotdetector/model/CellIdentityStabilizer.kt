package io.github.cloolalang.notspotdetector.model

/**
 * Keeps EARFCN/PCI visible when Android returns partial or transient nulls between polls.
 * Clears only on genuine no-service / 2G-without-fallback conditions.
 */
fun CellIdentitySnapshot.coalesceWith(previous: CellIdentitySnapshot): CellIdentitySnapshot {
    return CellIdentitySnapshot(
        lteEarfcn = lteEarfcn ?: previous.lteEarfcn,
        ltePci = ltePci ?: previous.ltePci,
        nrEarfcn = nrEarfcn ?: previous.nrEarfcn,
        nrPci = nrPci ?: previous.nrPci,
        gsmEarfcn = gsmEarfcn ?: previous.gsmEarfcn,
        gsmBsic = gsmBsic ?: previous.gsmBsic
    )
}

fun ConnectivityStats.shouldClearCellIdentity(): Boolean {
    if (isMonitoring && noSignalActive) return true
    if (isOn2g && !monitor2gFallbackEnabled) return true
    if (isCompleteNoService) return true
    if (isOn2g && monitor2gFallbackEnabled && !hasLteNrSignal) return true
    return false
}

fun ConnectivityStats.withStabilizedCellIdentity(
    previous: CellIdentitySnapshot
): Pair<ConnectivityStats, CellIdentitySnapshot> {
    if (shouldClearCellIdentity()) {
        return copy(
            rsrpDbm = null,
            rsrqDb = null,
            lteEarfcn = null,
            ltePci = null,
            nrEarfcn = null,
            nrPci = null,
            gsmEarfcn = null,
            gsmBsic = null
        ) to CellIdentitySnapshot()
    }

    val stabilized = CellIdentitySnapshot.fromStats(this).coalesceWith(previous)
    return copy(
        lteEarfcn = stabilized.lteEarfcn,
        ltePci = stabilized.ltePci,
        nrEarfcn = stabilized.nrEarfcn,
        nrPci = stabilized.nrPci,
        gsmEarfcn = stabilized.gsmEarfcn,
        gsmBsic = stabilized.gsmBsic
    ) to stabilized
}
