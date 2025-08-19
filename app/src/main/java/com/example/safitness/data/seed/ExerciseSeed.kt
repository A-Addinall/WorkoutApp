package com.example.safitness.data.seed

import com.example.safitness.core.*
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.Exercise
import com.example.safitness.data.entities.ExerciseMuscle
import com.example.safitness.data.entities.ExerciseEquipment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ExerciseSeed {

    val DEFAULT_EXERCISES = listOf(
        Exercise(name = "Barbell Bench Press", workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.BARBELL, modality = Modality.STRENGTH, isUnilateral = false),
        Exercise(name = "Dumbbell Incline Press", workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.DUMBBELL, modality = Modality.STRENGTH, isUnilateral = false),
        Exercise(name = "Overhead Press", workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.BARBELL, modality = Modality.STRENGTH),
        Exercise(name = "Push-Up", workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.BODYWEIGHT, modality = Modality.STRENGTH),
        Exercise(name = "Dip", workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.BODYWEIGHT, modality = Modality.STRENGTH),
        Exercise(name = "Dumbbell Bench Press", workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.DUMBBELL, modality = Modality.STRENGTH),
        Exercise(name = "Dumbbell Flye", workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.DUMBBELL, modality = Modality.STRENGTH),
        Exercise(name = "Cable Flye", workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.CABLE, modality = Modality.STRENGTH),
        Exercise(name = "Seated Dumbbell Shoulder Press", workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.DUMBBELL, modality = Modality.STRENGTH),
        Exercise(name = "Arnold Press", workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.DUMBBELL, modality = Modality.STRENGTH),
        Exercise(name = "Lateral Raise", workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.DUMBBELL, modality = Modality.STRENGTH),
        Exercise(name = "Triceps Pushdown", workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.CABLE, modality = Modality.STRENGTH),
        Exercise(name = "Barbell Bent-Over Row", workoutType = WorkoutType.PULL, primaryEquipment = Equipment.BARBELL, modality = Modality.STRENGTH),
        Exercise(name = "One-Arm DB Row", workoutType = WorkoutType.PULL, primaryEquipment = Equipment.DUMBBELL, modality = Modality.STRENGTH, isUnilateral = true),
        Exercise(name = "Lat Pulldown", workoutType = WorkoutType.PULL, primaryEquipment = Equipment.CABLE, modality = Modality.STRENGTH),
        Exercise(name = "Pull-Up", workoutType = WorkoutType.PULL, primaryEquipment = Equipment.BODYWEIGHT, modality = Modality.STRENGTH),
        Exercise(name = "Chin-Up", workoutType = WorkoutType.PULL, primaryEquipment = Equipment.BODYWEIGHT, modality = Modality.STRENGTH),
        Exercise(name = "Face Pull", workoutType = WorkoutType.PULL, primaryEquipment = Equipment.CABLE, modality = Modality.STRENGTH),
        Exercise(name = "Back Squat", workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.BARBELL, modality = Modality.STRENGTH),
        Exercise(name = "Front Squat", workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.BARBELL, modality = Modality.STRENGTH),
        Exercise(name = "Deadlift", workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.BARBELL, modality = Modality.STRENGTH),
        Exercise(name = "Romanian Deadlift", workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.BARBELL, modality = Modality.STRENGTH),
        Exercise(name = "Goblet Squat", workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.DUMBBELL, modality = Modality.STRENGTH),
        Exercise(name = "Bulgarian Split Squat", workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.DUMBBELL, modality = Modality.STRENGTH, isUnilateral = true),
        Exercise(name = "Walking Lunge", workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.DUMBBELL, modality = Modality.STRENGTH, isUnilateral = true),
        Exercise(name = "Hip Thrust", workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.BARBELL, modality = Modality.STRENGTH),
        Exercise(name = "Standing Calf Raise", workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.DUMBBELL, modality = Modality.STRENGTH),
        Exercise(name = "Crunch", workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.BODYWEIGHT, modality = Modality.STRENGTH),
        Exercise(name = "Hanging Leg Raise", workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.BODYWEIGHT, modality = Modality.STRENGTH),
        Exercise(name = "Plank", workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.BODYWEIGHT, modality = Modality.STRENGTH),
        Exercise(name = "Russian Twist", workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.DUMBBELL, modality = Modality.STRENGTH)
    )

    suspend fun seedOrUpdate(db: AppDatabase) = withContext(Dispatchers.IO) {
        val exDao = db.exerciseDao()
        val metaDao = db.exerciseMetadataDao()
        exDao.insertAllIgnore(DEFAULT_EXERCISES)

        // Map names -> IDs
        val ids = DEFAULT_EXERCISES.mapNotNull { e ->
            exDao.getIdByName(e.name)?.let { id -> e.name to id }
        }.toMap()

        val muscles = mutableListOf<ExerciseMuscle>()
        val equipment = mutableListOf<ExerciseEquipment>()

        // Example muscle mapping (extend as needed)
        ids["Barbell Bench Press"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.CHEST, role = "PRIMARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.TRICEPS, role = "SECONDARY")
        }
        ids["Back Squat"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.QUADS, role = "PRIMARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.GLUTES, role = "SECONDARY")
        }
        ids["Deadlift"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.HAMSTRINGS, role = "PRIMARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.GLUTES, role = "PRIMARY")
        }

        // Example equipment flexibility
        ids["Goblet Squat"]?.let { id ->
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.DUMBBELL)
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.KETTLEBELL)
        }

        metaDao.insertAllMuscles(muscles)
        metaDao.insertAllEquipment(equipment)
    }
}
