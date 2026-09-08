package io.github.cloolalang.notspotdetector.audio

import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerChoice
import org.junit.Assert.assertEquals
import org.junit.Test

class TtsVoiceCatalogTest {

    @Test
    fun classifyGenderFromHints_usesVoiceFeatures() {
        assertEquals(
            TtsVoiceCatalog.VoiceGenderHint.FEMALE,
            TtsVoiceCatalog.classifyGenderFromHints("en-us-x-sfg-local", setOf("female"))
        )
        assertEquals(
            TtsVoiceCatalog.VoiceGenderHint.MALE,
            TtsVoiceCatalog.classifyGenderFromHints("en-us-x-sfg-local", setOf("male"))
        )
    }

    @Test
    fun classifyGenderFromHints_usesKnownVoiceNames() {
        assertEquals(
            TtsVoiceCatalog.VoiceGenderHint.FEMALE,
            TtsVoiceCatalog.classifyGenderFromHints("en-gb-x-gba-zira-local", emptySet())
        )
        assertEquals(
            TtsVoiceCatalog.VoiceGenderHint.MALE,
            TtsVoiceCatalog.classifyGenderFromHints("en-gb-x-gbb-david-local", emptySet())
        )
    }

    @Test
    fun classifyGenderFromHints_usesGoogleVoicePatterns() {
        assertEquals(
            TtsVoiceCatalog.VoiceGenderHint.FEMALE,
            TtsVoiceCatalog.classifyGenderFromHints("en-gb-x-gba-local", emptySet())
        )
        assertEquals(
            TtsVoiceCatalog.VoiceGenderHint.MALE,
            TtsVoiceCatalog.classifyGenderFromHints("en-gb-x-gbb-local", emptySet())
        )
        assertEquals(
            TtsVoiceCatalog.VoiceGenderHint.FEMALE,
            TtsVoiceCatalog.classifyGenderFromHints("en-us-x-tpf-local", emptySet())
        )
        assertEquals(
            TtsVoiceCatalog.VoiceGenderHint.MALE,
            TtsVoiceCatalog.classifyGenderFromHints("en-us-x-tpd-local", emptySet())
        )
    }

    @Test
    fun voiceAnnouncerChoice_fromId_roundTrips() {
        VoiceAnnouncerChoice.selectableChoices.forEach { choice ->
            assertEquals(choice, VoiceAnnouncerChoice.fromId(choice.id))
        }
        assertEquals(VoiceAnnouncerChoice.SYSTEM_DEFAULT, VoiceAnnouncerChoice.fromId(null))
        assertEquals(VoiceAnnouncerChoice.SYSTEM_DEFAULT, VoiceAnnouncerChoice.fromId("unknown"))
    }
}
