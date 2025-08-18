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
 * Seeds representative Engine plans covering:
 * - FOR_TIME  (Run/Row time trial)
 * - FOR_DISTANCE (Run/Row/Bike accumulation)
 * - FOR_CALORIES (Bike accumulation)
 * - EMOM-style variants (route via title contains "EMOM"; intent remains FOR_DISTANCE / FOR_CALORIES)
 */
object EngineLibrarySeeder {
    private const val TAG = "EngineSeed"

    suspend fun seedIfNeeded(db: AppDatabase) = withContext(Dispatchers.IO) {
        val repo = EnginePlanRepository(db.enginePlanDao())
        if (repo.count() > 0) {
            Log.d(TAG, "Engine plans exist; skipping.")
            return@withContext
        }
        Log.d(TAG, "Seeding Engine library...")

        // --- RUN (For Time) ---
        repo.upsert(
            EnginePlanEntity(
                title = "Run — 5k For Time",
                mode = EngineMode.RUN.name,
                intent = EngineIntent.FOR_TIME.name,
                programDistanceMeters = 5_000,
                description = "Benchmark 5k. Even splits.",
                rxNotes = "Aim negative splits.",
                scaledNotes = "Drop to 3k if needed."
            ),
            listOf(
                EngineComponentEntity(0,0,1,"Warm-up","Easy 800m + drills"),
                EngineComponentEntity(0,0,2,"Main Set","5,000m for time"),
                EngineComponentEntity(0,0,3,"Cooldown","Walk 5–10 minutes")
            )
        )

        // --- ROW (For Time) ---
        repo.upsert(
            EnginePlanEntity(
                title = "Row — 2k For Time",
                mode = EngineMode.ROW.name,
                intent = EngineIntent.FOR_TIME.name,
                programDistanceMeters = 2_000,
                description = "Classic 2k test.",
                rxNotes = "Controlled start; strong finish.",
                scaledNotes = "Reduce to 1k."
            ),
            listOf(EngineComponentEntity(0,0,1,"Main Set","2,000m for time"))
        )

        // --- RUN (For Distance accumulation) ---
        repo.upsert(
            EnginePlanEntity(
                title = "Run — 20:00 Accumulation (Meters)",
                mode = EngineMode.RUN.name,
                intent = EngineIntent.FOR_DISTANCE.name,
                programDurationSeconds = 20*60,
                description = "Max distance in 20 minutes.",
                rxNotes = "Z3 pace.",
                scaledNotes = "Walk/jog intervals."
            ),
            listOf(EngineComponentEntity(0,0,1,"Main Set","20:00 for Distance"))
        )

        // --- ROW (For Distance accumulation) ---
        repo.upsert(
            EnginePlanEntity(
                title = "Row — 10:00 Accumulation (Meters)",
                mode = EngineMode.ROW.name,
                intent = EngineIntent.FOR_DISTANCE.name,
                programDurationSeconds = 10*60,
                description = "Max meters in 10:00.",
                rxNotes = "Consistent split.",
                scaledNotes = "Lower damper."
            ),
            listOf(EngineComponentEntity(0,0,1,"Main Set","10:00 for Distance"))
        )

        // --- BIKE (For Calories accumulation) ---
        repo.upsert(
            EnginePlanEntity(
                title = "Bike — 10:00 for Calories",
                mode = EngineMode.BIKE.name,
                intent = EngineIntent.FOR_CALORIES.name,
                programDurationSeconds = 10*60,
                programTargetCalories = 150,
                description = "Max calories in 10 minutes.",
                rxNotes = "Cadence > 75 rpm.",
                scaledNotes = "Lower resistance."
            ),
            listOf(EngineComponentEntity(0,0,1,"Main Set","10:00 for Calories"))
        )

        // --- BIKE (For Distance accumulation) ---
        repo.upsert(
            EnginePlanEntity(
                title = "Bike — 20:00 Accumulation (Meters)",
                mode = EngineMode.BIKE.name,
                intent = EngineIntent.FOR_DISTANCE.name,
                programDurationSeconds = 20*60,
                description = "Max distance in 20 minutes.",
                rxNotes = "Sustainable wattage.",
                scaledNotes = "12:00 instead of 20:00."
            ),
            listOf(EngineComponentEntity(0,0,1,"Main Set","20:00 for Distance"))
        )

        // --- EMOM variants (use title flag; keeps schema stable) ---
        repo.upsert(
            EnginePlanEntity(
                title = "Row — EMOM 20:00 (20 cal)",
                mode = EngineMode.ROW.name,
                intent = EngineIntent.FOR_CALORIES.name,
                programDurationSeconds = 20*60,
                programTargetCalories = 20,
                description = "Every minute: 20 calories.",
                rxNotes = "Spin-up fast; settle to pace.",
                scaledNotes = "16/12 cal or EMOM 12:00."
            ),
            listOf(EngineComponentEntity(0,0,1,"Main Set","EMOM 20:00 — 20 cal / min"))
        )
        repo.upsert(
            EnginePlanEntity(
                title = "Run — EMOM 16:00 (200 m)",
                mode = EngineMode.RUN.name,
                intent = EngineIntent.FOR_DISTANCE.name,
                programDurationSeconds = 16*60,
                programDistanceMeters = 200,
                description = "Every minute: 200 m shuttle.",
                rxNotes = "Crisp turns; posture tall.",
                scaledNotes = "150 m or EMOM 12:00."
            ),
            listOf(EngineComponentEntity(0,0,1,"Main Set","EMOM 16:00 — 200 m / min"))
        )

        Log.d(TAG, "Engine seed complete.")
    }
}
