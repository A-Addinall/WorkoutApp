package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.Index

/**
 * Metcon selections scheduled for a concrete date.
 * Uniqueness is per (dateEpochDay, planId).
 */
@Entity(
    tableName = "program_metcon_selection",
    primaryKeys = ["dateEpochDay", "planId"],
    indices = [
        Index("dateEpochDay"),
        Index("planId")
    ]
)
data class ProgramMetconSelection(
    val dateEpochDay: Long,
    val planId: Long,
    val required: Boolean,
    val displayOrder: Int
)
