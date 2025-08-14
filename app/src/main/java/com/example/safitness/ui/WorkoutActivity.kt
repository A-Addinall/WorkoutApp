package com.example.safitness.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import com.example.safitness.R
import com.example.safitness.core.Modality
import com.example.safitness.data.dao.ExerciseWithSelection
import com.example.safitness.data.dao.SelectionWithPlanAndComponents
import com.example.safitness.data.repo.Repos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        dayIndex = intent.getIntExtra("DAY_INDEX", 1).coerceIn(1, 5)
        workoutName = intent.getStringExtra("WORKOUT_NAME") ?: "Day $dayIndex"

        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle)
        layoutExercises = findViewById(R.id.layoutExercises)
        tvWorkoutTitle.text = workoutName

        findViewById<ImageView?>(R.id.ivBack)?.setOnClickListener { finish() }

        // Start a session for this day (strength logs attach to it)
        lifecycleScope.launch(Dispatchers.IO) {
            if (sessionId == 0L) {
                sessionId = Repos.workoutRepository(this@WorkoutActivity).startSession(dayIndex)
            }
        }

        vm.programForDay.observe(this) { items ->
            lastProgramItems = items ?: emptyList()
            rebuildWorkoutUi()
        }
        vm.metconsForDay.observe(this) { selections ->
            lastMetconSelections = selections ?: emptyList()
            rebuildWorkoutUi()
        }

        vm.setDay(dayIndex)
    }

    /** Rebuilds the day screen: Strength (all items), then Metcon plan cards. */
    private fun rebuildWorkoutUi() {
        layoutExercises.removeAllViews()

        // Strength = everything that is NOT metcon
        val strength = lastProgramItems.filter { it.exercise.modality != Modality.METCON }

        if (strength.isNotEmpty()) {
            addSectionHeader("Strength")
            strength.forEach { addStrengthCard(it) }
        }

        // One card per selected Metcon plan (plan-based only; legacy removed)
        if (lastMetconSelections.isNotEmpty()) {
            addMetconPlanCards(lastMetconSelections)
        }

        if (strength.isEmpty() && lastMetconSelections.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "No programmed work for today."
                textSize = 16f
                setPadding(24)
            }
            layoutExercises.addView(emptyView)
        }
    }

    /* ------------------------------ Strength UI ------------------------------ */

    private fun addSectionHeader(title: String) {
        val tv = TextView(this).apply {
            text = title
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(8, 16, 8, 8)
        }
        layoutExercises.addView(tv)
    }

    /** Strength card styled like metcon, with 20sp title and reps-only meta. */
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
            textSize = 20f                // match metcon title size
            setTypeface(typeface, Typeface.BOLD)
        }

        // Show only reps from the selection added in the library (targetReps)
        val reps = item.targetReps
        val meta = TextView(this).apply {
            textSize = 14f
            text = reps?.let { "Target: ${it} reps" } ?: ""
        }

        card.addView(title)
        if (reps != null && reps > 0) card.addView(meta)

        card.setOnClickListener {
            // We still pass equipment etc. to ExerciseDetailActivity to log sets,
            // but we no longer show those details on the card itself.
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

    /* -------------------------- Metcon (plan-based) -------------------------- */

    /** One pretty card per selected Metcon plan (new model only). */
    private fun addMetconPlanCards(
        selections: List<SelectionWithPlanAndComponents>
    ) {
        if (selections.isEmpty()) return

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

            vm.lastMetconSeconds.observe(this) { sec ->
                tvLast.text = if (sec != null && sec > 0) {
                    val m = sec / 60; val s = sec % 60
                    "Last time: ${m}m ${s}s"
                } else "No previous time"
            }

            card.setOnClickListener {
                startActivity(Intent(this, MetconActivity::class.java).apply {
                    putExtra("DAY_INDEX", dayIndex)
                    putExtra("WORKOUT_NAME", workoutName)
                    putExtra("PLAN_ID", plan.id)
                })
            }

            layoutExercises.addView(card)
        }
    }
}
