package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Simple, dev-friendly day item to attach Engine or Skill work to a specific day.
 * We keep this separate from existing day items to avoid touching your other flows.
 */
@Entity(tableName = "day_engine_skill")
data class DayEngineSkillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,

    // Which day
    val weekIndex: Int,     // dev uses week 1; keep flexible
    val dayIndex: Int,      // 1..5
    val orderIndex: Int = 0,

    // Kinds: "ENGINE" or "SKILL"
    val kind: String,

    // ---- ENGINE fields (when kind == "ENGINE") ----
    val engineMode: String? = null,    // RUN | ROW | BIKE
    val engineIntent: String? = null,  // FOR_TIME | FOR_DISTANCE | FOR_CALORIES
    val programDistanceMeters: Int? = null,
    val programDurationSeconds: Int? = null,
    val programTargetCalories: Int? = null,

    // ---- SKILL fields (when kind == "SKILL") ----
    val skill: String? = null,         // e.g., DOUBLE_UNDERS, HANDSTAND_HOLD, MUSCLE_UP
    val skillTestType: String? = null, // MAX_REPS_UNBROKEN | FOR_TIME_REPS | MAX_HOLD_SECONDS | ATTEMPTS
    val targetReps: Int? = null,
    val targetDurationSeconds: Int? = null,
    val progressionLevel: String? = null,
    val scaledVariant: String? = null
)
