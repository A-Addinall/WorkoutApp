// app/src/main/java/com/example/safitness/ui/LibraryViewModel.kt
package com.example.safitness.ui

import androidx.lifecycle.*
import com.example.safitness.core.Equipment
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.entities.Exercise
import com.example.safitness.data.repo.WorkoutRepository
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class LibraryViewModel(private val repo: WorkoutRepository) : ViewModel() {

    private val typeFilter = MutableLiveData<WorkoutType?>(null)
    private val eqFilter = MutableLiveData<Equipment?>(null)

    // Flow -> LiveData so Activities can .observe(this)
    val exercises: LiveData<List<Exercise>> =
        MediatorLiveData<List<Exercise>>().apply {
            fun reload() {
                val type = typeFilter.value
                val eq = eqFilter.value
                // Create a new LiveData source each time filters change
                repo.getExercises(type, eq)
                    .map { it.sortedBy { e -> e.name } }
                    .asLiveData()
                    .also { src ->
                        addSource(src) { value = it }
                    }
            }
            addSource(typeFilter) { reload() }
            addSource(eqFilter) { reload() }
            reload()
        }

    fun setTypeFilter(type: WorkoutType?) { typeFilter.value = type }
    fun setEqFilter(eq: Equipment?) { eqFilter.value = eq }
}
