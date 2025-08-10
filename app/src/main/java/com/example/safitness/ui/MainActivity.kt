package com.example.safitness.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView     // ✅ import
import com.example.safitness.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<CardView>(R.id.cardDay1)?.setOnClickListener {
            openWorkout(1, "Day 1 — Push")
        }
        findViewById<CardView>(R.id.cardDay2)?.setOnClickListener {
            openWorkout(2, "Day 2 — Pull")
        }
        findViewById<CardView>(R.id.cardDay3)?.setOnClickListener {
            openWorkout(3, "Day 3 — Legs/Core")
        }
        // If you added Day D/E cards in XML:
        findViewById<CardView>(R.id.cardDay4)?.setOnClickListener {
            openWorkout(4, "Day 4 — Mixed")
        }
        findViewById<CardView>(R.id.cardDay5)?.setOnClickListener {
            openWorkout(5, "Day 5 — Mixed")
        }

        findViewById<CardView>(R.id.cardExerciseLibrary)?.setOnClickListener {
            startActivity(Intent(this, ExerciseLibraryActivity::class.java))
        }
        findViewById<CardView>(R.id.cardPersonalRecords)?.setOnClickListener {
            startActivity(Intent(this, PersonalRecordsActivity::class.java))
        }
        findViewById<CardView>(R.id.cardSettings)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun openWorkout(day: Int, name: String) {
        startActivity(
            Intent(this, WorkoutActivity::class.java).apply {
                putExtra("DAY_INDEX", day)
                putExtra("WORKOUT_NAME", name)
            }
        )
    }
}
