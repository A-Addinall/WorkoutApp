package com.example.safitness.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.safitness.core.Equipment
import com.example.safitness.data.entities.SetLog
import com.example.safitness.data.entities.WorkoutSession

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: SetLog): Long

    @Query("""
        SELECT * FROM SetLog 
        WHERE exerciseId = :exerciseId 
          AND equipment = :equipment
          AND (:reps IS NULL OR reps = :reps)
        ORDER BY id DESC 
        LIMIT :limit
    """)
    suspend fun lastSets(
        exerciseId: Long,
        equipment: Equipment,
        limit: Int = 10,
        reps: Int? = null
    ): List<SetLog>


    /* -------- Metcon helpers -------- */

    // Last time-only entry for this day (any session for that day)
    @Query("""
        SELECT sl.timeSeconds FROM SetLog sl
        JOIN WorkoutSession ws ON ws.id = sl.sessionId
        WHERE ws.dayIndex = :day
          AND sl.timeSeconds IS NOT NULL
        ORDER BY sl.id DESC
        LIMIT 1
    """)
    suspend fun lastMetconSecondsForDay(day: Int): Int?
}
