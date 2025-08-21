package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "program_metcon_selection",
    indices = [Index("dateEpochDay"), Index("planId")]
)
data class ProgramMetconSelection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayIndex: Int? = null,   // legacy
    val dateEpochDay: Long?,     // add only if using date in this legacy table
    val planId: Long,
    val required: Boolean,
    val displayOrder: Int
)
