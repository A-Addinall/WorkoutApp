package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skill_logs")
data class SkillLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val createdAtEpochSec: Long = System.currentTimeMillis() / 1000,

    val skill: String,          // e.g., "BAR_MUSCLE_UP"
    val testType: String,       // SkillTestType

    // ATTEMPTS
    val attemptsSuccess: Int? = null,
    val attemptsFail: Int? = null,

    // MAX_HOLD_SECONDS
    val holdSeconds: Int? = null,

    // FOR_TIME_REPS / MAX_REPS_UNBROKEN
    val reps: Int? = null,

    val notes: String? = null
)
