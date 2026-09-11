package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkOperatorDisplayTest {

    @Test
    fun formatHomeOperatorDisplay_usesHomeNameInLimitedService() {
        val stats = ConnectivityStats(
            isLimitedService = true,
            homeNetworkOperatorName = "Vodafone UK",
            servingNetworkOperatorName = "EE",
            networkOperatorName = "EE",
            homePlmn = "23415",
            plmn = "23430"
        )

        assertEquals("Vodafone UK", stats.formatHomeOperatorDisplay())
        assertEquals("EE", stats.formatVisitedOperatorDisplay())
        assertEquals("Vodafone UK", stats.formatNetworkOperatorDisplay())
    }

    @Test
    fun formatVisitedOperatorDisplay_nullWhenNotLimitedService() {
        val stats = ConnectivityStats(
            networkOperatorName = "Vodafone UK",
            homeNetworkOperatorName = "Vodafone UK",
            plmn = "23415"
        )

        assertEquals("Vodafone UK", stats.formatHomeOperatorDisplay())
        assertNull(stats.formatVisitedOperatorDisplay())
    }

    @Test
    fun formatVisitedOperatorDisplay_usesPlmnWhenTelephonyStillShowsHomeName() {
        val stats = ConnectivityStats(
            isLimitedService = true,
            homeNetworkOperatorName = "EE",
            servingNetworkOperatorName = "EE",
            networkOperatorName = "EE",
            homePlmn = "23430",
            plmn = "23415"
        )

        assertEquals("EE", stats.formatHomeOperatorDisplay())
        assertEquals("23415", stats.formatVisitedOperatorDisplay())
    }
}
