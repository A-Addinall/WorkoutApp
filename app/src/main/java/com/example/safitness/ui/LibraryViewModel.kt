package com.example.safitness.ui

import androidx.lifecycle.*
import com.example.safitness.core.Equipment
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.entities.Exercise
import com.example.safitness.data.entities.MetconPlan
import com.example.safitness.data.repo.WorkoutRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

class LibraryViewModel(private val repo: WorkoutRepository) : ViewModel() {

    // Filters for exercise browsing
    private val typeFilter = MutableLiveData<WorkoutType?>(null)
    private val eqFilter = MutableLiveData<Equipment?>(null)

    // Selection context (date-first)
    private val targetEpochDay = MutableLiveData(LocalDate.now().toEpochDay())
    fun setTargetDate(epochDay: Long) { targetEpochDay.value = epochDay }

    // -------- Exercise library (browsing) --------
    val exercises: LiveData<List<Exercise>> =
        MediatorLiveData<List<Exercise>>().apply {
            fun reload() {
                val type = typeFilter.value
                val eq = eqFilter.value
                repo.getExercises(type, eq)
                    .map { it.sortedBy { e -> e.name } }
                    .asLiveData()
                    .also { src -> addSource(src) { value = it } }
            }
            addSource(typeFilter) { reload() }
            addSource(eqFilter) { reload() }
            reload()
        }

    fun setTypeFilter(type: WorkoutType?) { typeFilter.value = type }
    fun setEqFilter(eq: Equipment?) { eqFilter.value = eq }

    // -------- Strength (date-first) --------
    val strengthExerciseIdsForDate: LiveData<Set<Long>> =
        targetEpochDay.switchMap { epoch ->
            repo.programForDate(epoch)
                .map { list -> list.map { it.exercise.id }.toSet() }
                .asLiveData()
        }

    fun addStrengthToDate(epochDay: Long, exercise: Exercise, required: Boolean, preferred: Equipment?, targetReps: Int?) =
        viewModelScope.launch { repo.addStrengthToDate(epochDay, exercise, required, preferred, targetReps) }

    fun removeStrengthFromDate(epochDay: Long, exerciseId: Long) =
        viewModelScope.launch { repo.removeStrengthFromDate(epochDay, exerciseId) }

    fun setStrengthRequiredForDate(epochDay: Long, exerciseId: Long, required: Boolean) =
        viewModelScope.launch { repo.setStrengthRequiredForDate(epochDay, exerciseId, required) }

    fun setStrengthTargetRepsForDate(epochDay: Long, exerciseId: Long, reps: Int?) =
        viewModelScope.launch { repo.setStrengthTargetRepsForDate(epochDay, exerciseId, reps) }

    // -------- Metcons (date-first) --------
    val metconPlans: LiveData<List<MetconPlan>> =
        repo.metconPlans().map { it.sortedBy { p -> p.title } }.asLiveData()

    val metconPlanIdsForDate: LiveData<Set<Long>> =
        targetEpochDay.switchMap { epoch ->
            repo.metconsForDate(epoch)
                .map { list -> list.map { it.selection.planId }.toSet() }
                .asLiveData()
        }

    fun addMetconToDate(epochDay: Long, planId: Long, required: Boolean, order: Int) =
        viewModelScope.launch { repo.addMetconToDate(epochDay, planId, required, order) }

    fun removeMetconFromDate(epochDay: Long, planId: Long) =
        viewModelScope.launch { repo.removeMetconFromDate(epochDay, planId) }

    fun setMetconRequiredForDate(epochDay: Long, planId: Long, required: Boolean) =
        viewModelScope.launch { repo.setMetconRequiredForDate(epochDay, planId, required) }

    fun setMetconOrderForDate(epochDay: Long, planId: Long, order: Int) =
        viewModelScope.launch { repo.setMetconOrderForDate(epochDay, planId, order) }

    // -------- Engine (date-first) --------
    val enginePlanIdsForDate: LiveData<Set<Long>> =
        targetEpochDay.switchMap { epoch -> repo.enginePlanIdsForDate(epoch).asLiveData() }

    fun addEngineToDate(epochDay: Long, planId: Long, required: Boolean, order: Int) =
        viewModelScope.launch { repo.addEngineToDate(epochDay, planId, required, order) }

    fun removeEngineFromDate(epochDay: Long, planId: Long) =
        viewModelScope.launch { repo.removeEngineFromDate(epochDay, planId) }

    fun setEngineRequiredForDate(epochDay: Long, planId: Long, required: Boolean) =
        viewModelScope.launch { repo.setEngineRequiredForDate(epochDay, planId, required) }

    fun setEngineOrderForDate(epochDay: Long, planId: Long, order: Int) =
        viewModelScope.launch { repo.setEngineOrderForDate(epochDay, planId, order) }

    // -------- Skills (date-first) --------
    val skillPlanIdsForDate: LiveData<Set<Long>> =
        targetEpochDay.switchMap { epoch -> repo.skillPlanIdsForDate(epoch).asLiveData() }

    fun addSkillToDate(epochDay: Long, planId: Long, required: Boolean, order: Int) =
        viewModelScope.launch { repo.addSkillToDate(epochDay, planId, required, order) }

    fun removeSkillFromDate(epochDay: Long, planId: Long) =
        viewModelScope.launch { repo.removeSkillFromDate(epochDay, planId) }

    fun setSkillRequiredForDate(epochDay: Long, planId: Long, required: Boolean) =
        viewModelScope.launch { repo.setSkillRequiredForDate(epochDay, planId, required) }

    fun setSkillOrderForDate(epochDay: Long, planId: Long, order: Int) =
        viewModelScope.launch { repo.setSkillOrderForDate(epochDay, planId, order) }
}
