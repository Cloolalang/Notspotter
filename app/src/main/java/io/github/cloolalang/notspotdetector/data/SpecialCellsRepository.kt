package io.github.cloolalang.notspotdetector.data

import android.content.Context
import android.net.Uri
import io.github.cloolalang.notspotdetector.model.SpecialCellCatalog
import io.github.cloolalang.notspotdetector.model.SpecialCellCsv
import io.github.cloolalang.notspotdetector.model.SpecialCellsImportResult
import java.io.File
import java.io.IOException

class SpecialCellsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val storedFile = File(appContext.filesDir, STORED_FILE_NAME)
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): SpecialCellCatalog {
        if (!storedFile.exists()) {
            return restoreExample()
        }
        return parseStoredFile()
    }

    fun restoreExample(): SpecialCellCatalog {
        val text = readExampleAsset()
        storedFile.writeText(text)
        prefs.edit()
            .putBoolean(KEY_IS_EXAMPLE, true)
            .putString(KEY_SOURCE_LABEL, SpecialCellCsv.EXAMPLE_SOURCE_LABEL)
            .apply()
        return SpecialCellCsv.parse(
            text = text,
            sourceLabel = SpecialCellCsv.EXAMPLE_SOURCE_LABEL,
            isExample = true
        )
    }

    fun importFromUri(uri: Uri): SpecialCellsImportResult {
        return try {
            val text = appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return SpecialCellsImportResult.InvalidFile
            val label = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null }
                ?: "Imported list"
            val parsed = SpecialCellCsv.parse(text, sourceLabel = label, isExample = false)
            if (parsed.cells.isEmpty()) {
                return if (parsed.warnings.isNotEmpty()) {
                    SpecialCellsImportResult.InvalidFile
                } else {
                    SpecialCellsImportResult.EmptyFile
                }
            }
            storedFile.writeText(text)
            prefs.edit()
                .putBoolean(KEY_IS_EXAMPLE, false)
                .putString(KEY_SOURCE_LABEL, label)
                .apply()
            SpecialCellsImportResult.Imported
        } catch (_: IOException) {
            SpecialCellsImportResult.Failed
        } catch (_: SecurityException) {
            SpecialCellsImportResult.Failed
        }
    }

    private fun parseStoredFile(): SpecialCellCatalog {
        val text = storedFile.readText()
        val isExample = prefs.getBoolean(KEY_IS_EXAMPLE, false)
        val label = prefs.getString(KEY_SOURCE_LABEL, null)
            ?: if (isExample) SpecialCellCsv.EXAMPLE_SOURCE_LABEL else storedFile.name
        return SpecialCellCsv.parse(text, sourceLabel = label, isExample = isExample)
    }

    private fun readExampleAsset(): String {
        return appContext.assets.open(SpecialCellCsv.EXAMPLE_ASSET_NAME).bufferedReader().use { it.readText() }
    }

    companion object {
        const val CSV_MIME_TYPE = "text/csv"
        private const val STORED_FILE_NAME = "special-cells.csv"
        private const val PREFS_NAME = "notspot_special_cells"
        private const val KEY_IS_EXAMPLE = "is_example"
        private const val KEY_SOURCE_LABEL = "source_label"
    }
}
