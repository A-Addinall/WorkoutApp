package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "program_metcon_selection",
    indices = [Index(value = ["dayIndex"]), Index(value = ["planId"])]
)
data class ProgramMetconSelection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dayIndex: Int,          // 1..5
    val planId: Long,           // FK to MetconPlan.id
    val required: Boolean,      // mirror strength "required/optional"
    val displayOrder: Int = 0   // ordering within the day
)
