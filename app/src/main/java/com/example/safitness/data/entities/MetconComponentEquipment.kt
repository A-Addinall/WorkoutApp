package com.example.safitness.data.entities

import androidx.room.*
import com.example.safitness.core.Equipment

@Entity(
    tableName = "metcon_component_equipment",
    indices = [Index("componentId"), Index("equipment")],
    foreignKeys = [
        ForeignKey(entity = MetconComponent::class, parentColumns = ["id"], childColumns = ["componentId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class MetconComponentEquipment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val componentId: Long,
    val equipment: Equipment
)
