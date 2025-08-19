package com.example.safitness.data.seed

import androidx.room.withTransaction
import com.example.safitness.core.*
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.MetconComponent
import com.example.safitness.data.entities.MetconPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                PlanDef("CINDY_AMRAP20", "Cindy (AMRAP 20)", MetconType.AMRAP, 20),
                PlanDef("HELEN_FT", "Helen (For Time)", MetconType.FOR_TIME, 12),
                PlanDef("EMOM12_BIKE_KB_TTB", "EMOM 12: Bike/KB/TTB", MetconType.EMOM, 12, emom = 60)
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

            dao.insertPlansIgnore(plans)
            defs.forEach { d ->
                dao.updatePlanByKey(d.key, d.title, d.type, d.duration, d.emom, d.archived)
            }
            val idByKey = defs.associate { d -> d.key to dao.getPlanIdByKey(d.key) }

            val comps = listOf(
                MetconComponent(planId = idByKey.getValue("CINDY_AMRAP20"), orderInPlan = 1, text = "5 Pull-ups", blockType = BlockType.AMRAP, movement = MovementPattern.VERTICAL_PULL, reps = 5),
                MetconComponent(planId = idByKey.getValue("CINDY_AMRAP20"), orderInPlan = 2, text = "10 Push-ups", blockType = BlockType.AMRAP, movement = MovementPattern.HORIZONTAL_PUSH, reps = 10),
                MetconComponent(planId = idByKey.getValue("CINDY_AMRAP20"), orderInPlan = 3, text = "15 Air Squats", blockType = BlockType.AMRAP, movement = MovementPattern.SQUAT, reps = 15),

                MetconComponent(planId = idByKey.getValue("HELEN_FT"), orderInPlan = 1, text = "Run 400 m", blockType = BlockType.FOR_TIME, movement = MovementPattern.GAIT),
                MetconComponent(planId = idByKey.getValue("HELEN_FT"), orderInPlan = 2, text = "21 Kettlebell Swings", blockType = BlockType.FOR_TIME, movement = MovementPattern.HINGE, reps = 21),
                MetconComponent(planId = idByKey.getValue("HELEN_FT"), orderInPlan = 3, text = "12 Pull-ups", blockType = BlockType.FOR_TIME, movement = MovementPattern.VERTICAL_PULL, reps = 12),

                MetconComponent(planId = idByKey.getValue("EMOM12_BIKE_KB_TTB"), orderInPlan = 1, text = "Minute 1: 14/12 cal Bike", blockType = BlockType.EMOM, movement = MovementPattern.GAIT),
                MetconComponent(planId = idByKey.getValue("EMOM12_BIKE_KB_TTB"), orderInPlan = 2, text = "Minute 2: 15 Kettlebell Swings", blockType = BlockType.EMOM, movement = MovementPattern.HINGE, reps = 15),
                MetconComponent(planId = idByKey.getValue("EMOM12_BIKE_KB_TTB"), orderInPlan = 3, text = "Minute 3: 10 Toes-to-Bar", blockType = BlockType.EMOM, movement = MovementPattern.VERTICAL_PULL, reps = 10)
            )

            dao.insertComponentsIgnore(comps)
            comps.forEach { c -> dao.updateComponentText(c.planId, c.orderInPlan, c.text) }
        }
    }
}
