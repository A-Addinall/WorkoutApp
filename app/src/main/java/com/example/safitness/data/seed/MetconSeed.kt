package com.example.safitness.data.seed

import com.example.safitness.core.MetconType
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.MetconPlan
import com.example.safitness.data.entities.MetconComponent

object MetconSeed {
    suspend fun seedDefaults(db: AppDatabase) {
        val dao = db.metconDao()
        if (dao.countPlans() > 0) return

        // FOR_TIME
        val ftId = dao.insertPlan(
            MetconPlan(
                title = "21-15-9 Thrusters & Burpees",
                type = MetconType.FOR_TIME
            )
        )
        dao.insertComponents(
            listOf(
                MetconComponent(planId = ftId, orderInPlan = 1, text = "Thrusters"),
                MetconComponent(planId = ftId, orderInPlan = 2, text = "Burpees")
            )
        )

        // AMRAP 12
        val amrapId = dao.insertPlan(
            MetconPlan(
                title = "12-min AMRAP Triplet",
                type = MetconType.AMRAP,
                durationMinutes = 12
            )
        )
        dao.insertComponents(
            listOf(
                MetconComponent(planId = amrapId, orderInPlan = 1, text = "10 Kettlebell Swings"),
                MetconComponent(planId = amrapId, orderInPlan = 2, text = "10 Push-ups"),
                MetconComponent(planId = amrapId, orderInPlan = 3, text = "200m Row")
            )
        )

        // EMOM 14
        val emomId = dao.insertPlan(
            MetconPlan(
                title = "14-min EMOM",
                type = MetconType.EMOM,
                durationMinutes = 14,
                emomIntervalSec = 60
            )
        )
        dao.insertComponents(
            listOf(
                MetconComponent(planId = emomId, orderInPlan = 1, text = "Odd: 10 Cal Row"),
                MetconComponent(planId = emomId, orderInPlan = 2, text = "Even: 15 Air Squats")
            )
        )
    }
}
