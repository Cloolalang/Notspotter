package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LteLayerResilienceDebouncerTest {

    private fun reading(primary: Int, alternateLayers: Int, alternateCells: Int) =
        LteLayerResilienceReading(
            primaryLayerCellCount = primary,
            alternateLayerCount = alternateLayers,
            alternateLayerCellCount = alternateCells
        )

    @Test
    fun requiresTwoPollsToConfirmANewReading() {
        val debouncer = LteLayerResilienceDebouncer()
        val candidate = reading(2, 2, 3)

        val firstPoll = debouncer.update(candidate)
        assertNull(firstPoll.confirmedReading)
        assertFalse(firstPoll.transitioned)

        val result = debouncer.update(candidate)
        assertEquals(candidate, result.confirmedReading)
        assertTrue(result.transitioned)
    }

    @Test
    fun requiresTwoMatchingPollsToChangeAnAlreadyConfirmedReading() {
        val debouncer = LteLayerResilienceDebouncer()
        val original = reading(1, 1, 1)
        debouncer.update(original)
        debouncer.update(original)

        val updated = reading(2, 2, 3)
        val firstChangePoll = debouncer.update(updated)
        assertEquals(original, firstChangePoll.confirmedReading)
        assertFalse(firstChangePoll.transitioned)

        val result = debouncer.update(updated)
        assertEquals(updated, result.confirmedReading)
        assertTrue(result.transitioned)
    }

    @Test
    fun partialChangeIsTreatedAsADifferentReadingAsAWhole() {
        val debouncer = LteLayerResilienceDebouncer()
        val original = reading(2, 2, 3)
        debouncer.update(original)
        debouncer.update(original)

        // Only alternateLayerCellCount differs — still a distinct reading, needs full confirmation.
        val partiallyChanged = reading(2, 2, 4)
        val firstPoll = debouncer.update(partiallyChanged)
        assertEquals(original, firstPoll.confirmedReading)
        assertFalse(firstPoll.transitioned)
    }

    @Test
    fun singlePollFlickerDoesNotChangeConfirmedReading() {
        val debouncer = LteLayerResilienceDebouncer()
        val stable = reading(2, 2, 3)
        debouncer.update(stable)
        debouncer.update(stable)

        debouncer.update(reading(2, 3, 4))
        debouncer.update(stable)

        assertEquals(stable, debouncer.confirmedReading)
    }

    @Test
    fun nullReadingIsTreatedAsItsOwnDistinctValue() {
        val debouncer = LteLayerResilienceDebouncer()
        val stable = reading(2, 2, 3)
        debouncer.update(stable)
        debouncer.update(stable)

        val firstNullPoll = debouncer.update(null)
        assertEquals(stable, firstNullPoll.confirmedReading)
        assertFalse(firstNullPoll.transitioned)

        val result = debouncer.update(null)
        assertNull(result.confirmedReading)
        assertTrue(result.transitioned)
    }

    @Test
    fun resetClearsConfirmedState() {
        val debouncer = LteLayerResilienceDebouncer()
        val stable = reading(2, 2, 3)
        debouncer.update(stable)
        debouncer.update(stable)

        debouncer.reset()

        assertNull(debouncer.confirmedReading)
        assertNull(debouncer.update(stable).confirmedReading)
    }
}
