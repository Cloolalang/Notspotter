package io.github.cloolalang.notspotdetector.model

/**
 * One radio-debug sample for drive testing. Captures the cell the app chose, the
 * subscription ServiceState keys, and a compact dump of every LTE/NR/GSM row from
 * [android.telephony.TelephonyManager.getAllCellInfo].
 */
data class RadioDebugSnapshot(
    val chosenRat: String?,
    val chosenLteEarfcn: Int?,
    val chosenLtePci: Int?,
    val chosenRsrp: Int?,
    val chosenRsrq: Int?,
    val chosenPlmn: String?,
    val expectedPlmn: String?,
    val homePlmn: String?,
    val serviceStateLte: String,
    val signalLtePcis: String,
    val dualSim: Boolean,
    val simLabel: String?,
    val registeredLteCount: Int,
    val cells: String,
    val flags: List<String>
) {
    val isSuspect: Boolean get() = flags.isNotEmpty()

    fun bannerLine(): String {
        val chosen = formatEarfcnPci(chosenLteEarfcn, chosenLtePci)
        val flagText = if (flags.isEmpty()) "ok" else flags.joinToString(",")
        return "Radio debug: $chosen plmn=${chosenPlmn ?: "?"} " +
            "expect=${expectedPlmn ?: homePlmn ?: "?"} ss=$serviceStateLte [$flagText]"
    }

    fun bodyForDedupe(): String {
        return listOf(
            chosenRat,
            chosenLteEarfcn,
            chosenLtePci,
            chosenRsrp,
            chosenRsrq,
            chosenPlmn,
            expectedPlmn,
            homePlmn,
            serviceStateLte,
            signalLtePcis,
            dualSim,
            registeredLteCount,
            cells,
            flags
        ).joinToString("|")
    }

    fun logLine(timestamp: String): String {
        val flagText = if (flags.isEmpty()) "ok" else flags.joinToString(",")
        return "$timestamp chosen=${formatEarfcnPci(chosenLteEarfcn, chosenLtePci)}" +
            " rat=${chosenRat ?: "—"} plmn=${chosenPlmn ?: "?"} rsrp=${chosenRsrp ?: "—"}" +
            " rsrq=${chosenRsrq ?: "—"} expect=${expectedPlmn ?: "?"} home=${homePlmn ?: "?"}" +
            " ss=$serviceStateLte sigPci=$signalLtePcis dual=${if (dualSim) 1 else 0}" +
            " sim=${simLabel ?: "—"} flags=$flagText cells=$cells"
    }

    companion object {
        fun flags(
            chosenLteEarfcn: Int?,
            chosenPlmn: String?,
            expectedPlmn: String?,
            homePlmn: String?,
            serviceStateLteEarfcn: Int?,
            registeredLteCount: Int,
            registeredPlmns: Collection<String>,
            chosenLtePci: Int? = null,
            serviceStateLtePci: Int? = null
        ): List<String> {
            val flags = mutableListOf<String>()
            val expect = normalizePlmn(expectedPlmn) ?: normalizePlmn(homePlmn)
            val chosen = normalizePlmn(chosenPlmn)
            if (chosen != null && expect != null && chosen != expect) {
                flags += "plmn-mismatch"
            }
            if (chosenLteEarfcn != null && serviceStateLteEarfcn != null &&
                chosenLteEarfcn != serviceStateLteEarfcn
            ) {
                flags += "ss-earfcn-mismatch"
            }
            if (chosenLtePci != null && serviceStateLtePci != null &&
                chosenLtePci != serviceStateLtePci
            ) {
                flags += "ss-pci-mismatch"
            }
            if (registeredLteCount >= 2) {
                flags += "multi-registered"
            }
            val distinct = registeredPlmns.mapNotNull { normalizePlmn(it) }.toSet()
            if (distinct.size >= 2) {
                flags += "multi-plmn"
            }
            return flags
        }

        fun normalizePlmn(value: String?): String? {
            val digits = value?.filter { it.isDigit() }.orEmpty()
            return digits.takeIf { it.length >= 5 }
        }

        fun formatEarfcnPci(earfcn: Int?, pci: Int?): String {
            return "${earfcn ?: "—"}/${pci ?: "—"}"
        }
    }
}
