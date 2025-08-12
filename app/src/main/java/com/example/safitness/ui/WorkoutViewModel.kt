package com.example.safitness.ui

import androidx.lifecycle.*
import com.example.safitness.core.Equipment
import com.example.safitness.core.MetconResult
import com.example.safitness.data.dao.ExerciseWithSelection
import com.example.safitness.data.dao.MetconSummary
import com.example.safitness.data.repo.WorkoutRepository
import kotlinx.coroutines.launch

class WorkoutViewModel(private val repo: WorkoutRepository) : ViewModel() {

    private val dayLive = MutableLiveData<Int>()
    val programForDay: LiveData<List<ExerciseWithSelection>> =
        dayLive.switchMap { day -> repo.programForDay(day).asLiveData() }

    private val _lastMetcon = MutableLiveData<MetconSummary?>(null)
    val lastMetcon: LiveData<MetconSummary?> = _lastMetcon

    fun setDay(day: Int) {
        dayLive.value = day
        viewModelScope.launch {
            _lastMetcon.value = repo.lastMetconForDay(day)
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

    fun logMetcon(day: Int, seconds: Int, result: MetconResult?) = viewModelScope.launch {
        repo.logMetcon(day, seconds, result)
        _lastMetcon.value = repo.lastMetconForDay(day)
    }

    suspend fun getLastSuccessfulWeight(exerciseId: Long, equipment: Equipment, reps: Int?) =
        repo.getLastSuccessfulWeight(exerciseId, equipment, reps)

    suspend fun getSuggestedWeight(
        exerciseId: Long,
        equipment: Equipment,
        reps: Int?
    ) = repo.getSuggestedWeight(exerciseId, equipment, reps)
}
