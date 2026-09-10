package io.github.cloolalang.notspotdetector.model

enum class SignalStrengthTier {
    /** RXSS 2 — Level Range A (highest RSRP band below very strong). */
    MILD,
    /** RXSS 3 — Level Range B. */
    GOOD,
    /** RXSS 4 — Level Range C. */
    FAIR,
    /** RXSS 5 — Level Range D. */
    POOR,
    /** RXSS 6 — Signal low (weakest usable RSRP band). */
    CRITICAL,
    /** RXSS 7 — 2G good signal. */
    G2_STRONG,
    /** RXSS 8 — 2G weak. */
    G2_WEAK,
    /** RXSS 15 — Home 2G no signal (debounced). */
    G2_NO_SIGNAL,
    /** RXSS 0 — Dead zone. */
    DEADZONE,
    /** RXSS 10 — 4G/5G no signal (debounced). */
    NO_SIGNAL,
    /** RXSS 11 — Searching for home 2G. */
    SEARCHING_2G,
    /** RXSS 12 — Limited service alt 4G. */
    LIMITED_SERVICE,
    /** RXSS 13 — Limited alt 2G. */
    LIMITED_ALT_2G,
    /** RXSS 14 — RSRQ poor (overlay). */
    RSRQ_POOR,
    /** RXSS 31 — WiFi calling, no cellular signal. */
    WIFI_CALLING;

    /** RX Signal State catalogue number — see [RXSS_CATALOGUE.md]. */
    val rxssNumber: Int
        get() = when (this) {
            MILD -> Rxss.LEVEL_RANGE_A
            GOOD -> Rxss.LEVEL_RANGE_B
            FAIR -> Rxss.LEVEL_RANGE_C
            POOR -> Rxss.LEVEL_RANGE_D
            CRITICAL -> Rxss.SIGNAL_LOW
            G2_STRONG -> Rxss.G2_GOOD
            G2_WEAK -> Rxss.G2_WEAK
            G2_NO_SIGNAL -> Rxss.HOME_2G_NO_SIGNAL
            DEADZONE -> Rxss.DEADZONE
            NO_SIGNAL -> Rxss.LTE_NR_NO_SIGNAL
            SEARCHING_2G -> Rxss.SEARCH_HOME_2G
            LIMITED_SERVICE -> Rxss.LIMITED_ALT_4G
            LIMITED_ALT_2G -> Rxss.LIMITED_ALT_2G
            RSRQ_POOR -> Rxss.RSRQ_POOR
            WIFI_CALLING -> Rxss.WIFI_CALLING_NO_SIGNAL
        }

    /** @deprecated Renamed to [rxssNumber] — same value. */
    val displayNumber: Int
        get() = rxssNumber

    val intervalMs: Long
        get() = when (this) {
            MILD -> 5_000L
            GOOD -> 4_000L
            FAIR -> 2_500L
            POOR -> 1_000L
            CRITICAL -> 500L
            G2_STRONG -> 2_000L
            G2_WEAK -> 500L
            G2_NO_SIGNAL -> 600L
            DEADZONE -> 250L
            NO_SIGNAL -> 600L
            SEARCHING_2G -> 600L
            LIMITED_SERVICE -> 900L
            LIMITED_ALT_2G -> 600L
            RSRQ_POOR -> 800L
            WIFI_CALLING -> 600L
        }

    val isCampStateTier: Boolean
        get() = when (this) {
            G2_STRONG,
            G2_WEAK,
            G2_NO_SIGNAL,
            DEADZONE,
            NO_SIGNAL,
            SEARCHING_2G,
            LIMITED_SERVICE,
            LIMITED_ALT_2G,
            RSRQ_POOR,
            WIFI_CALLING -> true
            else -> false
        }

    val pulseDurationRatio: Float
        get() = 1f

    fun pulseDurationMs(baseDurationMs: Int): Int {
        return (baseDurationMs * pulseDurationRatio).toInt().coerceAtLeast(MIN_SIGNAL_PULSE_DURATION_MS)
    }
}

/** Classified RX Signal State for the latest RSRP/RSRQ measurement (UI and diagnostics). */
enum class SignalMeasurementTier {
    VERY_STRONG,
    MILD,
    GOOD,
    FAIR,
    POOR,
    CRITICAL,
    G2_STRONG,
    G2_WEAK,
    G2_NO_SIGNAL,
    DEADZONE,
    NO_SIGNAL,
    SEARCHING_2G,
    LIMITED_SERVICE,
    LIMITED_ALT_2G,
    /** RXSS **20** — limited visited 4G/5G camp, no usable RSRP. */
    LIMITED_4G_NO_SIGNAL,
    /** RXSS **23** — limited visited 2G camp, no usable RX. */
    LIMITED_ALT_2G_NO_SIGNAL,
    RSRQ_POOR,
    /** RXSS **31** — in service via WiFi calling only, no cellular RAT/RSRP. */
    WIFI_CALLING,
    PERMISSION_REQUIRED,
    UNAVAILABLE;

    /** RX Signal State catalogue number, or null for non-RXSS states. */
    val rxssNumber: Int?
        get() = when (this) {
            VERY_STRONG -> Rxss.SIGNAL_HIGH
            MILD -> SignalStrengthTier.MILD.rxssNumber
            GOOD -> SignalStrengthTier.GOOD.rxssNumber
            FAIR -> SignalStrengthTier.FAIR.rxssNumber
            POOR -> SignalStrengthTier.POOR.rxssNumber
            CRITICAL -> SignalStrengthTier.CRITICAL.rxssNumber
            G2_STRONG -> Rxss.G2_GOOD
            G2_WEAK -> Rxss.G2_WEAK
            G2_NO_SIGNAL -> Rxss.HOME_2G_NO_SIGNAL
            DEADZONE -> Rxss.DEADZONE
            NO_SIGNAL -> Rxss.LTE_NR_NO_SIGNAL
            SEARCHING_2G -> Rxss.SEARCH_HOME_2G
            LIMITED_SERVICE -> Rxss.LIMITED_ALT_4G
            LIMITED_ALT_2G -> Rxss.LIMITED_ALT_2G
            LIMITED_4G_NO_SIGNAL -> Rxss.LIMITED_4G_NO_SIGNAL
            LIMITED_ALT_2G_NO_SIGNAL -> Rxss.LIMITED_ALT_2G_NO_SIGNAL
            RSRQ_POOR -> Rxss.RSRQ_POOR
            WIFI_CALLING -> Rxss.WIFI_CALLING_NO_SIGNAL
            else -> null
        }

    /** @deprecated Renamed to [rxssNumber] — same value. */
    val displayNumber: Int?
        get() = rxssNumber

    fun toStrengthTier(): SignalStrengthTier? = when (this) {
        G2_NO_SIGNAL -> SignalStrengthTier.G2_NO_SIGNAL
        DEADZONE -> SignalStrengthTier.DEADZONE
        NO_SIGNAL -> SignalStrengthTier.NO_SIGNAL
        SEARCHING_2G -> SignalStrengthTier.SEARCHING_2G
        LIMITED_SERVICE -> SignalStrengthTier.LIMITED_SERVICE
        LIMITED_ALT_2G -> SignalStrengthTier.LIMITED_ALT_2G
        RSRQ_POOR -> SignalStrengthTier.RSRQ_POOR
        WIFI_CALLING -> SignalStrengthTier.WIFI_CALLING
        else -> null
    }

    /** True when this measurement maps to an RXSS row with Signal = No (catalogue). */
    fun isNoSignalRxss(): Boolean {
        val number = rxssNumber ?: return false
        return number in Rxss.NO_SIGNAL_RXSS_NUMBERS
    }
}

/**
 * Limited-service camp with no usable signal (RXSS **20** / **23** and related placeholders),
 * before those rows are classified directly in [resolveSignalMeasurementTier].
 */
fun ConnectivityStats.isLimitedServiceNoSignalCamp(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!isLimitedService) return false
    if (isLimitedServiceAlt2g()) {
        if (!hasHomeGsmSignal && rsrpDbm == null) return true
        return isRsrpTooWeakForService(settings)
    }
    if (usesG2SignalTiers()) return false
    return isRsrpTooWeakForService(settings) || rsrpDbm == null
}

/**
 * Concurrent RXSS 1–6 (limited 4G) or 7 or 8 (limited visited 2G) while camped on 12 or 13.
 * Null when the no-signal overlay (20 or 23) applies.
 */
/** True when limited service has a concurrent RXSS 1–6 or 7–8 overlay (not 20/23). */
fun ConnectivityStats.shouldPlayLimitedServiceSignalOverlay(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!isMonitoring || !isLimitedService) return false
    return resolveLimitedServiceSignalOverlayRxss(settings) != null
}

fun ConnectivityStats.resolveLimitedServiceSignalOverlayRxss(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Int? {
    if (!isLimitedService || isLimitedServiceNoSignalCamp(settings)) return null
    if (isLimitedServiceAlt2g()) {
        return when (settings.resolveG2SignalStrengthTier(rsrpDbm)) {
            SignalStrengthTier.G2_STRONG -> Rxss.G2_GOOD
            SignalStrengthTier.G2_WEAK -> Rxss.G2_WEAK
            else -> null
        }
    }
    if (usesG2SignalTiers()) return null
    val rsrp = rsrpDbm ?: return null
    if (isRsrpTooWeakForService(settings)) return null
    if (settings.isVeryStrongRsrp(rsrp)) return Rxss.SIGNAL_HIGH
    return settings.resolveSignalStrengthTier(rsrp)?.rxssNumber
}

/**
 * Primary RXSS is a catalogue no-signal row (Signal = No), or a search/transition state with no
 * camped cell (RXSS 11 searching 2G, and any future 16/17/18/24/25). [ConnectivityStats.noSignalActive]
 * is the debounced flag driving those transition states, so it is checked directly rather than
 * only the narrower catalogue "No" tier-number list — otherwise states like RXSS 11 (which the
 * catalogue marks Signal = "—", not "No") would slip through and allow a stale cell-reselect
 * announcement while there is no usable cell to report.
 *
 * Also treats [SignalMeasurementTier.UNAVAILABLE] as no-signal: this tier is returned when the
 * device is camped (`radioAccessType` present) but has no RSRP/RSRQ measurement yet — e.g. right
 * after entering a dead zone or losing signal, before the two-poll [noSignalActive] debounce
 * confirms it. Without this, a stale/flickering PCI or EARFCN carried over between polls by
 * `coalesceWith` in `CellIdentityStabilizer.kt` could still trigger a cell-reselect announcement
 * during that debounce window even though there is no usable signal to report.
 */
fun ConnectivityStats.isInNoSignalRxss(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (isLimitedServiceNoSignalCamp(settings)) return true
    if (noSignalActive) return true
    val tier = resolveSignalMeasurementTier(settings)
    if (tier == SignalMeasurementTier.UNAVAILABLE) return true
    return tier.isNoSignalRxss()
}

/** Whether **VA-10** cell-reselect voice may fire on this poll. */
fun ConnectivityStats.shouldAllowCellReselectVoice(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!isMonitoring) return false
    return !isInNoSignalRxss(settings)
}

/** Whether **VA-16** “2 G” periodic camp voice may fire on this poll. */
fun ConnectivityStats.shouldAllowG2CampedPeriodicVoice(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!isMonitoring || !usesG2SignalTiers()) return false
    if (isLimitedServiceAlt2g()) return false
    if (shouldAllowLimitedServicePeriodicVoice(settings)) return false
    if (shouldPlayG2NoSignalVoiceAnnouncements(settings)) return false
    if (isG2WeakSignal(settings)) return false
    return !isInNoSignalRxss(settings)
}

/**
 * Whether **VA-14** limited-service periodic voice may fire on this poll — the 30s cycling
 * "limited service" reminder for RXSS 12/13 with a measurable RSRP/RX overlay (1-5 or 7).
 * Only the dedicated overlay voices take over instead: VA-15 (4G) / VA-18 (2G) "signal low" on
 * the critical/weak overlay (6 on 4G, 8 on 2G) via [isSignalLowVoiceCamp], and VA-12 on the
 * no-signal overlay (20/23) via [isInNoSignalRxss]. Any other RSRP/RX tier overlay (1-5, 7)
 * must still let VA-14 cycle every 30 s.
 */
fun ConnectivityStats.shouldAllowLimitedServicePeriodicVoice(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!isMonitoring || isPassiveIdleMode) return false
    if (!isLimitedService) return false
    if (isSignalLowVoiceCamp(settings)) return false
    return !isInNoSignalRxss(settings)
}

/**
 * Whether **VA-18** 2G weak "signal low" periodic voice may fire on this poll — the non-limited
 * camped-2G-fallback case only (RXSS 8 while camped on home 2G after LTE/NR loss). The equivalent
 * announcement for limited visited-2G weak overlay (RXSS 13 overlay 8) is handled exclusively by
 * the tier5-style periodic job via [isSignalLowVoiceCamp] / `shouldPlayTier5StylePeriodicVoice` —
 * this function must return false for [isLimitedServiceAlt2g], otherwise both periodic jobs
 * schedule independently and the same "signal low" announcement plays twice every 30 s.
 */
fun ConnectivityStats.shouldAllowG2WeakPeriodicVoice(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!isMonitoring) return false
    if (isLimitedServiceAlt2g()) return false
    if (!usesG2SignalTiers()) return false
    if (shouldAllowLimitedServicePeriodicVoice(settings)) return false
    if (!isG2WeakSignal(settings)) return false
    return !isInNoSignalRxss(settings)
}

/** RXSS **6** on limited 4G overlay, or RXSS **8** on limited visited 2G overlay. */
fun ConnectivityStats.isSignalLowVoiceCamp(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (isLimitedServiceAlt2g()) return isG2WeakSignal(settings)
    return isTier6CriticalSignal(settings)
}

/**
 * RX Signal State catalogue numbers — see [RXSS_CATALOGUE.md].
 * Implemented in the app: **0**, **1–9**, **10–15**, **14** (overlay), **20** / **23** (limited no-signal overlays), **28–30** (technology change), **31** (WiFi calling, no cellular signal).
 */
object Rxss {
    const val DEADZONE = 0
    const val SIGNAL_HIGH = 1
    const val LEVEL_RANGE_A = 2
    const val LEVEL_RANGE_B = 3
    const val LEVEL_RANGE_C = 4
    const val LEVEL_RANGE_D = 5
    const val SIGNAL_LOW = 6
    const val G2_GOOD = 7
    const val G2_WEAK = 8
    /** Momentary event — LTE/NR/2G cell reselect (PCI, EARFCN, or BSIC change). */
    const val CELL_CHANGE = 9
    /** Momentary event — camped on 2G after RAT change. */
    const val TECH_CHANGE_TO_2G = 28
    /** Momentary event — camped on LTE after RAT change. */
    const val TECH_CHANGE_TO_4G = 29
    /** Momentary event — camped on NR or EN-DC after RAT change. */
    const val TECH_CHANGE_TO_5G_ENDC = 30
    const val LTE_NR_NO_SIGNAL = 10
    const val SEARCH_HOME_2G = 11
    const val LIMITED_ALT_4G = 12
    const val LIMITED_ALT_2G = 13
    const val RSRQ_POOR = 14
    const val HOME_2G_NO_SIGNAL = 15

    /** Placeholder — not yet classified in live detection. */
    const val LIMITED_HOME_4G = 19
    const val LIMITED_4G_NO_SIGNAL = 20
    const val LIMITED_HOME_2G_NO_SIGNAL = 21
    const val LIMITED_HOME_2G = 22
    const val LIMITED_ALT_2G_NO_SIGNAL = 23
    const val SEARCH_HOME_4G_FROM_LOSS = 24
    const val SEARCH_ALT_4G_FROM_LOSS = 25
    const val LIMITED_ALT_4G_NO_SIGNAL_SUSPENDED = 26
    const val LIMITED_ALT_2G_NO_SIGNAL_SUSPENDED = 27

    /**
     * In service via WiFi calling / VoWiFi only — no cellular RAT camped and no RSRP/RSRQ
     * measurable. Distinguishes this from RXSS 10 (camped 4G/5G, no signal) and prevents it from
     * being misclassified as RXSS 11 (searching for home 2G).
     */
    const val WIFI_CALLING_NO_SIGNAL = 31

    /** RXSS rows whose Signal column is **No** in [RXSS_CATALOGUE.md] — no VA-10 cell reselect voice. */
    val NO_SIGNAL_RXSS_NUMBERS = setOf(
        DEADZONE,
        LTE_NR_NO_SIGNAL,
        HOME_2G_NO_SIGNAL,
        LIMITED_4G_NO_SIGNAL,
        LIMITED_HOME_2G_NO_SIGNAL,
        LIMITED_ALT_2G_NO_SIGNAL,
        LIMITED_ALT_4G_NO_SIGNAL_SUSPENDED,
        LIMITED_ALT_2G_NO_SIGNAL_SUSPENDED,
        WIFI_CALLING_NO_SIGNAL
    )
}

/** @deprecated Use [Rxss.SIGNAL_HIGH]. */
const val VERY_STRONG_TIER_NUMBER = Rxss.SIGNAL_HIGH

/** @deprecated Use [Rxss.G2_GOOD]. */
const val G2_STRONG_TIER_NUMBER = Rxss.G2_GOOD

/** @deprecated Use [Rxss.G2_WEAK]. */
const val G2_WEAK_TIER_NUMBER = Rxss.G2_WEAK

/** @deprecated Use [Rxss.CELL_CHANGE]. */
const val CELL_CHANGE_RXSS_NUMBER = Rxss.CELL_CHANGE

/** @deprecated Use [Rxss.DEADZONE]. */
const val DEADZONE_TIER_NUMBER = Rxss.DEADZONE

/** @deprecated Use [Rxss.DEADZONE]. */
const val DEADZONE_RXSS_CATALOGUE_NUMBER = Rxss.DEADZONE

/** @deprecated Use [Rxss.LTE_NR_NO_SIGNAL]. */
const val NO_SIGNAL_TIER_NUMBER = Rxss.LTE_NR_NO_SIGNAL

/** @deprecated Use [Rxss.SEARCH_HOME_2G]. */
const val SEARCHING_2G_TIER_NUMBER = Rxss.SEARCH_HOME_2G

/** @deprecated Use [Rxss.LIMITED_ALT_4G]. */
const val LIMITED_SERVICE_TIER_NUMBER = Rxss.LIMITED_ALT_4G

/** @deprecated Use [Rxss.LIMITED_ALT_2G]. */
const val LIMITED_ALT_2G_TIER_NUMBER = Rxss.LIMITED_ALT_2G

/** @deprecated Use [Rxss.RSRQ_POOR]. */
const val RSRQ_POOR_TIER_NUMBER = Rxss.RSRQ_POOR

/** @deprecated Use [Rxss.HOME_2G_NO_SIGNAL]. */
const val G2_NO_SIGNAL_TIER_NUMBER = Rxss.HOME_2G_NO_SIGNAL

/** @deprecated Use [Rxss.LIMITED_HOME_4G]. Not yet implemented. */
const val LIMITED_HOME_4G_TIER_NUMBER = Rxss.LIMITED_HOME_4G

/** @deprecated Use [Rxss.LIMITED_4G_NO_SIGNAL]. Placeholder only. */
const val LIMITED_4G_NO_SIGNAL_TIER_NUMBER = Rxss.LIMITED_4G_NO_SIGNAL

/** @deprecated Use [Rxss.LIMITED_HOME_2G_NO_SIGNAL]. Placeholder only. */
const val LIMITED_HOME_2G_NO_SIGNAL_TIER_NUMBER = Rxss.LIMITED_HOME_2G_NO_SIGNAL

/** @deprecated Use [Rxss.LIMITED_HOME_2G]. Placeholder only. */
const val LIMITED_HOME_2G_TIER_NUMBER = Rxss.LIMITED_HOME_2G

/** @deprecated Use [Rxss.LIMITED_ALT_2G_NO_SIGNAL]. Placeholder only. */
const val LIMITED_ALT_2G_NO_SIGNAL_TIER_NUMBER = Rxss.LIMITED_ALT_2G_NO_SIGNAL

/** @deprecated Use [Rxss.SEARCH_HOME_4G_FROM_LOSS]. Placeholder only. */
const val SEARCH_HOME_4G_FROM_LOSS_TIER_NUMBER = Rxss.SEARCH_HOME_4G_FROM_LOSS

/** @deprecated Use [Rxss.SEARCH_ALT_4G_FROM_LOSS]. Placeholder only. */
const val SEARCH_ALT_4G_FROM_LOSS_TIER_NUMBER = Rxss.SEARCH_ALT_4G_FROM_LOSS

/** @deprecated Use [Rxss.LIMITED_ALT_4G_NO_SIGNAL_SUSPENDED]. Placeholder only. */
const val LIMITED_ALT_4G_NO_SIGNAL_SUSPENDED_TIER_NUMBER = Rxss.LIMITED_ALT_4G_NO_SIGNAL_SUSPENDED

/** @deprecated Use [Rxss.LIMITED_ALT_2G_NO_SIGNAL_SUSPENDED]. Placeholder only. */
const val LIMITED_ALT_2G_NO_SIGNAL_SUSPENDED_TIER_NUMBER = Rxss.LIMITED_ALT_2G_NO_SIGNAL_SUSPENDED

const val DEFAULT_SIGNAL_PULSE_DURATION_MS = AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
const val MIN_SIGNAL_PULSE_DURATION_MS = AudioVolumeSettings.MIN_SIGNAL_PULSE_DURATION_MS

/** @deprecated Use [DEFAULT_SIGNAL_PULSE_DURATION_MS] from alert settings. */
const val SIGNAL_STRENGTH_PULSE_MS = DEFAULT_SIGNAL_PULSE_DURATION_MS
const val SIGNAL_STRENGTH_TONE_HZ = 600.0
const val VERY_STRONG_SIGNAL_INTERVAL_MS = 2_500L

/** Passive-only sessions play tier clicks at this multiple of the base rate (2 = twice as fast). */
const val PASSIVE_CLICK_RATE_MULTIPLIER = 2L
const val MIN_PASSIVE_CLICK_INTERVAL_MS = 125L

/** @deprecated Use [PassiveSignalSettings.DEFAULT_NO_SIGNAL_RSRP_DBM]. */
const val NO_USABLE_SIGNAL_RSRP_DBM = PassiveSignalSettings.DEFAULT_NO_SIGNAL_RSRP_DBM

/** @deprecated Use [PassiveSignalSettings.DEFAULT_MILD_RSRP_MIN_DBM]. */
const val SIGNAL_TIER_MILD_RSRP_DBM = PassiveSignalSettings.DEFAULT_MILD_RSRP_MIN_DBM

/** @deprecated Use [PassiveSignalSettings.DEFAULT_FAIR_RSRP_MIN_DBM]. */
const val SIGNAL_TIER_FAIR_RSRP_DBM = PassiveSignalSettings.DEFAULT_FAIR_RSRP_MIN_DBM

/** @deprecated Use [PassiveSignalSettings.DEFAULT_POOR_RSRP_MIN_DBM]. */
const val SIGNAL_TIER_POOR_RSRP_DBM = PassiveSignalSettings.DEFAULT_POOR_RSRP_MIN_DBM

/** @deprecated Use [PassiveSignalSettings.DEFAULT_CRITICAL_RSRQ_DB]. */
const val SIGNAL_TIER_CRITICAL_RSRQ_DB = PassiveSignalSettings.DEFAULT_CRITICAL_RSRQ_DB

/** @deprecated Use [PassiveSignalSettings.DEFAULT_VERY_STRONG_RSRP_MIN_DBM]. */
const val VERY_STRONG_SIGNAL_RSRP_DBM = PassiveSignalSettings.DEFAULT_VERY_STRONG_RSRP_MIN_DBM

fun ConnectivityStats.isRsrpTooWeakForService(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    return settings.isRsrpTooWeakForService(rsrpDbm)
}

fun CellularRadioMetrics.hasUsableSignalForMonitoring(
    monitor2gFallback: Boolean,
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (isLimitedService) return true
    if (rsrpDbm != null && settings.isRsrpTooWeakForService(rsrpDbm)) return false

    return when {
        isOn2g && !monitor2gFallback -> true
        isOn2g && monitor2gFallback -> radioAccessType != null || rsrpDbm != null
        else -> radioAccessType != null || rsrpDbm != null || rsrqDb != null
    }
}

fun ConnectivityStats.resolveSignalStrengthTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): SignalStrengthTier? {
    if (!signalPermissionGranted) return null
    if (isRsrpTooWeakForService(settings)) return null
    if (usesG2SignalTiers()) {
        return settings.resolveG2SignalStrengthTier(rsrpDbm)
    }
    return settings.resolveSignalStrengthTier(rsrpDbm)
}

fun ConnectivityStats.usesG2SignalTiers(): Boolean {
    return isOn2g && monitor2gFallbackEnabled
}

fun ConnectivityStats.shouldPlayDeadzoneTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    return isMonitoring && isCompleteNoService && settings.deadzoneTierSoundEnabled
}

fun ConnectivityStats.computeDeadzoneClickIntervalMs(
    settings: PassiveSignalSettings = PassiveSignalSettings(),
    pulseDurationMs: Int = settings.deadzoneTierPulseDurationMs
): Long {
    return SettingsCompatibility.resolveTierClickIntervalMs(
        configuredMs = settings.deadzoneTierClickIntervalMs.toLong(),
        signalPulseDurationMs = pulseDurationMs,
        isPassiveOnlySession = isPassiveOnlySession
    )
}

fun ConnectivityStats.resolveG2SignalStrengthTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): SignalStrengthTier? {
    if (!usesG2SignalTiers()) return null
    return settings.resolveG2SignalStrengthTier(rsrpDbm)
}

/**
 * Classifies the latest cellular measurement into an RXSS for display.
 * Uses the same RSRP/RSRQ boundaries as alert clicks, without alert-play gating.
 */
fun ConnectivityStats.resolveSignalMeasurementTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): SignalMeasurementTier {
    if (!signalPermissionGranted) return SignalMeasurementTier.PERMISSION_REQUIRED
    if (isLimitedService) {
        if (isLimitedServiceNoSignalCamp(settings)) {
            return if (isLimitedServiceAlt2g()) {
                SignalMeasurementTier.LIMITED_ALT_2G_NO_SIGNAL
            } else {
                SignalMeasurementTier.LIMITED_4G_NO_SIGNAL
            }
        }
        return if (isLimitedServiceAlt2g()) {
            SignalMeasurementTier.LIMITED_ALT_2G
        } else {
            SignalMeasurementTier.LIMITED_SERVICE
        }
    }
    if (isMonitoring && isCompleteNoService) return SignalMeasurementTier.DEADZONE
    if (isMonitoring && searching2gFallbackActive) return SignalMeasurementTier.SEARCHING_2G
    // RXSS 31 — WiFi calling registered as the in-service transport with no cellular RAT/RSRP.
    // Checked ahead of the generic no-signal branch below so it gets its own catalogue number
    // and voice wording instead of being reported as a plain "LTE/NR no signal" (RXSS 10).
    if (isMonitoring && isWifiCallingActive && noSignalActive) {
        return SignalMeasurementTier.WIFI_CALLING
    }
    if (isMonitoring && noSignalActive && usesG2SignalTiers()) {
        return SignalMeasurementTier.G2_NO_SIGNAL
    }
    if (isMonitoring && noSignalActive) return SignalMeasurementTier.NO_SIGNAL
    if (isRsrpTooWeakForService(settings)) {
        return if (usesG2SignalTiers()) {
            SignalMeasurementTier.G2_NO_SIGNAL
        } else {
            SignalMeasurementTier.NO_SIGNAL
        }
    }
    if (!cellularAvailable && rsrpDbm == null && rsrqDb == null) {
        return SignalMeasurementTier.UNAVAILABLE
    }

    if (usesG2SignalTiers()) {
        return when (settings.resolveG2SignalStrengthTier(rsrpDbm)) {
            SignalStrengthTier.G2_STRONG -> SignalMeasurementTier.G2_STRONG
            SignalStrengthTier.G2_WEAK -> SignalMeasurementTier.G2_WEAK
            null -> SignalMeasurementTier.G2_NO_SIGNAL
            else -> SignalMeasurementTier.UNAVAILABLE
        }
    }

    val rsrp = rsrpDbm
    if (rsrp != null && settings.isVeryStrongRsrp(rsrp)) {
        return SignalMeasurementTier.VERY_STRONG
    }

    return when (resolveSignalStrengthTier(settings)) {
        SignalStrengthTier.MILD -> SignalMeasurementTier.MILD
        SignalStrengthTier.GOOD -> SignalMeasurementTier.GOOD
        SignalStrengthTier.FAIR -> SignalMeasurementTier.FAIR
        SignalStrengthTier.POOR -> SignalMeasurementTier.POOR
        SignalStrengthTier.CRITICAL -> SignalMeasurementTier.CRITICAL
        SignalStrengthTier.G2_STRONG -> SignalMeasurementTier.G2_STRONG
        SignalStrengthTier.G2_WEAK -> SignalMeasurementTier.G2_WEAK
        SignalStrengthTier.G2_NO_SIGNAL -> SignalMeasurementTier.G2_NO_SIGNAL
        SignalStrengthTier.DEADZONE -> SignalMeasurementTier.DEADZONE
        SignalStrengthTier.NO_SIGNAL -> SignalMeasurementTier.NO_SIGNAL
        SignalStrengthTier.SEARCHING_2G -> SignalMeasurementTier.SEARCHING_2G
        SignalStrengthTier.LIMITED_SERVICE -> SignalMeasurementTier.LIMITED_SERVICE
        SignalStrengthTier.LIMITED_ALT_2G -> SignalMeasurementTier.LIMITED_ALT_2G
        SignalStrengthTier.RSRQ_POOR -> SignalMeasurementTier.RSRQ_POOR
        SignalStrengthTier.WIFI_CALLING -> SignalMeasurementTier.WIFI_CALLING
        null -> SignalMeasurementTier.UNAVAILABLE
    }
}

/** True when the latest measurement maps to RXSS 5 (Level Range D / poor RSRP band). */
fun ConnectivityStats.isTier5PoorSignal(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!signalPermissionGranted) return false
    if (isLimitedService) {
        if (isLimitedServiceAlt2g() || isLimitedServiceNoSignalCamp(settings)) return false
        return settings.resolveSignalStrengthTier(rsrpDbm) == SignalStrengthTier.POOR
    }
    if (usesG2SignalTiers()) return false
    return settings.resolveSignalStrengthTier(rsrpDbm) == SignalStrengthTier.POOR
}

/** True when the latest measurement maps to RXSS 6 (signal low / critical RSRP band). */
fun ConnectivityStats.isTier6CriticalSignal(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!signalPermissionGranted) return false
    if (isLimitedService) {
        if (isLimitedServiceAlt2g() || isLimitedServiceNoSignalCamp(settings)) return false
        return settings.resolveSignalStrengthTier(rsrpDbm) == SignalStrengthTier.CRITICAL
    }
    if (usesG2SignalTiers()) return false
    return settings.resolveSignalStrengthTier(rsrpDbm) == SignalStrengthTier.CRITICAL
}

/** True when camped on home 2G with weak RX (RXSS 8), above the no-signal threshold. */
fun ConnectivityStats.isG2WeakSignal(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (!signalPermissionGranted || !usesG2SignalTiers()) return false
    if (isLimitedService && !isLimitedServiceAlt2g()) return false
    if (isLimitedServiceNoSignalCamp(settings)) return false
    if (noSignalActive || isRsrpTooWeakForService(settings)) return false
    return settings.resolveG2SignalStrengthTier(rsrpDbm) == SignalStrengthTier.G2_WEAK
}

/** RXSS 6 — periodic “signal low” reminders (**VA-15**). RXSS 5 (Level Range D) is signal pulses only. */
fun ConnectivityStats.shouldPlayTier5StylePeriodicVoice(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    return isSignalLowVoiceCamp(settings)
}

/** RXSS used for passive RSRP click interval (RSRQ overlay is RXSS 14, separate). */
fun ConnectivityStats.resolvePassiveClickRateTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): SignalStrengthTier? {
    if (!signalPermissionGranted) return null
    if (isRsrpTooWeakForService(settings)) return null
    if (isLimitedService && !isLimitedServiceNoSignalCamp(settings)) {
        if (isLimitedServiceAlt2g()) {
            return settings.resolveG2SignalStrengthTier(rsrpDbm)
        }
        if (!usesG2SignalTiers()) {
            return settings.resolveSignalStrengthTier(rsrpDbm)
        }
    }
    if (usesG2SignalTiers()) {
        return settings.resolveG2SignalStrengthTier(rsrpDbm)
    }
    return settings.resolveSignalStrengthTier(rsrpDbm)
}

fun ConnectivityStats.shouldPlayWeakSignalTier(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (isRsrpTooWeakForService(settings)) return false
    if (usesG2SignalTiers()) {
        return resolveG2SignalStrengthTier(settings) != null
    }
    if (shouldPlayVeryStrongSignalIndicator(settings)) return false
    val rsrp = rsrpDbm ?: return false
    return rsrp <= settings.veryStrongRsrpMinDbm
}

fun ConnectivityStats.computeSignalStrengthClickIntervalMs(
    settings: PassiveSignalSettings = PassiveSignalSettings(),
    signalPulseDurationMs: Int = DEFAULT_SIGNAL_PULSE_DURATION_MS
): Long {
    val configuredMs = if (usesG2SignalTiers()) {
        val tier = resolveG2SignalStrengthTier(settings)
        (tier?.let { settings.clickIntervalMsForTier(it) } ?: settings.g2WeakTierClickIntervalMs).toLong()
    } else if (shouldPlayVeryStrongSignalIndicator(settings)) {
        settings.veryStrongTierClickIntervalMs.toLong()
    } else {
        resolvePassiveClickRateTier(settings)?.let { settings.clickIntervalMsForTier(it) }
            ?: settings.levelRangeAbcdClickIntervalMs.toLong()
    }
    return SettingsCompatibility.resolveTierClickIntervalMs(
        configuredMs = configuredMs,
        signalPulseDurationMs = signalPulseDurationMs,
        isPassiveOnlySession = isPassiveOnlySession
    )
}

fun ConnectivityStats.computeSignalStrengthClickDurationMs(
    baseDurationMs: Int,
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Int {
    return resolveSignalStrengthTier(settings)?.pulseDurationMs(baseDurationMs) ?: baseDurationMs
}

fun ConnectivityStats.shouldPlayCurrentTierSignalPulse(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (usesG2SignalTiers()) {
        val tier = resolveG2SignalStrengthTier(settings) ?: return false
        return settings.isTierSoundEnabled(tier)
    }
    if (shouldPlayVeryStrongSignalIndicator(settings)) {
        return settings.veryStrongTierSoundEnabled
    }
    if (!shouldPlayWeakSignalTier(settings)) return false
    val tier = resolvePassiveClickRateTier(settings) ?: resolveSignalStrengthTier(settings) ?: return false
    return settings.isTierSoundEnabled(tier)
}

fun ConnectivityStats.shouldPlayVeryStrongSignalIndicator(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    if (usesG2SignalTiers()) return false
    if (!isMonitoring || shouldPlayFlatline(settings)) return false
    if (!cellularAvailable && !shouldPlayLimitedServiceSignalOverlay(settings)) return false
    if (shouldPlayLimitedServiceTone()) return false
    if (shouldPlay2gLimitedServicePulse()) return false
    if (!signalPermissionGranted) return false
    val rsrp = rsrpDbm ?: return false
    return settings.isVeryStrongRsrp(rsrp)
}
