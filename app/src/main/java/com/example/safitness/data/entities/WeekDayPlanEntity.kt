package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per (phase, concrete calendar day).
 * weekIndex/dayIndex are retained for legacy displays but dateEpochDay is canonical.
 */
@Entity(
    tableName = "week_day_plan",
    indices = [
        Index("dateEpochDay"),
        Index(value = ["phaseId", "dateEpochDay"], unique = true)
    ]
)
data class WeekDayPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phaseId: Long,
    val weekIndex: Int,
    val dayIndex: Int,
    val dateEpochDay: Long
)
