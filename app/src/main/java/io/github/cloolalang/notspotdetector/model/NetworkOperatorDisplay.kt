package io.github.cloolalang.notspotdetector.model

/** Home-network operator for the metrics card. */
fun ConnectivityStats.formatHomeOperatorDisplay(): String? {
    return homeNetworkOperatorName?.trim()?.takeIf { it.isNotBlank() }
        ?: networkOperatorName?.trim()?.takeIf { it.isNotBlank() }
        ?: homePlmn?.trim()?.takeIf { it.isNotBlank() }
}

/** Visited-network operator while in limited service away from the home PLMN. */
fun ConnectivityStats.formatVisitedOperatorDisplay(): String? {
    return resolveLimitedServiceVisitedOperatorName()
}

/** Home operator for the network metric row. */
fun ConnectivityStats.formatNetworkOperatorDisplay(): String? {
    return formatHomeOperatorDisplay()
}
