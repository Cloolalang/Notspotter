package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServingCellSelectionTest {

    @Test
    fun matchesRegisteredKeys_requiresEarfcnAndPciWhenBothSidesHaveThem() {
        assertTrue(
            ServingCellSelection.matchesRegisteredKeys(
                earfcn = 6300,
                pci = 106,
                registeredEarfcn = 6300,
                registeredPci = 106
            )
        )
        assertFalse(
            ServingCellSelection.matchesRegisteredKeys(
                earfcn = 6300,
                pci = 107,
                registeredEarfcn = 6300,
                registeredPci = 106
            )
        )
        assertFalse(
            ServingCellSelection.matchesRegisteredKeys(
                earfcn = 6400,
                pci = 118,
                registeredEarfcn = 6300,
                registeredPci = 118
            )
        )
    }

    @Test
    fun pickRegisteredIdentity_prefersExpectedPlmnAndKeepsPairTogether() {
        val picked = ServingCellSelection.pickRegisteredIdentity(
            candidates = listOf(
                ServingCellSelection.RegisteredIdentity(6400, 118, "23410"),
                ServingCellSelection.RegisteredIdentity(6300, 107, "23415")
            ),
            expectedPlmns = listOf("23415")
        )

        assertEquals(6300, picked?.earfcn)
        assertEquals(107, picked?.pci)
    }

    @Test
    fun resolveLteIdentity_overridesOtherSimWhenServiceStateDisagrees() {
        val (earfcn, pci) = ServingCellSelection.resolveLteIdentity(
            rankedEarfcn = 6400,
            rankedPci = 118,
            keyMatchEarfcn = 6300,
            keyMatchPci = 107,
            registeredEarfcn = 6300,
            registeredPci = 107
        )

        assertEquals(6300, earfcn)
        assertEquals(107, pci)
    }

    @Test
    fun resolveLteIdentity_serviceStateWinsOverPciOnlyNeighbour() {
        val (earfcn, pci) = ServingCellSelection.resolveLteIdentity(
            rankedEarfcn = 6400,
            rankedPci = 328,
            keyMatchEarfcn = 6400,
            keyMatchPci = 328,
            registeredEarfcn = 3501,
            registeredPci = 328
        )

        assertEquals(3501, earfcn)
        assertEquals(328, pci)
    }

    @Test
    fun isStaleCell_dropsRemovedSimCacheOlderThanFreshGroup() {
        assertTrue(ServingCellSelection.isStaleCell(ageMs = 4_507_078L, newestAgeMs = 37_636L))
        assertFalse(ServingCellSelection.isStaleCell(ageMs = 37_636L, newestAgeMs = 37_636L))
        assertFalse(ServingCellSelection.isStaleCell(ageMs = 568_521L, newestAgeMs = 568_521L))
        assertTrue(ServingCellSelection.isStaleCell(ageMs = 180_000L, newestAgeMs = null))
    }

    @Test
    fun resolveLteIdentity_usesServiceStateWhenKeyMatchMissing() {
        val (earfcn, pci) = ServingCellSelection.resolveLteIdentity(
            rankedEarfcn = 6400,
            rankedPci = 118,
            keyMatchEarfcn = null,
            keyMatchPci = null,
            registeredEarfcn = 6300,
            registeredPci = 107
        )

        assertEquals(6300, earfcn)
        assertEquals(107, pci)
    }
}
