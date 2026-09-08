package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VeryStrongSignalTest {

    @Test
    fun veryStrongRsrpUsesConfigurableThreshold() {
        val settings = PassiveSignalSettings(veryStrongRsrpMinDbm = -80)
        assertTrue(settings.isVeryStrongRsrp(-79))
        assertFalse(settings.isVeryStrongRsrp(-80))
        assertFalse(settings.isVeryStrongRsrp(-81))
    }

    @Test
    fun veryStrongPulseFrequencyIsTwentyFivePercentHigher() {
        val volumes = AudioVolumeSettings(signalPulseFrequencyHz = 600)
        assertEquals(750, volumes.veryStrongPulseFrequencyHz())

        val low = AudioVolumeSettings(signalPulseFrequencyHz = 400)
        assertEquals(500, low.veryStrongPulseFrequencyHz())
    }

    @Test
    fun normalizedKeepsVeryStrongAboveMild() {
        val settings = PassiveSignalSettings(
            mildRsrpMinDbm = -95,
            veryStrongRsrpMinDbm = -96
        ).normalized()
        assertTrue(settings.veryStrongRsrpMinDbm > settings.mildRsrpMinDbm)
    }
}
