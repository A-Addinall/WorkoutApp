package com.example.safitness.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.safitness.data.entities.PersonalRecord

@Dao
interface PersonalRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: PersonalRecord): Long

    @Query("SELECT * FROM personal_record WHERE exerciseId = :exerciseId ORDER BY value DESC, date DESC LIMIT 1")
    suspend fun bestForExercise(exerciseId: Long): PersonalRecord?
}
