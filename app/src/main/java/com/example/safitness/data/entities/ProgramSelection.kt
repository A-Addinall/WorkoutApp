// app/src/main/java/com/example/safitness/data/entities/ProgramSelection.kt
package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.Index
import com.example.safitness.core.Equipment

/**
 * Strength exercise selections scheduled for a concrete date.
 * Uniqueness is per (dateEpochDay, exerciseId).
 */
@Entity(
    tableName = "ProgramSelection",
    primaryKeys = ["dateEpochDay", "exerciseId"],
    indices = [
        Index("dateEpochDay"),
        Index("exerciseId")
    ]
)
data class ProgramSelection(
    val dateEpochDay: Long,
    val exerciseId: Long,
    val required: Boolean,
    val preferredEquipment: Equipment?,
    val targetReps: Int?
)
