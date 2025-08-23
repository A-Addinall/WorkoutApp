package com.example.safitness.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safitness.data.dao.UserProfileDao
import com.example.safitness.data.entities.UserProfile
import com.example.safitness.core.Equipment
import com.example.safitness.ml.Goal
import com.example.safitness.ml.ExperienceLevel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UserProfileUi(
    val goal: Goal = Goal.STRENGTH,
    val experience: ExperienceLevel = ExperienceLevel.BEGINNER,
    val sessionMinutes: Int = 45,
    val programWeeks: Int = 4,
    val equipmentCsv: String = "BODYWEIGHT",
    val workoutDaysCsv: String? = null,
    val includeEngine: Boolean = false,
    val engineModesCsv: String? = null,
    val preferredSkillsCsv: String? = null
)

class UserProfileViewModel(private val dao: UserProfileDao) : ViewModel() {
    val ui: StateFlow<UserProfileUi> = dao.flowProfile()
        .map { p ->
            if (p == null) UserProfileUi()
            else UserProfileUi(
                goal = p.goal,
                experience = p.experience,
                sessionMinutes = p.sessionMinutes,
                programWeeks = p.programWeeks,
                equipmentCsv = p.equipmentCsv,
                workoutDaysCsv = p.workoutDaysCsv,
                includeEngine = p.includeEngine,
                engineModesCsv = p.engineModesCsv,
                preferredSkillsCsv = p.preferredSkillsCsv
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfileUi())

    fun save(u: UserProfileUi) = viewModelScope.launch {
        val daysPerWeek = u.workoutDaysCsv?.split(',')?.size ?: 3
        dao.upsert(
            UserProfile(
                id = 1L,
                goal = u.goal,
                experience = u.experience,
                daysPerWeek = daysPerWeek,
                sessionMinutes = u.sessionMinutes,
                equipmentCsv = u.equipmentCsv,
                programWeeks = u.programWeeks,
                workoutDaysCsv = u.workoutDaysCsv,
                includeEngine = u.includeEngine,
                engineModesCsv = u.engineModesCsv,
                preferredSkillsCsv = u.preferredSkillsCsv
            )
        )
    }
}
