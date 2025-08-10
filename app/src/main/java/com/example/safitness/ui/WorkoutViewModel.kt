// app/src/main/java/com/example/safitness/ui/WorkoutViewModel.kt
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

    fun setDay(day: Int) { dayLive.value = day }

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

    suspend fun getLastSuccessfulWeight(exerciseId: Long) =
        repo.getLastSuccessfulWeight(exerciseId)

    suspend fun getSuggestedWeight(exerciseId: Long) =
        repo.getSuggestedWeight(exerciseId)
}
