package com.example.safitness.ml

import com.example.safitness.core.*
import com.example.safitness.data.dao.LibraryDao

/**
 * Minimal stub that just returns 2-3 sensible specs so you can wire UI/DB without waiting on a real model.
 * Replace with network client later.
 */
class LocalMLStub(private val libraryDao: LibraryDao): MLService {
    override suspend fun generate(req: GenerateRequest): GenerateResponse {
        // Extremely simple: pick 2-3 exercises by focus + available equipment (ignore metadata intricacies)
        val fallbackEq = if (req.user.availableEquipment.isEmpty())
            listOf(Equipment.DUMBBELL, Equipment.BARBELL, Equipment.BODYWEIGHT)
        else req.user.availableEquipment

        // Pull a few exercises by legacy type for now (works with your current DB/UI).
        val type = when (req.focus) {
            WorkoutFocus.PUSH -> WorkoutType.PUSH
            WorkoutFocus.PULL -> WorkoutType.PULL
            WorkoutFocus.LEGS, WorkoutFocus.LOWER -> WorkoutType.LEGS_CORE
            WorkoutFocus.UPPER, WorkoutFocus.FULL_BODY -> WorkoutType.PUSH // simple bias; fine for stub
            WorkoutFocus.CORE, WorkoutFocus.CONDITIONING -> WorkoutType.LEGS_CORE
        }
        val pool = libraryDao.getExercises(type) // your existing DAO; no primaryEquipment arg
        val picks = pool.take(3)

        val strength = picks.mapIndexed { idx, e ->
            ExerciseSpec(
                exerciseId = e.id,
                sets = if (idx == 0) 4 else 3,
                repsMin = if (idx == 0) 5 else 8,
                repsMax = if (idx == 0) 8 else 12,
                intensityType = null,
                intensityValue = null
            )
        }

        val metcon = if (req.modality == Modality.METCON) {
            GenerateResponse(
                strength = emptyList(),
                metcon = MetconSpec(
                    blockType = BlockType.AMRAP,
                    durationSec = 12 * 60,
                    intervalSec = null,
                    components = listOf(
                        MetconComponentSpec("10 Push-ups", 10, MovementPattern.HORIZONTAL_PUSH, listOf(Equipment.BODYWEIGHT)),
                        MetconComponentSpec("15 Air Squats", 15, MovementPattern.SQUAT, listOf(Equipment.BODYWEIGHT))
                    )
                )
            ).metcon
        } else null

        return GenerateResponse(strength = strength, metcon = metcon)
    }
}
