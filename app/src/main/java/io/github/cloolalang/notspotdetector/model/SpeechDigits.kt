package io.github.cloolalang.notspotdetector.model

/** Formats integers so TTS reads each digit separately (6400 → "6 4 0 0"). */
object SpeechDigits {
    fun format(value: Int): String {
        return value.toString().toCharArray().joinToString(" ")
    }
}
