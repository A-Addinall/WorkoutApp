package com.example.safitness.ui.engine

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.safitness.R
import com.google.android.material.textfield.TextInputEditText

class EngineAccumulationActivity : AppCompatActivity() {

    private var running = false
    private var seconds = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_engine_accumulation)

        val tvTimer = findViewById<TextView>(R.id.tvTimer)
        val btnStartStop = findViewById<Button>(R.id.btnStartStop)
        val btnReset = findViewById<Button>(R.id.btnReset)
        val etValue = findViewById<TextInputEditText>(R.id.etValue)
        findViewById<ImageView>(R.id.ivBack)?.setOnClickListener { finish() }

        // Switch units via Intent extra: ENGINE_TARGET = "METERS" | "CALORIES"
        val unit = intent.getStringExtra("ENGINE_TARGET") ?: "METERS"
        findViewById<TextView>(R.id.tvUnitLabel)?.text =
            if (unit == "CALORIES") "Calories" else "Meters"

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
            etValue.setText("")
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
