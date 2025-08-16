package com.example.safitness.core

/**
 * Epley formula for estimated 1RM.
 * Returns null for invalid inputs or reps > 12 (by design).
 */
fun estimateOneRepMax(weightKg: Double, reps: Int): Double? {
    if (weightKg <= 0.0 || reps <= 0 || reps > 12) return null
    return weightKg * (1 + reps / 30.0)
}

/**
 * Simple table mapping target reps to % of 1RM for load suggestions.
 * (Conservative defaults; adjust later if needed.)
 */
fun repsToPercentage(reps: Int): Double {
    return when (reps) {
        1 -> 1.00
        2 -> 0.95
        3 -> 0.93
        4 -> 0.90
        5 -> 0.87
        6 -> 0.85
        7 -> 0.83
        8 -> 0.80
        9 -> 0.77
        10 -> 0.75
        11 -> 0.72
        12 -> 0.70
        else -> 0.70 // default lower-bound if called out of range
    }
}
