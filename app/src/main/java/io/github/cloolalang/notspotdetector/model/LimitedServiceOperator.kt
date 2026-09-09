package io.github.cloolalang.notspotdetector.model

/** Serving-network operator when camped away from the home PLMN. */
fun ConnectivityStats.resolveCampedVisitedOperatorName(): String? {
    val homeName = homeNetworkOperatorName?.trim()?.takeIf { it.isNotBlank() }
    val servingName = servingNetworkOperatorName?.trim()?.takeIf { it.isNotBlank() }
        ?: networkOperatorName?.trim()?.takeIf { it.isNotBlank() }
        ?: return null

    if (homeName != null && servingName.equals(homeName, ignoreCase = true)) {
        return null
    }

    val homePlmn = homePlmn?.trim()?.takeIf { it.isNotBlank() }
    val servingPlmn = plmn?.trim()?.takeIf { it.isNotBlank() }
    if (homePlmn != null && servingPlmn != null && homePlmn == servingPlmn) {
        return null
    }

    if (homeName == null && homePlmn == null) {
        return null
    }

    if (homeName != null && !servingName.equals(homeName, ignoreCase = true)) {
        return servingName
    }

    if (homePlmn != null && servingPlmn != null && homePlmn != servingPlmn) {
        return servingName
    }

    return null
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
