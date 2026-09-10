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

    /**
     * Speaks round multiples of 100 the way network/radio jargon informally reads them, e.g.
     * 800 -> "eight hundred", 1900 -> "nineteen hundred", 2600 -> "twenty six hundred" — instead of
     * [toWords]'s literal "two thousand six hundred". Values under 1000 fall back to [toWords]
     * (already "hundred"-style, e.g. 450 -> "four hundred fifty").
     */
    fun toHundredsWords(value: Int): String {
        if (value < 0) return "minus ${toHundredsWords(-value)}"
        if (value < 1_000) return toWords(value)
        val hundreds = value / 100
        val remainder = value % 100
        val hundredsWords = "${toWords(hundreds)} hundred"
        return if (remainder != 0) "$hundredsWords ${toWords(remainder)}" else hundredsWords
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
