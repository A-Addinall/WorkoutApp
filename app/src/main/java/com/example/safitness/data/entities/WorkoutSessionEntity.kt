package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_session",
    indices = [
        Index("dateEpochDay"),
        Index("phaseId"),
        Index("weekIndex"),
        Index("dayIndex")
    ]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    // Phase/calendar context (optional while migrating)
    val phaseId: Long? = null,
    val weekIndex: Int? = null,
    // Legacy support (1..5), keep nullable during migration
    val dayIndex: Int? = null,
    // New canonical key for a day
    val dateEpochDay: Long,
    // Timestamps (you can later consolidate to one)
    val startTs: Long = System.currentTimeMillis(),
    val createdAtEpochSec: Long = System.currentTimeMillis() / 1000
)
