package com.example.safitness.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.ArrayDeque
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class CuePlayer(
    ctx: Context,
    private val preferVoice: Boolean = true,
    private val voiceOnAlarmStream: Boolean = false,
    private val enginePackage: String? = null // e.g. GOOGLE_ENGINE; null = default engine
) : AudioManager.OnAudioFocusChangeListener {

    companion object {
        const val TAG = "CuePlayer"
        const val GOOGLE_ENGINE = "com.google.android.tts"
        private val FOCUS_TOKEN = Any()
    }

    private val app = ctx.applicationContext
    private val am = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val main = Handler(Looper.getMainLooper())
    private val released = AtomicBoolean(false)
    private val initStartedAt = SystemClock.uptimeMillis()
    private val pending = ArrayDeque<String>()

    private val pip = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    private val buzz = ToneGenerator(AudioManager.STREAM_ALARM, 100)

    @Volatile private var ttsReady = false
    private var tts: TextToSpeech? = null

    init {
        // Try requested engine first; if it fails we'll fall back inside onTtsInit()
        createTts(useDefaultEngine = (enginePackage == null))
    }

    // --- TTS creation & init handling --------------------------------------

    private fun createTts(useDefaultEngine: Boolean) {
        tts = if (!useDefaultEngine && enginePackage != null) {
            TextToSpeech(app, { status -> onTtsInit(status, usedDefault = false) }, enginePackage)
        } else {
            TextToSpeech(app) { status -> onTtsInit(status, usedDefault = true) }
        }
    }

    private fun onTtsInit(status: Int, usedDefault: Boolean) {
        if (released.get()) return
        val t = tts ?: return

        val ok = status == TextToSpeech.SUCCESS
        var langOk = false
        if (ok) {
            // Prefer UK; fall back to device default
            val resUk = t.setLanguage(Locale.UK)
            langOk = if (resUk == TextToSpeech.LANG_MISSING_DATA || resUk == TextToSpeech.LANG_NOT_SUPPORTED) {
                t.setLanguage(Locale.getDefault()) >= TextToSpeech.LANG_AVAILABLE
            } else resUk >= TextToSpeech.LANG_AVAILABLE
        }

        if (Build.VERSION.SDK_INT >= 21) {
            val usage = if (voiceOnAlarmStream) AudioAttributes.USAGE_ALARM
            else AudioAttributes.USAGE_ASSISTANCE_SONIFICATION
            t.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
        }
        t.setSpeechRate(1.08f)
        t.setPitch(1.0f)
        ttsReady = ok && langOk

        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { Log.d(TAG, "TTS start: $utteranceId") }
            override fun onDone(utteranceId: String?)  { Log.d(TAG, "TTS done:  $utteranceId") }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { Log.e(TAG, "TTS error: $utteranceId") }
            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "TTS error($errorCode): $utteranceId")
            }
        })

        val engineName = runCatching { t.defaultEngine }.getOrNull()
        Log.i(TAG, "onInit ok=$ok langOk=$langOk engine=$engineName voiceOnAlarm=$voiceOnAlarmStream usedDefault=$usedDefault")

        // If we tried a specific engine and init failed, fall back to default once.
        if (!ttsReady && !usedDefault && enginePackage != null) {
            Log.w(TAG, "Engine '$enginePackage' failed; falling back to default engine")
            createTts(useDefaultEngine = true)
            return
        }

        if (ttsReady) flushPending()
    }

    // --- Audio focus --------------------------------------------------------

    private val focusReq: AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= 26)
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setOnAudioFocusChangeListener(this, main)
                .setWillPauseWhenDucked(false)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()
        else null

    override fun onAudioFocusChange(focusChange: Int) { /* no-op */ }

    private fun withFocus(durationMs: Int, play: () -> Unit) {
        if (released.get()) return
        if (Build.VERSION.SDK_INT >= 26) am.requestAudioFocus(focusReq!!)
        else @Suppress("DEPRECATION")
        am.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        runCatching { play() }
        main.removeCallbacksAndMessages(FOCUS_TOKEN)
        main.postAtTime({ abandonFocus() }, FOCUS_TOKEN, SystemClock.uptimeMillis() + durationMs + 50)
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= 26) focusReq?.let { am.abandonAudioFocusRequest(it) }
        else @Suppress("DEPRECATION") am.abandonAudioFocus(this)
    }

    // --- Public API ---------------------------------------------------------

    /** Speak a short phrase; queues if TTS not ready; beeps after long init timeout. */
    fun say(phrase: String) {
        if (!preferVoice || released.get()) { pip(); return }
        val t = tts
        if (ttsReady && t != null) {
            speakNow(t, phrase)
        } else {
            pending.addLast(phrase)
            val elapsed = SystemClock.uptimeMillis() - initStartedAt
            if (elapsed > 10_000L) { // after 10s without init, at least pip
                Log.w(TAG, "TTS not ready after 10s; beeping instead")
                pip()
            }
        }
    }

    fun pip(durationMs: Int = 150) = withFocus(durationMs) {
        pip.startTone(ToneGenerator.TONE_PROP_BEEP, durationMs)
    }

    fun tick(durationMs: Int = 150) = withFocus(durationMs) {
        pip.startTone(ToneGenerator.TONE_PROP_ACK, durationMs)
    }

    fun finalBuzz(durationMs: Int = 600) = withFocus(durationMs) {
        buzz.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, durationMs)
    }

    fun release() {
        if (released.compareAndSet(false, true)) {
            abandonFocus()
            runCatching { pip.release() }
            runCatching { buzz.release() }
            runCatching { tts?.shutdown() }
            main.removeCallbacksAndMessages(FOCUS_TOKEN)
            pending.clear()
        }
    }

    // --- internals ----------------------------------------------------------

    private fun speakNow(t: TextToSpeech, phrase: String) {
        val est = (phrase.length * 40).coerceIn(350, 1500)
        withFocus(est) {
            if (Build.VERSION.SDK_INT >= 21) {
                t.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "cue:${System.nanoTime()}")
            } else {
                @Suppress("DEPRECATION")
                t.speak(
                    phrase,
                    TextToSpeech.QUEUE_FLUSH,
                    hashMapOf(
                        TextToSpeech.Engine.KEY_PARAM_STREAM to (
                                if (voiceOnAlarmStream) AudioManager.STREAM_ALARM else AudioManager.STREAM_MUSIC
                                ).toString()
                    )
                )
            }
        }
    }

    private fun flushPending() {
        val t = tts ?: return
        while (pending.isNotEmpty()) speakNow(t, pending.removeFirst())
    }
}
