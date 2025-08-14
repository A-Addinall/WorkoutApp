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
        val plans = listOf(
            MetconPlan(title = "Cindy (AMRAP 20)", type = MetconType.AMRAP, durationMinutes = 20),

            // — FOR_TIME: add estimated durations for filtering —
            MetconPlan(
                title = "Helen (For Time)",
                type = MetconType.FOR_TIME,
                durationMinutes = 12
            ),                // typical 9–15
            MetconPlan(
                title = "Chipper 50-40-30-20-10",
                type = MetconType.FOR_TIME,
                durationMinutes = 18
            ),         // typical 15–25
            MetconPlan(
                title = "For Time: 5 x 400 m + Deadlifts",
                type = MetconType.FOR_TIME,
                durationMinutes = 25
            ), // typical 20–30
            MetconPlan(
                title = "Karen (For Time)",
                type = MetconType.FOR_TIME,
                durationMinutes = 10
            ),                // typical 6–12
            MetconPlan(
                title = "For Time: 4 Rounds Row & Push Press",
                type = MetconType.FOR_TIME,
                durationMinutes = 22
            ),

            // EMOM/AMRAP keep using duration or interval as you already do
            MetconPlan(
                title = "EMOM 12: Bike/KB/TTB",
                type = MetconType.EMOM,
                emomIntervalSec = 60,
                durationMinutes = 12
            ),
            MetconPlan(
                title = "AMRAP 16: Box/DB Thruster/Sit-up",
                type = MetconType.AMRAP,
                durationMinutes = 16
            ),
            MetconPlan(
                title = "EMOM 20: Row/Burpees + Squats",
                type = MetconType.EMOM,
                emomIntervalSec = 60,
                durationMinutes = 20
            ),
            MetconPlan(
                title = "AMRAP 10: Wall Balls & Box Jumps",
                type = MetconType.AMRAP,
                durationMinutes = 10
            ),
            MetconPlan(
                title = "EMOM 15: Clean + TTB",
                type = MetconType.EMOM,
                emomIntervalSec = 60,
                durationMinutes = 15
            ),
            MetconPlan(
                title = "AMRAP 18: Run/Burpees/DB Snatch",
                type = MetconType.AMRAP,
                durationMinutes = 18
            )
        )

// Insert-or-ignore, then UPDATE IDs by resolving ignored rows via title
        val inserted = dao.insertPlansIgnore(plans) // returns -1L for ignored
        val planIds = plans.indices.map { i ->
            val id = inserted[i]
            if (id != -1L) id else dao.getPlanIdByTitle(plans[i].title)
        }

// Shorthand for components
        val p1 = planIds[0];
        val p2 = planIds[1];
        val p3 = planIds[2];
        val p4 = planIds[3]
        val p5 = planIds[4];
        val p6 = planIds[5];
        val p7 = planIds[6];
        val p8 = planIds[7]
        val p9 = planIds[8];
        val p10 = planIds[9];
        val p11 = planIds[10];
        val p12 = planIds[11]

        val comps = buildList {
            // 1) Cindy (AMRAP 20)
            add(MetconComponent(planId = p1, orderInPlan = 1, text = "5 Pull-ups"))
            add(MetconComponent(planId = p1, orderInPlan = 2, text = "10 Push-ups"))
            add(MetconComponent(planId = p1, orderInPlan = 3, text = "15 Air Squats"))

            // 2) Helen (For Time) — 3 rounds
            add(MetconComponent(planId = p2, orderInPlan = 1, text = "Run 400 m"))
            add(
                MetconComponent(
                    planId = p2,
                    orderInPlan = 2,
                    text = "21 Kettlebell Swings (24/16 kg)"
                )
            )
            add(MetconComponent(planId = p2, orderInPlan = 3, text = "12 Pull-ups"))
            add(MetconComponent(planId = p2, orderInPlan = 4, text = "Repeat for 3 rounds"))

            // 3) Chipper 50-40-30-20-10 (For Time)
            add(MetconComponent(planId = p3, orderInPlan = 1, text = "50 Box Jumps (24/20 in)"))
            add(
                MetconComponent(
                    planId = p3,
                    orderInPlan = 2,
                    text = "40 Kettlebell Swings (24/16 kg)"
                )
            )
            add(MetconComponent(planId = p3, orderInPlan = 3, text = "30 Toes-to-Bar"))
            add(MetconComponent(planId = p3, orderInPlan = 4, text = "20 Burpees"))
            add(MetconComponent(planId = p3, orderInPlan = 5, text = "10 Clean & Jerk (60/40 kg)"))

            // 4) EMOM 12: Bike/KB/TTB
            add(MetconComponent(planId = p4, orderInPlan = 1, text = "Minute 1: 14/12 cal Bike"))
            add(
                MetconComponent(
                    planId = p4,
                    orderInPlan = 2,
                    text = "Minute 2: 15 Kettlebell Swings (24/16 kg)"
                )
            )
            add(MetconComponent(planId = p4, orderInPlan = 3, text = "Minute 3: 10 Toes-to-Bar"))

            // 5) For Time: 5 x 400 m + Deadlifts
            add(MetconComponent(planId = p5, orderInPlan = 1, text = "Run 400 m"))
            add(MetconComponent(planId = p5, orderInPlan = 2, text = "15 Deadlifts (60/40 kg)"))
            add(MetconComponent(planId = p5, orderInPlan = 3, text = "Repeat for 5 rounds"))

            // 6) AMRAP 16: Box/DB Thruster/Sit-up
            add(MetconComponent(planId = p6, orderInPlan = 1, text = "8 Box Jump Overs (24/20 in)"))
            add(
                MetconComponent(
                    planId = p6,
                    orderInPlan = 2,
                    text = "10 Dumbbell Thrusters (2×15 kg)"
                )
            )
            add(MetconComponent(planId = p6, orderInPlan = 3, text = "12 Sit-ups"))

            // 7) EMOM 20: Row/Burpees + Squats
            add(MetconComponent(planId = p7, orderInPlan = 1, text = "Odd minutes: 12/10 cal Row"))
            add(
                MetconComponent(
                    planId = p7,
                    orderInPlan = 2,
                    text = "Even minutes: 10 Burpees + 20 Air Squats"
                )
            )

            // 8) Karen (For Time)
            add(
                MetconComponent(
                    planId = p8,
                    orderInPlan = 1,
                    text = "150 Wall Balls (9/6 kg to 10/9 ft)"
                )
            )

            // 9) AMRAP 10: Wall Balls & Box Jumps
            add(MetconComponent(planId = p9, orderInPlan = 1, text = "10 Wall Balls (9/6 kg)"))
            add(MetconComponent(planId = p9, orderInPlan = 2, text = "10 Box Jumps (24/20 in)"))

            // 10) EMOM 15: Clean + TTB
            add(
                MetconComponent(
                    planId = p10,
                    orderInPlan = 1,
                    text = "Minute 1: 1–3 Power Clean (build)"
                )
            )
            add(MetconComponent(planId = p10, orderInPlan = 2, text = "Minute 2: 10 Toes-to-Bar"))
            add(
                MetconComponent(
                    planId = p10,
                    orderInPlan = 3,
                    text = "Minute 3: 12 Hand-release Push-ups"
                )
            )

            // 11) For Time: 4 Rounds Row & Push Press
            add(MetconComponent(planId = p11, orderInPlan = 1, text = "Row 500 m"))
            add(MetconComponent(planId = p11, orderInPlan = 2, text = "12 Push Press (40/30 kg)"))
            add(MetconComponent(planId = p11, orderInPlan = 3, text = "Repeat for 4 rounds"))

            // 12) AMRAP 18: Run/Burpees/DB Snatch
            add(MetconComponent(planId = p12, orderInPlan = 1, text = "Run 200 m"))
            add(MetconComponent(planId = p12, orderInPlan = 2, text = "12 Burpees"))
            add(
                MetconComponent(
                    planId = p12,
                    orderInPlan = 3,
                    text = "12 Dumbbell Snatches (alternating)"
                )
            )
        }

        dao.insertComponentsIgnore(comps)
    }
    suspend fun seedOrUpdate(db: AppDatabase) = withContext(Dispatchers.IO) {
        // simply reuse the idempotent logic you already wrote in seed(...)
        seed(db)
    }
}
