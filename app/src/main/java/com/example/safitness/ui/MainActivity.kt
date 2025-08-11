// app/src/main/java/com/example/safitness/ui/MainActivity.kt
package com.example.safitness.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.safitness.R
import com.example.safitness.data.repo.Repos
import kotlinx.coroutines.*

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
                val label = repo.daySummaryLabel(day)  // "Push", "Mixed", or "Empty"
                withContext(Dispatchers.Main) {
                    val cardId = resources.getIdentifier("cardDay$day", "id", packageName)
                    val card = findViewById<androidx.cardview.widget.CardView>(cardId)
                    val title = card?.findViewById<TextView>(R.id.tvDayTitle)
                    title?.text = "Day $day - $label"
                }
            }
        }
    }

    private fun openDay(day: Int) {
        startActivity(Intent(this, WorkoutActivity::class.java).apply {
            putExtra("DAY_INDEX", day)
            putExtra("WORKOUT_NAME", "Day $day")
        })
    }

    override fun onDestroy() {
        super.onDestroy(); scope.cancel()
    }
}
