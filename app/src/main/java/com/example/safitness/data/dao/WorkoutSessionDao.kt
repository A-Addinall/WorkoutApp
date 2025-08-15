package com.example.safitness.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.safitness.data.entities.WorkoutSessionEntity

@Dao
interface WorkoutSessionDao {

    @Insert
    suspend fun insert(entity: WorkoutSessionEntity): Long

    @Query("SELECT * FROM workout_session WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WorkoutSessionEntity?
}
