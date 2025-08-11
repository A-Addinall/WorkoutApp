package com.example.safitness.ui

import androidx.lifecycle.*
import com.example.safitness.core.Equipment
import com.example.safitness.data.dao.ExerciseWithSelection
import com.example.safitness.data.repo.WorkoutRepository
import kotlinx.coroutines.launch

class WorkoutViewModel(private val repo: WorkoutRepository) : ViewModel() {

    private val dayLive = MutableLiveData<Int>()
    val programForDay: LiveData<List<ExerciseWithSelection>> =
        dayLive.switchMap { day -> repo.programForDay(day).asLiveData() }

    private val _lastMetconSeconds = MutableLiveData<Int>(0)
    val lastMetconSeconds: LiveData<Int> = _lastMetconSeconds

    fun setDay(day: Int) {
        dayLive.value = day
        viewModelScope.launch {
            _lastMetconSeconds.value = repo.lastMetconSecondsForDay(day) ?: 0
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

    fun logMetcon(day: Int, seconds: Int) = viewModelScope.launch {
        repo.logMetcon(day, seconds)
        _lastMetconSeconds.value = repo.lastMetconSecondsForDay(day) ?: 0
    }

    suspend fun getLastSuccessfulWeight(exerciseId: Long, equipment: Equipment, reps: Int?) =
        repo.getLastSuccessfulWeight(exerciseId, equipment, reps)

    suspend fun getSuggestedWeight(
            exerciseId: Long,
            equipment: Equipment,
            reps: Int?
        ) = repo.getSuggestedWeight(exerciseId, equipment, reps)
}
