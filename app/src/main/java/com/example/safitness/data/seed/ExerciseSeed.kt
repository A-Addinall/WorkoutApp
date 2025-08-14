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
        // — Added common PUSH exercises —
        Exercise(
            name = "Push-Up",
            workoutType = WorkoutType.PUSH,
            primaryEquipment = Equipment.BODYWEIGHT,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Dip",
            workoutType = WorkoutType.PUSH,
            primaryEquipment = Equipment.BODYWEIGHT,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Dumbbell Bench Press",
            workoutType = WorkoutType.PUSH,
            primaryEquipment = Equipment.DUMBBELL,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Dumbbell Flye",
            workoutType = WorkoutType.PUSH,
            primaryEquipment = Equipment.DUMBBELL,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Cable Flye",
            workoutType = WorkoutType.PUSH,
            primaryEquipment = Equipment.CABLE,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Seated Dumbbell Shoulder Press",
            workoutType = WorkoutType.PUSH,
            primaryEquipment = Equipment.DUMBBELL,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Arnold Press",
            workoutType = WorkoutType.PUSH,
            primaryEquipment = Equipment.DUMBBELL,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Lateral Raise",
            workoutType = WorkoutType.PUSH,
            primaryEquipment = Equipment.DUMBBELL,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Triceps Pushdown",
            workoutType = WorkoutType.PUSH,
            primaryEquipment = Equipment.CABLE,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Overhead Triceps Extension (Cable)",
            workoutType = WorkoutType.PUSH,
            primaryEquipment = Equipment.CABLE,
            modality = Modality.STRENGTH
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
        // — Added common PULL exercises —
        Exercise(
            name = "Pull-Up",
            workoutType = WorkoutType.PULL,
            primaryEquipment = Equipment.BODYWEIGHT,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Chin-Up",
            workoutType = WorkoutType.PULL,
            primaryEquipment = Equipment.BODYWEIGHT,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Seated Cable Row",
            workoutType = WorkoutType.PULL,
            primaryEquipment = Equipment.CABLE,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Face Pull",
            workoutType = WorkoutType.PULL,
            primaryEquipment = Equipment.CABLE,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Straight-Arm Pulldown",
            workoutType = WorkoutType.PULL,
            primaryEquipment = Equipment.CABLE,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Rear Delt Fly (Dumbbell)",
            workoutType = WorkoutType.PULL,
            primaryEquipment = Equipment.DUMBBELL,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Dumbbell Biceps Curl",
            workoutType = WorkoutType.PULL,
            primaryEquipment = Equipment.DUMBBELL,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Hammer Curl",
            workoutType = WorkoutType.PULL,
            primaryEquipment = Equipment.DUMBBELL,
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
        // — Added common LEGS + CORE exercises —
        Exercise(
            name = "Deadlift",
            workoutType = WorkoutType.LEGS_CORE,
            primaryEquipment = Equipment.BARBELL,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Front Squat",
            workoutType = WorkoutType.LEGS_CORE,
            primaryEquipment = Equipment.BARBELL,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Goblet Squat",
            workoutType = WorkoutType.LEGS_CORE,
            primaryEquipment = Equipment.DUMBBELL,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Bulgarian Split Squat",
            workoutType = WorkoutType.LEGS_CORE,
            primaryEquipment = Equipment.DUMBBELL,
            modality = Modality.STRENGTH,
            isUnilateral = true
        ),
        Exercise(
            name = "Step-Up",
            workoutType = WorkoutType.LEGS_CORE,
            primaryEquipment = Equipment.DUMBBELL,
            modality = Modality.STRENGTH,
            isUnilateral = true
        ),
        Exercise(
            name = "Hip Thrust",
            workoutType = WorkoutType.LEGS_CORE,
            primaryEquipment = Equipment.BARBELL,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Glute Bridge",
            workoutType = WorkoutType.LEGS_CORE,
            primaryEquipment = Equipment.BODYWEIGHT,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Standing Calf Raise",
            workoutType = WorkoutType.LEGS_CORE,
            primaryEquipment = Equipment.DUMBBELL,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Crunch",
            workoutType = WorkoutType.LEGS_CORE,
            primaryEquipment = Equipment.BODYWEIGHT,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Hanging Leg Raise",
            workoutType = WorkoutType.LEGS_CORE,
            primaryEquipment = Equipment.BODYWEIGHT,
            modality = Modality.STRENGTH
        ),
        Exercise(
            name = "Side Plank",
            workoutType = WorkoutType.LEGS_CORE,
            primaryEquipment = Equipment.BODYWEIGHT,
            modality = Modality.STRENGTH,
            isUnilateral = true
        ),
        Exercise(
            name = "Russian Twist",
            workoutType = WorkoutType.LEGS_CORE,
            primaryEquipment = Equipment.DUMBBELL,
            modality = Modality.STRENGTH
        )
    )

    suspend fun seedOrUpdate(db: AppDatabase) = withContext(Dispatchers.IO) {
        val dao = db.exerciseDao()
        dao.insertAllIgnore(DEFAULT_EXERCISES) // safe to call on every app start
    }
}
