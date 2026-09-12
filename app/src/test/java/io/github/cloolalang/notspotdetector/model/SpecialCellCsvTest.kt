package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpecialCellCsvTest {

    @Test
    fun parse_exampleRows_readsRequiredAndOptionalFields() {
        val catalog = SpecialCellCsv.parse(EXAMPLE)

        assertEquals(23, catalog.size)
        val robinHood = catalog.cells.first()
        assertEquals("Robin Hood", robinHood.site)
        assertEquals("Streetworks", robinHood.type)
        assertEquals(SpecialCellRat.G4, robinHood.rat)
        assertEquals(6300, robinHood.channel)
        assertEquals(107, robinHood.pci)
        assertEquals("23415", robinHood.plmn)
        assertEquals("Tesco", catalog.cells.last().site)
        assertEquals(2850, catalog.cells.last().channel)
        assertEquals(340, catalog.cells.last().pci)
        assertTrue(catalog.warnings.isEmpty())
    }

    @Test
    fun parse_quotedSiteWithComma_keepsComma() {
        val catalog = SpecialCellCsv.parse(
            """
            site,type,mno,rat,sector,channel,pci
            "Ikea, Oxford St",DAS,EE,5G,S1,648125,99
            """.trimIndent()
        )

        assertEquals("Ikea, Oxford St", catalog.cells.single().site)
    }

    @Test
    fun parse_duplicateKey_addsWarningButKeepsRows() {
        val catalog = SpecialCellCsv.parse(
            """
            site,type,mno,rat,sector,channel,pci
            A,DAS,EE,4G,S1,6300,101
            B,DAS,EE,4G,S2,6300,101
            """.trimIndent()
        )

        assertEquals(2, catalog.size)
        assertEquals(1, catalog.warnings.size)
    }

    @Test
    fun match_prefersPlmnWhenTwoRowsShareChannelAndPci() {
        val catalog = SpecialCellCsv.parse(
            """
            site,type,mno,rat,sector,channel,pci,plmn
            Home,DAS,Vodafone,4G,S1,1800,42,23415
            Other,DAS,EE,4G,S1,1800,42,23430
            """.trimIndent()
        )
        val stats = ConnectivityStats(
            lteEarfcn = 1800,
            ltePci = 42,
            plmn = "23415",
            cellIdentityPermissionGranted = true
        )

        val match = SpecialCellMatcher.match(stats, catalog)

        assertEquals("Home", match?.cell?.site)
        assertEquals(SpecialCellLayer.LTE, match?.layer)
    }

    @Test
    fun match_nrRow_usesServingNrIdentity() {
        val catalog = SpecialCellCsv.parse(
            """
            site,type,mno,rat,sector,channel,pci
            Ikea-Oxfordst,DAS,EE,5G,S1,648125,99
            """.trimIndent()
        )
        val stats = ConnectivityStats(
            radioAccessType = "5G EN-DC",
            lteEarfcn = 1800,
            ltePci = 42,
            nrEarfcn = 648125,
            nrPci = 99,
            cellIdentityPermissionGranted = true
        )

        val match = SpecialCellMatcher.match(stats, catalog)

        assertEquals("Ikea-Oxfordst", match?.cell?.site)
        assertEquals(SpecialCellLayer.NR, match?.layer)
    }

    @Test
    fun match_unknownIdentity_returnsNull() {
        val catalog = SpecialCellCsv.parse(
            """
            site,type,mno,rat,sector,channel,pci
            Strand,SmallCell,VMO2,4G,S1,1228,456
            """.trimIndent()
        )
        val stats = ConnectivityStats(
            lteEarfcn = 1800,
            ltePci = 42,
            cellIdentityPermissionGranted = true
        )

        assertNull(SpecialCellMatcher.match(stats, catalog))
    }

    @Test
    fun match_listedChannelAndPci_stillHitsWhenPhoneOmitsPlmn() {
        val catalog = SpecialCellCsv.parse(
            """
            site,type,mno,rat,sector,channel,pci,plmn
            Robin Hood,Streetworks,Vodafone,4G,S1,6300,107,23415
            """.trimIndent()
        )
        val stats = ConnectivityStats(
            lteEarfcn = 6300,
            ltePci = 107,
            plmn = null,
            cellIdentityPermissionGranted = true
        )

        assertEquals("Robin Hood", SpecialCellMatcher.match(stats, catalog)?.cell?.site)
    }

    @Test
    fun match_normalizesDashedPlmn() {
        val catalog = SpecialCellCsv.parse(
            """
            site,type,mno,rat,sector,channel,pci,plmn
            Robin Hood,Streetworks,Vodafone,4G,S1,6300,107,23415
            """.trimIndent()
        )
        val stats = ConnectivityStats(
            lteEarfcn = 6300,
            ltePci = 107,
            plmn = "234-15",
            cellIdentityPermissionGranted = true
        )

        assertEquals("Robin Hood", SpecialCellMatcher.match(stats, catalog)?.cell?.site)
    }

    @Test
    fun announcement_speaksTypeSiteAndSector() {
        val cell = SpecialCell(
            site = "Ikea-Oxfordst",
            type = "DAS",
            mno = "EE",
            rat = SpecialCellRat.G5,
            sector = "S1",
            channel = 648125,
            pci = 99,
            speakAs = "Ikea Oxford Street"
        )

        assertEquals(
            "D A S, Ikea Oxford Street, sector one",
            SpecialCellAnnouncement.format(
                cell = cell,
                speakType = true,
                speakSite = true,
                speakSector = true
            )
        )
    }

    companion object {
        private val EXAMPLE = """
            site,type,mno,rat,sector,channel,pci,plmn,speak,speak_as,notes
            Robin Hood,Streetworks,Vodafone,4G,S1,6300,107,23415,yes,Robin Hood,
            Robin Hood,Streetworks,Vodafone,4G,S1,247,107,23415,yes,Robin Hood,
            Robin Hood,Streetworks,Vodafone,4G,S2,6300,106,23415,yes,Robin Hood,
            Robin Hood,Streetworks,Vodafone,4G,S2,247,106,23415,yes,Robin Hood,
            Robin Hood,Streetworks,Vodafone,4G,S3,6300,105,23415,yes,Robin Hood,
            Robin Hood,Streetworks,Vodafone,4G,S3,247,105,23415,yes,Robin Hood,
            Hill Farm,Macro,Vodafone,4G,S1,6300,327,23415,yes,Hill Farm,
            Hill Farm,Macro,Vodafone,4G,S1,3501,327,23415,yes,Hill Farm,
            Hill Farm,Macro,Vodafone,4G,S1,223,327,23415,yes,Hill Farm,
            Hill Farm,Macro,Vodafone,4G,S2,6300,329,23415,yes,Hill Farm,
            Hill Farm,Macro,Vodafone,4G,S2,3501,329,23415,yes,Hill Farm,
            Hill Farm,Macro,Vodafone,4G,S2,223,329,23415,yes,Hill Farm,
            Hill Farm,Macro,Vodafone,4G,S3,6300,328,23415,yes,Hill Farm,
            Hill Farm,Macro,Vodafone,4G,S3,3501,328,23415,yes,Hill Farm,
            Hill Farm,Macro,Vodafone,4G,S3,223,328,23415,yes,Hill Farm,
            Tesco,Macro,Vodafone,4G,S1,6300,339,23415,yes,Tesco,
            Tesco,Macro,Vodafone,4G,S1,3501,339,23415,yes,Tesco,
            Tesco,Macro,Vodafone,4G,S1,223,339,23415,yes,Tesco,
            Tesco,Macro,Vodafone,4G,S1,2850,339,23415,yes,Tesco,
            Tesco,Macro,Vodafone,4G,S2,6300,340,23415,yes,Tesco,
            Tesco,Macro,Vodafone,4G,S2,3501,340,23415,yes,Tesco,
            Tesco,Macro,Vodafone,4G,S2,223,340,23415,yes,Tesco,
            Tesco,Macro,Vodafone,4G,S2,2850,340,23415,yes,Tesco,
        """.trimIndent()
    }
}
