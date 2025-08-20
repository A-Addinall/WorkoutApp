package com.example.safitness.data.repo

import com.example.safitness.core.Equipment
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.dao.LibraryDao
import com.example.safitness.data.dao.PlanDao
import com.example.safitness.domain.planner.SimplePlanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlannerRepository(
    private val planDao: PlanDao,
    private val libraryDao: LibraryDao
) {
    private val planner = SimplePlanner(libraryDao)

    suspend fun generateSuggestionsForDay(
        dayIndex: Int,
        focus: WorkoutType,
        availableEq: List<Equipment>
    ): List<com.example.safitness.domain.planner.Suggestion> = withContext(Dispatchers.IO) {
        planner.suggestFor(focus, availableEq)
    }

    /**
     * Persist suggestions into the EXISTING strength "day list" (so current UI shows them).
     * Adjust the insert to match your PlanDao method signatures.
     */
    suspend fun persistSuggestionsToDay(
        dayIndex: Int,
        suggestions: List<com.example.safitness.domain.planner.Suggestion>
    ) = withContext(Dispatchers.IO) {
        // TODO: adapt these to your actual PlanDao add/insert APIs.
        // Common pattern: insert DayItem rows for the given day.
        // Example (pseudo):
        // suggestions.forEachIndexed { order, s ->
        //     planDao.insertDayItem(
        //         day = dayIndex,
        //         exerciseId = s.exercise.id,
        //         required = true,
        //         targetReps = s.repsMax, // or store as notes if you only have single reps
        //         sortOrder = order
        //     )
        // }
    }
}
