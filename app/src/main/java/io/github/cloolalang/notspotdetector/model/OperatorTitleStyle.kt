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

    fun detectBrand(stats: ConnectivityStats): UkOperatorBrand {
        val candidates = listOfNotNull(
            stats.homeNetworkOperatorName,
            stats.networkOperatorName,
            stats.servingNetworkOperatorName
        )
        for (candidate in candidates) {
            val brand = detectBrand(candidate)
            if (brand != UkOperatorBrand.OTHER) {
                return brand
            }
        }
        return UkOperatorBrand.OTHER
    }

    fun resolveLabel(stats: ConnectivityStats): String? {
        val raw = stats.homeNetworkOperatorName
            ?: stats.networkOperatorName
            ?: stats.servingNetworkOperatorName
        val brand = detectBrand(stats)
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

const val LOW_SIGNAL_TITLE_TIER_MIN = 5

fun SignalMeasurementTier.isLowOrNoSignalForTitle(): Boolean {
    val tierNumber = displayNumber
    if (tierNumber != null && tierNumber >= LOW_SIGNAL_TITLE_TIER_MIN) {
        return true
    }
    return this == SignalMeasurementTier.NO_SIGNAL
}

fun ConnectivityStats.isLowOrNoSignalForTitle(
    settings: PassiveSignalSettings = PassiveSignalSettings()
): Boolean {
    return resolveSignalMeasurementTier(settings).isLowOrNoSignalForTitle()
}
