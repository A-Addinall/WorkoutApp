package com.example.safitness.ml

import com.example.safitness.core.*
import com.example.safitness.data.dao.LibraryDao
import com.example.safitness.data.dao.PlanDao

import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.Collections
import com.example.safitness.domain.planner.Suggestion
import com.example.safitness.data.entities.Exercise   // harmless even if not used elsewhere



/**
 * Minimal stub that just returns 2-3 sensible specs so you can wire UI/DB without waiting on a real model.
 * Replace with a network client later.
 */
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

        // Week window (Mon..Sun) for the *target* date
        val weekStart = date.with(java.time.DayOfWeek.MONDAY).toEpochDay()
        val weekEnd = weekStart + 6
        val plannedThisWeek: Set<Long> = planDao.strengthIdsBetween(weekStart, weekEnd).toSet()

        fun mapFocusToType(f: WorkoutFocus): WorkoutType = when (f) {
            WorkoutFocus.PUSH -> WorkoutType.PUSH
            WorkoutFocus.PULL -> WorkoutType.PULL
            WorkoutFocus.LEGS, WorkoutFocus.LOWER -> WorkoutType.LEGS_CORE
            WorkoutFocus.UPPER -> if (daySeed % 2L == 0L) WorkoutType.PUSH else WorkoutType.PULL
            WorkoutFocus.FULL_BODY -> if (daySeed % 2L == 0L) WorkoutType.PUSH else WorkoutType.PULL
            WorkoutFocus.CORE -> WorkoutType.LEGS_CORE
            WorkoutFocus.CONDITIONING -> when ((daySeed % 3L).toInt()) {
                0 -> WorkoutType.PUSH
                1 -> WorkoutType.PULL
                else -> WorkoutType.LEGS_CORE
            }
        }

        val focusType = mapFocusToType(req.focus)

        // METCON path unchanged
        if (req.modality == Modality.METCON) {
            val targetMin = (req.user.sessionMinutes.coerceIn(20, 90) / 4).coerceIn(12, 16)
            val metcon = MetconSpec(
                blockType = BlockType.AMRAP,
                durationSec = targetMin * 60,
                intervalSec = null,
                components = listOf(
                    MetconComponentSpec(
                        "10 Push-ups",
                        10,
                        MovementPattern.HORIZONTAL_PUSH,
                        listOf(Equipment.BODYWEIGHT)
                    ),
                    MetconComponentSpec(
                        "15 Air Squats",
                        15,
                        MovementPattern.SQUAT,
                        listOf(Equipment.BODYWEIGHT)
                    ),
                    MetconComponentSpec(
                        "10 DB Row / side",
                        10,
                        MovementPattern.HORIZONTAL_PULL,
                        listOf(Equipment.DUMBBELL)
                    )
                )
            )
            return GenerateResponse(strength = emptyList(), metcon = metcon)
        }

        // ---- Build pools (type-aware, equipment-filtered), excluding this week's picks ----
        val typedRaw = libraryDao.getExercises(type = focusType, eq = null).first()
            .filter { it.primaryEquipment in eqPool }

        // Try to exclude week-duplicates; if that makes the pool too small, fall back to raw
        val typed = typedRaw.filter { it.id !in plannedThisWeek }.ifEmpty { typedRaw }

        fun isPull(e: Exercise) =
            e.movement == MovementPattern.HORIZONTAL_PULL || e.movement == MovementPattern.VERTICAL_PULL

        fun isPush(e: Exercise) =
            e.movement == MovementPattern.HORIZONTAL_PUSH || e.movement == MovementPattern.VERTICAL_PUSH

        fun isLegsCore(e: Exercise) =
            e.movement == MovementPattern.SQUAT || e.movement == MovementPattern.HINGE || e.movement == MovementPattern.LUNGE

        fun stableShuffle(xs: List<Exercise>): List<Exercise> =
            xs.sortedBy { ((it.id.toInt() shl 3) xor shuffleSeed) }

        // Partition into main vs accessory (heuristic)
        fun isAccessoryForFocus(e: Exercise): Boolean = when (focusType) {
            WorkoutType.PULL -> !isPull(e) || (e.primaryEquipment != Equipment.BARBELL && e.primaryEquipment != Equipment.BODYWEIGHT)
            WorkoutType.PUSH -> !isPush(e) || (e.primaryEquipment != Equipment.BARBELL && e.primaryEquipment != Equipment.BODYWEIGHT)
            WorkoutType.LEGS_CORE -> !isLegsCore(e) || (e.primaryEquipment != Equipment.BARBELL && e.primaryEquipment != Equipment.BODYWEIGHT)
            else -> true
        }

        val (mainPrim, accPrim) = when (focusType) {
            WorkoutType.PULL -> typed.partition(::isPull)
            WorkoutType.PUSH -> typed.partition(::isPush)
            WorkoutType.LEGS_CORE -> typed.partition(::isLegsCore)
            else -> typed to emptyList()
        }

        // If still thin, widen to any type (still filtered by equipment), also excluding week dupes if possible
        val widenedAnyRaw = if (typed.size < 3) {
            libraryDao.getExercises(type = null, eq = null).first()
                .filter { it.primaryEquipment in eqPool }
        } else emptyList()
        val widenedAny =
            widenedAnyRaw.filter { it.id !in plannedThisWeek }.ifEmpty { widenedAnyRaw }

        val mainPool = (mainPrim + widenedAny.filter {
            when (focusType) {
                WorkoutType.PULL -> isPull(it)
                WorkoutType.PUSH -> isPush(it)
                WorkoutType.LEGS_CORE -> isLegsCore(it)
                else -> true
            }
        }).distinctBy { it.id }

        val accPool =
            (accPrim + typed.filter(::isAccessoryForFocus) + widenedAny.filter(::isAccessoryForFocus))
                .distinctBy { it.id }
                .filter { it.id !in mainPool.map { m -> m.id }.toSet() }

        // ---- Pick 3: 1 main + 2 accessories (prefer different movement families) ----
        val picks = mutableListOf<Exercise>()
        stableShuffle(mainPool).firstOrNull()?.let { picks += it }

        fun movementFamily(e: Exercise): String = when (e.movement) {
            MovementPattern.HORIZONTAL_PULL, MovementPattern.VERTICAL_PULL -> "PULL"
            MovementPattern.HORIZONTAL_PUSH, MovementPattern.VERTICAL_PUSH -> "PUSH"
            MovementPattern.SQUAT, MovementPattern.HINGE, MovementPattern.LUNGE -> "LEGS"
            else -> "OTHER"
        }

        val mainFam = picks.firstOrNull()?.let(::movementFamily)

        val acc1 = stableShuffle(accPool).firstOrNull { a ->
            a.id !in picks.asSequence().map { it.id }.toSet() && (mainFam == null || movementFamily(
                a
            ) != mainFam)
        }
        if (acc1 != null) picks += acc1

        val acc2 = stableShuffle(accPool).firstOrNull { a ->
            a.id !in picks.asSequence().map { it.id }.toSet() && (mainFam == null || movementFamily(
                a
            ) != mainFam)
        } ?: stableShuffle(accPool).firstOrNull { a ->
            a.id !in picks.asSequence().map { it.id }.toSet()
        }
        if (acc2 != null) picks += acc2

        val finalPicks = picks.distinctBy { it.id }.take(3)

        // Schemes by experience
        data class Scheme(val sets: Int, val reps: Int)

        val mainScheme = when (req.user.experience) {
            ExperienceLevel.BEGINNER -> Scheme(4, 5)
            ExperienceLevel.INTERMEDIATE -> Scheme(5, 5)
            ExperienceLevel.ADVANCED -> Scheme(5, 3)
        }
        val secScheme = when (req.user.experience) {
            ExperienceLevel.BEGINNER -> Scheme(3, 8)
            ExperienceLevel.INTERMEDIATE -> Scheme(3, 10)
            ExperienceLevel.ADVANCED -> Scheme(4, 8)
        }
        val accScheme = when (req.user.experience) {
            ExperienceLevel.BEGINNER -> Scheme(2, 12)
            ExperienceLevel.INTERMEDIATE -> Scheme(3, 12)
            ExperienceLevel.ADVANCED -> Scheme(3, 12)
        }

        val strength = finalPicks.mapIndexed { idx, e ->
            val sch = when (idx) {
                0 -> mainScheme; 1 -> secScheme; else -> accScheme
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