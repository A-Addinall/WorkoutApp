package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.safitness.core.MetconType

@Entity(tableName = "metcon_plan")
data class MetconPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val type: MetconType,
    val durationMinutes: Int? = null,   // for AMRAP or time-capped plans
    val emomIntervalSec: Int? = null    // for EMOM
)
