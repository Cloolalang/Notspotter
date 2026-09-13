package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test


class SelectedApnTest {

    @Test
    fun format_joinsNameAndApnWhenDifferent() {
        assertEquals("EE Internet · eeinternet", formatSelectedApnDisplay("EE Internet", "eeinternet"))
    }

    @Test
    fun format_singleValueWhenNameMatchesApn() {
        assertEquals("everywhere", formatSelectedApnDisplay("everywhere", "everywhere"))
    }

    @Test
    fun format_apnOnly() {
        assertEquals("everywhere", formatSelectedApnDisplay(null, "everywhere"))
    }

    @Test
    fun format_empty_isNull() {
        assertNull(formatSelectedApnDisplay("  ", ""))
    }

    @Test
    fun sanitize_keepsDataApn() {
        assertEquals("everywhere", sanitizeApnExtra("everywhere"))
        assertEquals("eeinternet", sanitizeApnExtra("\"eeinternet\""))
    }

    @Test
    fun sanitize_dropsImsAndEmpty() {
        assertNull(sanitizeApnExtra("ims"))
        assertNull(sanitizeApnExtra("null"))
        assertNull(sanitizeApnExtra("  "))
    }

    @Test
    fun dataState_keepsDefaultApn() {
        assertEquals(
            "everywhere",
            apnFromDataConnectionState(
                apn = "everywhere",
                apnType = "default,supl",
                subscriptionId = 2,
                wantedSubscriptionId = 2
            )
        )
    }

    @Test
    fun dataState_dropsImsAndOtherSim() {
        assertNull(apnFromDataConnectionState("ims", "ims"))
        assertNull(
            apnFromDataConnectionState(
                apn = "everywhere",
                apnType = "default",
                subscriptionId = 1,
                wantedSubscriptionId = 2
            )
        )
    }

    @Test
    fun pickPreferred_prefersCurrentDefault() {
        val chosen = pickPreferredApnDisplay(
            listOf(
                ApnCandidate(name = "IMS", apn = "ims", type = "ims", current = true),
                ApnCandidate(name = "Vodafone UK", apn = "everywhere", type = "default", current = true),
                ApnCandidate(name = "MMS", apn = "mms", type = "mms")
            )
        )
        assertEquals("Vodafone UK · everywhere", chosen)
    }
}
