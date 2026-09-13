package io.github.cloolalang.notspotdetector.model

fun formatMobileDataEnabledSuffix(enabled: Boolean?): String? {
    return when (enabled) {
        true -> "Data on"
        false -> "Data off"
        null -> null
    }
}
