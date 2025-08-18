package com.example.safitness.data.dao

import androidx.room.*
import com.example.safitness.data.entities.SkillComponentEntity
import com.example.safitness.data.entities.SkillPlanEntity

@Dao
interface SkillPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: SkillPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponents(components: List<SkillComponentEntity>)

    @Query("SELECT COUNT(*) FROM skill_plans")
    suspend fun countPlans(): Int

    @Query("SELECT * FROM skill_plans ORDER BY id")
    suspend fun getPlans(): List<SkillPlanEntity>

    @Query("SELECT * FROM skill_components WHERE planId = :planId ORDER BY orderIndex ASC")
    suspend fun getComponents(planId: Long): List<SkillComponentEntity>

    // Debug helpers (optional)
    @Query("DELETE FROM skill_components") suspend fun deleteAllComponents()
    @Query("DELETE FROM skill_plans") suspend fun deleteAllPlans()
}
