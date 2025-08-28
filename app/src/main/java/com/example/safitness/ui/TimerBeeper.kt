package com.example.safitness.ui

import android.content.Context
import android.media.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Race-safe timer beeper that briefly ducks other audio.
 * Uses transient audio focus (MAY_DUCK) and louder tones.
 */
class TimerBeeper(context: Context) : AudioManager.OnAudioFocusChangeListener {
    private val released = AtomicBoolean(false)
    private val main = Handler(Looper.getMainLooper())
    private val am = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Louder than before (100). Keep pip on MUSIC so user volume still applies.
    private val pipTone  = ToneGenerator(AudioManager.STREAM_MUSIC, /*volume%*/ 100)
    // Make the end buzz harder to miss by using the alarm stream.
    private val buzzTone = ToneGenerator(AudioManager.STREAM_ALARM, /*volume%*/ 100)

    private val attrs: AudioAttributes? =
        if (Build.VERSION.SDK_INT >= 21)
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        else null

    private val focusReq: AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= 26)
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setOnAudioFocusChangeListener(this, main)
                .setWillPauseWhenDucked(false)
                .also { if (attrs != null) it.setAudioAttributes(attrs) }
                .build()
        else null

    override fun onAudioFocusChange(focusChange: Int) {
        // We don't need to react; other apps will duck themselves.
    }

    /** Acquire transient ducking focus around a short tone and release it after. */
    private fun withFocus(durationMs: Int, block: () -> Unit) {
        if (released.get()) return

        val granted = if (Build.VERSION.SDK_INT >= 26) {
            am.requestAudioFocus(focusReq!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

        // Play regardless; focus is just best-effort.
        runCatching(block)

        // Abandon focus shortly after the tone finishes (coalesced).
        main.removeCallbacksAndMessages(FOCUS_TOKEN)
        val whenMs = SystemClock.uptimeMillis() + durationMs + 50
        main.postAtTime({ abandonFocus() }, FOCUS_TOKEN, whenMs)
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= 26) {
            focusReq?.let { am.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(this)
        }
    }

    /** Short pip for countdown (5..1 and 3..1 cues). */
    fun countdownPip() = withFocus(150) {
        pipTone.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    }

    /** Minute tick for EMOM (and other periodic cues). */
    fun minuteTick() = withFocus(150) {
        pipTone.startTone(ToneGenerator.TONE_PROP_ACK, 150)
    }

    /** Longer end tone when time expires / workout completes. */
    fun finalBuzz() = withFocus(600) {
        buzzTone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 600)
    }

    fun release() {
        if (released.compareAndSet(false, true)) {
            abandonFocus()
            runCatching { pipTone.release() }
            runCatching { buzzTone.release() }
            main.removeCallbacksAndMessages(FOCUS_TOKEN)
        }
    }

    private companion object { val FOCUS_TOKEN = Any() }
}
