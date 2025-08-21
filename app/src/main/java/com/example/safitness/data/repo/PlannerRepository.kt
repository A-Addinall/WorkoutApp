package com.example.safitness.data.repo

import com.example.safitness.core.Equipment
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.dao.LibraryDao
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
import com.example.safitness.data.dao.MetconDao

class PlannerRepository(
    private val planDao: PlanDao,
    private val libraryDao: LibraryDao,
    private val metconDao: MetconDao
) {
    private val planner = SimplePlanner(libraryDao)
    private val allowedRepBuckets = intArrayOf(3, 5, 8, 10, 12, 15)

    suspend fun generateSuggestionsForDay(
        dayIndex: Int,
        focus: WorkoutType,
        availableEq: List<Equipment>
    ): List<Suggestion> = planner.suggestFor(
        focus = focus,
        availableEq = availableEq
    )

    suspend fun persistSuggestionsToDay(
        dayIndex: Int,
        suggestions: List<Suggestion>,
        replaceStrength: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val phaseId = ensurePhaseAndWeek()
        var dayPlanId = planDao.getPlanId(phaseId, week = 1, day = dayIndex)
        if (dayPlanId == null) {
            planDao.insertWeekPlans(
                listOf(
                    WeekDayPlanEntity(
                        id = 0L,
                        phaseId = phaseId,
                        weekIndex = 1,
                        dayIndex = dayIndex,
                        dateEpochDay = null
                    )
                )
            )
            dayPlanId = planDao.getPlanId(phaseId, week = 1, day = dayIndex)
        }
        if (dayPlanId == null) return@withContext

        if (replaceStrength) {
            planDao.clearStrength(dayPlanId) // requires the DAO method above
        }

        var nextOrder = planDao.nextStrengthSortOrder(dayPlanId)
        val toInsert = mutableListOf<DayItemEntity>()

        suggestions.forEach { s ->
            val exerciseId = s.exercise.id
            val exists = planDao.strengthItemCount(dayPlanId, exerciseId) > 0

            val target = pickTargetReps(s.repsMin, s.repsMax)

            if (exists) {
                planDao.updateStrengthTargetReps(dayPlanId, exerciseId, target)
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

    private suspend fun ensurePhaseAndWeek(): Long {
        planDao.currentPhaseId()?.let { return it }

        // create a default phase (fix: compute epoch inline)
        val phaseId = planDao.insertPhase(
            PhaseEntity(
                id = 0L,
                name = "Auto Phase",
                startDateEpochDay = LocalDate.now().toEpochDay(),
                weeks = 4
            )
        )

        // create week 1 (days 1..5)
        val weekPlans = (1..5).map { day ->
            WeekDayPlanEntity(
                id = 0L,
                phaseId = phaseId,
                weekIndex = 1,
                dayIndex = day,
                dateEpochDay = null
            )
        }
        planDao.insertWeekPlans(weekPlans)

        return phaseId
    }

    // In PlannerRepository.kt

    suspend fun persistMetconPlanToDay(
        dayIndex: Int,
        planId: Long,
        replaceMetcon: Boolean = false,
        required: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val phaseId = ensurePhaseAndWeek()
        var dayPlanId = planDao.getPlanId(phaseId, week = 1, day = dayIndex)
        if (dayPlanId == null) {
            planDao.insertWeekPlans(
                listOf(
                    WeekDayPlanEntity(
                        id = 0L,
                        phaseId = phaseId,
                        weekIndex = 1,
                        dayIndex = dayIndex,
                        dateEpochDay = null
                    )
                )
            )
            dayPlanId = planDao.getPlanId(phaseId, week = 1, day = dayIndex)
        }
        if (dayPlanId == null) return@withContext

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

    suspend fun regenerateDayWithMetcon(
        dayIndex: Int,
        focus: WorkoutType,
        availableEq: List<Equipment>,
        metconPlanId: Long
    ) = withContext(Dispatchers.IO) {
        val suggestions = generateSuggestionsForDay(dayIndex, focus, availableEq)
        persistSuggestionsToDay(dayIndex, suggestions, replaceStrength = true)
        persistMetconPlanToDay(dayIndex, metconPlanId, replaceMetcon = true, required = true)
    }


    private fun pickTargetReps(min: Int, max: Int): Int {
        allowedRepBuckets.firstOrNull { it in min..max }?.let { return it }
        return allowedRepBuckets.minBy { abs(it - min) }
    }
}
