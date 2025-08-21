package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per (phase, weekIndex 1..N, dayIndex 1..7).
 * We’ll still *use only 1..5* days in UI for now.
 */
@Entity(
    tableName = "week_day_plan",
    indices = [Index("dateEpochDay")] // add if not present
    // Optionally: Index(value = ["phaseId", "dateEpochDay"], unique = true)
)
data class WeekDayPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phaseId: Long,
    val weekIndex: Int,
    val dayIndex: Int,
    val dateEpochDay: Long? // keep nullable during migration; can make non-null later
)

