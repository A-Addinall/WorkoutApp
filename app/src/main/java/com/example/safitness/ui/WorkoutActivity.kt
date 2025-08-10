// app/src/main/java/com/example/safitness/ui/WorkoutActivity.kt
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
import com.example.safitness.data.dao.ExerciseWithSelection
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
    private var workoutName: String = "Workout"
    private var sessionId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)

        dayIndex = intent.getIntExtra("DAY_INDEX", 1).coerceIn(1, 5)
        workoutName = intent.getStringExtra("WORKOUT_NAME") ?: "Day $dayIndex"

        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle)
        layoutExercises = findViewById(R.id.layoutExercises)
        tvWorkoutTitle.text = workoutName

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        // Start a session on first load
        lifecycleScope.launch(Dispatchers.IO) {
            if (sessionId == 0L) {
                sessionId = Repos.workoutRepository(this@WorkoutActivity).startSession(dayIndex)
            }
        }

        vm.setDay(dayIndex)
        vm.programForDay.observe(this) { render(it) }
    }

    private fun render(items: List<ExerciseWithSelection>) {
        layoutExercises.removeAllViews()

        val required = items.filter { it.required }
        val optional = items - required

        if (required.isNotEmpty()) {
            addSection("Required")
            required.forEach { addExerciseCard(it) }
        }
        if (optional.isNotEmpty()) {
            addSection("Optional")
            optional.forEach { addExerciseCard(it) }
        }
        if (items.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No exercises planned for Day $dayIndex."
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
        val cardView = layoutInflater.inflate(R.layout.item_exercise_card, layoutExercises, false)

        val tvExerciseName = cardView.findViewById<TextView>(R.id.tvExerciseName)
        val tvRepRange = cardView.findViewById<TextView>(R.id.tvRepRange)
        val tvLastWeight = cardView.findViewById<TextView>(R.id.tvLastWeight)

        tvExerciseName.text = item.exercise.name
        tvRepRange.text = item.targetReps?.toString() ?: item.exercise.modality.name
        tvLastWeight.text = "" // (optional) fill from repository if needed

        val equip: Equipment = item.preferredEquipment ?: item.exercise.primaryEquipment
        cardView.setOnClickListener {
            val i = Intent(this, ExerciseDetailActivity::class.java).apply {
                putExtra("SESSION_ID", sessionId)
                putExtra("EXERCISE_ID", item.exercise.id)
                putExtra("EXERCISE_NAME", item.exercise.name)
                putExtra("EQUIPMENT", equip.name)
                putExtra("TARGET_REPS", item.targetReps ?: 8)
            }
            startActivity(i)
        }

        layoutExercises.addView(cardView)
    }
}
