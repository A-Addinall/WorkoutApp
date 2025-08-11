package com.example.safitness.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.safitness.R
import com.example.safitness.core.Modality
import com.example.safitness.data.repo.Repos

class MetconActivity : AppCompatActivity() {

    private val vm: WorkoutViewModel by viewModels {
        WorkoutViewModelFactory(Repos.workoutRepository(this))
    }

    private lateinit var layoutMetconExercises: LinearLayout
    private lateinit var tvWorkoutTitle: TextView
    private lateinit var tvTimer: TextView
    private lateinit var btnStartStop: Button
    private lateinit var btnReset: Button
    private lateinit var btnComplete: Button
    private lateinit var tvLastTime: TextView

    private var dayIndex: Int = 1
    private var workoutName: String = "Metcon"

    private var timer: CountDownTimer? = null
    private var isRunning = false
    private var timeElapsedMs = 0L
    private var startTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metcon)

        dayIndex = intent.getIntExtra("DAY_INDEX", 1).coerceIn(1, 5)
        workoutName = intent.getStringExtra("WORKOUT_NAME") ?: "Day $dayIndex"

        bindViews()
        tvWorkoutTitle.text = "$workoutName – Metcon"
        tvTimer.text = "00:00"

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        // Load metcon list for the day
        vm.setDay(dayIndex)
        vm.programForDay.observe(this) { items ->
            layoutMetconExercises.removeAllViews()
            val metconItems = items.filter { it.exercise.modality == Modality.METCON }
            if (metconItems.isEmpty()) {
                val tv = TextView(this).apply {
                    text = "No metcon exercises for Day $dayIndex."
                    textSize = 16f
                    setPadding(16, 16, 16, 16)
                }
                layoutMetconExercises.addView(tv)
            } else {
                metconItems.forEach { item ->
                    val row = layoutInflater.inflate(
                        R.layout.item_metcon_exercise,
                        layoutMetconExercises,
                        false
                    )
                    row.findViewById<TextView>(R.id.tvExerciseName).text = item.exercise.name
                    row.findViewById<TextView>(R.id.tvRepRange).text =
                        item.targetReps?.let { "$it reps" } ?: "As prescribed"
                    layoutMetconExercises.addView(row)
                }
            }
        }

        // Last metcon time
        vm.lastMetconSeconds.observe(this) { sec ->
            tvLastTime.text = if (sec > 0) "Last time: ${sec / 60}m ${sec % 60}s"
            else "No previous time"
        }

        btnStartStop.setOnClickListener { if (isRunning) stopTimer() else startTimer() }
        btnReset.setOnClickListener { resetTimer() }
        btnComplete.setOnClickListener { completeMetcon() }
    }

    private fun bindViews() {
        layoutMetconExercises = findViewById(R.id.layoutMetconExercises)
        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle)
        tvTimer = findViewById(R.id.tvTimer)
        btnStartStop = findViewById(R.id.btnStartStop)
        btnReset = findViewById(R.id.btnReset)
        btnComplete = findViewById(R.id.btnComplete)
        tvLastTime = findViewById(R.id.tvLastTime)
    }

    private fun startTimer() {
        if (isRunning) return
        startTime = System.currentTimeMillis() - timeElapsedMs
        isRunning = true
        btnStartStop.text = "PAUSE"
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(ms: Long) {
                timeElapsedMs = System.currentTimeMillis() - startTime
                updateTimerDisplay()
            }
            override fun onFinish() {}
        }.also { it.start() }
    }

    private fun stopTimer() {
        if (!isRunning) return
        timer?.cancel()
        isRunning = false
        btnStartStop.text = "START"
    }

    private fun resetTimer() {
        timer?.cancel()
        isRunning = false
        timeElapsedMs = 0L
        startTime = 0L
        btnStartStop.text = "START"
        tvTimer.text = "00:00"
    }

    private fun updateTimerDisplay() {
        val totalSeconds = (timeElapsedMs / 1000).toInt()
        tvTimer.text = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    private fun completeMetcon() {
        if (timeElapsedMs == 0L) {
            Toast.makeText(this, "Please start the timer first!", Toast.LENGTH_SHORT).show()
            return
        }
        if (isRunning) stopTimer()
        val totalSeconds = (timeElapsedMs / 1000).toInt()
        vm.logMetcon(dayIndex, totalSeconds)
        Toast.makeText(this, "Metcon completed in ${totalSeconds / 60}m ${totalSeconds % 60}s!", Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
