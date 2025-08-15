package com.example.safitness.data.repo

import com.example.safitness.core.Equipment
import com.example.safitness.core.MetconResult
import com.example.safitness.data.dao.*
import com.example.safitness.data.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class WorkoutRepository(
    private val libraryDao: LibraryDao,
    private val programDao: ProgramDao,
    private val sessionDao: SessionDao,
    private val prDao: PersonalRecordDao,
    private val metconDao: MetconDao,
    private val planDao: PlanDao
) {
    /* ---------- Phase 1: unified Metcon display DTO ---------- */
    data class MetconDisplay(
        val type: String,         // "FOR_TIME" | "AMRAP" | "EMOM" | "LEGACY"
        val timeSeconds: Int?,
        val rounds: Int?,
        val extraReps: Int?,
        val intervals: Int?,
        val result: String?,      // RX | Scaled | null
        val createdAt: Long
    )

    /* ---------- Library ---------- */
    fun getExercises(type: com.example.safitness.core.WorkoutType?, eq: com.example.safitness.core.Equipment?) =
        libraryDao.getExercises(type, eq)
    suspend fun countExercises() = libraryDao.countExercises()

    /* ---------- Program (strength) ---------- */
    suspend fun addToDay(
        day: Int,
        exercise: Exercise,
        required: Boolean,
        preferred: com.example.safitness.core.Equipment?,
        targetReps: Int?
    ) = programDao.upsert(
        ProgramSelection(
            dayIndex = day,
            exerciseId = exercise.id,
            required = required,
            preferredEquipment = preferred,
            targetReps = targetReps
        )
    )

    /** Prefer Week/Day model if populated; else legacy. */
    fun programForDay(day: Int): Flow<List<ExerciseWithSelection>> =
        resolveDayPlanIdOrNull(week = 1, day = day).flatMapLatest { dayPlanId ->
            if (dayPlanId == null) {
                programDao.getProgramForDay(day)
            } else {
                planDao.flowDayStrengthFor(dayPlanId).flatMapLatest { rows ->
                    if (rows.isEmpty()) programDao.getProgramForDay(day)
                    else flowOf(rows.map { r ->
                        ExerciseWithSelection(
                            exercise = r.exercise,
                            required = r.required,
                            preferredEquipment = null,
                            targetReps = r.targetReps
                        )
                    })
                }
            }
        }

    suspend fun setRequired(day: Int, exerciseId: Long, required: Boolean) =
        programDao.setRequired(day, exerciseId, required)
    suspend fun setTargetReps(day: Int, exerciseId: Long, reps: Int?) =
        programDao.setTargetReps(day, exerciseId, reps)
    suspend fun removeFromDay(day: Int, exerciseId: Long) =
        programDao.remove(day, exerciseId)
    suspend fun isInProgram(day: Int, exerciseId: Long) =
        programDao.exists(day, exerciseId) > 0
    suspend fun selectedTargetReps(day: Int, exerciseId: Long) =
        programDao.getTargetReps(day, exerciseId)
    suspend fun requiredFor(day: Int, exerciseId: Long) =
        programDao.getRequired(day, exerciseId) ?: false
    suspend fun daySummaryLabel(day: Int): String {
        val types = programDao.distinctTypesForDay(day)
        return when {
            types.isEmpty() -> "Empty"
            types.size > 1 -> "Mixed"
            else -> types.first().name.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    /* ---------- Sessions / logging ---------- */
    suspend fun startSession(day: Int): Long =
        sessionDao.insertSession(WorkoutSession(dayIndex = day))

    suspend fun logStrengthSet(
        sessionId: Long, exerciseId: Long, equipment: Equipment,
        setNumber: Int, reps: Int, weight: Double, rpe: Double?,
        success: Boolean, notes: String?
    ): Long = sessionDao.insertSet(
        SetLog(
            sessionId = sessionId, exerciseId = exerciseId, equipment = equipment,
            setNumber = setNumber, reps = reps, weight = weight, timeSeconds = null,
            rpe = rpe, success = success, notes = notes
        )
    )

    suspend fun logTimeOnlySet(
        sessionId: Long, exerciseId: Long, equipment: Equipment,
        setNumber: Int, timeSeconds: Int, rpe: Double?, success: Boolean?, notes: String?
    ): Long = sessionDao.insertSet(
        SetLog(
            sessionId = sessionId, exerciseId = exerciseId, equipment = equipment,
            setNumber = setNumber, reps = 0, weight = null, timeSeconds = timeSeconds,
            rpe = rpe, success = success, notes = notes
        )
    )

    suspend fun logMetcon(day: Int, seconds: Int, resultType: MetconResult) {
        val sessionId = startSession(day)
        sessionDao.insertSet(
            SetLog(
                sessionId = sessionId, exerciseId = 0L, equipment = Equipment.BODYWEIGHT,
                setNumber = 1, reps = 0, weight = null, timeSeconds = seconds,
                rpe = null, success = null, notes = null, metconResult = resultType
            )
        )
    }

    suspend fun lastMetconSecondsForDay(day: Int): Int = sessionDao.lastMetconSecondsForDay(day) ?: 0
    suspend fun lastMetconForDay(day: Int): MetconSummary? = sessionDao.lastMetconForDay(day)

    /* ---------- PR / suggestions ---------- */
    suspend fun bestPR(exerciseId: Long) = prDao.bestForExercise(exerciseId)
    suspend fun getLastSuccessfulWeight(exerciseId: Long, equipment: Equipment, reps: Int?): Double? {
        val recent = sessionDao.lastSets(exerciseId, equipment, 20, reps)
        return recent.firstOrNull { it.success == true && it.weight != null }?.weight
    }
    suspend fun getSuggestedWeight(exerciseId: Long, equipment: Equipment, reps: Int?): Double? =
        getLastSuccessfulWeight(exerciseId, equipment, reps)?.let { it * 1.02 }

    /* ---------- Metcon library (plans) ---------- */
    fun metconPlans(): Flow<List<MetconPlan>> = metconDao.getAllPlans()

    /** Prefer day_item (METCON) → PlanWithComponents; else legacy. */
    fun metconsForDay(day: Int): Flow<List<SelectionWithPlanAndComponents>> =
        resolveDayPlanIdOrNull(week = 1, day = day).flatMapLatest { dayPlanId ->
            if (dayPlanId == null) {
                metconDao.getMetconsForDay(day)
            } else {
                planDao.flowDayMetconsFor(dayPlanId).flatMapLatest { rows ->
                    if (rows.isEmpty()) {
                        metconDao.getMetconsForDay(day)
                    } else {
                        val flows = rows.map { row ->
                            metconDao.getPlanWithComponents(row.planId).map { pwc ->
                                SelectionWithPlanAndComponents(
                                    selection = ProgramMetconSelection(
                                        id = 0L, dayIndex = day, planId = row.planId,
                                        required = row.required, displayOrder = row.sortOrder
                                    ),
                                    planWithComponents = pwc
                                )
                            }
                        }
                        if (flows.isEmpty()) flowOf(emptyList())
                        else combine(flows) { it.toList().sortedBy { s -> s.selection.displayOrder } }
                    }
                }
            }
        }

    fun planWithComponents(planId: Long): Flow<PlanWithComponents> =
        metconDao.getPlanWithComponents(planId)

    /* ---------- EDITS (dual-path) ---------- */

    /** Add a metcon to a day: write to day_item when a plan exists; otherwise legacy table. */
    suspend fun addMetconToDay(day: Int, planId: Long, required: Boolean, orderInDay: Int): Long {
        val dayPlanId = currentDayPlanIdOrNull(week = 1, day = day)
        return if (dayPlanId == null) {
            // Legacy path
            metconDao.upsertSelection(
                ProgramMetconSelection(dayIndex = day, planId = planId, required = required, displayOrder = orderInDay)
            )
        } else {
            // New model path (skip duplicates)
            if (planDao.metconItemCount(dayPlanId, planId) == 0) {
                planDao.insertItems(
                    listOf(
                        DayItemEntity(
                            dayPlanId = dayPlanId,
                            itemType = "METCON",
                            refId = planId,
                            required = required,
                            sortOrder = orderInDay
                        )
                    )
                )
            }
            0L
        }
    }

    /** Remove a metcon from a day (dual-path). */
    suspend fun removeMetconFromDay(day: Int, planId: Long) {
        val dayPlanId = currentDayPlanIdOrNull(week = 1, day = day)
        if (dayPlanId == null) {
            metconDao.removeSelection(day, planId)
        } else {
            planDao.deleteMetconItem(dayPlanId, planId)
        }
    }

    /** Toggle required flag (dual-path). */
    suspend fun setMetconRequired(day: Int, planId: Long, required: Boolean) {
        val dayPlanId = currentDayPlanIdOrNull(week = 1, day = day)
        if (dayPlanId == null) {
            metconDao.setRequired(day, planId, required)
        } else {
            planDao.updateMetconRequired(dayPlanId, planId, required)
        }
    }

    /** Update display order (dual-path). */
    suspend fun setMetconOrder(day: Int, planId: Long, orderInDay: Int) {
        val dayPlanId = currentDayPlanIdOrNull(week = 1, day = day)
        if (dayPlanId == null) {
            metconDao.setDisplayOrder(day, planId, orderInDay)
        } else {
            planDao.updateMetconOrder(dayPlanId, planId, orderInDay)
        }
    }

    /* ---------- Plan-scoped logs (already Phase 1) ---------- */

    fun lastMetconForPlan(planId: Long): Flow<MetconLog?> = metconDao.lastForPlan(planId)

    suspend fun logMetconForTime(day: Int, planId: Long, timeSeconds: Int, result: MetconResult) {
        val log = MetconLog(
            dayIndex = day, planId = planId, type = "FOR_TIME",
            durationSeconds = 0, timeSeconds = timeSeconds, rounds = null, extraReps = null,
            intervalsCompleted = null, result = result.name
        )
        metconDao.insertLog(log)
    }

    suspend fun logMetconAmrap(
        day: Int, planId: Long, durationSeconds: Int, rounds: Int, extraReps: Int, result: MetconResult
    ) {
        val log = MetconLog(
            dayIndex = day, planId = planId, type = "AMRAP",
            durationSeconds = durationSeconds, timeSeconds = null,
            rounds = rounds, extraReps = extraReps, intervalsCompleted = null, result = result.name
        )
        metconDao.insertLog(log)
    }

    suspend fun logMetconEmom(
        day: Int, planId: Long, durationSeconds: Int, intervalsCompleted: Int?, result: MetconResult
    ) {
        val log = MetconLog(
            dayIndex = day, planId = planId, type = "EMOM",
            durationSeconds = durationSeconds, timeSeconds = null, rounds = null, extraReps = null,
            intervalsCompleted = intervalsCompleted, result = result.name
        )
        metconDao.insertLog(log)
    }

    /* ---------- Unified "last metcon" (prefer logs; fallback legacy) ---------- */

    fun lastMetconDisplayForDay(day: Int): Flow<MetconDisplay?> =
        metconDao.lastForDay(day).flatMapLatest { log ->
            if (log != null) {
                flowOf<MetconDisplay?>(
                    MetconDisplay(
                        type = log.type,
                        timeSeconds = log.timeSeconds,
                        rounds = log.rounds,
                        extraReps = log.extraReps,
                        intervals = log.intervalsCompleted,
                        result = log.result,
                        createdAt = log.createdAt
                    )
                )
            } else {
                flow<MetconDisplay?> {
                    val legacy = sessionDao.lastMetconForDay(day)
                    emit(
                        legacy?.let {
                            MetconDisplay(
                                type = "LEGACY",
                                timeSeconds = it.timeSeconds,
                                rounds = null,
                                extraReps = null,
                                intervals = null,
                                result = null,
                                createdAt = 0L // legacy has no timestamp
                            )
                        }
                    )
                }
            }
        }

    /* ===================== Phase 0 — plan helpers ===================== */

    suspend fun getPlanFor(phaseId: Long, week: Int, day: Int): WeekDayPlanEntity? =
        planDao.getPlan(phaseId, week, day)

    suspend fun getNextPlannedDay(afterDayPlanId: Long): WeekDayPlanEntity? {
        val current = planDao.getPlanById(afterDayPlanId) ?: return null
        return planDao.getNextAfter(current.phaseId, current.weekIndex, current.dayIndex)
    }

    suspend fun attachCalendar(phaseId: Long, startEpochDay: Long) {
        val plans = planDao.getPlansForPhaseOrdered(phaseId)
        var cursor = startEpochDay
        plans.forEach { plan -> planDao.updatePlanDate(plan.id, cursor); cursor += 1 }
    }

    /* ---------- Internal ---------- */

    private suspend fun currentDayPlanIdOrNull(week: Int, day: Int): Long? {
        val phaseId = planDao.currentPhaseId() ?: return null
        return planDao.getPlanId(phaseId, week, day)
    }

    private fun resolveDayPlanIdOrNull(week: Int, day: Int): Flow<Long?> = flowOf(Unit).map {
        val phaseId = planDao.currentPhaseId() ?: return@map null
        planDao.getPlanId(phaseId, week, day)
    }
}
