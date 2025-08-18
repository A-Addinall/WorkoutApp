package com.example.safitness.ui.engine

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.safitness.R
import com.example.safitness.data.db.AppDatabase
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

class EngineAccumulationActivity : AppCompatActivity() {

    private lateinit var tvWorkoutTitle: TextView
    private lateinit var tvTimer: TextView
    private lateinit var btnStartStop: Button
    private lateinit var btnReset: Button
    private lateinit var btnComplete: Button
    private lateinit var tvLastTime: TextView
    private lateinit var tvUnitLabel: TextView
    private lateinit var etValue: TextInputEditText

    private var cardTitle: TextView? = null
    private var cardMeta: TextView? = null
    private var cardComponents: LinearLayout? = null

    private var planId: Long = -1L
    private var dayIndex: Int = 1
    private var durationSeconds: Int = 0
    private var targetUnit: String = "METERS" // or "CALORIES"

    private var isRunning = false
    private var isCountdown = false
    private var timeElapsedMs = 0L
    private var startTime = 0L
    private var timer: CountDownTimer? = null
    private var preTimer: CountDownTimer? = null
    private val beeper = com.example.safitness.ui.TimerBeeper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_engine_accumulation)

        planId = intent.getLongExtra("PLAN_ID", -1L)
        dayIndex = intent.getIntExtra("DAY_INDEX", 1).coerceIn(1, 5)
        durationSeconds = intent.getIntExtra("DURATION_SECONDS", 0)
        targetUnit = intent.getStringExtra("ENGINE_TARGET") ?: targetUnit

        bindViews()
        tvWorkoutTitle.text = "Engine – Accumulation"
        tvTimer.text = "00:00"
        tvUnitLabel.text = if (targetUnit == "CALORIES") "Calories" else "Meters"
        findViewById<ImageView>(R.id.ivBack)?.setOnClickListener { finish() }

        populateCardFromPlan(planId)
        tvLastTime.text = loadLastLabel()

        btnStartStop.setOnClickListener {
            when {
                isCountdown -> cancelPreCountdown()
                isRunning   -> stopTimer()
                else        -> startPreCountdown()
            }
        }
        btnReset.setOnClickListener { resetAll() }
        btnComplete.setOnClickListener { completeAccumulation() }
    }

    private fun bindViews() {
        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle)
        tvTimer = findViewById(R.id.tvTimer)
        tvLastTime = findViewById(R.id.tvLastTime)
        btnStartStop = findViewById(R.id.btnStartStop)
        btnReset = findViewById(R.id.btnReset)
        btnComplete = findViewById(R.id.btnComplete)
        tvUnitLabel = findViewById(R.id.tvUnitLabel)
        etValue = findViewById(R.id.etValue)
        cardTitle = findViewById(R.id.tvPlanCardTitle)
        cardMeta = findViewById(R.id.tvPlanMeta)
        cardComponents = findViewById(R.id.layoutPlanComponents)
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
        timer = object : CountDownTimer(Long.MAX_VALUE, 1_000) {
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
        etValue.setText("")
    }

    private fun updateTimerDisplay() {
        val totalSeconds = (timeElapsedMs / 1000).toInt()
        tvTimer.text = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
    }

    private fun completeAccumulation() {
        if (isCountdown) {
            cancelPreCountdown()
            Toast.makeText(this, "Countdown cancelled.", Toast.LENGTH_SHORT).show()
            return
        }
        if (isRunning) stopTimer()
        val v = etValue.text?.toString()?.trim().orEmpty().ifBlank { "0" }
        val label = if (targetUnit == "CALORIES") "$v cal" else "$v m"
        saveLast(label)
        beeper.finalBuzz()
        Toast.makeText(this, "Engine (accumulation) complete.", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun populateCardFromPlan(planId: Long) {
        if (planId <= 0L) return
        lifecycleScope.launch {
            val db = AppDatabase.get(this@EngineAccumulationActivity)
            val plan = withContext(Dispatchers.IO) {
                db.enginePlanDao().getPlans().firstOrNull { it.id == planId }
            } ?: return@launch

            tvWorkoutTitle.text = "Engine – ${plan.title}"
            cardTitle?.text = plan.title
            cardMeta?.text = buildEngineMeta(
                plan.intent,
                plan.programDistanceMeters,
                plan.programTargetCalories,
                plan.programDurationSeconds
            )

            val comps = withContext(Dispatchers.IO) { db.enginePlanDao().getComponents(plan.id) }
                .sortedBy { it.orderIndex }
            cardComponents?.removeAllViews()
            comps.forEach { c ->
                val line = c.title.ifBlank { c.description ?: "" }
                cardComponents?.addView(TextView(this@EngineAccumulationActivity).apply {
                    text = "• $line"
                    textSize = 16f
                    setPadding(0, 4, 0, 4)
                })
            }
        }
    }

    private fun buildEngineMeta(
        intent: String?,
        meters: Int?,
        calories: Int?,
        seconds: Int?
    ): String {
        fun pretty(s: String) = s.replace('_', ' ').lowercase().replaceFirstChar(Char::titlecase)
        val bits = mutableListOf<String>()
        when (intent) {
            "FOR_TIME" -> if ((meters ?: 0) > 0) bits += "${meters} m"
            "FOR_DISTANCE" -> if ((seconds ?: 0) > 0) bits += "${seconds!! / 60} min"
            "FOR_CALORIES" -> {
                if ((calories ?: 0) > 0) bits += "${calories} cal"
                if ((seconds ?: 0) > 0) bits += "${seconds!! / 60} min"
            }
        }
        if (!intent.isNullOrBlank()) bits += pretty(intent)
        return bits.joinToString(" • ")
    }

    // --------- "Last result" via SharedPreferences ---------

    private fun prefs() = getSharedPreferences("last_results", Context.MODE_PRIVATE)
    private fun saveLast(label: String) {
        prefs().edit().putString("engine_acc_last_$planId", label).apply()
    }
    private fun loadLastLabel(): String =
        prefs().getString("engine_acc_last_$planId", null)?.let { "Last: $it" } ?: "No previous result"

    override fun onDestroy() {
        super.onDestroy()
        preTimer?.cancel(); preTimer = null
        timer?.cancel(); timer = null
        beeper.release()
    }
}
