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

        // --- MUSCLES (Legs/Core) ---
        ids["Front Squat"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.QUADS, role = "PRIMARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.GLUTES, role = "SECONDARY")
        }
        ids["Goblet Squat"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.QUADS, role = "PRIMARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.GLUTES, role = "SECONDARY")
        }
        ids["Bulgarian Split Squat"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.QUADS, role = "PRIMARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.GLUTES, role = "SECONDARY")
        }
        ids["Romanian Deadlift"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.HAMSTRINGS, role = "PRIMARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.GLUTES, role = "SECONDARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.ERECTORS, role = "SECONDARY")
        }
        ids["Hip Thrust"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.GLUTES, role = "PRIMARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.HAMSTRINGS, role = "SECONDARY")
        }
        ids["Walking Lunge"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.QUADS, role = "PRIMARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.GLUTES, role = "SECONDARY")
        }
        ids["Standing Calf Raise"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.CALVES, role = "PRIMARY")
        }
        ids["Crunch"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.ABS, role = "PRIMARY")
        }
        ids["Hanging Leg Raise"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.ABS, role = "PRIMARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.OBLIQUES, role = "SECONDARY")
        }
        ids["Plank"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.ABS, role = "PRIMARY")
        }
        ids["Russian Twist"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.OBLIQUES, role = "PRIMARY")
        }

// --- EQUIPMENT (Legs/Core) ---
        ids["Back Squat"]?.let { id ->
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.BARBELL)
        }
        ids["Front Squat"]?.let { id ->
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.BARBELL)
        }
        ids["Goblet Squat"]?.let { id ->
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.DUMBBELL)
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.KETTLEBELL)
        }
        ids["Bulgarian Split Squat"]?.let { id ->
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.DUMBBELL)
        }
        ids["Walking Lunge"]?.let { id ->
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.DUMBBELL)
        }
        ids["Deadlift"]?.let { id ->
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.BARBELL)
        }
        ids["Romanian Deadlift"]?.let { id ->
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.BARBELL)
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.DUMBBELL)
        }
        ids["Hip Thrust"]?.let { id ->
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.BARBELL)
        }
        ids["Standing Calf Raise"]?.let { id ->
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.DUMBBELL)
        }
        ids["Crunch"]?.let { id ->
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.BODYWEIGHT)
        }
        ids["Hanging Leg Raise"]?.let { id ->
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.BODYWEIGHT)
        }
        ids["Plank"]?.let { id ->
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.BODYWEIGHT)
        }
        ids["Russian Twist"]?.let { id ->
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.DUMBBELL)
        }
        // ---------- PUSH (Chest / Delts / Triceps) ----------
        ids["Barbell Bench Press"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.CHEST,   role = "PRIMARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.TRICEPS, role = "SECONDARY")
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.BARBELL)
        }
        ids["Incline Dumbbell Press"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.CHEST,     role = "PRIMARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.DELTS_ANT, role = "SECONDARY")
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.DUMBBELL)
        }
        ids["Overhead Press"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.DELTS_ANT, role = "PRIMARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.TRICEPS,   role = "SECONDARY")
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.BARBELL)
        }
        ids["Dumbbell Shoulder Press"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.DELTS_ANT, role = "PRIMARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.TRICEPS,   role = "SECONDARY")
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.DUMBBELL)
        }
        ids["Dips"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.CHEST,   role = "PRIMARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.TRICEPS, role = "SECONDARY")
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.BODYWEIGHT)
        }
        ids["Push-up"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.CHEST, role = "PRIMARY")
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.BODYWEIGHT)
        }
        ids["Cable Fly"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.CHEST, role = "PRIMARY")
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.CABLE)
        }
        ids["Dumbbell Fly"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.CHEST, role = "PRIMARY")
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.DUMBBELL)
        }
        ids["Triceps Pushdown"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.TRICEPS, role = "PRIMARY")
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.CABLE)
        }

// ---------- PULL (Lats / Upper back / Biceps / Rear delts) ----------
        ids["Pull-up"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.LATS,   role = "PRIMARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.BICEPS, role = "SECONDARY")
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.BODYWEIGHT)
        }
        ids["Lat Pulldown"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.LATS,   role = "PRIMARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.BICEPS, role = "SECONDARY")
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.MACHINE)
        }
        ids["Barbell Bent-over Row"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.UPPER_BACK, role = "PRIMARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.LATS,       role = "SECONDARY")
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.BARBELL)
        }
        ids["Dumbbell Row"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.LATS,   role = "PRIMARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.BICEPS, role = "SECONDARY")
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.DUMBBELL)
        }
        ids["Seated Cable Row"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.LATS,       role = "PRIMARY")
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.UPPER_BACK, role = "SECONDARY")
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.CABLE)
        }
        ids["Face Pull"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.REAR_DELTS, role = "PRIMARY")
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.CABLE)
        }
        ids["Barbell Curl"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.BICEPS, role = "PRIMARY")
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.BARBELL)
        }
        ids["Dumbbell Curl"]?.let { id ->
            muscles += ExerciseMuscle(exerciseId = id, muscle = MuscleGroup.BICEPS, role = "PRIMARY")
            equipment += ExerciseEquipment(exerciseId = id, equipment = Equipment.DUMBBELL)
        }
        metaDao.insertAllMuscles(muscles)
        metaDao.insertAllEquipment(equipment)
    }
}
