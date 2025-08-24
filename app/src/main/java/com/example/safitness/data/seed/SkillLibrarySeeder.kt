package com.example.safitness.data.seed

import android.util.Log
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.SkillComponentEntity
import com.example.safitness.data.entities.SkillPlanEntity
import com.example.safitness.data.repo.SkillPlanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Seeds a representative Skill library.
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

        // Bar Muscle-Up - attempts progression
        repo.upsert(
            SkillPlanEntity(
                title = "Bar Muscle-Up — Path to First Rep",
                skill = "BAR_MUSCLE_UP",
                description = "Progression from shapes to turnover.",
                defaultTestType = "ATTEMPTS",
                targetDurationSeconds = 300,
                rxNotes = "Beat swing → high pull → turnover.",
                scaledNotes = "Bands / low-bar transitions as needed."
            ),
            listOf(
                SkillComponentEntity(0,0,1,"Hollow/Arch Holds","20s each, 3 sets","MAX_HOLD_SECONDS", targetHoldSeconds = 20),
                SkillComponentEntity(0,0,2,"Beat Swings","Crisp shapes, lat engagement","FOR_TIME_REPS", targetReps = 30),
                SkillComponentEntity(0,0,3,"Explosive Kip to C2B","Powerful kip; lats + hips","FOR_TIME_REPS", targetReps = 15),
                SkillComponentEntity(0,0,4,"Low Bar Transition Drill","Fast wrist turnover","ATTEMPTS"),
                SkillComponentEntity(0,0,5,"Banded MU Attempts","Light band, focus timing","ATTEMPTS")
            )
        )

        // Handstand Push-Up — attempts
        repo.upsert(
            SkillPlanEntity(
                title = "Handstand Push-Up — Path to First Rep",
                skill = "HANDSTAND_PUSH_UP",
                description = "From wall walks to controlled negatives.",
                defaultTestType = "ATTEMPTS",
                targetDurationSeconds = 300,
                rxNotes = "Stacked line, braced trunk.",
                scaledNotes = "Reduce ROM or box pike."
            ),
            listOf(
                SkillComponentEntity(0,0,1,"Wall Walks","3–5 controlled reps","FOR_TIME_REPS", targetReps = 5),
                SkillComponentEntity(0,0,2,"Handstand Hold","Kick-up, maintain line","MAX_HOLD_SECONDS", targetHoldSeconds = 30),
                SkillComponentEntity(0,0,3,"Eccentric HSPU (Negatives)","3–5s lower","FOR_TIME_REPS", targetReps = 10),
                SkillComponentEntity(0,0,4,"Partial ROM / Box Pike","Quality first","ATTEMPTS")
            )
        )

        // Handstand Hold — max time
        repo.upsert(
            SkillPlanEntity(
                title = "Handstand Hold — Max Accumulated Time",
                skill = "HANDSTAND",
                description = "Accumulate time under a quality line.",
                defaultTestType = "MAX_HOLD_SECONDS",
                targetDurationSeconds = 180,
                rxNotes = "Nose over fingertips; ribs down.",
                scaledNotes = "Wall-facing or box pike."
            ),
            listOf(SkillComponentEntity(0,0,1,"Kick-up Holds","Sets of 20–30s","MAX_HOLD_SECONDS", targetHoldSeconds = 30))
        )

        // Double-Unders — for time & ladder
        repo.upsert(
            SkillPlanEntity(
                title = "Double-Unders — 100 Reps For Time",
                skill = "DOUBLE_UNDER",
                description = "Unbroken if possible; relax shoulders.",
                defaultTestType = "FOR_TIME_REPS",
                targetDurationSeconds = 300,
                rxNotes = "Soft knees; elbows tucked.",
                scaledNotes = "Single-unders x 200."
            ),
            listOf(SkillComponentEntity(0,0,1,"Main Set","100 DU for time","FOR_TIME_REPS", targetReps = 100))
        )
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

        // Toes-to-Bar — EMOM micro-density
        repo.upsert(
            SkillPlanEntity(
                title = "Toes-to-Bar — EMOM 10:00 (6 reps)",
                skill = "TOES_TO_BAR",
                description = "Every minute perform 6 quality reps.",
                defaultTestType = "EMOM",
                targetDurationSeconds = 10 * 60,
                rxNotes = "Lat-driven kip; stay hollow.",
                scaledNotes = "Hanging knee raises."
            ),
            listOf(SkillComponentEntity(0,0,1,"Main Set","EMOM 10:00 — 6 reps","FOR_TIME_REPS", targetReps = 6))
        )

        // Chest-to-Bar — AMRAP window
        repo.upsert(
            SkillPlanEntity(
                title = "Chest-to-Bar — AMRAP 7:00",
                skill = "CHEST_TO_BAR",
                description = "As many reps as possible in 7 minutes.",
                defaultTestType = "AMRAP",
                targetDurationSeconds = 7 * 60,
                rxNotes = "Kip rhythm consistent; no early arm pull.",
                scaledNotes = "Jumping C2B or regular pull-ups."
            ),
            listOf(SkillComponentEntity(0,0,1,"Main Set","AMRAP 7:00 — C2B reps","FOR_TIME_REPS", targetReps = 0))
        )

        // Kipping Pull-Up — EMOM density
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

        // Rope Climb — attempts/progression
        repo.upsert(
            SkillPlanEntity(
                title = "Rope Climb — Technique & Attempts",
                skill = "ROPE_CLIMB",
                description = "Foot lock + hip pinch; controlled descents.",
                defaultTestType = "ATTEMPTS",
                targetDurationSeconds = 300,
                rxNotes = "Practice J-hook/S-hook; keep arms straight.",
                scaledNotes = "Towel pulls / rope seated pulls."
            ),
            listOf(
                SkillComponentEntity(0,0,1,"Foot Lock Drills","Floor to stand practice","ATTEMPTS"),
                SkillComponentEntity(0,0,2,"Seated Rope Pulls","Short powerful pulls","FOR_TIME_REPS", targetReps = 20),
                SkillComponentEntity(0,0,3,"Short Ascents","2–3m ascents with control","ATTEMPTS")
            )
        )

        Log.d(TAG, "Skill seed complete.")
    }
}
