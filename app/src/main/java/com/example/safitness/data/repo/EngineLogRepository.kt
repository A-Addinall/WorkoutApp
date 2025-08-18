package com.example.safitness.data.repo

import com.example.safitness.core.EngineCalculator
import com.example.safitness.core.EngineIntent
import com.example.safitness.core.EngineMode
import com.example.safitness.data.dao.EngineLogDao
import com.example.safitness.data.entities.EngineLogEntity

class EngineLogRepository(
    private val dao: EngineLogDao
) {

    suspend fun insert(raw: EngineLogEntity): Long {
        validateEngineProgramTargets(raw)
        validateEngineResultForIntent(raw)
        val withPace = maybeWithComputedPace(raw)
        return dao.insert(withPace)
    }

    suspend fun recent(mode: EngineMode, limit: Int = 20) =
        dao.recent(mode.name, limit)

    suspend fun recentByIntent(
        mode: EngineMode,
        intent: EngineIntent,
        limit: Int = 20
    ) = dao.recentByIntent(mode.name, intent.name, limit)

    /** Convenience for a fixed-distance RUN time trial (e.g., 5k). */
    suspend fun logRunForTime(
        epochSeconds: Long,
        distanceMeters: Int,
        timeSeconds: Int,
        scaled: Boolean = false,
        notes: String? = null
    ): Long {
        require(distanceMeters > 0) { "distanceMeters must be > 0" }
        require(timeSeconds > 0) { "timeSeconds must be > 0" }

        val entity = EngineLogEntity(
            date = epochSeconds,
            mode = EngineMode.RUN.name,
            intent = EngineIntent.FOR_TIME.name,
            programDistanceMeters = distanceMeters,
            resultTimeSeconds = timeSeconds,
            scaled = scaled,
            notes = notes
        )
        return insert(entity)
    }

    // ------------------------
    // Validation / helpers
    // ------------------------

    /** Exactly one of the program targets must be set. */
    private fun validateEngineProgramTargets(e: EngineLogEntity) {
        val setCount = listOf(
            e.programDistanceMeters,
            e.programDurationSeconds,
            e.programTargetCalories
        ).count { it != null }
        require(setCount == 1) {
            "Exactly one program target is required: " +
                    "programDistanceMeters XOR programDurationSeconds XOR programTargetCalories."
        }

        e.programDistanceMeters?.let { require(it > 0) { "programDistanceMeters must be > 0" } }
        e.programDurationSeconds?.let { require(it > 0) { "programDurationSeconds must be > 0" } }
        e.programTargetCalories?.let { require(it > 0) { "programTargetCalories must be > 0" } }
    }

    /** Result requirements depend on intent. */
    private fun validateEngineResultForIntent(e: EngineLogEntity) {
        val intent = when (e.intent) {
            EngineIntent.FOR_TIME.name -> EngineIntent.FOR_TIME
            EngineIntent.FOR_DISTANCE.name -> EngineIntent.FOR_DISTANCE
            EngineIntent.FOR_CALORIES.name -> EngineIntent.FOR_CALORIES
            else -> error("Unknown EngineIntent: ${e.intent}")
        }

        when (intent) {
            EngineIntent.FOR_TIME -> {
                require(e.programDistanceMeters != null) {
                    "FOR_TIME requires programDistanceMeters to be set."
                }
                require((e.resultTimeSeconds ?: 0) > 0) {
                    "FOR_TIME requires resultTimeSeconds > 0."
                }
            }
            EngineIntent.FOR_DISTANCE -> {
                require(e.programDurationSeconds != null) {
                    "FOR_DISTANCE requires programDurationSeconds to be set."
                }
                require((e.resultDistanceMeters ?: 0) > 0) {
                    "FOR_DISTANCE requires resultDistanceMeters > 0."
                }
            }
            EngineIntent.FOR_CALORIES -> {
                require(e.programDurationSeconds != null) {
                    "FOR_CALORIES requires programDurationSeconds to be set."
                }
                require((e.resultCalories ?: 0) > 0) {
                    "FOR_CALORIES requires resultCalories > 0."
                }
            }
        }

        // Basic non-negative checks on results if present
        e.resultTimeSeconds?.let { require(it > 0) { "resultTimeSeconds must be > 0" } }
        e.resultDistanceMeters?.let { require(it > 0) { "resultDistanceMeters must be > 0" } }
        e.resultCalories?.let { require(it > 0) { "resultCalories must be > 0" } }
    }

    /** Compute/stash a pace value when it is clearly defined. */
    private fun maybeWithComputedPace(e: EngineLogEntity): EngineLogEntity {
        // Only compute RUN pace for fixed-distance efforts where we have distance + time.
        if (e.mode == EngineMode.RUN.name &&
            e.intent == EngineIntent.FOR_TIME.name &&
            e.programDistanceMeters != null &&
            e.resultTimeSeconds != null
        ) {
            val pace = EngineCalculator.paceSecondsPerKm(e.programDistanceMeters, e.resultTimeSeconds)
            if (pace != e.pace) {
                return e.copy(pace = pace)
            }
        }
        return e
    }
}
