package com.example.safitness.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.safitness.R
import com.example.safitness.core.Modality
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.repo.Repos
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.LinkedHashSet
import androidx.lifecycle.lifecycleScope
import com.example.safitness.BuildConfig
import com.example.safitness.data.repo.PlannerRepository
import com.example.safitness.core.Equipment

class MainActivity : AppCompatActivity() {
    private val scope = MainScope()

    companion object {
        private const val REQ_CODE_POST_NOTIFICATIONS = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // inside onCreate() AFTER setContentView(...)
        if (BuildConfig.DEBUG) {
            lifecycleScope.launch {
                val repo = PlannerRepository(
                    planDao = Repos.planDao(this@MainActivity),
                    libraryDao = Repos.libraryDao(this@MainActivity)
                )

                val suggestions = repo.generateSuggestionsForDay(
                    dayIndex = 1,
                    focus = WorkoutType.PUSH,
                    availableEq = listOf(Equipment.DUMBBELL, Equipment.BODYWEIGHT)
                )
                val summary = suggestions.joinToString { s ->
                    "${s.exercise.name} ${s.repsMin}-${s.repsMax} x${s.targetSets}"
                }
                val db = com.example.safitness.data.db.AppDatabase.get(this@MainActivity)
                android.util.Log.d("PLANNER", "muscles=${db.exerciseMetadataDao().countMuscles()} eqLinks=${db.exerciseMetadataDao().countExerciseEquipment()}")

                android.util.Log.d("PLANNER", "SUGGESTIONS: $summary")

                // Optional: persist so current UI shows them
                // repo.persistSuggestionsToDay(dayIndex = 1, suggestions)
                // Toast.makeText(this@MainActivity, "Day 1 autoloaded (${suggestions.size})", Toast.LENGTH_SHORT).show()
            }
        }


        // Ask for notifications permission (Android 13+)
        requestNotificationPermission()

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
        val card = findViewById<CardView>(cardId)

        card.findViewById<com.google.android.material.button.MaterialButton?>(R.id.btnStartDay)
            ?.setOnClickListener { openDay(day) }

        card.findViewById<com.google.android.material.button.MaterialButton?>(R.id.btnEditDay)
            ?.setOnClickListener {
                startActivity(Intent(this, ExerciseLibraryActivity::class.java).apply {
                    putExtra("DAY_INDEX", day)
                })
            }
    }

    private fun refreshDayLabels() {
        scope.launch(Dispatchers.IO) {
            val repo = Repos.workoutRepository(this@MainActivity)

            for (day in 1..5) {
                val program = repo.programForDay(day).first()
                val strengthItems = program.filter { it.exercise.modality != Modality.METCON }

                val labels = LinkedHashSet<String>()
                strengthItems.forEach { item ->
                    labels += mapWorkoutTypeToLabel(item.exercise.workoutType)
                }

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

    // -------- Notifications permission (Android 13+) --------

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQ_CODE_POST_NOTIFICATIONS
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQ_CODE_POST_NOTIFICATIONS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                )
                if (showRationale) {
                    // Soft deny: explain why it matters
                    Toast.makeText(
                        this,
                        "Enable notifications to hear rest-timer beeps in the background.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // “Don’t ask again” or policy deny: point to Settings
                    Toast.makeText(
                        this,
                        "Notifications are disabled. Enable them in Settings to hear the rest timer.",
                        Toast.LENGTH_LONG
                    ).show()
                    // (Optional) Offer to open settings automatically:
                    // openAppNotificationSettings()
                }
            }
        }
    }

    // Open app-specific notification settings (only used if you decide to trigger it)
    @Suppress("unused")
    private fun openAppNotificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
            startActivity(intent)
        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
