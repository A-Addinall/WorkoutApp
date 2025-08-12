package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.safitness.core.MetconType

@Entity(tableName = "metcon_plan")
data class MetconPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val type: MetconType,
    /** Total minutes for AMRAP/EMOM; null for FOR_TIME. */
    val durationMinutes: Int? = null,
    /** EMOM interval seconds (usually 60); null except for EMOM. */
    val emomIntervalSec: Int? = null
)
