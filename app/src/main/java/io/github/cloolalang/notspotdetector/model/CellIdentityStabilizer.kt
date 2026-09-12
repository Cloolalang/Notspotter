package io.github.cloolalang.notspotdetector.model

/**
 * Keeps EARFCN/PCI visible when Android returns partial or transient nulls between polls.
 * Clears only on genuine no-service / 2G-without-fallback conditions.
 */
fun CellIdentitySnapshot.coalesceWith(previous: CellIdentitySnapshot): CellIdentitySnapshot {
    val nextLteEarfcn = lteEarfcn ?: previous.lteEarfcn?.takeIf {
        ltePci == null || previous.ltePci == null || ltePci == previous.ltePci
    }
    val nextLtePci = ltePci ?: previous.ltePci?.takeIf {
        val earfcn = lteEarfcn ?: nextLteEarfcn
        earfcn == null || previous.lteEarfcn == null || earfcn == previous.lteEarfcn
    }
    val nextNrEarfcn = nrEarfcn ?: previous.nrEarfcn?.takeIf {
        nrPci == null || previous.nrPci == null || nrPci == previous.nrPci
    }
    val nextNrPci = nrPci ?: previous.nrPci?.takeIf {
        val earfcn = nrEarfcn ?: nextNrEarfcn
        earfcn == null || previous.nrEarfcn == null || earfcn == previous.nrEarfcn
    }
    val nextGsmEarfcn = gsmEarfcn ?: previous.gsmEarfcn?.takeIf {
        gsmBsic == null || previous.gsmBsic == null || gsmBsic == previous.gsmBsic
    }
    val nextGsmBsic = gsmBsic ?: previous.gsmBsic?.takeIf {
        val earfcn = gsmEarfcn ?: nextGsmEarfcn
        earfcn == null || previous.gsmEarfcn == null || earfcn == previous.gsmEarfcn
    }
    return CellIdentitySnapshot(
        lteEarfcn = nextLteEarfcn,
        ltePci = nextLtePci,
        nrEarfcn = nextNrEarfcn,
        nrPci = nextNrPci,
        nrBand = nrBand ?: previous.nrBand?.takeIf {
            val earfcn = nrEarfcn ?: nextNrEarfcn
            earfcn == null || previous.nrEarfcn == null || earfcn == previous.nrEarfcn
        },
        gsmEarfcn = nextGsmEarfcn,
        gsmBsic = nextGsmBsic
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
