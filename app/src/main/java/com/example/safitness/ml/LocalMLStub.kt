package com.example.safitness.ml

import com.example.safitness.core.*
import com.example.safitness.data.dao.LibraryDao
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Minimal stub that just returns 2-3 sensible specs so you can wire UI/DB without waiting on a real model.
 * Replace with a network client later.
 */
class LocalMLStub(
    private val libraryDao: LibraryDao
) : MLService {
    override suspend fun generate(req: GenerateRequest): GenerateResponse {
        // ---------- 0) Normalise inputs ----------
        val userEq: List<Equipment> =
            if (req.user.availableEquipment.isEmpty())
                listOf(Equipment.DUMBBELL, Equipment.BARBELL, Equipment.BODYWEIGHT)
            else req.user.availableEquipment

        // Map the focus you already use in MainActivity -> your legacy WorkoutType buckets
        fun mapFocusToType(f: WorkoutFocus): WorkoutType = when (f) {
            WorkoutFocus.PUSH -> WorkoutType.PUSH
            WorkoutFocus.PULL -> WorkoutType.PULL
            WorkoutFocus.LEGS,
            WorkoutFocus.LOWER -> WorkoutType.LEGS_CORE

            WorkoutFocus.UPPER -> WorkoutType.PULL     // balances the week vs PUSH
            WorkoutFocus.FULL_BODY -> WorkoutType.LEGS_CORE
            WorkoutFocus.CORE,
            WorkoutFocus.CONDITIONING -> WorkoutType.LEGS_CORE
        }

        // ---------- 1) METCON path (kept simple, deterministic) ----------
        if (req.modality == Modality.METCON) {
            // 12–16 min AMRAP scaled by session length
            val targetMin = (req.user.sessionMinutes.coerceIn(20, 90) / 4).coerceIn(12, 16)
            val metcon = MetconSpec(
                blockType = BlockType.AMRAP,
                durationSec = targetMin * 60,
                intervalSec = null,
                components = listOf(
                    // Keep components generic; UI shows note + pattern + allowed equipment
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

        // ---------- 2) STRENGTH path ----------
        // 2a) Choose exercises using your SimplePlanner for variety + equipment constraints
        val planner = com.example.safitness.domain.planner.SimplePlanner(libraryDao)
        val type = mapFocusToType(req.focus)

        // Ask the planner for a reasonable pool; we'll then trim/cap to the budget
        val pool = planner
            .suggestFor(focus = type, availableEq = userEq)
            .distinctBy { it.exercise.id }        // kill dupes
            .toMutableList()
        if (pool.isNotEmpty()) {
            val seed = LocalDate.now().toEpochDay().toInt()
            val off = kotlin.math.abs(seed) % pool.size
            java.util.Collections.rotate(pool, -off)
        }
        if (pool.isEmpty()) {
            // Fallback: return something harmless if the library has no match
            val metcon = MetconSpec(
                blockType = BlockType.AMRAP,
                durationSec = 12 * 60,
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
                    )
                )
            )
            return GenerateResponse(strength = emptyList(), metcon = metcon)
        }

        // 2b) Volume budget by experience + session length (rough rule of thumb)
        val minutes = req.user.sessionMinutes.coerceIn(20, 120)
        val setsBudget = when (req.user.experience) {
            ExperienceLevel.BEGINNER -> (minutes / 10.0).toInt().coerceIn(8, 14)
            ExperienceLevel.INTERMEDIATE -> (minutes / 8.0).toInt().coerceIn(10, 18)
            ExperienceLevel.ADVANCED -> (minutes / 7.0).toInt().coerceIn(12, 20)
        }

        // 2c) Pick 1 main + up to 2 accessories under the budget
        val picks = mutableListOf<com.example.safitness.domain.planner.Suggestion>()
        val main = pool.removeAt(0)              // the planner tends to put a sensible main first
        picks += main

        // Prefer accessories from different patterns if available
        for (cand in pool) {
            if (picks.size >= 3) break
            val already = picks.any { it.exercise.id == cand.exercise.id }
            if (!already) picks += cand
        }

        // 2d) Rep schemes by goal (simple periodisation)
        data class Scheme(val mainSets: Int, val mainReps: Int, val accSets: Int, val accReps: Int)

        val scheme = when (req.user.goal) {
            Goal.STRENGTH -> Scheme(mainSets = 5, mainReps = 5, accSets = 3, accReps = 8)
            Goal.HYPERTROPHY, Goal.RECOMP, Goal.GENERAL_FITNESS ->
                Scheme(mainSets = 4, mainReps = 8, accSets = 3, accReps = 12)

            Goal.ENDURANCE -> Scheme(mainSets = 4, mainReps = 12, accSets = 3, accReps = 15)
        }

        // Distribute sets to respect the volume budget
        val plannedMainSets = scheme.mainSets
        val remainingForAcc = (setsBudget - plannedMainSets).coerceAtLeast(0)
        val accEach = if (picks.size > 1) (remainingForAcc / (picks.size - 1)).coerceIn(
            2,
            scheme.accSets
        ) else 0

        // 2e) Build ExerciseSpec list
        val strength = picks.mapIndexed { idx, s ->
            val isMain = idx == 0
            ExerciseSpec(
                exerciseId = s.exercise.id,
                sets = if (isMain) plannedMainSets else accEach,
                // PlannerRepository.applyMlToDate() will snap these to allowed buckets;
                // we feed typical targets that map cleanly.
                targetReps = if (isMain) scheme.mainReps else scheme.accReps,
                intensityType = null,
                intensityValue = null
            )
        }.filter { it.sets > 0 } // don’t emit empty accessories when budget is tiny

        return GenerateResponse(strength = strength, metcon = null)
    }
}
