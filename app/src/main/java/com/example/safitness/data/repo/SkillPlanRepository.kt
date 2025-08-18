package com.example.safitness.data.repo

import com.example.safitness.data.dao.SkillPlanDao
import com.example.safitness.data.entities.SkillComponentEntity
import com.example.safitness.data.entities.SkillPlanEntity

class SkillPlanRepository(private val dao: SkillPlanDao) {
    suspend fun upsert(plan: SkillPlanEntity, comps: List<SkillComponentEntity>): Long {
        val id = dao.insertPlan(plan)
        if (comps.isNotEmpty()) dao.insertComponents(comps.map { it.copy(planId = id) })
        return id
    }
    suspend fun count() = dao.countPlans()
    suspend fun plans() = dao.getPlans()
    suspend fun components(planId: Long) = dao.getComponents(planId)
}
