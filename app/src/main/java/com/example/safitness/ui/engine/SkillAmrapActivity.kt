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
import kotlin.math.floor

class SkillAmrapActivity : AppCompatActivity() {

    private lateinit var tvWorkoutTitle: TextView
    private lateinit var tvTimer: TextView
    private lateinit var btnStartStop: Button
    private lateinit var btnReset: Button
    private lateinit var btnComplete: Button
    private lateinit var tilRounds: TextInputLayout
    private lateinit var tilReps: TextInputLayout
    private lateinit var etRounds: TextInputEditText
    private lateinit var etReps: TextInputEditText

    // Included card
    private var cardTitle: TextView? = null
    private var cardMeta: TextView? = null
    private var cardComponents: LinearLayout? = null

    private var planId: Long = -1
    private var dayIndex: Int = 1
    private var durationSeconds: Int = 20 * 60

    private var preTimer: CountDownTimer? = null
    private var timer: CountDownTimer? = null
    private val beeper = com.example.safitness.ui.TimerBeeper()
    private var lastWarnSecond = -1
    private var phase: Phase = Phase.IDLE
    private var remainingMs = 0L
    private var allowReseed = true

    private var rounds = 0
    private var extraReps = 0

    private enum class Phase { IDLE, PRE, RUN }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skill_amrap) // includes activity_skill_emom layout

        planId = intent.getLongExtra("PLAN_ID", -1L)
        dayIndex = intent.getIntExtra("DAY_INDEX", 1).coerceIn(1, 5)
        durationSeconds = intent.getIntExtra("DURATION_SECONDS", durationSeconds)

        // The included EMOM layout defines these ids
        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle)
        tvTimer = findViewById(R.id.tvTimer)
        btnStartStop = findViewById(R.id.btnStartStop)
        btnReset = findViewById(R.id.btnReset)
        btnComplete = findViewById(R.id.btnComplete)

        // Add AMRAP inputs
        tilRounds = TextInputLayout(this).apply { hint = "Rounds" }
        tilReps = TextInputLayout(this).apply { hint = "Extra reps" }
        etRounds = TextInputEditText(tilRounds.context).apply { setText("0") }
        etReps = TextInputEditText(tilReps.context).apply { setText("0") }
        tilRounds.addView(etRounds)
        tilReps.addView(etReps)
        // place these above the ScrollView card (re-using rowAttempts spot)
        val rowAttempts = findViewById<LinearLayout>(R.id.rowAttempts)
        rowAttempts.removeAllViews()
        rowAttempts.addView(tilRounds, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        rowAttempts.addView(tilReps, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        cardTitle = findViewById(R.id.tvPlanCardTitle)
        cardMeta = findViewById(R.id.tvPlanMeta)
        cardComponents = findViewById(R.id.layoutPlanComponents)

        tvWorkoutTitle.text = "Skill – AMRAP"
        tvTimer.text = "00:00"
        findViewById<ImageView>(R.id.ivBack)?.setOnClickListener { finish() }

        lifecycleScope.launch {
            val db = AppDatabase.get(this@SkillAmrapActivity)
            val pwc = withContext(Dispatchers.IO) {
                val p = db.skillPlanDao().getPlans().firstOrNull { it.id == planId }
                val comps = p?.let { db.skillPlanDao().getComponents(it.id) }.orEmpty()
                Pair(p, comps.sortedBy { it.orderIndex })
            }
            val plan = pwc.first
            if (plan != null) {
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
                pwc.second.forEach { c ->
                    val line = c.title.ifBlank { c.description ?: "" }
                    cardComponents?.addView(TextView(this@SkillAmrapActivity).apply {
                        text = "• $line"
                        textSize = 16f
                        setPadding(0, 4, 0, 4)
                    })
                }
            }
        }

        btnStartStop.setOnClickListener {
            when (phase) {
                Phase.IDLE -> startPreCountdown()
                Phase.PRE -> cancelPreCountdown()
                Phase.RUN -> pause()
            }
        }
        btnReset.setOnClickListener { resetAll() }
        btnComplete.setOnClickListener { complete() }
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

    private fun cancelPreCountdown() { preTimer?.cancel(); preTimer = null; phase = Phase.IDLE; btnStartStop.text = "START"; updateTimer() }
    private fun startMainCountdown() {
        btnStartStop.text = "PAUSE"
        timer?.cancel()
        timer = object : CountDownTimer(remainingMs, 1_000) {
            override fun onTick(ms: Long) { remainingMs = ms; updateTimer(); maybeBeepCountdown() }
            override fun onFinish() {
                timer = null; phase = Phase.IDLE; remainingMs = 0L
                btnStartStop.text = "START"; updateTimer(); beeper.finalBuzz()
                Toast.makeText(this@SkillAmrapActivity, "Time!", Toast.LENGTH_SHORT).show()
            }
        }.also { it.start() }
    }
    private fun pause() { timer?.cancel(); timer = null; phase = Phase.IDLE; btnStartStop.text = "START" }
    private fun resetAll() {
        preTimer?.cancel(); preTimer = null
        timer?.cancel(); timer = null
        phase = Phase.IDLE; allowReseed = true; remainingMs = durationSeconds * 1000L
        rounds = 0; extraReps = 0; etRounds.setText("0"); etReps.setText("0")
        updateTimer()
    }

    private fun updateTimer() {
        val total = (remainingMs / 1000).toInt()
        tvTimer.text = String.format("%02d:%02d", total / 60, total % 60)
    }
    private fun maybeBeepCountdown() {
        if (phase != Phase.RUN) return
        val secLeft = (remainingMs / 1000).toInt()
        if (secLeft in 1..3) beeper.countdownPip()
    }
    private fun complete() {
        if (phase == Phase.PRE) { cancelPreCountdown(); Toast.makeText(this, "Countdown cancelled.", Toast.LENGTH_SHORT).show(); return }
        if (phase == Phase.RUN) pause()
        rounds = etRounds.text?.toString()?.toIntOrNull() ?: 0
        extraReps = etReps.text?.toString()?.toIntOrNull() ?: 0
        beeper.finalBuzz()
        Toast.makeText(this, "Skill AMRAP logged: $rounds + $extraReps", Toast.LENGTH_LONG).show()
        finish()
    }
}
