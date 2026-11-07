package io.github.cloolalang.notspotdetector.multiue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PanelFoldTest {

    @Test
    fun replaceLeavesTheMultiUePanelOnThisPhone() {
        PanelFold.set("multi-ue", false)
        PanelFold.set("histogram", true)
        PanelFold.replace(mapOf("histogram" to false, "multi-ue" to true))
        assertEquals(false, PanelFold.snapshot()["multi-ue"])
        assertEquals(false, PanelFold.snapshot()["histogram"])
        PanelFold.replace(emptyMap())
        assertEquals(false, PanelFold.snapshot()["multi-ue"])
        assertFalse(PanelFold.snapshot().containsKey("histogram"))
    }
}
