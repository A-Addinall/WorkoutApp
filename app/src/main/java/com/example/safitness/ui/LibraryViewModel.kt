// app/src/main/java/com/example/safitness/ui/LibraryViewModel.kt
package com.example.safitness.ui

import androidx.lifecycle.*
import com.example.safitness.core.Equipment
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.entities.Exercise
import com.example.safitness.data.entities.MetconPlan
import com.example.safitness.data.repo.WorkoutRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LibraryViewModel(private val repo: WorkoutRepository) : ViewModel() {

    private val typeFilter = MutableLiveData<WorkoutType?>(null)
    private val eqFilter = MutableLiveData<Equipment?>(null)

    // EXISTING: Exercises list
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

    // NEW: Metcon plans (library)
    val metconPlans: LiveData<List<MetconPlan>> =
        repo.metconPlans().map { it.sortedBy { p -> p.title } }.asLiveData()

    // Track which day the user is targeting for Metcons in this screen
    private val metconDay = MutableLiveData<Int>(1)
    fun setMetconDay(day: Int) { metconDay.value = day }

    // Whether each plan is already selected for the chosen day
    val metconPlanIdsForDay: LiveData<Set<Long>> =
        metconDay.switchMap { day ->
            repo.metconsForDay(day)
                .map { list -> list.map { it.selection.planId }.toSet() }
                .asLiveData()
        }
    fun addMetconToDay(day: Int, planId: Long, required: Boolean, order: Int) =
        viewModelScope.launch { repo.addMetconToDay(day, planId, required, order) }

    fun removeMetconFromDay(day: Int, planId: Long) =
        viewModelScope.launch { repo.removeMetconFromDay(day, planId) }

    fun setMetconRequired(day: Int, planId: Long, required: Boolean) =
        viewModelScope.launch { repo.setMetconRequired(day, planId, required) }

    fun setMetconOrder(day: Int, planId: Long, order: Int) =
        viewModelScope.launch { repo.setMetconOrder(day, planId, order) }

    val enginePlanIdsForDay: LiveData<Set<Long>> =
        metconDay.switchMap { day ->
            repo.metconsForDay(day)
                .map { list -> list.map { it.selection.planId }.toSet() }
                .asLiveData()
        }
}
