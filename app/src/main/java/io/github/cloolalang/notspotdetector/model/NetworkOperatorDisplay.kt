package io.github.cloolalang.notspotdetector.model

/** Home and camped operator for the network metric row. */
fun ConnectivityStats.formatNetworkOperatorDisplay(): String? {
    if (isLimitedService) {
        val visited = resolveLimitedServiceVisitedOperatorName()
        val home = homeNetworkOperatorName?.trim()?.takeIf { it.isNotBlank() }
            ?: networkOperatorName?.trim()?.takeIf { it.isNotBlank() }
        if (home != null && visited != null) {
            return "$home · $visited"
        }
    }
    return networkOperatorName?.trim()?.takeIf { it.isNotBlank() } ?: plmn?.trim()?.takeIf { it.isNotBlank() }
}
