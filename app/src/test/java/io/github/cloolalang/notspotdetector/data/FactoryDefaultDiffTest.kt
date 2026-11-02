package io.github.cloolalang.notspotdetector.data

import io.github.cloolalang.notspotdetector.model.AppSettingsSnapshot
import io.github.cloolalang.notspotdetector.model.SettingsProfile
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class FactoryDefaultDiffTest {
    @Test
    fun profileVersusFactoryDefaults() {
        val json = listOf(
            File("src/test/resources/factory-defaults-profile.json"),
            File("app/src/test/resources/factory-defaults-profile.json")
        ).first { it.exists() }.readText()
        val imported = AppSettingsSnapshotCodec.decodeProfile(json)!!.settings.normalized()
        val defaults = AppSettingsSnapshot.defaults()
        val importedJson = settingsJson(imported)
        val defaultJson = settingsJson(defaults)
        val diffs = mutableListOf<String>()
        walk("", importedJson, defaultJson, diffs)
        if (diffs.isNotEmpty()) {
            fail(diffs.joinToString("\n"))
        }
    }

    private fun settingsJson(settings: AppSettingsSnapshot): JSONObject {
        val encoded = AppSettingsSnapshotCodec.encodeProfile(
            SettingsProfile(id = "x", name = "x", savedAtMs = 0L, settings = settings)
        )
        return JSONObject(encoded).getJSONObject("profile").getJSONObject("settings")
    }

    private fun walk(path: String, imported: Any?, defaults: Any?, diffs: MutableList<String>) {
        when {
            imported is JSONObject && defaults is JSONObject -> {
                val keys = (imported.keys().asSequence() + defaults.keys().asSequence()).distinct()
                for (key in keys) {
                    val next = if (path.isEmpty()) key else "$path.$key"
                    walk(next, imported.opt(key), defaults.opt(key), diffs)
                }
            }
            imported is JSONArray && defaults is JSONArray -> {
                val count = maxOf(imported.length(), defaults.length())
                for (index in 0 until count) {
                    walk("$path[$index]", imported.opt(index), defaults.opt(index), diffs)
                }
            }
            else -> {
                if (!sameValue(imported, defaults)) {
                    diffs += "$path: profile=$imported factory=$defaults"
                }
            }
        }
    }

    private fun sameValue(imported: Any?, defaults: Any?): Boolean {
        if (imported?.toString() == defaults?.toString()) return true
        val importedNumber = (imported as? Number)?.toDouble() ?: return false
        val defaultNumber = (defaults as? Number)?.toDouble() ?: return false
        return kotlin.math.abs(importedNumber - defaultNumber) < 0.0001
    }
}
