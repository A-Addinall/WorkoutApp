package com.example.safitness.data.repo

import com.example.safitness.core.SkillTestType
import com.example.safitness.core.SkillType
import com.example.safitness.data.dao.SkillLogDao
import com.example.safitness.data.entities.SkillLogEntity

class SkillLogRepository(
    private val dao: SkillLogDao
) {

    suspend fun insert(raw: SkillLogEntity): Long {
        validateSkillResult(raw)
        return dao.insert(raw)
    }

    suspend fun recent(
        skill: SkillType,
        testType: SkillTestType,
        limit: Int = 20
    ) = dao.recent(skill.name, testType.name, limit)

    /** Convenience: unbroken DU test. */
    suspend fun logDoubleUndersMaxReps(
        epochSeconds: Long,
        reps: Int,
        scaled: Boolean = false,
        notes: String? = null
    ): Long {
        require(reps > 0) { "reps must be > 0" }
        val entity = SkillLogEntity(
            date = epochSeconds,
            skill = SkillType.DOUBLE_UNDERS.name,
            testType = SkillTestType.MAX_REPS_UNBROKEN.name,
            reps = reps,
            scaled = scaled,
            notes = notes
        )
        return insert(entity)
    }

    // ------------------------
    // Validation
    // ------------------------

    private fun validateSkillResult(e: SkillLogEntity) {
        val test = when (e.testType) {
            SkillTestType.MAX_REPS_UNBROKEN.name -> SkillTestType.MAX_REPS_UNBROKEN
            SkillTestType.FOR_TIME_REPS.name -> SkillTestType.FOR_TIME_REPS
            SkillTestType.MAX_HOLD_SECONDS.name -> SkillTestType.MAX_HOLD_SECONDS
            SkillTestType.ATTEMPTS.name -> SkillTestType.ATTEMPTS
            else -> error("Unknown SkillTestType: ${e.testType}")
        }

        // Common integrity checks
        require(e.skill.isNotBlank()) { "skill must be set" }
        require(e.date > 0) { "date (epoch seconds) must be > 0" }

        when (test) {
            SkillTestType.MAX_REPS_UNBROKEN -> {
                require((e.reps ?: 0) > 0) { "MAX_REPS_UNBROKEN requires reps > 0" }
            }
            SkillTestType.FOR_TIME_REPS -> {
                require((e.timeSeconds ?: 0) > 0) { "FOR_TIME_REPS requires timeSeconds > 0" }
                require((e.targetReps ?: 0) > 0) { "FOR_TIME_REPS requires targetReps > 0" }
            }
            SkillTestType.MAX_HOLD_SECONDS -> {
                require((e.maxHoldSeconds ?: 0) > 0) { "MAX_HOLD_SECONDS requires maxHoldSeconds > 0" }
            }
            SkillTestType.ATTEMPTS -> {
                require((e.attempts ?: 0) > 0) { "ATTEMPTS requires attempts > 0" }
            }
        }

        // Disallow contradictory result combos for clarity (non-fatal, but helps data hygiene)
        val nonNullResults = listOf(e.reps, e.timeSeconds, e.maxHoldSeconds)
            .count { it != null }
        require(nonNullResults <= 2) {
            "Too many result fields set. Keep only the ones relevant to $test."
        }
    }
}
