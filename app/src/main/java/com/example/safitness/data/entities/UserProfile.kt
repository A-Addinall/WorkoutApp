package com.example.safitness.data.entities

import androidx.room.*
import com.example.safitness.core.Equipment
import com.example.safitness.ml.Goal
import com.example.safitness.ml.ExperienceLevel

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Long = 1L,               // single-profile app
    val goal: Goal = Goal.STRENGTH,
    val experience: ExperienceLevel = ExperienceLevel.BEGINNER,
    val daysPerWeek: Int = 3,
    val sessionMinutes: Int = 45,
    val equipmentCsv: String = "BODYWEIGHT",
    val programWeeks: Int = 4// store as CSV for simplicity
)

/** simple helpers to convert a Set<Equipment> <-> CSV */
fun Set<Equipment>.toCsv() = joinToString(",") { it.name }
fun String.toEquipmentSet(): Set<Equipment> =
    if (isBlank()) emptySet() else split(",").mapNotNull { runCatching { Equipment.valueOf(it) }.getOrNull() }.toSet()
