// app/src/main/java/com/example/safitness/data/entities/ProgramSelection.kt
package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.safitness.core.Equipment

@Entity
data class ProgramSelection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dayIndex: Int,
    val exerciseId: Long,
    val required: Boolean,
    val preferredEquipment: Equipment?,
    val targetReps: Int? // selected rep target (3/5/8/10/12/15) or null
)
