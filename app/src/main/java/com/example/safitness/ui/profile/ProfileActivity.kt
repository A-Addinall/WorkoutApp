package com.example.safitness.ui.profile

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.safitness.R
import com.example.safitness.core.Equipment
import com.example.safitness.data.entities.UserProfile
import com.example.safitness.data.repo.Repos
import com.example.safitness.ml.ExperienceLevel
import com.example.safitness.ml.Goal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek

class ProfileActivity : AppCompatActivity() {

    private lateinit var rgGoal: RadioGroup
    private lateinit var rgExperience: RadioGroup

    // Equipment
    private lateinit var cbBody: CheckBox; private lateinit var cbDb: CheckBox; private lateinit var cbKb: CheckBox
    private lateinit var cbBb: CheckBox;  private lateinit var cbCable: CheckBox; private lateinit var cbMachine: CheckBox; private lateinit var cbBand: CheckBox

    // Weeks / Minutes
    private lateinit var seekMinutes: SeekBar; private lateinit var tvMinutes: TextView
    private lateinit var seekWeeks: SeekBar;   private lateinit var tvWeeks: TextView

    // Days
    private lateinit var cbMon: CheckBox; private lateinit var cbTue: CheckBox; private lateinit var cbWed: CheckBox
    private lateinit var cbThu: CheckBox; private lateinit var cbFri: CheckBox; private lateinit var cbSat: CheckBox; private lateinit var cbSun: CheckBox

    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        rgGoal = findViewById(R.id.rgGoal)
        rgExperience = findViewById(R.id.rgExperience)

        cbBody = findViewById(R.id.cbEqBodyweight)
        cbDb   = findViewById(R.id.cbEqDumbbell)
        cbKb   = findViewById(R.id.cbEqKettlebell)
        cbBb   = findViewById(R.id.cbEqBarbell)
        cbCable= findViewById(R.id.cbEqCable)
        cbMachine=findViewById(R.id.cbEqMachine)
        cbBand = findViewById(R.id.cbEqBand)

        seekWeeks   = findViewById(R.id.seekWeeks)
        tvWeeks     = findViewById(R.id.tvWeeksValue)
        seekMinutes = findViewById(R.id.seekMinutes)
        tvMinutes   = findViewById(R.id.tvMinutesValue)

        cbMon = findViewById(R.id.cbMon); cbTue = findViewById(R.id.cbTue); cbWed = findViewById(R.id.cbWed)
        cbThu = findViewById(R.id.cbThu); cbFri = findViewById(R.id.cbFri); cbSat = findViewById(R.id.cbSat); cbSun = findViewById(R.id.cbSun)

        // SeekBars
        tvWeeks.text = (seekWeeks.progress + 1).toString()
        seekWeeks.setOnSeekBarChangeListener(simpleSeek { p -> tvWeeks.text = (p + 1).toString() })

        tvMinutes.text = (seekMinutes.progress + 20).toString()
        seekMinutes.setOnSeekBarChangeListener(simpleSeek { p -> tvMinutes.text = (p + 20).toString() })

        // Load profile
        lifecycleScope.launch {
            val dao = Repos.userProfileDao(this@ProfileActivity)
            val profile = dao.flowProfile().first() ?: UserProfile()
            bindProfile(profile)
        }

        btnSave = findViewById(R.id.btnSaveProfile)
        btnSave.setOnClickListener {
            lifecycleScope.launch {
                val dao = Repos.userProfileDao(this@ProfileActivity)
                val profile = collectProfileFromUi()
                dao.upsert(profile)
                Toast.makeText(this@ProfileActivity, "Saved", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun simpleSeek(onChange: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) { onChange(p) }
        override fun onStartTrackingTouch(p0: SeekBar?) {}
        override fun onStopTrackingTouch(p0: SeekBar?) {}
    }

    private fun bindProfile(p: UserProfile) {
        // Goal
        when (p.goal) {
            Goal.STRENGTH        -> findViewById<RadioButton>(R.id.rbGoalStrength).isChecked = true
            Goal.HYPERTROPHY     -> findViewById<RadioButton>(R.id.rbGoalHypertrophy).isChecked = true
            Goal.ENDURANCE       -> findViewById<RadioButton>(R.id.rbGoalEndurance).isChecked = true
            Goal.RECOMP, Goal.GENERAL_FITNESS -> { /* leave default */ }
        }
        // Experience
        when (p.experience) {
            ExperienceLevel.BEGINNER     -> findViewById<RadioButton>(R.id.rbExpBeginner).isChecked = true
            ExperienceLevel.INTERMEDIATE -> findViewById<RadioButton>(R.id.rbExpIntermediate).isChecked = true
            ExperienceLevel.ADVANCED     -> findViewById<RadioButton>(R.id.rbExpAdvanced).isChecked = true
        }
        // Equipment
        val eq = (p.equipmentCsv ?: "").split(',').filter { it.isNotBlank() }.toSet()
        cbBody.isChecked    = eq.contains(Equipment.BODYWEIGHT.name)
        cbDb.isChecked      = eq.contains(Equipment.DUMBBELL.name)
        cbKb.isChecked      = eq.contains(Equipment.KETTLEBELL.name)
        cbBb.isChecked      = eq.contains(Equipment.BARBELL.name)
        cbCable.isChecked   = eq.contains(Equipment.CABLE.name)
        cbMachine.isChecked = eq.contains(Equipment.MACHINE.name)
        cbBand.isChecked    = eq.contains(Equipment.BAND.name)

        // Weeks / Minutes
        val weeks = (p.programWeeks ?: 4).coerceIn(1, 12)
        seekWeeks.progress = weeks - 1
        tvWeeks.text = weeks.toString()

        val minutes = (p.sessionMinutes ?: 45).coerceIn(20, 120)
        seekMinutes.progress = minutes - 20
        tvMinutes.text = minutes.toString()

        // Days (prefer explicit CSV if present)
        val days = parseDaysCsv(p.workoutDaysCsv) ?: deriveDaysOfWeek((p.daysPerWeek ?: 3).coerceIn(1,7))
        cbMon.isChecked = DayOfWeek.MONDAY in days
        cbTue.isChecked = DayOfWeek.TUESDAY in days
        cbWed.isChecked = DayOfWeek.WEDNESDAY in days
        cbThu.isChecked = DayOfWeek.THURSDAY in days
        cbFri.isChecked = DayOfWeek.FRIDAY in days
        cbSat.isChecked = DayOfWeek.SATURDAY in days
        cbSun.isChecked = DayOfWeek.SUNDAY in days
    }

    private fun collectProfileFromUi(): UserProfile {
        val goal = when {
            findViewById<RadioButton>(R.id.rbGoalHypertrophy).isChecked -> Goal.HYPERTROPHY
            findViewById<RadioButton>(R.id.rbGoalEndurance).isChecked   -> Goal.ENDURANCE
            else -> Goal.STRENGTH
        }
        val exp = when {
            findViewById<RadioButton>(R.id.rbExpAdvanced).isChecked     -> ExperienceLevel.ADVANCED
            findViewById<RadioButton>(R.id.rbExpIntermediate).isChecked -> ExperienceLevel.INTERMEDIATE
            else -> ExperienceLevel.BEGINNER
        }
        val eqCsv = buildList {
            if (cbBody.isChecked) add(Equipment.BODYWEIGHT.name)
            if (cbDb.isChecked)   add(Equipment.DUMBBELL.name)
            if (cbKb.isChecked)   add(Equipment.KETTLEBELL.name)
            if (cbBb.isChecked)   add(Equipment.BARBELL.name)
            if (cbCable.isChecked)add(Equipment.CABLE.name)
            if (cbMachine.isChecked)add(Equipment.MACHINE.name)
            if (cbBand.isChecked) add(Equipment.BAND.name)
        }.joinToString(",")

        val selectedDays = buildSet {
            if (cbMon.isChecked) add(DayOfWeek.MONDAY)
            if (cbTue.isChecked) add(DayOfWeek.TUESDAY)
            if (cbWed.isChecked) add(DayOfWeek.WEDNESDAY)
            if (cbThu.isChecked) add(DayOfWeek.THURSDAY)
            if (cbFri.isChecked) add(DayOfWeek.FRIDAY)
            if (cbSat.isChecked) add(DayOfWeek.SATURDAY)
            if (cbSun.isChecked) add(DayOfWeek.SUNDAY)
        }
        val daysCsv = if (selectedDays.isEmpty()) null else selectedDays.joinToString(",") { it.name.take(3) }

        val weeks   = seekWeeks.progress + 1
        val minutes = seekMinutes.progress + 20

        // Keep legacy daysPerWeek in sync for older code paths
        val daysPerWeek = selectedDays.size.coerceIn(1, 7)

        return UserProfile(
            id = 1L,
            goal = goal,
            experience = exp,
            daysPerWeek = daysPerWeek,
            sessionMinutes = minutes,
            equipmentCsv = eqCsv,
            programWeeks = weeks,
            workoutDaysCsv = daysCsv
        )
    }

    private fun parseDaysCsv(csv: String?): Set<DayOfWeek>? =
        csv?.split(',')?.mapNotNull { k ->
            when (k.trim().uppercase()) {
                "MON","MONDAY" -> DayOfWeek.MONDAY
                "TUE","TUESDAY"-> DayOfWeek.TUESDAY
                "WED","WEDNESDAY"-> DayOfWeek.WEDNESDAY
                "THU","THURSDAY"-> DayOfWeek.THURSDAY
                "FRI","FRIDAY" -> DayOfWeek.FRIDAY
                "SAT","SATURDAY"-> DayOfWeek.SATURDAY
                "SUN","SUNDAY" -> DayOfWeek.SUNDAY
                else -> null
            }
        }?.toSet()?.takeIf { it.isNotEmpty() }

    private fun deriveDaysOfWeek(n: Int): Set<DayOfWeek> = when (n.coerceIn(1,7)) {
        1 -> setOf(DayOfWeek.MONDAY)
        2 -> setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
        3 -> setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        4 -> setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        5 -> setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        6 -> DayOfWeek.values().toSet() - DayOfWeek.SUNDAY
        else -> DayOfWeek.values().toSet()
    }
}
