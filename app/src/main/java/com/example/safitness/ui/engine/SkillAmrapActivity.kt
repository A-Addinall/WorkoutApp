package com.example.safitness.ui.engine

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.safitness.R
import com.example.safitness.data.db.AppDatabase
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SkillAmrapActivity : AppCompatActivity() {

    private lateinit var tvWorkoutTitle: TextView
    private lateinit var tvTimer: TextView
    private lateinit var btnStartStop: Button
    private lateinit var btnReset: Button
    private lateinit var btnComplete: Button

    // Optional previous label if present
    private var tvLastTime: TextView? = null

    // Included card
    private var cardTitle: TextView? = null
    private var cardMeta: TextView? = null
    private var cardComponents: LinearLayout? = null

    // AMRAP inputs
    private lateinit var tilRounds: TextInputLayout
    private lateinit var tilReps: TextInputLayout
    private lateinit var etRounds: TextInputEditText
    private lateinit var etReps: TextInputEditText

    private var planId: Long = -1
    private var dayIndex: Int = 1
    private var durationSeconds: Int = 20 * 60

    private var preTimer: CountDownTimer? = null
    private var timer: CountDownTimer? = null
    private val beeper = com.example.safitness.ui.TimerBeeper()
    private var phase: Phase = Phase.IDLE
    private var remainingMs = 0L
    private var allowReseed = true

    private enum class Phase { IDLE, PRE, RUN }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skill_amrap)

        planId = intent.getLongExtra("PLAN_ID", -1L)
        dayIndex = intent.getIntExtra("DAY_INDEX", 1).coerceIn(1, 5)
        durationSeconds = intent.getIntExtra("DURATION_SECONDS", durationSeconds)

        // bind base views
        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle)
        tvTimer = findViewById(R.id.tvTimer)
        btnStartStop = findViewById(R.id.btnStartStop)
        btnReset = findViewById(R.id.btnReset)
        btnComplete = findViewById(R.id.btnComplete)

        tvLastTime = findViewById(R.id.tvLastTime)

        // AMRAP inputs (replace attempts row if that exists)
        tilRounds = TextInputLayout(this).apply { hint = "Rounds" }
        tilReps = TextInputLayout(this).apply { hint = "Extra reps" }
        etRounds = TextInputEditText(tilRounds.context).apply { setText("0") }
        etReps = TextInputEditText(tilReps.context).apply { setText("0") }
        tilRounds.addView(etRounds)
        tilReps.addView(etReps)
        findViewById<LinearLayout?>(R.id.rowAttempts)?.let { row ->
            row.removeAllViews()
            row.addView(tilRounds, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(tilReps, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        // included card
        cardTitle = findViewById(R.id.tvPlanCardTitle)
        cardMeta = findViewById(R.id.tvPlanMeta)
        cardComponents = findViewById(R.id.layoutPlanComponents)

        tvWorkoutTitle.text = "Skill – AMRAP"
        tvTimer.text = "00:00"
        findViewById<ImageView>(R.id.ivBack)?.setOnClickListener { finish() }

        populateCardFromPlan(planId)

        btnStartStop.setOnClickListener {
            when (phase) {
                Phase.IDLE -> startPreCountdown()
                Phase.PRE  -> cancelPreCountdown()
                Phase.RUN  -> pause()
            }
        }
        btnReset.setOnClickListener { resetAll() }
        btnComplete.setOnClickListener { completeAmrap() }
    }

    private fun populateCardFromPlan(planId: Long) {
        if (planId <= 0L) return
        lifecycleScope.launch {
            val db = AppDatabase.get(this@SkillAmrapActivity)
            val (plan, comps) = withContext(Dispatchers.IO) {
                val p = db.skillPlanDao().getPlans().firstOrNull { it.id == planId }
                val c = p?.let { db.skillPlanDao().getComponents(it.id) }.orEmpty()
                Pair(p, c.sortedBy { it.orderIndex })
            }

            plan ?: return@launch

            val mins = (plan.targetDurationSeconds ?: durationSeconds).coerceAtLeast(60) / 60
            durationSeconds = mins * 60
            if (allowReseed && phase == Phase.IDLE) {
                remainingMs = durationSeconds * 1000L
                updateTimer()
            }

            tvWorkoutTitle.text = "Skill – ${plan.title}"
            cardTitle?.text = plan.title
            cardMeta?.text = (plan.description ?: "").ifBlank { plan.defaultTestType ?: "" }

            cardComponents?.removeAllViews()
            comps.forEach { c ->
                val line = c.title.ifBlank { c.description ?: "" }
                cardComponents?.addView(TextView(this@SkillAmrapActivity).apply {
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
        if (phase != Phase.IDLE) return
        phase = Phase.PRE
        btnStartStop.text = "CANCEL"
        preTimer = object : CountDownTimer(5_000, 1_000) {
            override fun onTick(ms: Long) {
                val secLeft = (ms / 1000).toInt() + 1
                tvTimer.text = String.format("%02d:%02d", 0, secLeft)
                beeper.countdownPip()
            }
            override fun onFinish() {
                beeper.finalBuzz()
                phase = Phase.RUN
                startMainCountdown()
            }
        }.also { it.start() }
    }

    private fun cancelPreCountdown() {
        preTimer?.cancel(); preTimer = null
        phase = Phase.IDLE
        btnStartStop.text = "START"
        updateTimer()
    }

    private fun startMainCountdown() {
        btnStartStop.text = "PAUSE"
        timer?.cancel()
        timer = object : CountDownTimer(remainingMs, 1_000) {
            override fun onTick(ms: Long) {
                remainingMs = ms
                updateTimer()
                val secLeft = (remainingMs / 1000).toInt()
                if (secLeft in 1..3) beeper.countdownPip()
            }
            override fun onFinish() {
                timer = null
                phase = Phase.IDLE
                remainingMs = 0L
                btnStartStop.text = "START"
                updateTimer()
                beeper.finalBuzz()
                Toast.makeText(this@SkillAmrapActivity, "Time!", Toast.LENGTH_SHORT).show()
            }
        }.also { it.start() }
    }

    private fun pause() {
        timer?.cancel(); timer = null
        phase = Phase.IDLE
        btnStartStop.text = "START"
    }

    private fun resetAll() {
        preTimer?.cancel(); preTimer = null
        timer?.cancel(); timer = null
        phase = Phase.IDLE
        allowReseed = true
        remainingMs = durationSeconds * 1000L
        updateTimer()
        etRounds.setText("0")
        etReps.setText("0")
    }

    private fun updateTimer() {
        val total = (remainingMs / 1000).toInt()
        tvTimer.text = String.format("%02d:%02d", total / 60, total % 60)
    }

    private fun completeAmrap() {
        if (phase == Phase.PRE) {
            cancelPreCountdown()
            Toast.makeText(this, "Countdown cancelled.", Toast.LENGTH_SHORT).show()
            return
        }
        if (phase == Phase.RUN) pause()
        val rounds = etRounds.text?.toString()?.toIntOrNull() ?: 0
        val reps = etReps.text?.toString()?.toIntOrNull() ?: 0
        beeper.finalBuzz()
        Toast.makeText(this, "Skill AMRAP logged: $rounds + $reps", Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        preTimer?.cancel(); preTimer = null
        timer?.cancel(); timer = null
        beeper.release()
    }
}
