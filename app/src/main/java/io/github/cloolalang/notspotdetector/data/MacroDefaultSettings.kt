package io.github.cloolalang.notspotdetector.data

import android.content.Context
import io.github.cloolalang.notspotdetector.model.AppSettingsSnapshot

/** Bundled outdoor-macro settings profile applied from Alert settings. */
object MacroDefaultSettings {
    const val ASSET_NAME = "macro-defaults.json"

    fun load(context: Context): AppSettingsSnapshot? {
        val json = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        return AppSettingsSnapshotCodec.decodeProfile(json)?.settings
    }
}
