package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_session",
    indices = [Index("phaseId"), Index("weekIndex"), Index("dayIndex")]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val phaseId: Long?,
    val weekIndex: Int?, // nullable while we bridge old 5‑day flow
    val dayIndex: Int,   // still used by current UI (1..5)
    val startTs: Long = System.currentTimeMillis()
)
