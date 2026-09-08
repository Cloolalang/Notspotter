package io.github.cloolalang.notspotdetector.model

/**
 * Legacy display filter — prefer [withStabilizedCellIdentity] in [MonitorState] for monitoring.
 * Kept for callers that only need a one-shot clear on no-service.
 */
fun ConnectivityStats.withCellIdentityForDisplay(
    @Suppress("UNUSED_PARAMETER") settings: PassiveSignalSettings = PassiveSignalSettings()
): ConnectivityStats {
    if (!shouldClearCellIdentity()) return this
    return copy(
        rsrpDbm = null,
        rsrqDb = null,
        lteEarfcn = null,
        ltePci = null,
        nrEarfcn = null,
        nrPci = null,
        gsmEarfcn = null,
        gsmBsic = null
    )
}
