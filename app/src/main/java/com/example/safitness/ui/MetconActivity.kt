package com.example.safitness.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.safitness.R
import com.example.safitness.core.MetconResult
import com.example.safitness.core.Modality
import com.example.safitness.data.repo.Repos
import kotlinx.coroutines.launch

class MetconActivity : AppCompatActivity() {

    private val vm: WorkoutViewModel by viewModels {
        WorkoutViewModelFactory(Repos.workoutRepository(this))
    }

    // Header + timer controls
    private lateinit var tvWorkoutTitle: TextView
    private lateinit var tvTimer: TextView
    private lateinit var btnStartStop: Button
    private lateinit var btnReset: Button
    private lateinit var btnComplete: Button
    private lateinit var tvLastTime: TextView

    // RX / Scaled toggle
    private lateinit var rbRx: RadioButton
    private lateinit var rbScaled: RadioButton

    // Preferred: views from item_metcon_plan_card.xml (when the layout includes the card)
    private var cardTitle: TextView? = null
    private var cardComponents: LinearLayout? = null
    private var cardLast: TextView? = null

    // Legacy container (older layout variant)
    private var legacyExercisesContainer: LinearLayout? = null

    // Intent args
    private var dayIndex: Int = 1
    private var workoutName: String = "Metcon"
    private var planId: Long = -1L

    // Timer state (count-up)
    private var timer: CountDownTimer? = null
    private var isRunning = false
    private var timeElapsedMs = 0L
    private var startTime = 0L

    // NEW: pre-start 5s countdown
    private var preTimer: CountDownTimer? = null
    private var isCountdown = false
    private val beeper = TimerBeeper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metcon)

        dayIndex   = intent.getIntExtra("DAY_INDEX", 1).coerceIn(1, 5)
        workoutName = intent.getStringExtra("WORKOUT_NAME") ?: "Day $dayIndex"
        planId     = intent.getLongExtra("PLAN_ID", -1L)

        bindViews()

        tvWorkoutTitle.text = "$workoutName – Metcon"
        tvTimer.text = "00:00"

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        vm.setDay(dayIndex)

        if (planId > 0L) {
            vm.planWithComponents(planId).observe(this) { pwc ->
                val plan = pwc.plan
                val comps = pwc.components.sortedBy { it.orderInPlan }

                tvWorkoutTitle.text = "$workoutName – ${plan.title}"
                cardTitle?.text = plan.title

                cardComponents?.let { container ->
                    container.removeAllViews()
                    comps.forEach { c ->
                        container.addView(TextView(this).apply {
                            text = "• ${c.text}"
                            textSize = 16f
                            setPadding(0, 4, 0, 4)
                        })
                    }
                } ?: run {
                    legacyExercisesContainer?.let { container ->
                        container.removeAllViews()
                        comps.forEach { c ->
                            container.addView(TextView(this).apply {
                                text = "• ${c.text}"
                                textSize = 16f
                                setPadding(0, 4, 0, 4)
                            })
                        }
                    }
                }
            }

            vm.lastMetconForPlan(planId).observe(this) { last ->
                val label = if (last != null && last.type == "FOR_TIME" && (last.timeSeconds ?: 0) > 0) {
                    val sec = last.timeSeconds!!
                    val m = sec / 60; val s = sec % 60
                    val tag = last.result
                    "Last time: ${m}m ${s}s ($tag)"
                } else "No previous time"
                tvLastTime.text = label
                cardLast?.text = label
            }

        } else {
            vm.programForDay.observe(this) { items ->
                val metconItems = items.filter { it.exercise.modality == Modality.METCON }
                if (cardComponents != null) {
                    cardTitle?.text = "Metcon"
                    cardComponents!!.removeAllViews()
                    if (metconItems.isEmpty()) {
                        cardComponents!!.addView(TextView(this).apply {
                            text = "No metcon exercises for Day $dayIndex."
                            textSize = 16f
                            setPadding(16, 16, 16, 16)
                        })
                    } else {
                        metconItems.forEach { item ->
                            cardComponents!!.addView(TextView(this).apply {
                                text = "• ${item.exercise.name}"
                                textSize = 16f
                                setPadding(0, 4, 0, 4)
                            })
                        }
                    }
                } else {
                    legacyExercisesContainer?.let { container ->
                        container.removeAllViews()
                        if (metconItems.isEmpty()) {
                            container.addView(TextView(this).apply {
                                text = "No metcon exercises for Day $dayIndex."
                                textSize = 16f
                                setPadding(16, 16, 16, 16)
                            })
                        } else {
                            metconItems.forEach { item ->
                                val row = layoutInflater.inflate(
                                    R.layout.item_metcon_exercise,
                                    container,
                                    false
                                )
                                row.findViewById<TextView>(R.id.tvExerciseName).text = item.exercise.name
                                row.findViewById<TextView>(R.id.tvRepRange).text =
                                    item.targetReps?.let { "$it reps" } ?: "As prescribed"
                                container.addView(row)
                            }
                        }
                    }
                }
            }
        }

        btnStartStop.setOnClickListener {
            when {
                isCountdown -> cancelPreCountdown()
                isRunning   -> stopTimer()
                else        -> startPreCountdown()
            }
        }
        btnReset.setOnClickListener { resetAll() }
        btnComplete.setOnClickListener { completeMetcon() }
    }

    private fun bindViews() {
        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle)
        tvTimer = findViewById(R.id.tvTimer)
        btnStartStop = findViewById(R.id.btnStartStop)
        btnReset = findViewById(R.id.btnReset)
        btnComplete = findViewById(R.id.btnComplete)
        tvLastTime = findViewById(R.id.tvLastTime)

        rbRx = findViewById(R.id.rbRx)
        rbScaled = findViewById(R.id.rbScaled)

        cardTitle = findViewById(R.id.tvPlanCardTitle)
        cardComponents = findViewById(R.id.layoutPlanComponents)
        cardLast = findViewById(R.id.tvPlanLastTime)

        if (cardComponents == null) {
            legacyExercisesContainer = findViewById(R.id.layoutMetconExercises)
        }
    }

    /* ---------------------------- Pre-start countdown ---------------------------- */

    private fun startPreCountdown() {
        if (isRunning || isCountdown) return
        isCountdown = true
        btnStartStop.text = "CANCEL"
        preTimer = object : CountDownTimer(5_000, 1_000) {
            override fun onTick(ms: Long) {
                val secLeft = (ms / 1000).toInt() + 1
                tvTimer.text = String.format("%02d:%02d", 0, secLeft)
                beeper.countdownPip()
            }
            override fun onFinish() {
                beeper.finalBuzz()
                isCountdown = false
                startTimer()
            }
        }.also { it.start() }
    }

    private fun cancelPreCountdown() {
        preTimer?.cancel()
        isCountdown = false
        btnStartStop.text = "START"
        updateTimerDisplay() // restore current elapsed
    }

    /* ---------------------------- Main timer (count-up) ---------------------------- */

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

    private fun resetAll() {
        preTimer?.cancel()
        isCountdown = false
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
        if (isCountdown) {
            cancelPreCountdown()
            Toast.makeText(this, "Countdown cancelled.", Toast.LENGTH_SHORT).show()
            return
        }
        if (timeElapsedMs == 0L) {
            Toast.makeText(this, "Please start the timer first!", Toast.LENGTH_SHORT).show()
            return
        }
        if (isRunning) stopTimer()
        val totalSeconds = (timeElapsedMs / 1000).toInt()

        val result = when {
            rbRx.isChecked -> MetconResult.RX
            rbScaled.isChecked -> MetconResult.SCALED
            else -> null
        }
        if (result == null) {
            Toast.makeText(this, "Please select RX or Scaled.", Toast.LENGTH_SHORT).show()
            return
        }

        if (planId <= 0L) {
            Toast.makeText(this, "No plan ID — cannot log plan-scoped time.", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            vm.logMetconForTime(dayIndex, planId, totalSeconds, result)
            beeper.finalBuzz()
            Toast.makeText(
                this@MetconActivity,
                "Metcon completed in ${totalSeconds / 60}m ${totalSeconds % 60}s (${result.name})!",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        preTimer?.cancel()
        timer?.cancel()
        beeper.release()
    }
}
