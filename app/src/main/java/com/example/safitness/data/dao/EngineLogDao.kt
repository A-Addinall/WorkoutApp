package com.example.safitness.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.safitness.data.entities.EngineLogEntity

@Dao
interface EngineLogDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(log: EngineLogEntity): Long

    @Query("SELECT * FROM engine_log WHERE mode = :mode ORDER BY date DESC LIMIT :limit")
    suspend fun recent(mode: String, limit: Int = 20): List<EngineLogEntity>

    @Query("""
    SELECT * FROM engine_log 
    WHERE mode = :mode AND intent = :intent 
    ORDER BY date DESC 
    LIMIT :limit
  """)
    suspend fun recentByIntent(mode: String, intent: String, limit: Int = 20): List<EngineLogEntity>

    @Query("DELETE FROM engine_log")
    suspend fun clearAll()
}
