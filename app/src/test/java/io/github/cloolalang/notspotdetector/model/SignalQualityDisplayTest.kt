package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SignalQualityDisplayTest {

    @Test
    fun displayedRsrpAndRsrq_blankWhenCellInfoIsNotFresh() {
        val stale = ConnectivityStats(
            rsrpDbm = -98,
            rsrqDb = -12,
            signalQualityFresh = false
        )

        assertNull(stale.displayedRsrpDbm())
        assertNull(stale.displayedRsrqDb())
    }

    @Test
    fun displayedRsrpAndRsrq_keepLiveValues() {
        val live = ConnectivityStats(
            rsrpDbm = -98,
            rsrqDb = -12,
            signalQualityFresh = true
        )

        assertEquals(-98, live.displayedRsrpDbm())
        assertEquals(-12, live.displayedRsrqDb())
    }

    @Test
    fun displayedRxLev_shownInHomeLimitedService() {
        val homeLimited2g = ConnectivityStats(
            isLimitedService = true,
            isOn2g = true,
            rsrpDbm = -85,
            homeNetworkOperatorName = "Vodafone",
            servingNetworkOperatorName = "Vodafone",
            homePlmn = "23415",
            plmn = "23415",
            signalQualityFresh = true
        )

        assertEquals(-85, homeLimited2g.displayedRsrpDbm())
    }

    @Test
    fun displayedRsrpAndRsrq_shownInHomeLimited4g() {
        val homeLimited4g = ConnectivityStats(
            isLimitedService = true,
            rsrpDbm = -102,
            rsrqDb = -14,
            homeNetworkOperatorName = "Vodafone",
            servingNetworkOperatorName = "Vodafone",
            homePlmn = "23415",
            plmn = "23415",
            signalQualityFresh = true
        )

        assertEquals(-102, homeLimited4g.displayedRsrpDbm())
        assertEquals(-14, homeLimited4g.displayedRsrqDb())
    }

    @Test
    fun displayedRsrpAndRxLev_blankInVisitedLimitedService() {
        val visited4g = ConnectivityStats(
            isLimitedService = true,
            rsrpDbm = -102,
            rsrqDb = -14,
            homeNetworkOperatorName = "Vodafone",
            servingNetworkOperatorName = "EE",
            homePlmn = "23415",
            plmn = "23430",
            signalQualityFresh = true
        )
        val visited2g = visited4g.copy(
            isOn2g = true,
            rsrpDbm = -85,
            rsrqDb = null
        )

        assertNull(visited4g.displayedRsrpDbm())
        assertNull(visited4g.displayedRsrqDb())
        assertNull(visited2g.displayedRsrpDbm())
    }
}
