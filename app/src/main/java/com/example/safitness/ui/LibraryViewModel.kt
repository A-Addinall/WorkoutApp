// app/src/main/java/com/example/safitness/ui/LibraryViewModel.kt
package com.example.safitness.ui

import androidx.lifecycle.*
import com.example.safitness.core.Equipment
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.entities.Exercise
import com.example.safitness.data.entities.MetconPlan
import com.example.safitness.data.repo.WorkoutRepository
import com.example.safitness.core.EngineIntent
import com.example.safitness.core.EngineMode
import com.example.safitness.core.SkillTestType
import com.example.safitness.core.SkillType
import com.example.safitness.ui.library.EngineUiItem
import com.example.safitness.ui.library.SkillUiItem
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

    // Actions for plans (wrapped in viewModelScope; no removal of existing APIs)
    fun addMetconToDay(day: Int, planId: Long, required: Boolean, order: Int) =
        viewModelScope.launch { repo.addMetconToDay(day, planId, required, order) }

    fun removeMetconFromDay(day: Int, planId: Long) =
        viewModelScope.launch { repo.removeMetconFromDay(day, planId) }

    fun setMetconRequired(day: Int, planId: Long, required: Boolean) =
        viewModelScope.launch { repo.setMetconRequired(day, planId, required) }

    fun setMetconOrder(day: Int, planId: Long, order: Int) =
        viewModelScope.launch { repo.setMetconOrder(day, planId, order) }
    private val engineSkillDay = MutableLiveData<Int>(1)
    fun setEngineSkillDay(day: Int) { engineSkillDay.value = day }
    fun getEngineSkillDay(): LiveData<Int> = engineSkillDay

    // UI lists for Engine and Skills (static from enums)
    val engineItems: LiveData<List<EngineUiItem>> = MutableLiveData<List<EngineUiItem>>().apply {
        val list = mutableListOf<EngineUiItem>()
        for (m in EngineMode.values()) {
            for (i in EngineIntent.values()) {
                val title = "${m.name} — ${i.name}"
                val subtitle = when (i) {
                    EngineIntent.FOR_TIME -> "Target distance; log a time"
                    EngineIntent.FOR_DISTANCE -> "Target duration; log meters"
                    EngineIntent.FOR_CALORIES -> "Target duration; log calories"
                }
                list += EngineUiItem(m.name, i.name, title, subtitle)
            }
        }
        value = list
    }

    val skillItems: LiveData<List<SkillUiItem>> = MutableLiveData<List<SkillUiItem>>().apply {
        val defaults = mapOf(
            SkillType.DOUBLE_UNDERS.name to SkillTestType.MAX_REPS_UNBROKEN.name,
            SkillType.HANDSTAND_HOLD.name to SkillTestType.MAX_HOLD_SECONDS.name,
            SkillType.MUSCLE_UP.name to SkillTestType.ATTEMPTS.name
        )
        val list = mutableListOf<SkillUiItem>()
        for ((skill, test) in defaults) {
            val title = "$skill — $test"
            val subtitle = "Tap to add/remove for the day"
            list += SkillUiItem(skill, test, title, subtitle)
        }
        value = list
    }
}
