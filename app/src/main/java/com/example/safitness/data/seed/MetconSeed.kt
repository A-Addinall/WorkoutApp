package com.example.safitness.data.seed

import androidx.room.withTransaction
import com.example.safitness.core.MetconType
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.MetconComponent
import com.example.safitness.data.entities.MetconPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Idempotent + convergent seeding using stable keys.
 * - Re-runnable on every app start.
 * - Adds missing rows, updates changed fields, and (optionally) prunes extra components.
 */
object MetconSeed {

    private data class PlanDef(
        val key: String,
        val title: String,
        val type: MetconType,
        val duration: Int?,          // minutes (AMRAP total / EMOM total / FOR_TIME estimate)
        val emom: Int? = null,       // seconds; EMOM only
        val archived: Boolean = false
    )
    suspend fun seed(db: AppDatabase) = seedOrUpdate(db)
    suspend fun seedOrUpdate(db: AppDatabase) = withContext(Dispatchers.IO) {
        val dao = db.metconDao()

        db.withTransaction {
            val defs = listOf(
                PlanDef("CINDY_AMRAP20", "Cindy (AMRAP 20)", MetconType.AMRAP, 20),

                // --- FOR_TIME (include estimates for filtering) ---
                PlanDef("HELEN_FT", "Helen (For Time)", MetconType.FOR_TIME, 12),
                PlanDef("CHIPPER_50_40_30_20_10_FT", "Chipper 50-40-30-20-10", MetconType.FOR_TIME, 18),
                PlanDef("FT_5X400M_DL", "For Time: 5 x 400 m + Deadlifts", MetconType.FOR_TIME, 25),
                PlanDef("KAREN_FT", "Karen (For Time)", MetconType.FOR_TIME, 10),
                PlanDef("FT_4R_ROW_PP", "For Time: 4 Rounds Row & Push Press", MetconType.FOR_TIME, 22),

                // --- Timed formats (use total duration) ---
                PlanDef("EMOM12_BIKE_KB_TTB", "EMOM 12: Bike/KB/TTB", MetconType.EMOM, 12, emom = 60),
                PlanDef("AMRAP16_BOX_DBTHR_SIT", "AMRAP 16: Box/DB Thruster/Sit-up", MetconType.AMRAP, 16),
                PlanDef("EMOM20_ROW_BUR_SQ", "EMOM 20: Row/Burpees + Squats", MetconType.EMOM, 20, emom = 60),
                PlanDef("AMRAP10_WB_BJ", "AMRAP 10: Wall Balls & Box Jumps", MetconType.AMRAP, 10),
                PlanDef("EMOM15_CLN_TTB", "EMOM 15: Clean + TTB", MetconType.EMOM, 15, emom = 60),
                PlanDef("AMRAP18_RUN_BUR_SN", "AMRAP 18: Run/Burpees/DB Snatch", MetconType.AMRAP, 18)
            )

            // 1) Insert-or-ignore plans, then resolve IDs by key; update rows to converge with seed
            val plans = defs.map { d ->
                MetconPlan(
                    canonicalKey = d.key,
                    title = d.title,
                    type = d.type,
                    durationMinutes = d.duration,
                    emomIntervalSec = d.emom,
                    isArchived = d.archived
                )
            }

            val inserted = dao.insertPlansIgnore(plans)
            val idByKey = defs.indices.associate { i ->
                val id = if (inserted[i] != -1L) inserted[i] else dao.getPlanIdByKey(defs[i].key)
                defs[i].key to id
            }
            defs.forEach { d ->
                dao.updatePlanByKey(d.key, d.title, d.type, d.duration, d.emom, d.archived)
            }

            // 2) Build components declaratively; (planId, orderInPlan) is the identity for a row
            val comps = buildList {
                fun addLine(key: String, order: Int, text: String) =
                    add(MetconComponent(planId = idByKey.getValue(key), orderInPlan = order, text = text))

                // Cindy
                addLine("CINDY_AMRAP20", 1, "5 Pull-ups")
                addLine("CINDY_AMRAP20", 2, "10 Push-ups")
                addLine("CINDY_AMRAP20", 3, "15 Air Squats")

                // Helen — 3 rounds
                addLine("HELEN_FT", 1, "Run 400 m")
                addLine("HELEN_FT", 2, "21 Kettlebell Swings (24/16 kg)")
                addLine("HELEN_FT", 3, "12 Pull-ups")
                addLine("HELEN_FT", 4, "Repeat for 3 rounds")

                // Chipper 50-40-30-20-10
                addLine("CHIPPER_50_40_30_20_10_FT", 1, "50 Box Jumps (24/20 in)")
                addLine("CHIPPER_50_40_30_20_10_FT", 2, "40 Kettlebell Swings (24/16 kg)")
                addLine("CHIPPER_50_40_30_20_10_FT", 3, "30 Toes-to-Bar")
                addLine("CHIPPER_50_40_30_20_10_FT", 4, "20 Burpees")
                addLine("CHIPPER_50_40_30_20_10_FT", 5, "10 Clean & Jerk (60/40 kg)")

                // For Time: 5 × 400 m + Deadlifts — 5 rounds
                addLine("FT_5X400M_DL", 1, "Run 400 m")
                addLine("FT_5X400M_DL", 2, "15 Deadlifts (60/40 kg)")
                addLine("FT_5X400M_DL", 3, "Repeat for 5 rounds")

                // Karen
                addLine("KAREN_FT", 1, "150 Wall Balls (9/6 kg to 10/9 ft)")

                // For Time: 4 Rounds Row & Push Press
                addLine("FT_4R_ROW_PP", 1, "Row 500 m")
                addLine("FT_4R_ROW_PP", 2, "12 Push Press (40/30 kg)")
                addLine("FT_4R_ROW_PP", 3, "Repeat for 4 rounds")

                // EMOM/AMRAPs
                addLine("EMOM12_BIKE_KB_TTB", 1, "Minute 1: 14/12 cal Bike")
                addLine("EMOM12_BIKE_KB_TTB", 2, "Minute 2: 15 Kettlebell Swings (24/16 kg)")
                addLine("EMOM12_BIKE_KB_TTB", 3, "Minute 3: 10 Toes-to-Bar")

                addLine("AMRAP16_BOX_DBTHR_SIT", 1, "8 Box Jump Overs (24/20 in)")
                addLine("AMRAP16_BOX_DBTHR_SIT", 2, "10 Dumbbell Thrusters (2×15 kg)")
                addLine("AMRAP16_BOX_DBTHR_SIT", 3, "12 Sit-ups")

                addLine("EMOM20_ROW_BUR_SQ", 1, "Odd minutes: 12/10 cal Row")
                addLine("EMOM20_ROW_BUR_SQ", 2, "Even minutes: 10 Burpees + 20 Air Squats")

                addLine("AMRAP10_WB_BJ", 1, "10 Wall Balls (9/6 kg)")
                addLine("AMRAP10_WB_BJ", 2, "10 Box Jumps (24/20 in)")

                addLine("EMOM15_CLN_TTB", 1, "Minute 1: 1–3 Power Clean (build)")
                addLine("EMOM15_CLN_TTB", 2, "Minute 2: 10 Toes-to-Bar")
                addLine("EMOM15_CLN_TTB", 3, "Minute 3: 12 Hand-release Push-ups")

                addLine("AMRAP18_RUN_BUR_SN", 1, "Run 200 m")
                addLine("AMRAP18_RUN_BUR_SN", 2, "12 Burpees")
                addLine("AMRAP18_RUN_BUR_SN", 3, "12 Dumbbell Snatches (alternating)")
            }

            // 3) Upsert + converge components, then (optionally) prune extras
            dao.insertComponentsIgnore(comps)
            comps.forEach { c -> dao.updateComponentText(c.planId, c.orderInPlan, c.text) }

            // Optional prune: keep DB exactly matching the seed
            val ordersByPlan = comps.groupBy { it.planId }.mapValues { (_, rows) ->
                rows.map { it.orderInPlan }
            }
            ordersByPlan.forEach { (planId, validOrders) ->
                if (validOrders.isEmpty()) dao.deleteAllComponentsForPlan(planId)
                else dao.deleteComponentsNotIn(planId, validOrders)
            }
        }
    }
}
