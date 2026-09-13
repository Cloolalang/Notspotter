package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RsrpBandEdgesTest {

    private val settings = PassiveSignalSettings().normalized()

    @Test
    fun normalizedLocksFixedBandsAndAdjustableEnds() {
        val custom = PassiveSignalSettings(
            veryStrongRsrpMinDbm = -60,
            mildRsrpMinDbm = -80,
            goodRsrpMinDbm = -90,
            fairRsrpMinDbm = -100,
            poorRsrpMinDbm = -127,
            noSignalRsrpDbm = -120
        ).normalized()

        assertEquals(-60, custom.veryStrongRsrpMinDbm)
        assertEquals(-95, custom.mildRsrpMinDbm)
        assertEquals(-105, custom.goodRsrpMinDbm)
        assertEquals(-115, custom.fairRsrpMinDbm)
        assertEquals(-127, custom.poorRsrpMinDbm)
        assertEquals(-128, custom.noSignalRsrpDbm)
    }

    @Test
    fun veryStrongSliderClampsToMinus75ThroughMinus50() {
        assertEquals(
            -75,
            PassiveSignalSettings(veryStrongRsrpMinDbm = -90).normalized().veryStrongRsrpMinDbm
        )
        assertEquals(
            -50,
            PassiveSignalSettings(veryStrongRsrpMinDbm = -40).normalized().veryStrongRsrpMinDbm
        )
    }

    @Test
    fun rxss10SliderClampsToMinus135ThroughMinus125AndStaysBelowRxss6() {
        assertEquals(
            -135,
            PassiveSignalSettings(noSignalRsrpDbm = -140).normalized().noSignalRsrpDbm
        )
        assertEquals(
            -126,
            PassiveSignalSettings(
                noSignalRsrpDbm = -120,
                poorRsrpMinDbm = -125
            ).normalized().noSignalRsrpDbm
        )
        assertEquals(
            -129,
            PassiveSignalSettings(
                noSignalRsrpDbm = -125,
                poorRsrpMinDbm = -128
            ).normalized().noSignalRsrpDbm
        )
    }

    @Test
    fun rxss6HighEndClampsBetweenRxss10AndMinus125() {
        assertEquals(
            -134,
            PassiveSignalSettings(poorRsrpMinDbm = -140).normalized().poorRsrpMinDbm
        )
        assertEquals(
            -125,
            PassiveSignalSettings(poorRsrpMinDbm = -120).normalized().poorRsrpMinDbm
        )
        val raisedFloor = PassiveSignalSettings(
            poorRsrpMinDbm = -130,
            noSignalRsrpDbm = -128
        ).normalized()
        assertEquals(-127, raisedFloor.poorRsrpMinDbm)
        assertEquals(-128, raisedFloor.noSignalRsrpDbm)

        val lowered = PassiveSignalSettings(
            poorRsrpMinDbm = -130,
            noSignalRsrpDbm = -135
        ).normalized()
        assertEquals(-130, lowered.poorRsrpMinDbm)
        assertEquals(-135, lowered.noSignalRsrpDbm)
        assertEquals(SignalStrengthTier.POOR, lowered.resolveSignalStrengthTier(-129))
        assertEquals(SignalStrengthTier.CRITICAL, lowered.resolveSignalStrengthTier(-130))
        assertEquals(SignalStrengthTier.CRITICAL, lowered.resolveSignalStrengthTier(-134))
        assertNull(lowered.resolveSignalStrengthTier(-135))
    }

    @Test
    fun defaultBandsDoNotOverlap() {
        assertEquals(SignalStrengthTier.MILD, settings.resolveSignalStrengthTier(-85))
        assertEquals(SignalStrengthTier.GOOD, settings.resolveSignalStrengthTier(-95))
        assertEquals(SignalStrengthTier.FAIR, settings.resolveSignalStrengthTier(-105))
        assertEquals(SignalStrengthTier.POOR, settings.resolveSignalStrengthTier(-115))
        assertEquals(SignalStrengthTier.CRITICAL, settings.resolveSignalStrengthTier(-125))
        assertNull(settings.resolveSignalStrengthTier(-135))
        assertTrue(settings.isRsrpTooWeakForService(-135))
        assertFalse(settings.isRsrpTooWeakForService(-134))
        assertTrue(settings.isVeryStrongRsrp(-74))
        assertFalse(settings.isVeryStrongRsrp(-75))
    }

    @Test
    fun g2BandsClampAndStayAdjacent() {
        val custom = PassiveSignalSettings(
            g2WeakMaxDbm = -90,
            g2NoSignalRsrpDbm = -90
        ).normalized()
        assertEquals(-90, custom.g2WeakMaxDbm)
        assertEquals(-95, custom.g2NoSignalRsrpDbm)

        assertEquals(
            -134,
            PassiveSignalSettings(g2WeakMaxDbm = -140).normalized().g2WeakMaxDbm
        )
        assertEquals(
            -85,
            PassiveSignalSettings(g2WeakMaxDbm = -70).normalized().g2WeakMaxDbm
        )
        val raisedFloor = PassiveSignalSettings(
            g2WeakMaxDbm = -100,
            g2NoSignalRsrpDbm = -95
        ).normalized()
        assertEquals(-94, raisedFloor.g2WeakMaxDbm)
        assertEquals(-95, raisedFloor.g2NoSignalRsrpDbm)
        assertEquals(
            -135,
            PassiveSignalSettings(g2NoSignalRsrpDbm = -140).normalized().g2NoSignalRsrpDbm
        )

        assertEquals(SignalStrengthTier.G2_STRONG, settings.resolveG2SignalStrengthTier(-84))
        assertEquals(SignalStrengthTier.G2_WEAK, settings.resolveG2SignalStrengthTier(-85))
        assertEquals(SignalStrengthTier.G2_WEAK, settings.resolveG2SignalStrengthTier(-134))
        assertNull(settings.resolveG2SignalStrengthTier(-135))
    }
}
