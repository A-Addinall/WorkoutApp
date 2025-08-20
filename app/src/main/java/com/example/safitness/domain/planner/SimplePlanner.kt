package com.example.safitness.domain.planner

import com.example.safitness.core.Equipment
import com.example.safitness.core.MovementPattern
import com.example.safitness.core.MuscleGroup
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.dao.LibraryDao
import com.example.safitness.data.entities.Exercise

data class Suggestion(
    val exercise: Exercise,
    val targetSets: Int,
    val repsMin: Int,
    val repsMax: Int
)

class SimplePlanner(
    private val libraryDao: LibraryDao
) {
    private fun Boolean.toInt() = if (this) 1 else 0

    suspend fun suggestFor(
        focus: WorkoutType,
        availableEq: List<Equipment>,
        maxItems: Int = 4
    ): List<Suggestion> {
        val eqPool = if (availableEq.isEmpty()) {
            listOf(Equipment.BARBELL, Equipment.DUMBBELL, Equipment.BODYWEIGHT, Equipment.CABLE, Equipment.KETTLEBELL)
        } else availableEq

        val usedIds = mutableSetOf<Long>()

        suspend fun candidateList(
            movement: MovementPattern?,
            primary: List<MuscleGroup>
        ): List<Exercise> {
            // Prefer metadata filter
            val list = libraryDao.filterExercises(
                movement = movement,
                muscles = primary,
                equipment = eqPool,
                musclesEmpty = primary.isEmpty().toInt(),
                equipmentEmpty = eqPool.isEmpty().toInt()
            )
            if (list.isNotEmpty()) return list

            // Fallback: equipment‑only
            return libraryDao.filterExercises(
                movement = null,
                muscles = emptyList(),
                equipment = eqPool,
                musclesEmpty = 1,
                equipmentEmpty = eqPool.isEmpty().toInt()
            )
        }

        suspend fun pickUnique(
            movement: MovementPattern?,
            primary: List<MuscleGroup>
        ): Exercise? {
            val list = candidateList(movement, primary)
            // First unused; if none, allow a different movement to diversify
            val unused = list.firstOrNull { it.id !in usedIds }
            if (unused != null) return unused

            // Try a different movement that still fits the focus
            val altMovement = when (movement) {
                MovementPattern.SQUAT -> MovementPattern.LUNGE
                MovementPattern.LUNGE -> MovementPattern.SQUAT
                MovementPattern.HORIZONTAL_PUSH -> MovementPattern.VERTICAL_PUSH
                MovementPattern.VERTICAL_PUSH -> MovementPattern.HORIZONTAL_PUSH
                MovementPattern.HORIZONTAL_PULL -> MovementPattern.VERTICAL_PULL
                MovementPattern.VERTICAL_PULL -> MovementPattern.HORIZONTAL_PULL
                MovementPattern.HINGE -> MovementPattern.SQUAT
                else -> null
            }
            if (altMovement != null) {
                val alt = candidateList(altMovement, primary).firstOrNull { it.id !in usedIds }
                if (alt != null) return alt
            }

            // As a last resort, take the first (even if duplicate) so we always return something
            return list.firstOrNull()
        }

        fun makeSuggestion(index: Int, ex: Exercise) = when (index) {
            0 -> Suggestion(ex, 4, 5, 8)    // main
            1 -> Suggestion(ex, 3, 8, 12)   // secondary
            else -> Suggestion(ex, 3, 10, 15)
        }

        val picks = when (focus) {
            WorkoutType.PUSH -> listOf(
                MovementPattern.HORIZONTAL_PUSH to listOf(MuscleGroup.CHEST),
                MovementPattern.VERTICAL_PUSH to listOf(MuscleGroup.DELTS_MED, MuscleGroup.DELTS_ANT),
                null to listOf(MuscleGroup.TRICEPS)
            )

            WorkoutType.PULL -> listOf(
                MovementPattern.VERTICAL_PULL to listOf(MuscleGroup.LATS),
                MovementPattern.HORIZONTAL_PULL to listOf(MuscleGroup.UPPER_BACK, MuscleGroup.LATS),
                null to listOf(MuscleGroup.BICEPS)
            )

            WorkoutType.LEGS_CORE -> listOf(
                MovementPattern.SQUAT to listOf(MuscleGroup.QUADS),
                MovementPattern.HINGE to listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
                null to listOf(MuscleGroup.ABS, MuscleGroup.OBLIQUES)
            )

            else -> emptyList()
        }

        val suggestions = mutableListOf<Suggestion>()
        for ((idx, pair) in picks.withIndex()) {
            val (movement, muscles) = pair
            val ex = pickUnique(movement, muscles) ?: continue
            usedIds += ex.id
            suggestions += makeSuggestion(idx, ex)
            if (suggestions.size >= maxItems) break
        }

        return suggestions
    }
}
