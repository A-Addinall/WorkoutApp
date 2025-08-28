package com.example.safitness.data.seed

import androidx.room.withTransaction
import com.example.safitness.core.*
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.MetconComponent
import com.example.safitness.data.entities.MetconPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extra Metcon seeds to add variety. This file ONLY adds; it does not remove.
 * It assumes the same DAO API as MetconSeed: insertPlansIgnore, updatePlanByKey, getPlanIdByKey, insertComponentsIgnore.
 *
 * Patterns avoid exotic enum values and stick to MovementPattern.{SQUAT, HINGE, VERTICAL_PULL, HORIZONTAL_PUSH, GAIT}
 * so it remains compatible with lean installs.
 */
object MetconSeed {

    private data class PlanDef(
        val key: String,
        val title: String,
        val type: MetconType,
        val duration: Int?,
        val emom: Int? = null,
        val archived: Boolean = false
    )

    suspend fun seedOrUpdate(db: AppDatabase) = withContext(Dispatchers.IO) {
        val dao = db.metconDao()

        db.withTransaction {
            val defs = listOf(
                // For Time

                PlanDef("FORTIME_FRAN_21_15_9", "Fran — For Time 21-15-9", MetconType.FOR_TIME, 8),
                PlanDef("FORTIME_DT_5RFT", "DT — 5 Rounds For Time", MetconType.FOR_TIME, 15),
                PlanDef("CHIPPER_RUN_WB_STEP_PU_PU", "Chipper — Run, Wall Balls, Step-ups, Push-ups, Pull-ups", MetconType.FOR_TIME, 22),
                PlanDef("FORTIME_21_15_9_KBS_SITUP", "For Time: 21–15–9 KB Swings + Sit-ups", MetconType.FOR_TIME, 10),

                // AMRAP
                PlanDef("AMRAP12_BW_TRIPLET", "AMRAP 12: Push-ups / Air Squats / Sit-ups", MetconType.AMRAP, 12),
                PlanDef("AMRAP12_BW_TRIPLET", "AMRAP 12: Push-ups / Air Squats / Sit-ups", MetconType.AMRAP, 12),
                PlanDef("AMRAP20_CINDY", "Cindy (AMRAP 20)", MetconType.AMRAP, 20),
                PlanDef("CHIPPER_RUN_WB_STEP_PU_PU", "Chipper — Run, Wall Balls, Step-ups, Push-ups, Pull-ups", MetconType.FOR_TIME, 22),
                PlanDef("AMRAP16_DB_COUPLET", "AMRAP 16: Alt DB Snatch + Step-ups", MetconType.AMRAP, 16),
                PlanDef("AMRAP36_ENGINE_BUILDER_YGIG", "Engine Builder (Pairs YGIG) (Row+Movement per Rnd)— AMRAP 36", MetconType.AMRAP, duration = 36),

                // EMOM / Intervals
                PlanDef("EMOM24_ROW_BURPEE_KBS_REST", "EMOM 24: Row / Burpees / KB Swings / Rest", MetconType.EMOM, 24, emom = 60),
                PlanDef("EMOM20_PU_PUSHUP_SQUAT", "EMOM 20: Pull-ups / Push-ups / Squats", MetconType.EMOM, 20, emom = 60),
                PlanDef("EMOM18_PU_THRUSTERS_REST", "EMOM 18: Pull-ups / Thrusters / Rest", MetconType.EMOM, 18, emom = 60),

                // Chippers / Ladders
                PlanDef("CHIPPER_ROW_KBS_PU_RUN", "For Time: 1k Row, 50 KB Swings, 30 Pull-ups, 800m Run", MetconType.FOR_TIME, 20),
                PlanDef("FORTIME_10_TO_1_DL_BURPEE", "For Time: 10→1 Deadlift + Burpee", MetconType.FOR_TIME, 16),

                // Simple intervals without machines
                PlanDef("BURPEE_SQUAT", "Tabata: Burpees + Air Squats", MetconType.EMOM, 8) // 8 rounds 20/10
                        // --- More AMRAP / EMOM / For-Time variants (follow existing variables) ---
            )

            val plans = defs.map {
                MetconPlan(
                    canonicalKey = it.key,
                    title = it.title,
                    type = it.type,
                    durationMinutes = it.duration,
                    emomIntervalSec = it.emom,
                    isArchived = it.archived
                )
            }

            // Insert-if-absent, then soft-update titles/types so re-seeding is idempotent.
            dao.insertPlansIgnore(plans)
            defs.forEach { d ->
                dao.updatePlanByKey(d.key, d.title, d.type, d.duration, d.emom, d.archived)
            }
            val idByKey = defs.associate { d -> d.key to dao.getPlanIdByKey(d.key) }

            val comps = listOf(
                // Cindy: 5 PU, 10 Push-ups, 15 Air Squats AMRAP 20
                MetconComponent(planId = idByKey.getValue("AMRAP20_CINDY"), orderInPlan = 1, text = "5 Pull-ups", blockType = BlockType.AMRAP, movement = MovementPattern.VERTICAL_PULL, reps = 5),
                MetconComponent(planId = idByKey.getValue("AMRAP20_CINDY"), orderInPlan = 2, text = "10 Push-ups", blockType = BlockType.AMRAP, movement = MovementPattern.HORIZONTAL_PUSH, reps = 10),
                MetconComponent(planId = idByKey.getValue("AMRAP20_CINDY"), orderInPlan = 3, text = "15 Air Squats", blockType = BlockType.AMRAP, movement = MovementPattern.SQUAT, reps = 15),

                // Fran 21-15-9 Thrusters + Pull-ups
                MetconComponent(planId = idByKey.getValue("FORTIME_FRAN_21_15_9"), orderInPlan = 1, text = "21-15-9 Thrusters", blockType = BlockType.FOR_TIME, movement = MovementPattern.SQUAT, reps = 45),
                MetconComponent(planId = idByKey.getValue("FORTIME_FRAN_21_15_9"), orderInPlan = 2, text = "21-15-9 Pull-ups", blockType = BlockType.FOR_TIME, movement = MovementPattern.VERTICAL_PULL, reps = 45),

                // DT 5RFT: 12 DL, 9 HPC, 6 PJ
                MetconComponent(planId = idByKey.getValue("FORTIME_DT_5RFT"), orderInPlan = 1, text = "12 Deadlifts", blockType = BlockType.FOR_TIME, movement = MovementPattern.HINGE, reps = 12),
                MetconComponent(planId = idByKey.getValue("FORTIME_DT_5RFT"), orderInPlan = 2, text = "9 Hang Power Cleans", blockType = BlockType.FOR_TIME, movement = MovementPattern.HINGE, reps = 9),
                MetconComponent(planId = idByKey.getValue("FORTIME_DT_5RFT"), orderInPlan = 3, text = "6 Push Jerks", blockType = BlockType.FOR_TIME, movement = MovementPattern.HORIZONTAL_PUSH, reps = 6),

                // EMOM 24 rotating: Row / Burpees / KB Swings / Rest
                MetconComponent(planId = idByKey.getValue("EMOM24_ROW_BURPEE_KBS_REST"), orderInPlan = 1, text = "Min 1: 12/10 cal Row", blockType = BlockType.EMOM, movement = MovementPattern.GAIT),
                MetconComponent(planId = idByKey.getValue("EMOM24_ROW_BURPEE_KBS_REST"), orderInPlan = 2, text = "Min 2: 12 Burpees", blockType = BlockType.EMOM, movement = MovementPattern.HORIZONTAL_PUSH, reps = 12),
                MetconComponent(planId = idByKey.getValue("EMOM24_ROW_BURPEE_KBS_REST"), orderInPlan = 3, text = "Min 3: 20 Kettlebell Swings", blockType = BlockType.EMOM, movement = MovementPattern.HINGE, reps = 20),
                MetconComponent(planId = idByKey.getValue("EMOM24_ROW_BURPEE_KBS_REST"), orderInPlan = 4, text = "Min 4: Rest", blockType = BlockType.EMOM, movement = MovementPattern.SQUAT),

                // EMOM 20 rotating: Pull-ups / Push-ups / Squats
                MetconComponent(planId = idByKey.getValue("EMOM20_PU_PUSHUP_SQUAT"), orderInPlan = 1, text = "Min 1: 6–10 Pull-ups", blockType = BlockType.EMOM, movement = MovementPattern.VERTICAL_PULL, reps = 8),
                MetconComponent(planId = idByKey.getValue("EMOM20_PU_PUSHUP_SQUAT"), orderInPlan = 2, text = "Min 2: 12–20 Push-ups", blockType = BlockType.EMOM, movement = MovementPattern.HORIZONTAL_PUSH, reps = 15),
                MetconComponent(planId = idByKey.getValue("EMOM20_PU_PUSHUP_SQUAT"), orderInPlan = 3, text = "Min 3: 18–25 Air Squats", blockType = BlockType.EMOM, movement = MovementPattern.SQUAT, reps = 20),
                MetconComponent(planId = idByKey.getValue("EMOM20_PU_PUSHUP_SQUAT"), orderInPlan = 4, text = "Min 4: Rest", blockType = BlockType.EMOM, movement = MovementPattern.SQUAT),

                // For Time chipper
                MetconComponent(planId = idByKey.getValue("CHIPPER_ROW_KBS_PU_RUN"), orderInPlan = 1, text = "Row 1,000m", blockType = BlockType.FOR_TIME, movement = MovementPattern.GAIT),
                MetconComponent(planId = idByKey.getValue("CHIPPER_ROW_KBS_PU_RUN"), orderInPlan = 2, text = "50 Kettlebell Swings", blockType = BlockType.FOR_TIME, movement = MovementPattern.HINGE, reps = 50),
                MetconComponent(planId = idByKey.getValue("CHIPPER_ROW_KBS_PU_RUN"), orderInPlan = 3, text = "30 Pull-ups", blockType = BlockType.FOR_TIME, movement = MovementPattern.VERTICAL_PULL, reps = 30),
                MetconComponent(planId = idByKey.getValue("CHIPPER_ROW_KBS_PU_RUN"), orderInPlan = 4, text = "Run 800m", blockType = BlockType.FOR_TIME, movement = MovementPattern.GAIT),

                // For Time 10→1 DL + Burpee
                MetconComponent(planId = idByKey.getValue("FORTIME_10_TO_1_DL_BURPEE"), orderInPlan = 1, text = "Deadlift ladder 10→1", blockType = BlockType.FOR_TIME, movement = MovementPattern.HINGE, reps = 55),
                MetconComponent(planId = idByKey.getValue("FORTIME_10_TO_1_DL_BURPEE"), orderInPlan = 2, text = "Burpee ladder 10→1", blockType = BlockType.FOR_TIME, movement = MovementPattern.HORIZONTAL_PUSH, reps = 55),

                // Tabata (16 x 20s work / 10s rest alternating two moves)
                MetconComponent(planId = idByKey.getValue("BURPEE_SQUAT"), orderInPlan = 1, text = "Alt: 60s Burpees / 60s Air Squats x 4", blockType = BlockType.EMOM, movement = MovementPattern.HORIZONTAL_PUSH, reps = 80),

                // --- AMRAP 12: Push-ups / Air Squats / Sit-ups ---
                MetconComponent(planId = idByKey.getValue("AMRAP12_BW_TRIPLET"), orderInPlan = 1, text = "8 Push-ups", blockType = BlockType.AMRAP, movement = MovementPattern.HORIZONTAL_PUSH, reps = 8),
                MetconComponent(planId = idByKey.getValue("AMRAP12_BW_TRIPLET"), orderInPlan = 2, text = "10 Air Squats", blockType = BlockType.AMRAP, movement = MovementPattern.SQUAT, reps = 10),
                MetconComponent(planId = idByKey.getValue("AMRAP12_BW_TRIPLET"), orderInPlan = 3, text = "12 Sit-ups", blockType = BlockType.AMRAP, movement = MovementPattern.CORE, reps = 12),

                // --- EMOM 18: Pull-ups / Thrusters / Rest ---
                MetconComponent(planId = idByKey.getValue("EMOM18_PU_THRUSTERS_REST"), orderInPlan = 1, text = "Min 1: 6–10 Pull-ups", blockType = BlockType.EMOM, movement = MovementPattern.VERTICAL_PULL, reps = 8),
                MetconComponent(planId = idByKey.getValue("EMOM18_PU_THRUSTERS_REST"), orderInPlan = 2, text = "Min 2: 10 Thrusters", blockType = BlockType.EMOM, movement = MovementPattern.SQUAT, reps = 10),
                MetconComponent(planId = idByKey.getValue("EMOM18_PU_THRUSTERS_REST"), orderInPlan = 3, text = "Min 3: Rest", blockType = BlockType.EMOM, movement = MovementPattern.CORE, reps = null),

                // --- For Time: 21–15–9 KB Swings + Sit-ups ---
                MetconComponent(planId = idByKey.getValue("FORTIME_21_15_9_KBS_SITUP"), orderInPlan = 1, text = "Kettlebell Swings 21–15–9", blockType = BlockType.FOR_TIME, movement = MovementPattern.HINGE, reps = 45),
                MetconComponent(planId = idByKey.getValue("FORTIME_21_15_9_KBS_SITUP"), orderInPlan = 2, text = "Sit-ups 21–15–9", blockType = BlockType.FOR_TIME, movement = MovementPattern.CORE, reps = 45),

                // --- Chipper: Run 800, 50 Wall Balls, 40 Step-ups, 30 Push-ups, 20 Pull-ups ---
                MetconComponent(planId = idByKey.getValue("CHIPPER_RUN_WB_STEP_PU_PU"), orderInPlan = 1, text = "Run 800 m", blockType = BlockType.FOR_TIME, movement = MovementPattern.GAIT, reps = null),
                MetconComponent(planId = idByKey.getValue("CHIPPER_RUN_WB_STEP_PU_PU"), orderInPlan = 2, text = "50 Wall Balls", blockType = BlockType.FOR_TIME, movement = MovementPattern.SQUAT, reps = 50),
                MetconComponent(planId = idByKey.getValue("CHIPPER_RUN_WB_STEP_PU_PU"), orderInPlan = 3, text = "40 Step-ups", blockType = BlockType.FOR_TIME, movement = MovementPattern.SQUAT, reps = 40),
                MetconComponent(planId = idByKey.getValue("CHIPPER_RUN_WB_STEP_PU_PU"), orderInPlan = 4, text = "30 Push-ups", blockType = BlockType.FOR_TIME, movement = MovementPattern.HORIZONTAL_PUSH, reps = 30),
                MetconComponent(planId = idByKey.getValue("CHIPPER_RUN_WB_STEP_PU_PU"), orderInPlan = 5, text = "20 Pull-ups", blockType = BlockType.FOR_TIME, movement = MovementPattern.VERTICAL_PULL, reps = 20),

                // --- AMRAP 16: Alt DB Snatch + Step-ups ---
                MetconComponent(planId = idByKey.getValue("AMRAP16_DB_COUPLET"), orderInPlan = 1, text = "10 Alt DB Snatch", blockType = BlockType.AMRAP, movement = MovementPattern.HINGE, reps = 10),
                MetconComponent(planId = idByKey.getValue("AMRAP16_DB_COUPLET"), orderInPlan = 2, text = "12 Step-ups", blockType = BlockType.AMRAP, movement = MovementPattern.SQUAT, reps = 12),

                //--- AMRAP36_ENGINE_BUILDER_YGIG ---
                MetconComponent(planId = idByKey.getValue("AMRAP36_ENGINE_BUILDER_YGIG"), orderInPlan = 1, text = "15/12 cal Bike", blockType = BlockType.AMRAP, movement = MovementPattern.GAIT,reps = null),
                MetconComponent(planId = idByKey.getValue("AMRAP36_ENGINE_BUILDER_YGIG"), orderInPlan = 2, text = "10 Burpees over Rower", blockType = BlockType.AMRAP, movement = MovementPattern.HORIZONTAL_PUSH, reps = 10),
                MetconComponent(planId = idByKey.getValue("AMRAP36_ENGINE_BUILDER_YGIG"), orderInPlan = 3, text = "15/12 cal Bike", blockType = BlockType.AMRAP, movement = MovementPattern.GAIT,reps = null),
                MetconComponent(planId = idByKey.getValue("AMRAP36_ENGINE_BUILDER_YGIG"), orderInPlan = 4, text = "16 Kettlebell Swings (24/16 kg)", blockType = BlockType.AMRAP, movement = MovementPattern.HINGE, reps = 16),
                MetconComponent(planId = idByKey.getValue("AMRAP36_ENGINE_BUILDER_YGIG"), orderInPlan = 5, text = "15/12 cal Bike", blockType = BlockType.AMRAP, movement = MovementPattern.GAIT,reps = null),
                MetconComponent(planId = idByKey.getValue("AMRAP36_ENGINE_BUILDER_YGIG"), orderInPlan = 6, text = "16 DB Hang Lunges (2 × 15/10 kg)", blockType = BlockType.AMRAP, movement = MovementPattern.SQUAT, reps = 16)
            )

            dao.insertComponentsIgnore(comps)
            comps.forEach { c -> dao.updateComponentText(c.planId, c.orderInPlan, c.text) }
        }
        }
}
