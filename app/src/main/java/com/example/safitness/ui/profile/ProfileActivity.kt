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

class ProfileActivity : AppCompatActivity() {

    private lateinit var rgGoal: RadioGroup
    private lateinit var rgExperience: RadioGroup
    private lateinit var cbBody: CheckBox
    private lateinit var cbDb: CheckBox
    private lateinit var cbKb: CheckBox
    private lateinit var cbBb: CheckBox
    private lateinit var cbCable: CheckBox
    private lateinit var cbMachine: CheckBox
    private lateinit var cbBand: CheckBox
    private lateinit var seekDays: SeekBar
    private lateinit var tvDays: TextView
    private lateinit var seekMinutes: SeekBar
    private lateinit var tvMinutes: TextView
    private lateinit var btnSave: Button
    private lateinit var seekWeeks: SeekBar
    private lateinit var tvWeeks: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        rgGoal = findViewById(R.id.rgGoal)
        rgExperience = findViewById(R.id.rgExperience)
        cbBody = findViewById(R.id.cbEqBodyweight)
        cbDb = findViewById(R.id.cbEqDumbbell)
        cbKb = findViewById(R.id.cbEqKettlebell)
        cbBb = findViewById(R.id.cbEqBarbell)
        cbCable = findViewById(R.id.cbEqCable)
        cbMachine = findViewById(R.id.cbEqMachine)
        cbBand = findViewById(R.id.cbEqBand)
        seekDays = findViewById(R.id.seekDays)
        tvDays = findViewById(R.id.tvDaysValue)
        seekMinutes = findViewById(R.id.seekMinutes)
        tvMinutes = findViewById(R.id.tvMinutesValue)
        btnSave = findViewById(R.id.btnSaveProfile)

        // SeekBars: days = progress + 1..7 ; minutes = progress + 20..120
        tvDays.text = (seekDays.progress + 1).toString()
        tvMinutes.text = (seekMinutes.progress + 20).toString()
        seekWeeks = findViewById(R.id.seekWeeks)
        tvWeeks   = findViewById(R.id.tvWeeksValue)

        seekDays.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                tvDays.text = (p + 1).toString()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
        seekMinutes.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                tvMinutes.text = (p + 20).toString()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        seekWeeks.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                tvWeeks.text = (p + 1).toString()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        // Load existing profile
        lifecycleScope.launch {
            val dao = Repos.userProfileDao(this@ProfileActivity)
            val profile = dao.flowProfile().first() ?: UserProfile()
            bindProfile(profile)
        }

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

    private fun bindProfile(p: UserProfile) {
        // Goal
        when (p.goal) {
            Goal.STRENGTH -> findViewById<RadioButton>(R.id.rbGoalStrength).isChecked = true
            Goal.HYPERTROPHY -> findViewById<RadioButton>(R.id.rbGoalHypertrophy).isChecked = true
            Goal.ENDURANCE -> findViewById<RadioButton>(R.id.rbGoalEndurance).isChecked = true
            Goal.RECOMP -> TODO()
            Goal.GENERAL_FITNESS -> TODO()
        }
        // Experience
        when (p.experience) {
            ExperienceLevel.BEGINNER -> findViewById<RadioButton>(R.id.rbExpBeginner).isChecked = true
            ExperienceLevel.INTERMEDIATE -> findViewById<RadioButton>(R.id.rbExpIntermediate).isChecked = true
            ExperienceLevel.ADVANCED -> findViewById<RadioButton>(R.id.rbExpAdvanced).isChecked = true
        }
        // Equipment CSV → checkboxes
        val eq = p.equipmentCsv.split(',').toSet()
        cbBody.isChecked = eq.contains(Equipment.BODYWEIGHT.name)
        cbDb.isChecked = eq.contains(Equipment.DUMBBELL.name)
        cbKb.isChecked = eq.contains(Equipment.KETTLEBELL.name)
        cbBb.isChecked = eq.contains(Equipment.BARBELL.name)
        cbCable.isChecked = eq.contains(Equipment.CABLE.name)
        cbMachine.isChecked = eq.contains(Equipment.MACHINE.name)
        cbBand.isChecked = eq.contains(Equipment.BAND.name)

        // Days / minutes
        seekDays.progress = (p.daysPerWeek - 1).coerceIn(0, 6)
        tvDays.text = p.daysPerWeek.toString()
        seekMinutes.progress = (p.sessionMinutes - 20).coerceIn(0, 100)
        tvMinutes.text = p.sessionMinutes.toString()
        seekWeeks.progress = (p.programWeeks - 1).coerceIn(0, 11)
        tvWeeks.text = p.programWeeks.toString()
    }

    private fun collectProfileFromUi(): UserProfile {
        val goal = when (findViewById<RadioButton>(R.id.rbGoalHypertrophy).isChecked) {
            true -> Goal.HYPERTROPHY
            false -> when (findViewById<RadioButton>(R.id.rbGoalEndurance).isChecked) {
                true -> Goal.ENDURANCE
                false -> Goal.STRENGTH
            }
        }
        val exp = when {
            findViewById<RadioButton>(R.id.rbExpAdvanced).isChecked -> ExperienceLevel.ADVANCED
            findViewById<RadioButton>(R.id.rbExpIntermediate).isChecked -> ExperienceLevel.INTERMEDIATE
            else -> ExperienceLevel.BEGINNER
        }
        val eq = mutableListOf<String>()
        if (cbBody.isChecked) eq += Equipment.BODYWEIGHT.name
        if (cbDb.isChecked) eq += Equipment.DUMBBELL.name
        if (cbKb.isChecked) eq += Equipment.KETTLEBELL.name
        if (cbBb.isChecked) eq += Equipment.BARBELL.name
        if (cbCable.isChecked) eq += Equipment.CABLE.name
        if (cbMachine.isChecked) eq += Equipment.MACHINE.name
        if (cbBand.isChecked) eq += Equipment.BAND.name

        val days = seekDays.progress + 1
        val minutes = seekMinutes.progress + 20
        val weeks = seekWeeks.progress + 1

        return UserProfile(
            id = 1L,
            goal = goal,
            experience = exp,
            daysPerWeek = days,
            sessionMinutes = minutes,
            equipmentCsv = eq.joinToString(","),
            programWeeks = weeks
        )
    }
}
