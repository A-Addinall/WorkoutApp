package com.example.safitness.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.safitness.R
import com.example.safitness.core.Modality
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.repo.Repos
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.LinkedHashSet

class MainActivity : AppCompatActivity() {
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind all five day cards with Start/Edit actions
        bindDayCard(1, R.id.cardDay1)
        bindDayCard(2, R.id.cardDay2)
        bindDayCard(3, R.id.cardDay3)
        bindDayCard(4, R.id.cardDay4)
        bindDayCard(5, R.id.cardDay5)

        findViewById<CardView>(R.id.cardPersonalRecords)?.setOnClickListener {
            startActivity(Intent(this, PersonalRecordsActivity::class.java))
        }
        findViewById<CardView>(R.id.cardSettings)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDayLabels()
    }

    private fun bindDayCard(day: Int, cardId: Int) {
        val card = findViewById<androidx.cardview.widget.CardView>(cardId)

        // Start = opens the workout for this day (keep as you have)
        card.findViewById<com.google.android.material.button.MaterialButton?>(R.id.btnStartDay)
            ?.setOnClickListener { openDay(day) }

        // EDIT = open the library for THIS day (keep DAY_INDEX!)
        card.findViewById<com.google.android.material.button.MaterialButton?>(R.id.btnEditDay)
            ?.setOnClickListener {
                startActivity(Intent(this, ExerciseLibraryActivity::class.java).apply {
                    putExtra("DAY_INDEX", day)  // this does NOT filter the catalogue
                })
        }
    }

    private fun refreshDayLabels() {
        scope.launch(Dispatchers.IO) {
            val repo = Repos.workoutRepository(this@MainActivity)

            for (day in 1..5) {
                // 1) Strength program for the day (exclude metcon modality)
                val program = repo.programForDay(day).first()
                val strengthItems = program.filter { it.exercise.modality != Modality.METCON }

                // Preserve first-seen order and avoid duplicates
                val labels = LinkedHashSet<String>()
                strengthItems.forEach { item ->
                    labels += mapWorkoutTypeToLabel(item.exercise.workoutType)
                }

                // 2) Metcon selections for the day (if any, append "Metcon")
                val metcons = repo.metconsForDay(day).first()
                if (metcons.isNotEmpty()) labels += "Metcon"

                val label = if (labels.isEmpty()) "Empty" else labels.joinToString(" • ")

                withContext(Dispatchers.Main) {
                    val cardId = resources.getIdentifier("cardDay$day", "id", packageName)
                    val card = findViewById<CardView>(cardId)
                    val title = card?.findViewById<TextView>(R.id.tvDayTitle)
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
