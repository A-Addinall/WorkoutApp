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
    indices = [Index("planId"), Index("dayIndex")]
)
data class MetconLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayIndex: Int,
    val planId: Long,
    val type: String,

    /** Programmed duration in seconds (AMRAP/EMOM use this; For Time can be 0). */
    val durationSeconds: Int,

    /** For FOR_TIME only. Nullable for other modes. */
    val timeSeconds: Int? = null,

    /** For AMRAP only. Nullable for other modes. */
    val rounds: Int? = null,
    val extraReps: Int? = null,

    /** For EMOM only (e.g. minutes/intervals completed). Nullable for others. */
    val intervalsCompleted: Int? = null,

    /** "RX" or "SCALED" */
    val result: String,

    val createdAt: Long = System.currentTimeMillis(),
    val notes: String? = null
)
