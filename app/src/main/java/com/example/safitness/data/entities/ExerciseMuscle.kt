package com.example.safitness.data.entities

import androidx.room.*
import com.example.safitness.core.MuscleGroup

@Entity(
    tableName = "exercise_muscle",
    indices = [Index("exerciseId"), Index("muscle")],
    foreignKeys = [
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class ExerciseMuscle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val muscle: MuscleGroup,
    val role: String // "PRIMARY" or "SECONDARY"
)
