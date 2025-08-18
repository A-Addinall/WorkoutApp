package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Skill logs (e.g., DOUBLE_UNDERS, HANDSTAND_HOLD, MUSCLE_UP).
 */
@Entity(
    tableName = "skill_log",
    indices = [
        Index("date"),
        Index(value = ["skill", "testType", "date"])
    ]
)
data class SkillLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: Long? = null,
    /** epoch seconds */
    val date: Long,

    /** e.g., DOUBLE_UNDERS | HANDSTAND_HOLD | MUSCLE_UP */
    val skill: String,

    /** e.g., MAX_REPS_UNBROKEN | FOR_TIME_REPS | MAX_HOLD_SECONDS | ATTEMPTS */
    val testType: String,

    // ---- Program (targets) ----
    val targetReps: Int? = null,
    val targetDurationSeconds: Int? = null,
    val progressionLevel: String? = null,
    val scaledVariant: String? = null, // e.g., "single unders"

    // ---- Results ----
    val reps: Int? = null,
    val timeSeconds: Int? = null,
    val maxHoldSeconds: Int? = null,
    val attempts: Int? = null,

    val scaled: Boolean = false,
    val notes: String? = null
)
