package io.github.cloolalang.notspotdetector.network

import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LtePlusDisplayTest {

    @Test
    fun plainLteIsNotThePlusIcon() {
        assertFalse(
            LtePlusDisplay.isLtePlusIcon(
                TelephonyManager.NETWORK_TYPE_LTE,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE
            )
        )
    }

    @Test
    fun lteCarrierAggregationAndAdvancedProAreThePlusIcon() {
        assertTrue(
            LtePlusDisplay.isLtePlusIcon(
                TelephonyManager.NETWORK_TYPE_LTE,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA
            )
        )
        assertTrue(
            LtePlusDisplay.isLtePlusIcon(
                TelephonyManager.NETWORK_TYPE_LTE,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO
            )
        )
    }

    @Test
    fun nrOverrideDoesNotRelabelAsFourGPlus() {
        assertFalse(
            LtePlusDisplay.isLtePlusIcon(
                TelephonyManager.NETWORK_TYPE_NR,
                TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA
            )
        )
    }
}
