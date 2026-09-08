package io.github.cloolalang.notspotdetector.audio

import android.speech.tts.Voice
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerChoice
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerOption
import java.util.Locale

object TtsVoiceCatalog {

    enum class VoiceGenderHint {
        MALE,
        FEMALE,
        UNKNOWN
    }

    private val femaleNameHints = listOf(
        "female",
        "woman",
        "girl",
        "zira",
        "susan",
        "hazel",
        "kate",
        "emma",
        "amy",
        "joanna",
        "salli",
        "ivy",
        "aria",
        "linda",
        "heather",
        "moira",
        "tessa",
        "fiona",
        "veena",
        "kendra"
    )

    private val maleNameHints = listOf(
        "male",
        "man",
        "boy",
        "david",
        "mark",
        "james",
        "daniel",
        "george",
        "brian",
        "matthew",
        "ryan",
        "tom",
        "alex",
        "fred",
        "guy",
        "russell",
        "benjamin",
        "lee",
        "aaron",
        "nathan"
    )

    private val googleFemaleVoicePatterns = listOf(
        "-gba-",
        "-gbc-",
        "-tpf-",
        "-sfg-",
        "-sxg-",
        "-tif-"
    )

    private val googleMaleVoicePatterns = listOf(
        "-gbb-",
        "-gbg-",
        "-gbd-",
        "-tpd-",
        "-iob-",
        "-jsm-",
        "-std-"
    )

    private val slottedChoices = listOf(
        VoiceAnnouncerChoice.MALE_1,
        VoiceAnnouncerChoice.MALE_2,
        VoiceAnnouncerChoice.MALE_3,
        VoiceAnnouncerChoice.FEMALE_1,
        VoiceAnnouncerChoice.FEMALE_2,
        VoiceAnnouncerChoice.FEMALE_3
    )

    fun classifyVoiceGender(voice: Voice): VoiceGenderHint {
        return classifyGenderFromHints(
            name = voice.name,
            features = voice.features.orEmpty()
        )
    }

    fun classifyGenderFromHints(name: String, features: Set<String>): VoiceGenderHint {
        val normalizedFeatures = features.map { it.lowercase(Locale.US) }
        if (normalizedFeatures.any { "female" in it || it == "f" }) {
            return VoiceGenderHint.FEMALE
        }
        if (normalizedFeatures.any { "male" in it || it == "m" }) {
            return VoiceGenderHint.MALE
        }

        val normalizedName = name.lowercase(Locale.US)
        if (normalizedName.contains("#female") || normalizedName.endsWith(":female")) {
            return VoiceGenderHint.FEMALE
        }
        if (normalizedName.contains("#male") || normalizedName.endsWith(":male")) {
            return VoiceGenderHint.MALE
        }
        if (googleFemaleVoicePatterns.any { pattern -> normalizedName.contains(pattern) }) {
            return VoiceGenderHint.FEMALE
        }
        if (googleMaleVoicePatterns.any { pattern -> normalizedName.contains(pattern) }) {
            return VoiceGenderHint.MALE
        }
        if (femaleNameHints.any { hint -> normalizedName.contains(hint) }) {
            return VoiceGenderHint.FEMALE
        }
        if (maleNameHints.any { hint -> normalizedName.contains(hint) }) {
            return VoiceGenderHint.MALE
        }
        return VoiceGenderHint.UNKNOWN
    }

    fun resolveOptions(
        voices: Set<Voice>,
        locale: Locale = Locale.getDefault()
    ): List<VoiceAnnouncerOption> {
        val assignments = buildVoiceAssignments(voices, locale)
        return VoiceAnnouncerChoice.selectableChoices.map { choice ->
            val voice = assignments[choice]
            VoiceAnnouncerOption(
                choice = choice,
                engineVoiceId = voice?.name,
                engineVoiceName = voice?.let(::displayVoiceName),
                available = choice == VoiceAnnouncerChoice.SYSTEM_DEFAULT || voice != null
            )
        }
    }

    fun resolveVoice(
        voices: Set<Voice>,
        choice: VoiceAnnouncerChoice,
        locale: Locale = Locale.getDefault(),
        engineVoiceId: String? = null
    ): Voice? {
        if (choice == VoiceAnnouncerChoice.SYSTEM_DEFAULT) {
            return null
        }
        findVoiceById(voices, engineVoiceId)?.let { return it }
        return buildVoiceAssignments(voices, locale)[choice]
    }

    fun findVoiceById(voices: Set<Voice>, engineVoiceId: String?): Voice? {
        if (engineVoiceId.isNullOrBlank()) return null
        return voices.find { it.name == engineVoiceId }
    }

    fun displayVoiceName(voice: Voice): String {
        return voice.name.substringAfterLast('-').substringAfterLast(':').ifBlank { voice.name }
    }

    internal fun buildVoiceAssignments(
        voices: Set<Voice>,
        locale: Locale = Locale.getDefault()
    ): Map<VoiceAnnouncerChoice, Voice?> {
        val sorted = localeVoices(voices, locale)
        if (sorted.isEmpty()) {
            return VoiceAnnouncerChoice.selectableChoices.associateWith { null }
        }

        val maleVoices = sorted.filter { classifyVoiceGender(it) == VoiceGenderHint.MALE }
        val femaleVoices = sorted.filter { classifyVoiceGender(it) == VoiceGenderHint.FEMALE }
        val unknownVoices = sorted.filter { classifyVoiceGender(it) == VoiceGenderHint.UNKNOWN }

        val usedVoiceNames = mutableSetOf<String>()
        val assignments = mutableMapOf<VoiceAnnouncerChoice, Voice?>()
        assignments[VoiceAnnouncerChoice.SYSTEM_DEFAULT] = null

        assignDistinctSlots(
            choices = listOf(
                VoiceAnnouncerChoice.MALE_1,
                VoiceAnnouncerChoice.MALE_2,
                VoiceAnnouncerChoice.MALE_3
            ),
            primaryPool = maleVoices,
            fallbackPool = unknownVoices,
            usedVoiceNames = usedVoiceNames,
            assignments = assignments
        )
        assignDistinctSlots(
            choices = listOf(
                VoiceAnnouncerChoice.FEMALE_1,
                VoiceAnnouncerChoice.FEMALE_2,
                VoiceAnnouncerChoice.FEMALE_3
            ),
            primaryPool = femaleVoices,
            fallbackPool = unknownVoices.filter { it.name !in usedVoiceNames },
            usedVoiceNames = usedVoiceNames,
            assignments = assignments
        )

        val remaining = sorted.filter { it.name !in usedVoiceNames }
        slottedChoices
            .filter { assignments[it] == null }
            .forEachIndexed { index, choice ->
                val voice = remaining.getOrNull(index) ?: return@forEachIndexed
                usedVoiceNames.add(voice.name)
                assignments[choice] = voice
            }

        return assignments
    }

    private fun localeVoices(voices: Set<Voice>, locale: Locale): List<Voice> {
        val languageMatches = voices
            .filter { matchesLocale(it, locale) }
            .sortedBy { it.name.lowercase(Locale.US) }
        if (languageMatches.isNotEmpty()) {
            return languageMatches
        }
        return voices.sortedBy { it.name.lowercase(Locale.US) }
    }

    private fun assignDistinctSlots(
        choices: List<VoiceAnnouncerChoice>,
        primaryPool: List<Voice>,
        fallbackPool: List<Voice>,
        usedVoiceNames: MutableSet<String>,
        assignments: MutableMap<VoiceAnnouncerChoice, Voice?>
    ) {
        choices.forEachIndexed { index, choice ->
            val voice = takeVoiceAtSlot(primaryPool, index, usedVoiceNames)
                ?: takeVoiceAtSlot(fallbackPool, index, usedVoiceNames)
            assignments[choice] = voice
        }
    }

    private fun takeVoiceAtSlot(
        pool: List<Voice>,
        slotIndex: Int,
        usedVoiceNames: MutableSet<String>
    ): Voice? {
        val unused = pool.filter { it.name !in usedVoiceNames }
        val voice = unused.getOrNull(slotIndex) ?: return null
        usedVoiceNames.add(voice.name)
        return voice
    }

    private fun matchesLocale(voice: Voice, locale: Locale): Boolean {
        return voice.locale.language.equals(locale.language, ignoreCase = true)
    }
}
