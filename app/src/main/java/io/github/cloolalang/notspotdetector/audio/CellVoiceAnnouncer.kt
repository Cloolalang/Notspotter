package io.github.cloolalang.notspotdetector.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.ArrayDeque

class CellVoiceAnnouncer(context: Context) : TextToSpeech.OnInitListener {

    companion object {
        /** Default TTS rate is 1.0; slightly faster for concise cell-change alerts. */
        private const val SPEECH_RATE = 1.2f
    }

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private val pending = ArrayDeque<Pair<String, Float>>()

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts?.language = Locale.getDefault()
            tts?.setSpeechRate(SPEECH_RATE)
            drainPending()
        }
    }

    fun speak(text: String, volume: Float) {
        if (volume <= 0f || text.isBlank()) return
        if (!ready) {
            pending.addLast(text to volume)
            return
        }
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f))
        }
        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            params,
            "cell_reselect_${System.nanoTime()}"
        )
    }

    fun stop() {
        tts?.stop()
        pending.clear()
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        ready = false
    }

    private fun drainPending() {
        while (pending.isNotEmpty()) {
            val (text, volume) = pending.removeFirst()
            speak(text, volume)
        }
    }
}
