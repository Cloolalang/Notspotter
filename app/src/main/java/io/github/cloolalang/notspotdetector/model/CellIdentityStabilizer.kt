package io.github.cloolalang.notspotdetector.model

/**
 * Keeps EARFCN/PCI visible when Android returns partial or transient nulls between polls.
 * Clears only on genuine no-service / 2G-without-fallback conditions.
 */
fun CellIdentitySnapshot.coalesceWith(previous: CellIdentitySnapshot): CellIdentitySnapshot {
    return CellIdentitySnapshot(
        lteEarfcn = lteEarfcn ?: previous.lteEarfcn,
        ltePci = ltePci ?: previous.ltePci?.takeIf {
            lteEarfcn == null || previous.lteEarfcn == null || lteEarfcn == previous.lteEarfcn
        },
        nrEarfcn = nrEarfcn ?: previous.nrEarfcn,
        nrPci = nrPci ?: previous.nrPci?.takeIf {
            nrEarfcn == null || previous.nrEarfcn == null || nrEarfcn == previous.nrEarfcn
        },
        nrBand = nrBand ?: previous.nrBand?.takeIf {
            nrEarfcn == null || previous.nrEarfcn == null || nrEarfcn == previous.nrEarfcn
        },
        gsmEarfcn = gsmEarfcn ?: previous.gsmEarfcn,
        gsmBsic = gsmBsic ?: previous.gsmBsic?.takeIf {
            gsmEarfcn == null || previous.gsmEarfcn == null || gsmEarfcn == previous.gsmEarfcn
        }
    )
}

fun ConnectivityStats.shouldClearCellIdentity(): Boolean {
    if (isLimitedService) return false
    if (networkServiceMode.isRadioPoweredOff()) return true
    if (isMonitoring && noSignalActive) return true
    if (isOn2g && !monitor2gFallbackEnabled) return true
    if (isCompleteNoService) return true
    return false
}

/** On pure 2G fallback, drop stale LTE/NR cell identity but keep GSM signal and identity. */
fun ConnectivityStats.shouldClearLteNrCellIdentity(): Boolean {
    return isOn2g && monitor2gFallbackEnabled && !hasLteNrSignal
}

fun ConnectivityStats.withStabilizedCellIdentity(
    previous: CellIdentitySnapshot
): Pair<ConnectivityStats, CellIdentitySnapshot> {
    if (shouldClearCellIdentity()) {
        return copy(
            rsrpDbm = null,
            rsrqDb = null,
            lteSinrDb = null,
            nrSinrDb = null,
            lteEarfcn = null,
            ltePci = null,
            nrEarfcn = null,
            nrPci = null,
            nrBand = null,
            gsmEarfcn = null,
            gsmBsic = null
        ) to CellIdentitySnapshot()
    }

    val current = CellIdentitySnapshot.fromStats(this)
    val stabilized = if (shouldClearLteNrCellIdentity()) {
        CellIdentitySnapshot(
            lteEarfcn = null,
            ltePci = null,
            nrEarfcn = null,
            nrPci = null,
            nrBand = null,
            gsmEarfcn = current.gsmEarfcn ?: previous.gsmEarfcn,
            gsmBsic = current.gsmBsic ?: previous.gsmBsic
        )
    } else {
        current.coalesceWith(previous)
    }
    return copy(
        lteEarfcn = stabilized.lteEarfcn,
        ltePci = stabilized.ltePci,
        nrEarfcn = stabilized.nrEarfcn,
        nrPci = stabilized.nrPci,
        nrBand = stabilized.nrBand,
        gsmEarfcn = stabilized.gsmEarfcn,
        gsmBsic = stabilized.gsmBsic
    ) to stabilized
}
