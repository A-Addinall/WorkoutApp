// app/src/main/java/com/example/safitness/data/dao/ProgramDao.kt
package com.example.safitness.data.dao

import androidx.room.*
import com.example.safitness.core.Equipment
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.entities.Exercise
import com.example.safitness.data.entities.ProgramSelection
import kotlinx.coroutines.flow.Flow

data class ExerciseWithSelection(
    @Embedded val exercise: Exercise,
    val required: Boolean,
    val preferredEquipment: Equipment?,
    val targetReps: Int?,
    val targetSets: Int? = null        // <-- NEW (default keeps old call sites compiling)
)


@Dao
interface ProgramDao {

    /* -------- Date-first only -------- */

    @Transaction
    @Query("""
    SELECT e.*, ps.required, ps.preferredEquipment, ps.targetReps
    FROM Exercise e
    JOIN ProgramSelection ps ON ps.exerciseId = e.id
    WHERE ps.dateEpochDay = :epochDay
    ORDER BY ps.required DESC, e.name ASC
""")
    fun getProgramForDate(epochDay: Long): Flow<List<ExerciseWithSelection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(selection: ProgramSelection)

    @Query("DELETE FROM ProgramSelection WHERE dateEpochDay = :epochDay AND exerciseId = :exerciseId")
    suspend fun removeByDate(epochDay: Long, exerciseId: Long)

    @Query("UPDATE ProgramSelection SET required = :required WHERE dateEpochDay = :epochDay AND exerciseId = :exerciseId")
    suspend fun setRequiredByDate(epochDay: Long, exerciseId: Long, required: Boolean)

    @Query("UPDATE ProgramSelection SET preferredEquipment = :preferred WHERE dateEpochDay = :epochDay AND exerciseId = :exerciseId")
    suspend fun setPreferredByDate(epochDay: Long, exerciseId: Long, preferred: com.example.safitness.core.Equipment?)

    @Query("UPDATE ProgramSelection SET targetReps = :reps WHERE dateEpochDay = :epochDay AND exerciseId = :exerciseId")
    suspend fun setTargetRepsByDate(epochDay: Long, exerciseId: Long, reps: Int?)

    @Query("SELECT COUNT(*) FROM ProgramSelection WHERE dateEpochDay = :epochDay AND exerciseId = :exerciseId")
    suspend fun existsByDate(epochDay: Long, exerciseId: Long): Int

    @Query("SELECT targetReps FROM ProgramSelection WHERE dateEpochDay = :epochDay AND exerciseId = :exerciseId LIMIT 1")
    suspend fun getTargetRepsByDate(epochDay: Long, exerciseId: Long): Int?

    @Query("SELECT required FROM ProgramSelection WHERE dateEpochDay = :epochDay AND exerciseId = :exerciseId LIMIT 1")
    suspend fun getRequiredByDate(epochDay: Long, exerciseId: Long): Boolean?

    @Query("""
    SELECT DISTINCT e.workoutType
    FROM Exercise e
    JOIN ProgramSelection ps ON ps.exerciseId = e.id
    WHERE ps.dateEpochDay = :epochDay
""")
    suspend fun distinctTypesForDate(epochDay: Long): List<com.example.safitness.core.WorkoutType>
}
