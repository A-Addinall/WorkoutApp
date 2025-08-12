package com.example.safitness.core

enum class WorkoutType { PUSH, PULL, LEGS_CORE, METCON }
enum class Equipment { BARBELL, DUMBBELL, KETTLEBELL, CABLE, MACHINE, BODYWEIGHT }
enum class Modality { STRENGTH, METCON }

/** Result recorded per metcon completion. */
enum class MetconResult { RX, SCALED }

/** Plan scoring/mode. */
enum class MetconType { FOR_TIME, AMRAP, EMOM }
