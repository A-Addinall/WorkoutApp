package com.example.safitness.core

enum class WorkoutType { PUSH, PULL, LEGS_CORE, METCON }
enum class Equipment { BARBELL, DUMBBELL, BODYWEIGHT, MACHINE, OTHER, KETTLEBELL, CABLE }
enum class Modality { STRENGTH, METCON }

enum class RepScheme(val reps: Int) {
    R3(3), R5(5), R8(8), R10(10), R12(12), R15(15);

    companion object {
        fun fromReps(value: Int): RepScheme =
            RepScheme.entries.firstOrNull { it.reps == value } ?: R8
    }
}