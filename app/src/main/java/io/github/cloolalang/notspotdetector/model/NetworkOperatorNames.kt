package io.github.cloolalang.notspotdetector.model

/**
 * Separates the home MNO name from an MVNO / virtual-network brand when Android exposes both.
 */
object NetworkOperatorNames {

    fun normalize(raw: String?): String? {
        return raw?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    }

    fun namesMatch(left: String?, right: String?): Boolean {
        val a = normalize(left)?.lowercase() ?: return false
        val b = normalize(right)?.lowercase() ?: return false
        return a == b
    }

    fun isGenericSimDisplayName(name: String): Boolean {
        return GENERIC_SIM_DISPLAY.matches(name.trim())
    }

    /**
     * Underlying host MNO. Prefers the parent carrier-ID name, then the camped network name when
     * still on the home PLMN (SIM SPN is often the MVNO brand).
     */
    fun resolveHomeMnoName(
        simOperatorName: String? = null,
        simCarrierIdName: String? = null,
        servingOperatorName: String? = null,
        homePlmn: String? = null,
        servingPlmn: String? = null
    ): String? {
        normalize(simCarrierIdName)?.let { return it }
        val serving = normalize(servingOperatorName)
        val spn = normalize(simOperatorName)
        val samePlmn = homePlmn.isNullOrBlank() ||
            servingPlmn.isNullOrBlank() ||
            homePlmn == servingPlmn
        if (samePlmn && serving != null && (spn == null || !namesMatch(serving, spn))) {
            return serving
        }
        return spn ?: serving
    }

    /**
     * MVNO / virtual-network brand. Null unless a name is distinct from the home MNO.
     * When parent and specific carrier IDs are present and equal, the SIM is a host MNO
     * (including a user-renamed SIM in Settings).
     */
    fun resolveVirtualOperatorName(
        homeMnoName: String?,
        simOperatorName: String? = null,
        simSpecificCarrierIdName: String? = null,
        subscriptionCarrierName: String? = null,
        subscriptionDisplayName: String? = null,
        simCarrierId: Int? = null,
        simSpecificCarrierId: Int? = null
    ): String? {
        val home = normalize(homeMnoName)
        val branded = listOfNotNull(
            normalize(simSpecificCarrierIdName),
            normalize(simOperatorName),
            normalize(subscriptionCarrierName),
            normalize(subscriptionDisplayName)?.takeUnless { isGenericSimDisplayName(it) }
        ).distinctBy { it.lowercase() }
            .filter { home == null || !namesMatch(it, home) }

        if (branded.isEmpty()) return null

        val idsKnown = simCarrierId != null && simSpecificCarrierId != null &&
            simCarrierId > 0 && simSpecificCarrierId > 0
        if (idsKnown && simCarrierId == simSpecificCarrierId) {
            return null
        }
        return branded.first()
    }

    private val GENERIC_SIM_DISPLAY = Regex(
        """(?i)^(sim|card|slot|e-?sim)\s*\d*$"""
    )
}
