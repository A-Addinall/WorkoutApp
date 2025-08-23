package com.example.safitness.domain.planner

import com.example.safitness.core.Equipment
import com.example.safitness.core.MovementPattern
import com.example.safitness.core.MuscleGroup
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.dao.LibraryDao
import com.example.safitness.data.entities.Exercise
import kotlin.math.abs

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

    /**
     * Suggest up to [maxItems] strength movements for a given focus.
     * We try to pick a main lift, then accessories with different patterns.
     * Variety is made deterministic with [epochDay] rotation.
     */
    suspend fun suggestFor(
        focus: WorkoutType,
        availableEq: List<Equipment>,
        maxItems: Int = 4,
        epochDay: Long? = null
    ): List<Suggestion> {
        val eqPool = if (availableEq.isEmpty())
            listOf(
                Equipment.BARBELL, Equipment.DUMBBELL, Equipment.BODYWEIGHT,
                Equipment.CABLE, Equipment.KETTLEBELL
            )
        else
            availableEq

        val usedIds = mutableSetOf<Long>()

        // ---- helpers --------------------------------------------------------

        suspend fun candidateList(
            movement: MovementPattern?,
            primary: List<MuscleGroup>
        ): List<Exercise> {
            // Preferred: use metadata
            val list = libraryDao.filterExercises(
                movement = movement,
                muscles = primary,
                equipment = eqPool,
                musclesEmpty = primary.isEmpty().toInt(),
                equipmentEmpty = eqPool.isEmpty().toInt()
            )
            if (list.isNotEmpty()) return list

            // Fallback: equipment-only pool
            return libraryDao.filterExercises(
                movement = null,
                muscles = emptyList(),
                equipment = eqPool,
                musclesEmpty = 1,
                equipmentEmpty = eqPool.isEmpty().toInt()
            )
        }

        fun <T> rotate(xs: List<T>, seed: Int): List<T> {
            if (xs.isEmpty()) return xs
            val off = abs(seed) % xs.size
            return if (off == 0) xs else xs.drop(off) + xs.take(off)
        }

        suspend fun pickUnique(
            movement: MovementPattern?,
            primary: List<MuscleGroup>
        ): Exercise? {
            val base = candidateList(movement, primary)
            if (base.isEmpty()) return null

            val seed = abs((epochDay ?: 0L).toInt())
            val rot = rotate(base, seed)

            // first unused in rotated order
            rot.firstOrNull { it.id !in usedIds }?.let { return it }

            // try a sensible alternate movement (also rotated)
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
                val alt = rotate(candidateList(altMovement, primary), seed)
                    .firstOrNull { it.id !in usedIds }
                if (alt != null) return alt
            }

            // last resort: first of rotated
            return rot.first()
        }

        fun makeSuggestion(
            sets: Int,
            reps: Int,
            ex: Exercise
        ) = Suggestion(exercise = ex, targetSets = sets, repsMin = reps, repsMax = reps)

        // ---- focus → blueprint ---------------------------------------------

        data class Blueprint(
            val main: Pair<MovementPattern?, List<MuscleGroup>>,
            val accessories: List<Pair<MovementPattern?, List<MuscleGroup>>>,
            val mainSets: Int,
            val mainReps: Int,
            val accSets: Int,
            val accReps: Int
        )

        val bp: Blueprint = when (focus) {
            WorkoutType.PUSH -> Blueprint(
                main = MovementPattern.HORIZONTAL_PUSH to listOf(MuscleGroup.CHEST, MuscleGroup.DELTS_ANT, MuscleGroup.TRICEPS),
                accessories = listOf(
                    MovementPattern.VERTICAL_PUSH to listOf(MuscleGroup.DELTS_ANT, MuscleGroup.TRICEPS),
                    MovementPattern.HORIZONTAL_PULL to listOf(MuscleGroup.BACK, MuscleGroup.BICEPS)
                ),
                mainSets = 5, mainReps = 5, accSets = 3, accReps = 8
            )

            WorkoutType.PULL -> Blueprint(
                main = MovementPattern.HORIZONTAL_PULL to listOf(MuscleGroup.BACK, MuscleGroup.BICEPS),
                accessories = listOf(
                    MovementPattern.VERTICAL_PULL to listOf(MuscleGroup.BACK, MuscleGroup.BICEPS),
                    MovementPattern.HORIZONTAL_PUSH to listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS)
                ),
                mainSets = 5, mainReps = 5, accSets = 3, accReps = 8
            )

            WorkoutType.LEGS_CORE -> Blueprint(
                main = MovementPattern.SQUAT to listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES),
                accessories = listOf(
                    MovementPattern.HINGE to listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
                    null to listOf(MuscleGroup.CORE)
                ),
                mainSets = 5, mainReps = 5, accSets = 3, accReps = 10
            )

            WorkoutType.FULL -> Blueprint(
                main = MovementPattern.HINGE to listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.BACK),
                accessories = listOf(
                    MovementPattern.HORIZONTAL_PUSH to listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS),
                    MovementPattern.VERTICAL_PULL to listOf(MuscleGroup.BACK, MuscleGroup.BICEPS)
                ),
                mainSets = 4, mainReps = 6, accSets = 3, accReps = 10
            )
        }

        // ---- build picks ----------------------------------------------------

        val out = mutableListOf<Suggestion>()

        // main
        pickUnique(bp.main.first, bp.main.second)?.let { ex ->
            usedIds += ex.id
            out += makeSuggestion(bp.mainSets, bp.mainReps, ex)
        }

        // accessories
        for (acc in bp.accessories) {
            if (out.size >= maxItems) break
            val ex = pickUnique(acc.first, acc.second) ?: continue
            if (ex.id in usedIds) continue
            usedIds += ex.id
            out += makeSuggestion(bp.accSets, bp.accReps, ex)
        }

        // if still short, fill from broad equipment pool (deterministically rotated)
        if (out.size < maxItems) {
            val fillPool = libraryDao.filterExercises(
                movement = null,
                muscles = emptyList(),
                equipment = eqPool,
                musclesEmpty = 1,
                equipmentEmpty = eqPool.isEmpty().toInt()
            ).filter { it.id !in usedIds }

            if (fillPool.isNotEmpty()) {
                val seed = abs((epochDay ?: 0L).toInt())
                val rotated = rotate(fillPool, seed)
                for (ex in rotated) {
                    if (out.size >= maxItems) break
                    if (ex.id in usedIds) continue
                    usedIds += ex.id
                    out += makeSuggestion(bp.accSets, bp.accReps, ex)
                }
            }
        }

        return out.take(maxItems)
    }
}
