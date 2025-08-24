package com.example.safitness.ui.profile

import android.os.Bundle
import android.widget.*
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.safitness.R
import com.example.safitness.data.entities.UserProfile
import com.example.safitness.data.repo.Repos
import com.example.safitness.ml.ExperienceLevel
import com.example.safitness.ml.Goal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    // Equipment
    private lateinit var cbBody: CheckBox
    private lateinit var cbDb: CheckBox
    private lateinit var cbKb: CheckBox
    private lateinit var cbBb: CheckBox
    private lateinit var cbCable: CheckBox
    private lateinit var cbMachine: CheckBox
    private lateinit var cbBand: CheckBox

    // Engine
    private lateinit var cbIncludeEngine: CheckBox
    private lateinit var cbModeRun: CheckBox
    private lateinit var cbModeRow: CheckBox
    private lateinit var cbModeBike: CheckBox

    // Skills
    private lateinit var cbSkillDU: CheckBox
    private lateinit var cbSkillHSPU: CheckBox
    private lateinit var cbSkillHS: CheckBox
    private lateinit var cbSkillTTB: CheckBox
    private lateinit var cbSkillBMU: CheckBox
    private lateinit var cbSkillKPU: CheckBox

    // Days (use CompoundButton so XML can be ToggleButton or CheckBox)
    private lateinit var cbMon: CompoundButton
    private lateinit var cbTue: CompoundButton
    private lateinit var cbWed: CompoundButton
    private lateinit var cbThu: CompoundButton
    private lateinit var cbFri: CompoundButton
    private lateinit var cbSat: CompoundButton
    private lateinit var cbSun: CompoundButton

    // Duration controls
    private lateinit var seekMinutes: SeekBar
    private lateinit var tvMinutes: TextView
    private lateinit var seekWeeks: SeekBar
    private lateinit var tvWeeks: TextView

    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Equipment
        cbBody = findViewById(R.id.cbEqBodyweight)
        cbDb = findViewById(R.id.cbEqDumbbell)
        cbKb = findViewById(R.id.cbEqKettlebell)
        cbBb = findViewById(R.id.cbEqBarbell)
        cbCable = findViewById(R.id.cbEqCable)
        cbMachine = findViewById(R.id.cbEqMachine)
        cbBand = findViewById(R.id.cbEqBand)

        // Engine + modes
        cbIncludeEngine = findViewById(R.id.cbIncludeEngine)
        cbModeRun = findViewById(R.id.cbModeRun)
        cbModeRow = findViewById(R.id.cbModeRow)
        cbModeBike = findViewById(R.id.cbModeBike)

        // Skills
        cbSkillDU = findViewById(R.id.cbSkillDU)
        cbSkillHSPU = findViewById(R.id.cbSkillHSPU)
        cbSkillHS = findViewById(R.id.cbSkillHS)
        cbSkillTTB = findViewById(R.id.cbSkillTTB)
        cbSkillBMU = findViewById(R.id.cbSkillBMU)
        cbSkillKPU = findViewById(R.id.cbSkillKPU)

        // Days (bind as CompoundButton)
        cbMon = findViewById(R.id.cbMon)
        cbTue = findViewById(R.id.cbTue)
        cbWed = findViewById(R.id.cbWed)
        cbThu = findViewById(R.id.cbThu)
        cbFri = findViewById(R.id.cbFri)
        cbSat = findViewById(R.id.cbSat)
        cbSun = findViewById(R.id.cbSun)

        // Duration
        seekMinutes = findViewById(R.id.seekMinutes)
        tvMinutes = findViewById(R.id.tvMinutesValue)
        seekWeeks = findViewById(R.id.seekWeeks)
        tvWeeks = findViewById(R.id.tvWeeksValue)
        btnSave = findViewById(R.id.btnSaveProfile)

        tvMinutes.text = (seekMinutes.progress + 20).toString()
        tvWeeks.text = (seekWeeks.progress + 1).toString()

        seekMinutes.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                tvMinutes.text = (p + 20).toString()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
        seekWeeks.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
        findViewById<RadioButton>(R.id.rbGoalStrength).isChecked   = p.goal == Goal.STRENGTH
        findViewById<RadioButton>(R.id.rbGoalHypertrophy).isChecked = p.goal == Goal.HYPERTROPHY
        findViewById<RadioButton>(R.id.rbGoalEndurance).isChecked   = p.goal == Goal.ENDURANCE

        // Experience
        findViewById<RadioButton>(R.id.rbExpBeginner).isChecked     = p.experience == ExperienceLevel.BEGINNER
        findViewById<RadioButton>(R.id.rbExpIntermediate).isChecked = p.experience == ExperienceLevel.INTERMEDIATE
        findViewById<RadioButton>(R.id.rbExpAdvanced).isChecked     = p.experience == ExperienceLevel.ADVANCED

        // Equipment CSV → checkboxes
        val eq = (p.equipmentCsv.takeIf { it.isNotBlank() } ?: "BODYWEIGHT").split(',').toSet()
        cbBody.isChecked   = "BODYWEIGHT" in eq
        cbDb.isChecked     = "DUMBBELL"  in eq
        cbKb.isChecked     = "KETTLEBELL" in eq
        cbBb.isChecked     = "BARBELL"   in eq
        cbCable.isChecked  = "CABLE"     in eq
        cbMachine.isChecked= "MACHINE"   in eq
        cbBand.isChecked   = "BAND"      in eq

        // Minutes / Weeks
        seekMinutes.progress = (p.sessionMinutes - 20).coerceIn(0, 100)
        tvMinutes.text = p.sessionMinutes.toString()
        seekWeeks.progress = (p.programWeeks - 1).coerceIn(0, 11)
        tvWeeks.text = p.programWeeks.toString()

        // Engine
        cbIncludeEngine.isChecked = p.includeEngine
        val modes = parseCsv(p.engineModesCsv)
        cbModeRun.isChecked  = "RUN" in modes
        cbModeRow.isChecked  = "ROW" in modes
        cbModeBike.isChecked = "BIKE" in modes

        // Skills
        val skills = parseCsv(p.preferredSkillsCsv)
        cbSkillDU.isChecked   = "DOUBLE_UNDER"       in skills
        cbSkillHSPU.isChecked = "HANDSTAND_PUSH_UP"  in skills
        cbSkillHS.isChecked   = "HANDSTAND"          in skills
        cbSkillTTB.isChecked  = "TOES_TO_BAR"        in skills
        cbSkillBMU.isChecked  = "BAR_MUSCLE_UP"      in skills
        cbSkillKPU.isChecked  = "KIPPING_PULL_UP"    in skills

        // Days (explicit)
        bindDayButtons(p.workoutDaysCsv)
    }

    private fun collectProfileFromUi(): UserProfile {
        val goal = when {
            findViewById<RadioButton>(R.id.rbGoalHypertrophy).isChecked -> Goal.HYPERTROPHY
            findViewById<RadioButton>(R.id.rbGoalEndurance).isChecked   -> Goal.ENDURANCE
            else                                                        -> Goal.STRENGTH
        }
        val exp = when {
            findViewById<RadioButton>(R.id.rbExpAdvanced).isChecked     -> ExperienceLevel.ADVANCED
            findViewById<RadioButton>(R.id.rbExpIntermediate).isChecked -> ExperienceLevel.INTERMEDIATE
            else                                                        -> ExperienceLevel.BEGINNER
        }

        val eq = buildSet {
            if (cbBody.isChecked) add("BODYWEIGHT")
            if (cbDb.isChecked)   add("DUMBBELL")
            if (cbKb.isChecked)   add("KETTLEBELL")
            if (cbBb.isChecked)   add("BARBELL")
            if (cbCable.isChecked)add("CABLE")
            if (cbMachine.isChecked)add("MACHINE")
            if (cbBand.isChecked) add("BAND")
        }.joinToString(",")

        val minutes = tvMinutes.text.toString().toIntOrNull() ?: 45
        val weeks   = tvWeeks.text.toString().toIntOrNull() ?: 4

        val engineModes = buildList {
            if (cbModeRun.isChecked)  add("RUN")
            if (cbModeRow.isChecked)  add("ROW")
            if (cbModeBike.isChecked) add("BIKE")
        }.joinToString(",").ifBlank { null }

        val skills = buildList {
            if (cbSkillDU.isChecked)   add("DOUBLE_UNDER")
            if (cbSkillHSPU.isChecked) add("HANDSTAND_PUSH_UP")
            if (cbSkillHS.isChecked)   add("HANDSTAND")
            if (cbSkillTTB.isChecked)  add("TOES_TO_BAR")
            if (cbSkillBMU.isChecked)  add("BAR_MUSCLE_UP")
            if (cbSkillKPU.isChecked)  add("KIPPING_PULL_UP")
        }.joinToString(",").ifBlank { null }

        val daysCsv = daysCsvFromButtons()

        return UserProfile(
            id = 1L,
            goal = goal,
            experience = exp,
            daysPerWeek = daysCsv?.split(',')?.size ?: 3, // keep legacy field roughly in sync
            sessionMinutes = minutes,
            equipmentCsv = eq,
            programWeeks = weeks,
            workoutDaysCsv = daysCsv,
            includeEngine = cbIncludeEngine.isChecked,
            engineModesCsv = engineModes,
            preferredSkillsCsv = skills
        )
    }

    private fun parseCsv(csv: String?): Set<String> =
        csv?.split(',')?.mapNotNull { it.trim().takeIf { it.isNotEmpty() } }?.toSet() ?: emptySet()

    private fun bindDayButtons(csv: String?) {
        val set = parseCsv(csv).map { it.uppercase() }.toSet()
        cbMon.isChecked = "MON" in set || "MONDAY" in set
        cbTue.isChecked = "TUE" in set || "TUESDAY" in set
        cbWed.isChecked = "WED" in set || "WEDNESDAY" in set
        cbThu.isChecked = "THU" in set || "THURSDAY" in set
        cbFri.isChecked = "FRI" in set || "FRIDAY" in set
        cbSat.isChecked = "SAT" in set || "SATURDAY" in set
        cbSun.isChecked = "SUN" in set || "SUNDAY" in set
    }

    private fun daysCsvFromButtons(): String? {
        val days = buildList {
            if (cbMon.isChecked) add("MON")
            if (cbTue.isChecked) add("TUE")
            if (cbWed.isChecked) add("WED")
            if (cbThu.isChecked) add("THU")
            if (cbFri.isChecked) add("FRI")
            if (cbSat.isChecked) add("SAT")
            if (cbSun.isChecked) add("SUN")
        }
        return days.joinToString(",").ifBlank { null }
    }
}
