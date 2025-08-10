package com.example.safitness.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.safitness.R

class PersonalRecordsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_placeholder)
        findViewById<android.widget.ImageView>(R.id.ivBack).setOnClickListener { finish() }
    }
}
