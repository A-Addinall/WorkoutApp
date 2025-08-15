package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Items attached to a day plan (strength or metcon pointer).
 * Strength items point to an exerciseId; metcon items point to a planId.
 */
@Entity(
    tableName = "day_item",
    foreignKeys = [
        ForeignKey(
            entity = WeekDayPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["dayPlanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("dayPlanId")]
)
data class DayItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val dayPlanId: Long,
    val itemType: String,     // "STRENGTH" | "METCON"
    val refId: Long,          // exerciseId OR metconPlanId
    val required: Boolean = true,
    val sortOrder: Int = 0,
    val targetReps: Int? = null,        // strength only
    // NEW: flexible prescription payload (JSON blob, optional)
    val prescriptionJson: String? = null
)
