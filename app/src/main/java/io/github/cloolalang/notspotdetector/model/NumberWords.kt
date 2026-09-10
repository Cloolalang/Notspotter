package io.github.cloolalang.notspotdetector.model

/**
 * Converts integers to natural spoken English words (e.g. 20 -> "twenty", 800 -> "eight hundred"),
 * for TTS phrases that read better as whole numbers than digit-by-digit (see [SpeechDigits] for the
 * digit-by-digit convention used elsewhere, e.g. channel/PCI/BSIC).
 */
object NumberWords {

    private val ONES = arrayOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
        "eighteen", "nineteen"
    )

    private val TENS = arrayOf(
        "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"
    )

    fun toWords(value: Int): String {
        if (value < 0) return "minus ${toWords(-value)}"
        if (value == 0) return ONES[0]
        return buildWords(value).trim()
    }

    private fun buildWords(n: Int): String {
        return when {
            n < 20 -> ONES[n]
            n < 100 -> TENS[n / 10] + if (n % 10 != 0) " ${ONES[n % 10]}" else ""
            n < 1_000 -> "${ONES[n / 100]} hundred" + if (n % 100 != 0) " ${buildWords(n % 100)}" else ""
            else -> "${buildWords(n / 1_000)} thousand" + if (n % 1_000 != 0) " ${buildWords(n % 1_000)}" else ""
        }
    }
}
