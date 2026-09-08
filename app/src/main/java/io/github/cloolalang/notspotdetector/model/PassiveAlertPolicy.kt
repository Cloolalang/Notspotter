package io.github.cloolalang.notspotdetector.model

/**
 * Whether current readings warrant signal-tier and quality alert sounds during quiet passive mode.
 */
fun ConnectivityStats.meetsPassiveCriticalAlertCriteria(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!signalPermissionGranted) return false
    if (shouldPlayFlatline(settings) || isRsrpTooWeakForService(settings)) return true
    if (rsrqDb != null && rsrqDb < settings.quietAlertRsrqDb) return true
    val rsrp = rsrpDbm ?: return false
    return rsrp <= settings.quietAlertRsrpMaxDbm
}

/**
 * Signal-tier clicks, cell-change bell, technology-change sweep, and limited-service tone
 * during passive-only monitoring. No-signal (flatline) tone is not gated here.
 */
fun ConnectivityStats.shouldPlayPassiveSignalAndQualityAlerts(
    monitoringSettings: MonitoringSettings,
    passiveSettings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!isPassiveOnlySession || isPassiveIdleMode) return true
    if (!monitoringSettings.passiveQuietUntilCritical) return true
    return meetsPassiveCriticalAlertCriteria(passiveSettings)
}
