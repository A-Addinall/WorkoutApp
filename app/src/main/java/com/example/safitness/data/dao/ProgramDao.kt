// app/src/main/java/com/example/safitness/data/dao/ProgramDao.kt
package com.example.safitness.data.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.safitness.core.Equipment
import com.example.safitness.data.entities.Exercise
import com.example.safitness.data.entities.ProgramSelection
import kotlinx.coroutines.flow.Flow

data class ExerciseWithSelection(
    @Embedded val exercise: Exercise,
    val required: Boolean,
    val preferredEquipment: Equipment?,
    val targetReps: Int?
)

@Dao
interface ProgramDao {

    @Transaction
    @Query("""
        SELECT e.*, ps.required, ps.preferredEquipment, ps.targetReps
        FROM Exercise e
        JOIN ProgramSelection ps ON ps.exerciseId = e.id
        WHERE ps.dayIndex = :day
        ORDER BY ps.required DESC, e.name ASC
    """)
    fun getProgramForDay(day: Int): Flow<List<ExerciseWithSelection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(selection: ProgramSelection)

    @Query("DELETE FROM ProgramSelection WHERE dayIndex = :day AND exerciseId = :exerciseId")
    suspend fun remove(day: Int, exerciseId: Long)

    @Query("UPDATE ProgramSelection SET required = :required WHERE dayIndex = :day AND exerciseId = :exerciseId")
    suspend fun setRequired(day: Int, exerciseId: Long, required: Boolean)

    @Query("UPDATE ProgramSelection SET preferredEquipment = :preferred WHERE dayIndex = :day AND exerciseId = :exerciseId")
    suspend fun setPreferred(day: Int, exerciseId: Long, preferred: Equipment?)

    @Query("UPDATE ProgramSelection SET targetReps = :reps WHERE dayIndex = :day AND exerciseId = :exerciseId")
    suspend fun setTargetReps(day: Int, exerciseId: Long, reps: Int?)
}
