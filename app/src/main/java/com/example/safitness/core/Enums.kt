package com.example.safitness.core

enum class WorkoutType { PUSH, PULL, LEGS_CORE, METCON }

enum class Equipment { BARBELL, DUMBBELL, CABLE, BODYWEIGHT, KETTLEBELL, MACHINE }

enum class Modality { STRENGTH, METCON }

/** Types of metcon plans you can define in the library. */
enum class MetconType { FOR_TIME, AMRAP, EMOM }

/** User result selection in metcon UI. Not yet persisted (time is persisted). */
enum class MetconResult { RX, SCALED }
