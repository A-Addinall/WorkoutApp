package com.example.safitness.data.entities

import androidx.room.*
import com.example.safitness.core.Equipment

@Entity(
    tableName = "exercise_equipment",
    indices = [Index("exerciseId"), Index("equipment")],
    foreignKeys = [
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class ExerciseEquipment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val equipment: Equipment
)
