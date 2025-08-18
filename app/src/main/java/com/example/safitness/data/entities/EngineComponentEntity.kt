package com.example.safitness.data.entities

import androidx.room.*

@Entity(
    tableName = "engine_components",
    foreignKeys = [
        ForeignKey(
            entity = EnginePlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId"), Index("orderIndex")]
)
data class EngineComponentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val orderIndex: Int,
    val title: String,
    val description: String? = null
)
