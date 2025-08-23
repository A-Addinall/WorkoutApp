@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.safitness.data.repo

import com.example.safitness.core.Equipment
import com.example.safitness.core.MetconResult
import com.example.safitness.core.PrCelebrationEvent
import com.example.safitness.core.estimateOneRepMax
import com.example.safitness.core.repsToPercentage
import com.example.safitness.data.dao.*
import com.example.safitness.data.entities.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.max

class WorkoutRepository(
    private val libraryDao: LibraryDao,
    private val programDao: ProgramDao,
    private val sessionDao: SessionDao,
    private val prDao: PersonalRecordDao,
    private val metconDao: MetconDao,
    private val planDao: PlanDao
) {
    /* ---------- Types ---------- */
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
    fun getExercises(
        type: com.example.safitness.core.WorkoutType?,
        eq: com.example.safitness.core.Equipment?
    ) = libraryDao.getExercises(type, eq)

    suspend fun countExercises() = libraryDao.countExercises()

    /** UTC “today” epochDay used for default writes. */
    val date: Long = LocalDate.now(ZoneOffset.UTC).toEpochDay()

    /* =========================================================
       STRENGTH — DATE-FIRST
       ========================================================= */
    fun programForDate(epochDay: Long): Flow<List<ExerciseWithSelection>> =
        planDao.flowPlanIdByDate(epochDay).flatMapLatest { dayPlanId ->
            if (dayPlanId != null) {
                planDao.flowDayStrengthFor(dayPlanId).map { rows ->
                    rows.map { r ->
                        ExerciseWithSelection(
                            exercise = r.exercise,
                            required = r.required,
                            preferredEquipment = null,
                            targetReps = r.targetReps
                        )
                    }
                }
            } else {
                flowOf(emptyList())
            }
        }

    suspend fun addStrengthToDate(
        epochDay: Long,
        exercise: Exercise,
        required: Boolean,
        preferred: com.example.safitness.core.Equipment?,
        targetReps: Int?
    ): Boolean {
        val dayPlanId = ensurePlanForDate(epochDay) ?: return false
        if (planDao.strengthItemCount(dayPlanId, exercise.id) == 0) {
            val order = planDao.nextStrengthSortOrder(dayPlanId)
            planDao.insertItems(
                listOf(
                    DayItemEntity(
                        id = 0L,
                        dayPlanId = dayPlanId,
                        itemType = "STRENGTH",
                        refId = exercise.id,
                        required = required,
                        sortOrder = order,
                        targetReps = targetReps,
                        prescriptionJson = null
                    )
                )
            )
        } else {
            planDao.updateStrengthRequired(dayPlanId, exercise.id, required)
            planDao.updateStrengthTargetReps(dayPlanId, exercise.id, targetReps)
        }
        return true
    }


    suspend fun removeStrengthFromDate(epochDay: Long, exerciseId: Long): Boolean {
        val dayPlanId = planDao.getPlanIdByDate(epochDay) ?: return false
        planDao.deleteStrengthItem(dayPlanId, exerciseId); return true
    }

    suspend fun setStrengthRequiredForDate(epochDay: Long, exerciseId: Long, required: Boolean): Boolean {
        val dayPlanId = planDao.getPlanIdByDate(epochDay) ?: return false
        planDao.updateStrengthRequired(dayPlanId, exerciseId, required); return true
    }

    suspend fun setStrengthTargetRepsForDate(epochDay: Long, exerciseId: Long, reps: Int?): Boolean {
        val dayPlanId = planDao.getPlanIdByDate(epochDay) ?: return false
        planDao.updateStrengthTargetReps(dayPlanId, exerciseId, reps); return true
    }

    suspend fun isInProgramForDate(epochDay: Long, exerciseId: Long): Boolean {
        val dayPlanId = planDao.getPlanIdByDate(epochDay) ?: return false
        return planDao.existsStrength(dayPlanId, exerciseId) > 0
    }

    suspend fun selectedTargetRepsForDate(epochDay: Long, exerciseId: Long): Int? {
        val dayPlanId = planDao.getPlanIdByDate(epochDay) ?: return null
        return planDao.getStrengthTargetReps(dayPlanId, exerciseId)
    }

    suspend fun requiredForDate(epochDay: Long, exerciseId: Long): Boolean {
        val dayPlanId = planDao.getPlanIdByDate(epochDay) ?: return false
        return planDao.getStrengthRequired(dayPlanId, exerciseId) ?: false
    }

    /* =========================================================
       SESSIONS / LOGGING — DATE-FIRST
       ========================================================= */
    suspend fun startSessionForDate(epochDay: Long): Long =
        sessionDao.insertSession(WorkoutSessionEntity(dateEpochDay = epochDay))

    @Deprecated("Use startSessionForDate(epochDay)", level = DeprecationLevel.ERROR)
    suspend fun startSession(@Suppress("UNUSED_PARAMETER") day: Int): Long =
        startSessionForDate(date)

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

    suspend fun logMetconForTimeForDate(epochDay: Long, planId: Long, timeSeconds: Int, result: MetconResult) {
        val log = MetconLog(
            dayIndex = 0,
            planId = planId,
            type = "FOR_TIME",
            durationSeconds = 0,
            timeSeconds = timeSeconds,
            rounds = null,
            extraReps = null,
            intervalsCompleted = null,
            result = result.name,
            dateEpochDay = epochDay
        )
        metconDao.insertLog(log)
    }

    suspend fun logMetconAmrapForDate(
        epochDay: Long, planId: Long, durationSeconds: Int, rounds: Int, extraReps: Int, result: MetconResult
    ) {
        val log = MetconLog(
            dayIndex = 0,
            planId = planId,
            type = "AMRAP",
            durationSeconds = durationSeconds,
            timeSeconds = null,
            rounds = rounds,
            extraReps = extraReps,
            intervalsCompleted = null,
            result = result.name,
            dateEpochDay = epochDay
        )
        metconDao.insertLog(log)
    }

    suspend fun logMetconEmomForDate(
        epochDay: Long, planId: Long, durationSeconds: Int, intervalsCompleted: Int?, result: MetconResult
    ) {
        val log = MetconLog(
            dayIndex = 0,
            planId = planId,
            type = "EMOM",
            durationSeconds = durationSeconds,
            timeSeconds = null,
            rounds = null,
            extraReps = null,
            intervalsCompleted = intervalsCompleted,
            result = result.name,
            dateEpochDay = epochDay
        )
        metconDao.insertLog(log)
    }

    @Deprecated("Use logMetcon*ForDate(...)", level = DeprecationLevel.ERROR)
    suspend fun logMetcon(day: Int, seconds: Int, result: MetconResult) { /* removed */ }

    /* =========================================================
       PR / SUGGESTIONS
       ========================================================= */
    suspend fun bestE1RM(exerciseId: Long, equipment: Equipment): Double? =
        prDao.bestEstimated1RM(exerciseId, equipment)

    suspend fun bestRMAtReps(exerciseId: Long, equipment: Equipment, reps: Int): Double? =
        prDao.bestWeightAtReps(exerciseId, equipment, reps)

    suspend fun getLastSuccessfulWeight(
        exerciseId: Long,
        equipment: Equipment,
        reps: Int?
    ): Double? {
        val recent = sessionDao.lastSets(exerciseId, equipment, 20, reps)
        return recent.firstOrNull { it.success == true && it.weight != null }?.weight
    }

    suspend fun suggestNextLoadKg(
        exerciseId: Long,
        equipment: Equipment,
        reps: Int
    ): Double? = bestE1RM(exerciseId, equipment)?.let { it * repsToPercentage(reps) }

    suspend fun evaluateAndRecordPrIfAny(
        exerciseId: Long,
        equipment: Equipment,
        reps: Int,
        weightKg: Double,
        success: Boolean
    ): PrCelebrationEvent? {
        if (!success) return null
        if (reps <= 0 || reps > 12) return null

        val prevHardKg = prDao.bestWeightAtReps(exerciseId, equipment, reps)
        val prevSoftE1rm = prDao.bestEstimated1RM(exerciseId, equipment)

        val hardCandidateKg = weightKg
        val softCandidateE1rm = estimateOneRepMax(weightKg, reps) ?: return null

        val hardStep = equipmentMinIncrementKg(equipment)
        val hardImproved = (prevHardKg == null) || ((hardCandidateKg - prevHardKg) >= hardStep)

        val softImproved = if (prevSoftE1rm == null) {
            true
        } else {
            val threshold = max(1.0, prevSoftE1rm * 0.01) // 1 kg or 1%
            (softCandidateE1rm - prevSoftE1rm) >= threshold
        }

        if (!hardImproved && !softImproved) return null

        if (softImproved) prDao.upsertEstimated1RM(exerciseId, equipment, softCandidateE1rm, date)
        if (hardImproved) prDao.upsertRepMax(exerciseId, equipment, reps, hardCandidateKg, date)

        return PrCelebrationEvent(
            exerciseId = exerciseId,
            equipment = equipment,
            isHardPr = hardImproved,
            reps = if (hardImproved) reps else null,
            newWeightKg = if (hardImproved) hardCandidateKg else null,
            prevWeightKg = if (hardImproved) prevHardKg else null,
            newE1rmKg = softCandidateE1rm,
            prevE1rmKg = prevSoftE1rm
        )
    }

    suspend fun previewPrEvent(
        exerciseId: Long,
        equipment: com.example.safitness.core.Equipment,
        reps: Int,
        weightKg: Double
    ): com.example.safitness.core.PrCelebrationEvent? {
        if (reps <= 0 || reps > 12) return null

        val prevHardKg = prDao.bestWeightAtReps(exerciseId, equipment, reps)
        val prevSoftE1rm = prDao.bestEstimated1RM(exerciseId, equipment)

        val softCandidateE1rm = estimateOneRepMax(weightKg, reps) ?: return null
        val hardCandidateKg = weightKg

        val hardStep = equipmentMinIncrementKg(equipment)
        val hardImproved = (prevHardKg == null) || ((hardCandidateKg - prevHardKg) >= hardStep)
        val softImproved = (prevSoftE1rm == null) ||
                ((softCandidateE1rm - prevSoftE1rm) >= kotlin.math.max(1.0, prevSoftE1rm * 0.01))

        if (!hardImproved && !softImproved) return null

        val celebrateHard = hardImproved
        return com.example.safitness.core.PrCelebrationEvent(
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

    private fun equipmentMinIncrementKg(equipment: Equipment): Double = when (equipment) {
        Equipment.BARBELL -> 2.5
        Equipment.DUMBBELL -> 1.0
        Equipment.KETTLEBELL -> 2.0
        else -> 1.0
    }

    /* =========================================================
       METCONS — DATE-FIRST
       ========================================================= */
    fun metconPlans(): Flow<List<MetconPlan>> = metconDao.getAllPlans()

    fun metconsForDate(epochDay: Long): Flow<List<SelectionWithPlanAndComponents>> =
        planDao.flowPlanIdByDate(epochDay).flatMapLatest { dayPlanId ->
            if (dayPlanId == null) {
                metconDao.getMetconsForDate(epochDay)
            } else {
                planDao.flowDayMetconsFor(dayPlanId).flatMapLatest { rows ->
                    if (rows.isEmpty()) metconDao.getMetconsForDate(epochDay) else {
                        val flows = rows.map { row ->
                            metconDao.getPlanWithComponents(row.planId).map { pwc ->
                                SelectionWithPlanAndComponents(
                                    selection = ProgramMetconSelection(
                                        dateEpochDay = epochDay,
                                        planId = row.planId,
                                        required = row.required,
                                        displayOrder = row.sortOrder
                                    ),
                                    planWithComponents = pwc
                                )
                            }
                        }
                        combine(flows) { it.toList().sortedBy { s -> s.selection.displayOrder } }
                    }
                }
            }
        }

    fun planWithComponents(planId: Long): Flow<PlanWithComponents> =
        metconDao.getPlanWithComponents(planId)

    fun lastMetconDisplayForDate(epochDay: Long): Flow<MetconDisplay?> =
        metconDao.lastForDate(epochDay).flatMapLatest { log ->
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
                    val s = sessionDao.lastMetconForDate(epochDay)
                    emit(
                        s?.let {
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

    fun lastMetconForPlan(planId: Long): Flow<MetconLog?> =
        metconDao.lastForPlan(planId)

    // --- METCON mutations (date-first) ---

    suspend fun addMetconToDate(
        epochDay: Long,
        planId: Long,
        required: Boolean,
        orderInDay: Int
    ): Boolean {
        val dayPlanId = planDao.getPlanIdByDate(epochDay)
        return if (dayPlanId == null) {
            // No plan row for the date yet — store in per-date selections
            metconDao.upsertSelection(
                ProgramMetconSelection(
                    dateEpochDay = epochDay,
                    planId = planId,
                    required = required,
                    displayOrder = orderInDay
                )
            )
            true
        } else {
            // Plan exists — mirror Engine/Skill behaviour using day items
            if (planDao.metconItemCount(dayPlanId, planId) == 0) {
                planDao.insertItems(
                    listOf(
                        DayItemEntity(
                            id = 0L,
                            dayPlanId = dayPlanId,
                            itemType = "METCON",
                            refId = planId,
                            required = required,
                            sortOrder = orderInDay,
                            targetReps = null,
                            prescriptionJson = null
                        )
                    )
                )
            } else {
                planDao.updateMetconRequired(dayPlanId, planId, required)
                planDao.updateMetconOrder(dayPlanId, planId, orderInDay)
            }
            true
        }
    }

    suspend fun removeMetconFromDate(epochDay: Long, planId: Long): Boolean {
        // Prefer removing from the date-first plan if one exists for this date
        val dayPlanId = planDao.getPlanIdByDate(epochDay)
        if (dayPlanId != null) {
            // Only delete if this plan is actually attached to the day
            if (planDao.metconItemCount(dayPlanId, planId) > 0) {
                planDao.deleteMetconItem(dayPlanId, planId)
                return true
            }
            // If a day plan exists but this metcon isn't there, we fall through to legacy
            // just in case an old selection was left around.
        }

        // Legacy fallback: remove from the old per-date selection table
        // (kept for backward compatibility / dates that haven’t been migrated yet)
        metconDao.removeSelectionByDate(epochDay, planId)
        return true
    }

    suspend fun setMetconRequiredForDate(
        epochDay: Long,
        planId: Long,
        required: Boolean
    ): Boolean {
        val dayPlanId = planDao.getPlanIdByDate(epochDay)
        return if (dayPlanId == null) {
            metconDao.setRequiredByDate(epochDay, planId, required); true
        } else {
            planDao.updateMetconRequired(dayPlanId, planId, required); true
        }
    }

    suspend fun setMetconOrderForDate(
        epochDay: Long,
        planId: Long,
        orderInDay: Int
    ): Boolean {
        val dayPlanId = planDao.getPlanIdByDate(epochDay)
        return if (dayPlanId == null) {
            metconDao.setDisplayOrderByDate(epochDay, planId, orderInDay); true
        } else {
            planDao.updateMetconOrder(dayPlanId, planId, orderInDay); true
        }
    }


    /* =========================================================
       ENGINE / SKILLS — DATE-FIRST
       ========================================================= */
    fun enginePlanIdsForDate(epochDay: Long): Flow<Set<Long>> =
        planDao.flowPlanIdByDate(epochDay).flatMapLatest { dayPlanId ->
            if (dayPlanId == null) flowOf(emptySet())
            else planDao.engineItemIds(dayPlanId).map { it.toSet() }
        }

    fun skillPlanIdsForDate(epochDay: Long): Flow<Set<Long>> =
        planDao.flowPlanIdByDate(epochDay).flatMapLatest { dayPlanId ->
            if (dayPlanId == null) flowOf(emptySet())
            else planDao.skillItemIds(dayPlanId).map { it.toSet() }
        }

    suspend fun addEngineToDate(epochDay: Long, planId: Long, required: Boolean, orderInDay: Int): Boolean {
        val dayPlanId = ensurePlanForDate(epochDay) ?: return false
        if (planDao.engineItemCount(dayPlanId, planId) == 0) {
            planDao.insertItems(
                listOf(
                    DayItemEntity(
                        id = 0L,
                        dayPlanId = dayPlanId,
                        itemType = "ENGINE",
                        refId = planId,
                        required = required,
                        sortOrder = orderInDay,
                        prescriptionJson = null,
                        targetReps = null
                    )
                )
            )
        }
        return true
    }


    suspend fun removeEngineFromDate(epochDay: Long, planId: Long): Boolean {
        val dayPlanId = planDao.getPlanIdByDate(epochDay) ?: return false
        planDao.deleteEngineItem(dayPlanId, planId); return true
    }

    suspend fun setEngineRequiredForDate(epochDay: Long, planId: Long, required: Boolean): Boolean {
        val dayPlanId = planDao.getPlanIdByDate(epochDay) ?: return false
        planDao.updateEngineRequired(dayPlanId, planId, required); return true
    }

    suspend fun setEngineOrderForDate(epochDay: Long, planId: Long, orderInDay: Int): Boolean {
        val dayPlanId = planDao.getPlanIdByDate(epochDay) ?: return false
        planDao.updateEngineOrder(dayPlanId, planId, orderInDay); return true
    }

    suspend fun addSkillToDate(epochDay: Long, planId: Long, required: Boolean, orderInDay: Int): Boolean {
        val dayPlanId = ensurePlanForDate(epochDay) ?: return false
        if (planDao.skillItemCount(dayPlanId, planId) == 0) {
            planDao.insertItems(
                listOf(
                    DayItemEntity(
                        id = 0L,
                        dayPlanId = dayPlanId,
                        itemType = "SKILL",
                        refId = planId,
                        required = required,
                        sortOrder = orderInDay,
                        prescriptionJson = null,
                        targetReps = null
                    )
                )
            )
        }
        return true
    }
    private suspend fun ensurePlanForDate(epochDay: Long): Long? {
        planDao.getPlanIdByDate(epochDay)?.let { return it }

        val phaseId = ensurePhase()
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
                startDateEpochDay = java.time.LocalDate.now().toEpochDay(),
                weeks = 4
            )
        )
    }


    suspend fun removeSkillFromDate(epochDay: Long, planId: Long): Boolean {
        val dayPlanId = planDao.getPlanIdByDate(epochDay) ?: return false
        planDao.deleteSkillItem(dayPlanId, planId); return true
    }

    suspend fun setSkillRequiredForDate(epochDay: Long, planId: Long, required: Boolean): Boolean {
        val dayPlanId = planDao.getPlanIdByDate(epochDay) ?: return false
        planDao.updateSkillRequired(dayPlanId, planId, required); return true
    }

    suspend fun setSkillOrderForDate(epochDay: Long, planId: Long, orderInDay: Int): Boolean {
        val dayPlanId = planDao.getPlanIdByDate(epochDay) ?: return false
        planDao.updateSkillOrder(dayPlanId, planId, orderInDay); return true
    }

    /* =========================================================
       PHASE / CALENDAR HELPERS
       ========================================================= */
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

    /* ---------- Rest timer (unchanged) ---------- */
    data class RestState(
        val sessionId: Long,
        val exerciseId: Long,
        val durationMs: Long,
        val remainingMs: Long,
        val isRunning: Boolean
    )
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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
                val next = kotlin.math.max(0L, cur.remainingMs - 1000L)
                _restState.value = cur.copy(remainingMs = next)
                if (next == 0L) {
                    pauseRestTimer()
                    return@launch
                }
            }
        }
    }
}
