package com.example.safitness.data.repo

import com.example.safitness.data.dao.EnginePlanDao
import com.example.safitness.data.entities.EngineComponentEntity
import com.example.safitness.data.entities.EnginePlanEntity

class EnginePlanRepository(private val dao: EnginePlanDao) {
    suspend fun upsert(plan: EnginePlanEntity, comps: List<EngineComponentEntity>): Long {
        val id = dao.insertPlan(plan)
        if (comps.isNotEmpty()) dao.insertComponents(comps.map { it.copy(planId = id) })
        return id
    }
    suspend fun count() = dao.countPlans()
    suspend fun plans() = dao.getPlans()
    suspend fun components(planId: Long) = dao.getComponents(planId)
}
