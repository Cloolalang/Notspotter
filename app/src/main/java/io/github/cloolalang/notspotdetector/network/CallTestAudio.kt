package io.github.cloolalang.notspotdetector.network

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.telecom.InCallService

/**
 * Silences a running call test.
 *
 * Android does not apply [AudioManager.ADJUST_MUTE] to [AudioManager.STREAM_VOICE_CALL],
 * so the voice-call volume is set to zero instead. The phone often puts that volume
 * back when a call connects, so it is set again until the test stops. The microphone
 * is muted as well. Both are restored when the test stops.
 *
 * The held state is stored so a test that dies mid-call still restores the previous
 * volume the next time the app starts.
 */
object CallTestAudio {
    private val handler = Handler(Looper.getMainLooper())
    private val reapplyLoop = Runnable { reapplyScheduled() }

    @Volatile
    private var requested = false

    private var appContext: Context? = null

    /** Volume the platform refused to lower past, so a retry does not keep writing it. */
    private var refusedVolume = -1

    fun isRequested(): Boolean = requested

    fun setRequested(context: Context, mute: Boolean) {
        val application = context.applicationContext
        appContext = application
        requested = mute
        if (mute) {
            hold(application)
            handler.removeCallbacks(reapplyLoop)
            handler.postDelayed(reapplyLoop, REAPPLY_MS)
        } else {
            handler.removeCallbacks(reapplyLoop)
            release(application)
        }
    }

    /** A new call, or a change in its audio path, can restore the call volume. */
    fun reapply() {
        if (!requested) return
        val context = appContext ?: return
        silenceVoice(audioManager(context))
        muteMicrophone(audioManager(context))
    }

    /** Restores audio when a previous test muted it and this process is no longer in that test. */
    fun releaseAbandoned(context: Context) {
        if (requested) return
        release(context.applicationContext)
    }

    private fun reapplyScheduled() {
        if (!requested) return
        reapply()
        handler.postDelayed(reapplyLoop, REAPPLY_MS)
    }

    private fun hold(context: Context) {
        val prefs = prefs(context)
        val audio = audioManager(context)
        if (!prefs.getBoolean(KEY_HELD, false)) {
            prefs.edit()
                .putBoolean(KEY_HELD, true)
                .putInt(KEY_VOICE_VOLUME, audio.getStreamVolume(AudioManager.STREAM_VOICE_CALL))
                .putBoolean(KEY_MIC_WAS_MUTED, audio.isMicrophoneMute)
                .commit()
        }
        refusedVolume = -1
        silenceVoice(audio)
        muteMicrophone(audio)
    }

    private fun release(context: Context) {
        val prefs = prefs(context)
        if (!prefs.getBoolean(KEY_HELD, false)) return
        val audio = audioManager(context)
        val savedVolume = prefs.getInt(KEY_VOICE_VOLUME, -1)
        if (savedVolume >= 0) {
            runCatching {
                audio.setStreamVolume(AudioManager.STREAM_VOICE_CALL, savedVolume, 0)
            }
        }
        val micWasMuted = prefs.getBoolean(KEY_MIC_WAS_MUTED, false)
        runCatching { audio.setMicrophoneMute(micWasMuted) }
        CallTestInCallMute.muteMicrophone(micWasMuted)
        refusedVolume = -1
        prefs.edit().clear().apply()
    }

    private fun silenceVoice(audio: AudioManager) {
        val current = audio.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
        if (current <= 0 || current == refusedVolume) return
        runCatching { audio.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 0, 0) }
        val after = audio.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
        if (after >= current) {
            refusedVolume = after
        }
    }

    private fun muteMicrophone(audio: AudioManager) {
        if (!audio.isMicrophoneMute) {
            runCatching { audio.setMicrophoneMute(true) }
        }
        CallTestInCallMute.muteMicrophone(true)
    }

    private fun audioManager(context: Context): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private const val PREFS_NAME = "notspot_call_test_audio"
    private const val KEY_HELD = "held"
    private const val KEY_VOICE_VOLUME = "voice_volume"
    private const val KEY_MIC_WAS_MUTED = "mic_was_muted"
    private const val REAPPLY_MS = 400L
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
