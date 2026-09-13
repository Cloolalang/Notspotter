package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SimOperatorSelectionTest {

    @Test
    fun format_auto() {
        assertEquals("Auto", formatSimOperatorSelectionSuffix(SimOperatorSelectionMode.AUTO))
    }

    @Test
    fun format_unknown_isNull() {
        assertNull(formatSimOperatorSelectionSuffix(SimOperatorSelectionMode.UNKNOWN))
    }

    @Test
    fun format_manual_isTypeOnly() {
        assertEquals("Manual", formatSimOperatorSelectionSuffix(SimOperatorSelectionMode.MANUAL))
    }

    @Test
    fun resolve_prefersServingNameWhenPlmnMatchesServing() {
        assertEquals(
            "EE",
            resolveManualSelectedOperatorName(
                selectedPlmn = "23430",
                homePlmn = "23415",
                servingPlmn = "23430",
                homeOperatorName = "Vodafone UK",
                servingOperatorName = "EE"
            )
        )
    }

    @Test
    fun resolve_unknownPlmn_doesNotUseStaleServingName() {
        assertEquals(
            "23430",
            resolveManualSelectedOperatorName(
                selectedPlmn = "23430",
                homePlmn = "23415",
                servingPlmn = "23410",
                homeOperatorName = "Vodafone UK",
                servingOperatorName = "O2"
            )
        )
    }

    @Test
    fun resolve_fallsBackToPlmnWhenNamesMissing() {
        assertEquals(
            "23430",
            resolveManualSelectedOperatorName(
                selectedPlmn = "23430",
                homePlmn = "23415",
                servingPlmn = null,
                homeOperatorName = null,
                servingOperatorName = null
            )
        )
    }
}
