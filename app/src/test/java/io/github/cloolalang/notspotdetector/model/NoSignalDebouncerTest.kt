package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoSignalDebouncerTest {

    @Test
    fun requiresTwoPollsToEnterNoSignal() {
        val debouncer = NoSignalDebouncer()

        assertFalse(debouncer.update(true).confirmedActive)
        assertFalse(debouncer.update(true).transitioned)

        val result = debouncer.update(true)
        assertTrue(result.confirmedActive)
        assertTrue(result.transitioned)
    }

    @Test
    fun requiresTwoPollsToExitNoSignal() {
        val debouncer = NoSignalDebouncer()
        debouncer.update(true)
        debouncer.update(true)

        assertTrue(debouncer.update(false).confirmedActive)
        assertFalse(debouncer.update(false).transitioned)

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
