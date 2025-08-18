package com.example.safitness.data.entities

import androidx.room.*

@Entity(
    tableName = "skill_components",
    foreignKeys = [
        ForeignKey(
            entity = SkillPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId"), Index("orderIndex")]
)
data class SkillComponentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val orderIndex: Int,
    val title: String,
    val description: String? = null,
    val testType: String? = null,       // ATTEMPTS / MAX_HOLD_SECONDS / FOR_TIME_REPS / MAX_REPS_UNBROKEN
    val targetDurationSeconds: Int? = null,
    val targetReps: Int? = null,
    val targetHoldSeconds: Int? = null
)
