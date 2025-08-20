package com.example.safitness.data.repo

import com.example.safitness.core.Modality
import com.example.safitness.ml.*
import com.example.safitness.core.WorkoutFocus
import com.example.safitness.data.dao.PlanDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MLPlannerRepository(
    private val planDao: PlanDao,
    private val ml: MLService
) {
    suspend fun generateAndPersist(dateIso: String, focus: WorkoutFocus, modality: Modality, user: UserContext) =
        withContext(Dispatchers.IO) {
            val resp = ml.generate(GenerateRequest(dateIso, focus, modality, user))

            // Persist into your existing day items so the current UI renders them.
            // Adjust to your PlanDao insert signatures.
            if (resp.strength.isNotEmpty()) {
                // Example: persist 1..n strength items into "Day 1" (or map by date if you switch to date-based)
                resp.strength.forEachIndexed { order, s ->
                    // TODO: replace with your actual insert (e.g., DayItemEntity)
                    // planDao.insertDayStrengthItem(dayIndex = 1, exerciseId = s.exerciseId, targetReps = s.repsMax, sortOrder = order)
                }
            }

            resp.metcon?.let { m ->
                // TODO: insert a metcon row + its text lines using your current MetconDao/PlanDao
            }

            resp
        }
}
