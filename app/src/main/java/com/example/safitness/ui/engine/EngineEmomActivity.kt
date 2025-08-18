package com.example.safitness.ui.engine

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.safitness.R
import com.google.android.material.textfield.TextInputEditText

/**
 * Simple EMOM driver for Engine (e.g., "EMOM 20 cal").
 * - Timer counts up in mm:ss.
 * - You can log the target per minute (calories or metres) for reference using a text input.
 * - Reuses the same control pattern as Metcon EMOM.
 */
class EngineEmomActivity : AppCompatActivity() {

    private var running = false
    private var seconds = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_engine_emom)

        val tvTitle = findViewById<TextView>(R.id.tvWorkoutTitle)
        val tvTimer = findViewById<TextView>(R.id.tvTimer)
        val btnStartStop = findViewById<Button>(R.id.btnStartStop)
        val btnReset = findViewById<Button>(R.id.btnReset)
        val ivBack = findViewById<ImageView>(R.id.ivBack)
        val etTarget = findViewById<TextInputEditText>(R.id.etTargetPerMinute)
        ivBack?.setOnClickListener { finish() }

        // Optional: pass unit & target via intent extras
        // ENGINE_EMOM_UNIT: "CALORIES" | "METERS"
        // ENGINE_EMOM_TARGET_PER_MIN: Int
        val unit = (intent.getStringExtra("ENGINE_EMOM_UNIT") ?: "CALORIES").uppercase()
        val target = intent.getIntExtra("ENGINE_EMOM_TARGET_PER_MIN", 20)
        tvTitle?.text = if (unit == "METERS") "Engine – EMOM (metres)" else "Engine – EMOM (cal)"
        etTarget?.setText(target.toString())

        fun render() {
            val m = seconds / 60
            val s = seconds % 60
            tvTimer.text = String.format("%02d:%02d", m, s)
        }

        btnStartStop.setOnClickListener {
            running = !running
            btnStartStop.text = if (running) "STOP" else "START"
        }
        btnReset.setOnClickListener {
            seconds = 0
            running = false
            btnStartStop.text = "START"
            render()
        }

        tvTimer.post(object : Runnable {
            override fun run() {
                if (running) seconds++
                render()
                tvTimer.postDelayed(this, 1000)
            }
        })
    }
}
