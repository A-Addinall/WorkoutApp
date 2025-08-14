package com.example.safitness.data.seed

import com.example.safitness.core.MetconType
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.MetconComponent
import com.example.safitness.data.entities.MetconPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MetconSeed {

    suspend fun seed(db: AppDatabase) = withContext(Dispatchers.IO) {
        val dao = db.metconDao()

        // Example plans
        val plans = listOf(
            MetconPlan(title = "21-15-9 Thrusters & Pull-ups", type = MetconType.FOR_TIME),
            MetconPlan(title = "12-min AMRAP DB Snatch + Burpees", type = MetconType.AMRAP, durationMinutes = 12),
            MetconPlan(title = "EMOM 10: Row + Push-ups", type = MetconType.EMOM, emomIntervalSec = 60, durationMinutes = 10)
        )
        val planIds = dao.insertPlans(plans)

        val comps = buildList {
            // Plan 1: 21-15-9
            val p1 = planIds[0]
            add(MetconComponent(planId = p1, orderInPlan = 1, text = "21 Thrusters (40/30kg)"))
            add(MetconComponent(planId = p1, orderInPlan = 2, text = "21 Pull-ups"))
            add(MetconComponent(planId = p1, orderInPlan = 3, text = "15 Thrusters"))
            add(MetconComponent(planId = p1, orderInPlan = 4, text = "15 Pull-ups"))
            add(MetconComponent(planId = p1, orderInPlan = 5, text = "9 Thrusters"))
            add(MetconComponent(planId = p1, orderInPlan = 6, text = "9 Pull-ups"))

            // Plan 2: AMRAP 12
            val p2 = planIds[1]
            add(MetconComponent(planId = p2, orderInPlan = 1, text = "10 DB Snatches (alternating)"))
            add(MetconComponent(planId = p2, orderInPlan = 2, text = "8 Burpees"))

            // Plan 3: EMOM 10
            val p3 = planIds[2]
            add(MetconComponent(planId = p3, orderInPlan = 1, text = "Odd minutes: 12/10 cal Row"))
            add(MetconComponent(planId = p3, orderInPlan = 2, text = "Even minutes: 15 Push-ups"))
        }

        dao.insertComponents(comps)
    }
}
