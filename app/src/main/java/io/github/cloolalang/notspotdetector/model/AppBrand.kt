package io.github.cloolalang.notspotdetector.model

/** Name shown in the top title bar. */
enum class AppBrand {
    NOTSPOTTER,
    CELLNEX;

    companion object {
        fun fromStoredName(name: String?): AppBrand {
            return entries.firstOrNull { it.name == name } ?: NOTSPOTTER
        }
    }
}
