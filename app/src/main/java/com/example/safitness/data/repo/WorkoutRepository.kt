@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.safitness.data.repo

import android.util.Log
import com.example.safitness.core.Equipment
import com.example.safitness.core.MetconResult
import com.example.safitness.core.PrCelebrationEvent
import com.example.safitness.core.estimateOneRepMax
import com.example.safitness.core.repsToPercentage
import com.example.safitness.data.dao.*
import com.example.safitness.data.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max

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
    /* ---------- Program (strength) ---------- */
    suspend fun addToDay(
        day: Int,
        exercise: Exercise,
        required: Boolean,
        preferred: com.example.safitness.core.Equipment?,
        targetReps: Int?
    ) {
        val dayPlanId = currentDayPlanIdOrNull(week = 1, day = day)
        if (dayPlanId == null) {
            // legacy
            programDao.upsert(
                ProgramSelection(
                    dayIndex = day,
                    exerciseId = exercise.id,
                    required = required,
                    preferredEquipment = preferred,
                    targetReps = targetReps
                )
            )
        } else {
            // Phase/new model
            if (planDao.strengthItemCount(dayPlanId, exercise.id) == 0) {
                val order = planDao.nextStrengthSortOrder(dayPlanId)
                planDao.insertItems(
                    listOf(
                        DayItemEntity(
                            dayPlanId = dayPlanId,
                            itemType = "STRENGTH",
                            refId = exercise.id,
                            required = required,
                            sortOrder = order,
                            targetReps = targetReps
                        )
                    )
                )
            } else {
                // If already present, just update optional fields
                planDao.updateStrengthRequired(dayPlanId, exercise.id, required)
                planDao.updateStrengthTargetReps(dayPlanId, exercise.id, targetReps)
            }
        }
    }

    suspend fun setRequired(day: Int, exerciseId: Long, required: Boolean) {
        val dayPlanId = currentDayPlanIdOrNull(week = 1, day = day)
        if (dayPlanId == null) programDao.setRequired(day, exerciseId, required)
        else planDao.updateStrengthRequired(dayPlanId, exerciseId, required)
    }

    suspend fun setTargetReps(day: Int, exerciseId: Long, reps: Int?) {
        val dayPlanId = currentDayPlanIdOrNull(week = 1, day = day)
        if (dayPlanId == null) programDao.setTargetReps(day, exerciseId, reps)
        else planDao.updateStrengthTargetReps(dayPlanId, exerciseId, reps)
    }

    suspend fun removeFromDay(day: Int, exerciseId: Long) {
        val dayPlanId = currentDayPlanIdOrNull(week = 1, day = day)
        if (dayPlanId == null) programDao.remove(day, exerciseId)
        else planDao.deleteStrengthItem(dayPlanId, exerciseId)
    }

    suspend fun isInProgram(day: Int, exerciseId: Long): Boolean {
        val dayPlanId = currentDayPlanIdOrNull(week = 1, day = day)
        return if (dayPlanId == null) programDao.exists(day, exerciseId) > 0
        else planDao.existsStrength(dayPlanId, exerciseId) > 0
    }

    suspend fun selectedTargetReps(day: Int, exerciseId: Long): Int? {
        val dayPlanId = currentDayPlanIdOrNull(week = 1, day = day)
        return if (dayPlanId == null) programDao.getTargetReps(day, exerciseId)
        else planDao.getStrengthTargetReps(dayPlanId, exerciseId)
    }

    suspend fun requiredFor(day: Int, exerciseId: Long): Boolean {
        val dayPlanId = currentDayPlanIdOrNull(week = 1, day = day)
        return if (dayPlanId == null) programDao.getRequired(day, exerciseId) ?: false
        else planDao.getStrengthRequired(dayPlanId, exerciseId) ?: false
    }


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

    suspend fun logMetcon(day: Int, seconds: Int, result: MetconResult) {
        val sessionId = startSession(day)
        sessionDao.insertSet(
            SetLog(
                sessionId = sessionId, exerciseId = 0L, equipment = Equipment.BODYWEIGHT,
                setNumber = 1, reps = 0, weight = null, timeSeconds = seconds,
                rpe = null, success = null, notes = null, metconResult = result
            )
        )
    }

    suspend fun lastMetconSecondsForDay(day: Int): Int = sessionDao.lastMetconSecondsForDay(day) ?: 0
    suspend fun lastMetconForDay(day: Int): MetconSummary? = sessionDao.lastMetconForDay(day)

    /* ---------- PR / suggestions ---------- */
    suspend fun bestE1RM(exerciseId: Long, equipment: Equipment): Double? {
        return prDao.bestEstimated1RM(exerciseId, equipment)
    }

    suspend fun bestRMAtReps(exerciseId: Long, equipment: Equipment, reps: Int): Double? {
        return prDao.bestWeightAtReps(exerciseId, equipment, reps)
    }

    suspend fun getLastSuccessfulWeight(exerciseId: Long, equipment: Equipment, reps: Int?): Double? {
        val recent = sessionDao.lastSets(exerciseId, equipment, 20, reps)
        return recent.firstOrNull { it.success == true && it.weight != null }?.weight
    }

    /**
     * Suggest a next load for a planned set of `reps`.
     * Prefers historical E1RM; if none, returns null (UI can hide suggestion).
     */
    suspend fun suggestNextLoadKg(
        exerciseId: Long,
        equipment: Equipment,
        reps: Int
    ): Double? {
        val e1rm = bestE1RM(exerciseId, equipment) ?: return null
        val pct = repsToPercentage(reps)
        return e1rm * pct
    }

    /** Evaluate PRs, persist records, and return a unified celebration event (or null). */
    suspend fun evaluateAndRecordPrIfAny(
        exerciseId: Long,
        equipment: Equipment,
        reps: Int,
        weightKg: Double,
        success: Boolean
    ): PrCelebrationEvent? {
        if (!success) {
            Log.d("PR", "No PR: success=false")
            return null
        }
        if (reps <= 0 || reps > 12) {
            Log.d("PR", "No PR: reps out of range ($reps)")
            return null
        }

        val date = todayEpochDayUtc()

        // Previous bests
        val prevHardKg = prDao.bestWeightAtReps(exerciseId, equipment, reps)
        val prevSoftE1rm = prDao.bestEstimated1RM(exerciseId, equipment)

        // Candidates
        val hardCandidateKg = weightKg
        val softCandidateE1rm = estimateOneRepMax(weightKg, reps)
        if (softCandidateE1rm == null) {
            Log.d("PR", "No PR: softCandidateE1rm=null")
            return null
        }

        // Thresholds
        val hardStep = equipmentMinIncrementKg(equipment)
        val hardImproved = (prevHardKg == null) || ((hardCandidateKg - prevHardKg) >= hardStep)
        val softImproved = (prevSoftE1rm == null) ||
                ((softCandidateE1rm - prevSoftE1rm) >= kotlin.math.max(1.0, prevSoftE1rm * 0.01))

        Log.d(
            "PR",
            "prevHard=$prevHardKg, newHard=$hardCandidateKg, hardStep=$hardStep, hardImproved=$hardImproved | " +
                    "prevE1rm=$prevSoftE1rm, newE1rm=$softCandidateE1rm, softImproved=$softImproved"
        )

        if (!hardImproved && !softImproved) return null

        // Persist both improvements if applicable
        if (softImproved) {
            prDao.upsertEstimated1RM(exerciseId, equipment, softCandidateE1rm, date)
            Log.d("PR", "Upserted E1RM=$softCandidateE1rm")
        }
        if (hardImproved) {
            prDao.upsertRepMax(exerciseId, equipment, reps, hardCandidateKg, date)
            Log.d("PR", "Upserted RM[$reps]=$hardCandidateKg")
        }

        val celebrateHard = hardImproved
        val event = PrCelebrationEvent(
            exerciseId = exerciseId,
            equipment = equipment,
            isHardPr = celebrateHard,
            reps = if (celebrateHard) reps else null,
            newWeightKg = if (celebrateHard) hardCandidateKg else null,
            prevWeightKg = if (celebrateHard) prevHardKg else null,
            newE1rmKg = softCandidateE1rm,
            prevE1rmKg = prevSoftE1rm
        )
        Log.d("PR", "Emitting PR event: $event")
        return event
    }

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

    /* ---------- Plan-scoped logs ---------- */

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
                flow {
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
                                createdAt = 0L
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
    /**
     * Preview whether logging this successful set WOULD produce a PR (no DB writes).
     * - Uses the same thresholds/guardrails as evaluateAndRecordPrIfAny(...)
     * - Returns a PrCelebrationEvent if it would be a PR, else null.
     */
    suspend fun previewPrEvent(
        exerciseId: Long,
        equipment: Equipment,
        reps: Int,
        weightKg: Double
    ): PrCelebrationEvent? {
        // Guardrails (as if success == true)
        if (reps <= 0 || reps > 12) return null

        val prevHardKg = prDao.bestWeightAtReps(exerciseId, equipment, reps)
        val prevSoftE1rm = prDao.bestEstimated1RM(exerciseId, equipment)

        val hardCandidateKg = weightKg
        val softCandidateE1rm = estimateOneRepMax(weightKg, reps) ?: return null

        val hardStep = equipmentMinIncrementKg(equipment)
        val hardImproved = (prevHardKg == null) || ((hardCandidateKg - prevHardKg) >= hardStep)
        val softImproved = (prevSoftE1rm == null) ||
                ((softCandidateE1rm - prevSoftE1rm) >= kotlin.math.max(1.0, prevSoftE1rm * 0.01))

        if (!hardImproved && !softImproved) return null

        val celebrateHard = hardImproved
        return PrCelebrationEvent(
            exerciseId = exerciseId,
            equipment = equipment,
            isHardPr = celebrateHard,
            reps = if (celebrateHard) reps else null,
            newWeightKg = if (celebrateHard) hardCandidateKg else null,
            prevWeightKg = if (celebrateHard) prevHardKg else null,
            newE1rmKg = softCandidateE1rm,
            prevE1rmKg = prevSoftE1rm
        )
    }


    /* ---------- Internal ---------- */

    private suspend fun currentDayPlanIdOrNull(week: Int, day: Int): Long? {
        val phaseId = planDao.currentPhaseId() ?: return null
        return planDao.getPlanId(phaseId, week, day)
    }

    private fun resolveDayPlanIdOrNull(week: Int, day: Int): Flow<Long?> = flow {
        val phaseId = planDao.currentPhaseId()
        if (phaseId == null) {
            emit(null)
        } else {
            emit(planDao.getPlanId(phaseId, week, day))
        }
    }
    // ===== Rest Timer (in-memory, additive) =====
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    data class RestState(
        val sessionId: Long,
        val exerciseId: Long,
        val durationMs: Long,
        val remainingMs: Long,
        val isRunning: Boolean
    )

    private val _restState = MutableStateFlow<RestState?>(null)
    val restTimerState: StateFlow<RestState?> = _restState
    private var ticker: Job? = null

    fun startRestTimer(sessionId: Long, exerciseId: Long, baseDurationMs: Long) {
        _restState.value = RestState(sessionId, exerciseId, baseDurationMs, baseDurationMs, true)
        restartTicker()
    }
    fun addFailBonusRest(ms: Long = 30_000L) {
        _restState.value = _restState.value?.let { it.copy(remainingMs = it.remainingMs + ms) }
    }
    fun pauseRestTimer() { _restState.value = _restState.value?.copy(isRunning = false); ticker?.cancel() }
    fun resumeRestTimer() { _restState.value = _restState.value?.copy(isRunning = true); restartTicker() }
    fun clearRestTimer() { ticker?.cancel(); _restState.value = null }

    private fun restartTicker() {
        ticker?.cancel()
        val s = _restState.value ?: return
        if (!s.isRunning) return
        ticker = repoScope.launch {
            while (isActive) {
                delay(1000L)
                val cur = _restState.value ?: return@launch
                if (!cur.isRunning) return@launch
                val next = max(0L, cur.remainingMs - 1000L)
                _restState.value = cur.copy(remainingMs = next)
                if (next == 0L) {
                    pauseRestTimer()
                    return@launch
                }
            }
        }
    }

}

/* ---------- Private utils ---------- */

private fun equipmentMinIncrementKg(equipment: Equipment): Double = when (equipment) {
    Equipment.BARBELL -> 2.5
    Equipment.DUMBBELL -> 1.0
    Equipment.KETTLEBELL -> 2.0
    else -> 1.0
}

private fun todayEpochDayUtc(): Long =
    java.time.LocalDate.now(java.time.ZoneOffset.UTC).toEpochDay()

