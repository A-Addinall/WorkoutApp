package com.example.safitness.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.safitness.R
import com.example.safitness.core.Modality
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.repo.Repos
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.LinkedHashSet

class MainActivity : AppCompatActivity() {
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<androidx.cardview.widget.CardView>(R.id.cardDay1).setOnClickListener { openDay(1) }
        findViewById<androidx.cardview.widget.CardView>(R.id.cardDay2).setOnClickListener { openDay(2) }
        findViewById<androidx.cardview.widget.CardView>(R.id.cardDay3).setOnClickListener { openDay(3) }
        findViewById<androidx.cardview.widget.CardView>(R.id.cardDay4).setOnClickListener { openDay(4) }
        findViewById<androidx.cardview.widget.CardView>(R.id.cardDay5).setOnClickListener { openDay(5) }

        findViewById<androidx.cardview.widget.CardView>(R.id.cardExerciseLibrary)?.setOnClickListener {
            startActivity(Intent(this, ExerciseLibraryActivity::class.java))
        }
        findViewById<androidx.cardview.widget.CardView>(R.id.cardPersonalRecords)?.setOnClickListener {
            startActivity(Intent(this, PersonalRecordsActivity::class.java))
        }
        findViewById<androidx.cardview.widget.CardView>(R.id.cardSettings)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDayLabels()
    }

    private fun refreshDayLabels() {
        scope.launch(Dispatchers.IO) {
            val repo = Repos.workoutRepository(this@MainActivity)

            for (day in 1..5) {
                // 1) Strength program for the day (exclude metcon modality)
                val program = repo.programForDay(day).first() // Flow<List<ExerciseWithSelection>>
                val strengthItems = program.filter { it.exercise.modality != Modality.METCON }

                // Preserve first-seen order and avoid duplicates
                val labels = LinkedHashSet<String>()
                strengthItems.forEach { item ->
                    labels += mapWorkoutTypeToLabel(item.exercise.workoutType)
                }

                // 2) Metcon selections for the day (if any, append "Metcon")
                val metcons = repo.metconsForDay(day).first() // Flow<List<SelectionWithPlanAndComponents>>
                if (metcons.isNotEmpty()) labels += "Metcon"

                val label = if (labels.isEmpty()) "Empty" else labels.joinToString(" • ")

                withContext(Dispatchers.Main) {
                    val cardId = resources.getIdentifier("cardDay$day", "id", packageName)
                    val card = findViewById<androidx.cardview.widget.CardView>(cardId)
                    val title = card?.findViewById<TextView>(R.id.tvDayTitle)
                    // Keep your existing "Day N - ..." format
                    title?.text = "Day $day - $label"
                }
            }
        }
    }

    private fun mapWorkoutTypeToLabel(t: WorkoutType): String = when (t) {
        WorkoutType.PUSH -> "Push"
        WorkoutType.PULL -> "Pull"
        WorkoutType.LEGS_CORE -> "Legs & Core"
        else -> t.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }

    private fun openDay(day: Int) {
        startActivity(Intent(this, WorkoutActivity::class.java).apply {
            putExtra("DAY_INDEX", day)
            putExtra("WORKOUT_NAME", "Day $day")
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
