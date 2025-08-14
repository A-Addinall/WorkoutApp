package com.example.safitness.ui

import android.media.AudioManager
import android.media.ToneGenerator
import java.util.concurrent.atomic.AtomicBoolean

/** Lightweight, race-safe beeper for timers. Uses STREAM_MUSIC so system volume applies. */
class TimerBeeper {
    private val released = AtomicBoolean(false)
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, /*volume%*/ 80)

    private fun play(toneType: Int, durationMs: Int) {
        if (released.get()) return
        runCatching { tone.startTone(toneType, durationMs) }
        // If startTone throws because underlying native object was released, we swallow it.
    }

    /** Short pip for countdown (5..1 and 3..1 cues). */
    fun countdownPip() = play(ToneGenerator.TONE_PROP_BEEP, 120)

    /** Minute tick for EMOM (and other periodic cues). */
    fun minuteTick() = play(ToneGenerator.TONE_PROP_ACK, 120)

    /** Longer end tone when time expires / workout completes. */
    fun finalBuzz() = play(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 450)

    fun release() {
        if (released.compareAndSet(false, true)) {
            runCatching { tone.release() }
        }
    }
}
