package com.example.safitness.ml

import com.example.safitness.core.*
import com.example.safitness.data.dao.LibraryDao
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
    private val libraryDao: LibraryDao
) : MLService {
    override suspend fun generate(req: GenerateRequest): GenerateResponse {
        // ---------- 0) Normalise inputs ----------
        val eqPool: List<Equipment> =
            if (req.user.availableEquipment.isEmpty())
                listOf(Equipment.DUMBBELL, Equipment.BARBELL, Equipment.BODYWEIGHT)
            else req.user.availableEquipment

        // One seed only (Long), and a derived Int for sorting
        val daySeed: Long = runCatching { LocalDate.parse(req.date).toEpochDay() }
            .getOrElse { LocalDate.now().toEpochDay() }
        val shuffleSeed: Int = daySeed.toInt() // deterministic, sufficient for stable sort key

        // Exhaustive mapping (avoids LEGS bias for CORE/FULL_BODY/CONDITIONING)
        fun mapFocusToType(f: WorkoutFocus): WorkoutType = when (f) {
            WorkoutFocus.PUSH          -> WorkoutType.PUSH
            WorkoutFocus.PULL          -> WorkoutType.PULL
            WorkoutFocus.LEGS,
            WorkoutFocus.LOWER         -> WorkoutType.LEGS_CORE
            WorkoutFocus.UPPER         -> WorkoutType.PUSH
            WorkoutFocus.FULL_BODY     -> if (daySeed % 2L == 0L) WorkoutType.PUSH else WorkoutType.PULL
            WorkoutFocus.CORE          -> if (daySeed % 2L == 0L) WorkoutType.PULL else WorkoutType.PUSH
            WorkoutFocus.CONDITIONING  -> when ((daySeed % 3L).toInt()) {
                0 -> WorkoutType.PUSH
                1 -> WorkoutType.PULL
                else -> WorkoutType.LEGS_CORE
            }
        }

        // ---------- 1) METCON path ----------
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

        // ---------- 2) STRENGTH path ----------
        val focusType = mapFocusToType(req.focus)

        // Start broad by type, then filter by equipment
        val typed = libraryDao.getExercises(type = focusType, eq = null).first()
            .filter { e -> e.primaryEquipment in eqPool }

        // Partition helpers
        fun isPull(e: Exercise) = e.movement == MovementPattern.HORIZONTAL_PULL || e.movement == MovementPattern.VERTICAL_PULL
        fun isPush(e: Exercise) = e.movement == MovementPattern.HORIZONTAL_PUSH || e.movement == MovementPattern.VERTICAL_PUSH
        fun isLegsCore(e: Exercise) =
            e.movement == MovementPattern.SQUAT || e.movement == MovementPattern.HINGE ||
                    e.movement == MovementPattern.LUNGE || e.movement == MovementPattern.CORE

        val primaries = when (focusType) {
            WorkoutType.PULL      -> typed.filter(::isPull)
            WorkoutType.PUSH      -> typed.filter(::isPush)
            WorkoutType.LEGS_CORE -> typed.filter(::isLegsCore)
            else                  -> typed
        }

        // Deterministic shuffle by shuffleSeed, then de-dupe by id
        fun stableShuffle(xs: List<Exercise>): List<Exercise> =
            xs.sortedBy { ((it.id.toInt() shl 3) xor shuffleSeed) }

        var pool = stableShuffle(primaries).distinctBy { it.id }

        // Widen if needed
        if (pool.size < 3) {
            val existing = pool.asSequence().map { p -> p.id }.toSet()
            val widened = stableShuffle(typed).filter { it.id !in existing }
            pool = (pool + widened).distinctBy { it.id }
        }
        if (pool.size < 3) {
            val allAnyType = libraryDao.getExercises(type = null, eq = null).first()
                .filter { it.primaryEquipment in eqPool }
            val existing = pool.asSequence().map { p -> p.id }.toSet()
            val widened2 = stableShuffle(allAnyType).filter { it.id !in existing }
            pool = (pool + widened2).distinctBy { it.id }
        }

        // Simple scheme by experience
        data class Scheme(val sets: Int, val reps: Int)
        val mainScheme = when (req.user.experience) {
            ExperienceLevel.BEGINNER     -> Scheme(4, 5)
            ExperienceLevel.INTERMEDIATE -> Scheme(5, 5)
            ExperienceLevel.ADVANCED     -> Scheme(5, 3)
        }
        val secScheme = when (req.user.experience) {
            ExperienceLevel.BEGINNER     -> Scheme(3, 8)
            ExperienceLevel.INTERMEDIATE -> Scheme(3, 10)
            ExperienceLevel.ADVANCED     -> Scheme(4, 8)
        }
        val accScheme = when (req.user.experience) {
            ExperienceLevel.BEGINNER     -> Scheme(2, 12)
            ExperienceLevel.INTERMEDIATE -> Scheme(3, 12)
            ExperienceLevel.ADVANCED     -> Scheme(3, 12)
        }

        val picks = pool.take(3)
        val strength = picks.mapIndexed { idx, e ->
            val sch = when (idx) { 0 -> mainScheme; 1 -> secScheme; else -> accScheme }
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