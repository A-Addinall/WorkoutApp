package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.safitness.core.MetconType
import androidx.room.Index

// MetconPlan.kt
@Entity(
    tableName = "metcon_plan",
    indices = [Index(value = ["title"], unique = true)]   // <— add
)
data class MetconPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val type: MetconType,
    val durationMinutes: Int? = null,
    val emomIntervalSec: Int? = null
)

