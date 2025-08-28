package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "engine_plans",
    indices = [Index(value = ["title"], unique = true)])
data class EnginePlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,                       // e.g., "Row — 2k For Time"
    val mode: String,                        // EngineMode enum name
    val intent: String,                      // EngineIntent enum name
    val programDistanceMeters: Int? = null,  // FOR_TIME
    val programDurationSeconds: Int? = null, // FOR_DISTANCE / FOR_CALORIES
    val programTargetCalories: Int? = null,  // FOR_CALORIES (optional)
    val description: String? = null,
    val rxNotes: String? = null,
    val scaledNotes: String? = null
)
