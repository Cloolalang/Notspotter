package io.github.cloolalang.notspotdetector.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.RSRQ_POOR_TIER_NUMBER
import io.github.cloolalang.notspotdetector.model.Rxss
import io.github.cloolalang.notspotdetector.model.SignalMeasurementTier

@StringRes
fun rxssStateNameResId(tierNumber: Int): Int? {
    return when (tierNumber) {
        Rxss.DEADZONE -> R.string.rxss_state_deadzone
        Rxss.SIGNAL_HIGH -> R.string.rxss_state_signal_high
        Rxss.LEVEL_RANGE_A -> R.string.rxss_state_level_range_a
        Rxss.LEVEL_RANGE_B -> R.string.rxss_state_level_range_b
        Rxss.LEVEL_RANGE_C -> R.string.rxss_state_level_range_c
        Rxss.LEVEL_RANGE_D -> R.string.rxss_state_level_range_d
        Rxss.SIGNAL_LOW -> R.string.rxss_state_signal_low
        Rxss.G2_GOOD -> R.string.rxss_state_g2_good
        Rxss.G2_WEAK -> R.string.rxss_state_g2_weak
        Rxss.CELL_CHANGE -> R.string.rxss_state_cell_change
        Rxss.TECH_CHANGE_TO_2G -> R.string.rxss_state_tech_change_to_2g
        Rxss.TECH_CHANGE_TO_4G -> R.string.rxss_state_tech_change_to_4g
        Rxss.TECH_CHANGE_TO_5G_ENDC -> R.string.rxss_state_tech_change_to_5g_endc
        Rxss.LTE_NR_NO_SIGNAL -> R.string.rxss_state_lte_nr_no_signal
        Rxss.SEARCH_HOME_2G -> R.string.rxss_state_search_home_2g
        Rxss.LIMITED_ALT_4G -> R.string.rxss_state_limited_alt_4g
        Rxss.LIMITED_ALT_2G -> R.string.rxss_state_limited_alt_2g
        Rxss.LIMITED_4G_NO_SIGNAL -> R.string.rxss_state_limited_4g_no_signal
        Rxss.LIMITED_ALT_2G_NO_SIGNAL -> R.string.rxss_state_limited_alt_2g_no_signal
        Rxss.RSRQ_POOR -> R.string.rxss_state_rsrq_poor
        Rxss.HOME_2G_NO_SIGNAL -> R.string.rxss_state_home_2g_no_signal
        else -> null
    }
}

@Composable
fun rxssSectionTitle(tierNumber: Int): String {
    val stateNameRes = rxssStateNameResId(tierNumber)
    return if (stateNameRes != null) {
        stringResource(
            R.string.passive_signal_tier_section,
            tierNumber,
            stringResource(stateNameRes)
        )
    } else {
        stringResource(R.string.passive_signal_tier_section_fallback, tierNumber)
    }
}

@Composable
fun rxssClickIntervalLabel(tierNumber: Int): String {
    return stringResource(R.string.passive_signal_tier_click_interval, tierNumber)
}

@Composable
fun rxssSoundEnabledLabel(tierNumber: Int): String {
    return stringResource(R.string.passive_signal_tier_sound_enabled, tierNumber)
}

@Composable
fun rxssBoundaryAboveLabel(tierNumber: Int): String {
    return stringResource(R.string.passive_signal_boundary_tier_above, tierNumber)
}

@Composable
fun signalMeasurementDisplayLabel(
    tier: SignalMeasurementTier,
    rsrqTierActive: Boolean,
    limitedServiceSignalOverlayRxss: Int? = null
): String {
    tier.rxssNumber?.let { number ->
        val rxssParts = mutableListOf(number)
        if (tier == SignalMeasurementTier.LIMITED_SERVICE ||
            tier == SignalMeasurementTier.LIMITED_ALT_2G
        ) {
            limitedServiceSignalOverlayRxss?.let { rxssParts.add(it) }
        }
        if (rsrqTierActive &&
            tier != SignalMeasurementTier.LIMITED_4G_NO_SIGNAL &&
            tier != SignalMeasurementTier.LIMITED_ALT_2G_NO_SIGNAL
        ) {
            rxssParts.add(RSRQ_POOR_TIER_NUMBER)
        }
        return if (rxssParts.size == 1) {
            stringResource(R.string.signal_tier_number, rxssParts.single())
        } else {
            stringResource(
                R.string.signal_tier_composite,
                rxssParts.joinToString(" · ") { part -> "RXSS $part" }
            )
        }
    }
    if (rsrqTierActive) {
        return stringResource(R.string.signal_tier_number, RSRQ_POOR_TIER_NUMBER)
    }
    return stringResource(
        when (tier) {
            SignalMeasurementTier.PERMISSION_REQUIRED -> R.string.signal_permission_required
            SignalMeasurementTier.UNAVAILABLE -> R.string.signal_tier_unavailable
            else -> R.string.signal_tier_unavailable
        }
    )
}
