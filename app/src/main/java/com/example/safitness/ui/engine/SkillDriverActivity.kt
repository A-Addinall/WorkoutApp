package com.example.safitness.ui.engine

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.safitness.core.SkillTestType
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.SkillLogEntity
import com.example.safitness.data.repo.SkillLogRepository
import com.example.safitness.databinding.ActivitySkillDriverBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SkillDriverActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkillDriverBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySkillDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra("TITLE") ?: "Skill Driver"
        val skill = intent.getStringExtra("SKILL") ?: "BAR_MUSCLE_UP"
        val defaultTest = intent.getStringExtra("DEFAULT_TEST") ?: SkillTestType.ATTEMPTS.name

        binding.txtTitle.text = title

        // Toggle sections based on test
        showSection(defaultTest)

        binding.btnSave.setOnClickListener {
            val db = AppDatabase.get(this)
            CoroutineScope(Dispatchers.IO).launch {
                val repo = SkillLogRepository(db.skillLogDao())
                val entity = when (SkillTestType.valueOf(currentTestType())) {
                    SkillTestType.ATTEMPTS -> SkillLogEntity(
                        skill = skill,
                        testType = SkillTestType.ATTEMPTS.name,
                        attemptsSuccess = binding.etSuccess.text?.toString()?.toIntOrNull() ?: 0,
                        attemptsFail = binding.etFail.text?.toString()?.toIntOrNull() ?: 0
                    )
                    SkillTestType.MAX_HOLD_SECONDS -> SkillLogEntity(
                        skill = skill,
                        testType = SkillTestType.MAX_HOLD_SECONDS.name,
                        holdSeconds = binding.etHold.text?.toString()?.toIntOrNull() ?: 0
                    )
                    SkillTestType.FOR_TIME_REPS -> SkillLogEntity(
                        skill = skill,
                        testType = SkillTestType.FOR_TIME_REPS.name,
                        reps = binding.etReps.text?.toString()?.toIntOrNull() ?: 0
                    )
                    SkillTestType.MAX_REPS_UNBROKEN -> SkillLogEntity(
                        skill = skill,
                        testType = SkillTestType.MAX_REPS_UNBROKEN.name,
                        reps = binding.etReps.text?.toString()?.toIntOrNull() ?: 0
                    )
                }
                repo.log(entity)
            }
            finish()
        }

        binding.rgTestType.setOnCheckedChangeListener { _, _ ->
            showSection(currentTestType())
        }
        // Pre-select
        when (defaultTest) {
            SkillTestType.ATTEMPTS.name -> binding.rbAttempts.isChecked = true
            SkillTestType.MAX_HOLD_SECONDS.name -> binding.rbHold.isChecked = true
            SkillTestType.FOR_TIME_REPS.name -> binding.rbForTimeReps.isChecked = true
            SkillTestType.MAX_REPS_UNBROKEN.name -> binding.rbMaxReps.isChecked = true
        }
    }

    private fun currentTestType(): String = when (binding.rgTestType.checkedRadioButtonId) {
        binding.rbAttempts.id -> SkillTestType.ATTEMPTS.name
        binding.rbHold.id -> SkillTestType.MAX_HOLD_SECONDS.name
        binding.rbForTimeReps.id -> SkillTestType.FOR_TIME_REPS.name
        binding.rbMaxReps.id -> SkillTestType.MAX_REPS_UNBROKEN.name
        else -> SkillTestType.ATTEMPTS.name
    }

    private fun showSection(test: String) {
        val t = SkillTestType.valueOf(test)
        binding.groupAttempts.visibility = if (t == SkillTestType.ATTEMPTS) View.VISIBLE else View.GONE
        binding.groupHold.visibility = if (t == SkillTestType.MAX_HOLD_SECONDS) View.VISIBLE else View.GONE
        binding.groupReps.visibility = if (t == SkillTestType.FOR_TIME_REPS || t == SkillTestType.MAX_REPS_UNBROKEN) View.VISIBLE else View.GONE
    }
}
