package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "program_metcon_selection",
    indices = [Index("dayIndex"), Index(value = ["dayIndex","displayOrder"])]
)
data class ProgramMetconSelection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dayIndex: Int,
    val planId: Long,
    val required: Boolean,
    val displayOrder: Int
)
