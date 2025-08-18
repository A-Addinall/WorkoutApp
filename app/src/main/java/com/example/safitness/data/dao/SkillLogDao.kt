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

    @Query("""
    SELECT * FROM skill_log 
    WHERE skill = :skill AND testType = :testType 
    ORDER BY date DESC 
    LIMIT :limit
  """)
    suspend fun recent(skill: String, testType: String, limit: Int = 20): List<SkillLogEntity>

    @Query("DELETE FROM skill_log")
    suspend fun clearAll()
}
