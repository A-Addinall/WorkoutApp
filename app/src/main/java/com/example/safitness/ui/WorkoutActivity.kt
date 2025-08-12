package com.example.safitness.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.safitness.R
import com.example.safitness.core.Equipment
import com.example.safitness.core.Modality
import com.example.safitness.data.dao.ExerciseWithSelection
import com.example.safitness.data.repo.Repos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class WorkoutActivity : AppCompatActivity() {

    private val vm: WorkoutViewModel by viewModels {
        WorkoutViewModelFactory(Repos.workoutRepository(this))
    }

    private var sessionId: Long = 0L
    private lateinit var tvWorkoutTitle: TextView
    private lateinit var layoutExercises: LinearLayout

    private var dayIndex: Int = 1
    private var workoutName: String = "Workout"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        dayIndex = intent.getIntExtra("DAY_INDEX", 1).coerceIn(1, 5)
        workoutName = intent.getStringExtra("WORKOUT_NAME") ?: "Day $dayIndex"

        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle)
        layoutExercises = findViewById(R.id.layoutExercises)
        tvWorkoutTitle.text = workoutName

        // start a session for this day (strength sets will attach to it)
        lifecycleScope.launch(Dispatchers.IO) {
            if (sessionId == 0L) {
                sessionId = Repos.workoutRepository(this@WorkoutActivity).startSession(dayIndex)
            }
        }

        vm.setDay(dayIndex)
        vm.programForDay.observe(this) { render(it) }

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
    }

    private fun render(items: List<ExerciseWithSelection>) {
        layoutExercises.removeAllViews()

        val metcon = items.filter { it.exercise.modality == Modality.METCON }
        val nonMetcon = items - metcon

        val required = nonMetcon.filter { it.required }
        val optional = nonMetcon - required

        if (required.isNotEmpty()) {
            addSection("Required")
            required.forEach { addExerciseCard(it) }
        }
        if (optional.isNotEmpty()) {
            addSection("Optional")
            optional.forEach { addExerciseCard(it) }
        }

        // Metcon card (single entry that lists the day’s metcon movements)
        if (metcon.isNotEmpty()) addMetconCard(metcon)

        if (items.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No exercises planned for Day $dayIndex.\nOpen the Library to add some."
                textSize = 16f
                setPadding(24, 24, 24, 24)
            }
            layoutExercises.addView(empty)
        }
    }

    private fun addSection(title: String) {
        val t = TextView(this).apply {
            text = title
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(16, 24, 16, 8)
        }
        layoutExercises.addView(t)
    }

    private fun addExerciseCard(item: ExerciseWithSelection) {
        val card = layoutInflater.inflate(R.layout.item_exercise_card, layoutExercises, false)

        card.findViewById<TextView>(R.id.tvExerciseName).text = item.exercise.name
        card.findViewById<TextView>(R.id.tvRepRange).text =
            item.targetReps?.let { "Reps: $it" } ?: "Reps: --"

        val lastWeight = runBlocking {
            Repos.workoutRepository(this@WorkoutActivity).getLastSuccessfulWeight(
                exerciseId = item.exercise.id,
                equipment = item.preferredEquipment ?: item.exercise.primaryEquipment ?: Equipment.BARBELL,
                reps = item.targetReps
            )
        }
        card.findViewById<TextView>(R.id.tvLastWeight).text =
            if (lastWeight != null) "Last: ${lastWeight}kg" else "Last: --kg"

        card.setOnClickListener {
            startActivity(Intent(this, ExerciseDetailActivity::class.java).apply {
                putExtra("SESSION_ID", sessionId)
                putExtra("EXERCISE_ID", item.exercise.id)
                putExtra("EXERCISE_NAME", item.exercise.name)
                putExtra("EQUIPMENT", (item.preferredEquipment ?: item.exercise.primaryEquipment)?.name)
                putExtra("TARGET_REPS", item.targetReps)
            })
        }
        layoutExercises.addView(card)
    }

    private fun addMetconCard(items: List<ExerciseWithSelection>) {
        addSection("Metcon")

        val card = layoutInflater.inflate(R.layout.item_metcon_card, layoutExercises, false)
        val listContainer = card.findViewById<LinearLayout>(R.id.layoutMetconExercises)
        val tvLastTime = card.findViewById<TextView>(R.id.tvLastTime)

        // list the metcon movements
        items.forEach { item ->
            val row = layoutInflater.inflate(R.layout.item_metcon_exercise, listContainer, false)
            row.findViewById<TextView>(R.id.tvExerciseName).text = item.exercise.name
            row.findViewById<TextView>(R.id.tvRepRange).text =
                item.targetReps?.let { "$it reps" } ?: "As prescribed"
            listContainer.addView(row)
        }

        // observe last time + tag for this day
        vm.lastMetcon.observe(this) { sum ->
            tvLastTime.text = if (sum != null && sum.timeSeconds > 0) {
                val m = sum.timeSeconds / 60
                val s = sum.timeSeconds % 60
                val tag = sum.metconResult?.name ?: ""
                if (tag.isNotEmpty()) "Last time: ${m}m ${s}s ($tag)" else "Last time: ${m}m ${s}s"
            } else {
                "No previous time"
            }
        }

        // tap → metcon screen
        card.setOnClickListener {
            startActivity(Intent(this, MetconActivity::class.java).apply {
                putExtra("DAY_INDEX", dayIndex)
                putExtra("WORKOUT_NAME", workoutName)
            })
        }

        layoutExercises.addView(card)
    }
}
