package com.example.safitness.data.entities

import androidx.room.*
import com.example.safitness.core.MuscleGroup

@Entity(
    tableName = "metcon_component_muscle",
    indices = [Index("componentId"), Index("muscle")],
    foreignKeys = [
        ForeignKey(entity = MetconComponent::class, parentColumns = ["id"], childColumns = ["componentId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class MetconComponentMuscle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val componentId: Long,
    val muscle: MuscleGroup,
    val role: String // "PRIMARY" or "SECONDARY"
)
