package io.github.cloolalang.notspotdetector.network

import android.content.Context
import android.media.AudioManager
import android.telecom.InCallService

/**
 * Silences a running call test. The voice-call stream is muted so the phone does not play
 * the call, and the microphone is muted. Both are put back when the test stops.
 *
 * The held state is stored so a test that dies mid-call still restores the previous audio
 * the next time the app starts.
 */
object CallTestAudio {
    @Volatile
    private var requested = false

    fun isRequested(): Boolean = requested

    fun setRequested(context: Context, mute: Boolean) {
        requested = mute
        if (mute) hold(context.applicationContext) else release(context.applicationContext)
    }

    /** A new call can clear the mute. Put it back while a muted test is running. */
    fun onCallAdded(service: InCallService) {
        if (!requested) return
        runCatching { service.setMuted(true) }
        hold(service.applicationContext)
    }

    /** Restores audio when a previous test muted it and this process is no longer in that test. */
    fun releaseAbandoned(context: Context) {
        if (requested) return
        release(context.applicationContext)
    }

    private fun hold(context: Context) {
        val prefs = prefs(context)
        val audio = audioManager(context)
        if (!prefs.getBoolean(KEY_HELD, false)) {
            prefs.edit()
                .putBoolean(KEY_HELD, true)
                .putBoolean(KEY_STREAM_WAS_MUTED, audio.isStreamMute(AudioManager.STREAM_VOICE_CALL))
                .putBoolean(KEY_MIC_WAS_MUTED, audio.isMicrophoneMute)
                .apply()
        }
        if (!audio.isStreamMute(AudioManager.STREAM_VOICE_CALL)) {
            audio.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_MUTE, 0)
        }
        if (!audio.isMicrophoneMute) {
            audio.setMicrophoneMute(true)
        }
        CallTestInCallMute.muteMicrophone(true)
    }

    private fun release(context: Context) {
        val prefs = prefs(context)
        if (!prefs.getBoolean(KEY_HELD, false)) return
        val audio = audioManager(context)
        val streamWasMuted = prefs.getBoolean(KEY_STREAM_WAS_MUTED, false)
        val micWasMuted = prefs.getBoolean(KEY_MIC_WAS_MUTED, false)
        if (!streamWasMuted && audio.isStreamMute(AudioManager.STREAM_VOICE_CALL)) {
            audio.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_UNMUTE, 0)
        }
        audio.setMicrophoneMute(micWasMuted)
        CallTestInCallMute.muteMicrophone(micWasMuted)
        prefs.edit().clear().apply()
    }

    private fun audioManager(context: Context): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private const val PREFS_NAME = "notspot_call_test_audio"
    private const val KEY_HELD = "held"
    private const val KEY_STREAM_WAS_MUTED = "stream_was_muted"
    private const val KEY_MIC_WAS_MUTED = "mic_was_muted"
}

/** The live in-call service, so a muted test can mute the microphone of the current call. */
object CallTestInCallMute {
    @Volatile
    private var service: InCallService? = null

    fun attach(inCallService: InCallService) {
        service = inCallService
    }

    fun detach(inCallService: InCallService) {
        if (service === inCallService) service = null
    }

    fun muteMicrophone(muted: Boolean) {
        val current = service ?: return
        runCatching { current.setMuted(muted) }
    }
}
