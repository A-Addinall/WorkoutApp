package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "engine_logs")
data class EngineLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAtEpochSec: Long = System.currentTimeMillis() / 1000,

    val mode: String,        // EngineMode
    val intent: String,      // EngineIntent

    // Program targets (one relevant depending on intent)
    val programDistanceMeters: Int? = null,   // FOR_TIME target distance
    val programDurationSeconds: Int? = null,  // FOR_DISTANCE / FOR_CALORIES time cap
    val programTargetCalories: Int? = null,   // Optional goal for calories

    // Results (one relevant depending on intent)
    val resultTimeSeconds: Int? = null,       // FOR_TIME result
    val resultDistanceMeters: Int? = null,    // FOR_DISTANCE result
    val resultCalories: Int? = null,          // FOR_CALORIES result

    val notes: String? = null
)
