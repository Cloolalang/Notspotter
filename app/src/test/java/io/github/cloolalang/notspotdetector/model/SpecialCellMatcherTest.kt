package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpecialCellMatcherTest {

    private val catalog = SpecialCellCatalog(
        cells = listOf(
            SpecialCell(
                site = "Robin Hood",
                type = "Streetworks",
                mno = "Vodafone",
                rat = SpecialCellRat.G4,
                sector = "S2",
                channel = 6300,
                pci = 106,
                plmn = "23415"
            ),
            SpecialCell(
                site = "Old 2G",
                type = "Macro",
                mno = "Vodafone",
                rat = SpecialCellRat.G2,
                sector = "S1",
                channel = 62,
                pci = 12,
                plmn = "23415"
            ),
            SpecialCell(
                site = "Hill Farm",
                type = "Macro",
                mno = "Vodafone",
                rat = SpecialCellRat.G4,
                sector = "S3",
                channel = 3501,
                pci = 328,
                plmn = "23415"
            )
        )
    )

    @Test
    fun reselectHoldMs_coversSeveralMeasurementIntervals() {
        assertEquals(4_000L, SpecialCellMatcher.reselectHoldMs(1_000L))
        assertEquals(3_000L, SpecialCellMatcher.reselectHoldMs(500L))
        assertEquals(8_000L, SpecialCellMatcher.reselectHoldMs(10_000L))
    }

    @Test
    fun match_usesLteEarfcnPciOnly() {
        val stats = ConnectivityStats(
            isOn2g = false,
            radioAccessType = "4G",
            lteEarfcn = 6300,
            ltePci = 106,
            gsmEarfcn = 62,
            gsmBsic = 12,
            plmn = "23415",
            cellIdentityPermissionGranted = true
        )

        val match = SpecialCellMatcher.match(stats, catalog)

        assertEquals("Robin Hood", match?.cell?.site)
        assertEquals(SpecialCellLayer.LTE, match?.layer)
        assertEquals("6300/106", SpecialCellMatcher.servingIdentitySummary(stats))
    }

    @Test
    fun match_ignores2gIdentityAnd2gCatalogRows() {
        val stats = ConnectivityStats(
            isOn2g = true,
            radioAccessType = "2G",
            gsmEarfcn = 62,
            gsmBsic = 12,
            plmn = "23415",
            cellIdentityPermissionGranted = true
        )

        assertNull(SpecialCellMatcher.match(stats, catalog))
        assertNull(SpecialCellMatcher.servingIdentitySummary(stats))
    }

    @Test
    fun match_endcStillUses4gEarfcnPci() {
        val stats = ConnectivityStats(
            radioAccessType = "5G EN-DC",
            lteEarfcn = 6300,
            ltePci = 106,
            nrEarfcn = 648125,
            nrPci = 99,
            plmn = "23415",
            cellIdentityPermissionGranted = true
        )

        val match = SpecialCellMatcher.match(stats, catalog)

        assertEquals("Robin Hood", match?.cell?.site)
        assertEquals("6300/106", SpecialCellMatcher.servingIdentitySummary(stats))
    }

    @Test
    fun match_roamingSimLimitedServiceStillHitsListedCell() {
        val stats = ConnectivityStats(
            radioAccessType = "4G",
            lteEarfcn = 3501,
            ltePci = 328,
            plmn = "23410",
            homePlmn = "20801",
            isLimitedService = true,
            networkServiceMode = NetworkServiceMode.LIMITED_SERVICE,
            cellIdentityPermissionGranted = true
        )

        val match = SpecialCellMatcher.match(stats, catalog)

        assertEquals("Hill Farm", match?.cell?.site)
        assertEquals("3501/328", SpecialCellMatcher.servingIdentitySummary(stats))
    }

    @Test
    fun match_4gEarfcnPciHits4gRow() {
        val stats = ConnectivityStats(
            radioAccessType = "4G",
            lteEarfcn = 3501,
            ltePci = 328,
            plmn = "23415",
            cellIdentityPermissionGranted = true
        )

        val match = SpecialCellMatcher.match(stats, catalog)

        assertEquals("Hill Farm", match?.cell?.site)
        assertEquals("3501/328", SpecialCellMatcher.servingIdentitySummary(stats))
    }
}
