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
    indices = [Index(value = ["phaseId", "weekIndex", "dayIndex"], unique = true)]
)
data class WeekDayPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val phaseId: Long,
    val weekIndex: Int,
    val dayIndex: Int, // 1..7; current UI cares about 1..5
    val displayName: String? = null, // optional “Week 1 Day 1: Lower”
    // NEW: date attachment (nullable until user picks a start date)
    val dateEpochDay: Long? = null
)
