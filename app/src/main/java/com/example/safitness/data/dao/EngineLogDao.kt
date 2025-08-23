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

    @Query("SELECT * FROM engine_logs ORDER BY createdAtEpochSec DESC LIMIT :limit")
    suspend fun recent(limit: Int = 50): List<EngineLogEntity>

    @Query("""
    SELECT * FROM engine_logs
    WHERE dateEpochDay = :epochDay
    ORDER BY createdAtEpochSec DESC
    LIMIT :limit
""")
    suspend fun recentForDate(epochDay: Long, limit: Int = 50): List<EngineLogEntity>

}
