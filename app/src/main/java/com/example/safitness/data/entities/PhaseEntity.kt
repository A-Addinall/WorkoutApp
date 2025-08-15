package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phase")
data class PhaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,            // e.g., "Phase 1", "Base", "Build"
    val startDateEpochDay: Long, // java.time.LocalDate.toEpochDay()
    val weeks: Int               // e.g., 4..6
)
