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
import kotlinx.coroutines.flow.first
import com.example.safitness.data.entities.MetconPlan
import com.example.safitness.core.MetconType
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
        availableEq: List<Equipment>,
        epochDay: Long? = null
    ): List<Suggestion> = planner.suggestFor(
        focus = focus,
        availableEq = availableEq,
        epochDay = epochDay
    )

    /** Persist strength suggestions for a concrete calendar date. */
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

        // Normalise names so two DB rows that look the same to the user count as duplicates.
        fun canonical(name: String) =
            name.trim().lowercase().replace("\\s+".toRegex(), " ")

        // 1) de-dupe by id, then by normalised name; 2) cap at main+2 accessories
        val unique = suggestions
            .asSequence()
            .filter { it.exercise.id != 0L }
            .distinctBy { it.exercise.id }
            .distinctBy { canonical(it.exercise.name) }
            .take(3)
            .toList()

        // Protect against any last-mile duplication on this day
        val seenIds = mutableSetOf<Long>()
        var nextOrder = planDao.nextStrengthSortOrder(dayPlanId)
        val toInsert = mutableListOf<DayItemEntity>()

        unique.forEach { spec ->
            val exId = spec.exercise.id
            if (!seenIds.add(exId)) return@forEach  // already queued this exercise

            val exists = planDao.strengthItemCount(dayPlanId, exId) > 0
            val target = pickTargetReps(spec.repsMin, spec.repsMax)

            if (exists) {
                // Update target / required in place
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
    /** Attach/replace a SKILL plan for a concrete calendar date. */
    /** Attach (or update) a Skill plan for a date. Default is additive, not replacing others. */
    suspend fun persistSkillPlanToDate(
        epochDay: Long,
        planId: Long,
        replaceExisting: Boolean = false,
        required: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val dayPlanId = ensurePlanForDate(epochDay) ?: return@withContext

        if (replaceExisting) {
            val existing = planDao.skillItemIdsForDay(dayPlanId)
            for (ref in existing) planDao.deleteSkillItem(dayPlanId, ref)
        }

        val exists = planDao.skillItemCount(dayPlanId, planId) > 0
        if (exists) {
            planDao.updateSkillRequired(dayPlanId, planId, required)
        } else {
            val order = planDao.nextSortOrder(dayPlanId)
            planDao.insertItems(
                listOf(
                    DayItemEntity(
                        id = 0L,
                        dayPlanId = dayPlanId,
                        itemType = "SKILL",
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

    suspend fun clearStrengthAndMetconForDate(epochDay: Long) = withContext(Dispatchers.IO) {
        val phaseId = planDao.currentPhaseId() ?: return@withContext
        val dayPlanId = planDao.getPlanIdByDateInPhase(phaseId, epochDay)
            ?: planDao.getPlanIdByDate(epochDay)
            ?: return@withContext
        planDao.clearStrength(dayPlanId)
        planDao.clearMetcon(dayPlanId)
    }
    /** Attach/replace an ENGINE plan for a concrete calendar date. */
    /** Attach/replace Engine plan for a concrete calendar date. */
    suspend fun persistEnginePlanToDate(
        epochDay: Long,
        planId: Long,
        replaceExisting: Boolean = true,
        required: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val dayPlanId = ensurePlanForDate(epochDay) ?: return@withContext

        if (replaceExisting) {
            // No clearEngine() in DAO, so remove any existing ENGINE items explicitly
            val existing = planDao.engineItemIdsForDay(dayPlanId)
            for (ref in existing) planDao.deleteEngineItem(dayPlanId, ref)
        }

        val exists = planDao.engineItemCount(dayPlanId, planId) > 0
        if (exists) {
            planDao.updateEngineRequired(dayPlanId, planId, required)
        } else {
            val order = planDao.nextSortOrder(dayPlanId)
            planDao.insertItems(
                listOf(
                    DayItemEntity(
                        id = 0L,
                        dayPlanId = dayPlanId,
                        itemType = "ENGINE",
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
        // Pull suggestions with epochDay seed (variety across dates)
        val raw = generateSuggestions(focus = focus, availableEq = availableEq, epochDay = epochDay)

        // 1) De-dupe by exercise id
        val dedup = raw.distinctBy { it.exercise.id }

        // 2) Keep a sensible cap (main + up to 2 accessories)
        val suggestions = if (dedup.size <= 3) dedup else dedup.take(3)

        // Persist — strength first (replace), then the chosen metcon (replace)
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

        if (replaceStrength) planDao.clearStrength(dayPlanId)

        fun canonical(name: String) =
            name.trim().lowercase().replace("\\s+".toRegex(), " ")

        // ---------- Strength ----------
        var nextOrder = planDao.nextStrengthSortOrder(dayPlanId)
        val toInsert = mutableListOf<DayItemEntity>()
        val seenIds = mutableSetOf<Long>()
        val seenNames = mutableSetOf<String>()

        // De-dupe by exerciseId and also by normalised name
        val strength = resp.strength
            .asSequence()
            .filter { it.exerciseId != 0L }
            .filter { seenIds.add(it.exerciseId) }
            .toList()

        strength.forEach { spec ->
            val exId = spec.exerciseId
            val exists = planDao.strengthItemCount(dayPlanId, exId) > 0

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

        if (toInsert.isNotEmpty()) {
            planDao.insertItems(toInsert)
        }

        // ---------- Metcon ----------
        resp.metcon?.let {
            val planId = pickMetconPlanIdForFocus(
                focus = focus,
                availableEq = availableEq,
                epochDay = epochDay            // deterministic variety by date
            )
            if (planId != null) {
                persistMetconPlanToDate(epochDay, planId, replaceMetcon = replaceMetcon, required = true)
            }
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

    /** Choose a metcon plan deterministically based on the date, to add variety across the week. */
    suspend fun pickMetconPlanIdForFocus(
        focus: com.example.safitness.core.WorkoutType,
        availableEq: List<com.example.safitness.core.Equipment>,
        epochDay: Long
    ): Long? = withContext(kotlinx.coroutines.Dispatchers.IO) {
        // Fetch all (non-archived) plans
        val plans: List<MetconPlan> =
            metconDao.getAllPlans().first().filter { !it.isArchived }  // Flow<List<MetconPlan>> -> List<MetconPlan>
        if (plans.isEmpty()) return@withContext null

        // Gentle bias: AMRAP for PUSH/PULL; FOR_TIME for LEGS_CORE; otherwise neutral
        val preferredType: MetconType? = when (focus) {
            com.example.safitness.core.WorkoutType.PUSH,
            com.example.safitness.core.WorkoutType.PULL -> MetconType.AMRAP
            com.example.safitness.core.WorkoutType.LEGS_CORE -> MetconType.FOR_TIME
            else -> null
        }

        val ranked = plans.sortedWith(
            compareBy<MetconPlan> { p -> if (preferredType != null && p.type == preferredType) 0 else 1 }
                .thenBy { it.title }
        )

        val idx = (kotlin.math.abs(epochDay) % ranked.size).toInt()
        ranked[idx].id
    }


    private fun pickTargetReps(min: Int, max: Int): Int {
        allowedRepBuckets.firstOrNull { it in min..max }?.let { return it }
        return allowedRepBuckets.minBy { abs(it - min) }
    }
}
