// app/src/main/java/com/example/safitness/data/entities/ProgramSelection.kt
package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "ProgramSelection",
    primaryKeys = ["dayIndex", "exerciseId"],  // keep the legacy PK
    indices = [
        Index("dateEpochDay"),                 // allow fast date queries
        Index(value = ["dateEpochDay", "exerciseId"], unique = false) // optional helper
    ]
)
data class ProgramSelection(
    val dayIndex: Int,                         // legacy PK part
    val exerciseId: Long,                      // legacy PK part
    val dateEpochDay: Long?,                   // NEW: nullable during migration
    val required: Boolean,
    val preferredEquipment: com.example.safitness.core.Equipment?,
    val targetReps: Int?
)
