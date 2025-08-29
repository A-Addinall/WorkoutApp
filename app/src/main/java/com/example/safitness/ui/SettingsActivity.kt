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
        private const val KEY_EMOM_WORK_SECONDS = "emom_work_seconds"
        private const val KEY_EMOM_SAY_REST = "emom_say_rest"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val etEmomWork = findViewById<EditText>(R.id.etEmomWork)           // NEW
        val swEmomSayRest = findViewById<Switch>(R.id.switchEmomSayRest)   // NEW
        val ivBack = findViewById<ImageView>(R.id.ivBack)
        val etRestTime = findViewById<EditText>(R.id.etRestTime)
        val btnSave = findViewById<Button>(R.id.btnSaveSettings)

        // Load existing
        etRestTime.setText(prefs.getInt(KEY_REST_SECONDS, 120).toString())
        etEmomWork.setText(prefs.getInt(KEY_EMOM_WORK_SECONDS, 40).toString()) // NEW
        swEmomSayRest.isChecked = prefs.getBoolean(KEY_EMOM_SAY_REST, true)    // NEW

        ivBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val restSec = etRestTime.text.toString().toIntOrNull()?.coerceIn(10, 600) ?: 120
            val emomWorkSec = etEmomWork.text.toString().toIntOrNull()?.coerceIn(5, 55) ?: 40 // NEW
            val sayRest = swEmomSayRest.isChecked

            prefs.edit()
                .putInt(KEY_REST_SECONDS, restSec)
                .putInt(KEY_EMOM_WORK_SECONDS, emomWorkSec)   // NEW
                .putBoolean(KEY_EMOM_SAY_REST, sayRest)       // NEW
                .apply()

            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
