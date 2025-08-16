package com.example.safitness.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.safitness.core.Equipment
import com.example.safitness.data.entities.PersonalRecord

@Dao
interface PersonalRecordDao {

    @Query("""
        SELECT MAX(value) FROM personal_record
        WHERE exerciseId = :exerciseId
          AND recordType = 'E1RM'
          AND equipment = :equipment
    """)
    suspend fun bestEstimated1RM(
        exerciseId: Long,
        equipment: Equipment
    ): Double?

    @Query("""
        SELECT MAX(value) FROM personal_record
        WHERE exerciseId = :exerciseId
          AND recordType = 'RM'
          AND equipment = :equipment
          AND reps = :reps
    """)
    suspend fun bestWeightAtReps(
        exerciseId: Long,
        equipment: Equipment,
        reps: Int
    ): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInternal(entity: PersonalRecord): Long

    @Transaction
    suspend fun upsertEstimated1RM(
        exerciseId: Long,
        equipment: Equipment,
        valueKg: Double,
        dateEpochDay: Long,
        notes: String? = null
    ): Long {
        return upsertInternal(
            PersonalRecord(
                exerciseId = exerciseId,
                recordType = "E1RM",
                value = valueKg,
                dateEpochDay = dateEpochDay,
                notes = notes,
                equipment = equipment,
                reps = null
            )
        )
    }

    @Transaction
    suspend fun upsertRepMax(
        exerciseId: Long,
        equipment: Equipment,
        reps: Int,
        valueKg: Double,
        dateEpochDay: Long,
        notes: String? = null
    ): Long {
        return upsertInternal(
            PersonalRecord(
                exerciseId = exerciseId,
                recordType = "RM",
                value = valueKg,
                dateEpochDay = dateEpochDay,
                notes = notes,
                equipment = equipment,
                reps = reps
            )
        )
    }
}
