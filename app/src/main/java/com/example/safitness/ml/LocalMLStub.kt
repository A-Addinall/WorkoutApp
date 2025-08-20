package com.example.safitness.ml

import com.example.safitness.core.*
import com.example.safitness.data.dao.LibraryDao
import kotlinx.coroutines.flow.first

/**
 * Minimal stub that just returns 2-3 sensible specs so you can wire UI/DB without waiting on a real model.
 * Replace with a network client later.
 */
class LocalMLStub(
    private val libraryDao: LibraryDao
) : MLService {
    override suspend fun generate(req: GenerateRequest): GenerateResponse {
        val fallbackEq = if (req.user.availableEquipment.isEmpty())
            listOf(Equipment.DUMBBELL, Equipment.BARBELL, Equipment.BODYWEIGHT)
        else req.user.availableEquipment

        // Map focus to legacy WorkoutType for your existing DAO
        val type = when (req.focus) {
            WorkoutFocus.PUSH -> WorkoutType.PUSH
            WorkoutFocus.PULL -> WorkoutType.PULL
            WorkoutFocus.LEGS, WorkoutFocus.LOWER -> WorkoutType.LEGS_CORE
            WorkoutFocus.UPPER, WorkoutFocus.FULL_BODY -> WorkoutType.PUSH
            WorkoutFocus.CORE, WorkoutFocus.CONDITIONING -> WorkoutType.LEGS_CORE
        }

        // ✅ Collect the Flow into a List
        val pool = libraryDao.getExercises(
            type = type,
            eq = fallbackEq.firstOrNull()
        ).first()

        // Grab up to 3
        val picks = pool.take(3)

        val strength = picks.mapIndexed { idx, e ->
            val (sets, reps) = when (idx) {
                0 -> 4 to 5   // main lift
                1 -> 3 to 8   // secondary
                else -> 3 to 12 // accessory
            }
            ExerciseSpec(
                exerciseId = e.id,   // Exercise.id is a Long:contentReference[oaicite:3]{index=3}
                sets = sets,
                targetReps = reps,
                intensityType = null,
                intensityValue = null
            )
        }

        val metcon = if (req.modality == Modality.METCON) {
            MetconSpec(
                blockType = BlockType.AMRAP,
                durationSec = 12 * 60,
                intervalSec = null,
                components = listOf(
                    MetconComponentSpec("10 Push-ups", 10, MovementPattern.HORIZONTAL_PUSH, listOf(Equipment.BODYWEIGHT)),
                    MetconComponentSpec("15 Air Squats", 15, MovementPattern.SQUAT, listOf(Equipment.BODYWEIGHT))
                )
            )
        } else null

        return GenerateResponse(strength = strength, metcon = metcon)
    }
}
