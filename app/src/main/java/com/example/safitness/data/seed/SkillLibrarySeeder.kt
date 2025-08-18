package com.example.safitness.data.seed

import android.util.Log
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.SkillComponentEntity
import com.example.safitness.data.entities.SkillPlanEntity
import com.example.safitness.data.repo.SkillPlanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SkillLibrarySeeder {
    private const val TAG = "SkillSeed"

    suspend fun seedIfNeeded(db: AppDatabase) = withContext(Dispatchers.IO) {
        val repo = SkillPlanRepository(db.skillPlanDao())
        if (repo.count() > 0) {
            Log.d(TAG, "Skill plans exist; skipping.")
            return@withContext
        }
        Log.d(TAG, "Seeding Skill library...")

        // Bar Muscle-Up
        val barMuPlan = SkillPlanEntity(
            title = "Bar Muscle-Up — Path to First Rep",
            skill = "BAR_MUSCLE_UP",
            description = "Progression from shapes to turnover.",
            defaultTestType = "ATTEMPTS",
            targetDurationSeconds = 300,
            rxNotes = "Beat swing → high pull → turnover.",
            scaledNotes = "Bands / low-bar transitions as needed."
        )
        val barMuComps = listOf(
            SkillComponentEntity(0, 0, 1, "Hollow/Arch Holds", "20s each, 3 sets",
                testType = "MAX_HOLD_SECONDS", targetHoldSeconds = 20),
            SkillComponentEntity(0, 0, 2, "Beat Swings", "Crisp shapes, lat engagement",
                testType = "FOR_TIME_REPS", targetReps = 30),
            SkillComponentEntity(0, 0, 3, "Explosive Kip to C2B", "Powerful kip; lats + hips",
                testType = "FOR_TIME_REPS", targetReps = 15),
            SkillComponentEntity(0, 0, 4, "Low Bar Transition Drill", "Fast wrist turnover",
                testType = "ATTEMPTS"),
            SkillComponentEntity(0, 0, 5, "Banded MU Attempts", "Light band, focus timing",
                testType = "ATTEMPTS")
        )
        repo.upsert(barMuPlan, barMuComps)

        // Handstand Push-Up
        val hspuPlan = SkillPlanEntity(
            title = "Handstand Push-Up — Path to First Rep",
            skill = "HANDSTAND_PUSH_UP",
            description = "From wall walks to controlled negatives.",
            defaultTestType = "ATTEMPTS",
            targetDurationSeconds = 300,
            rxNotes = "Stacked line, braced trunk.",
            scaledNotes = "Reduce ROM or box pike."
        )
        val hspuComps = listOf(
            SkillComponentEntity(0, 0, 1, "Wall Walks", "3–5 controlled reps",
                testType = "FOR_TIME_REPS", targetReps = 5),
            SkillComponentEntity(0, 0, 2, "Handstand Hold", "Kick-up, maintain line",
                testType = "MAX_HOLD_SECONDS", targetHoldSeconds = 30),
            SkillComponentEntity(0, 0, 3, "Eccentric HSPU (Negatives)", "3–5s lower",
                testType = "FOR_TIME_REPS", targetReps = 10),
            SkillComponentEntity(0, 0, 4, "Partial ROM / Box Pike", "Quality first",
                testType = "ATTEMPTS")
        )
        repo.upsert(hspuPlan, hspuComps)

        Log.d(TAG, "Skill seed complete.")
    }
}
