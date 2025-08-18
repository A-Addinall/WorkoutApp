package com.example.safitness.core

enum class WorkoutType { PUSH, PULL, LEGS_CORE }

enum class Equipment { BARBELL, DUMBBELL, CABLE, BODYWEIGHT, KETTLEBELL }

enum class Modality { STRENGTH, METCON }

/** Types of metcon plans you can define in the library. */
enum class MetconType { FOR_TIME, AMRAP, EMOM }

/** User result selection in metcon UI. Not yet persisted (time is persisted). */
enum class MetconResult { RX, SCALED }

enum class EngineMode { RUN, ROW, BIKE }

enum class EngineIntent { FOR_TIME, FOR_DISTANCE, FOR_CALORIES }

enum class SkillType { DOUBLE_UNDERS, HANDSTAND_HOLD, MUSCLE_UP }

enum class SkillTestType { MAX_REPS_UNBROKEN, FOR_TIME_REPS, MAX_HOLD_SECONDS, ATTEMPTS }
