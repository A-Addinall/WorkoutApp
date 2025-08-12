package com.example.safitness.data.repo

import com.example.safitness.core.Equipment
import com.example.safitness.core.MetconResult
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.dao.*
import com.example.safitness.data.entities.Exercise
import com.example.safitness.data.entities.MetconPlan
import com.example.safitness.data.entities.ProgramMetconSelection
import com.example.safitness.data.entities.ProgramSelection
import com.example.safitness.data.entities.SetLog
import com.example.safitness.data.entities.WorkoutSession
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val libraryDao: LibraryDao,
    private val programDao: ProgramDao,
    private val sessionDao: SessionDao,
    private val prDao: PersonalRecordDao,
    private val metconDao: MetconDao
) {
    /* ---------- Library ---------- */
    fun getExercises(type: WorkoutType?, eq: Equipment?) =
        libraryDao.getExercises(type, eq)

    suspend fun countExercises() = libraryDao.countExercises()

    /* ---------- Program (strength) ---------- */
    suspend fun addToDay(
        day: Int,
        exercise: Exercise,
        required: Boolean,
        preferred: Equipment?,
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

    fun programForDay(day: Int): Flow<List<ExerciseWithSelection>> =
        programDao.getProgramForDay(day)

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
        sessionId: Long,
        exerciseId: Long,
        equipment: Equipment,
        setNumber: Int,
        reps: Int,
        weight: Double,
        rpe: Double?,
        success: Boolean,
        notes: String?
    ): Long = sessionDao.insertSet(
        SetLog(
            sessionId = sessionId,
            exerciseId = exerciseId,
            equipment = equipment,
            setNumber = setNumber,
            reps = reps,
            weight = weight,
            timeSeconds = null,
            rpe = rpe,
            success = success,
            notes = notes
        )
    )

    suspend fun logTimeOnlySet(
        sessionId: Long,
        exerciseId: Long,
        equipment: Equipment,
        setNumber: Int,
        timeSeconds: Int,
        rpe: Double?,
        success: Boolean?,
        notes: String?
    ): Long = sessionDao.insertSet(
        SetLog(
            sessionId = sessionId,
            exerciseId = exerciseId,
            equipment = equipment,
            setNumber = setNumber,
            reps = 0,
            weight = null,
            timeSeconds = timeSeconds,
            rpe = rpe,
            success = success,
            notes = notes
        )
    )

    // Existing 2-arg API.
    suspend fun logMetcon(day: Int, seconds: Int) {
        val sessionId = startSession(day)
        logTimeOnlySet(
            sessionId = sessionId,
            exerciseId = 0L,
            equipment = Equipment.BARBELL, // or BODYWEIGHT
            setNumber = 1,
            timeSeconds = seconds,
            rpe = null,
            success = null,
            notes = null
        )
    }

    // Overload to accept RX/Scaled; stored once schema is extended.
    suspend fun logMetcon(day: Int, seconds: Int, resultType: MetconResult) {
        logMetcon(day, seconds)
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

    /* ---------- Metcon plans ---------- */
    fun metconPlans(): Flow<List<MetconPlan>> = metconDao.getAllPlans()

    fun metconsForDay(day: Int): Flow<List<MetconDao.SelectionWithPlanAndComponents>> =
        metconDao.getMetconsForDay(day)

    suspend fun addMetconToDay(
        day: Int,
        planId: Long,
        required: Boolean,
        displayOrder: Int
    ): Long = metconDao.upsertSelection(
        ProgramMetconSelection(
            dayIndex = day,
            planId = planId,
            required = required,
            displayOrder = displayOrder
        )
    )

    suspend fun removeMetconFromDay(day: Int, planId: Long) =
        metconDao.removeSelection(day, planId)

    suspend fun setMetconRequired(selectionId: Long, required: Boolean) =
        metconDao.setRequired(selectionId, required)

    suspend fun setMetconOrder(selectionId: Long, order: Int) =
        metconDao.setDisplayOrder(selectionId, order)
}
