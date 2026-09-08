package io.github.cloolalang.notspotdetector.model

object NetworkOperatorSpeech {

    /**
     * Formats operator names so TTS reads short acronyms clearly (e.g. "EE" → "E E").
     */
    fun formatForSpeech(networkOperatorName: String?): String? {
        val trimmed = networkOperatorName?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (trimmed.length <= 4 &&
            trimmed.all { it.isLetter() || it.isDigit() } &&
            trimmed.any { it.isLetter() } &&
            trimmed.filter { it.isLetter() }.all { it.isUpperCase() }
        ) {
            return trimmed.toCharArray().joinToString(" ")
        }
        return trimmed
    }
}
