package com.example.safitness.data.repo

import com.example.safitness.core.Equipment
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.dao.LibraryDao
import com.example.safitness.data.dao.MetconDao
import com.example.safitness.data.dao.PlanDao
import com.example.safitness.data.entities.DayItemEntity
import com.example.safitness.data.entities.PhaseEntity
import com.example.safitness.data.entities.WeekDayPlanEntity
import com.example.safitness.domain.planner.SimplePlanner
import com.example.safitness.domain.planner.Suggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import java.time.LocalDate
import com.example.safitness.core.BlockType
import com.example.safitness.core.MovementPattern

class PlannerRepository(
    private val planDao: PlanDao,
    private val libraryDao: LibraryDao,
    private val metconDao: MetconDao
) {
    private val planner = SimplePlanner(libraryDao)
    private val allowedRepBuckets = intArrayOf(3, 5, 8, 10, 12, 15)

    /** Pure suggestion generation (date-agnostic). */
    suspend fun generateSuggestions(
        focus: WorkoutType,
        availableEq: List<Equipment>
    ): List<Suggestion> = planner.suggestFor(
        focus = focus,
        availableEq = availableEq
    )

    /** Persist strength suggestions for a concrete calendar date. */
    suspend fun persistSuggestionsToDate(
        epochDay: Long,
        suggestions: List<Suggestion>,
        replaceStrength: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val dayPlanId = ensurePlanForDate(epochDay) ?: return@withContext

        if (replaceStrength) {
            planDao.clearStrength(dayPlanId)
        }

        var nextOrder = planDao.nextStrengthSortOrder(dayPlanId)
        val toInsert = mutableListOf<DayItemEntity>()

        suggestions.forEach { s ->
            val exerciseId = s.exercise.id
            val exists = planDao.strengthItemCount(dayPlanId, exerciseId) > 0
            val target = pickTargetReps(s.repsMin, s.repsMax)

            if (exists) {
                planDao.updateStrengthTargetReps(dayPlanId, exerciseId, target)
                planDao.updateStrengthRequired(dayPlanId, exerciseId, true)
            } else {
                toInsert += DayItemEntity(
                    id = 0L,
                    dayPlanId = dayPlanId,
                    itemType = "STRENGTH",
                    refId = exerciseId,
                    required = true,
                    sortOrder = nextOrder++,
                    targetReps = target,
                    prescriptionJson = null
                )
            }
        }

        if (toInsert.isNotEmpty()) {
            planDao.insertItems(toInsert)
        }
    }

    /** Attach/replace metcon plan for a concrete calendar date. */
    suspend fun persistMetconPlanToDate(
        epochDay: Long,
        planId: Long,
        replaceMetcon: Boolean = false,
        required: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val dayPlanId = ensurePlanForDate(epochDay) ?: return@withContext

        if (replaceMetcon) {
            planDao.clearMetcon(dayPlanId)
        }

        val exists = planDao.metconItemCount(dayPlanId, planId) > 0
        if (exists) {
            planDao.updateMetconRequired(dayPlanId, planId, required)
        } else {
            val order = planDao.nextSortOrder(dayPlanId)
            planDao.insertItems(
                listOf(
                    DayItemEntity(
                        id = 0L,
                        dayPlanId = dayPlanId,
                        itemType = "METCON",
                        refId = planId,
                        required = required,
                        sortOrder = order,
                        targetReps = null,
                        prescriptionJson = null
                    )
                )
            )
        }
    }

    /** One-shot convenience: (re)build a date with strength + metcon. */
    suspend fun regenerateDateWithMetcon(
        epochDay: Long,
        focus: WorkoutType,
        availableEq: List<Equipment>,
        metconPlanId: Long
    ) = withContext(Dispatchers.IO) {
        val suggestions = generateSuggestions(focus, availableEq)
        persistSuggestionsToDate(epochDay, suggestions, replaceStrength = true)
        persistMetconPlanToDate(epochDay, metconPlanId, replaceMetcon = true, required = true)
    }

    private suspend fun ensurePlanForDate(epochDay: Long): Long? {
        planDao.getPlanIdByDate(epochDay)?.let { return it }

        val phaseId = ensurePhase()
        // Minimal week/day placeholders; dateEpochDay is canonical.
        val row = WeekDayPlanEntity(
            id = 0L,
            phaseId = phaseId,
            weekIndex = 1,
            dayIndex = 1,
            dateEpochDay = epochDay
        )
        planDao.insertWeekPlans(listOf(row))
        return planDao.getPlanIdByDate(epochDay)
    }

    private suspend fun ensurePhase(): Long {
        planDao.currentPhaseId()?.let { return it }
        return planDao.insertPhase(
            PhaseEntity(
                id = 0L,
                name = "Auto Phase",
                startDateEpochDay = LocalDate.now().toEpochDay(),
                weeks = 4
            )
        )
    }
    suspend fun applyMlToDate(
        epochDay: Long,
        resp: com.example.safitness.ml.GenerateResponse,
        focus: com.example.safitness.core.WorkoutType,
        availableEq: List<com.example.safitness.core.Equipment>,
        replaceStrength: Boolean = false,
        replaceMetcon: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val dayPlanId = ensurePlanForDate(epochDay) ?: return@withContext

        // ---------- Strength ----------
        if (replaceStrength) {
            planDao.clearStrength(dayPlanId)
        }
        var nextOrder = planDao.nextStrengthSortOrder(dayPlanId)
        val toInsert = mutableListOf<DayItemEntity>()

        resp.strength.forEach { spec ->
            val exId = spec.exerciseId
            val exists = planDao.strengthItemCount(dayPlanId, exId) > 0
            // Snap target reps to your allowed buckets
            val target = run {
                val buckets = intArrayOf(3, 5, 8, 10, 12, 15)
                buckets.minBy { kotlin.math.abs(it - spec.targetReps) }
            }

            if (exists) {
                planDao.updateStrengthTargetReps(dayPlanId, exId, target)
                planDao.updateStrengthRequired(dayPlanId, exId, true)
            } else {
                toInsert += DayItemEntity(
                    id = 0L,
                    dayPlanId = dayPlanId,
                    itemType = "STRENGTH",
                    refId = exId,
                    required = true,
                    sortOrder = nextOrder++,
                    targetReps = target,
                    prescriptionJson = null
                )
            }
        }
        if (toInsert.isNotEmpty()) planDao.insertItems(toInsert)

        // ---------- Metcon ----------
        resp.metcon?.let { m ->
            // Prefer to *choose* an existing plan that matches focus & block type
            val preferredBlock = when (m.blockType) {
                com.example.safitness.core.BlockType.EMOM     -> com.example.safitness.core.BlockType.EMOM
                com.example.safitness.core.BlockType.AMRAP    -> com.example.safitness.core.BlockType.AMRAP
                com.example.safitness.core.BlockType.FOR_TIME -> com.example.safitness.core.BlockType.FOR_TIME
                else -> null
            }
            val planId = pickMetconPlanIdForFocus(focus, availableEq, preferredBlock)
            if (planId != null) {
                persistMetconPlanToDate(epochDay, planId, replaceMetcon = replaceMetcon, required = true)
            }
            // If no plan matched, we silently skip metcon (keeps UX predictable).
        }
    }



    private fun focusMovementsFor(type: WorkoutType): List<MovementPattern> = when (type) {
        WorkoutType.PUSH -> listOf(
            MovementPattern.HORIZONTAL_PUSH,
            MovementPattern.VERTICAL_PUSH
        )
        WorkoutType.PULL -> listOf(
            MovementPattern.VERTICAL_PULL,
            MovementPattern.HORIZONTAL_PULL
        )
        WorkoutType.LEGS_CORE -> listOf(
            MovementPattern.SQUAT,
            MovementPattern.HINGE,
            MovementPattern.LUNGE
        )
        else -> emptyList()
    }

    suspend fun pickMetconPlanIdForFocus(
        focus: WorkoutType,
        availableEq: List<com.example.safitness.core.Equipment>,
        preferredBlock: BlockType? = null
    ): Long? {
        val movements = focusMovementsFor(focus)
        val hits = metconDao.rankPlansForFocus(
            blockType = preferredBlock,
            movements = movements,
            movementFilterEmpty = if (movements.isEmpty()) 1 else 0,
            availableEquipment = if (availableEq.isEmpty()) listOf(
                com.example.safitness.core.Equipment.BODYWEIGHT,
                com.example.safitness.core.Equipment.DUMBBELL,
                com.example.safitness.core.Equipment.KETTLEBELL
            ) else availableEq,
            focus = focus,
            limit = 5
        )
        return hits.firstOrNull()?.planId
    }

    private fun pickTargetReps(min: Int, max: Int): Int {
        allowedRepBuckets.firstOrNull { it in min..max }?.let { return it }
        return allowedRepBuckets.minBy { abs(it - min) }
    }
}
