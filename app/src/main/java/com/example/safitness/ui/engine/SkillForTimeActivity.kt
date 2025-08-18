package com.example.safitness.ui.engine

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.safitness.R
import com.example.safitness.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SkillForTimeActivity : AppCompatActivity() {

    private lateinit var tvWorkoutTitle: TextView
    private lateinit var tvTimer: TextView
    private lateinit var btnStartStop: Button
    private lateinit var btnReset: Button

    // Optional previous label (present in some layouts)
    private var tvLastTime: TextView? = null

    // included card
    private var cardTitle: TextView? = null
    private var cardMeta: TextView? = null
    private var cardComponents: LinearLayout? = null

    private var planId: Long = -1
    private var dayIndex: Int = 1

    private var isRunning = false
    private var isCountdown = false
    private var timeElapsedMs = 0L
    private var startTime = 0L
    private var timer: CountDownTimer? = null
    private var preTimer: CountDownTimer? = null
    private val beeper = com.example.safitness.ui.TimerBeeper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skill_for_time)

        planId = intent.getLongExtra("PLAN_ID", -1L)
        dayIndex = intent.getIntExtra("DAY_INDEX", 1).coerceIn(1, 5)

        // bind views
        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle)
        tvTimer = findViewById(R.id.tvTimer)
        btnStartStop = findViewById(R.id.btnStartStop)
        btnReset = findViewById(R.id.btnReset)

        tvLastTime = findViewById(R.id.tvLastTime) // may be null if not in layout

        cardTitle = findViewById(R.id.tvPlanCardTitle)
        cardMeta = findViewById(R.id.tvPlanMeta)
        cardComponents = findViewById(R.id.layoutPlanComponents)

        tvWorkoutTitle.text = "Skill – For Time"
        tvTimer.text = "00:00"
        findViewById<ImageView>(R.id.ivBack)?.setOnClickListener { finish() }

        // populate card from DB
        populateCardFromPlan(planId)

        btnStartStop.setOnClickListener {
            when {
                isCountdown -> cancelPreCountdown()
                isRunning   -> stopTimer()
                else        -> startPreCountdown()
            }
        }
        btnReset.setOnClickListener { resetAll() }
    }

    private fun populateCardFromPlan(planId: Long) {
        if (planId <= 0L) return
        lifecycleScope.launch {
            val db = AppDatabase.get(this@SkillForTimeActivity)
            val (plan, comps) = withContext(Dispatchers.IO) {
                val p = db.skillPlanDao().getPlans().firstOrNull { it.id == planId }
                val c = p?.let { db.skillPlanDao().getComponents(it.id) }.orEmpty()
                Pair(p, c.sortedBy { it.orderIndex })
            }

            plan ?: return@launch

            tvWorkoutTitle.text = "Skill – ${plan.title}"
            cardTitle?.text = plan.title
            cardMeta?.text = (plan.description ?: "").ifBlank { plan.defaultTestType ?: "" }

            cardComponents?.removeAllViews()
            comps.forEach { c ->
                val line = c.title.ifBlank { c.description ?: "" }
                cardComponents?.addView(TextView(this@SkillForTimeActivity).apply {
                    text = "• $line"
                    textSize = 16f
                    setPadding(0, 4, 0, 4)
                })
            }

            // Previous result label (placeholder until wired to real log)
            tvLastTime?.text = "No previous result"
        }
    }

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
        updateTimerDisplay()
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

    private fun resetAll() {
        preTimer?.cancel(); isCountdown = false
        timer?.cancel(); isRunning = false
        timeElapsedMs = 0L; startTime = 0L
        btnStartStop.text = "START"
        tvTimer.text = "00:00"
    }

    private fun updateTimerDisplay() {
        val totalSeconds = (timeElapsedMs / 1000).toInt()
        tvTimer.text = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    override fun onDestroy() {
        super.onDestroy()
        preTimer?.cancel(); preTimer = null
        timer?.cancel(); timer = null
        beeper.release()
    }
}
