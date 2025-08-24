package com.example.safitness.data.seed

import com.example.safitness.core.*
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.Exercise
import com.example.safitness.data.entities.ExerciseMuscle
import com.example.safitness.data.entities.ExerciseEquipment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ExerciseSeed {

    // IMPORTANT: Names below are used again in the metadata section. Keep exact strings.
    val DEFAULT_EXERCISES = listOf(
        // -------- PUSH (Chest / Delts / Triceps) --------
        Exercise(name = "Barbell Bench Press",          workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.BARBELL,   modality = Modality.STRENGTH, isUnilateral = false),
        Exercise(name = "Incline Barbell Bench Press",  workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.BARBELL,   modality = Modality.STRENGTH, isUnilateral = false),
        Exercise(name = "Dumbbell Bench Press",         workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.DUMBBELL,  modality = Modality.STRENGTH, isUnilateral = false),
        Exercise(name = "Dumbbell Incline Press",       workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.DUMBBELL,  modality = Modality.STRENGTH, isUnilateral = false),
        Exercise(name = "Overhead Press",               workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.BARBELL,   modality = Modality.STRENGTH),
        Exercise(name = "Seated Dumbbell Shoulder Press", workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.DUMBBELL, modality = Modality.STRENGTH),
        Exercise(name = "Arnold Press",                 workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.DUMBBELL,  modality = Modality.STRENGTH),
        Exercise(name = "Push-Up",                      workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.BODYWEIGHT,modality = Modality.STRENGTH),
        Exercise(name = "Dip",                          workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.BODYWEIGHT,modality = Modality.STRENGTH),
        Exercise(name = "Close-Grip Bench Press",       workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.BARBELL,   modality = Modality.STRENGTH),
        Exercise(name = "Skull Crusher",                workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.BARBELL,   modality = Modality.STRENGTH),
        Exercise(name = "Overhead Triceps Extension (Cable)", workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.CABLE, modality = Modality.STRENGTH),
        Exercise(name = "Dumbbell Flye",                workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.DUMBBELL,  modality = Modality.STRENGTH),
        Exercise(name = "Cable Flye",                   workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.CABLE,     modality = Modality.STRENGTH),
        Exercise(name = "Lateral Raise",                workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.DUMBBELL,  modality = Modality.STRENGTH),
        Exercise(name = "Front Raise",                  workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.DUMBBELL,  modality = Modality.STRENGTH),
        Exercise(name = "Machine Chest Press",          workoutType = WorkoutType.PUSH, primaryEquipment = Equipment.MACHINE,   modality = Modality.STRENGTH),

        // -------- PULL (Lats / Upper back / Biceps / Rear delts) --------
        Exercise(name = "Barbell Bent-Over Row",        workoutType = WorkoutType.PULL, primaryEquipment = Equipment.BARBELL,   modality = Modality.STRENGTH),
        Exercise(name = "Pendlay Row",                  workoutType = WorkoutType.PULL, primaryEquipment = Equipment.BARBELL,   modality = Modality.STRENGTH),
        Exercise(name = "One-Arm DB Row",               workoutType = WorkoutType.PULL, primaryEquipment = Equipment.DUMBBELL,  modality = Modality.STRENGTH, isUnilateral = true),
        Exercise(name = "Chest-Supported DB Row",       workoutType = WorkoutType.PULL, primaryEquipment = Equipment.DUMBBELL,  modality = Modality.STRENGTH),
        Exercise(name = "Lat Pulldown",                 workoutType = WorkoutType.PULL, primaryEquipment = Equipment.CABLE,     modality = Modality.STRENGTH),
        Exercise(name = "Seated Cable Row",             workoutType = WorkoutType.PULL, primaryEquipment = Equipment.CABLE,     modality = Modality.STRENGTH),
        Exercise(name = "Straight-Arm Pulldown",        workoutType = WorkoutType.PULL, primaryEquipment = Equipment.CABLE,     modality = Modality.STRENGTH),
        Exercise(name = "Pull-Up",                      workoutType = WorkoutType.PULL, primaryEquipment = Equipment.BODYWEIGHT,modality = Modality.STRENGTH),
        Exercise(name = "Chin-Up",                      workoutType = WorkoutType.PULL, primaryEquipment = Equipment.BODYWEIGHT,modality = Modality.STRENGTH),
        Exercise(name = "Face Pull",                    workoutType = WorkoutType.PULL, primaryEquipment = Equipment.CABLE,     modality = Modality.STRENGTH),
        Exercise(name = "Rear Delt Flye (DB)",          workoutType = WorkoutType.PULL, primaryEquipment = Equipment.DUMBBELL,  modality = Modality.STRENGTH),
        Exercise(name = "Barbell Curl",                 workoutType = WorkoutType.PULL, primaryEquipment = Equipment.BARBELL,   modality = Modality.STRENGTH),
        Exercise(name = "Dumbbell Curl",                workoutType = WorkoutType.PULL, primaryEquipment = Equipment.DUMBBELL,  modality = Modality.STRENGTH),
        Exercise(name = "Hammer Curl",                  workoutType = WorkoutType.PULL, primaryEquipment = Equipment.DUMBBELL,  modality = Modality.STRENGTH),

        // -------- LEGS/CORE (Squat / Hinge / Lunge / Abs) --------
        Exercise(name = "Back Squat",                   workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.BARBELL,   modality = Modality.STRENGTH),
        Exercise(name = "Front Squat",                  workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.BARBELL,   modality = Modality.STRENGTH),
        Exercise(name = "Deadlift",                     workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.BARBELL,   modality = Modality.STRENGTH),
        Exercise(name = "Romanian Deadlift",            workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.BARBELL,   modality = Modality.STRENGTH),
        Exercise(name = "Good Morning",                 workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.BARBELL,   modality = Modality.STRENGTH),
        Exercise(name = "Goblet Squat",                 workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.DUMBBELL,  modality = Modality.STRENGTH),
        Exercise(name = "Bulgarian Split Squat",        workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.DUMBBELL,  modality = Modality.STRENGTH, isUnilateral = true),
        Exercise(name = "Walking Lunge",                workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.DUMBBELL,  modality = Modality.STRENGTH, isUnilateral = true),
        Exercise(name = "Step-Up",                      workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.DUMBBELL,  modality = Modality.STRENGTH, isUnilateral = true),
        Exercise(name = "Hip Thrust",                   workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.BARBELL,   modality = Modality.STRENGTH),
        Exercise(name = "Leg Press",                    workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.MACHINE,   modality = Modality.STRENGTH),
        Exercise(name = "Leg Extension",                workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.MACHINE,   modality = Modality.STRENGTH),
        Exercise(name = "Leg Curl",                     workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.MACHINE,   modality = Modality.STRENGTH),
        Exercise(name = "Standing Calf Raise",          workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.DUMBBELL,  modality = Modality.STRENGTH),
        Exercise(name = "Seated Calf Raise",            workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.MACHINE,   modality = Modality.STRENGTH),
        Exercise(name = "Crunch",                       workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.BODYWEIGHT,modality = Modality.STRENGTH),
        Exercise(name = "Hanging Leg Raise",            workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.BODYWEIGHT,modality = Modality.STRENGTH),
        Exercise(name = "Knees-to-Elbows",              workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.BODYWEIGHT,modality = Modality.STRENGTH),
        Exercise(name = "Plank",                        workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.BODYWEIGHT,modality = Modality.STRENGTH),
        Exercise(name = "Side Plank",                   workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.BODYWEIGHT,modality = Modality.STRENGTH, isUnilateral = true),
        Exercise(name = "Ab Wheel Rollout",             workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.BAND,      modality = Modality.STRENGTH),
        Exercise(name = "Cable Woodchop",               workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.CABLE,     modality = Modality.STRENGTH),
        Exercise(name = "Pallof Press",                 workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.CABLE,     modality = Modality.STRENGTH),
        Exercise(name = "Russian Twist",                workoutType = WorkoutType.LEGS_CORE, primaryEquipment = Equipment.DUMBBELL,  modality = Modality.STRENGTH)
    )

    suspend fun seedOrUpdate(db: AppDatabase) = withContext(Dispatchers.IO) {
        val exDao = db.exerciseDao()
        val metaDao = db.exerciseMetadataDao()
        exDao.insertAllIgnore(DEFAULT_EXERCISES)

        // Map names -> IDs (only for successfully inserted/found)
        val ids: Map<String, Long> = DEFAULT_EXERCISES.mapNotNull { e ->
            exDao.getIdByName(e.name)?.let { id -> e.name to id }
        }.toMap()

        val muscles = mutableListOf<ExerciseMuscle>()
        val equipment = mutableListOf<ExerciseEquipment>()

        // ---------------- LEGS/CORE muscles ----------------
        fun legsPrimary(id: Long, mg: MuscleGroup) { muscles += ExerciseMuscle(exerciseId = id, muscle = mg, role = "PRIMARY") }
        fun legsSecondary(id: Long, mg: MuscleGroup) { muscles += ExerciseMuscle(exerciseId = id, muscle = mg, role = "SECONDARY") }

        ids["Back Squat"]?.let { id ->
            legsPrimary(id, MuscleGroup.QUADS); legsSecondary(id, MuscleGroup.GLUTES); legsSecondary(id, MuscleGroup.ERECTORS)
        }
        ids["Front Squat"]?.let { id ->
            legsPrimary(id, MuscleGroup.QUADS); legsSecondary(id, MuscleGroup.GLUTES)
        }
        ids["Deadlift"]?.let { id ->
            legsPrimary(id, MuscleGroup.HAMSTRINGS); legsSecondary(id, MuscleGroup.GLUTES); legsSecondary(id, MuscleGroup.ERECTORS)
        }
        ids["Romanian Deadlift"]?.let { id ->
            legsPrimary(id, MuscleGroup.HAMSTRINGS); legsSecondary(id, MuscleGroup.GLUTES); legsSecondary(id, MuscleGroup.ERECTORS)
        }
        ids["Good Morning"]?.let { id ->
            legsPrimary(id, MuscleGroup.ERECTORS); legsSecondary(id, MuscleGroup.HAMSTRINGS)
        }
        ids["Goblet Squat"]?.let { id ->
            legsPrimary(id, MuscleGroup.QUADS); legsSecondary(id, MuscleGroup.GLUTES)
        }
        ids["Bulgarian Split Squat"]?.let { id ->
            legsPrimary(id, MuscleGroup.QUADS); legsSecondary(id, MuscleGroup.GLUTES)
        }
        ids["Walking Lunge"]?.let { id ->
            legsPrimary(id, MuscleGroup.QUADS); legsSecondary(id, MuscleGroup.GLUTES)
        }
        ids["Step-Up"]?.let { id ->
            legsPrimary(id, MuscleGroup.QUADS); legsSecondary(id, MuscleGroup.GLUTES)
        }
        ids["Hip Thrust"]?.let { id ->
            legsPrimary(id, MuscleGroup.GLUTES); legsSecondary(id, MuscleGroup.HAMSTRINGS)
        }
        ids["Leg Press"]?.let { id ->
            legsPrimary(id, MuscleGroup.QUADS); legsSecondary(id, MuscleGroup.GLUTES)
        }
        ids["Leg Extension"]?.let { id ->
            legsPrimary(id, MuscleGroup.QUADS)
        }
        ids["Leg Curl"]?.let { id ->
            legsPrimary(id, MuscleGroup.HAMSTRINGS)
        }
        ids["Standing Calf Raise"]?.let { id ->
            legsPrimary(id, MuscleGroup.CALVES)
        }
        ids["Seated Calf Raise"]?.let { id ->
            legsPrimary(id, MuscleGroup.CALVES)
        }
        ids["Crunch"]?.let { id ->
            legsPrimary(id, MuscleGroup.ABS)
        }
        ids["Hanging Leg Raise"]?.let { id ->
            legsPrimary(id, MuscleGroup.ABS); legsSecondary(id, MuscleGroup.OBLIQUES)
        }
        ids["Knees-to-Elbows"]?.let { id ->
            legsPrimary(id, MuscleGroup.ABS); legsSecondary(id, MuscleGroup.OBLIQUES)
        }
        ids["Plank"]?.let { id ->
            legsPrimary(id, MuscleGroup.ABS)
        }
        ids["Side Plank"]?.let { id ->
            legsPrimary(id, MuscleGroup.OBLIQUES)
        }
        ids["Ab Wheel Rollout"]?.let { id ->
            legsPrimary(id, MuscleGroup.ABS); legsSecondary(id, MuscleGroup.ERECTORS)
        }
        ids["Cable Woodchop"]?.let { id ->
            legsPrimary(id, MuscleGroup.OBLIQUES)
        }
        ids["Pallof Press"]?.let { id ->
            legsPrimary(id, MuscleGroup.OBLIQUES)
        }
        ids["Russian Twist"]?.let { id ->
            legsPrimary(id, MuscleGroup.OBLIQUES)
        }

        // ---------------- LEGS/CORE equipment ----------------
        fun eq(id: Long, e: Equipment) { equipment += ExerciseEquipment(exerciseId = id, equipment = e) }

        ids["Back Squat"]?.let { eq(it, Equipment.BARBELL) }
        ids["Front Squat"]?.let { eq(it, Equipment.BARBELL) }
        ids["Deadlift"]?.let { eq(it, Equipment.BARBELL) }
        ids["Romanian Deadlift"]?.let { id -> eq(id, Equipment.BARBELL) }
        ids["Good Morning"]?.let { eq(it, Equipment.BARBELL) }
        ids["Goblet Squat"]?.let { id -> eq(id, Equipment.DUMBBELL); eq(id, Equipment.KETTLEBELL) }
        ids["Bulgarian Split Squat"]?.let { eq(it, Equipment.DUMBBELL) }
        ids["Walking Lunge"]?.let { eq(it, Equipment.DUMBBELL) }
        ids["Step-Up"]?.let { eq(it, Equipment.DUMBBELL) }
        ids["Hip Thrust"]?.let { eq(it, Equipment.BARBELL) }
        ids["Leg Press"]?.let { eq(it, Equipment.MACHINE) }
        ids["Leg Extension"]?.let { eq(it, Equipment.MACHINE) }
        ids["Leg Curl"]?.let { eq(it, Equipment.MACHINE) }
        ids["Standing Calf Raise"]?.let { eq(it, Equipment.DUMBBELL) }
        ids["Seated Calf Raise"]?.let { eq(it, Equipment.MACHINE) }
        ids["Crunch"]?.let { eq(it, Equipment.BODYWEIGHT) }
        ids["Hanging Leg Raise"]?.let { eq(it, Equipment.BODYWEIGHT) }
        ids["Knees-to-Elbows"]?.let { eq(it, Equipment.BODYWEIGHT) }
        ids["Plank"]?.let { eq(it, Equipment.BODYWEIGHT) }
        ids["Side Plank"]?.let { eq(it, Equipment.BODYWEIGHT) }
        ids["Ab Wheel Rollout"]?.let { eq(it, Equipment.BAND) }
        ids["Cable Woodchop"]?.let { eq(it, Equipment.CABLE) }
        ids["Pallof Press"]?.let { eq(it, Equipment.CABLE) }
        ids["Russian Twist"]?.let { eq(it, Equipment.DUMBBELL) }

        // ---------------- PUSH muscles & equipment ----------------
        fun pushPrimary(id: Long, mg: MuscleGroup) { muscles += ExerciseMuscle(exerciseId = id, muscle = mg, role = "PRIMARY") }
        fun pushSecondary(id: Long, mg: MuscleGroup) { muscles += ExerciseMuscle(exerciseId = id, muscle = mg, role = "SECONDARY") }

        ids["Barbell Bench Press"]?.let { id ->
            pushPrimary(id, MuscleGroup.CHEST); pushSecondary(id, MuscleGroup.TRICEPS); eq(id, Equipment.BARBELL)
        }
        ids["Incline Barbell Bench Press"]?.let { id ->
            pushPrimary(id, MuscleGroup.CHEST); pushSecondary(id, MuscleGroup.DELTS_ANT); eq(id, Equipment.BARBELL)
        }
        ids["Dumbbell Bench Press"]?.let { id ->
            pushPrimary(id, MuscleGroup.CHEST); pushSecondary(id, MuscleGroup.TRICEPS); eq(id, Equipment.DUMBBELL)
        }
        ids["Dumbbell Incline Press"]?.let { id ->
            pushPrimary(id, MuscleGroup.CHEST); pushSecondary(id, MuscleGroup.DELTS_ANT); eq(id, Equipment.DUMBBELL)
        }
        ids["Overhead Press"]?.let { id ->
            pushPrimary(id, MuscleGroup.DELTS_ANT); pushSecondary(id, MuscleGroup.TRICEPS); eq(id, Equipment.BARBELL)
        }
        ids["Seated Dumbbell Shoulder Press"]?.let { id ->
            pushPrimary(id, MuscleGroup.DELTS_ANT); pushSecondary(id, MuscleGroup.TRICEPS); eq(id, Equipment.DUMBBELL)
        }
        ids["Arnold Press"]?.let { id ->
            pushPrimary(id, MuscleGroup.DELTS_ANT); pushSecondary(id, MuscleGroup.DELTS_MED); eq(id, Equipment.DUMBBELL)
        }
        ids["Push-Up"]?.let { id ->
            pushPrimary(id, MuscleGroup.CHEST); eq(id, Equipment.BODYWEIGHT)
        }
        ids["Dip"]?.let { id ->
            pushPrimary(id, MuscleGroup.CHEST); pushSecondary(id, MuscleGroup.TRICEPS); eq(id, Equipment.BODYWEIGHT)
        }
        ids["Close-Grip Bench Press"]?.let { id ->
            pushPrimary(id, MuscleGroup.TRICEPS); pushSecondary(id, MuscleGroup.CHEST); eq(id, Equipment.BARBELL)
        }
        ids["Skull Crusher"]?.let { id ->
            pushPrimary(id, MuscleGroup.TRICEPS); eq(id, Equipment.BARBELL)
        }
        ids["Overhead Triceps Extension (Cable)"]?.let { id ->
            pushPrimary(id, MuscleGroup.TRICEPS); eq(id, Equipment.CABLE)
        }
        ids["Dumbbell Flye"]?.let { id ->
            pushPrimary(id, MuscleGroup.CHEST); eq(id, Equipment.DUMBBELL)
        }
        ids["Cable Flye"]?.let { id ->
            pushPrimary(id, MuscleGroup.CHEST); eq(id, Equipment.CABLE)
        }
        ids["Lateral Raise"]?.let { id ->
            pushPrimary(id, MuscleGroup.DELTS_MED); eq(id, Equipment.DUMBBELL)
        }
        ids["Front Raise"]?.let { id ->
            pushPrimary(id, MuscleGroup.DELTS_ANT); eq(id, Equipment.DUMBBELL)
        }
        ids["Machine Chest Press"]?.let { id ->
            pushPrimary(id, MuscleGroup.CHEST); pushSecondary(id, MuscleGroup.TRICEPS); eq(id, Equipment.MACHINE)
        }

        // ---------------- PULL muscles & equipment ----------------
        fun pullPrimary(id: Long, mg: MuscleGroup) { muscles += ExerciseMuscle(exerciseId = id, muscle = mg, role = "PRIMARY") }
        fun pullSecondary(id: Long, mg: MuscleGroup) { muscles += ExerciseMuscle(exerciseId = id, muscle = mg, role = "SECONDARY") }

        ids["Barbell Bent-Over Row"]?.let { id ->
            pullPrimary(id, MuscleGroup.UPPER_BACK); pullSecondary(id, MuscleGroup.LATS); eq(id, Equipment.BARBELL)
        }
        ids["Pendlay Row"]?.let { id ->
            pullPrimary(id, MuscleGroup.UPPER_BACK); pullSecondary(id, MuscleGroup.LATS); eq(id, Equipment.BARBELL)
        }
        ids["One-Arm DB Row"]?.let { id ->
            pullPrimary(id, MuscleGroup.LATS); pullSecondary(id, MuscleGroup.BICEPS); eq(id, Equipment.DUMBBELL)
        }
        ids["Chest-Supported DB Row"]?.let { id ->
            pullPrimary(id, MuscleGroup.LATS); pullSecondary(id, MuscleGroup.UPPER_BACK); eq(id, Equipment.DUMBBELL)
        }
        ids["Lat Pulldown"]?.let { id ->
            pullPrimary(id, MuscleGroup.LATS); pullSecondary(id, MuscleGroup.BICEPS); eq(id, Equipment.CABLE)
        }
        ids["Seated Cable Row"]?.let { id ->
            pullPrimary(id, MuscleGroup.LATS); pullSecondary(id, MuscleGroup.UPPER_BACK); eq(id, Equipment.CABLE)
        }
        ids["Straight-Arm Pulldown"]?.let { id ->
            pullPrimary(id, MuscleGroup.LATS); eq(id, Equipment.CABLE)
        }
        ids["Pull-Up"]?.let { id ->
            pullPrimary(id, MuscleGroup.LATS); pullSecondary(id, MuscleGroup.BICEPS); eq(id, Equipment.BODYWEIGHT)
        }
        ids["Chin-Up"]?.let { id ->
            pullPrimary(id, MuscleGroup.BICEPS); pullSecondary(id, MuscleGroup.LATS); eq(id, Equipment.BODYWEIGHT)
        }
        ids["Face Pull"]?.let { id ->
            pullPrimary(id, MuscleGroup.REAR_DELTS); eq(id, Equipment.CABLE)
        }
        ids["Rear Delt Flye (DB)"]?.let { id ->
            pullPrimary(id, MuscleGroup.REAR_DELTS); eq(id, Equipment.DUMBBELL)
        }
        ids["Barbell Curl"]?.let { id ->
            pullPrimary(id, MuscleGroup.BICEPS); eq(id, Equipment.BARBELL)
        }
        ids["Dumbbell Curl"]?.let { id ->
            pullPrimary(id, MuscleGroup.BICEPS); eq(id, Equipment.DUMBBELL)
        }
        ids["Hammer Curl"]?.let { id ->
            pullPrimary(id, MuscleGroup.BICEPS); eq(id, Equipment.DUMBBELL)
        }

        // Commit metadata
        metaDao.insertAllMuscles(muscles)
        metaDao.insertAllEquipment(equipment)
    }
}
