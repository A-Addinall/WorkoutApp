package com.example.safitness.ui

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.safitness.R
import com.example.safitness.core.MetconResult
import com.example.safitness.data.repo.Repos
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import kotlin.math.max

class MetconAmrapActivity : AppCompatActivity() {

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

    // Direct input fields (replacing +/- buttons)
    private lateinit var tilRounds: TextInputLayout
    private lateinit var etRounds: TextInputEditText
    private lateinit var tilReps: TextInputLayout
    private lateinit var etReps: TextInputEditText

    private var dayIndex: Int = 1
    private var planId: Long = -1L
    private var durationSeconds: Int = 20 * 60

    private var timer: CountDownTimer? = null
    private var remainingMs = 0L

    private var rounds = 0
    private var extraReps = 0

    private var preTimer: CountDownTimer? = null
    private val beeper = TimerBeeper()
    private var lastWarnSecond = -1 // main countdown 3..1

    private enum class TimerPhase { IDLE, PRECOUNT, RUNNING }
    private var phase: TimerPhase = TimerPhase.IDLE
    private var allowReseed = true  // only reseed from LiveData when IDLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metcon_amrap)

        dayIndex = intent.getIntExtra("DAY_INDEX", 1).coerceIn(1, 5)
        planId = intent.getLongExtra("PLAN_ID", -1L)

        bindViews()
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.title = "AMRAP"

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        vm.planWithComponents(planId).observe(this) { pwc ->
            val mins = (pwc?.plan?.durationMinutes ?: 20).coerceAtLeast(1)
            durationSeconds = mins * 60

            if (allowReseed && phase == TimerPhase.IDLE) {
                remainingMs = durationSeconds * 1000L
                updateTimer()
            }

            // Plan card binding
            findViewById<TextView?>(R.id.tvPlanCardTitle)?.text = pwc?.plan?.title ?: "AMRAP"
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
        }

        setupScoreInputs()

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

        tilRounds = findViewById(R.id.tilRounds)
        etRounds = findViewById(R.id.etRounds)
        tilReps = findViewById(R.id.tilReps)
        etReps = findViewById(R.id.etReps)

        // Initialise to 0 to match defaults in layout
        etRounds.setText(rounds.toString())
        etReps.setText(extraReps.toString())
    }

    /** Replace +/- with validated numeric input **/
    private fun setupScoreInputs() {
        fun parseIntOrNull(cs: CharSequence?): Int? =
            cs?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.toIntOrNull()

        etRounds.doAfterTextChanged { text ->
            val v = parseIntOrNull(text)
            when {
                v == null || v < 0 -> {
                    tilRounds.error = getString(R.string.enter_valid_rounds) // "Enter valid rounds (0+)"
                }
                else -> {
                    tilRounds.error = null
                    rounds = v
                }
            }
        }

        etReps.doAfterTextChanged { text ->
            val v = parseIntOrNull(text)
            when {
                v == null || v < 0 -> {
                    tilReps.error = getString(R.string.enter_valid_reps) // "Enter valid reps (0+)"
                }
                else -> {
                    tilReps.error = null
                    extraReps = v
                }
            }
        }

        etReps.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus()
                hideKeyboard(v)
                true
            } else false
        }
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

    /* ---------------------------- MAIN countdown (AMRAP) ---------------------------- */

    private fun startMainCountdown() {
        if (phase != TimerPhase.PRECOUNT) return
        phase = TimerPhase.RUNNING
        btnStartStop.text = "PAUSE"

        timer?.cancel()
        timer = object : CountDownTimer(remainingMs, 1_000) {
            override fun onTick(ms: Long) {
                remainingMs = ms
                updateTimer()
                maybeBeepCountdown()
            }
            override fun onFinish() {
                timer = null
                phase = TimerPhase.IDLE
                remainingMs = 0L
                btnStartStop.text = "START"
                updateTimer()
                beeper.finalBuzz()
                Toast.makeText(this@MetconAmrapActivity, "Time!", Toast.LENGTH_SHORT).show()
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
        remainingMs = durationSeconds * 1000L
        updateTimer()
        // Reset inputs to 0 for clarity
        rounds = 0; extraReps = 0
        etRounds.setText("0"); etReps.setText("0")
        tilRounds.error = null; tilReps.error = null
    }

    private fun updateTimer() {
        val total = (remainingMs / 1000).toInt()
        tvTimer.text = String.format("%02d:%02d", total / 60, total % 60)
    }

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

        // Validate numbers before logging
        val valid = (rounds >= 0 && extraReps >= 0)
        if (!valid) {
            Toast.makeText(this, "Enter valid rounds/reps (0+).", Toast.LENGTH_SHORT).show()
            return
        }

        // NEW: read epochDay at the point of logging (no field/import churn)
        val epochDay = intent.getLongExtra(
            MainActivity.EXTRA_DATE_EPOCH_DAY,
            java.time.LocalDate.now().toEpochDay()
        )

        lifecycleScope.launch {
            // CHANGED: date-first logging
            vm.logMetconAmrapForDate(
                epochDay = epochDay,
                planId = planId,
                durationSeconds = durationSeconds,
                rounds = rounds,
                extraReps = extraReps,
                result = result
            )
            beeper.finalBuzz()
            Toast.makeText(
                this@MetconAmrapActivity,
                "Logged: $rounds rounds + $extraReps reps ($result).",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }


    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        preTimer?.cancel(); preTimer = null
        timer?.cancel(); timer = null
        beeper.release()
    }
}
