package io.github.cloolalang.notspotdetector.model

object SpecialCellCsv {

    const val SCHEMA_VERSION = 1
    const val EXAMPLE_ASSET_NAME = "special-cells.example.csv"
    const val EXAMPLE_SOURCE_LABEL = "Example list"

    fun parse(text: String, sourceLabel: String = "", isExample: Boolean = false): SpecialCellCatalog {
        val cells = mutableListOf<SpecialCell>()
        val warnings = mutableListOf<String>()
        var header: Map<String, Int>? = null

        text.lineSequence().forEachIndexed { index, rawLine ->
            val lineNumber = index + 1
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed

            val columns = splitCsvLine(line)
            if (header == null) {
                header = columns.mapIndexed { columnIndex, name ->
                    normalizeHeader(name) to columnIndex
                }.toMap()
                if (!REQUIRED_HEADERS.all { header!!.containsKey(it) }) {
                    warnings.add("Line $lineNumber: header must include site, type, mno, rat, sector, channel, pci")
                    return SpecialCellCatalog(
                        cells = emptyList(),
                        warnings = warnings,
                        sourceLabel = sourceLabel,
                        isExample = isExample
                    )
                }
                return@forEachIndexed
            }

            val row = header!!
            fun cell(name: String): String = row[name]?.let { columns.getOrNull(it).orEmpty().trim() }.orEmpty()

            val site = cell("site")
            val type = cell("type")
            val mno = cell("mno")
            val ratRaw = cell("rat")
            val sector = cell("sector")
            val channelRaw = cell("channel")
            val pciRaw = cell("pci")
            if (listOf(site, type, mno, ratRaw, sector, channelRaw, pciRaw).any { it.isBlank() }) {
                warnings.add("Line $lineNumber: missing required field")
                return@forEachIndexed
            }
            val rat = SpecialCellRat.fromCsv(ratRaw)
            val channel = channelRaw.toIntOrNull()
            val pci = pciRaw.toIntOrNull()
            if (rat == null || channel == null || pci == null) {
                warnings.add("Line $lineNumber: invalid rat, channel, or pci")
                return@forEachIndexed
            }

            cells.add(
                SpecialCell(
                    site = site,
                    type = type,
                    mno = mno,
                    rat = rat,
                    sector = sector,
                    channel = channel,
                    pci = pci,
                    plmn = cell("plmn").ifBlank { null },
                    speak = parseSpeak(cell("speak")),
                    speakAs = cell("speak_as").ifBlank { null },
                    notes = cell("notes").ifBlank { null },
                    sourceLine = lineNumber
                )
            )
        }

        if (header == null) {
            warnings.add("No header row found")
        }

        val seenKeys = mutableSetOf<String>()
        cells.forEach { cell ->
            val key = "${cell.rat.name}:${cell.channel}:${cell.pci}:${cell.plmn.orEmpty()}"
            if (!seenKeys.add(key)) {
                warnings.add("Line ${cell.sourceLine}: duplicate match key ${cell.rat.displayLabel} ${cell.channel}/${cell.pci}")
            }
        }

        return SpecialCellCatalog(
            cells = cells,
            warnings = warnings,
            sourceLabel = sourceLabel,
            isExample = isExample
        )
    }

    private fun parseSpeak(raw: String): Boolean {
        return when (raw.lowercase()) {
            "", "yes", "true", "1", "y" -> true
            "no", "false", "0", "n" -> false
            else -> true
        }
    }

    private fun normalizeHeader(name: String): String {
        return name.trim().lowercase().replace(" ", "_").replace("-", "_")
    }

    fun splitCsvLine(line: String): List<String> {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' -> {
                    if (inQuotes && index + 1 < line.length && line[index + 1] == '"') {
                        current.append('"')
                        index++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    values.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
            index++
        }
        values.add(current.toString())
        return values
    }

    private val REQUIRED_HEADERS = setOf(
        "site",
        "type",
        "mno",
        "rat",
        "sector",
        "channel",
        "pci"
    )
}
