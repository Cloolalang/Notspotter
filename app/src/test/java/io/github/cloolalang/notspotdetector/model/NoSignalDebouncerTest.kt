package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoSignalDebouncerTest {

    @Test
    fun requiresTwoPollsToEnterNoSignal() {
        val debouncer = NoSignalDebouncer()

        val firstEnterPoll = debouncer.update(true)
        assertFalse(firstEnterPoll.confirmedActive)
        assertFalse(firstEnterPoll.transitioned)

        val result = debouncer.update(true)
        assertTrue(result.confirmedActive)
        assertTrue(result.transitioned)
    }

    @Test
    fun requiresTwoPollsToExitNoSignal() {
        val debouncer = NoSignalDebouncer()
        debouncer.update(true)
        debouncer.update(true)

        val firstExitPoll = debouncer.update(false)
        assertTrue(firstExitPoll.confirmedActive)
        assertFalse(firstExitPoll.transitioned)

        val result = debouncer.update(false)
        assertFalse(result.confirmedActive)
        assertTrue(result.transitioned)
    }

    @Test
    fun singlePollOscillationDoesNotChangeState() {
        val debouncer = NoSignalDebouncer()

        debouncer.update(true)
        debouncer.update(false)
        debouncer.update(true)

        assertFalse(debouncer.confirmedActive)
    }

    @Test
    fun resetClearsConfirmedState() {
        val debouncer = NoSignalDebouncer()
        debouncer.update(true)
        debouncer.update(true)

        debouncer.reset()

        assertFalse(debouncer.confirmedActive)
        assertFalse(debouncer.update(true).confirmedActive)
    }
}
