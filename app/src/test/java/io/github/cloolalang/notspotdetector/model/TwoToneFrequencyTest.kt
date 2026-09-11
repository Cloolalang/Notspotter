package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TwoToneFrequencyTest {

    @Test
    fun twoToneHighFrequencyHz_isSpreadPercentAboveLowerTone() {
        assertEquals(690, AudioVolumeSettings.twoToneHighFrequencyHz(600, 15))
        assertEquals(630, AudioVolumeSettings.twoToneHighFrequencyHz(600, 5))
        assertEquals(780, AudioVolumeSettings.twoToneHighFrequencyHz(600, 30))
    }

    @Test
    fun twoToneHighFrequencyHz_clampsToPulseFrequencyCeiling() {
        assertEquals(
            AudioVolumeSettings.MAX_SIGNAL_PULSE_FREQUENCY_HZ,
            AudioVolumeSettings.twoToneHighFrequencyHz(5_000, 30)
        )
    }

    @Test
    fun limitedServiceTwoToneHighFrequencyHz_usesStoredSpread() {
        val volumes = AudioVolumeSettings(
            limitedServiceTierPulseFrequencyHz = 800,
            limitedServiceTwoToneSpreadPercent = 20
        ).normalized()
        assertEquals(960, volumes.limitedServiceTwoToneHighFrequencyHz())
    }
}
