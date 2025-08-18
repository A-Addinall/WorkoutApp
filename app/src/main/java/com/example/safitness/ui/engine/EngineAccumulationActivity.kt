package com.example.safitness.ui.engine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.safitness.core.EngineIntent
import com.example.safitness.core.EngineSkillKeys
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.EngineLogEntity
import com.example.safitness.data.repo.EngineLogRepository
import com.example.safitness.databinding.ActivityEngineAccumulationBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EngineAccumulationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEngineAccumulationBinding
    private var remainingSec = 0
    private var ticker: Runnable? = null
    private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEngineAccumulationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra(EngineSkillKeys.TITLE) ?: "Engine — Accumulation"
        val mode = intent.getStringExtra(EngineSkillKeys.ENGINE_MODE) ?: "BIKE"
        val intentType = intent.getStringExtra(EngineSkillKeys.ENGINE_INTENT) ?: EngineIntent.FOR_DISTANCE.name
        remainingSec = intent.getIntExtra(EngineSkillKeys.PROGRAM_DURATION_SECONDS, 600)

        binding.txtTitle.text = title
        binding.txtTimer.text = formatHMS(remainingSec)

        if (intentType == EngineIntent.FOR_CALORIES.name) {
            binding.tilMeters.hint = "Calories"
            binding.etMeters.hint = "cal"
        } else {
            binding.tilMeters.hint = "Meters"
            binding.etMeters.hint = "m"
        }

        binding.btnStart.setOnClickListener { start() }
        binding.btnPause.setOnClickListener { pause() }
        binding.btnComplete.setOnClickListener {
            val input = binding.etMeters.text?.toString()?.trim().orEmpty()
            val value = input.toIntOrNull() ?: 0
            saveResult(mode, intentType, value)
            finish()
        }
    }

    private fun start() {
        if (running) return
        running = true
        ticker = object : Runnable {
            override fun run() {
                remainingSec -= 1
                if (remainingSec <= 0) {
                    remainingSec = 0
                    binding.txtTimer.text = formatHMS(remainingSec)
                    pause()
                } else {
                    binding.txtTimer.text = formatHMS(remainingSec)
                    binding.txtTimer.postDelayed(this, 1000)
                }
            }
        }.also { binding.txtTimer.post(it) }
    }

    private fun pause() {
        running = false
        ticker?.let { binding.txtTimer.removeCallbacks(it) }
    }

    private fun saveResult(mode: String, intentType: String, inputValue: Int) {
        val db = AppDatabase.get(this)
        CoroutineScope(Dispatchers.IO).launch {
            val entity = when (intentType) {
                EngineIntent.FOR_CALORIES.name -> EngineLogEntity(
                    mode = mode,
                    intent = EngineIntent.FOR_CALORIES.name,
                    programDurationSeconds = intent.getIntExtra(EngineSkillKeys.PROGRAM_DURATION_SECONDS, 600),
                    programTargetCalories = intent.getIntExtra(EngineSkillKeys.PROGRAM_TARGET_CALORIES, 0).takeIf { it > 0 },
                    resultCalories = inputValue
                )
                else -> EngineLogEntity(
                    mode = mode,
                    intent = EngineIntent.FOR_DISTANCE.name,
                    programDurationSeconds = intent.getIntExtra(EngineSkillKeys.PROGRAM_DURATION_SECONDS, 600),
                    resultDistanceMeters = inputValue
                )
            }
            EngineLogRepository(db.engineLogDao()).log(entity)
        }
    }

    private fun formatHMS(s: Int): String {
        val h = s / 3600
        val m = (s % 3600) / 60
        val ss = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, ss) else "%02d:%02d".format(m, ss)
    }
}
