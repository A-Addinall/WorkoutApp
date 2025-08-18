package com.example.safitness.data.repo

import com.example.safitness.core.EngineIntent
import com.example.safitness.data.dao.EngineLogDao
import com.example.safitness.data.entities.EngineLogEntity

class EngineLogRepository(private val dao: EngineLogDao) {

    suspend fun log(entry: EngineLogEntity): Long {
        validate(entry)
        return dao.insert(entry)
    }

    suspend fun recent(limit: Int = 50) = dao.recent(limit)

    private fun validate(e: EngineLogEntity) {
        // Basic fields
        require(e.mode.isNotBlank()) { "Engine mode is required." }
        require(e.intent.isNotBlank()) { "Engine intent is required." }

        // Program targets
        when (EngineIntent.valueOf(e.intent)) {
            EngineIntent.FOR_TIME -> {
                require((e.programDistanceMeters ?: 0) > 0) { "Program distance (m) required for FOR_TIME." }
                require((e.resultTimeSeconds ?: 0) > 0) { "Result time (s) required for FOR_TIME." }
                require(e.resultDistanceMeters == null && e.resultCalories == null) { "Only resultTimeSeconds must be set for FOR_TIME." }
            }
            EngineIntent.FOR_DISTANCE -> {
                require((e.programDurationSeconds ?: 0) > 0) { "Program duration (s) required for FOR_DISTANCE." }
                require((e.resultDistanceMeters ?: 0) > 0) { "Result distance (m) required for FOR_DISTANCE." }
                require(e.resultTimeSeconds == null && e.resultCalories == null) { "Only resultDistanceMeters must be set for FOR_DISTANCE." }
            }
            EngineIntent.FOR_CALORIES -> {
                require((e.programDurationSeconds ?: 0) > 0) { "Program duration (s) required for FOR_CALORIES." }
                require((e.resultCalories ?: 0) > 0) { "Result calories required for FOR_CALORIES." }
                require(e.resultTimeSeconds == null && e.resultDistanceMeters == null) { "Only resultCalories must be set for FOR_CALORIES." }
            }
        }
    }
}
