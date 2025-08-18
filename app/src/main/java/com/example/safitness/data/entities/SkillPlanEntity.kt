package com.example.safitness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skill_plans")
data class SkillPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,                 // e.g., "Bar Muscle-Up — Path to First Rep"
    val skill: String,                 // your Skill enum/string
    val description: String? = null,
    val defaultTestType: String? = null,
    val targetDurationSeconds: Int? = null,
    val rxNotes: String? = null,
    val scaledNotes: String? = null
)
