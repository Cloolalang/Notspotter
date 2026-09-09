package io.github.cloolalang.notspotdetector.ui

import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.Rxss
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RxssLabelsTest {

    @Test
    fun rxssStateNameResId_mapsImplementedThresholdTiers() {
        assertEquals(R.string.rxss_state_deadzone, rxssStateNameResId(Rxss.DEADZONE))
        assertEquals(R.string.rxss_state_signal_high, rxssStateNameResId(Rxss.SIGNAL_HIGH))
        assertEquals(R.string.rxss_state_level_range_a, rxssStateNameResId(Rxss.LEVEL_RANGE_A))
        assertEquals(R.string.rxss_state_signal_low, rxssStateNameResId(Rxss.SIGNAL_LOW))
        assertEquals(R.string.rxss_state_g2_good, rxssStateNameResId(Rxss.G2_GOOD))
        assertEquals(R.string.rxss_state_cell_change, rxssStateNameResId(Rxss.CELL_CHANGE))
        assertEquals(R.string.rxss_state_tech_change_to_4g, rxssStateNameResId(Rxss.TECH_CHANGE_TO_4G))
        assertEquals(R.string.rxss_state_lte_nr_no_signal, rxssStateNameResId(Rxss.LTE_NR_NO_SIGNAL))
        assertEquals(R.string.rxss_state_rsrq_poor, rxssStateNameResId(Rxss.RSRQ_POOR))
    }

    @Test
    fun rxssStateNameResId_returnsNullForUnimplementedTiers() {
        assertNull(rxssStateNameResId(Rxss.LIMITED_HOME_4G))
        assertNull(rxssStateNameResId(99))
    }
}
