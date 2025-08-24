package com.example.safitness.ui

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.example.safitness.R
import com.example.safitness.core.Modality
import com.example.safitness.data.dao.ExerciseWithSelection
import com.example.safitness.data.dao.SelectionWithPlanAndComponents
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.EnginePlanEntity
import com.example.safitness.data.entities.SkillPlanEntity
import com.example.safitness.data.repo.Repos
import com.example.safitness.ui.engine.EngineAccumulationActivity
import com.example.safitness.ui.engine.EngineEmomActivity
import com.example.safitness.ui.engine.EngineForTimeActivity
import com.example.safitness.ui.engine.SkillAmrapActivity
import com.example.safitness.ui.engine.SkillEmomActivity
import com.example.safitness.ui.engine.SkillForTimeActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.safitness.ui.MainActivity.Companion.EXTRA_DATE_EPOCH_DAY
import com.example.safitness.ui.MainActivity.Companion.EXTRA_DAY_INDEX
import com.example.safitness.ui.MainActivity.Companion.EXTRA_WORKOUT_NAME

class WorkoutActivity : AppCompatActivity() {

    private val vm: WorkoutViewModel by viewModels {
        WorkoutViewModelFactory(Repos.workoutRepository(this))
    }

    private lateinit var tvWorkoutTitle: TextView
    private lateinit var layoutExercises: LinearLayout

    private var dayIndex: Int = 1
    private var workoutName: String = ""
    private var sessionId: Long = 0L

    private var lastProgramItems: List<ExerciseWithSelection> = emptyList()
    private var lastMetconSelections: List<SelectionWithPlanAndComponents> = emptyList()
    private var lastEnginePlans: List<EnginePlanEntity> = emptyList()
    private var lastSkillPlans: List<SkillPlanEntity> = emptyList()

    // Collapsible flags
    private var collapseStrength = false
    private var collapseMetcon = false
    private var collapseEngine = false
    private var collapseSkills = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        dayIndex = intent.getIntExtra(EXTRA_DAY_INDEX, 1).coerceIn(1, 5)
        workoutName = intent.getStringExtra(EXTRA_WORKOUT_NAME) ?: "Day $dayIndex"

        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle)
        layoutExercises = findViewById(R.id.layoutExercises)

        // ----- Date-aware title -----
        val epochDay: Long = intent.getLongExtra(EXTRA_DATE_EPOCH_DAY, LocalDate.now().toEpochDay())
        val selectedDate: LocalDate = LocalDate.ofEpochDay(epochDay)
        val prettyTitle: String = intent.getStringExtra(EXTRA_WORKOUT_NAME)
            ?: selectedDate.format(DateTimeFormatter.ofPattern("EEE d MMM, yyyy"))
        tvWorkoutTitle.text = prettyTitle
// Open date picker from title or calendar icon
        val openPicker: (Unit) -> Unit = {
            val picker = MaterialDatePicker.Builder
                .datePicker()
                .setTitleText("Select a date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            picker.addOnPositiveButtonClickListener { utcMillis ->
                val selected = Instant.ofEpochMilli(utcMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                openWorkoutForDate(selected)
            }

            picker.show(supportFragmentManager, "date_picker")
        }
        tvWorkoutTitle.setOnClickListener { openPicker(Unit) }
        findViewById<ImageView?>(R.id.ivCalendar)?.setOnClickListener { openPicker(Unit) }

        findViewById<ImageView?>(R.id.ivBack)?.setOnClickListener { finish() }

        // ----- Edit Day button -> Library (legacy still needs a 1..5 day index) -----
        findViewById<com.google.android.material.button.MaterialButton?>(R.id.btnEditDay)?.setOnClickListener {
            val i = Intent(this, ExerciseLibraryActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_DAY_INDEX, dayIndex)
                putExtra(MainActivity.EXTRA_DATE_EPOCH_DAY, epochDay)
            }
            startActivity(i)
        }

        // Start a session (repository records the actual date internally)
        lifecycleScope.launch(Dispatchers.IO) {
            if (sessionId == 0L) {
                val repo = Repos.workoutRepository(this@WorkoutActivity)
                sessionId = repo.startSessionForDate(epochDay)

                // Pre-create the planned sets so they’re visible immediately
                repo.ensureDefaultStrengthSetsForSession(
                    sessionId = sessionId,
                    epochDay = epochDay
                )
            }
        }



        // ---------- DATE-FIRST DATA SOURCES ----------
        // Strength (by date, falls back to legacy weekday if no day plan exists)
        Repos.workoutRepository(this).programForDate(epochDay).asLiveData().observe(this) { items ->
            lastProgramItems = items ?: emptyList()
            rebuildWorkoutUi()
        }

        // Metcons (by date, falls back to legacy selection-by-date)
        Repos.workoutRepository(this).metconsForDate(epochDay).asLiveData().observe(this) { selections ->
            lastMetconSelections = selections ?: emptyList()
            rebuildWorkoutUi()
        }

        // Engine / Skill plan IDs for the date, then load details from their DAOs
        Repos.workoutRepository(this).enginePlanIdsForDate(epochDay).asLiveData()
            .observe(this) { ids ->
                lifecycleScope.launch {
                    lastEnginePlans = fetchEnginePlansByIds(ids ?: emptySet())
                    rebuildWorkoutUi()
                }
            }

        Repos.workoutRepository(this).skillPlanIdsForDate(epochDay).asLiveData()
            .observe(this) { ids ->
                lifecycleScope.launch {
                    lastSkillPlans = fetchSkillPlansByIds(ids ?: emptySet())
                    rebuildWorkoutUi()
                }
            }

        // (No need to call vm.setDay(...) any more; everything is date-first here.)
    }
    /** Re-open this screen for the chosen date (same behaviour as MainActivity.openForDate). */
    private fun openWorkoutForDate(date: java.time.LocalDate) {
        val pretty = date.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM, yyyy"))
        val i = Intent(this, WorkoutActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_DATE_EPOCH_DAY, date.toEpochDay())
            putExtra(MainActivity.EXTRA_WORKOUT_NAME, pretty)
            // Keep sending a benign legacy index so older flows don't break.
            putExtra(MainActivity.EXTRA_DAY_INDEX, 1)
        }
        startActivity(i)
        finish() // close the old date instance
    }

    /** Rebuild the whole list with collapsible sections. */
    private fun rebuildWorkoutUi() {
        layoutExercises.removeAllViews()

        // --- Strength ---
        addSectionHeader("Strength", collapseStrength) {
            collapseStrength = !collapseStrength
            rebuildWorkoutUi()
        }
        if (!collapseStrength) {
            val strength = lastProgramItems.filter { it.exercise.modality != Modality.METCON }
            strength.forEach { addStrengthCard(it) }
            if (strength.isEmpty()) addEmptyLine("No strength programmed.")
        }

        // --- Metcon ---
        addSectionHeader("Metcon", collapseMetcon) {
            collapseMetcon = !collapseMetcon
            rebuildWorkoutUi()
        }
        if (!collapseMetcon) {
            if (lastMetconSelections.isNotEmpty()) addMetconPlanCards(lastMetconSelections)
            else addEmptyLine("No metcon programmed.")
        }

        // --- Engine ---
        addSectionHeader("Engine", collapseEngine) {
            collapseEngine = !collapseEngine
            rebuildWorkoutUi()
        }
        if (!collapseEngine) {
            if (lastEnginePlans.isNotEmpty()) lastEnginePlans.forEach { addEngineWorkoutCard(it) }
            else addEmptyLine("No engine work programmed.")
        }

        // --- Skills ---
        addSectionHeader("Skills", collapseSkills) {
            collapseSkills = !collapseSkills
            rebuildWorkoutUi()
        }
        if (!collapseSkills) {
            if (lastSkillPlans.isNotEmpty()) lastSkillPlans.forEach { addSkillWorkoutCard(it) }
            else addEmptyLine("No skills programmed.")
        }
    }

    /** Section header with chevron and tap-to-toggle. */
    private fun addSectionHeader(title: String, collapsed: Boolean, onToggle: () -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12)
            val tv = TextView(context).apply {
                text = title
                setTypeface(typeface, Typeface.BOLD)
                textSize = 18f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val iv = ImageView(context).apply {
                setImageResource(if (collapsed) R.drawable.ic_chevron_down else R.drawable.ic_chevron_up)
            }
            addView(tv); addView(iv)
            setOnClickListener { onToggle() }
        }
        layoutExercises.addView(row)
    }

    private fun addEmptyLine(message: String) {
        val tv = TextView(this).apply {
            text = message
            setPadding(16)
        }
        layoutExercises.addView(tv)
    }

    /** ---- Strength (your proven-good UI) ---- */
    private fun addStrengthCard(item: ExerciseWithSelection) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_metcon_card)
            setPadding(16)
            isClickable = true
            isFocusable = true
            foreground = getDrawable(android.R.drawable.list_selector_background)
        }

        val title = TextView(this).apply {
            text = item.exercise.name
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
        }

        val sets = item.targetSets
        val reps = item.targetReps
        val rxText = when {
            sets != null && reps != null -> "Target: $sets × $reps"
            reps != null                 -> "Target reps: $reps"
            else                         -> "Target: —"
        }
        val tvRx = TextView(this).apply {
            text = rxText
            textSize = 14f
        }

        card.addView(title)
        if (reps != null && reps > 0) card.addView(tvRx)

        card.setOnClickListener {
            val equip = (item.preferredEquipment ?: item.exercise.primaryEquipment).name
            startActivity(Intent(this, ExerciseDetailActivity::class.java).apply {
                putExtra("SESSION_ID", sessionId)
                putExtra("EXERCISE_ID", item.exercise.id)
                putExtra("EXERCISE_NAME", item.exercise.name)
                putExtra("EQUIPMENT", equip)
                putExtra("TARGET_REPS", reps ?: -1)
                putExtra("TARGET_SETS", item.targetSets)

            })
        }

        layoutExercises.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12 }
        )
    }

    /** ---- Metcon ---- */
    private fun addMetconPlanCards(
        selections: List<SelectionWithPlanAndComponents>
    ) {
        selections.sortedBy { it.selection.displayOrder }.forEach { sel ->
            val card = layoutInflater.inflate(R.layout.item_metcon_plan_card, layoutExercises, false)

            val tvTitle = card.findViewById<TextView>(R.id.tvPlanCardTitle)
            val compsContainer = card.findViewById<LinearLayout>(R.id.layoutPlanComponents)
            val tvLast = card.findViewById<TextView>(R.id.tvPlanLastTime)

            val plan = sel.planWithComponents.plan
            val components = sel.planWithComponents.components.sortedBy { it.orderInPlan }

            tvTitle.text = plan.title

            compsContainer.removeAllViews()
            components.forEach { comp ->
                compsContainer.addView(TextView(this).apply {
                    text = "• ${comp.text}"
                    textSize = 16f
                    setPadding(0, 4, 0, 4)
                })
            }

            // Observe directly from repository to avoid ViewModel dependency changes
            Repos.workoutRepository(this).lastMetconForPlan(plan.id).asLiveData().observe(this) { last ->
                tvLast.text = when (last?.type) {
                    "FOR_TIME" -> {
                        val sec = last.timeSeconds ?: 0
                        if (sec > 0) "Last: ${sec / 60}m ${sec % 60}s (${last.result})"
                        else "No previous result"
                    }
                    "AMRAP" -> {
                        val r = last.rounds ?: 0
                        val er = last.extraReps ?: 0
                        if (r + er > 0) "Last: ${r} rds + ${er} reps (${last.result})"
                        else "No previous result"
                    }
                    "EMOM" -> {
                        val intv = last.intervalsCompleted ?: 0
                        if (intv > 0) "Last: ${intv} intervals (${last.result})"
                        else "No previous result"
                    }
                    else -> "No previous result"
                }
            }

            card.setOnClickListener {
                val title = plan.title.lowercase()
                val intent = when {
                    title.contains("amrap") -> Intent(this, MetconAmrapActivity::class.java)
                    title.contains("emom")  -> Intent(this, MetconEmomActivity::class.java)
                    else                    -> Intent(this, MetconActivity::class.java) // For Time
                }.apply {
                    putExtra(EXTRA_DAY_INDEX, dayIndex)
                    putExtra(EXTRA_WORKOUT_NAME, workoutName)
                    putExtra("PLAN_ID", plan.id)
                    putExtra("DURATION_MINUTES", plan.durationMinutes)
                }
                startActivity(intent)
            }

            layoutExercises.addView(card)
        }
    }

    // ---------------- Engine & Skill workout cards (with "last" via SharedPreferences) ----------------

    private fun addEngineWorkoutCard(plan: EnginePlanEntity) {
        val card = layoutInflater.inflate(R.layout.item_engine_workout_card, layoutExercises, false)
        val tvTitle = card.findViewById<TextView>(R.id.tvPlanCardTitle)
        val tvMeta  = card.findViewById<TextView>(R.id.tvPlanMeta)
        val comps   = card.findViewById<LinearLayout>(R.id.layoutPlanComponents)
        val tvLast  = card.findViewById<TextView>(R.id.tvPlanLastTime)

        tvTitle.text = plan.title
        tvMeta.text = buildEngineMeta(plan)
        tvLast.text = loadLastEngineLabel(this, plan)

        lifecycleScope.launch {
            val rows = withContext(Dispatchers.IO) {
                AppDatabase.get(this@WorkoutActivity).enginePlanDao().getComponents(plan.id)
            }
            comps.removeAllViews()
            rows.sortedBy { it.orderIndex }.forEach { c ->
                val line = c.title.ifBlank { c.description ?: "" }
                comps.addView(TextView(this@WorkoutActivity).apply {
                    text = "• $line"
                    textSize = 16f
                    setPadding(0, 4, 0, 4)
                })
            }
        }

        card.setOnClickListener {
            val titleL = plan.title.lowercase()
            val intent = when {
                titleL.contains("emom") -> Intent(this, EngineEmomActivity::class.java).apply {
                    putExtra(
                        "ENGINE_EMOM_UNIT",
                        if ((plan.programTargetCalories ?: 0) > 0) "CALORIES" else "METERS"
                    )
                }
                plan.intent.equals("FOR_TIME", true) ->
                    Intent(this, EngineForTimeActivity::class.java)
                else ->
                    Intent(this, EngineAccumulationActivity::class.java).apply {
                        putExtra(
                            "ENGINE_TARGET",
                            if ((plan.programTargetCalories ?: 0) > 0) "CALORIES" else "METERS"
                        )
                    }
            }.apply {
                putExtra(EXTRA_DAY_INDEX, dayIndex)
                putExtra(EXTRA_WORKOUT_NAME, workoutName)
                putExtra("PLAN_ID", plan.id)
                putExtra("DURATION_SECONDS", plan.programDurationSeconds ?: 0)
                putExtra("TARGET_METERS", plan.programDistanceMeters ?: 0)
                putExtra("TARGET_CALORIES", plan.programTargetCalories ?: 0)
                putExtra("INTENT", plan.intent)
                putExtra("MODE", plan.mode)
            }
            startActivity(intent)
        }

        layoutExercises.addView(card)
    }

    private fun addSkillWorkoutCard(plan: SkillPlanEntity) {
        val card = layoutInflater.inflate(R.layout.item_skill_workout_card, layoutExercises, false)
        val tvTitle = card.findViewById<TextView>(R.id.tvPlanCardTitle)
        val tvMeta  = card.findViewById<TextView>(R.id.tvPlanMeta)
        val comps   = card.findViewById<LinearLayout>(R.id.layoutPlanComponents)
        val tvLast  = card.findViewById<TextView>(R.id.tvPlanLastTime)

        tvTitle.text = plan.title
        tvMeta.text = when (plan.defaultTestType?.uppercase()) {
            "MAX_HOLD_SECONDS" -> "Max hold • ${plan.targetDurationSeconds ?: 0}s"
            "FOR_TIME_REPS"     -> "For time (reps)"
            "EMOM"              -> "EMOM • ${plan.targetDurationSeconds?.div(60) ?: 0} min"
            "AMRAP"             -> "AMRAP • ${plan.targetDurationSeconds?.div(60) ?: 0} min"
            "ATTEMPTS"          -> "Attempts"
            else                -> plan.description ?: ""
        }
        tvLast.text = loadLastSkillLabel(this, plan)

        lifecycleScope.launch {
            val rows = withContext(Dispatchers.IO) {
                AppDatabase.get(this@WorkoutActivity).skillPlanDao().getComponents(plan.id)
            }
            comps.removeAllViews()
            rows.sortedBy { it.orderIndex }.forEach { c ->
                val line = c.title.ifBlank { c.description ?: "" }
                comps.addView(TextView(this@WorkoutActivity).apply {
                    text = "• $line"
                    textSize = 16f
                    setPadding(0, 4, 0, 4)
                })
            }
        }

        card.setOnClickListener {
            val testType = plan.defaultTestType?.uppercase() ?: ""
            val titleL = plan.title.lowercase()
            val intent = when {
                testType.contains("EMOM") || titleL.contains("emom") ->
                    Intent(this, SkillEmomActivity::class.java)
                testType.contains("AMRAP") || titleL.contains("amrap") ->
                    Intent(this, SkillAmrapActivity::class.java)
                testType.contains("FOR_TIME") || testType.contains("FOR_TIME_REPS") || titleL.contains("for time") ->
                    Intent(this, SkillForTimeActivity::class.java)
                else ->
                    Intent(this, SkillAmrapActivity::class.java)
            }.apply {
                putExtra(EXTRA_DAY_INDEX, dayIndex)
                putExtra(EXTRA_WORKOUT_NAME, workoutName)
                putExtra("PLAN_ID", plan.id)
                putExtra("DURATION_SECONDS", plan.targetDurationSeconds ?: 0)
            }
            startActivity(intent)
        }

        layoutExercises.addView(card)
    }

    // ---------------- Helpers ----------------

    private fun buildEngineMeta(p: EnginePlanEntity): String {
        fun pretty(s: String) = s.replace('_', ' ').lowercase().replaceFirstChar(Char::titlecase)
        val bits = mutableListOf<String>()
        bits += pretty(p.mode)
        when (p.intent) {
            "FOR_TIME" -> { val m = p.programDistanceMeters ?: 0; if (m > 0) bits += "$m m" }
            "FOR_DISTANCE" -> { val sec = p.programDurationSeconds ?: 0; if (sec > 0) bits += "${sec / 60} min" }
            "FOR_CALORIES" -> {
                val c = p.programTargetCalories ?: 0; if (c > 0) bits += "$c cal"
                val sec = p.programDurationSeconds ?: 0; if (sec > 0) bits += "${sec / 60} min"
            }
        }
        bits += pretty(p.intent)
        return bits.joinToString(" • ")
    }

    private suspend fun fetchEnginePlansByIds(ids: Set<Long>): List<EnginePlanEntity> {
        if (ids.isEmpty()) return emptyList()
        val db = AppDatabase.get(this)
        return withContext(Dispatchers.IO) {
            db.enginePlanDao().getPlans().filter { it.id in ids }.sortedBy { it.title }
        }
    }

    private suspend fun fetchSkillPlansByIds(ids: Set<Long>): List<SkillPlanEntity> {
        if (ids.isEmpty()) return emptyList()
        val db = AppDatabase.get(this)
        return withContext(Dispatchers.IO) {
            db.skillPlanDao().getPlans().filter { it.id in ids }.sortedBy { it.title }
        }
    }

    // ---------------- "Last" labels via SharedPreferences ----------------

    private fun prefs(): android.content.SharedPreferences =
        getSharedPreferences("last_results", Context.MODE_PRIVATE)

    private fun loadLastEngineLabel(ctx: Context, p: EnginePlanEntity): String {
        val sp = prefs()
        return when {
            p.title.contains("emom", true) -> {
                val key = "engine_emom_last_${p.id}"
                sp.getString(key, null)?.let { "Last: $it" } ?: "No previous result"
            }
            p.intent.equals("FOR_TIME", true) -> {
                val key = "engine_for_time_last_${p.id}"
                sp.getInt(key, -1).takeIf { it > 0 }?.let {
                    "Last: ${it / 60}m ${it % 60}s"
                } ?: "No previous result"
            }
            else -> { // accumulation (meters or calories)
                val key = "engine_acc_last_${p.id}"
                sp.getString(key, null)?.let { "Last: $it" } ?: "No previous result"
            }
        }
    }

    private fun loadLastSkillLabel(ctx: Context, p: SkillPlanEntity): String {
        val sp = prefs()
        val tt = p.defaultTestType?.uppercase() ?: ""
        return when {
            tt.contains("EMOM") -> {
                val key = "skill_emom_last_${p.id}"
                sp.getString(key, null)?.let { "Last: $it" } ?: "No previous result"
            }
            tt.contains("AMRAP") -> {
                val key = "skill_amrap_last_${p.id}"
                sp.getString(key, null)?.let { "Last: $it" } ?: "No previous result"
            }
            tt.contains("FOR_TIME") || tt.contains("FOR_TIME_REPS") -> {
                val key = "skill_for_time_last_${p.id}"
                sp.getInt(key, -1).takeIf { it > 0 }?.let {
                    "Last: ${it / 60}m ${it % 60}s"
                } ?: "No previous result"
            }
            else -> {
                val key = "skill_attempts_last_${p.id}"
                sp.getString(key, null)?.let { "Last: $it" } ?: "No previous result"
            }
        }
    }
}
