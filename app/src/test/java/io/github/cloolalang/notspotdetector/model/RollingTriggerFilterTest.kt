package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RollingTriggerFilterTest {

    private val defaultNoSignal = RxssStateFilterSettings.DEFAULT_NO_SIGNAL

    @Test
    fun defaultNoSignalFilter_requiresTwoPollsToEnter() {
        val filter = RollingTriggerFilter()

        val firstEnterPoll = filter.update(true, defaultNoSignal)
        assertFalse(firstEnterPoll.confirmedActive)
        assertFalse(firstEnterPoll.transitioned)

        val result = filter.update(true, defaultNoSignal)
        assertTrue(result.confirmedActive)
        assertTrue(result.transitioned)
    }

    @Test
    fun defaultNoSignalFilter_requiresTwoPollsToExit() {
        val filter = RollingTriggerFilter()
        filter.update(true, defaultNoSignal)
        filter.update(true, defaultNoSignal)

        val firstExitPoll = filter.update(false, defaultNoSignal)
        assertTrue(firstExitPoll.confirmedActive)
        assertFalse(firstExitPoll.transitioned)

        val result = filter.update(false, defaultNoSignal)
        assertFalse(result.confirmedActive)
        assertTrue(result.transitioned)
    }

    @Test
    fun defaultNoSignalFilter_singlePollOscillationDoesNotChangeState() {
        val filter = RollingTriggerFilter()

        filter.update(true, defaultNoSignal)
        filter.update(false, defaultNoSignal)
        filter.update(true, defaultNoSignal)

        assertFalse(filter.confirmedActive)
    }

    @Test
    fun inactiveFilter_adoptsImmediately() {
        val filter = RollingTriggerFilter()
        val inactive = RxssStateFilterSettings.INACTIVE

        val result = filter.update(true, inactive)
        assertTrue(result.confirmedActive)
        assertTrue(result.transitioned)
    }

    @Test
    fun zeroSeconds_adoptsImmediatelyEvenWhenEnabled() {
        val filter = RollingTriggerFilter()
        val settings = RxssStateFilterSettings(enabled = true, windowSeconds = 0, balancePercent = 50)

        val result = filter.update(true, settings)
        assertTrue(result.confirmedActive)
        assertTrue(result.transitioned)
    }

    @Test
    fun threeSecondHold_needsThreeFurtherMatchingPolls() {
        val filter = RollingTriggerFilter()
        val settings = RxssStateFilterSettings(enabled = true, windowSeconds = 3, balancePercent = 50)

        assertFalse(filter.update(true, settings).confirmedActive)
        assertFalse(filter.update(true, settings).confirmedActive)
        assertFalse(filter.update(true, settings).confirmedActive)
        assertTrue(filter.update(true, settings).confirmedActive)
    }

    @Test
    fun balanceAllEntry_exitsImmediately() {
        val filter = RollingTriggerFilter()
        val settings = RxssStateFilterSettings(enabled = true, windowSeconds = 2, balancePercent = 100)
        filter.update(true, settings)
        filter.update(true, settings)
        filter.update(true, settings)
        assertTrue(filter.confirmedActive)

        val exit = filter.update(false, settings)
        assertFalse(exit.confirmedActive)
        assertTrue(exit.transitioned)
    }

    @Test
    fun balanceAllExit_entersImmediately() {
        val filter = RollingTriggerFilter()
        val settings = RxssStateFilterSettings(enabled = true, windowSeconds = 2, balancePercent = 0)

        val enter = filter.update(true, settings)
        assertTrue(enter.confirmedActive)
        assertTrue(enter.transitioned)
    }

    @Test
    fun resetClearsConfirmedState() {
        val filter = RollingTriggerFilter()
        filter.update(true, defaultNoSignal)
        filter.update(true, defaultNoSignal)

        filter.reset()

        assertFalse(filter.confirmedActive)
        assertFalse(filter.update(true, defaultNoSignal).confirmedActive)
    }

    @Test
    fun holdSeconds_equalBalanceUsesFullWindow() {
        val settings = RxssStateFilterSettings(enabled = true, windowSeconds = 4, balancePercent = 50)
        assertEquals(4, settings.holdSeconds(entering = true))
        assertEquals(4, settings.holdSeconds(entering = false))
    }

    @Test
    fun holdSeconds_splitsWindowByBalance() {
        val settings = RxssStateFilterSettings(enabled = true, windowSeconds = 10, balancePercent = 70)
        assertEquals(7, settings.holdSeconds(entering = true))
        assertEquals(3, settings.holdSeconds(entering = false))
    }
}
