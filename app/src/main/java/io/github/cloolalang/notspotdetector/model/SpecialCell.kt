package io.github.cloolalang.notspotdetector.model

enum class SpecialCellRat {
    G2,
    G4,
    G5;

    val displayLabel: String
        get() = when (this) {
            G2 -> "2G"
            G4 -> "4G"
            G5 -> "5G"
        }

    companion object {
        fun fromCsv(raw: String): SpecialCellRat? {
            val normalized = raw.trim().uppercase()
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "")
            return when (normalized) {
                "2G", "GSM", "GERAN" -> G2
                "4G", "LTE", "EUTRA" -> G4
                "5G", "NR", "NRSA", "5GENDC", "ENDC", "5GNSA" -> G5
                else -> null
            }
        }
    }
}

enum class SpecialCellLayer {
    LTE,
    NR,
    GSM
}

data class SpecialCell(
    val site: String,
    val type: String,
    val mno: String,
    val rat: SpecialCellRat,
    val sector: String,
    val channel: Int,
    val pci: Int,
    val plmn: String? = null,
    val speak: Boolean = true,
    val speakAs: String? = null,
    val notes: String? = null,
    val sourceLine: Int = 0
) {
    val matchKey: String
        get() = "${rat.name}:$channel:$pci:${plmn.orEmpty()}"

    val displaySite: String
        get() = speakAs?.takeIf { it.isNotBlank() } ?: site
}

data class SpecialCellMatch(
    val cell: SpecialCell,
    val layer: SpecialCellLayer
)

data class SpecialCellCatalog(
    val cells: List<SpecialCell> = emptyList(),
    val warnings: List<String> = emptyList(),
    val sourceLabel: String = "",
    val isExample: Boolean = false
) {
    val size: Int get() = cells.size
    val isEmpty: Boolean get() = cells.isEmpty()
}

enum class SpecialCellsImportResult {
    Imported,
    InvalidFile,
    EmptyFile,
    Failed
}
