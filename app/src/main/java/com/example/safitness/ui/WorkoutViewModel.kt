package com.example.safitness.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.safitness.core.Equipment
import com.example.safitness.core.MetconResult
import com.example.safitness.data.dao.ExerciseWithSelection
import com.example.safitness.data.dao.MetconSummary
import com.example.safitness.data.dao.PlanWithComponents
import com.example.safitness.data.dao.SelectionWithPlanAndComponents
import com.example.safitness.data.entities.MetconLog
import com.example.safitness.data.repo.WorkoutRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.example.safitness.core.PrCelebrationEvent

class WorkoutViewModel(private val repo: WorkoutRepository) : ViewModel() {

    private val dayLive = MutableLiveData<Int>()

    /** Strength (non-metcon) programme entries for the selected day. */
    val programForDay: LiveData<List<ExerciseWithSelection>> =
        dayLive.switchMap { day -> repo.programForDay(day).asLiveData() }

    /** Legacy day-scoped last metcon summary (kept for compatibility). */
    private val _lastMetcon = MutableLiveData<MetconSummary?>()
    val lastMetcon: LiveData<MetconSummary?> = _lastMetcon

    /** Metcon plan selections for the selected day. */
    val metconsForDay: LiveData<List<SelectionWithPlanAndComponents>> =
        dayLive.switchMap { day -> repo.metconsForDay(day).asLiveData() }

    /** Load a specific metcon plan with its components. */
    fun planWithComponents(planId: Long): LiveData<PlanWithComponents> =
        repo.planWithComponents(planId).asLiveData()

    /** Legacy seconds-only label (day-scoped), used by some existing UI. */
    private val _lastMetconSeconds = MutableLiveData(0)
    val lastMetconSeconds: LiveData<Int> = _lastMetconSeconds

    /** NEW (Phase 1): prefer metcon_log; fallback to legacy summary */
    val lastMetconDisplay: LiveData<WorkoutRepository.MetconDisplay?> =
        dayLive.switchMap { day -> repo.lastMetconDisplayForDay(day).asLiveData() }

    // --- PR celebration one-shot events ---
// --- PR celebration one-shot events ---
    private val _prEvents = MutableSharedFlow<PrCelebrationEvent>(replay = 0, extraBufferCapacity = 1)
    val prEvents: SharedFlow<PrCelebrationEvent> = _prEvents

    suspend fun bestE1RM(exerciseId: Long, equipment: Equipment): Double? {
        return repo.bestE1RM(exerciseId, equipment)
    }
    fun setDay(day: Int) {
        dayLive.value = day
        viewModelScope.launch {
            // legacy day-scoped summaries
            _lastMetcon.value = repo.lastMetconForDay(day)
            _lastMetconSeconds.value = repo.lastMetconSecondsForDay(day)
        }
    }

    suspend fun previewPrEvent(
        exerciseId: Long,
        equipment: Equipment,
        reps: Int,
        weight: Double
    ) = repo.previewPrEvent(exerciseId, equipment, reps, weight)

    /* ----------------------------- Strength logging ----------------------------- */

    fun logStrengthSet(
        sessionId: Long,
        exerciseId: Long,
        equipment: Equipment,
        setNumber: Int,
        reps: Int,
        weight: Double,
        rpe: Double?,
        success: Boolean,
        notes: String?
    ) = viewModelScope.launch {
        repo.logStrengthSet(
            sessionId = sessionId,
            exerciseId = exerciseId,
            equipment = equipment,
            setNumber = setNumber,
            reps = reps,
            weight = weight,
            rpe = rpe,
            success = success,
            notes = notes
        )

        // PR pipeline
        runCatching {
            repo.evaluateAndRecordPrIfAny(
                exerciseId = exerciseId,
                equipment = equipment,
                reps = reps,
                weightKg = weight,
                success = success
            )
        }.getOrNull()?.let { pr ->
            _prEvents.emit(pr)
            // (Optional) hook rest-timer auto-extend here later.
        }
    }

    /* ----------------------------- Legacy metcon (day-scoped) ----------------------------- */

    fun logMetcon(day: Int, seconds: Int, result: MetconResult) = viewModelScope.launch {
        // kept for old For-Time path; new screens use the plan-scoped methods below
        repo.logMetcon(day, seconds, result)
        _lastMetcon.value = repo.lastMetconForDay(day)
    }

    /* ----------------------------- NEW: plan-scoped metcon API ----------------------------- */

    /** Observe the most recent metcon log for a specific plan (FOR_TIME / AMRAP / EMOM). */
    fun lastMetconForPlan(planId: Long): LiveData<MetconLog?> =
        repo.lastMetconForPlan(planId).asLiveData()

    /** FOR_TIME: store the actual completion time (seconds) against the plan. */
    fun logMetconForTime(day: Int, planId: Long, timeSeconds: Int, result: MetconResult) =
        viewModelScope.launch {
            repo.logMetconForTime(day, planId, timeSeconds, result)
        }

    /** AMRAP: store programmed duration + rounds + extra reps. */
    fun logMetconAmrap(
        day: Int,
        planId: Long,
        durationSeconds: Int,
        rounds: Int,
        extraReps: Int,
        result: MetconResult
    ) = viewModelScope.launch {
        repo.logMetconAmrap(day, planId, durationSeconds, rounds, extraReps, result)
    }

    /** EMOM: store programmed duration (+ optional intervals completed). */
    fun logMetconEmom(
        day: Int,
        planId: Long,
        durationSeconds: Int,
        intervalsCompleted: Int?,
        result: MetconResult
    ) = viewModelScope.launch {
        repo.logMetconEmom(day, planId, durationSeconds, intervalsCompleted, result)
    }

    /* ----------------------------- Day summary label ----------------------------- */

    data class DaySummary(val subtitle: String)

    fun daySummary(day: Int): LiveData<DaySummary> {
        // programForDay → strength items (metcon plans are stored separately)
        val strengthFlow = repo.programForDay(day) // Flow<List<ExerciseWithSelection>>
        val metconFlow = repo.metconsForDay(day)   // Flow<List<SelectionWithPlanAndComponents>>

        return combine(strengthFlow, metconFlow) { program, metcons ->
            val hasMetcon = metcons.isNotEmpty()

            val strengthCats = program
                .filter { it.exercise.modality != com.example.safitness.core.Modality.METCON }
                .filter { it.required }
                .map { it.exercise.workoutType }
                .map(::mapWorkoutTypeToLabel)
                .distinct()

            val subtitle = when {
                strengthCats.isEmpty() && hasMetcon -> "Metcon"
                strengthCats.isEmpty() && !hasMetcon -> "Empty"
                strengthCats.size == 1 && !hasMetcon -> strengthCats.first()
                strengthCats.size == 1 && hasMetcon -> "${strengthCats.first()} + Metcon"
                else -> {
                    val mixed = strengthCats.take(2).joinToString(" · ")
                    if (hasMetcon) "Mixed ($mixed) + Metcon" else "Mixed ($mixed)"
                }
            }

            DaySummary(subtitle = subtitle)
        }.asLiveData()
    }

    private fun mapWorkoutTypeToLabel(t: com.example.safitness.core.WorkoutType): String = when (t) {
        com.example.safitness.core.WorkoutType.PUSH -> "Push"
        com.example.safitness.core.WorkoutType.PULL -> "Pull"
        com.example.safitness.core.WorkoutType.LEGS_CORE -> "Legs & Core"
        else -> t.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }

    /* ----------------------------- Strength heuristics ----------------------------- */

    // Legacy helpers consumed by ExerciseDetailActivity
    suspend fun getLastSuccessfulWeight(exerciseId: Long, equipment: Equipment, reps: Int?) =
        repo.getLastSuccessfulWeight(exerciseId, equipment, reps)

    suspend fun suggestNextLoadKg(exerciseId: Long, equipment: Equipment, reps: Int): Double? =
        repo.suggestNextLoadKg(exerciseId, equipment, reps)

    class DebugTestViewModel(
        private val libraryDao: com.example.safitness.data.dao.LibraryDao,
        private val metconDao: com.example.safitness.data.dao.MetconDao
    ) : ViewModel() {

        fun runMetadataTests() = viewModelScope.launch {
            // A) Exercise metadata query
            val dbChestPushDB = libraryDao.filterExercises(
                movement = com.example.safitness.core.MovementPattern.HORIZONTAL_PUSH,
                muscles = listOf(com.example.safitness.core.MuscleGroup.CHEST),
                equipment = listOf(com.example.safitness.core.Equipment.DUMBBELL)
            )
            android.util.Log.d("TEST", "DB Horizontal Push (CHEST + DB): ${dbChestPushDB.map { it.name }}")

            // B) Metcon metadata query (AMRAP lower-body, bodyweight only)
            val amrapLegsBodyweight = metconDao.filterComponents(
                blockType = com.example.safitness.core.BlockType.AMRAP,
                movement = com.example.safitness.core.MovementPattern.SQUAT,
                muscles = listOf(
                    com.example.safitness.core.MuscleGroup.QUADS,
                    com.example.safitness.core.MuscleGroup.GLUTES
                ),
                equipment = listOf(com.example.safitness.core.Equipment.BODYWEIGHT)
            )
            android.util.Log.d("TEST", "AMRAP legs BW components: ${
                amrapLegsBodyweight.map { "planId=${it.planId} order=${it.orderInPlan} text=${it.text}" }
            }")

            // C) Sanity check a known metcon (Cindy/Helen) still load via your existing relation
            // (This uses your existing flow relation and ensures the text + structured fields coexist)
            // metconDao.getPlanWithComponents(planId).collect { planWithComps -> ... }  // if you want
        }
    }


}
