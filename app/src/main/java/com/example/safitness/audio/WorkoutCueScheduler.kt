// WorkoutCueScheduler.kt
package com.example.safitness.audio

import kotlinx.coroutines.*
import kotlin.math.max

class WorkoutCueScheduler(
    private val scope: CoroutineScope,
    private val cue: CuePlayer
) {
    private var jobs = mutableListOf<Job>()

    /** Schedule cues for a one-shot workout timer of [totalMs] (e.g., AMRAP or time-cap). */
    fun scheduleOneShot(totalMs: Long, voice: Boolean = true) {
        cancel()
        // Halfway
        jobs += scope.launch { delay(totalMs / 2); if (voice) cue.say("Halfway") else cue.pip() }
        // 3 minutes remaining (if long enough)
        if (totalMs >= 3 * 60_000L) jobs += scope.launch {
            delay(max(0, totalMs - 3 * 60_000L)); cue.say("Three minutes remaining")
        }
        // 1 minute remaining
        if (totalMs >= 60_000L) jobs += scope.launch {
            delay(max(0, totalMs - 60_000L)); cue.say("One minute remaining")
        }
        // Last 10 seconds: quick voice then beeps (to avoid chattiness)
        if (totalMs >= 10_000L) jobs += scope.launch {
            delay(max(0, totalMs - 10_000L))
            cue.say("Ten seconds")
            // short beeps for 5..1
            repeat(5) {
                delay(1000L); cue.pip()
            }
            delay(1000L); cue.finalBuzz()
        }
    }

    /** For EMOM/intervals: fire at the half of each round. */
// in WorkoutCueScheduler.kt

    /** For EMOM/intervals: cue at end of work segment each round, optionally say "Rest". */
    fun scheduleEveryRound(
        roundMs: Long,
        totalRounds: Int,
        workMs: Long,                 // e.g., 40_000L for 40 s
        voice: Boolean = true,
        sayRest: Boolean = true,      // say "Rest" instead of generic beep
        finalCountdownSec: Int = 3    // short end-of-work beeps before rest
    ) {
        cancel()

        // guardrails
        val clampedWork = workMs.coerceIn(1_000L, (roundMs - 1_000L).coerceAtLeast(1_000L))

        repeat(totalRounds) { round ->
            val start = round * roundMs
            jobs += scope.launch {
                delay(start + clampedWork)

                // optional brief countdown ending the work segment
                if (finalCountdownSec > 0) {
                    repeat(finalCountdownSec) { delay(250L); cue.pip() } // quick staccato
                }

                if (voice && sayRest) cue.say("Rest") else cue.pip()
            }
        }
    }

    fun cancel() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }
}
