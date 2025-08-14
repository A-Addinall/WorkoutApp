package com.example.safitness.ui

import android.media.AudioManager
import android.media.ToneGenerator

/** Lightweight beeper for timers. Uses STREAM_MUSIC so system volume applies. */
class TimerBeeper {
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, /*volume%*/ 80)

    /** Short pip for countdown (3,2,1) */
    fun countdownPip() {
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, /*ms*/ 120)
    }

    /** Minute tick for EMOM (and other periodic cues) */
    fun minuteTick() {
        tone.startTone(ToneGenerator.TONE_PROP_ACK, /*ms*/ 120)
    }

    /** Longer end tone when time expires / workout completes */
    fun finalBuzz() {
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, /*ms*/ 450)
    }

    fun release() = tone.release()
}
