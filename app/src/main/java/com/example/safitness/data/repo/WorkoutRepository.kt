package com.example.safitness.data.repo

import com.example.safitness.core.Equipment
import com.example.safitness.core.MetconResult
import com.example.safitness.data.dao.*
import com.example.safitness.data.entities.*
import kotlinx.coroutines.flow.*

class WorkoutRepository(
    private val libraryDao: LibraryDao,
    private val programDao: ProgramDao,
    private val sessionDao: SessionDao,
    private val prDao: PersonalRecordDao,
    private val metconDao: MetconDao,
    private val planDao: PlanDao
) {
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
                    if (rows.isEmpty()) {
                        programDao.getProgramForDay(day)
                    } else {
                        flowOf(
                            rows.map { r ->
                                ExerciseWithSelection(
                                    exercise = r.exercise,
                                    required = r.required,
                                    preferredEquipment = null,
                                    targetReps = r.targetReps
                                )
                            }
                        )
                    }
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

    suspend fun lastMetconSecondsForDay(day: Int): Int =
        sessionDao.lastMetconSecondsForDay(day) ?: 0

    suspend fun lastMetconForDay(day: Int): MetconSummary? =
        sessionDao.lastMetconForDay(day)

    /* ---------- PR / suggestions ---------- */
    suspend fun bestPR(exerciseId: Long) = prDao.bestForExercise(exerciseId)

    suspend fun getLastSuccessfulWeight(
        exerciseId: Long,
        equipment: Equipment,
        reps: Int?
    ): Double? {
        val recent = sessionDao.lastSets(
            exerciseId = exerciseId,
            equipment = equipment,
            limit = 20,
            reps = reps
        )
        return recent.firstOrNull { it.success == true && it.weight != null }?.weight
    }

    suspend fun getSuggestedWeight(
        exerciseId: Long,
        equipment: Equipment,
        reps: Int?
    ): Double? = getLastSuccessfulWeight(exerciseId, equipment, reps)?.let { it * 1.02 }

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
                        combine(flows) { it.toList().sortedBy { s -> s.selection.displayOrder } }
                    }
                }
            }
        }

    fun planWithComponents(planId: Long): Flow<PlanWithComponents> =
        metconDao.getPlanWithComponents(planId)

    suspend fun addMetconToDay(day: Int, planId: Long, required: Boolean, orderInDay: Int): Long =
        metconDao.upsertSelection(
            ProgramMetconSelection(dayIndex = day, planId = planId, required = required, displayOrder = orderInDay)
        )

    suspend fun removeMetconFromDay(day: Int, planId: Long) = metconDao.removeSelection(day, planId)
    suspend fun setMetconRequired(day: Int, planId: Long, required: Boolean) = metconDao.setRequired(day, planId, required)
    suspend fun setMetconOrder(day: Int, planId: Long, orderInDay: Int) = metconDao.setDisplayOrder(day, planId, orderInDay)

    fun lastMetconForPlan(planId: Long): Flow<MetconLog?> = metconDao.lastForPlan(planId)

    suspend fun logMetconForTime(day: Int, planId: Long, timeSeconds: Int, result: MetconResult) {
        metconDao.insertLog(
            MetconLog(
                dayIndex = day, planId = planId, type = "FOR_TIME",
                durationSeconds = 0, timeSeconds = timeSeconds, rounds = null, extraReps = null,
                intervalsCompleted = null, result = result.name
            )
        )
    }

    suspend fun logMetconAmrap(day: Int, planId: Long, durationSeconds: Int, rounds: Int, extraReps: Int, result: MetconResult) {
        metconDao.insertLog(
            MetconLog(
                dayIndex = day, planId = planId, type = "AMRAP",
                durationSeconds = durationSeconds, timeSeconds = null, rounds = rounds,
                extraReps = extraReps, intervalsCompleted = null, result = result.name
            )
        )
    }

    suspend fun logMetconEmom(day: Int, planId: Long, durationSeconds: Int, intervalsCompleted: Int?, result: MetconResult) {
        metconDao.insertLog(
            MetconLog(
                dayIndex = day, planId = planId, type = "EMOM",
                durationSeconds = durationSeconds, timeSeconds = null, rounds = null,
                extraReps = null, intervalsCompleted = intervalsCompleted, result = result.name
            )
        )
    }

    /* ---------- Phase 0 helpers ---------- */

    suspend fun getPlanFor(phaseId: Long, week: Int, day: Int): WeekDayPlanEntity? =
        planDao.getPlan(phaseId, week, day)

    suspend fun getNextPlannedDay(afterDayPlanId: Long): WeekDayPlanEntity? {
        val current = planDao.getPlanById(afterDayPlanId) ?: return null
        return planDao.getNextAfter(current.phaseId, current.weekIndex, current.dayIndex)
    }

    suspend fun attachCalendar(phaseId: Long, startEpochDay: Long) {
        var cursor = startEpochDay
        planDao.getPlansForPhaseOrdered(phaseId).forEach { plan ->
            planDao.updatePlanDate(plan.id, cursor); cursor += 1
        }
    }

    private fun resolveDayPlanIdOrNull(week: Int, day: Int): Flow<Long?> = flowOf(Unit).map {
        val phaseId = planDao.currentPhaseId() ?: return@map null
        planDao.getPlanId(phaseId, week, day)
    }
}
