package com.example.safitness.data.seed

import android.util.Log
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.SkillComponentEntity
import com.example.safitness.data.entities.SkillPlanEntity
import com.example.safitness.data.repo.SkillPlanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Seeds a representative Skill library covering these types:
 * - ATTEMPTS
 * - MAX_HOLD_SECONDS
 * - FOR_TIME_REPS
 * - EMOM
 * - AMRAP
 *
 * Idempotent: skips if any skill plans already exist.
 */
object SkillLibrarySeeder {
    private const val TAG = "SkillSeed"

    suspend fun seedIfNeeded(db: AppDatabase) = withContext(Dispatchers.IO) {
        val repo = SkillPlanRepository(db.skillPlanDao())
        if (repo.count() > 0) {
            Log.d(TAG, "Skill plans exist; skipping.")
            return@withContext
        }
        Log.d(TAG, "Seeding Skill library...")

        // =========================
        // ATTEMPTS — Bar Muscle-Up
        // =========================
        val barMu = SkillPlanEntity(
            title = "Bar Muscle-Up — Path to First Rep",
            skill = "BAR_MUSCLE_UP",
            description = "Progression from shapes to turnover.",
            defaultTestType = "ATTEMPTS",
            targetDurationSeconds = 300,
            rxNotes = "Beat swing → high pull → turnover.",
            scaledNotes = "Bands / low-bar transitions as needed."
        )
        val barMuComps = listOf(
            SkillComponentEntity(
                id = 0, planId = 0, orderIndex = 1,
                title = "Hollow/Arch Holds",
                description = "20s each, 3 sets",
                testType = "MAX_HOLD_SECONDS",
                targetHoldSeconds = 20
            ),
            SkillComponentEntity(
                id = 0, planId = 0, orderIndex = 2,
                title = "Beat Swings",
                description = "Crisp shapes, lat engagement",
                testType = "FOR_TIME_REPS",
                targetReps = 30
            ),
            SkillComponentEntity(
                id = 0, planId = 0, orderIndex = 3,
                title = "Explosive Kip to C2B",
                description = "Powerful kip; lats + hips",
                testType = "FOR_TIME_REPS",
                targetReps = 15
            ),
            SkillComponentEntity(
                id = 0, planId = 0, orderIndex = 4,
                title = "Low Bar Transition Drill",
                description = "Fast wrist turnover",
                testType = "ATTEMPTS"
            ),
            SkillComponentEntity(
                id = 0, planId = 0, orderIndex = 5,
                title = "Banded MU Attempts",
                description = "Light band, focus timing",
                testType = "ATTEMPTS"
            )
        )
        repo.upsert(barMu, barMuComps)

        // =========================
        // ATTEMPTS — Handstand Push-Up
        // =========================
        val hspu = SkillPlanEntity(
            title = "Handstand Push-Up — Path to First Rep",
            skill = "HANDSTAND_PUSH_UP",
            description = "From wall walks to controlled negatives.",
            defaultTestType = "ATTEMPTS",
            targetDurationSeconds = 300,
            rxNotes = "Stacked line, braced trunk.",
            scaledNotes = "Reduce ROM or box pike."
        )
        val hspuComps = listOf(
            SkillComponentEntity(
                id = 0, planId = 0, orderIndex = 1,
                title = "Wall Walks",
                description = "3–5 controlled reps",
                testType = "FOR_TIME_REPS",
                targetReps = 5
            ),
            SkillComponentEntity(
                id = 0, planId = 0, orderIndex = 2,
                title = "Handstand Hold",
                description = "Kick-up, maintain line",
                testType = "MAX_HOLD_SECONDS",
                targetHoldSeconds = 30
            ),
            SkillComponentEntity(
                id = 0, planId = 0, orderIndex = 3,
                title = "Eccentric HSPU (Negatives)",
                description = "3–5s lower",
                testType = "FOR_TIME_REPS",
                targetReps = 10
            ),
            SkillComponentEntity(
                id = 0, planId = 0, orderIndex = 4,
                title = "Partial ROM / Box Pike",
                description = "Quality first",
                testType = "ATTEMPTS"
            )
        )
        repo.upsert(hspu, hspuComps)

        // =========================
        // MAX HOLD — Handstand Hold
        // =========================
        val hsHold = SkillPlanEntity(
            title = "Handstand Hold — Max Accumulated Time",
            skill = "HANDSTAND",
            description = "Accumulate time under a quality line.",
            defaultTestType = "MAX_HOLD_SECONDS",
            targetDurationSeconds = 180,
            rxNotes = "Nose over fingertips; ribs down.",
            scaledNotes = "Wall-facing or box pike."
        )
        val hsHoldComps = listOf(
            SkillComponentEntity(
                id = 0, planId = 0, orderIndex = 1,
                title = "Kick-up Holds",
                description = "Sets of 20–30s",
                testType = "MAX_HOLD_SECONDS",
                targetHoldSeconds = 30
            )
        )
        repo.upsert(hsHold, hsHoldComps)

        // =========================
        // FOR TIME (reps) — Double-Unders
        // =========================
        val du100 = SkillPlanEntity(
            title = "Double-Unders — 100 Reps For Time",
            skill = "DOUBLE_UNDER",
            description = "Unbroken if possible; relax shoulders.",
            defaultTestType = "FOR_TIME_REPS",
            targetDurationSeconds = 300,
            rxNotes = "Soft knees; elbows tucked.",
            scaledNotes = "Single-unders x 200."
        )
        val du100Comps = listOf(
            SkillComponentEntity(
                id = 0, planId = 0, orderIndex = 1,
                title = "Main Set",
                description = "100 DU for time",
                testType = "FOR_TIME_REPS",
                targetReps = 100
            )
        )
        repo.upsert(du100, du100Comps)

        // =========================
        // EMOM — Toes-to-Bar
        // =========================
        val ttbEmom = SkillPlanEntity(
            title = "Toes-to-Bar — EMOM 10:00 (6 reps)",
            skill = "TOES_TO_BAR",
            description = "Every minute perform 6 quality reps.",
            defaultTestType = "EMOM",
            targetDurationSeconds = 10 * 60,
            rxNotes = "Lat-driven kip; stay hollow.",
            scaledNotes = "Hanging knee raises."
        )
        val ttbEmomComps = listOf(
            SkillComponentEntity(
                id = 0, planId = 0, orderIndex = 1,
                title = "Main Set",
                description = "EMOM 10:00 — 6 reps",
                testType = "FOR_TIME_REPS",
                targetReps = 6
            )
        )
        repo.upsert(ttbEmom, ttbEmomComps)

        // =========================
        // AMRAP — Chest-to-Bar
        // =========================
        val ctbAmrap = SkillPlanEntity(
            title = "Chest-to-Bar — AMRAP 7:00",
            skill = "CHEST_TO_BAR",
            description = "As many reps as possible in 7 minutes.",
            defaultTestType = "AMRAP",
            targetDurationSeconds = 7 * 60,
            rxNotes = "Kip rhythm consistent; no early arm pull.",
            scaledNotes = "Jumping C2B or regular pull-ups."
        )
        val ctbAmrapComps = listOf(
            SkillComponentEntity(
                id = 0, planId = 0, orderIndex = 1,
                title = "Main Set",
                description = "AMRAP 7:00 — C2B reps",
                testType = "FOR_TIME_REPS",
                targetReps = 0 // free volume
            )
        )
        repo.upsert(ctbAmrap, ctbAmrapComps)
        // Kipping Pull-Up — EMOM 10:00 (5 reps)
        repo.upsert(
            SkillPlanEntity(
                title = "Kipping Pull-Up — EMOM 10:00 (5 reps)",
                skill = "KIPPING_PULL_UP",
                description = "Technique under small density.",
                defaultTestType = "EMOM",
                targetDurationSeconds = 10 * 60,
                rxNotes = "Smooth beat swing; timing first.",
                scaledNotes = "Jumping PU / banded."
            ),
            listOf(SkillComponentEntity(0,0,1,"Main Set","EMOM 10:00 — 5 reps","FOR_TIME_REPS", targetReps = 5))
        )

// Double-Unders — Ladder 10→50 by 10
        repo.upsert(
            SkillPlanEntity(
                title = "Double-Unders — Ladder 10→50 by 10",
                skill = "DOUBLE_UNDER",
                description = "Short sets; composure over speed.",
                defaultTestType = "FOR_TIME_REPS",
                targetDurationSeconds = 300,
                rxNotes = "Relaxed shoulders; wrists drive.",
                scaledNotes = "Singles x2."
            ),
            listOf(SkillComponentEntity(0,0,1,"Main Set","10,20,30,40,50 DU","FOR_TIME_REPS", targetReps = 150))
        )


        Log.d(TAG, "Skill seed complete.")
    }
}
