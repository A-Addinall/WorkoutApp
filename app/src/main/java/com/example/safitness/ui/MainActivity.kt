package com.example.safitness.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.safitness.R
import com.example.safitness.core.Equipment
import com.example.safitness.core.Modality
import com.example.safitness.core.WorkoutFocus
import com.example.safitness.core.WorkoutType
import com.example.safitness.ml.ExperienceLevel
import com.example.safitness.ml.GenerateRequest
import com.example.safitness.ml.Goal
import com.example.safitness.ml.UserContext
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.example.safitness.data.repo.Repos
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQ_CODE_POST_NOTIFICATIONS = 1001

        // legacy key kept for older code paths (we still set it so nothing else breaks)
        const val EXTRA_DAY_INDEX = "DAY_INDEX"

        // new keys (date-first)
        const val EXTRA_DATE_EPOCH_DAY = "DATE_EPOCH_DAY"
        const val EXTRA_WORKOUT_NAME = "WORKOUT_NAME"
    }

    private var generating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestNotificationPermission()

        // Workout of the Day (today)
        findViewById<CardView>(R.id.cardWod).setOnClickListener {
            openForDate(LocalDate.now())
        }

        // Pick a date (calendar)
        findViewById<CardView>(R.id.cardPickDate).setOnClickListener {
            val picker = MaterialDatePicker.Builder
                .datePicker()
                .setTitleText("Select a date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            picker.addOnPositiveButtonClickListener { utcMillis ->
                val selected = Instant.ofEpochMilli(utcMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                openForDate(selected)
            }

            picker.show(supportFragmentManager, "date_picker")
        }

        // Profile
        findViewById<CardView>(R.id.cardProfile).setOnClickListener {
            startActivity(Intent(this, com.example.safitness.ui.profile.ProfileActivity::class.java))
        }

        // Generate Program (placeholder)
        findViewById<CardView>(R.id.cardGenerateProgram).setOnClickListener {
            if (generating) return@setOnClickListener
            generating = true
            runMlAndPersist(LocalDate.now())
        }

        // Personal Records
        findViewById<CardView>(R.id.cardPersonalRecords).setOnClickListener {
            startActivity(Intent(this, PersonalRecordsActivity::class.java))
        }

        // Settings
        findViewById<CardView>(R.id.cardSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    /** Open WorkoutActivity for a real date. UI shows the date; we also pass a legacy dayIndex for now. */
    private fun openForDate(date: LocalDate) {
        val pretty = date.format(DateTimeFormatter.ofPattern("EEE d MMM, yyyy"))
        val i = Intent(this, WorkoutActivity::class.java).apply {
            putExtra(EXTRA_DATE_EPOCH_DAY, date.toEpochDay())
            putExtra(EXTRA_WORKOUT_NAME, pretty)
            // Keep sending a benign legacy index so older flows don't break.
            // We do NOT gate weekends anymore.
            putExtra(EXTRA_DAY_INDEX, 1)
        }
        startActivity(i)
    }

    // ----- permissions (Android 13+) -----
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
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
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CODE_POST_NOTIFICATIONS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Toast.makeText(
                    this,
                    "Notifications are disabled. Enable them in Settings to hear the rest timer.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    /** Kick off ML generation, persist to the plan for [date], and navigate there. */
    private fun runMlAndPersist(date: LocalDate, navigate: Boolean = true) {
        val root = findViewById<android.view.View>(R.id.rootScroll)
        val epochDay = date.toEpochDay()

        fun defaultFocusFor(d: LocalDate): WorkoutFocus =
            when (d.dayOfWeek) {
                java.time.DayOfWeek.MONDAY    -> WorkoutFocus.PUSH
                java.time.DayOfWeek.TUESDAY   -> WorkoutFocus.PULL
                java.time.DayOfWeek.WEDNESDAY -> WorkoutFocus.LEGS
                java.time.DayOfWeek.THURSDAY  -> WorkoutFocus.UPPER
                java.time.DayOfWeek.FRIDAY    -> WorkoutFocus.LOWER
                java.time.DayOfWeek.SATURDAY  -> WorkoutFocus.FULL_BODY
                java.time.DayOfWeek.SUNDAY    -> WorkoutFocus.CONDITIONING
            }

        // Map ML focus to your existing core.WorkoutType
        fun toCoreType(f: WorkoutFocus): WorkoutType =
            when (f) {
                WorkoutFocus.PUSH          -> WorkoutType.PUSH
                WorkoutFocus.PULL          -> WorkoutType.PULL
                WorkoutFocus.LEGS          -> WorkoutType.LEGS_CORE
                WorkoutFocus.UPPER         -> WorkoutType.PULL
                WorkoutFocus.LOWER         -> WorkoutType.LEGS_CORE
                WorkoutFocus.FULL_BODY     -> WorkoutType.LEGS_CORE
                WorkoutFocus.CORE          -> WorkoutType.LEGS_CORE
                WorkoutFocus.CONDITIONING  -> WorkoutType.LEGS_CORE
            }

        val focus = defaultFocusFor(date)
        val user = com.example.safitness.ml.UserContext(
            goal = com.example.safitness.ml.Goal.GENERAL_FITNESS,
            experience = com.example.safitness.ml.ExperienceLevel.INTERMEDIATE,
            availableEquipment = listOf(
                com.example.safitness.core.Equipment.BARBELL,
                com.example.safitness.core.Equipment.DUMBBELL,
                com.example.safitness.core.Equipment.BODYWEIGHT
            ),
            sessionMinutes = 45,
            daysPerWeek = 5
        )

        lifecycleScope.launch {
            try {
                val ml = Repos.mlService(this@MainActivity)
                val baseReq = com.example.safitness.ml.GenerateRequest(
                    date = date.toString(),
                    focus = focus,
                    modality = com.example.safitness.core.Modality.STRENGTH,
                    user = user
                )
                // Generate strength and metcon, then merge
                val strengthResp = withContext(Dispatchers.IO) {
                    ml.generate(baseReq.copy(modality = com.example.safitness.core.Modality.STRENGTH))
                }
                val metconResp = withContext(Dispatchers.IO) {
                    ml.generate(baseReq.copy(modality = com.example.safitness.core.Modality.METCON))
                }
                val merged = strengthResp.copy(metcon = metconResp.metcon)

                // Persist the generated plan for the date
                withContext(Dispatchers.IO) {
                    val planner = Repos.plannerRepository(this@MainActivity)
                    planner.applyMlToDate(
                        epochDay = epochDay,
                        resp = merged,
                        focus = toCoreType(focus),
                        availableEq = user.availableEquipment,
                        replaceStrength = true,   // overwrite strength for the day
                        replaceMetcon = false     // keep existing metcon if present
                    )
                }

                Toast.makeText(
                    this@MainActivity,
                    "Workout generated for ${date.format(DateTimeFormatter.ofPattern("EEE d MMM"))}.",
                    Toast.LENGTH_LONG
                ).show()
                if (navigate) openForDate(date)
            } catch (t: Throwable) {
                Snackbar.make(root, "Couldn’t generate workout: ${t.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                generating = false
            }
        }
    }
    private fun generateWeek(start: LocalDate = LocalDate.now()) {
        if (generating) return
        generating = true
        lifecycleScope.launch {
            try {
                // Generate 7 consecutive days without opening each date
                for (i in 0..6) {
                    runMlAndPersist(start.plusDays(i.toLong()), navigate = false)
                }
                Toast.makeText(
                    this@MainActivity,
                    "Generated a 7-day block from ${start.format(DateTimeFormatter.ofPattern("EEE d MMM"))}.",
                    Toast.LENGTH_LONG
                ).show()
                openForDate(start)
            } finally {
                generating = false
            }
        }
    }

}

