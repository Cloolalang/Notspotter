package io.github.cloolalang.notspotdetector.model

/** Serving-network operator when camped away from the home PLMN. */
fun ConnectivityStats.resolveCampedVisitedOperatorName(): String? {
    val homeName = homeNetworkOperatorName?.trim()?.takeIf { it.isNotBlank() }
    val servingName = servingNetworkOperatorName?.trim()?.takeIf { it.isNotBlank() }
        ?: networkOperatorName?.trim()?.takeIf { it.isNotBlank() }
    val homePlmn = homePlmn?.trim()?.takeIf { it.isNotBlank() }
    val servingPlmn = plmn?.trim()?.takeIf { it.isNotBlank() }

    val isVisited = when {
        homePlmn != null && servingPlmn != null -> homePlmn != servingPlmn
        homeName != null && servingName != null -> !servingName.equals(homeName, ignoreCase = true)
        else -> false
    }
    if (!isVisited) return null
    if (homeName == null && homePlmn == null) return null

    val servingNameIsHome = homeName != null &&
        servingName != null &&
        servingName.equals(homeName, ignoreCase = true)
    return when {
        servingName != null && !servingNameIsHome -> servingName
        servingPlmn != null -> servingPlmn
        else -> servingName
    }
}

/**
 * Prefers the registered cell's operator when telephony still reports the home name/PLMN
 * during limited-service camp on a visited network.
 */
fun resolveServingOperatorFromCell(
    telephonyServingName: String?,
    homeName: String?,
    homePlmn: String?,
    cellServingName: String?,
    cellServingPlmn: String?
): String? {
    val cellName = cellServingName?.trim()?.takeIf { it.isNotBlank() }
    val telephonyName = telephonyServingName?.trim()?.takeIf { it.isNotBlank() }
    val home = homeName?.trim()?.takeIf { it.isNotBlank() }
    val cellPlmn = cellServingPlmn?.trim()?.takeIf { it.isNotBlank() }
    val homePlmnValue = homePlmn?.trim()?.takeIf { it.isNotBlank() }
    val cellIsVisited = homePlmnValue != null && cellPlmn != null && homePlmnValue != cellPlmn
    if (cellIsVisited) {
        val telephonyIsHome = home != null &&
            telephonyName != null &&
            telephonyName.equals(home, ignoreCase = true)
        return cellName
            ?: telephonyName?.takeUnless { telephonyIsHome }
            ?: cellPlmn
    }
    return telephonyName ?: cellName
}

/** Serving-network operator when camped away from the home PLMN during limited service. */
fun ConnectivityStats.resolveLimitedServiceVisitedOperatorName(): String? {
    if (!isLimitedService) return null
    return resolveCampedVisitedOperatorName()
}

/** Camped operator for signal-low / no-signal voice — `{name} visited` when on a visited PLMN. */
fun ConnectivityStats.formatCampedOperatorForSpeech(): String? {
    val name = servingNetworkOperatorName?.trim()?.takeIf { it.isNotBlank() }
        ?: networkOperatorName?.trim()?.takeIf { it.isNotBlank() }
        ?: return null
    val spoken = NetworkOperatorSpeech.formatForSpeech(name) ?: return null
    return if (resolveCampedVisitedOperatorName() != null) {
        "$spoken ${SignalStateAnnouncement.PHRASE_VISITED_OPERATOR_ROLE}"
    } else {
        spoken
    }
}

fun ConnectivityStats.limitedServiceVisitedOperatorChanged(previous: ConnectivityStats): Boolean {
    if (!isLimitedService || !previous.isLimitedService) return false

    val previousVisited = previous.resolveLimitedServiceVisitedOperatorName()
    val nextVisited = resolveLimitedServiceVisitedOperatorName()
    if (previousVisited == null || nextVisited == null) return false

    return !previousVisited.equals(nextVisited, ignoreCase = true)
}
