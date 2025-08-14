package com.example.safitness.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.safitness.R
import com.example.safitness.core.MetconResult
import com.example.safitness.data.repo.Repos
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class MetconEmomActivity : AppCompatActivity() {

    private val vm: WorkoutViewModel by viewModels {
        WorkoutViewModelFactory(Repos.workoutRepository(this))
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvTimer: TextView
    private lateinit var btnStartStop: Button
    private lateinit var btnReset: Button
    private lateinit var btnComplete: Button
    private lateinit var rbRx: RadioButton
    private lateinit var rbScaled: RadioButton

    private var dayIndex: Int = 1
    private var planId: Long = -1L
    private var durationSeconds: Int = 20 * 60

    private var timer: CountDownTimer? = null
    private var isRunning = false
    private var remainingMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metcon_emom)

        dayIndex = intent.getIntExtra("DAY_INDEX", 1).coerceIn(1, 5)
        planId = intent.getLongExtra("PLAN_ID", -1L)

        bindViews()
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.title = "EMOM"

        vm.planWithComponents(planId).observe(this) { pwc ->
            val mins = (pwc?.plan?.durationMinutes ?: 20).coerceAtLeast(1)
            durationSeconds = mins * 60
            remainingMs = durationSeconds * 1000L
            updateTimer()
        }

        btnStartStop.setOnClickListener { if (isRunning) pause() else start() }
        btnReset.setOnClickListener { reset() }
        btnComplete.setOnClickListener { complete() }
    }

    private fun bindViews() {
        toolbar = findViewById(R.id.toolbar)
        tvTimer = findViewById(R.id.tvTimer)
        btnStartStop = findViewById(R.id.btnStartStop)
        btnReset = findViewById(R.id.btnReset)
        btnComplete = findViewById(R.id.btnComplete)
        rbRx = findViewById(R.id.rbRx)
        rbScaled = findViewById(R.id.rbScaled)
    }

    private fun start() {
        if (isRunning || remainingMs <= 0L) return
        isRunning = true
        btnStartStop.text = "PAUSE"
        timer = object : CountDownTimer(remainingMs, 1000) {
            override fun onTick(ms: Long) { remainingMs = ms; updateTimer() }
            override fun onFinish() {
                isRunning = false
                remainingMs = 0L
                btnStartStop.text = "START"
                updateTimer()
                Toast.makeText(this@MetconEmomActivity, "Time!", Toast.LENGTH_SHORT).show()
            }
        }.also { it.start() }
    }

    private fun pause() {
        timer?.cancel()
        isRunning = false
        btnStartStop.text = "START"
    }

    private fun reset() {
        pause()
        remainingMs = durationSeconds * 1000L
        updateTimer()
    }

    private fun updateTimer() {
        val total = (remainingMs / 1000).toInt()
        tvTimer.text = String.format("%02d:%02d", total / 60, total % 60)
    }

    private fun complete() {
        if (isRunning) pause()
        val result = when {
            rbRx.isChecked -> MetconResult.RX
            rbScaled.isChecked -> MetconResult.SCALED
            else -> null
        }
        if (result == null) {
            Toast.makeText(this, "Please select RX or Scaled.", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            vm.logMetconEmom(
                day = dayIndex,
                planId = planId,
                durationSeconds = durationSeconds,
                intervalsCompleted = durationSeconds / 60, // 1 per minute
                result = result
            )
            Toast.makeText(this@MetconEmomActivity, "EMOM logged ($result).", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
