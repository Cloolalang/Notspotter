package io.github.cloolalang.notspotdetector.model

/** Serving-network operator when camped away from the home PLMN during limited service. */
fun ConnectivityStats.resolveLimitedServiceAlternativeOperatorName(): String? {
    if (!isLimitedService) return null

    val homeName = homeNetworkOperatorName?.trim()?.takeIf { it.isNotBlank() }
    val servingName = servingNetworkOperatorName?.trim()?.takeIf { it.isNotBlank() }
        ?: return null

    if (homeName != null && servingName.equals(homeName, ignoreCase = true)) {
        return null
    }

    val homePlmn = homePlmn?.trim()?.takeIf { it.isNotBlank() }
    val servingPlmn = plmn?.trim()?.takeIf { it.isNotBlank() }
    if (homePlmn != null && servingPlmn != null && homePlmn == servingPlmn) {
        return null
    }

    return servingName
}

fun ConnectivityStats.limitedServiceAlternativeOperatorChanged(previous: ConnectivityStats): Boolean {
    if (!isLimitedService || !previous.isLimitedService) return false

    val previousAlternative = previous.resolveLimitedServiceAlternativeOperatorName()
    val nextAlternative = resolveLimitedServiceAlternativeOperatorName()
    if (previousAlternative == null || nextAlternative == null) return false

    return !previousAlternative.equals(nextAlternative, ignoreCase = true)
}
