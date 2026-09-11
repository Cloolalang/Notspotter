package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LteLayerResilienceDebouncerTest {

    @Test
    fun requiresTwoPollsToConfirmANewLayerCount() {
        val debouncer = LteLayerResilienceDebouncer()

        val firstPoll = debouncer.update(3)
        assertNull(firstPoll.confirmedLayerCount)
        assertFalse(firstPoll.transitioned)

        val result = debouncer.update(3)
        assertEquals(3, result.confirmedLayerCount)
        assertTrue(result.transitioned)
    }

    @Test
    fun requiresTwoMatchingPollsToChangeAnAlreadyConfirmedCount() {
        val debouncer = LteLayerResilienceDebouncer()
        debouncer.update(2)
        debouncer.update(2)

        val firstDropPoll = debouncer.update(1)
        assertEquals(2, firstDropPoll.confirmedLayerCount)
        assertFalse(firstDropPoll.transitioned)

        val result = debouncer.update(1)
        assertEquals(1, result.confirmedLayerCount)
        assertTrue(result.transitioned)
    }

    @Test
    fun singlePollFlickerDoesNotChangeConfirmedCount() {
        val debouncer = LteLayerResilienceDebouncer()
        debouncer.update(2)
        debouncer.update(2)

        debouncer.update(3)
        debouncer.update(2)

        assertEquals(2, debouncer.confirmedLayerCount)
    }

    @Test
    fun differingPendingTargetsResetTheConfirmationCount() {
        val debouncer = LteLayerResilienceDebouncer()
        debouncer.update(2)
        debouncer.update(2)

        debouncer.update(3)
        // A different candidate (4) restarts confirmation rather than counting toward 3.
        val result = debouncer.update(4)

        assertEquals(2, result.confirmedLayerCount)
        assertFalse(result.transitioned)
    }

    @Test
    fun nullReadingIsTreatedAsItsOwnDistinctValue() {
        val debouncer = LteLayerResilienceDebouncer()
        debouncer.update(2)
        debouncer.update(2)

        val firstNullPoll = debouncer.update(null)
        assertEquals(2, firstNullPoll.confirmedLayerCount)
        assertFalse(firstNullPoll.transitioned)

        val result = debouncer.update(null)
        assertNull(result.confirmedLayerCount)
        assertTrue(result.transitioned)
    }

    @Test
    fun resetClearsConfirmedState() {
        val debouncer = LteLayerResilienceDebouncer()
        debouncer.update(3)
        debouncer.update(3)

        debouncer.reset()

        assertNull(debouncer.confirmedLayerCount)
        assertNull(debouncer.update(3).confirmedLayerCount)
    }
}
