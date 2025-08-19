package com.example.safitness.data.entities

import androidx.room.*
import com.example.safitness.core.BlockType
import com.example.safitness.core.MovementPattern

@Entity(
    tableName = "metcon_component",
    indices = [
        Index(value = ["planId", "orderInPlan"], unique = true),
        Index("planId")
    ],
    foreignKeys = [ForeignKey(
        entity = MetconPlan::class,
        parentColumns = ["id"],
        childColumns = ["planId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class MetconComponent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val orderInPlan: Int,
    val text: String,
    // NEW structured fields
    val blockType: BlockType? = null,     // EMOM/AMRAP/FOR_TIME/CIRCUIT/...
    val rounds: Int? = null,
    val durationSec: Int? = null,         // AMRAP or time-cap
    val emomIntervalSec: Int? = null,     // for EMOM blocks
    val movement: MovementPattern? = null,
    val reps: Int? = null,
    val intensityType: String? = null,    // "RPE" / "%1RM" / "LOAD" / "HR_ZONE"
    val intensityValue: Float? = null     // generic numeric value (e.g., 0.75 for %1RM, 8 for RPE)
)
