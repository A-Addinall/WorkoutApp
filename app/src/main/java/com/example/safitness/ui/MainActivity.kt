package com.example.safitness.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.safitness.R
import com.example.safitness.BuildConfig
import com.example.safitness.core.Equipment
import com.example.safitness.core.Modality
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.entities.UserProfile
import com.example.safitness.data.repo.PlannerRepository
import com.example.safitness.data.repo.Repos
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.LinkedHashSet


class MainActivity : AppCompatActivity() {
    private val scope = MainScope()

    companion object {
        private const val REQ_CODE_POST_NOTIFICATIONS = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ---- DEV: optional auto-generate on boot (disable if noisy) ----
        if (BuildConfig.DEBUG) {
            lifecycleScope.launch {
                generateDayFromProfile(
                    dayIndex = 1,
                    focus = WorkoutType.PUSH,
                    replaceStrength = true,
                    replaceMetcon = true
                )
            }
        }

        // Ask for notifications permission (Android 13+)
        requestNotificationPermission()

        // Bind the 5 day cards
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
        findViewById<CardView>(R.id.cardGenerateProgram)?.setOnClickListener {
            lifecycleScope.launch {
                val userDao = Repos.userProfileDao(this@MainActivity)
                val profile = userDao.flowProfile().first() ?: UserProfile()
                val daysToFill = minOf(5, profile.daysPerWeek)

                // simple default weekly split
                val split = listOf(
                    com.example.safitness.core.WorkoutType.PUSH,
                    com.example.safitness.core.WorkoutType.PULL,
                    com.example.safitness.core.WorkoutType.LEGS_CORE,
                    com.example.safitness.core.WorkoutType.PUSH,
                    com.example.safitness.core.WorkoutType.PULL
                )

                // fill visible 1..daysToFill using profile equipment
                for (d in 1..daysToFill) {
                    val focus = split[d - 1]
                    generateDayFromProfile(
                        dayIndex = d,
                        focus = focus,
                        replaceStrength = true,
                        replaceMetcon = true
                    )
                }

                refreshDayLabels()
                Toast.makeText(this@MainActivity, "Program generated from profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDayLabels()
    }


    private fun bindDayCard(day: Int, cardId: Int) {
        val card = findViewById<CardView>(cardId)

        findViewById<CardView>(R.id.cardProfile)?.setOnClickListener {
            startActivity(Intent(this, com.example.safitness.ui.profile.ProfileActivity::class.java))
        }
        // Start workout
        card.findViewById<com.google.android.material.button.MaterialButton?>(R.id.btnStartDay)
            ?.setOnClickListener { openDay(day) }

        // Edit day (existing behavior)
        card.findViewById<com.google.android.material.button.MaterialButton?>(R.id.btnEditDay)
            ?.setOnClickListener {
                startActivity(Intent(this, ExerciseLibraryActivity::class.java).apply {
                    putExtra("DAY_INDEX", day)
                })
            }

        // NEW: long-press the card to auto-generate from saved profile
        // (no XML change needed; nice for testing)
        card.setOnLongClickListener {
            // Map day -> focus (adjust if you prefer another split)
            val focus = when (day) {
                1 -> WorkoutType.PUSH
                2 -> WorkoutType.PULL
                3 -> WorkoutType.LEGS_CORE
                4 -> WorkoutType.PUSH
                else -> WorkoutType.PULL
            }
            lifecycleScope.launch {
                generateDayFromProfile(
                    dayIndex = day,
                    focus = focus,
                    replaceStrength = true,
                    replaceMetcon = true
                )
                refreshDayLabels()
                Toast.makeText(this@MainActivity, "Day $day generated", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    /**
     * Phase 5: one-tap generate using the saved User Profile.
     * - Reads profile (equipment, etc.)
     * - Generates strength + focus-matched metcon
     * - Persists to day_item
     */
    private suspend fun generateDayFromProfile(
        dayIndex: Int,
        focus: WorkoutType,
        replaceStrength: Boolean,
        replaceMetcon: Boolean
    ) {
        val planDao = Repos.planDao(this)
        val libraryDao = Repos.libraryDao(this)
        val metconDao = Repos.metconDao(this)
        val userDao = Repos.userProfileDao(this)

        // ✅ add this:
        val repo = PlannerRepository(planDao, libraryDao, metconDao)

        val profile = userDao.flowProfile().first() ?: com.example.safitness.data.entities.UserProfile()

        val availableEq: List<Equipment> =
            profile.equipmentCsv
                .split(',')
                .mapNotNull { runCatching { Equipment.valueOf(it) }.getOrNull() }
                .ifEmpty { listOf(Equipment.BODYWEIGHT, Equipment.DUMBBELL) }

        val suggestions = repo.generateSuggestionsForDay(dayIndex, focus, availableEq)
        repo.persistSuggestionsToDay(dayIndex, suggestions, replaceStrength = replaceStrength)

        repo.pickMetconPlanIdForFocus(focus, availableEq, preferredBlock = null)?.let { planId ->
            repo.persistMetconPlanToDay(dayIndex, planId, replaceMetcon = replaceMetcon, required = true)
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
                    Toast.makeText(
                        this,
                        "Enable notifications to hear rest-timer beeps in the background.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Notifications are disabled. Enable them in Settings to hear the rest timer.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
