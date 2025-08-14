package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// MetconComponent.kt
@Entity(
    tableName = "metcon_component",
    indices = [
        Index("planId"),
        Index(value = ["planId", "orderInPlan"], unique = true) // <— add
    ],
    foreignKeys = [ForeignKey(
        entity = MetconPlan::class,
        parentColumns = ["id"],
        childColumns = ["planId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class MetconComponent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val planId: Long,
    val orderInPlan: Int,
    val text: String
)
