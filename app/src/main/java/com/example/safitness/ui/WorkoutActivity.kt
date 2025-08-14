// app/src/main/java/com/example/safitness/ui/WorkoutActivity.kt
package com.example.safitness.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
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

    // Caches used to rebuild UI when LiveData updates arrive
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

        // Optional back button if present in the header
        findViewById<ImageView?>(R.id.ivBack)?.setOnClickListener { finish() }

        // Start a session for this day (strength sets will attach to it)
        lifecycleScope.launch(Dispatchers.IO) {
            if (sessionId == 0L) {
                sessionId = Repos.workoutRepository(this@WorkoutActivity).startSession(dayIndex)
            }
        }

        // Observe strength program for the day
        vm.programForDay.observe(this) { items ->
            lastProgramItems = items ?: emptyList()
            rebuildWorkoutUi()
        }

        // Observe selected Metcon plans for the day
        vm.metconsForDay.observe(this) { selections ->
            lastMetconSelections = selections ?: emptyList()
            rebuildWorkoutUi()
        }

        // Ensure VM knows which day we're on (also loads lastMetconSeconds in the VM)
        vm.setDay(dayIndex)
    }

    /** Rebuilds the day screen: Required / Optional strength, then Metcon card(s). */
    private fun rebuildWorkoutUi() {
        layoutExercises.removeAllViews()

        // Split program into strength vs legacy metcon exercises
        val metconExercises = lastProgramItems.filter { it.exercise.modality == Modality.METCON }
        val nonMetcon = lastProgramItems - metconExercises

        val required = nonMetcon.filter { it.required }
        val optional = nonMetcon - required

        if (required.isNotEmpty()) {
            addSectionHeader("Required")
            required.forEach { addExerciseCard(it) }
        }
        if (optional.isNotEmpty()) {
            addSectionHeader("Optional")
            optional.forEach { addExerciseCard(it) }
        }

        // NEW: One pretty card per selected Metcon plan (if any). Fallback to legacy metcon-exercise card.
        if (lastMetconSelections.isNotEmpty()) {
            addMetconPlanCards(lastMetconSelections)
        } else if (metconExercises.isNotEmpty()) {
            addLegacyMetconCard(metconExercises)
        }

        if (required.isEmpty() && optional.isEmpty() &&
            metconExercises.isEmpty() && lastMetconSelections.isEmpty()
        ) {
            val emptyView = TextView(this).apply {
                text = "No programmed work for today."
                setTextColor(Color.parseColor("#999999"))
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

    private fun addExerciseCard(item: ExerciseWithSelection) {
        val card = androidx.cardview.widget.CardView(this).apply {
            radius = 8f
            cardElevation = 2f
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16)
                setBackgroundColor(resources.getColor(android.R.color.white))
            }
            val title = TextView(context).apply {
                text = item.exercise.name
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
            }
            val meta = TextView(context).apply {
                val typeName = item.exercise.workoutType.name
                val equipName = (item.preferredEquipment ?: item.exercise.primaryEquipment).name
                val repsText = item.targetReps?.let { " • target ${it} reps" } ?: ""
                text = "$typeName • $equipName$repsText"
                textSize = 14f
                setTextColor(Color.parseColor("#666666"))
            }
            container.addView(title)
            container.addView(meta)
            addView(container)
        }

        card.setOnClickListener {
            val equip = (item.preferredEquipment ?: item.exercise.primaryEquipment).name
            val intent = Intent(this, ExerciseDetailActivity::class.java).apply {
                putExtra("SESSION_ID", sessionId)
                putExtra("EXERCISE_ID", item.exercise.id)
                putExtra("EXERCISE_NAME", item.exercise.name)
                putExtra("EQUIPMENT", equip)
                putExtra("TARGET_REPS", item.targetReps ?: -1)
            }
            startActivity(intent)
        }

        layoutExercises.addView(card)
    }

    /* ----------------------------- Metcon (legacy) ---------------------------- */

    private fun addLegacyMetconCard(items: List<ExerciseWithSelection>) {
        // If you still have res/layout/item_metcon_card.xml, inflate it; otherwise use a simple fallback.
        val resId = resources.getIdentifier("item_metcon_card", "layout", packageName)
        if (resId != 0) {
            val card = layoutInflater.inflate(resId, layoutExercises, false)

            // Set title if present
            val titleId = resources.getIdentifier("tvMetconTitle", "id", packageName)
            (card.findViewById<View?>(titleId) as? TextView)?.text = "Metcon"

            // Add bullet list into container if present
            val containerId = resources.getIdentifier("layoutMetconExercises", "id", packageName)
            val container = card.findViewById<LinearLayout?>(containerId)
            container?.removeAllViews()
            items.forEach { ex ->
                val row = TextView(this).apply {
                    text = "• ${ex.exercise.name}"
                    textSize = 16f
                    setPadding(0, 4, 0, 4)
                }
                container?.addView(row)
            }

            // Day-level "last time" (your current data model)
            val lastId = resources.getIdentifier("tvMetconLastTime", "id", packageName)
            val tvLast = card.findViewById<TextView?>(lastId)
            vm.lastMetconSeconds.observe(this) { sec ->
                tvLast?.text = if (sec != null && sec > 0) {
                    val m = sec / 60; val s = sec % 60
                    "Last time: ${m}m ${s}s"
                } else "No previous time"
            }

            card.setOnClickListener {
                startActivity(Intent(this, MetconActivity::class.java).apply {
                    putExtra("DAY_INDEX", dayIndex)
                    putExtra("WORKOUT_NAME", workoutName)
                })
            }

            layoutExercises.addView(card)
        } else {
            // Programmatic fallback
            val cv = androidx.cardview.widget.CardView(this).apply {
                radius = 8f; cardElevation = 2f
                val l = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL; setPadding(16)
                }
                val t = TextView(context).apply {
                    text = "Metcon"; setTypeface(typeface, Typeface.BOLD); textSize = 16f
                }
                l.addView(t)
                items.forEach { sel ->
                    val row = TextView(context).apply { text = "• ${sel.exercise.name}" }
                    l.addView(row)
                }
                addView(l)
            }
            layoutExercises.addView(cv)
        }
    }

    /* -------------------------- Metcon (new: per-plan) ------------------------- */

    /** One pretty card per selected Metcon plan (new model). */
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

            // Title
            tvTitle.text = plan.title

            // Components as bullets
            compsContainer.removeAllViews()
            components.forEach { comp ->
                compsContainer.addView(TextView(this).apply {
                    text = "• ${comp.text}"
                    textSize = 16f
                    setPadding(0, 4, 0, 4)
                })
            }

            // Day-level last time (current schema is day-scoped, not per-plan)
            vm.lastMetconSeconds.observe(this) { sec ->
                tvLast.text = if (sec != null && sec > 0) {
                    val m = sec / 60; val s = sec % 60
                    "Last time: ${m}m ${s}s"
                } else "No previous time"
            }

            // Tap → open Metcon timer scoped to THIS plan
            card.setOnClickListener {
                startActivity(Intent(this, MetconActivity::class.java).apply {
                    putExtra("DAY_INDEX", dayIndex)
                    putExtra("WORKOUT_NAME", workoutName)
                    putExtra("PLAN_ID", plan.id) // CRITICAL
                })
            }

            layoutExercises.addView(card)
        }
    }
}
