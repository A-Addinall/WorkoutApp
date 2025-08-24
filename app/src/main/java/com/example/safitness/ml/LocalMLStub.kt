package com.example.safitness.ml

import com.example.safitness.core.*
import com.example.safitness.data.dao.LibraryDao
import com.example.safitness.data.dao.PlanDao

import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.Collections

import com.example.safitness.data.entities.Exercise   // harmless even if not used elsewhere

class LocalMLStub(
    private val planDao: PlanDao,
    private val libraryDao: LibraryDao
) : MLService {
    override suspend fun generate(req: GenerateRequest): GenerateResponse {
        val eqPool: List<Equipment> =
            if (req.user.availableEquipment.isEmpty())
                listOf(Equipment.DUMBBELL, Equipment.BARBELL, Equipment.BODYWEIGHT)
            else req.user.availableEquipment

        val date = runCatching { LocalDate.parse(req.date) }.getOrElse { LocalDate.now() }
        val daySeed: Long = date.toEpochDay()
        val shuffleSeed: Int = daySeed.toInt()
        val isoWeek = java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()
        val micro = ((date.get(isoWeek) % 3) + 3) % 3 // 0,1,2 cycle for progression

        // Avoid repeats within the Mon..Sun of this date
        val weekStart = date.with(java.time.DayOfWeek.MONDAY).toEpochDay()
        val weekEnd   = weekStart + 6
        val plannedThisWeek: Set<Long> = planDao.strengthIdsBetween(weekStart, weekEnd).toSet()

        fun mapFocusToType(f: WorkoutFocus): WorkoutType = when (f) {
            WorkoutFocus.PUSH          -> WorkoutType.PUSH
            WorkoutFocus.PULL          -> WorkoutType.PULL
            WorkoutFocus.LEGS, WorkoutFocus.LOWER -> WorkoutType.LEGS_CORE
            WorkoutFocus.UPPER, WorkoutFocus.FULL_BODY ->
                if (daySeed % 2L == 0L) WorkoutType.PUSH else WorkoutType.PULL
            WorkoutFocus.CORE          -> WorkoutType.LEGS_CORE
            WorkoutFocus.CONDITIONING  -> when ((daySeed % 3L).toInt()) {
                0 -> WorkoutType.PUSH; 1 -> WorkoutType.PULL; else -> WorkoutType.LEGS_CORE
            }
        }
        val focusType = mapFocusToType(req.focus)

        // Early Metcon path unchanged
        if (req.modality == Modality.METCON) {
            val targetMin = (req.user.sessionMinutes.coerceIn(20, 90) / 4).coerceIn(12, 16)
            val metcon = MetconSpec(
                blockType = BlockType.AMRAP,
                durationSec = targetMin * 60,
                intervalSec = null,
                components = listOf(
                    MetconComponentSpec("10 Push-ups", 10, MovementPattern.HORIZONTAL_PUSH, listOf(Equipment.BODYWEIGHT)),
                    MetconComponentSpec("15 Air Squats", 15, MovementPattern.SQUAT,           listOf(Equipment.BODYWEIGHT)),
                    MetconComponentSpec("10 DB Row / side", 10, MovementPattern.HORIZONTAL_PULL, listOf(Equipment.DUMBBELL))
                )
            )
            return GenerateResponse(strength = emptyList(), metcon = metcon)
        }

        data class Want(val movement: MovementPattern?, val muscles: List<MuscleGroup>)

        // Blueprint: main + supporting muscles per focus
        val wantsBase: List<Want> = when (focusType) {
            WorkoutType.PUSH -> listOf(
                Want(MovementPattern.HORIZONTAL_PUSH, listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS)),
                Want(MovementPattern.VERTICAL_PUSH,   listOf(MuscleGroup.DELTS_MED, MuscleGroup.DELTS_ANT)),
                Want(null,                            listOf(MuscleGroup.TRICEPS))
            )
            WorkoutType.PULL -> listOf(
                Want(MovementPattern.HORIZONTAL_PULL, listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.LATS)),
                Want(MovementPattern.VERTICAL_PULL,   listOf(MuscleGroup.LATS, MuscleGroup.BACK)),
                Want(null,                            listOf(MuscleGroup.BICEPS))
            )
            WorkoutType.LEGS_CORE -> if (daySeed % 2L == 0L) {
                listOf(
                    Want(MovementPattern.SQUAT, listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES)),
                    Want(MovementPattern.HINGE, listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES)),
                    Want(null,                  listOf(MuscleGroup.ABS, MuscleGroup.OBLIQUES))
                )
            } else {
                listOf(
                    Want(MovementPattern.HINGE, listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES)),
                    Want(MovementPattern.SQUAT, listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES)),
                    Want(null,                  listOf(MuscleGroup.ABS, MuscleGroup.OBLIQUES))
                )
            }
            else -> emptyList()
        }

        // For INT/ADV, add an extra accessory slot per cycle: push (fly/isol.), pull (rear-delt/isol.), legs (unilateral)
        val extraWants: List<Want> = when (focusType) {
            WorkoutType.PUSH -> listOf(Want(null, listOf(MuscleGroup.CHEST)))          // e.g., fly/pec-deck
            WorkoutType.PULL -> listOf(Want(null, listOf(MuscleGroup.REAR_DELTS)))     // e.g., rear-delt row/raise
            WorkoutType.LEGS_CORE -> listOf(Want(MovementPattern.LUNGE, listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES)))
            else -> emptyList()
        }

        val targetAccessories = when (req.user.experience) {
            ExperienceLevel.BEGINNER     -> 2  // total 3
            ExperienceLevel.INTERMEDIATE -> 3  // total 4
            ExperienceLevel.ADVANCED     -> 4  // total 5
        }
        val wants = wantsBase + extraWants.take(maxOf(0, targetAccessories - 2))

        val typedPool = libraryDao.getExercises(type = focusType, eq = null).first()
            .filter { it.primaryEquipment in eqPool }

        fun stableShuffle(xs: List<Exercise>): List<Exercise> =
            xs.sortedBy { ((it.id.toInt() shl 3) xor shuffleSeed) }

        suspend fun poolFor(w: Want): List<Exercise> {
            val meta = libraryDao.filterExercises(
                movement = w.movement,
                muscles = w.muscles,
                equipment = eqPool,
                musclesEmpty = if (w.muscles.isEmpty()) 1 else 0,
                equipmentEmpty = if (eqPool.isEmpty()) 1 else 0
            ).filter { it.id !in plannedThisWeek }
            if (meta.isNotEmpty()) return meta

            val byType = typedPool.filter { it.id !in plannedThisWeek }
            if (byType.isNotEmpty()) return byType

            return libraryDao.getExercises(type = null, eq = null).first()
                .filter { it.primaryEquipment in eqPool }
        }

        fun fam(e: Exercise): String = when (e.movement) {
            MovementPattern.HORIZONTAL_PUSH, MovementPattern.VERTICAL_PUSH   -> "PUSH"
            MovementPattern.HORIZONTAL_PULL, MovementPattern.VERTICAL_PULL   -> "PULL"
            MovementPattern.SQUAT, MovementPattern.HINGE, MovementPattern.LUNGE -> "LEGS"
            else -> "OTHER"
        }

        val used = mutableSetOf<Long>()
        val picks = mutableListOf<Exercise>()
        wants.forEachIndexed { idx, want ->
            val pool = stableShuffle(poolFor(want)).filter { it.id !in used }
            val candidate = if (idx == 0) {
                // main
                pool.firstOrNull()
            } else {
                val mainFam = picks.firstOrNull()?.let(::fam)
                pool.firstOrNull { e -> mainFam == null || fam(e) != mainFam } ?: pool.firstOrNull()
            }
            if (candidate != null) {
                used += candidate.id
                picks += candidate
            }
            if (picks.size >= (1 + targetAccessories)) return@forEachIndexed
        }

        // Ensure we hit target count by widening if needed
        if (picks.size < (1 + targetAccessories)) {
            val widen = (typedPool + libraryDao.getExercises(type = null, eq = null).first()
                .filter { it.primaryEquipment in eqPool })
                .distinctBy { it.id }
                .filter { it.id !in used && it.id !in plannedThisWeek }
            picks += stableShuffle(widen).take(1 + targetAccessories - picks.size)
        }

        // --- Progression: 3-week undulating on the main; modest tweaks on accessories
        data class Scheme(val sets: Int, val reps: Int)
        fun mainScheme(exp: ExperienceLevel, micro: Int): Scheme = when (exp) {
            ExperienceLevel.BEGINNER -> listOf(Scheme(4,5), Scheme(3,8), Scheme(5,3))[micro]
            ExperienceLevel.INTERMEDIATE -> listOf(Scheme(5,5), Scheme(4,8), Scheme(5,3))[micro]
            ExperienceLevel.ADVANCED -> listOf(Scheme(5,3), Scheme(6,4), Scheme(5,2))[micro]
        }
        fun secScheme(exp: ExperienceLevel, micro: Int): Scheme = when (exp) {
            ExperienceLevel.BEGINNER -> listOf(Scheme(3,8), Scheme(3,10), Scheme(3,8))[micro]
            ExperienceLevel.INTERMEDIATE -> listOf(Scheme(3,10), Scheme(4,8), Scheme(3,12))[micro]
            ExperienceLevel.ADVANCED -> listOf(Scheme(4,8), Scheme(4,10), Scheme(4,8))[micro]
        }
        fun accScheme(exp: ExperienceLevel, micro: Int): Scheme = when (exp) {
            ExperienceLevel.BEGINNER -> listOf(Scheme(2,12), Scheme(2,15), Scheme(2,12))[micro]
            ExperienceLevel.INTERMEDIATE -> listOf(Scheme(3,12), Scheme(3,15), Scheme(3,12))[micro]
            ExperienceLevel.ADVANCED -> listOf(Scheme(3,12), Scheme(3,15), Scheme(4,10))[micro]
        }

        val strength = picks.mapIndexed { idx, e ->
            val sch = when (idx) {
                0 -> mainScheme(req.user.experience, micro)
                1 -> secScheme(req.user.experience, micro)
                else -> accScheme(req.user.experience, micro)
            }
            ExerciseSpec(
                exerciseId = e.id,
                sets = sch.sets,
                targetReps = sch.reps,
                intensityType = null,
                intensityValue = null
            )
        }

        return GenerateResponse(strength = strength, metcon = null)
    }
}