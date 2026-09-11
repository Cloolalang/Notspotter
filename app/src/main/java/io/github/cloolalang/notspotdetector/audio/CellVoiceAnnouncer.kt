package io.github.cloolalang.notspotdetector.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerChoice
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerOption
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerSelection
import java.util.ArrayDeque
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

class CellVoiceAnnouncer(context: Context) : TextToSpeech.OnInitListener {

    companion object {
        /** Default TTS rate is 1.0; slightly faster for concise cell-change alerts. */
        private const val SPEECH_RATE = 1.2f
        private const val DEFAULT_PITCH = 1.0f

        /**
         * Safety cap so a dropped TTS callback (common when the screen turns off) cannot stall
         * the monitoring alert mutex forever.
         */
        private const val MAX_UTTERANCE_WAIT_MS = 20_000L

        /**
         * Spoken guidance usage maps to the music stream (same volume as alert clicks) but is not
         * treated as background media that OEMs pause when the lock screen comes up.
         */
        private val TTS_AUDIO_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
            .build()
    }

    private data class PendingSpeech(
        val text: String,
        val volume: Float,
        val selection: VoiceAnnouncerSelection?
    )

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    private var ready = false
    private var availableVoices: Set<Voice> = emptySet()
    private var voiceSelectionProvider: () -> VoiceAnnouncerSelection = {
        VoiceAnnouncerSelection(VoiceAnnouncerChoice.SYSTEM_DEFAULT, null)
    }
    private var onReadyListener: (() -> Unit)? = null
    private val pending = ArrayDeque<PendingSpeech>()
    private var audioFocusRequest: AudioFocusRequest? = null
    @Volatile
    private var hasAudioFocus = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            refreshAvailableVoices()
            tts?.language = Locale.getDefault()
            tts?.setSpeechRate(SPEECH_RATE)
            applyPlaybackAudioAttributes()
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
            attachCompletionListener { abandonTtsAudioFocus() }
            speakOnMainThread(
                text = text,
                volume = volume,
                selection = selection,
                queueMode = TextToSpeech.QUEUE_FLUSH
            )
        }
    }

    suspend fun speakAwait(
        text: String,
        volume: Float,
        selection: VoiceAnnouncerSelection? = null
    ) {
        if (volume <= 0f || text.isBlank()) return
        val finished = AtomicBoolean(false)
        withTimeoutOrNull(MAX_UTTERANCE_WAIT_MS) {
            suspendCancellableCoroutine { continuation ->
                fun complete() {
                    if (!finished.compareAndSet(false, true)) return
                    abandonTtsAudioFocus()
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
                continuation.invokeOnCancellation {
                    mainHandler.post {
                        tts?.stop()
                        complete()
                    }
                }
                mainHandler.post {
                    if (!ready) {
                        pending.addLast(PendingSpeech(text, volume, selection))
                        complete()
                        return@post
                    }
                    val utteranceId = "voice_${System.nanoTime()}"
                    attachCompletionListener(utteranceId) { complete() }
                    val started = speakOnMainThread(
                        text = text,
                        volume = volume,
                        selection = selection,
                        queueMode = TextToSpeech.QUEUE_ADD,
                        utteranceId = utteranceId
                    )
                    if (!started) {
                        complete()
                    }
                }
            }
        }
        if (finished.compareAndSet(false, true)) {
            mainHandler.post {
                tts?.stop()
                abandonTtsAudioFocus()
            }
        }
    }

    fun stop() {
        mainHandler.post {
            tts?.stop()
            pending.clear()
            abandonTtsAudioFocus()
        }
    }

    fun shutdown() {
        mainHandler.post {
            tts?.stop()
            pending.clear()
            abandonTtsAudioFocus()
            tts?.shutdown()
            tts = null
            ready = false
            availableVoices = emptySet()
        }
    }

    private fun speakOnMainThread(
        text: String,
        volume: Float,
        selection: VoiceAnnouncerSelection?,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH,
        utteranceId: String = "voice_${System.nanoTime()}"
    ): Boolean {
        if (!ready) {
            pending.addLast(PendingSpeech(text, volume, selection))
            return false
        }
        refreshAvailableVoices()
        applySelectedVoice(selection)
        applyPlaybackAudioAttributes()
        requestTtsAudioFocus()
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f))
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }
        val result = tts?.speak(
            text,
            queueMode,
            params,
            utteranceId
        ) ?: TextToSpeech.ERROR
        if (result != TextToSpeech.SUCCESS) {
            abandonTtsAudioFocus()
            return false
        }
        return true
    }

    private fun attachCompletionListener(
        expectedUtteranceId: String? = null,
        onComplete: () -> Unit
    ) {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(id: String?) {
                if (expectedUtteranceId != null && id != expectedUtteranceId) return
                tts?.setOnUtteranceProgressListener(null)
                onComplete()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (expectedUtteranceId != null && id != expectedUtteranceId) return
                tts?.setOnUtteranceProgressListener(null)
                onComplete()
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                onError(utteranceId)
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                if (expectedUtteranceId != null && utteranceId != expectedUtteranceId) return
                tts?.setOnUtteranceProgressListener(null)
                onComplete()
            }
        })
    }

    private fun applyPlaybackAudioAttributes() {
        tts?.setAudioAttributes(TTS_AUDIO_ATTRIBUTES)
    }

    private fun requestTtsAudioFocus() {
        if (hasAudioFocus) return
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(TTS_AUDIO_ATTRIBUTES)
                .setWillPauseWhenDucked(false)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        hasAudioFocus = granted
    }

    private fun abandonTtsAudioFocus() {
        if (!hasAudioFocus) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { request ->
                audioManager.abandonAudioFocusRequest(request)
            }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
        hasAudioFocus = false
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
