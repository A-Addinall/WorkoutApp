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
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.example.safitness.R
import com.example.safitness.core.Equipment
import com.example.safitness.core.Modality
import com.example.safitness.data.dao.ExerciseWithSelection
import com.example.safitness.data.dao.MetconDao
import com.example.safitness.data.repo.Repos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.safitness.data.dao.SelectionWithPlanAndComponents

class WorkoutActivity : AppCompatActivity() {

    private val vm: WorkoutViewModel by viewModels {
        WorkoutViewModelFactory(Repos.workoutRepository(this))
    }

    private lateinit var tvWorkoutTitle: TextView
    private lateinit var layoutExercises: LinearLayout

    private var dayIndex: Int = 1
    private var workoutName: String = ""
    private var sessionId: Long = 0L

    // cache to rebuild the UI coherently
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

        // optional back button if present in the header
        findViewById<ImageView?>(R.id.ivBack)?.setOnClickListener { finish() }

        // start a session for this day (strength sets will attach to it)
        lifecycleScope.launch(Dispatchers.IO) {
            if (sessionId == 0L) {
                sessionId = Repos.workoutRepository(this@WorkoutActivity).startSession(dayIndex)
            }
        }

        // observe strength program for the day
        vm.programForDay.observe(this) { items ->
            lastProgramItems = items ?: emptyList()
            rebuildWorkoutUi()
        }

        // observe selected Metcon plans for the day
        vm.metconsForDay.observe(this) { selections ->
            lastMetconSelections = selections ?: emptyList()
            rebuildWorkoutUi()
        }

        // ensure VM knows which day we're on (also loads lastMetconSeconds)
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

        // Prefer new Metcon Plans card; if none selected, fall back to legacy metcon-exercise card
        if (lastMetconSelections.isNotEmpty()) {
            addOrUpdateMetconPlansCard(lastMetconSelections)
        } else if (metconExercises.isNotEmpty()) {
            addLegacyMetconCard(metconExercises)
        }

        if (required.isEmpty() && optional.isEmpty() &&
            metconExercises.isEmpty() && lastMetconSelections.isEmpty()
        ) {
            addSectionHeader("Empty")
        }
    }

    /* -------------------- UI helpers -------------------- */

    private fun addSectionHeader(title: String) {
        val tv = TextView(this).apply {
            text = title
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(8)
        }
        layoutExercises.addView(tv)
    }

    /** Minimal, dependency-free strength card; launches ExerciseDetailActivity on tap. */
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
                text = buildString {
                    append(item.exercise.workoutType.name)
                    append(" • ")
                    append((item.preferredEquipment ?: item.exercise.primaryEquipment).name)
                    item.targetReps?.let { append(" • Target: ${it} reps") }
                }
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

    /**
     * Pretty Metcon Plans card (new model) – matches your mock.
     * Requires layout: res/layout/item_metcon_plan_card.xml (as provided earlier).
     */
    private fun addOrUpdateMetconPlansCard(
        selections: List<SelectionWithPlanAndComponents>
    ) {
        // remove prior instance (avoid duplicates)
        for (i in 0 until layoutExercises.childCount) {
            val child = layoutExercises.getChildAt(i)
            if (child.tag == "metcon-plans") {
                layoutExercises.removeViewAt(i)
                break
            }
        }
        if (selections.isEmpty()) return

        val card = layoutInflater.inflate(R.layout.item_metcon_plan_card, layoutExercises, false)
        card.tag = "metcon-plans"

        val tvTitle = card.findViewById<TextView>(R.id.tvPlanCardTitle)
        val compsContainer = card.findViewById<LinearLayout>(R.id.layoutPlanComponents)
        val tvLast = card.findViewById<TextView>(R.id.tvPlanLastTime)

        // If multiple plans are selected, concatenate titles
        val title = selections.sortedBy { it.selection.displayOrder }
            .joinToString(" • ") { it.planWithComponents.plan.title }
        tvTitle.text = title

        compsContainer.removeAllViews()
        selections.sortedBy { it.selection.displayOrder }.forEach { sel ->
            val plan = sel.planWithComponents.plan
            val components = sel.planWithComponents.components.sortedBy { it.orderInPlan }

            // Optional subheading per plan if there are multiple
            if (selections.size > 1) {
                val sub = TextView(this).apply {
                    text = plan.title
                    setTextColor(Color.WHITE)
                    setTypeface(typeface, Typeface.BOLD)
                    textSize = 16f
                    setPadding(0, if (compsContainer.childCount == 0) 0 else 12, 0, 4)
                }
                compsContainer.addView(sub)
            }

            components.forEach { comp ->
                val row = TextView(this).apply {
                    text = "• ${comp.text}"
                    setTextColor(Color.WHITE)
                    textSize = 16f
                    setPadding(0, 4, 0, 4)
                }
                compsContainer.addView(row)
            }

            // spacer between plans
            if (selections.size > 1) {
                val spacer = View(this)
                spacer.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 12
                )
                compsContainer.addView(spacer)
            }
        }

        // last metcon time text
        vm.lastMetconSeconds.observe(this) { sec ->
            tvLast.text = if (sec != null && sec > 0) {
                val m = sec / 60; val s = sec % 60
                "Last time: ${m}m ${s}s"
            } else {
                "No previous time"
            }
        }

        // tap → open Metcon timer
        card.setOnClickListener {
            startActivity(Intent(this, MetconActivity::class.java).apply {
                putExtra("DAY_INDEX", dayIndex)
                putExtra("WORKOUT_NAME", workoutName)
            })
        }

        layoutExercises.addView(card)
    }

    /** Legacy metcon (old model: exercises flagged as Modality.METCON). */
    private fun addLegacyMetconCard(items: List<ExerciseWithSelection>) {
        // If you still have res/layout/item_metcon_card.xml, we can inflate it; otherwise use a simple fallback.
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
                    textSize = 14f
                }
                container?.addView(row)
            }

            // Last-time label if present
            val lastId = resources.getIdentifier("tvLastTime", "id", packageName)
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
            // Very small programmatic fallback
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
}
