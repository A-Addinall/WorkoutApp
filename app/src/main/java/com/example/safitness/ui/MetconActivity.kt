// app/src/main/java/com/example/safitness/ui/MetconActivity.kt
package com.example.safitness.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.safitness.R
import com.example.safitness.core.MetconResult
import com.example.safitness.core.Modality
import com.example.safitness.data.repo.Repos

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

    // Legacy container (when the layout still has a vertical list)
    private var legacyExercisesContainer: LinearLayout? = null

    // Intent args
    private var dayIndex: Int = 1
    private var workoutName: String = "Metcon"
    private var planId: Long = -1L

    // Timer state
    private var timer: CountDownTimer? = null
    private var isRunning = false
    private var timeElapsedMs = 0L
    private var startTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metcon)

        dayIndex   = intent.getIntExtra("DAY_INDEX", 1).coerceIn(1, 5)
        workoutName = intent.getStringExtra("WORKOUT_NAME") ?: "Day $dayIndex"
        planId     = intent.getLongExtra("PLAN_ID", -1L)

        bindViews()

        // Title seeded; will be refined once data arrives
        tvWorkoutTitle.text = "$workoutName – Metcon"
        tvTimer.text = "00:00"

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        // Load day context (populates last-metcon labels)
        vm.setDay(dayIndex)

        // If a specific plan was tapped, render *that* plan; otherwise fall back to legacy list.
        if (planId > 0L) {
            vm.planWithComponents(planId).observe(this) { pwc ->
                val plan = pwc.plan
                val comps = pwc.components.sortedBy { it.orderInPlan }

                // Header + card title
                tvWorkoutTitle.text = "$workoutName – ${plan.title}"
                cardTitle?.text = plan.title

                // Prefer card components if present; otherwise render into legacy list container
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
        } else {
            // Legacy fallback: render all metcon exercises for the day
            vm.programForDay.observe(this) { items ->
                val metconItems = items.filter { it.exercise.modality == Modality.METCON }

                // If we’ve got the card, use it; otherwise use the legacy list container.
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
                    // Legacy list container path
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

        // Last metcon time (+ RX/Scaled tag if present) — mirror into card footer if available
        vm.lastMetcon.observe(this) { sum ->
            val label = if (sum != null && sum.timeSeconds > 0) {
                val m = sum.timeSeconds / 60
                val s = sum.timeSeconds % 60
                val tag = sum.metconResult?.name ?: ""
                if (tag.isNotEmpty()) "Last time: ${m}m ${s}s ($tag)" else "Last time: ${m}m ${s}s"
            } else "No previous time"
            tvLastTime.text = label
            cardLast?.text = label
        }

        btnStartStop.setOnClickListener { if (isRunning) stopTimer() else startTimer() }
        btnReset.setOnClickListener { resetTimer() }
        btnComplete.setOnClickListener { completeMetcon() }
    }

    private fun bindViews() {
        // Header + timer + actions
        tvWorkoutTitle = findViewById(R.id.tvWorkoutTitle)
        tvTimer = findViewById(R.id.tvTimer)
        btnStartStop = findViewById(R.id.btnStartStop)
        btnReset = findViewById(R.id.btnReset)
        btnComplete = findViewById(R.id.btnComplete)
        tvLastTime = findViewById(R.id.tvLastTime)

        // Toggle
        rbRx = findViewById(R.id.rbRx)
        rbScaled = findViewById(R.id.rbScaled)

        // Prefer the plan card ids if present in the layout…
        cardTitle = findViewById(R.id.tvPlanCardTitle)
        cardComponents = findViewById(R.id.layoutPlanComponents)
        cardLast = findViewById(R.id.tvPlanLastTime)

        // …otherwise fall back to the legacy container id (older layout variant)
        if (cardComponents == null) {
            legacyExercisesContainer = findViewById(R.id.layoutMetconExercises)
        }
    }

    /* ---------------------------- Timer helpers ---------------------------- */

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

        val result = when {
            rbRx.isChecked -> MetconResult.RX
            rbScaled.isChecked -> MetconResult.SCALED
            else -> null
        }
        if (result == null) {
            Toast.makeText(this, "Please select RX or Scaled.", Toast.LENGTH_SHORT).show()
            return
        }

        // Logs at *day* level in current schema (per-plan logging would require persisting planId)
        vm.logMetcon(dayIndex, totalSeconds, result)

        Toast.makeText(
            this,
            "Metcon completed in ${totalSeconds / 60}m ${totalSeconds % 60}s (${result.name})!",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
