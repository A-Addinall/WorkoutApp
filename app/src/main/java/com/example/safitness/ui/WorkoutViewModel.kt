package com.example.safitness.ui

import androidx.lifecycle.*
import com.example.safitness.core.Equipment
import com.example.safitness.core.MetconResult
import com.example.safitness.data.dao.ExerciseWithSelection
import com.example.safitness.data.dao.MetconSummary
import com.example.safitness.data.repo.WorkoutRepository
import kotlinx.coroutines.launch
import com.example.safitness.data.dao.SelectionWithPlanAndComponents
import androidx.lifecycle.asLiveData
import com.example.safitness.data.dao.PlanWithComponents
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.combine

class WorkoutViewModel(private val repo: WorkoutRepository) : ViewModel() {

    private val dayLive = MutableLiveData<Int>()

    val programForDay: LiveData<List<ExerciseWithSelection>> =
        dayLive.switchMap { day -> repo.programForDay(day).asLiveData() }

    private val _lastMetcon = MutableLiveData<MetconSummary?>()
    val lastMetcon: LiveData<MetconSummary?> = _lastMetcon

    val metconsForDay: LiveData<List<SelectionWithPlanAndComponents>> =
        dayLive.switchMap { day -> repo.metconsForDay(day).asLiveData() }

    fun planWithComponents(planId: Long): LiveData<PlanWithComponents> =
        repo.planWithComponents(planId).asLiveData()

    // ADD: seconds-only summary for last metcon (used by WorkoutActivity plan card)
    private val _lastMetconSeconds = MutableLiveData(0)
    val lastMetconSeconds: LiveData<Int> = _lastMetconSeconds
    fun setDay(day: Int) {
        dayLive.value = day
        viewModelScope.launch {
            _lastMetcon.value = repo.lastMetconForDay(day)
            _lastMetconSeconds.value = repo.lastMetconSecondsForDay(day) // ADD
        }
    }

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
    }

    fun logMetcon(day: Int, seconds: Int, result: MetconResult) = viewModelScope.launch {
        repo.logMetcon(day, seconds, result)
        _lastMetcon.value = repo.lastMetconForDay(day)
    }

    // Legacy helpers used by ExerciseDetailActivity
    suspend fun getLastSuccessfulWeight(exerciseId: Long, equipment: Equipment, reps: Int?) =
        repo.getLastSuccessfulWeight(exerciseId, equipment, reps)

    suspend fun getSuggestedWeight(
        exerciseId: Long,
        equipment: Equipment,
        reps: Int?
    ) = repo.getSuggestedWeight(exerciseId, equipment, reps)


    data class DaySummary(val subtitle: String)

    fun daySummary(day: Int): LiveData<DaySummary> {
        // programForDay → strength items (metcon is stored separately)
        val strengthFlow = repo.programForDay(day) // Flow<List<ExerciseWithSelection>>
        val metconFlow = repo.metconsForDay(day)   // Flow<List<SelectionWithPlanAndComponents>>

        return combine(strengthFlow, metconFlow) { program, metcons ->
            val hasMetcon = metcons.isNotEmpty()

            // Only consider non-metcon exercises for the strength category summary
            val strengthCats = program
                .filter { it.exercise.modality != com.example.safitness.core.Modality.METCON }
                .filter { it.required } // if you prefer to reflect only the required focus
                .map { it.exercise.workoutType } // e.g. PUSH, PULL, LEGS_CORE
                .map(::mapWorkoutTypeToLabel)
                .distinct()

            val subtitle = when {
                strengthCats.isEmpty() && hasMetcon -> "Metcon"
                strengthCats.isEmpty() && !hasMetcon -> "Empty"
                strengthCats.size == 1 && !hasMetcon -> strengthCats.first()
                strengthCats.size == 1 && hasMetcon -> "${strengthCats.first()} + Metcon"
                else -> {
                    // Mixed strength day; show the top 2 to keep it readable
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
        // Fall back for any future/other types:
        else -> t.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }


}
