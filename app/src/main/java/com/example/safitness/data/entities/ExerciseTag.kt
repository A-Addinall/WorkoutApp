package com.example.safitness.data.entities

import androidx.room.*

@Entity(
    tableName = "exercise_tag",
    indices = [Index("exerciseId"), Index("tag")],
    foreignKeys = [
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class ExerciseTag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val tag: String
)
