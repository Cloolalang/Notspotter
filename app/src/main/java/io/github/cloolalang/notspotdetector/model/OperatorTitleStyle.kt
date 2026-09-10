package io.github.cloolalang.notspotdetector.model

enum class UkOperatorBrand {
    EE,
    VODAFONE,
    VMO2,
    OTHER
}

object OperatorTitleStyle {

    fun detectBrand(operatorName: String?): UkOperatorBrand {
        val normalized = normalizeOperatorName(operatorName) ?: return UkOperatorBrand.OTHER
        return detectBrandFromNormalized(normalized) ?: UkOperatorBrand.OTHER
    }

    /** Home SIM operator only — never camped / visited serving name (title bar). */
    fun resolveHomeOperatorName(stats: ConnectivityStats): String? {
        return stats.homeNetworkOperatorName?.trim()?.takeIf { it.isNotBlank() }
    }

    fun detectBrand(stats: ConnectivityStats): UkOperatorBrand {
        return detectBrand(resolveHomeOperatorName(stats))
    }

    fun resolveLabel(stats: ConnectivityStats): String? {
        val raw = resolveHomeOperatorName(stats) ?: return null
        val brand = detectBrand(raw)
        return shortLabel(brand, raw)
    }

    fun shortLabel(brand: UkOperatorBrand, operatorName: String?): String? {
        return when (brand) {
            UkOperatorBrand.EE -> "EE"
            UkOperatorBrand.VODAFONE -> "Vodafone"
            UkOperatorBrand.VMO2 -> "VMO2"
            UkOperatorBrand.OTHER -> operatorName?.trim()?.takeIf { it.isNotBlank() }
        }
    }

    private fun normalizeOperatorName(operatorName: String?): String? {
        return operatorName
            ?.trim()
            ?.lowercase()
            ?.replace("-", " ")
            ?.takeIf { it.isNotBlank() }
    }

    private fun detectBrandFromNormalized(normalized: String): UkOperatorBrand? {
        return when {
            normalized.contains("vodafone") -> UkOperatorBrand.VODAFONE
            normalized.contains("vmo2") ||
                normalized.contains("virgin") ||
                normalized == "o2" ||
                normalized == "o2 uk" ||
                normalized.startsWith("o2 ") -> UkOperatorBrand.VMO2
            normalized == "ee" ||
                normalized == "ee uk" ||
                normalized.startsWith("ee ") -> UkOperatorBrand.EE
            else -> null
        }
    }
}

fun SignalMeasurementTier.isLowOrNoSignalForTitle(): Boolean {
    return when (this) {
        SignalMeasurementTier.POOR,
        SignalMeasurementTier.CRITICAL,
        SignalMeasurementTier.G2_STRONG,
        SignalMeasurementTier.G2_WEAK,
        SignalMeasurementTier.G2_NO_SIGNAL,
        SignalMeasurementTier.DEADZONE,
        SignalMeasurementTier.NO_SIGNAL,
        SignalMeasurementTier.SEARCHING_2G,
        SignalMeasurementTier.WIFI_CALLING -> true
        else -> false
    }
}

fun ConnectivityStats.isLowOrNoSignalForTitle(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    return resolveSignalMeasurementTier(settings).isLowOrNoSignalForTitle() ||
        isRsrqPoor(settings)
}
