package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.safitness.core.Equipment
import com.example.safitness.core.MetconResult

@Entity
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val equipment: Equipment,
    val setNumber: Int,
    val reps: Int,
    val weight: Double?,      // null for time-only work
    val timeSeconds: Int?,    // null for weight-based sets
    val rpe: Double?,         // nullable
    val success: Boolean?,    // nullable for neutral
    val notes: String? = null,

    // NEW: mark metcon attempts as RX or Scaled (null for non-metcon or legacy rows)
    val metconResult: MetconResult? = null
)
