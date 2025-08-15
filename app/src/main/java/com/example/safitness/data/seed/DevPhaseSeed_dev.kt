package com.example.safitness.data.seed

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.safitness.data.dao.LibraryDao
import com.example.safitness.data.dao.MetconDao
import com.example.safitness.data.dao.PlanDao
import com.example.safitness.data.dao.ProgramDao
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
 * - If no phase/plans exist → create Phase "Dev Test Phase" + Week 1..2 (days 1..5).
 * - For Week 1 Day 1..5:
 *      • If NO items → mirror legacy selections; if legacy empty → add minimal defaults.
 *      • If items EXIST → top-up metcons to a max of 3 by adding non-duplicate plans.
 *
 * Idempotent: never duplicates items; only fills gaps.
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

        // 3) Populate Week 1 Day 1..5
        val allExercises = libraryDao.getExercises(null, null).first() // existing flow API
        val allPlans = metconDao.getAllPlans().first()

        for (day in 1..5) {
            val dayPlanId = planDao.getPlanId(phaseId, 1, day) ?: continue

            val hasAnyItems = planDao.countItemsForDay(dayPlanId) > 0
            val toInsert = mutableListOf<DayItemEntity>()

            if (!hasAnyItems) {
                // Try to mirror legacy first
                val legacyStrength = programDao.getProgramForDay(day).first() // List<ExerciseWithSelection>
                val legacyMetcons = metconDao.getMetconsForDay(day).first()   // List<SelectionWithPlanAndComponents>

                if (legacyStrength.isNotEmpty() || legacyMetcons.isNotEmpty()) {
                    // Mirror strength
                    legacyStrength.forEachIndexed { idx, exWithSel ->
                        toInsert.add(
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
                    // Mirror metcons
                    legacyMetcons.forEachIndexed { idx, selWithPlan ->
                        toInsert.add(
                            DayItemEntity(
                                dayPlanId = dayPlanId,
                                itemType = "METCON",
                                refId = selWithPlan.selection.planId,
                                required = selWithPlan.selection.required,
                                sortOrder = 100 + idx
                            )
                        )
                    }

                    // Top-up metcons to max 3 (without duplicates)
                    val usedPlanIds = legacyMetcons.map { it.selection.planId }.toSet()
                    val extraNeeded = (3 - usedPlanIds.size).coerceAtLeast(0)
                    if (extraNeeded > 0) {
                        val startOrder = 100 + legacyMetcons.size
                        val extras = allPlans.filter { it.id !in usedPlanIds }.take(extraNeeded)
                        extras.forEachIndexed { i, p ->
                            toInsert.add(
                                DayItemEntity(
                                    dayPlanId = dayPlanId,
                                    itemType = "METCON",
                                    refId = p.id,
                                    required = true,
                                    sortOrder = startOrder + i
                                )
                            )
                        }
                    }

                    Log.d(TAG, "Mirrored legacy for W1D$day → +${toInsert.size} items")
                } else {
                    // Minimal defaults: first 2 exercises + up to 3 metcon plans
                    val chosenExercises = allExercises.take(2)
                    chosenExercises.forEachIndexed { idx, ex ->
                        toInsert.add(
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
                    val metconCount = allPlans.size.coerceAtMost(3)
                    allPlans.take(metconCount).forEachIndexed { i, p ->
                        toInsert.add(
                            DayItemEntity(
                                dayPlanId = dayPlanId,
                                itemType = "METCON",
                                refId = p.id,
                                required = true,
                                sortOrder = 100 + i
                            )
                        )
                    }
                    Log.d(TAG, "Defaulted W1D$day → +${toInsert.size} items")
                }
            } else {
                // Already has items → only top-up metcons to a max of 3
                // Use existing PlanDao flow to read current metcon selections (no new DAO)
                val existingMetcons = planDao.flowDayMetconsFor(dayPlanId).first()
                val usedPlanIds = existingMetcons.map { it.planId }.toSet()
                val extraNeeded = (3 - existingMetcons.size).coerceAtLeast(0)

                if (extraNeeded > 0) {
                    val startOrder = (existingMetcons.maxOfOrNull { it.sortOrder } ?: 99) + 1
                    val extras = allPlans.filter { it.id !in usedPlanIds }.take(extraNeeded)
                    extras.forEachIndexed { i, p ->
                        toInsert.add(
                            DayItemEntity(
                                dayPlanId = dayPlanId,
                                itemType = "METCON",
                                refId = p.id,
                                required = true,
                                sortOrder = startOrder + i
                            )
                        )
                    }
                    Log.d(TAG, "Top-up W1D$day → added ${toInsert.size} metcon(s)")
                } else {
                    Log.d(TAG, "W1D$day already has ${existingMetcons.size} metcons; no top-up")
                }
            }

            if (toInsert.isNotEmpty()) {
                planDao.insertItems(toInsert)
                Log.d(TAG, "Inserted ${toInsert.size} items for W1D$day")
            }
        }

        Log.d(TAG, "Dev seed complete.")
    }
}
