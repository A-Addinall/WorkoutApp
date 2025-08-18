package com.example.safitness.ui.engine

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.safitness.core.EngineIntent
import com.example.safitness.core.EngineSkillKeys
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.EngineLogEntity
import com.example.safitness.data.repo.EngineLogRepository
import com.example.safitness.databinding.ActivityEngineForTimeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EngineForTimeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEngineForTimeBinding
    private val scope = CoroutineScope(Dispatchers.Main)

    private var timerRunning = false
    private var elapsedSec = 0
    private var ticker: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEngineForTimeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra(EngineSkillKeys.TITLE) ?: "Engine — For Time"
        val mode = intent.getStringExtra(EngineSkillKeys.ENGINE_MODE) ?: "ROW"
        val distance = intent.getIntExtra(EngineSkillKeys.PROGRAM_DISTANCE_METERS, 2000)

        binding.txtTitle.text = title
        binding.txtProgram.text = "Target: ${distance}m"

        binding.btnStart.setOnClickListener { startTimer() }
        binding.btnPause.setOnClickListener { pauseTimer() }
        binding.btnComplete.setOnClickListener {
            saveResult(mode, distance, elapsedSec)
            finish()
        }
    }

    private fun startTimer() {
        if (timerRunning) return
        timerRunning = true
        ticker = object : Runnable {
            override fun run() {
                elapsedSec += 1
                binding.txtTimer.text = formatHMS(elapsedSec)
                binding.txtPace.text = pacePer500(elapsedSec)
                binding.txtTimer.postDelayed(this, 1000)
            }
        }.also { binding.txtTimer.post(it) }
    }

    private fun pauseTimer() {
        timerRunning = false
        ticker?.let { binding.txtTimer.removeCallbacks(it) }
    }

    private fun saveResult(mode: String, distance: Int, timeSec: Int) {
        val db = AppDatabase.get(this)
        CoroutineScope(Dispatchers.IO).launch {
            EngineLogRepository(db.engineLogDao()).log(
                EngineLogEntity(
                    mode = mode,
                    intent = EngineIntent.FOR_TIME.name,
                    programDistanceMeters = distance,
                    resultTimeSeconds = timeSec
                )
            )
        }
    }

    private fun formatHMS(s: Int): String {
        val h = s / 3600
        val m = (s % 3600) / 60
        val ss = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, ss) else "%02d:%02d".format(m, ss)
    }

    private fun pacePer500(timeSec: Int): String {
        val distance = intent.getIntExtra(EngineSkillKeys.PROGRAM_DISTANCE_METERS, 2000).coerceAtLeast(1)
        val secPer500 = (timeSec.toDouble() / distance) * 500.0
        val mm = (secPer500 / 60.0).toInt()
        val ss = (secPer500 % 60.0).toInt()
        return "Pace: %d:%02d /500m".format(mm, ss)
    }
}
