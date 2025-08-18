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
 * DEV-ONLY: seed a phase and populate Week 1, Days 1..5 with
 * Strength + Metcon + Engine + Skill so all flows are testable.
 *
 * Idempotent: won’t duplicate entries; fills gaps only.
 */
object DevPhaseSeed_dev {

    private const val TAG = "DevPhaseSeed"

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun seedFromLegacy(db: AppDatabase) = withContext(Dispatchers.IO) {
        val planDao: PlanDao = db.planDao()
        val programDao: ProgramDao = db.programDao()
        val metconDao: MetconDao = db.metconDao()
        val libraryDao: LibraryDao = db.libraryDao()
        val engineDao = db.enginePlanDao()
        val skillDao  = db.skillPlanDao()

        // 1) Ensure a phase exists
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
        for (w in 1..2) for (d in 1..5) {
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

        // 3) Pull libraries once
        val allExercises = libraryDao.getExercises(null, null).first()
        val allMetcons   = metconDao.getAllPlans().first()
        val allEngines   = engineDao.getPlans()
        val allSkills    = skillDao.getPlans()

        // Engine groups for deterministic coverage
        val engineForTime     = allEngines.filter { it.intent.equals("FOR_TIME", true) }
        val engineForDistance = allEngines.filter { it.intent.equals("FOR_DISTANCE", true) }
        val engineForCalories = allEngines.filter { it.intent.equals("FOR_CALORIES", true) }
        val engineEmomFlagged = allEngines.filter { it.title.contains("EMOM", true) }

        // Skill groups for deterministic coverage
        val skillAttempts    = allSkills.filter { it.defaultTestType.equals("ATTEMPTS", true) }
        val skillMaxHold     = allSkills.filter { it.defaultTestType.equals("MAX_HOLD_SECONDS", true) }
        val skillForTimeReps = allSkills.filter { it.defaultTestType.equals("FOR_TIME_REPS", true) }
        val skillEmom        = allSkills.filter { it.defaultTestType.equals("EMOM", true) || it.title.contains("EMOM", true) }
        val skillAmrap       = allSkills.filter { it.defaultTestType.equals("AMRAP", true) || it.title.contains("AMRAP", true) }

        // 4) Populate W1D1..W1D5
        for (day in 1..5) {
            val dayPlanId = planDao.getPlanId(phaseId, 1, day) ?: continue
            val hasAnyItems = planDao.countItemsForDay(dayPlanId) > 0

            val toInsert = mutableListOf<DayItemEntity>()

            if (!hasAnyItems) {
                // Mirror legacy strength/metcon first
                val legacyStrength = programDao.getProgramForDay(day).first()
                val legacyMetcons  = metconDao.getMetconsForDay(day).first()

                // Strength
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

                // Metcon
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

                // Top up to 3 metcons if needed
                val usedMetconIds = legacyMetcons.map { it.selection.planId }.toSet()
                val extrasNeeded = (3 - usedMetconIds.size).coerceAtLeast(0)
                if (extrasNeeded > 0) {
                    val startOrder = 100 + legacyMetcons.size
                    val extras = allMetcons.filter { it.id !in usedMetconIds }.take(extrasNeeded)
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

                // Engine — vary per day for coverage
                val enginePick = when (day) {
                    1 -> engineForTime.firstOrNull()
                    2 -> engineForDistance.firstOrNull()
                    3 -> engineForCalories.firstOrNull()
                    4 -> engineEmomFlagged.firstOrNull()
                    else -> (engineForTime + engineForDistance + engineForCalories + engineEmomFlagged).firstOrNull()
                }
                enginePick?.let {
                    toInsert.add(
                        DayItemEntity(
                            dayPlanId = dayPlanId,
                            itemType = "ENGINE",
                            refId = it.id,
                            required = true,
                            sortOrder = 200
                        )
                    )
                }

                // Skill — vary per day for coverage
                val skillPick = when (day) {
                    1 -> skillAttempts.firstOrNull()
                    2 -> skillMaxHold.firstOrNull()
                    3 -> skillForTimeReps.firstOrNull()
                    4 -> skillEmom.firstOrNull()
                    else -> skillAmrap.firstOrNull() ?: (skillAttempts + skillMaxHold + skillForTimeReps).firstOrNull()
                }
                skillPick?.let {
                    toInsert.add(
                        DayItemEntity(
                            dayPlanId = dayPlanId,
                            itemType = "SKILL",
                            refId = it.id,
                            required = true,
                            sortOrder = 300
                        )
                    )
                }

                Log.d(TAG, "Initialized W1D$day → +${toInsert.size} items")
            } else {
                // Already has items → top-up Metcon, ensure ≥1 Engine & ≥1 Skill without duplicates
                val existingMetcons = planDao.flowDayMetconsFor(dayPlanId).first()
                val usedMetconIds = existingMetcons.map { it.planId }.toSet()
                val extraMetconNeeded = (3 - existingMetcons.size).coerceAtLeast(0)
                if (extraMetconNeeded > 0) {
                    val startOrder = (existingMetcons.maxOfOrNull { it.sortOrder } ?: 99) + 1
                    val extras = allMetcons.filter { it.id !in usedMetconIds }.take(extraMetconNeeded)
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

                // Ensure at least one ENGINE
                val existingEngineIds = planDao.engineItemIdsForDay(dayPlanId).toSet()
                if (existingEngineIds.isEmpty()) {
                    val anyEngine = (engineEmomFlagged + engineForTime + engineForDistance + engineForCalories)
                        .firstOrNull()
                    anyEngine?.let {
                        toInsert.add(
                            DayItemEntity(
                                dayPlanId = dayPlanId,
                                itemType = "ENGINE",
                                refId = it.id,
                                required = true,
                                sortOrder = 200
                            )
                        )
                    }
                }

                // Ensure at least one SKILL
                val existingSkillIds = planDao.skillItemIdsForDay(dayPlanId).toSet()
                if (existingSkillIds.isEmpty()) {
                    val anySkill = (skillEmom + skillAmrap + skillForTimeReps + skillMaxHold + skillAttempts)
                        .firstOrNull()
                    anySkill?.let {
                        toInsert.add(
                            DayItemEntity(
                                dayPlanId = dayPlanId,
                                itemType = "SKILL",
                                refId = it.id,
                                required = true,
                                sortOrder = 300
                            )
                        )
                    }
                }

                if (toInsert.isEmpty()) {
                    Log.d(TAG, "W1D$day already has sufficient items; no top-up")
                } else {
                    Log.d(TAG, "Top-up W1D$day → +${toInsert.size} item(s)")
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
