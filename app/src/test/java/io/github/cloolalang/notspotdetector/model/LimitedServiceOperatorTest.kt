package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitedServiceOperatorTest {

    @Test
    fun resolveCampedVisitedOperatorName_returnsServingWhenDifferentFromHomeWithoutLimitedService() {
        val stats = ConnectivityStats(
            homeNetworkOperatorName = "Vodafone",
            servingNetworkOperatorName = "EE",
            networkOperatorName = "EE",
            homePlmn = "23415",
            plmn = "23430"
        )

        assertEquals("EE", stats.resolveCampedVisitedOperatorName())
    }

    @Test
    fun resolveLimitedServiceVisitedOperatorName_returnsServingWhenDifferentFromHome() {
        val stats = ConnectivityStats(
            isLimitedService = true,
            homeNetworkOperatorName = "Vodafone UK",
            servingNetworkOperatorName = "EE",
            homePlmn = "23415",
            plmn = "23430"
        )

        assertEquals("EE", stats.resolveLimitedServiceVisitedOperatorName())
    }

    @Test
    fun resolveCampedVisitedOperatorName_usesPlmnWhenNamesMatchButPlmnsDiffer() {
        val stats = ConnectivityStats(
            homeNetworkOperatorName = "EE",
            servingNetworkOperatorName = "EE",
            networkOperatorName = "EE",
            homePlmn = "23430",
            plmn = "23415"
        )

        assertEquals("23415", stats.resolveCampedVisitedOperatorName())
    }

    @Test
    fun resolveServingOperatorFromCell_prefersCellNameWhenPlmnIsVisited() {
        assertEquals(
            "Vodafone",
            resolveServingOperatorFromCell(
                telephonyServingName = "EE",
                homeName = "EE",
                homePlmn = "23430",
                cellServingName = "Vodafone",
                cellServingPlmn = "23415"
            )
        )
    }

    @Test
    fun resolveServingOperatorFromCell_fallsBackToCellPlmnWhenTelephonyNameIsHome() {
        assertEquals(
            "23415",
            resolveServingOperatorFromCell(
                telephonyServingName = "EE",
                homeName = "EE",
                homePlmn = "23430",
                cellServingName = null,
                cellServingPlmn = "23415"
            )
        )
    }

    @Test
    fun resolveLimitedServiceVisitedOperatorName_returnsNullWhenSameOperator() {
        val stats = ConnectivityStats(
            isLimitedService = true,
            homeNetworkOperatorName = "Vodafone UK",
            servingNetworkOperatorName = "Vodafone UK",
            homePlmn = "23415",
            plmn = "23415"
        )

        assertNull(stats.resolveLimitedServiceVisitedOperatorName())
    }

    @Test
    fun limitedServiceVisitedOperatorChanged_detectsServingOperatorChange() {
        val previous = ConnectivityStats(
            isLimitedService = true,
            homeNetworkOperatorName = "Vodafone UK",
            servingNetworkOperatorName = "EE",
            homePlmn = "23415",
            plmn = "23430"
        )
        val next = previous.copy(
            servingNetworkOperatorName = "O2",
            plmn = "23410"
        )

        assertTrue(next.limitedServiceVisitedOperatorChanged(previous))
    }

    @Test
    fun limitedServiceVisitedOperatorChanged_ignoresInitialVisitedAppearance() {
        val previous = ConnectivityStats(
            isLimitedService = true,
            homeNetworkOperatorName = "Vodafone UK",
            servingNetworkOperatorName = null
        )
        val next = previous.copy(servingNetworkOperatorName = "EE", plmn = "23430")

        assertFalse(next.limitedServiceVisitedOperatorChanged(previous))
    }
}
