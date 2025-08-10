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
        WHERE exerciseId = :exerciseId AND equipment = :equipment 
        ORDER BY id DESC 
        LIMIT :limit
    """)
    suspend fun lastSets(
        exerciseId: Long,
        equipment: Equipment,
        limit: Int = 10
    ): List<SetLog>
}
