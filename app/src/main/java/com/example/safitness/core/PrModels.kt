package com.example.safitness.core

/**
 * Unified PR celebration event.
 * - If a hard (rep-max) PR happened, newWeightKg/prevWeightKg will be populated and isHardPr = true.
 * - If only a soft (e1RM) PR happened, newWeightKg/prevWeightKg will be null and isHardPr = false.
 * - newE1rmKg is always present when a PR is emitted; prevE1rmKg is present if a prior e1RM existed.
 */
data class PrCelebrationEvent(
    val exerciseId: Long,
    val equipment: Equipment,
    val isHardPr: Boolean,
    val reps: Int?,                // null if soft-only PR
    val newWeightKg: Double?,      // present for hard PR
    val prevWeightKg: Double?,     // present for hard PR when previous exists
    val newE1rmKg: Double,         // always present for both hard and soft PR
    val prevE1rmKg: Double?        // present if a previous e1RM existed
)
