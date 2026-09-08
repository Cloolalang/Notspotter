package io.github.cloolalang.notspotdetector.model

data class VoiceAnnouncerSelection(
    val choice: VoiceAnnouncerChoice,
    val engineVoiceId: String?
) {
    companion object {
        fun fromSettings(settings: AudioVolumeSettings): VoiceAnnouncerSelection {
            return VoiceAnnouncerSelection(
                choice = settings.voiceAnnouncerChoice,
                engineVoiceId = settings.voiceAnnouncerEngineId
            )
        }
    }
}

enum class VoiceAnnouncerChoice(val id: String) {
    SYSTEM_DEFAULT("system"),
    MALE_1("male_1"),
    MALE_2("male_2"),
    MALE_3("male_3"),
    FEMALE_1("female_1"),
    FEMALE_2("female_2"),
    FEMALE_3("female_3");

    val isMale: Boolean
        get() = name.startsWith("MALE")

    val isFemale: Boolean
        get() = name.startsWith("FEMALE")

    val slotIndex: Int
        get() = when (this) {
            SYSTEM_DEFAULT -> 0
            MALE_1, FEMALE_1 -> 0
            MALE_2, FEMALE_2 -> 1
            MALE_3, FEMALE_3 -> 2
        }

    companion object {
        val selectableChoices: List<VoiceAnnouncerChoice> = listOf(
            SYSTEM_DEFAULT,
            MALE_1,
            MALE_2,
            MALE_3,
            FEMALE_1,
            FEMALE_2,
            FEMALE_3
        )

        fun fromId(id: String?): VoiceAnnouncerChoice {
            if (id.isNullOrBlank()) return SYSTEM_DEFAULT
            return entries.find { it.id == id } ?: SYSTEM_DEFAULT
        }
    }
}

data class VoiceAnnouncerOption(
    val choice: VoiceAnnouncerChoice,
    val engineVoiceId: String?,
    val engineVoiceName: String?,
    val available: Boolean
)
