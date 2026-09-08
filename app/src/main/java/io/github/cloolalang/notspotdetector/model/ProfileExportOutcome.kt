package io.github.cloolalang.notspotdetector.model

enum class ProfileExportResult {
    Exported,
    ProfileNotFound,
    Failed
}

data class ProfileExportOutcome(
    val result: ProfileExportResult,
    val relativePath: String? = null
)
