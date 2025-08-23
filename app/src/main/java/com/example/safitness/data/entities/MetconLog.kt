package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Plan-scoped metcon results.
 * - type: "FOR_TIME" | "AMRAP" | "EMOM"
 * - result: "RX" | "SCALED"
 */
@Entity(
    tableName = "metcon_log",
    indices = [Index("dateEpochDay"), Index("planId")]
)
data class MetconLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayIndex: Int? = null,           // legacy
    val planId: Long? = null,            // plan-scoped logs
    val dateEpochDay: Long,              // new: enable date queries
    val type: String,
    val durationSeconds: Int,
    val timeSeconds: Int?,
    val rounds: Int?,
    val extraReps: Int?,
    val intervalsCompleted: Int?,
    val result: String?,
    val createdAt: Long = System.currentTimeMillis()
)

