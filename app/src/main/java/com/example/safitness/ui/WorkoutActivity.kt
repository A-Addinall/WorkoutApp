package com.example.safitness.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Parcelable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import com.example.safitness.R
import com.example.safitness.core.Modality
import com.example.safitness.data.dao.ExerciseWithSelection
import com.example.safitness.data.dao.SelectionWithPlanAndComponents
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.DayEngineSkillEntity
import com.example.safitness.data.repo.Repos
import com.example.safitness.ui.engine.AddEngineSkillDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkoutActivity : AppCompatActivity(), AddEngineSkillDialog.OnAdded {

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
    private var lastDayEngineSkills: List<DayEngineSkillEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        // Day + title from intent
        dayIndex = intent.getIntExtra("DAY_INDEX", 1).coerceIn(1, 5)
        workoutName = intent.getStringExtra("WORKOUT_NAME") ?: "Day $dayIndex"

        // Bind header
        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle)
        layoutExercises = findViewById(R.id.layoutExercises)
        tvWorkoutTitle.text = workoutName
        findViewById<ImageView?>(R.id.ivBack)?.setOnClickListener { finish() }

        // Ensure a session exists (dev)
        lifecycleScope.launch(Dispatchers.IO) {
            if (sessionId == 0L) {
                sessionId = Repos.workoutRepository(this@WorkoutActivity).startSession(dayIndex)
            }
        }

        // Observe program + metcons
        vm.programForDay.observe(this) { items ->
            lastProgramItems = items ?: emptyList()
            rebuildWorkoutUi()
        }
        vm.metconsForDay.observe(this) { selections ->
            lastMetconSelections = selections ?: emptyList()
            rebuildWorkoutUi()
        }

        // Set the current day and load Engine/Skill day items
        vm.setDay(dayIndex)
        loadEngineSkillItems()
    }

    /* ---------------- Menu (Engine & Skills entry + Add-to-day) ---------------- */

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_workout, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_engine_skills -> {
                startActivity(
                    Intent(
                        this,
                        com.example.safitness.ui.engine.EngineSkillsActivity::class.java
                    )
                )
                true
            }
            R.id.action_add_engine_skill -> {
                AddEngineSkillDialog.newInstance(1, dayIndex)
                    .show(supportFragmentManager, "addEngineSkill")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /* ---------------- Load Engine/Skill day attachments ---------------- */

    private fun loadEngineSkillItems() {
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                AppDatabase.get(this@WorkoutActivity)
                    .dayEngineSkillDao()
                    .forDay(1, dayIndex) // dev: week 1
            }
            lastDayEngineSkills = items
            rebuildWorkoutUi()
        }
    }

    override fun onAdded() {
        // Callback from AddEngineSkillDialog
        loadEngineSkillItems()
    }

    /* ---------------- Build the screen ---------------- */

    private fun rebuildWorkoutUi() {
        layoutExercises.removeAllViews()

        // Strength (non-METCON program items)
        val strength = lastProgramItems.filter { it.exercise.modality != Modality.METCON }
        if (strength.isNotEmpty()) {
            addSectionHeader("Strength")
            strength.forEach { addStrengthCard(it) }
        }

        // Metcons (selected metcon plans for the day)
        if (lastMetconSelections.isNotEmpty()) {
            addSectionHeader("Metcon")
            addMetconPlanCards(lastMetconSelections)
        }

        // Engine & Skills (from day_engine_skill)
        if (lastDayEngineSkills.isNotEmpty()) {
            addSectionHeader("Engine & Skills")
            lastDayEngineSkills.forEach { addEngineSkillCard(it) }
        }

        if (strength.isEmpty() && lastMetconSelections.isEmpty() && lastDayEngineSkills.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "No programmed work for today."
                textSize = 16f
                setPadding(24)
            }
            layoutExercises.addView(emptyView)
        }
    }

    private fun addSectionHeader(title: String) {
        val tv = TextView(this).apply {
            text = title
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(8, 16, 8, 8)
        }
        layoutExercises.addView(tv)
    }

    /* ---------------- Strength cards ---------------- */

    /** Renders one Strength item as a simple card. */
    private fun addStrengthCard(item: ExerciseWithSelection) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_metcon_card)
            setPadding(16)
            isClickable = true
            isFocusable = true
            foreground = ContextCompat.getDrawable(
                this@WorkoutActivity,
                android.R.drawable.list_selector_background
            )
        }

        val title = TextView(this).apply {
            text = item.exercise.name
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
        }

        val reps = item.targetReps
        val meta = TextView(this).apply {
            textSize = 14f
            text = reps?.let { "Target: ${it} reps" } ?: ""
        }

        card.addView(title)
        if (reps != null && reps > 0) card.addView(meta)

        card.setOnClickListener {
            val equip = (item.preferredEquipment ?: item.exercise.primaryEquipment).name
            startActivity(Intent(this, ExerciseDetailActivity::class.java).apply {
                putExtra("SESSION_ID", sessionId)
                putExtra("EXERCISE_ID", item.exercise.id)
                putExtra("EXERCISE_NAME", item.exercise.name)
                putExtra("EQUIPMENT", equip)
                putExtra("TARGET_REPS", reps ?: -1)
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

    /* ---------------- Metcon cards ---------------- */

    /** Renders all selected Metcon plans as cards with their components listed. */
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

            // If you have a VM method for "last result", leave as-is; otherwise it will just show empty or your layout's default.
            vm.lastMetconForPlan(plan.id).observe(this) { last ->
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
                    putExtra("DAY_INDEX", dayIndex)
                    putExtra("WORKOUT_NAME", workoutName)
                    putExtra("PLAN_ID", plan.id)
                    putExtra("DURATION_MINUTES", plan.durationMinutes)
                }
                startActivity(intent)
            }

            layoutExercises.addView(card)
        }
    }

    /* ---------------- Engine/Skill cards ---------------- */

    /** Simple card for each Engine/Skill day item. Tap opens quick log sheet. */
    private fun addEngineSkillCard(item: DayEngineSkillEntity) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_metcon_card)
            setPadding(16)
            isClickable = true
            isFocusable = true
            foreground = ContextCompat.getDrawable(
                this@WorkoutActivity,
                android.R.drawable.list_selector_background
            )
        }

        val title = TextView(this).apply {
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
        }
        val meta = TextView(this).apply { textSize = 14f }

        if (item.kind == "ENGINE") {
            title.text = "${item.engineMode} — ${item.engineIntent}"
            meta.text = when (item.engineIntent) {
                "FOR_TIME"     -> "Target: ${item.programDistanceMeters ?: 0} m"
                "FOR_DISTANCE" -> "Target: ${item.programDurationSeconds ?: 0} s"
                "FOR_CALORIES" -> "Target: ${item.programDurationSeconds ?: 0} s, ${item.programTargetCalories ?: 0} cal"
                else -> ""
            }
            card.setOnClickListener {
                com.example.safitness.ui.engine.EngineQuickLogSheet()
                    .show(supportFragmentManager, "engineQuickLog")
            }
        } else {
            title.text = "${item.skill} — ${item.skillTestType}"
            meta.text = when (item.skillTestType) {
                "MAX_REPS_UNBROKEN" -> "Target: unbroken set"
                "FOR_TIME_REPS"     -> "Target: ${item.targetReps ?: 0} reps in ${item.targetDurationSeconds ?: 0}s"
                "MAX_HOLD_SECONDS"  -> "Target: max hold"
                "ATTEMPTS"          -> "Target: attempts"
                else -> ""
            }
            card.setOnClickListener {
                com.example.safitness.ui.engine.SkillQuickLogSheet()
                    .show(supportFragmentManager, "skillQuickLog")
            }
        }

        card.addView(title)
        if (meta.text.isNotBlank()) card.addView(meta)

        layoutExercises.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12 }
        )
    }
}
