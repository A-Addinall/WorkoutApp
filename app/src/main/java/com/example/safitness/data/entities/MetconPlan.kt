package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.safitness.core.MetconType
import com.example.safitness.core.WorkoutType

/**
 * Stable-identity Metcon plan.
 * - canonicalKey is the logical ID (never changes).
 * - title is presentation (can change).
 */
@Entity(
    tableName = "metcon_plan",
    indices = [
        Index(value = ["canonicalKey"], unique = true),
        Index(value = ["title"]),
        Index(value = ["type"]),
        Index(value = ["durationMinutes"])
    ]
)
data class MetconPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val canonicalKey: String,
    val title: String,
    val type: MetconType,
    val durationMinutes: Int? = null,   // used by filters (AMRAP total / EMOM total / FOR_TIME estimate)
    val emomIntervalSec: Int? = null,   // seconds; only meaningful for EMOM
    val isArchived: Boolean = false,
    val focusWorkoutType: WorkoutType? = null
)
