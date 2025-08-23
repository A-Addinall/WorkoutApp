package com.example.safitness.data.seed

import android.util.Log
import com.example.safitness.core.EngineIntent
import com.example.safitness.core.EngineMode
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.EngineComponentEntity
import com.example.safitness.data.entities.EnginePlanEntity
import com.example.safitness.data.repo.EnginePlanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extra Engine seeds. Adds, never removes.
 */
object EngineLibrarySeeder {
    private const val TAG = "EngineSeedMore"

    suspend fun seedIfNeeded(db: AppDatabase) = withContext(Dispatchers.IO) {
        val repo = EnginePlanRepository(db.enginePlanDao())
        Log.d(TAG, "Seeding extra Engine plans...")

        val defs = listOf(
            // RUN
            EnginePlanEntity(
                title = "Run — 10k For Time",
                mode = EngineMode.RUN.name,
                intent = EngineIntent.FOR_TIME.name,
                description = "Sustained time trial effort.",
                rxNotes = "Aim for even pacing; hydrate.",
                scaledNotes = "5k for time if building."
            ),
            EnginePlanEntity(
                title = "Run — 12×400m (1:1 easy jog)",
                mode = EngineMode.RUN.name,
                intent = EngineIntent.FOR_TIME.name,
                description = "Cruise intervals around 5k pace.",
                rxNotes = "Keep the last 3 fastest.",
                scaledNotes = "8×400m."
            ),

            // ROW
            EnginePlanEntity(
                title = "Row — 5k For Time",
                mode = EngineMode.ROW.name,
                intent = EngineIntent.FOR_TIME.name,
                description = "Benchmark TT on the erg.",
                rxNotes = "26–30 spm; negative split.",
                scaledNotes = "2k for time if newer."
            ),
            EnginePlanEntity(
                title = "Row — 30:30 × 16 (easy 30s)",
                mode = EngineMode.ROW.name,
                intent = EngineIntent.FOR_DISTANCE.name,
                programDurationSeconds = 16 * 60,
                description = "Thirty on / thirty off. Count metres.",
                rxNotes = "Strong strokes; smooth recoveries.",
                scaledNotes = "12× instead of 16×."
            ),

            // BIKE
            EnginePlanEntity(
                title = "Bike — 5×5:00 Hard / 2:00 Easy",
                mode = EngineMode.BIKE.name,
                intent = EngineIntent.FOR_DISTANCE.name,
                programDurationSeconds = (5 * (5 + 2)) * 60,
                description = "Z4 efforts with Z2 recovery.",
                rxNotes = "Cadence > 80 rpm in work.",
                scaledNotes = "4× blocks."
            ),
            EnginePlanEntity(
                title = "Bike — 20:00 Time Trial",
                mode = EngineMode.BIKE.name,
                intent = EngineIntent.FOR_DISTANCE.name,
                programDurationSeconds = 20 * 60,
                description = "Sustained best effort for 20:00.",
                rxNotes = "Start conservatively; push last 5:00.",
                scaledNotes = "12–15 minutes."
            )
        )

        defs.forEach { plan ->
            repo.upsert(
                plan,
                listOf(
                    EngineComponentEntity(0,0,1,"Warm-up","Easy aerobic + drills (5–10 min)"),
                    EngineComponentEntity(0,0,2,"Main Set", plan.description ?: ""),
                    EngineComponentEntity(0,0,3,"Cooldown","Walk/Spin easy (5–10 min)")
                )
            )
        }

        Log.d(TAG, "Engine extra seed complete.")
    }
}
