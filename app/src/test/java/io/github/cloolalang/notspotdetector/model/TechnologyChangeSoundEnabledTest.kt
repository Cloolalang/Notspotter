package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TechnologyChangeSoundEnabledTest {

    @Test
    fun alertVolumes_defaultSoundEnabled() {
        val volumes = AudioVolumeSettings()

        assertTrue(volumes.technologyChangeAlertVolumes(TechnologyChangeTarget.TO_2G).soundEnabled)
        assertTrue(volumes.technologyChangeAlertVolumes(TechnologyChangeTarget.TO_4G).soundEnabled)
        assertTrue(volumes.technologyChangeAlertVolumes(TechnologyChangeTarget.TO_5G_ENDC).soundEnabled)
    }

    @Test
    fun withTechnologyChangeSoundEnabled_togglesEachTargetIndependently() {
        val muted2g = AudioVolumeSettings().withTechnologyChangeSoundEnabled(
            TechnologyChangeTarget.TO_2G,
            false
        )
        val muted4g = muted2g.withTechnologyChangeSoundEnabled(
            TechnologyChangeTarget.TO_4G,
            false
        )

        assertFalse(muted4g.technologyChangeAlertVolumes(TechnologyChangeTarget.TO_2G).soundEnabled)
        assertFalse(muted4g.technologyChangeAlertVolumes(TechnologyChangeTarget.TO_4G).soundEnabled)
        assertTrue(muted4g.technologyChangeAlertVolumes(TechnologyChangeTarget.TO_5G_ENDC).soundEnabled)
    }
}
