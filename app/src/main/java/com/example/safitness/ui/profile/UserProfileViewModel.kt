package com.example.safitness.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safitness.data.dao.UserProfileDao
import com.example.safitness.data.entities.UserProfile
import com.example.safitness.data.entities.toEquipmentSet
import com.example.safitness.data.entities.toCsv
import com.example.safitness.core.Equipment
import com.example.safitness.ml.Goal
import com.example.safitness.ml.ExperienceLevel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UserProfileUi(
    val goal: Goal = Goal.STRENGTH,
    val experience: ExperienceLevel = ExperienceLevel.BEGINNER,
    val daysPerWeek: Int = 3,
    val sessionMinutes: Int = 45,
    val equipment: Set<Equipment> = setOf(Equipment.BODYWEIGHT)
)

class UserProfileViewModel(private val dao: UserProfileDao) : ViewModel() {
    val ui: StateFlow<UserProfileUi> = dao.flowProfile()
        .map { p ->
            if (p == null) UserProfileUi()
            else UserProfileUi(
                goal = p.goal,
                experience = p.experience,
                daysPerWeek = p.daysPerWeek,
                sessionMinutes = p.sessionMinutes,
                equipment = p.equipmentCsv.toEquipmentSet()
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfileUi())

    fun save(u: UserProfileUi) = viewModelScope.launch {
        dao.upsert(
            UserProfile(
                id = 1L,
                goal = u.goal,
                experience = u.experience,
                daysPerWeek = u.daysPerWeek,
                sessionMinutes = u.sessionMinutes,
                equipmentCsv = u.equipment.toCsv()
            )
        )
    }
}
