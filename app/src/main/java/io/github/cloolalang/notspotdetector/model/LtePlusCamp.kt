package io.github.cloolalang.notspotdetector.model

/**
 * Keeps the technology tape on 4G+ while the phone stays on the LTE cell where the
 * status-bar plus icon was seen. Android republishes that icon's override whenever a
 * secondary carrier is added or dropped, including on a phone that is not moving, so the
 * raw flag chatters between 4G and 4G+ while the icon itself stays on 4G+.
 */
class LtePlusCamp {
    private var earfcn: Int? = null
    private var pci: Int? = null

    @Synchronized
    fun showPlus(radioIs4g: Boolean, rawPlus: Boolean, earfcn: Int?, pci: Int?): Boolean {
        if (!radioIs4g) return false
        if (rawPlus && earfcn != null && pci != null) {
            this.earfcn = earfcn
            this.pci = pci
            return true
        }
        if (earfcn != null && pci != null && earfcn == this.earfcn && pci == this.pci) {
            return true
        }
        if (earfcn != null && pci != null) {
            this.earfcn = null
            this.pci = null
            return rawPlus
        }
        return rawPlus || this.earfcn != null
    }
}
