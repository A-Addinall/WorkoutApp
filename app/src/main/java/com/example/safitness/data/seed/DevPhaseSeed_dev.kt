package com.example.safitness.data.seed

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.safitness.data.dao.*
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.DayItemEntity
import com.example.safitness.data.entities.PhaseEntity
import com.example.safitness.data.entities.WeekDayPlanEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * DEV-ONLY: Ensure a usable plan exists in the new model.
 *
 * Behavior:
 * - If no phase/plans exist → create Phase "Dev Test Phase" + Week 1..2 (days 1..5)
 * - For Week 1 Day 1..5: if there are NO items yet → mirror legacy selections
 *   - If legacy is empty too → create minimal defaults (2 exercises + 1 metcon)
 *
 * Idempotent: never duplicates items; only fills when empty.
 */
object DevPhaseSeed_dev {

    private const val TAG = "DevPhaseSeed"

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun seedFromLegacy(db: AppDatabase) = withContext(Dispatchers.IO) {
        val planDao: PlanDao = db.planDao()
        val programDao: ProgramDao = db.programDao()
        val metconDao: MetconDao = db.metconDao()
        val libraryDao: LibraryDao = db.libraryDao()

        // 1) Ensure we have (or create) a phase
        var phaseId = planDao.currentPhaseId()
        if (phaseId == null) {
            phaseId = planDao.insertPhase(
                PhaseEntity(
                    name = "Dev Test Phase",
                    startDateEpochDay = LocalDate.now().toEpochDay(),
                    weeks = 4
                )
            )
            Log.d(TAG, "Created phase id=$phaseId")
        } else {
            Log.d(TAG, "Using existing phase id=$phaseId")
        }

        // 2) Ensure Week 1..2 × Day 1..5 plans exist
        for (w in 1..2) {
            for (d in 1..5) {
                val existing = planDao.getPlanId(phaseId, w, d)
                if (existing == null) {
                    planDao.insertWeekPlans(
                        listOf(
                            WeekDayPlanEntity(
                                phaseId = phaseId,
                                weekIndex = w,
                                dayIndex = d,
                                displayName = "Week $w Day $d"
                            )
                        )
                    )
                    Log.d(TAG, "Inserted plan for Week $w Day $d")
                }
            }
        }

        // 3) For Week 1 Day 1..5: if empty, populate (mirror or defaults)
        val allExercises = libraryDao.getExercises(null, null).first() // existing flow API
        val allPlans = metconDao.getAllPlans().first()

        for (day in 1..5) {
            val dayPlanId = planDao.getPlanId(phaseId, 1, day) ?: continue
            val existingCount = planDao.countItemsForDay(dayPlanId)
            if (existingCount > 0) {
                Log.d(TAG, "Week 1 Day $day already has $existingCount items; skipping")
                continue
            }

            // Try to mirror legacy first
            val legacyStrength = programDao.getProgramForDay(day).first() // List<ExerciseWithSelection>
            val legacyMetcons = metconDao.getMetconsForDay(day).first()   // List<SelectionWithPlanAndComponents>

            val items = mutableListOf<DayItemEntity>()
            if (legacyStrength.isNotEmpty() || legacyMetcons.isNotEmpty()) {
                legacyStrength.forEachIndexed { idx, exWithSel ->
                    items.add(
                        DayItemEntity(
                            dayPlanId = dayPlanId,
                            itemType = "STRENGTH",
                            refId = exWithSel.exercise.id,
                            required = exWithSel.required,
                            sortOrder = idx,
                            targetReps = exWithSel.targetReps,
                            prescriptionJson = null
                        )
                    )
                }
                legacyMetcons.forEachIndexed { idx, selWithPlan ->
                    items.add(
                        DayItemEntity(
                            dayPlanId = dayPlanId,
                            itemType = "METCON",
                            refId = selWithPlan.selection.planId,
                            required = selWithPlan.selection.required,
                            sortOrder = 100 + idx
                        )
                    )
                }
                Log.d(TAG, "Mirrored legacy for Week 1 Day $day → ${items.size} items")
            } else {
                // Minimal defaults: first 2 exercises + first metcon plan (if available)
                val chosenExercises = allExercises.take(2)
                chosenExercises.forEachIndexed { idx, ex ->
                    items.add(
                        DayItemEntity(
                            dayPlanId = dayPlanId,
                            itemType = "STRENGTH",
                            refId = ex.id,
                            required = true,
                            sortOrder = idx,
                            targetReps = 5,
                            prescriptionJson = null
                        )
                    )
                }
                allPlans.firstOrNull()?.let { p ->
                    items.add(
                        DayItemEntity(
                            dayPlanId = dayPlanId,
                            itemType = "METCON",
                            refId = p.id,
                            required = true,
                            sortOrder = 100
                        )
                    )
                }
                Log.d(TAG, "Defaulted Week 1 Day $day → ${items.size} items")
            }

            if (items.isNotEmpty()) {
                planDao.insertItems(items)
                Log.d(TAG, "Inserted ${items.size} items for Week 1 Day $day")
            }
        }

        Log.d(TAG, "Dev seed complete.")
    }
}
