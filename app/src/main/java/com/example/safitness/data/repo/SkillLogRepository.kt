package com.example.safitness.data.repo

import com.example.safitness.core.SkillTestType
import com.example.safitness.data.dao.SkillLogDao
import com.example.safitness.data.entities.SkillLogEntity

class SkillLogRepository(private val dao: SkillLogDao) {

    suspend fun log(entry: SkillLogEntity): Long {
        validate(entry)
        return dao.insert(entry)
    }

    suspend fun recent(limit: Int = 50) = dao.recent(limit)

    private fun validate(s: SkillLogEntity) {
        require(s.skill.isNotBlank()) { "Skill is required." }
        require(s.testType.isNotBlank()) { "Test type is required." }

        when (SkillTestType.valueOf(s.testType)) {
            SkillTestType.ATTEMPTS -> {
                require((s.attemptsSuccess ?: 0) >= 0 && (s.attemptsFail ?: 0) >= 0) { "Attempts must be non-negative." }
                require((s.attemptsSuccess ?: 0) + (s.attemptsFail ?: 0) > 0) { "Record at least one attempt." }
                require(s.holdSeconds == null && s.reps == null) { "Only attempts fields should be set for ATTEMPTS." }
            }
            SkillTestType.MAX_HOLD_SECONDS -> {
                require((s.holdSeconds ?: 0) > 0) { "Hold seconds must be > 0." }
                require(s.attemptsSuccess == null && s.attemptsFail == null && s.reps == null) { "Only holdSeconds should be set for MAX_HOLD_SECONDS." }
            }
            SkillTestType.FOR_TIME_REPS, SkillTestType.MAX_REPS_UNBROKEN -> {
                require((s.reps ?: 0) > 0) { "Reps must be > 0." }
                require(s.attemptsSuccess == null && s.attemptsFail == null && s.holdSeconds == null) { "Only reps should be set for this test type." }
            }
        }
    }
}
