package com.example.safitness.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.safitness.data.entities.DayEngineSkillEntity

@Dao
interface DayEngineSkillDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DayEngineSkillEntity): Long

    @Query("""
        SELECT * FROM day_engine_skill
        WHERE weekIndex = :week AND dayIndex = :day
        ORDER BY orderIndex ASC, id ASC
    """)
    suspend fun forDay(week: Int, day: Int): List<DayEngineSkillEntity>

    // NEW: toggle helpers
    @Query("""
        DELETE FROM day_engine_skill
        WHERE weekIndex=:week AND dayIndex=:day
          AND kind='ENGINE' AND engineMode=:mode AND engineIntent=:intent
    """)
    suspend fun deleteEngine(week: Int, day: Int, mode: String, intent: String): Int

    @Query("""
        DELETE FROM day_engine_skill
        WHERE weekIndex=:week AND dayIndex=:day
          AND kind='SKILL' AND skill=:skill AND skillTestType=:testType
    """)
    suspend fun deleteSkill(week: Int, day: Int, skill: String, testType: String): Int
}
