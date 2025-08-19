package com.example.safitness.data.dao

import androidx.room.*
import com.example.safitness.data.entities.ExerciseEquipment
import com.example.safitness.data.entities.ExerciseMuscle

@Dao
interface ExerciseMetadataDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllMuscles(items: List<ExerciseMuscle>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllEquipment(items: List<ExerciseEquipment>)
}
