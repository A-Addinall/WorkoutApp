package com.example.safitness.data.dao

import androidx.room.*
import com.example.safitness.data.entities.EngineComponentEntity
import com.example.safitness.data.entities.EnginePlanEntity

@Dao
interface EnginePlanDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlan(plan: EnginePlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertComponents(components: List<EngineComponentEntity>)

    @Query("SELECT COUNT(*) FROM engine_plans")
    suspend fun countPlans(): Int

    @Query("SELECT * FROM engine_plans ORDER BY id")
    suspend fun getPlans(): List<EnginePlanEntity>

    @Query("SELECT * FROM engine_components WHERE planId = :planId ORDER BY orderIndex ASC")
    suspend fun getComponents(planId: Long): List<EngineComponentEntity>

    // Debug helpers (optional)
    @Query("DELETE FROM engine_components") suspend fun deleteAllComponents()
    @Query("DELETE FROM engine_plans") suspend fun deleteAllPlans()
}
