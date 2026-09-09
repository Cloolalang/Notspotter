package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkOperatorDisplayTest {

    @Test
    fun formatNetworkOperatorDisplay_showsHomeAndVisitedInLimitedService() {
        val stats = ConnectivityStats(
            isLimitedService = true,
            homeNetworkOperatorName = "Vodafone UK",
            servingNetworkOperatorName = "EE",
            homePlmn = "23415",
            plmn = "23430"
        )

        assertEquals("Vodafone UK · EE", stats.formatNetworkOperatorDisplay())
    }

    @Test
    fun formatNetworkOperatorDisplay_usesServingWhenNotLimitedService() {
        val stats = ConnectivityStats(
            networkOperatorName = "Vodafone UK",
            plmn = "23415"
        )

        assertEquals("Vodafone UK", stats.formatNetworkOperatorDisplay())
    }
}
