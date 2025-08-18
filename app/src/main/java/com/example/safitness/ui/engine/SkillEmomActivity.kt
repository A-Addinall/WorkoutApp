package com.example.safitness.ui.engine

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.safitness.R

class SkillEmomActivity : AppCompatActivity() {

    private var running = false
    private var seconds = 0
    private var success = 0
    private var fail = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skill_emom)

        val tvTimer = findViewById<TextView>(R.id.tvTimer)
        val btnStartStop = findViewById<Button>(R.id.btnStartStop)
        val btnReset = findViewById<Button>(R.id.btnReset)
        val btnSuccess = findViewById<Button>(R.id.btnSuccess)
        val btnFail = findViewById<Button>(R.id.btnFail)
        findViewById<ImageView>(R.id.ivBack)?.setOnClickListener { finish() }

        fun renderTimer() {
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
            success = 0
            fail = 0
            btnStartStop.text = "START"
            btnSuccess.text = "Success: 0"
            btnFail.text = "Fail: 0"
            renderTimer()
        }

        btnSuccess.setOnClickListener { success++; btnSuccess.text = "Success: $success" }
        btnFail.setOnClickListener { fail++; btnFail.text = "Fail: $fail" }

        tvTimer.post(object : Runnable {
            override fun run() {
                if (running) seconds++
                renderTimer()
                tvTimer.postDelayed(this, 1000)
            }
        })
    }
}
