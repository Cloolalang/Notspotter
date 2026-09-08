package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitedServiceOperatorTest {

    @Test
    fun resolveLimitedServiceAlternativeOperatorName_returnsServingWhenDifferentFromHome() {
        val stats = ConnectivityStats(
            isLimitedService = true,
            homeNetworkOperatorName = "Vodafone UK",
            servingNetworkOperatorName = "EE",
            homePlmn = "23415",
            plmn = "23430"
        )

        assertEquals("EE", stats.resolveLimitedServiceAlternativeOperatorName())
    }

    @Test
    fun resolveLimitedServiceAlternativeOperatorName_returnsNullWhenSameOperator() {
        val stats = ConnectivityStats(
            isLimitedService = true,
            homeNetworkOperatorName = "Vodafone UK",
            servingNetworkOperatorName = "Vodafone UK",
            homePlmn = "23415",
            plmn = "23415"
        )

        assertNull(stats.resolveLimitedServiceAlternativeOperatorName())
    }

    @Test
    fun limitedServiceAlternativeOperatorChanged_detectsServingOperatorChange() {
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

        assertTrue(next.limitedServiceAlternativeOperatorChanged(previous))
    }

    @Test
    fun limitedServiceAlternativeOperatorChanged_ignoresInitialAlternativeAppearance() {
        val previous = ConnectivityStats(
            isLimitedService = true,
            homeNetworkOperatorName = "Vodafone UK",
            servingNetworkOperatorName = null
        )
        val next = previous.copy(servingNetworkOperatorName = "EE", plmn = "23430")

        assertFalse(next.limitedServiceAlternativeOperatorChanged(previous))
    }
}
