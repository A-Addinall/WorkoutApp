package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Engine performance logs (RUN/ROW/BIKE etc).
 * We deliberately store enums as Strings to avoid adding TypeConverters.
 */
@Entity(
    tableName = "engine_log",
    indices = [
        Index("date"),
        Index(value = ["mode", "intent", "date"])
    ]
)
data class EngineLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: Long? = null,
    /** epoch seconds */
    val date: Long,

    /** e.g., RUN | ROW | BIKE */
    val mode: String,

    /** FOR_TIME | FOR_DISTANCE | FOR_CALORIES */
    val intent: String,

    // ---- Program (one of these is non-null, defining the target) ----
    val programDistanceMeters: Int? = null,
    val programDurationSeconds: Int? = null,
    val programTargetCalories: Int? = null,

    // ---- Result (one or more depending on intent) ----
    val resultTimeSeconds: Int? = null,
    val resultDistanceMeters: Int? = null,
    val resultCalories: Int? = null,

    /** Optional, convenience—store calculated pace */
    val pace: Double? = null,

    val scaled: Boolean = false,
    val notes: String? = null
)
