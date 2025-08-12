package com.example.safitness.data.repo

import com.example.safitness.core.Equipment
import com.example.safitness.core.MetconResult
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.dao.*
import com.example.safitness.data.entities.*
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val libraryDao: LibraryDao,
    private val programDao: ProgramDao,
    private val sessionDao: SessionDao,
    private val prDao: PersonalRecordDao
) {
    /* ---------- Library ---------- */
    fun getExercises(type: WorkoutType?, eq: Equipment?) =
        libraryDao.getExercises(type, eq)

    suspend fun countExercises() = libraryDao.countExercises()

    /* ---------- Program ---------- */
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
        notes: String?,
        metconResult: MetconResult?
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
            notes = notes,
            metconResult = metconResult
        )
    )

    /** New: log metcon with optional RX/SCALED tag */
    suspend fun logMetcon(day: Int, seconds: Int, result: MetconResult?) {
        val sessionId = startSession(day)
        val equip = Equipment.BODYWEIGHT // more neutral than BARBELL for metcon rows
        logTimeOnlySet(
            sessionId = sessionId,
            exerciseId = 0L,
            equipment = equip,
            setNumber = 1,
            timeSeconds = seconds,
            rpe = null,
            success = null,
            notes = null,
            metconResult = result
        )
    }

    // Backward compatibility for existing call sites
    suspend fun logMetcon(day: Int, seconds: Int) = logMetcon(day, seconds, null)

    /** Returns last metcon time only (legacy helper). */
    suspend fun lastMetconSecondsForDay(day: Int): Int =
        sessionDao.lastMetconForDay(day)?.timeSeconds ?: 0

    /** New: Returns time + result tag, or null if none logged yet. */
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
}
