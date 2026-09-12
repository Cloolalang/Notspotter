package io.github.cloolalang.notspotdetector.audio

/**
 * Android TTS [android.speech.tts.TextToSpeech.setSpeechRate] accepts any value &gt; 0, but
 * many engines only play audio reliably up to about 2×. Faster settings can make
 * [android.speech.tts.TextToSpeech.speak] fail or finish synthesis before the speaker starts —
 * short mock-mode phrases then become inaudible.
 */
object TtsSpeechRate {
    const val MIN = 0.5f
    const val MAX = 5.0f
    const val SAFE_MAX = 2.0f

    fun candidates(requested: Float): List<Float> {
        val rate = requested.coerceIn(MIN, MAX)
        return listOf(rate, 3.0f, SAFE_MAX, 1.5f, 1.2f)
            .filter { it <= rate + 0.001f }
            .distinct()
    }

    fun audioFocusHoldMs(appliedRate: Float): Long {
        return (120L + (appliedRate.coerceAtLeast(1f) * 80f).toLong()).coerceIn(80L, 500L)
    }
}
