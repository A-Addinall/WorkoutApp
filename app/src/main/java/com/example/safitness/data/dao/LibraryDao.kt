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

    @Query("""
        SELECT e.* FROM exercise e
        WHERE (:movement IS NULL OR e.movement = :movement)
          AND EXISTS (
            SELECT 1 FROM exercise_muscle em
            WHERE em.exerciseId = e.id AND em.role = 'PRIMARY' AND em.muscle IN (:muscles)
          )
          AND EXISTS (
            SELECT 1 FROM exercise_equipment ee
            WHERE ee.exerciseId = e.id AND ee.equipment IN (:equipment)
          )
        ORDER BY e.name ASC
    """)
    suspend fun filterExercises(
        movement: com.example.safitness.core.MovementPattern?,
        muscles: List<com.example.safitness.core.MuscleGroup>,
        equipment: List<com.example.safitness.core.Equipment>
    ): List<Exercise>
}
