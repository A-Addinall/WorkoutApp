package com.example.safitness.ui

import androidx.lifecycle.*
import com.example.safitness.core.Equipment
import com.example.safitness.core.MetconResult
import com.example.safitness.core.Modality
import com.example.safitness.core.PrCelebrationEvent
import com.example.safitness.data.dao.ExerciseWithSelection
import com.example.safitness.data.dao.PlanWithComponents
import com.example.safitness.data.dao.SelectionWithPlanAndComponents
import com.example.safitness.data.entities.MetconLog
import com.example.safitness.data.repo.WorkoutRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

class WorkoutViewModel(private val repo: WorkoutRepository) : ViewModel() {

    /** Selected calendar day (epochDay, UTC). Prefer this over any day-index. */
    private val dateLive = MutableLiveData<Long>(LocalDate.now().toEpochDay())
    fun setDate(epochDay: Long) { dateLive.value = epochDay }

    @Deprecated("Use setDate(epochDay) everywhere")
    fun setDay(@Suppress("UNUSED_PARAMETER") day: Int) {
        // Temporary shim to avoid caller crashes while migrating.
        dateLive.value = LocalDate.now().toEpochDay()
    }

    /* ---------------- Strength & Metcons (date-first) ---------------- */

    val programForDate: LiveData<List<ExerciseWithSelection>> =
        dateLive.switchMap { epoch -> repo.programForDate(epoch).asLiveData() }

    val metconsForDate: LiveData<List<SelectionWithPlanAndComponents>> =
        dateLive.switchMap { epoch -> repo.metconsForDate(epoch).asLiveData() }

    fun planWithComponents(planId: Long): LiveData<PlanWithComponents> =
        repo.planWithComponents(planId).asLiveData()

    val lastMetconDisplay: LiveData<WorkoutRepository.MetconDisplay?> =
        dateLive.switchMap { epoch -> repo.lastMetconDisplayForDate(epoch).asLiveData() }

    /* ---------------- PR / strength helpers ---------------- */

    private val _prEvents =
        kotlinx.coroutines.flow.MutableSharedFlow<PrCelebrationEvent>(replay = 0, extraBufferCapacity = 1)
    val prEvents: kotlinx.coroutines.flow.SharedFlow<PrCelebrationEvent> = _prEvents

    fun logStrengthSet(
        sessionId: Long,
        exerciseId: Long,
        equipment: Equipment,
        setNumber: Int,
        reps: Int,
        weight: Double,
        rpe: Double?,
        success: Boolean,
        notes: String?
    ) = viewModelScope.launch {
        repo.logStrengthSet(
            sessionId = sessionId,
            exerciseId = exerciseId,
            equipment = equipment,
            setNumber = setNumber,
            reps = reps,
            weight = weight,
            rpe = rpe,
            success = success,
            notes = notes
        )
        val pr = try {
            repo.evaluateAndRecordPrIfAny(
                exerciseId = exerciseId,
                equipment = equipment,
                reps = reps,
                weightKg = weight,
                success = success
            )
        } catch (_: Throwable) { null }
        if (pr != null) _prEvents.emit(pr)
    }

    // Needed by ExerciseDetailActivity
    suspend fun previewPrEvent(
        exerciseId: Long,
        equipment: Equipment,
        reps: Int,
        weightKg: Double
    ): PrCelebrationEvent? = repo.previewPrEvent(exerciseId, equipment, reps, weightKg)

    suspend fun bestE1RM(exerciseId: Long, equipment: Equipment): Double? =
        repo.bestE1RM(exerciseId, equipment)

    suspend fun getLastSuccessfulWeight(
        exerciseId: Long,
        equipment: Equipment,
        reps: Int?
    ) = repo.getLastSuccessfulWeight(exerciseId, equipment, reps)

    suspend fun suggestNextLoadKg(
        exerciseId: Long,
        equipment: Equipment,
        reps: Int
    ): Double? = repo.suggestNextLoadKg(exerciseId, equipment, reps)

    /* ---------------- Metcon logging (plan-scoped) ---------------- */

    fun lastMetconForPlan(planId: Long): LiveData<MetconLog?> =
        repo.lastMetconForPlan(planId).asLiveData()

    fun logMetconForTime(day: Int, planId: Long, timeSeconds: Int, result: MetconResult) =
        viewModelScope.launch { repo.logMetconForTime(day, planId, timeSeconds, result) }

    fun logMetconAmrap(day: Int, planId: Long, durationSeconds: Int, rounds: Int, extraReps: Int, result: MetconResult) =
        viewModelScope.launch { repo.logMetconAmrap(day, planId, durationSeconds, rounds, extraReps, result) }

    fun logMetconEmom(day: Int, planId: Long, durationSeconds: Int, intervalsCompleted: Int?, result: MetconResult) =
        viewModelScope.launch { repo.logMetconEmom(day, planId, durationSeconds, intervalsCompleted, result) }

    /* ---------------- Day subtitle (date-first) ---------------- */

    data class DaySummary(val subtitle: String)

    fun daySummaryForDate(epochDay: Long): LiveData<DaySummary> {
        val strengthFlow = repo.programForDate(epochDay)
        val metconFlow = repo.metconsForDate(epochDay)

        return combine(strengthFlow, metconFlow) { program, metcons ->
            val hasMetcon = metcons.isNotEmpty()

            val strengthCats: List<String> = program
                .asSequence()
                .filter { it.exercise.modality != com.example.safitness.core.Modality.METCON }
                .filter { it.required }
                .map { it.exercise.workoutType }
                .map(::mapWorkoutTypeToLabel)
                .distinct()
                .toList()

            val subtitle = when {
                strengthCats.isEmpty() && hasMetcon -> "Metcon"
                strengthCats.isEmpty() && !hasMetcon -> "Empty"
                strengthCats.size == 1 && !hasMetcon -> strengthCats.first()
                strengthCats.size == 1 && hasMetcon -> "${strengthCats.first()} + Metcon"
                else -> {
                    val mixed = strengthCats.take(2).joinToString(" · ")
                    if (hasMetcon) "Mixed ($mixed) + Metcon" else "Mixed ($mixed)"
                }
            }

            DaySummary(subtitle = subtitle)
        }.asLiveData()
    }

    private fun mapWorkoutTypeToLabel(t: com.example.safitness.core.WorkoutType): String = when (t) {
        com.example.safitness.core.WorkoutType.PUSH -> "Push"
        com.example.safitness.core.WorkoutType.PULL -> "Pull"
        com.example.safitness.core.WorkoutType.LEGS_CORE -> "Legs & Core"
        else -> t.name.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
    }
}
