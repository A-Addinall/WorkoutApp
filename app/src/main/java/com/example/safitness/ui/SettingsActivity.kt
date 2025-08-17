package com.example.safitness.ui

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.safitness.R

class SettingsActivity : AppCompatActivity() {

    private companion object {
        private const val PREFS_NAME = "user_settings"
        private const val KEY_REST_SECONDS = "rest_time_seconds"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        val etRestTime = findViewById<EditText>(R.id.etRestTime)
        val btnSave = findViewById<Button>(R.id.btnSaveSettings)

        // Load existing
        etRestTime.setText(prefs.getInt(KEY_REST_SECONDS, 120).toString())

        ivBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val restSec = etRestTime.text.toString().toIntOrNull()?.coerceIn(10, 600) ?: 120
            prefs.edit().putInt(KEY_REST_SECONDS, restSec).apply()
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
