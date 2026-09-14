package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkOperatorNamesTest {

    @Test
    fun resolveHomeMnoName_prefersParentCarrierIdOverSimSpn() {
        assertEquals(
            "Three",
            NetworkOperatorNames.resolveHomeMnoName(
                simOperatorName = "Smarty",
                simCarrierIdName = "Three",
                servingOperatorName = "Three"
            )
        )
    }

    @Test
    fun resolveHomeMnoName_usesCampedHostWhenSpnIsVirtualBrand() {
        assertEquals(
            "EE",
            NetworkOperatorNames.resolveHomeMnoName(
                simOperatorName = "BT Mobile",
                servingOperatorName = "EE",
                homePlmn = "23430",
                servingPlmn = "23430"
            )
        )
    }

    @Test
    fun resolveHomeMnoName_doesNotUseVisitedCampAsHome() {
        assertEquals(
            "Smarty",
            NetworkOperatorNames.resolveHomeMnoName(
                simOperatorName = "Smarty",
                servingOperatorName = "Vodafone UK",
                homePlmn = "23420",
                servingPlmn = "23415"
            )
        )
    }

    @Test
    fun resolveVirtualOperatorName_returnsBrandWhenDistinctFromHomeMno() {
        assertEquals(
            "Smarty",
            NetworkOperatorNames.resolveVirtualOperatorName(
                homeMnoName = "Three",
                simOperatorName = "Smarty",
                simSpecificCarrierIdName = "Smarty",
                simCarrierId = 10,
                simSpecificCarrierId = 99
            )
        )
    }

    @Test
    fun resolveVirtualOperatorName_hiddenWhenCarrierIdsMatch() {
        assertNull(
            NetworkOperatorNames.resolveVirtualOperatorName(
                homeMnoName = "Vodafone UK",
                subscriptionDisplayName = "Work SIM",
                simCarrierId = 15,
                simSpecificCarrierId = 15
            )
        )
    }

    @Test
    fun resolveVirtualOperatorName_hiddenWhenBrandMatchesHome() {
        assertNull(
            NetworkOperatorNames.resolveVirtualOperatorName(
                homeMnoName = "EE",
                simOperatorName = "EE",
                subscriptionCarrierName = "EE"
            )
        )
    }

    @Test
    fun resolveVirtualOperatorName_usesSettingsNameWhenIdsUnknown() {
        assertEquals(
            "giffgaff",
            NetworkOperatorNames.resolveVirtualOperatorName(
                homeMnoName = "O2",
                subscriptionDisplayName = "giffgaff"
            )
        )
    }
}
