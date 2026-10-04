package io.github.cloolalang.notspotdetector.model

/**
 * LTE ECGI as decimal digits only: camped PLMN plus the 28-bit ECI zero-padded to 9 digits.
 * Example: PLMN `23415` and ECI `0` → `23415000000000`. ECI `1234567` → `23415001234567`.
 */
object LteEcgi {

    data class Parsed(
        val plmn: String,
        val eci: Int,
        val canonical: String
    )

    fun format(plmn: String?, eci: Int?): String? {
        if (eci == null || eci !in 0..LTE_ECI_MAX) return null
        val digits = plmnDigits(plmn) ?: return null
        return digits + eci.toString().padStart(ECI_DECIMAL_WIDTH, '0')
    }

    fun parse(raw: String): Parsed? {
        val compact = raw.trim()
        if (compact.length != PLMN5_ECGI_LENGTH && compact.length != PLMN6_ECGI_LENGTH) return null
        if (!compact.all { it.isDigit() }) return null
        val eciDecimal = compact.takeLast(ECI_DECIMAL_WIDTH)
        val plmn = compact.dropLast(ECI_DECIMAL_WIDTH)
        if (plmn.length !in PLMN_MIN_LENGTH..PLMN_MAX_LENGTH) return null
        val eci = eciDecimal.toIntOrNull() ?: return null
        if (eci !in 0..LTE_ECI_MAX) return null
        val canonical = plmn + eci.toString().padStart(ECI_DECIMAL_WIDTH, '0')
        return Parsed(plmn = plmn, eci = eci, canonical = canonical)
    }

    private fun plmnDigits(raw: String?): String? {
        val digits = raw?.filter { it.isDigit() }.orEmpty()
        return digits.takeIf { it.length in PLMN_MIN_LENGTH..PLMN_MAX_LENGTH }
    }

    /** Decimal width of the maximum 28-bit ECI (`268435455`). */
    private const val ECI_DECIMAL_WIDTH = 9
    private const val PLMN_MIN_LENGTH = 5
    private const val PLMN_MAX_LENGTH = 6
    private const val PLMN5_ECGI_LENGTH = PLMN_MIN_LENGTH + ECI_DECIMAL_WIDTH
    private const val PLMN6_ECGI_LENGTH = PLMN_MAX_LENGTH + ECI_DECIMAL_WIDTH

    /** LTE 28-bit ECI maximum (`CellIdentityLte.getCi()`). */
    private const val LTE_ECI_MAX = 0x0FFFFFFF
}
