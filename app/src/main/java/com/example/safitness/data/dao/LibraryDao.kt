// app/src/main/java/com/example/safitness/data/dao/LibraryDao.kt
package com.example.safitness.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.safitness.core.Equipment
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.entities.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Query("""
        SELECT * FROM Exercise
        WHERE (:type IS NULL OR workoutType = :type)
          AND (:eq IS NULL OR primaryEquipment = :eq)
        ORDER BY name ASC
    """)
    fun getExercises(
        type: WorkoutType?,
        eq: Equipment?
    ): Flow<List<Exercise>>

    @Query("SELECT COUNT(*) FROM Exercise")
    suspend fun countExercises(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Exercise>)
}
