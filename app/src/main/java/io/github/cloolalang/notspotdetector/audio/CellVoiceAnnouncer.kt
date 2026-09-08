package io.github.cloolalang.notspotdetector.audio

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerChoice
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerOption
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerSelection
import java.util.ArrayDeque
import java.util.Locale

class CellVoiceAnnouncer(context: Context) : TextToSpeech.OnInitListener {

    companion object {
        /** Default TTS rate is 1.0; slightly faster for concise cell-change alerts. */
        private const val SPEECH_RATE = 1.2f
        private const val DEFAULT_PITCH = 1.0f
    }

    private data class PendingSpeech(
        val text: String,
        val volume: Float,
        val selection: VoiceAnnouncerSelection?
    )

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    private var ready = false
    private var availableVoices: Set<Voice> = emptySet()
    private var voiceSelectionProvider: () -> VoiceAnnouncerSelection = {
        VoiceAnnouncerSelection(VoiceAnnouncerChoice.SYSTEM_DEFAULT, null)
    }
    private var onReadyListener: (() -> Unit)? = null
    private val pending = ArrayDeque<PendingSpeech>()

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            refreshAvailableVoices()
            tts?.language = Locale.getDefault()
            tts?.setSpeechRate(SPEECH_RATE)
            applySelectedVoice()
            onReadyListener?.invoke()
            drainPending()
        }
    }

    fun setVoiceSelectionProvider(provider: () -> VoiceAnnouncerSelection) {
        voiceSelectionProvider = provider
        if (ready) {
            applySelectedVoice()
        }
    }

    @Deprecated("Use setVoiceSelectionProvider")
    fun setVoiceChoiceProvider(provider: () -> VoiceAnnouncerChoice) {
        setVoiceSelectionProvider {
            VoiceAnnouncerSelection(choice = provider(), engineVoiceId = null)
        }
    }

    fun setOnReadyListener(listener: (() -> Unit)?) {
        onReadyListener = listener
        if (ready) {
            listener?.invoke()
        }
    }

    fun getResolvedOptions(): List<VoiceAnnouncerOption> {
        refreshAvailableVoices()
        return TtsVoiceCatalog.resolveOptions(availableVoices, Locale.getDefault())
    }

    fun speak(
        text: String,
        volume: Float,
        selection: VoiceAnnouncerSelection? = null
    ) {
        if (volume <= 0f || text.isBlank()) return
        mainHandler.post {
            speakOnMainThread(text, volume, selection)
        }
    }

    fun stop() {
        mainHandler.post {
            tts?.stop()
            pending.clear()
        }
    }

    fun shutdown() {
        mainHandler.post {
            tts?.stop()
            pending.clear()
            tts?.shutdown()
            tts = null
            ready = false
            availableVoices = emptySet()
        }
    }

    private fun speakOnMainThread(
        text: String,
        volume: Float,
        selection: VoiceAnnouncerSelection?
    ) {
        if (!ready) {
            pending.addLast(PendingSpeech(text, volume, selection))
            return
        }
        refreshAvailableVoices()
        applySelectedVoice(selection)
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

    private fun refreshAvailableVoices() {
        val latest = tts?.voices ?: return
        availableVoices = latest
    }

    private fun applySelectedVoice(selectionOverride: VoiceAnnouncerSelection? = null) {
        val engine = tts ?: return
        val selection = selectionOverride ?: voiceSelectionProvider()
        if (selection.choice == VoiceAnnouncerChoice.SYSTEM_DEFAULT) {
            engine.language = Locale.getDefault()
            engine.setPitch(DEFAULT_PITCH)
            return
        }

        refreshAvailableVoices()
        val resolved = TtsVoiceCatalog.resolveVoice(
            voices = availableVoices,
            choice = selection.choice,
            locale = Locale.getDefault(),
            engineVoiceId = selection.engineVoiceId
        )
        if (resolved != null) {
            engine.setLanguage(resolved.locale)
            if (engine.setVoice(resolved) != TextToSpeech.SUCCESS) {
                engine.voice = resolved
            }
        } else {
            engine.language = Locale.getDefault()
        }
        engine.setPitch(pitchForChoice(selection.choice))
    }

    private fun pitchForChoice(choice: VoiceAnnouncerChoice): Float {
        return when (choice) {
            VoiceAnnouncerChoice.MALE_1 -> 0.82f
            VoiceAnnouncerChoice.MALE_2 -> 0.72f
            VoiceAnnouncerChoice.MALE_3 -> 0.92f
            VoiceAnnouncerChoice.FEMALE_1 -> 1.05f
            VoiceAnnouncerChoice.FEMALE_2 -> 1.15f
            VoiceAnnouncerChoice.FEMALE_3 -> 1.25f
            VoiceAnnouncerChoice.SYSTEM_DEFAULT -> DEFAULT_PITCH
        }
    }

    private fun drainPending() {
        while (pending.isNotEmpty()) {
            val item = pending.removeFirst()
            speakOnMainThread(item.text, item.volume, item.selection)
        }
    }
}
