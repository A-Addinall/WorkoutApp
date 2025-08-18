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

object EngineLibrarySeeder {
    private const val TAG = "EngineSeed"

    suspend fun seedIfNeeded(db: AppDatabase) = withContext(Dispatchers.IO) {
        val repo = EnginePlanRepository(db.enginePlanDao())
        if (repo.count() > 0) {
            Log.d(TAG, "Engine plans exist; skipping.")
            return@withContext
        }
        Log.d(TAG, "Seeding Engine library...")

        // RUN
        repo.upsert(
            EnginePlanEntity(
                title = "Run — 5k For Time",
                mode = EngineMode.RUN.name,
                intent = EngineIntent.FOR_TIME.name,
                programDistanceMeters = 5000,
                description = "Benchmark 5k. Even splits.",
                rxNotes = "Aim negative splits.",
                scaledNotes = "Reduce to 3k if needed."
            ),
            listOf(
                EngineComponentEntity(0, 0, 1, "Warm-up", "Easy 800m + drills"),
                EngineComponentEntity(0, 0, 2, "Main Set", "5,000m for time"),
                EngineComponentEntity(0, 0, 3, "Cooldown", "Walk 5–10 minutes")
            )
        )
        repo.upsert(
            EnginePlanEntity(
                title = "Run — 20:00 Accumulation (Meters)",
                mode = EngineMode.RUN.name,
                intent = EngineIntent.FOR_DISTANCE.name,
                programDurationSeconds = 20 * 60,
                description = "Max distance in 20 minutes.",
                rxNotes = "Sustainable Z3 pace.",
                scaledNotes = "Walk/jog intervals if needed."
            ),
            listOf(EngineComponentEntity(0, 0, 1, "Main Set", "20:00 for Distance"))
        )

        // ROW
        repo.upsert(
            EnginePlanEntity(
                title = "Row — 2k For Time",
                mode = EngineMode.ROW.name,
                intent = EngineIntent.FOR_TIME.name,
                programDistanceMeters = 2000,
                description = "Classic 2k test.",
                rxNotes = "Controlled start; strong finish.",
                scaledNotes = "Reduce to 1k."
            ),
            listOf(EngineComponentEntity(0, 0, 1, "Main Set", "2,000m for time"))
        )
        repo.upsert(
            EnginePlanEntity(
                title = "Row — 10:00 Accumulation (Meters)",
                mode = EngineMode.ROW.name,
                intent = EngineIntent.FOR_DISTANCE.name,
                programDurationSeconds = 10 * 60,
                description = "Max meters in 10:00.",
                rxNotes = "Hold consistent split.",
                scaledNotes = "Lower damper; focus on form."
            ),
            listOf(EngineComponentEntity(0, 0, 1, "Main Set", "10:00 for Distance"))
        )

        // BIKE
        repo.upsert(
            EnginePlanEntity(
                title = "Bike — 10:00 for Calories",
                mode = EngineMode.BIKE.name,
                intent = EngineIntent.FOR_CALORIES.name,
                programDurationSeconds = 10 * 60,
                programTargetCalories = 150,
                description = "Max calories in 10 minutes.",
                rxNotes = "Cadence > 75 rpm.",
                scaledNotes = "Lower resistance."
            ),
            listOf(EngineComponentEntity(0, 0, 1, "Main Set", "10:00 for Calories"))
        )
        repo.upsert(
            EnginePlanEntity(
                title = "Bike — 20:00 Accumulation (Meters)",
                mode = EngineMode.BIKE.name,
                intent = EngineIntent.FOR_DISTANCE.name,
                programDurationSeconds = 20 * 60,
                description = "Max distance in 20 minutes.",
                rxNotes = "Sustainable wattage.",
                scaledNotes = "Shorten to 12:00."
            ),
            listOf(EngineComponentEntity(0, 0, 1, "Main Set", "20:00 for Distance"))
        )

        Log.d(TAG, "Engine seed complete.")
    }
}
