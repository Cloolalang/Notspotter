package io.github.cloolalang.notspotdetector.model

object SpecialCellAnnouncement {

    /** VA-19 exit phrase when leaving a listed cell for a camped unlisted cell. */
    const val EXIT_MACRO_CELL = "macro cell"

    fun format(
        cell: SpecialCell,
        speakType: Boolean,
        speakSite: Boolean,
        speakSector: Boolean
    ): String {
        if (!cell.speak) return ""
        val parts = mutableListOf<String>()
        if (speakType) speakType(cell.type)?.let(parts::add)
        if (speakSite) speakSite(cell.displaySite)?.let(parts::add)
        if (speakSector) speakSector(cell.sector)?.let(parts::add)
        return parts.joinToString(", ")
    }

    fun previewText(
        catalog: SpecialCellCatalog,
        match: SpecialCellMatch?,
        speakType: Boolean,
        speakSite: Boolean,
        speakSector: Boolean
    ): String {
        val cell = match?.cell ?: catalog.cells.firstOrNull { it.speak } ?: catalog.cells.firstOrNull()
        return cell?.let { format(it, speakType, speakSite, speakSector) }.orEmpty()
    }

    private fun speakType(type: String): String? {
        val trimmed = type.trim()
        if (trimmed.isEmpty()) return null
        return when (trimmed.lowercase().replace(" ", "").replace("_", "").replace("-", "")) {
            "das" -> "D A S"
            "smallcell" -> "small cell"
            "macro" -> "macro"
            else -> trimmed.replace('-', ' ')
        }
    }

    private fun speakSite(site: String): String? {
        val spoken = site.trim().replace('-', ' ')
        return spoken.takeIf { it.isNotBlank() }
    }

    private fun speakSector(sector: String): String? {
        val trimmed = sector.trim()
        if (trimmed.isEmpty()) return null
        val number = trimmed.removePrefix("S").removePrefix("s").toIntOrNull()
            ?: trimmed.toIntOrNull()
        return if (number != null) {
            "sector ${NumberWords.toWords(number)}"
        } else {
            "sector $trimmed"
        }
    }
}
