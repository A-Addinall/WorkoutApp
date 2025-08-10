package com.example.safitness.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.safitness.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        findViewById<android.widget.ImageView>(R.id.ivBack).setOnClickListener { finish() }
    }
}
