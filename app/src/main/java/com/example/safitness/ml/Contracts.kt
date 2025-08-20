package com.example.safitness.ml

import com.example.safitness.core.*

data class UserContext(
    val goal: Goal,                       // e.g. RECOMPOSITION, STRENGTH, ENDURANCE
    val experience: ExperienceLevel,      // BEGINNER / INTERMEDIATE / ADVANCED
    val availableEquipment: List<Equipment>,
    val sessionMinutes: Int,
    val daysPerWeek: Int
)

enum class Goal { STRENGTH, HYPERTROPHY, ENDURANCE, RECOMP, GENERAL_FITNESS }
enum class ExperienceLevel { BEGINNER, INTERMEDIATE, ADVANCED }

data class GenerateRequest(
    val date: String,                     // ISO local date (YYYY-MM-DD)
    val focus: WorkoutFocus,              // PUSH / PULL / LEGS / UPPER / LOWER / FULL_BODY / CONDITIONING
    val modality: Modality,               // STRENGTH or METCON
    val user: UserContext
)

data class ExerciseSpec(
    val exerciseId: Long,                 // must refer to Exercise.id in DB
    val sets: Int,
    val repsMin: Int,
    val repsMax: Int,
    val intensityType: String? = null,    // "RPE", "%1RM", "LOAD"
    val intensityValue: Float? = null
)

data class MetconSpec(
    val blockType: BlockType,             // EMOM, AMRAP, FOR_TIME...
    val durationSec: Int?,
    val intervalSec: Int?,
    val components: List<MetconComponentSpec>
)

data class MetconComponentSpec(
    val note: String,                     // free text for UI (e.g., "15 KB swings")
    val reps: Int? = null,
    val movement: MovementPattern? = null,
    val equipment: List<Equipment> = emptyList()
)

data class GenerateResponse(
    val strength: List<ExerciseSpec> = emptyList(),
    val metcon: MetconSpec? = null
)
