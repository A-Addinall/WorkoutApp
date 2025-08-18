package com.example.safitness.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.safitness.data.entities.SkillLogEntity

@Dao
interface SkillLogDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(log: SkillLogEntity): Long

    @Query("SELECT * FROM skill_logs ORDER BY createdAtEpochSec DESC LIMIT :limit")
    suspend fun recent(limit: Int = 50): List<SkillLogEntity>
}
