package com.example.safitness.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.safitness.R
import com.example.safitness.core.MetconResult
import com.example.safitness.data.repo.Repos
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import kotlin.math.floor

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
    private var remainingMs = 0L

    private var preTimer: CountDownTimer? = null
    private val beeper = TimerBeeper()
    private var lastWarnSecond = -1
    private var lastMinuteMark = -1

    private enum class TimerPhase { IDLE, PRECOUNT, RUNNING }
    private var phase: TimerPhase = TimerPhase.IDLE
    private var allowReseed = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metcon_emom)

        dayIndex = intent.getIntExtra("DAY_INDEX", 1).coerceIn(1, 5)
        planId = intent.getLongExtra("PLAN_ID", -1L)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        bindViews()
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.title = "EMOM"

        vm.planWithComponents(planId).observe(this) { pwc ->
            val mins = (pwc?.plan?.durationMinutes ?: 20).coerceAtLeast(1)
            durationSeconds = mins * 60

            // Seed ONLY when idle; never while counting down or running
            if (allowReseed && phase == TimerPhase.IDLE) {
                remainingMs = durationSeconds * 1000L
                updateTimer()
            }

            // --- NEW: bind plan card like the other screens ---
            // Header
            findViewById<TextView?>(R.id.tvWorkoutTitle)?.text =
                "EMOM – ${pwc?.plan?.title ?: "EMOM"}"

            // Card title
            findViewById<TextView?>(R.id.tvPlanCardTitle)?.text =
                pwc?.plan?.title ?: "EMOM"

            // Card components
            val comps = pwc?.components?.sortedBy { it.orderInPlan }.orEmpty()
            val cont = findViewById<LinearLayout?>(R.id.layoutPlanComponents)
            cont?.removeAllViews()
            comps.forEach { c ->
                cont?.addView(TextView(this).apply {
                    text = "• ${c.text}"
                    textSize = 16f
                    setPadding(0, 4, 0, 4)
                })
            }
            // (We don’t show last time for EMOM, so tvPlanLastTime stays as-is/hidden)
        }

        btnStartStop.setOnClickListener {
            when (phase) {
                TimerPhase.PRECOUNT -> cancelPreCountdown()
                TimerPhase.RUNNING  -> pause()
                TimerPhase.IDLE     -> startPreCountdown()
            }
        }
        btnReset.setOnClickListener { resetAll() }
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

    /* ---------------------------- Pre-start countdown (5s) ---------------------------- */

    private fun startPreCountdown() {
        if (phase != TimerPhase.IDLE) return
        phase = TimerPhase.PRECOUNT
        allowReseed = false
        btnStartStop.text = "CANCEL"

        tvTimer.text = "00:05" // stable priming

        preTimer?.cancel()
        preTimer = object : CountDownTimer(5_000, 1_000) {
            override fun onTick(ms: Long) {
                val secLeft = ((ms + 999) / 1000).toInt() // ceil
                tvTimer.text = String.format("%02d:%02d", 0, secLeft)
                beeper.countdownPip()
            }
            override fun onFinish() {
                preTimer = null
                if (phase != TimerPhase.PRECOUNT) return
                beeper.finalBuzz()
                startMainCountdown()
            }
        }.also { it.start() }
    }

    private fun cancelPreCountdown() {
        preTimer?.cancel()
        preTimer = null
        phase = TimerPhase.IDLE
        btnStartStop.text = "START"
        updateTimer()
    }

    /* ---------------------------- MAIN EMOM countdown ---------------------------- */

    private fun startMainCountdown() {
        if (phase != TimerPhase.PRECOUNT) return
        phase = TimerPhase.RUNNING
        btnStartStop.text = "PAUSE"

        timer?.cancel()
        timer = object : CountDownTimer(remainingMs, 1_000) {
            override fun onTick(ms: Long) {
                remainingMs = ms
                updateTimer()
                maybeBeepMinute()
                maybeBeepCountdown()
            }
            override fun onFinish() {
                timer = null
                phase = TimerPhase.IDLE
                remainingMs = 0L
                btnStartStop.text = "START"
                updateTimer()
                beeper.finalBuzz()
                Toast.makeText(this@MetconEmomActivity, "Time!", Toast.LENGTH_SHORT).show()
            }
        }.also { it.start() }
    }

    private fun pause() {
        timer?.cancel(); timer = null
        phase = TimerPhase.IDLE
        btnStartStop.text = "START"
    }

    private fun resetAll() {
        preTimer?.cancel(); preTimer = null
        timer?.cancel(); timer = null
        phase = TimerPhase.IDLE
        allowReseed = true
        lastWarnSecond = -1
        lastMinuteMark = -1
        remainingMs = durationSeconds * 1000L
        updateTimer()
    }

    private fun updateTimer() {
        val total = (remainingMs / 1000).toInt()
        tvTimer.text = String.format("%02d:%02d", total / 60, total % 60)
    }

    /** Beep each minute boundary while running (except at 0). */
    private fun maybeBeepMinute() {
        if (phase != TimerPhase.RUNNING) return
        val elapsed = durationSeconds - (remainingMs / 1000).toInt()
        val minute = floor(elapsed / 60.0).toInt()
        if (elapsed > 0 && minute != lastMinuteMark) {
            lastMinuteMark = minute
            beeper.minuteTick()
        }
    }

    /** Beep at 3, 2, 1 seconds remaining (main countdown only). */
    private fun maybeBeepCountdown() {
        if (phase != TimerPhase.RUNNING) return
        val secLeft = (remainingMs / 1000).toInt()
        if (secLeft in 1..3 && secLeft != lastWarnSecond) {
            lastWarnSecond = secLeft
            beeper.countdownPip()
        }
    }

    private fun complete() {
        if (phase == TimerPhase.PRECOUNT) {
            cancelPreCountdown()
            Toast.makeText(this, "Countdown cancelled.", Toast.LENGTH_SHORT).show()
            return
        }
        if (phase == TimerPhase.RUNNING) pause()
        val result = when {
            rbRx.isChecked -> MetconResult.RX
            rbScaled.isChecked -> MetconResult.SCALED
            else -> null
        } ?: run {
            Toast.makeText(this, "Please select RX or Scaled.", Toast.LENGTH_SHORT).show()
            return
        }

        // NEW: read epochDay on demand
        val epochDay = intent.getLongExtra(
            MainActivity.EXTRA_DATE_EPOCH_DAY,
            java.time.LocalDate.now().toEpochDay()
        )

        lifecycleScope.launch {
            // CHANGED: date-first logging
            vm.logMetconEmomForDate(
                epochDay = epochDay,
                planId = planId,
                durationSeconds = durationSeconds,
                intervalsCompleted = durationSeconds / 60,
                result = result
            )
            beeper.finalBuzz()
            Toast.makeText(this@MetconEmomActivity, "EMOM logged ($result).", Toast.LENGTH_LONG).show()
            finish()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        preTimer?.cancel(); preTimer = null
        timer?.cancel(); timer = null
        beeper.release()
    }
}
