// app/src/main/java/com/example/safitness/data/repo/WorkoutRepository.kt
package com.example.safitness.data.repo

import com.example.safitness.core.Equipment
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.dao.*
import com.example.safitness.data.entities.Exercise
import com.example.safitness.data.entities.ProgramSelection
import com.example.safitness.data.entities.SetLog
import com.example.safitness.data.entities.WorkoutSession
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

    suspend fun setPreferredEquipment(day: Int, exerciseId: Long, preferred: Equipment?) =
        programDao.setPreferred(day, exerciseId, preferred)

    suspend fun setTargetReps(day: Int, exerciseId: Long, reps: Int?) =
        programDao.setTargetReps(day, exerciseId, reps)

    suspend fun removeFromDay(day: Int, exerciseId: Long) =
        programDao.remove(day, exerciseId)

    /* ---------- Sessions & Sets ---------- */
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

    suspend fun lastSets(exerciseId: Long, equipment: Equipment, limit: Int = 10) =
        sessionDao.lastSets(exerciseId, equipment, limit)

    /* ---------- PR / Suggestions (basic) ---------- */
    suspend fun bestPR(exerciseId: Long) = prDao.bestForExercise(exerciseId)

    suspend fun getLastSuccessfulWeight(exerciseId: Long): Double? {
        val recent = sessionDao.lastSets(exerciseId, Equipment.BARBELL, 20)
        return recent.firstOrNull { it.success == true && it.weight != null }?.weight
    }

    suspend fun getSuggestedWeight(exerciseId: Long): Double? =
        getLastSuccessfulWeight(exerciseId)?.let { it * 1.02 }
}
