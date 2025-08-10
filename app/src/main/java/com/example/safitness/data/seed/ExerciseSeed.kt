package com.example.safitness.data.seed

import com.example.safitness.core.Equipment
import com.example.safitness.core.Modality
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.entities.Exercise
import com.example.safitness.data.db.AppDatabase // adjust if your DB package differs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ExerciseSeed {

    // One canonical list. Use named args so constructor order can’t bite us.
    val DEFAULT_EXERCISES = listOf(
        // PUSH (Strength)
        Exercise(
            name = "Barbell Bench Press",
            workoutType = WorkoutType.PUSH,
            primaryEquipment = Equipment.BARBELL,
            modality = Modality.STRENGTH,
            isUnilateral = false
        ),
        Exercise(
            name = "Dumbbell Incline Press",
            workoutType = WorkoutType.PUSH,
            primaryEquipment = Equipment.DUMBBELL,
            modality = Modality.STRENGTH,
            isUnilateral = false
        ),
        Exercise(
            name = "Overhead Press",
            workoutType = WorkoutType.PUSH,
            primaryEquipment = Equipment.BARBELL,
            modality = Modality.STRENGTH,
            isUnilateral = false
        ),

        // PULL (Strength)
        Exercise(
            name = "Barbell Bent-Over Row",
            workoutType = WorkoutType.PULL,
            primaryEquipment = Equipment.BARBELL,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "One-Arm DB Row",
            workoutType = WorkoutType.PULL,
            primaryEquipment = Equipment.DUMBBELL,
            modality = Modality.STRENGTH,
            isUnilateral = true
        ),
        Exercise(
            name = "Lat Pulldown",
            workoutType = WorkoutType.PULL,
            primaryEquipment = Equipment.CABLE,
            modality = Modality.STRENGTH
        ),

        // LEGS+CORE (Strength)
        Exercise(
            name = "Back Squat",
            workoutType = WorkoutType.LEGS_CORE,
            primaryEquipment = Equipment.BARBELL,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Romanian Deadlift",
            workoutType = WorkoutType.LEGS_CORE,
            primaryEquipment = Equipment.BARBELL,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Walking Lunge",
            workoutType = WorkoutType.LEGS_CORE,
            primaryEquipment = Equipment.DUMBBELL,
            modality = Modality.STRENGTH,
            isUnilateral = true
        ),
        Exercise(
            name = "Plank",
            workoutType = WorkoutType.LEGS_CORE,
            primaryEquipment = Equipment.BODYWEIGHT,
            modality = Modality.STRENGTH
        ),

        // METCON (time/reps circuits)
        Exercise(
            name = "Burpees",
            workoutType = WorkoutType.METCON,
            primaryEquipment = Equipment.BODYWEIGHT,
            modality = Modality.METCON
        ),
        Exercise(
            name = "Kettlebell Swings",
            workoutType = WorkoutType.METCON,
            primaryEquipment = Equipment.KETTLEBELL,
            modality = Modality.METCON
        ),
        Exercise(
            name = "Row Erg",
            workoutType = WorkoutType.METCON,
            primaryEquipment = Equipment.MACHINE,
            modality = Modality.METCON
        )
    )

    /**
     * Inserts defaults only if the table is empty.
     */
    suspend fun seedIfEmpty(db: AppDatabase) = withContext(Dispatchers.IO) {
        val dao = db.exerciseDao()
        if (dao.count() == 0) {
            dao.insertAll(DEFAULT_EXERCISES)
        }
    }
}
