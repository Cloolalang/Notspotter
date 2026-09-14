package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun usableRegisteredPair_dropsBlankServiceStatePciZero() {
        assertEquals(null to null, ServingCellSelection.usableRegisteredPair(null, 0))
        assertEquals(null to null, ServingCellSelection.usableRegisteredPair(null, null))
        assertEquals(6300 to 0, ServingCellSelection.usableRegisteredPair(6300, 0))
        assertEquals(6300 to 106, ServingCellSelection.usableRegisteredPair(6300, 106))
    }

    @Test
    fun beatsServingRank_registeredMismatchBeatsNeighbourWithBlankPlmn() {
        assertTrue(
            ServingCellSelection.beatsServingRank(
                matchesRegisteredKeys = false,
                connectionRank = 1,
                plmnRank = 0,
                pciMatchesSignal = false,
                otherMatchesRegisteredKeys = false,
                otherConnectionRank = 0,
                otherPlmnRank = 1,
                otherPciMatchesSignal = false
            )
        )
    }

    @Test
    fun isMetricsStale_afterFifteenSeconds() {
        assertFalse(ServingCellSelection.isMetricsStale(14_999L))
        assertTrue(ServingCellSelection.isMetricsStale(15_001L))
        assertFalse(ServingCellSelection.isMetricsStale(null))
    }

    @Test
    fun preferCampedPair_samePciDifferentEarfcnUsesPrimaryCell() {
        assertTrue(
            ServingCellSelection.preferCampedPairOverRegistered(
                campedEarfcn = 3501,
                campedPci = 328,
                campedConnectionRank = 3,
                registeredEarfcn = 6300,
                registeredPci = 328,
                keyMatchEarfcn = 6300,
                keyMatchPci = 328,
                keyMatchConnectionRank = 2
            )
        )
        val (earfcn, pci) = ServingCellSelection.resolveLteIdentity(
            rankedEarfcn = 3501,
            rankedPci = 328,
            keyMatchEarfcn = 6300,
            keyMatchPci = 328,
            registeredEarfcn = 6300,
            registeredPci = 328,
            rankedConnectionRank = 3,
            keyMatchConnectionRank = 2
        )
        assertEquals(3501, earfcn)
        assertEquals(328, pci)
    }

    @Test
    fun preferCampedPair_neighbourSamePciKeepsServiceState() {
        assertFalse(
            ServingCellSelection.preferCampedPairOverRegistered(
                campedEarfcn = 6400,
                campedPci = 328,
                campedConnectionRank = 0,
                registeredEarfcn = 3501,
                registeredPci = 328,
                keyMatchEarfcn = 6400,
                keyMatchPci = 328,
                keyMatchConnectionRank = 0
            )
        )
    }

    @Test
    fun preferCampedPair_otherSimDifferentPciKeepsServiceState() {
        assertFalse(
            ServingCellSelection.preferCampedPairOverRegistered(
                campedEarfcn = 6400,
                campedPci = 118,
                campedConnectionRank = 3,
                registeredEarfcn = 6300,
                registeredPci = 107,
                keyMatchEarfcn = 6300,
                keyMatchPci = 107,
                keyMatchConnectionRank = 1
            )
        )
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

    @Test
    fun signalStrengthMatchesServingPcis_emptySetsAreNotAMismatch() {
        assertTrue(
            ServingCellSelection.signalStrengthMatchesServingPcis(
                servingLtePci = 106,
                servingNrPci = null,
                signalLtePcis = emptySet(),
                signalNrPcis = emptySet()
            )
        )
        assertTrue(
            ServingCellSelection.signalStrengthMatchesServingPcis(
                servingLtePci = 106,
                servingNrPci = null,
                signalLtePcis = setOf(106),
                signalNrPcis = emptySet()
            )
        )
        assertFalse(
            ServingCellSelection.signalStrengthMatchesServingPcis(
                servingLtePci = 106,
                servingNrPci = null,
                signalLtePcis = setOf(87),
                signalNrPcis = emptySet()
            )
        )
    }

    @Test
    fun isSignalQualityFresh_visitedLimitedServiceIsNeverFresh() {
        assertFalse(
            ServingCellSelection.isSignalQualityFresh(
                newestAgeMs = 0L,
                isVisitedLimitedService = true
            )
        )
        assertFalse(
            ServingCellSelection.isSignalQualityFresh(
                newestAgeMs = null,
                isVisitedLimitedService = true
            )
        )
        assertTrue(
            ServingCellSelection.isSignalQualityFresh(
                newestAgeMs = 14_999L,
                isVisitedLimitedService = false
            )
        )
        assertFalse(
            ServingCellSelection.isSignalQualityFresh(
                newestAgeMs = 15_001L,
                isVisitedLimitedService = false
            )
        )
        assertTrue(
            ServingCellSelection.isSignalQualityFresh(
                newestAgeMs = null,
                isVisitedLimitedService = false
            )
        )
    }

    @Test
    fun shouldUseSignalStrengthForServingMetrics_visitedLimitedDoesNotUseFrozenSignal() {
        assertFalse(
            ServingCellSelection.shouldUseSignalStrengthForServingMetrics(
                hasCampedIdentity = true,
                signalMatchesServing = true,
                signalQualityStale = false,
                isVisitedLimitedService = true
            )
        )
        assertFalse(
            ServingCellSelection.shouldUseSignalStrengthForServingMetrics(
                hasCampedIdentity = true,
                signalMatchesServing = true,
                signalQualityStale = true,
                isVisitedLimitedService = false
            )
        )
        assertTrue(
            ServingCellSelection.shouldUseSignalStrengthForServingMetrics(
                hasCampedIdentity = false,
                signalMatchesServing = false,
                signalQualityStale = false,
                isVisitedLimitedService = false
            )
        )
        assertTrue(
            ServingCellSelection.shouldUseSignalStrengthForServingMetrics(
                hasCampedIdentity = true,
                signalMatchesServing = true,
                signalQualityStale = false,
                isVisitedLimitedService = false
            )
        )
    }

    @Test
    fun pickServingMetric_usesSignalOnlyWhenAllowed() {
        assertEquals(
            -98,
            ServingCellSelection.pickServingMetric(
                fromCell = null,
                fromSignal = -98,
                useSignal = true
            )
        )
        assertNull(
            ServingCellSelection.pickServingMetric(
                fromCell = null,
                fromSignal = -98,
                useSignal = false
            )
        )
        assertEquals(
            -102,
            ServingCellSelection.pickServingMetric(
                fromCell = -102,
                fromSignal = -98,
                useSignal = true
            )
        )
    }
}
