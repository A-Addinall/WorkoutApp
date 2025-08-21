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
import com.example.safitness.data.entities.UserProfile
import com.example.safitness.data.repo.Repos
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*
import androidx.lifecycle.lifecycleScope

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQ_CODE_POST_NOTIFICATIONS = 1001
    }

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

        // Generate Program from Profile (stub kept)
        findViewById<CardView>(R.id.cardGenerateProgram).setOnClickListener {
            lifecycleScope.launch {
                val userDao = Repos.userProfileDao(this@MainActivity)
                val profile = userDao.flowProfile().first() ?: UserProfile()
                val daysToFill = minOf(5, profile.daysPerWeek)
                // Keep your existing generator wiring if you like; this is just a stub notice.
                Toast.makeText(this@MainActivity, "Program generation stub (unchanged)", Toast.LENGTH_SHORT).show()
            }
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

    /** Map a calendar date to your current 1..5 day model (temporary adapter). */
    private fun openForDate(date: LocalDate) {
        // For now, map weekdays -> Day 1..5 and warn on weekends.
        val dayIndex = when (date.dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> 1
            java.time.DayOfWeek.TUESDAY -> 2
            java.time.DayOfWeek.WEDNESDAY -> 3
            java.time.DayOfWeek.THURSDAY -> 4
            java.time.DayOfWeek.FRIDAY -> 5
            java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY -> null
        }

        if (dayIndex == null) {
            val prettyDow = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            Snackbar.make(findViewById(R.id.rootScroll),
                "No programmed workout for $prettyDow yet.",
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        val i = Intent(this, WorkoutActivity::class.java).apply {
            putExtra("DAY_INDEX", dayIndex)
            putExtra("WORKOUT_NAME", "Day $dayIndex")
            // If later you add date-backed storage, also pass:
            // putExtra("DATE_EPOCH_DAY", date.toEpochDay())
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
}
