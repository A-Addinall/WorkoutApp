package com.example.safitness.core

enum class WorkoutType { PUSH, PULL, LEGS_CORE }

enum class Equipment { BARBELL, DUMBBELL, CABLE, BODYWEIGHT, KETTLEBELL }

enum class Modality { STRENGTH, METCON }

/** Types of metcon plans you can define in the library. */
enum class MetconType { FOR_TIME, AMRAP, EMOM }

/** User result selection in metcon UI. Not yet persisted (time is persisted). */
enum class MetconResult { RX, SCALED }

enum class EngineMode { RUN, ROW, BIKE, SKI, ASSAULT_BIKE }
enum class EngineIntent { FOR_TIME, FOR_DISTANCE, FOR_CALORIES }

enum class SkillTestType {
    ATTEMPTS,            // success/fail counts
    MAX_HOLD_SECONDS,    // hold time
    FOR_TIME_REPS,       // reps completed in a time cap
    MAX_REPS_UNBROKEN    // max unbroken set
}

object EngineSkillKeys {
    const val ENGINE_MODE = "engine_mode"
    const val ENGINE_INTENT = "engine_intent"

    const val PROGRAM_DISTANCE_METERS = "program_distance_meters"     // For FOR_TIME
    const val PROGRAM_DURATION_SECONDS = "program_duration_seconds"   // For FOR_DISTANCE/CALORIES
    const val PROGRAM_TARGET_CALORIES = "program_target_calories"     // Optional

    const val TITLE = "title"
}
