package com.example.safitness.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.safitness.R
import com.example.safitness.core.Equipment
import com.example.safitness.core.PrCelebrationEvent
import com.example.safitness.data.entities.SetLog
import com.example.safitness.data.repo.Repos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ExerciseDetailActivity : AppCompatActivity() {

    private val vm: WorkoutViewModel by viewModels {
        WorkoutViewModelFactory(Repos.workoutRepository(this))
    }
    private val repo by lazy { Repos.workoutRepository(this) }

    private val beeper by lazy { TimerBeeper() }
    private var lastPippedSecond: Long = -1L
    private var lastRemainingMs: Long? = null

    private lateinit var tvExerciseName: TextView
    private lateinit var layoutSets: LinearLayout
    private lateinit var btnAddSet: Button
    private lateinit var btnCompleteExercise: Button
    private lateinit var etNotes: EditText

    private var sessionId: Long = 0L
    private var exerciseId: Long = 0L
    private var exerciseName: String = ""
    private var equipmentName: String = "BARBELL"
    private var targetReps: Int? = null
    private var targetSets: Int? = null

    private data class SetRow(
        val container: View,
        val etWeight: EditText,
        val etReps: EditText,
        val rgResult: RadioGroup
    )
    private val setRows = mutableListOf<SetRow>()

    private companion object {
        private const val PREFS_NAME = "user_settings"
        private const val KEY_REST_SECONDS = "rest_time_seconds"
        private const val DEFAULT_REST_SECONDS = 120
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_detail)

        sessionId = intent.getLongExtra("SESSION_ID", 0L)
        exerciseId = intent.getLongExtra("EXERCISE_ID", 0L)
        exerciseName = intent.getStringExtra("EXERCISE_NAME") ?: ""
        equipmentName = intent.getStringExtra("EQUIPMENT") ?: "BARBELL"
        targetReps = intent.getIntExtra("TARGET_REPS", 0).takeIf { it > 0 }
        targetSets = intent.getIntExtra("TARGET_SETS", 0).takeIf { it > 0 }

        bindViews()

        tvExerciseName.text = exerciseName
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        ensureBannerInflated()
        findViewById<View>(R.id.restTimerContainer)?.bringToFront()

        // If there are existing logs, render them read-only. Otherwise, pre-populate planned rows.
        lifecycleScope.launch {
            val existing = withContext(Dispatchers.IO) {
                com.example.safitness.data.db.AppDatabase
                    .get(this@ExerciseDetailActivity)
                    .sessionDao()
                    .setsForSessionExercise(sessionId, exerciseId)
            }
            if (existing.isNotEmpty()) {
                renderExistingSets(existing)
                btnAddSet.isEnabled = false
                btnCompleteExercise.isEnabled = false
                btnCompleteExercise.text = "Already logged"
            } else {
                val planned = (targetSets ?: 1).coerceAtLeast(1)
                repeat(planned) { addNewSetRow() }
                btnAddSet.setOnClickListener { addNewSetRow() }
                btnCompleteExercise.setOnClickListener { onCompleteExercise() }
            }
        }

        bindRestTimerBanner()
        repo.restTimerState.value?.let {
            forceShowBanner(it.remainingMs, it.durationMs, it.isRunning)
        }
    }

    private fun bindViews() {
        tvExerciseName = findViewById(R.id.tvExerciseName)
        layoutSets = findViewById(R.id.layoutSets)
        btnAddSet = findViewById(R.id.btnAddSet)
        btnCompleteExercise = findViewById(R.id.btnCompleteExercise)
        etNotes = findViewById(R.id.etNotes)
    }

    private fun getBaseRestMs(context: Context = this): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sec = prefs.getInt(KEY_REST_SECONDS, DEFAULT_REST_SECONDS)
        return (sec.coerceAtLeast(10)) * 1000L
    }

    /** Inflate an empty, editable row (used when there are no existing logs). */
    private fun addNewSetRow() {
        val setNumber = setRows.size + 1
        val v = layoutInflater.inflate(R.layout.item_set_entry, layoutSets, false)

        val tvSetNumber = v.findViewById<TextView>(R.id.tvSetNumber)
        val etWeight = v.findViewById<EditText>(R.id.etWeight)
        val etReps = v.findViewById<EditText>(R.id.etReps)
        val rgResult = v.findViewById<RadioGroup>(R.id.rgStrengthResult)

        tvSetNumber.text = "Set $setNumber:"
        etReps.setText(targetReps?.toString() ?: "")
        etReps.isEnabled = false
        etWeight.setHintTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))

        rgResult.setOnCheckedChangeListener { _, checkedId ->
            val weightVal = etWeight.text.toString().toDoubleOrNull()
            val repsVal = targetReps ?: etReps.text.toString().toIntOrNull()
            if (weightVal == null || repsVal == null || repsVal <= 0) return@setOnCheckedChangeListener

            val baseMs = getBaseRestMs()
            when (checkedId) {
                R.id.rbSuccess -> {
                    repo.startRestTimer(sessionId, exerciseId, baseMs)
                    resetBeepTracking()
                    forceShowBanner(baseMs, baseMs, isRunning = true)
                }
                R.id.rbFail -> {
                    val state = repo.restTimerState.value
                    if (state == null) {
                        val dur = baseMs + 30_000L
                        repo.startRestTimer(sessionId, exerciseId, dur)
                        resetBeepTracking()
                        forceShowBanner(dur, dur, isRunning = true)
                    } else {
                        repo.addFailBonusRest(30_000L)
                        forceShowBanner(
                            state.remainingMs + 30_000L,
                            state.durationMs + 30_000L,
                            state.isRunning
                        )
                    }
                    Toast.makeText(this, getString(R.string.rest_timer_bonus_toast), Toast.LENGTH_SHORT).show()
                }
            }
        }

        val row = SetRow(v, etWeight, etReps, rgResult)
        setRows += row
        layoutSets.addView(v)
    }

    /** Render rows using existing logs (read-only so the user can review). */
    private fun renderExistingSets(existing: List<SetLog>) {
        layoutSets.removeAllViews()
        setRows.clear()

        existing.forEachIndexed { idx, s ->
            val v = layoutInflater.inflate(R.layout.item_set_entry, layoutSets, false)
            val tvSetNumber = v.findViewById<TextView>(R.id.tvSetNumber)
            val etWeight = v.findViewById<EditText>(R.id.etWeight)
            val etReps = v.findViewById<EditText>(R.id.etReps)
            val rgResult = v.findViewById<RadioGroup>(R.id.rgStrengthResult)

            tvSetNumber.text = "Set ${idx + 1}:"
            val w = s.weight?.let { String.format(Locale.UK, "%.1f", it) } ?: ""
            etWeight.setText(w)
            etReps.setText(s.reps?.toString() ?: (targetReps?.toString() ?: ""))

            when (s.success) {
                true  -> rgResult.check(R.id.rbSuccess)
                false -> rgResult.check(R.id.rbFail)
                null  -> rgResult.clearCheck()
            }

            // make read-only
            etWeight.isEnabled = false
            etReps.isEnabled = false
            for (i in 0 until rgResult.childCount) rgResult.getChildAt(i).isEnabled = false

            layoutSets.addView(v)
        }
    }

    /** Preview → Log → PR feedback (per set), then close. */
    private fun onCompleteExercise() {
        lifecycleScope.launch {
            // avoid double logging
            val already = withContext(Dispatchers.IO) {
                com.example.safitness.data.db.AppDatabase
                    .get(this@ExerciseDetailActivity)
                    .sessionDao()
                    .setsForSessionExercise(sessionId, exerciseId)
            }
            if (already.isNotEmpty()) { finish(); return@launch }

            if (setRows.isEmpty()) {
                Toast.makeText(this@ExerciseDetailActivity, "Please add at least one set.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val equipment = runCatching { Equipment.valueOf(equipmentName) }.getOrElse { Equipment.BARBELL }
            val notes = etNotes.text?.toString()?.takeIf { it.isNotBlank() }

            var firstPr: PrCelebrationEvent? = null

            setRows.forEachIndexed { idx, row ->
                val weight = row.etWeight.text.toString().toDoubleOrNull() ?: 0.0
                val reps   = targetReps ?: row.etReps.text.toString().toIntOrNull() ?: 0
                val success = when (row.rgResult.checkedRadioButtonId) {
                    R.id.rbSuccess -> true
                    R.id.rbFail -> false
                    else -> false
                }

                // preview PR before logging (best effort)
                val preview = withContext(Dispatchers.IO) {
                    repo.previewPrEvent(exerciseId, equipment, reps, weight)
                }
                if (firstPr == null && preview != null) firstPr = preview

                // perform log
                withContext(Dispatchers.IO) {
                    repo.logStrengthSet(
                        sessionId = sessionId,
                        exerciseId = exerciseId,
                        equipment = equipment,
                        setNumber = idx + 1,
                        reps = reps,
                        weight = weight,
                        rpe = null,
                        success = success,
                        notes = notes
                    )
                }

                // start rest between sets
                if (idx < setRows.lastIndex) {
                    val base = getBaseRestMs()
                    repo.startRestTimer(sessionId, exerciseId, base)
                    resetBeepTracking()
                    forceShowBanner(base, base, isRunning = true)
                }
            }

            if (firstPr != null) {
                showCenteredToast("🎉 PR hit! New e1RM ${String.format(Locale.UK, "%.1f kg", firstPr!!.newE1rmKg)}")
            } else {
                showCenteredToast("✅ Exercise logged.")
            }
            finish()
        }
    }

    private fun showCenteredToast(msg: String) {
        val t = Toast.makeText(this, msg, Toast.LENGTH_SHORT)
        t.setGravity(Gravity.CENTER, 0, 0)
        t.show()
    }

    // ---- Rest timer banner & beep logic (unchanged) ----

    private fun ensureBannerInflated() {
        if (findViewById<View>(R.id.restTimerContainer) != null) return

        val root = findViewById<ConstraintLayout>(R.id.root) ?: return
        val header = findViewById<View>(R.id.layoutHeader) ?: return
        val scroll = findViewById<View>(R.id.scrollContent)

        val banner = layoutInflater.inflate(R.layout.include_rest_timer_banner, root, false)
        banner.id = R.id.restTimerContainer
        root.addView(banner)

        val set = ConstraintSet().apply { clone(root) }
        set.connect(banner.id, ConstraintSet.TOP, header.id, ConstraintSet.BOTTOM)
        set.connect(banner.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        set.connect(banner.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        set.constrainWidth(banner.id, ConstraintSet.MATCH_CONSTRAINT)
        set.constrainHeight(banner.id, ConstraintSet.WRAP_CONTENT)

        if (scroll != null) {
            set.clear(scroll.id, ConstraintSet.TOP)
            set.connect(scroll.id, ConstraintSet.TOP, banner.id, ConstraintSet.BOTTOM)
        }
        set.applyTo(root)
    }

    private fun bindRestTimerBanner() {
        val container = findViewById<View>(R.id.restTimerContainer) ?: return
        val value = container.findViewById<TextView>(R.id.restTimerValue)
        val toggle = container.findViewById<ImageButton>(R.id.restTimerToggle)
        val clear = container.findViewById<ImageButton>(R.id.restTimerClear)
        val progress = container.findViewById<ProgressBar>(R.id.restTimerProgress)

        lifecycleScope.launch {
            repo.restTimerState.collectLatest { state ->
                handleBeep(state?.remainingMs)
                if (state == null) {
                    container.visibility = View.GONE
                } else {
                    container.visibility = View.VISIBLE
                    value.text = formatMs(state.remainingMs)
                    toggle.setImageResource(if (state.isRunning) R.drawable.ic_pause_24 else R.drawable.ic_play_24)
                    val p = if (state.durationMs > 0)
                        (state.remainingMs * 1000L / state.durationMs).toInt().coerceIn(0, 1000) else 0
                    progress.max = 1000
                    progress.progress = p
                }

                if (state != null) {
                    com.example.safitness.service.RestTimerService.startOrUpdate(this@ExerciseDetailActivity)
                } else {
                    com.example.safitness.service.RestTimerService.clear(this@ExerciseDetailActivity)
                }
            }
        }

        toggle.setOnClickListener {
            val st = repo.restTimerState.value
            if (st?.isRunning == true) repo.pauseRestTimer() else repo.resumeRestTimer()
        }
        clear.setOnClickListener {
            resetBeepTracking()
            repo.clearRestTimer()
        }
    }

    private fun handleBeep(currentRemainingMs: Long?) {
        val prev = lastRemainingMs
        lastRemainingMs = currentRemainingMs
        if (currentRemainingMs == null) return

        val sec = (currentRemainingMs / 1000L)
        if (sec in 1..5 && sec != lastPippedSecond) {
            beeper.countdownPip()
            lastPippedSecond = sec
        }

        val prevSec = prev?.let { it / 1000L } ?: -1L
        if (prevSec > 0 && sec <= 0) {
            beeper.finalBuzz()
            resetBeepTracking()
        }
    }

    private fun resetBeepTracking() {
        lastPippedSecond = -1L
        lastRemainingMs = null
    }

    private fun formatMs(ms: Long): String {
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format("%02d:%02d", m, s)
    }
    private fun forceShowBanner(remainingMs: Long, durationMs: Long, isRunning: Boolean) {
        val container = findViewById<View>(R.id.restTimerContainer) ?: return
        val value = container.findViewById<TextView>(R.id.restTimerValue)
        val toggle = container.findViewById<ImageButton>(R.id.restTimerToggle)
        val progress = container.findViewById<ProgressBar>(R.id.restTimerProgress)

        container.visibility = View.VISIBLE
        value.text = formatMs(remainingMs)
        toggle.setImageResource(if (isRunning) R.drawable.ic_pause_24 else R.drawable.ic_play_24)

        progress.max = 1000
        val p = if (durationMs > 0)
            (remainingMs * 1000L / durationMs).toInt().coerceIn(0, 1000)
        else 0
        progress.progress = p

        container.bringToFront()
    }

}
