package com.example.safitness.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.safitness.R
import com.example.safitness.core.Equipment
import com.example.safitness.data.repo.Repos
import kotlinx.coroutines.launch

class ExerciseDetailActivity : AppCompatActivity() {

    private val vm: WorkoutViewModel by viewModels {
        WorkoutViewModelFactory(Repos.workoutRepository(this))
    }

    private lateinit var tvExerciseName: TextView
    private lateinit var tvLastSuccessful: TextView
    private lateinit var tvSuggestedWeight: TextView
    private lateinit var layoutSets: LinearLayout
    private lateinit var btnAddSet: Button
    private lateinit var btnCompleteExercise: Button
    private lateinit var etNotes: EditText

    private var sessionId: Long = 0L
    private var exerciseId: Long = 0L
    private var exerciseName: String = ""
    private var equipmentName: String = "BARBELL"
    private var targetReps: Int? = null

    private data class SetRow(
        val container: View,
        val etWeight: EditText,
        val etReps: EditText,
        val btnSuccess: Button,
        val btnFail: Button,
        var success: Boolean? = null
    )
    private val setRows = mutableListOf<SetRow>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_detail)

        sessionId = intent.getLongExtra("SESSION_ID", 0L)
        exerciseId = intent.getLongExtra("EXERCISE_ID", 0L)
        exerciseName = intent.getStringExtra("EXERCISE_NAME") ?: ""
        equipmentName = intent.getStringExtra("EQUIPMENT") ?: "BARBELL"
        targetReps = if (intent.hasExtra("TARGET_REPS")) intent.getIntExtra("TARGET_REPS", 0).takeIf { it > 0 } else null

        bindViews()
        tvExerciseName.text = exerciseName
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        // Prime header info
        refreshHeader()

        // Start with one row
        addNewSet()

        btnAddSet.setOnClickListener { addNewSet() }
        btnCompleteExercise.setOnClickListener { onCompleteExercise() }
    }

    private fun bindViews() {
        tvExerciseName = findViewById(R.id.tvExerciseName)
        tvLastSuccessful = findViewById(R.id.tvLastSuccessful)
        tvSuggestedWeight = findViewById(R.id.tvSuggestedWeight)
        layoutSets = findViewById(R.id.layoutSets)
        btnAddSet = findViewById(R.id.btnAddSet)
        btnCompleteExercise = findViewById(R.id.btnCompleteExercise)
        etNotes = findViewById(R.id.etNotes)
    }

    private fun addNewSet() {
        val setNumber = setRows.size + 1
        val v = layoutInflater.inflate(R.layout.item_set_entry, layoutSets, false)

        val tvSetNumber = v.findViewById<TextView>(R.id.tvSetNumber)
        val etWeight = v.findViewById<EditText>(R.id.etWeight)
        val etReps = v.findViewById<EditText>(R.id.etReps)
        val btnSuccess = v.findViewById<Button>(R.id.btnSuccess)
        val btnFail = v.findViewById<Button>(R.id.btnFail)

        tvSetNumber.text = "Set $setNumber:"
        etReps.setText(targetReps?.toString() ?: "")
        etReps.isEnabled = false

        // Start buttons in neutral grey
        val neutral = ContextCompat.getColor(this, android.R.color.darker_gray)
        btnSuccess.setBackgroundColor(neutral)
        btnFail.setBackgroundColor(neutral)

        val row = SetRow(v, etWeight, etReps, btnSuccess, btnFail, null)
        setRows += row
        layoutSets.addView(v)

        val equipment = runCatching { Equipment.valueOf(equipmentName) }.getOrElse { Equipment.BARBELL }

        btnSuccess.setOnClickListener {
            handleMark(row, isSuccess = true, setNumber = setNumber, equipment = equipment)
        }
        btnFail.setOnClickListener {
            handleMark(row, isSuccess = false, setNumber = setNumber, equipment = equipment)
        }
    }

    private fun handleMark(row: SetRow, isSuccess: Boolean, setNumber: Int, equipment: Equipment) {
        val weightVal = row.etWeight.text.toString().toDoubleOrNull()
        val repsVal = targetReps ?: row.etReps.text.toString().toIntOrNull()

        if (weightVal == null || repsVal == null || repsVal <= 0) {
            Toast.makeText(this, "Enter a valid weight; reps are set by the program.", Toast.LENGTH_SHORT).show()
            return
        }

        // Require exactly one of success/fail per set
        row.success = isSuccess
        val green = ContextCompat.getColor(this, android.R.color.holo_green_light)
        val red = ContextCompat.getColor(this, android.R.color.holo_red_light)
        val neutral = ContextCompat.getColor(this, android.R.color.darker_gray)
        if (isSuccess) {
            row.btnSuccess.setBackgroundColor(green)
            row.btnFail.setBackgroundColor(neutral)
        } else {
            row.btnSuccess.setBackgroundColor(neutral)
            row.btnFail.setBackgroundColor(red)
        }

        // Log without closing the screen
        lifecycleScope.launch {
            vm.logStrengthSet(
                sessionId = sessionId,
                exerciseId = exerciseId,
                equipment = equipment,
                setNumber = setNumber,
                reps = repsVal,
                weight = weightVal,
                rpe = if (isSuccess) 6.0 else 9.0,
                success = isSuccess,
                notes = etNotes.text?.toString()?.ifBlank { null }
            )
            Toast.makeText(this@ExerciseDetailActivity, if (isSuccess) "✅ Set logged" else "❌ Set logged", Toast.LENGTH_SHORT).show()
            refreshHeader() // update last/suggested after each log
        }
    }

    private fun onCompleteExercise() {
        val logged = setRows.count { it.success != null }
        if (logged == 0) {
            Toast.makeText(this, "Please mark at least one set as Success or Fail", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "Exercise completed! $logged sets logged.", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun refreshHeader() {
        val equipment = runCatching { Equipment.valueOf(equipmentName) }.getOrElse { Equipment.BARBELL }
        lifecycleScope.launch {
            val last = vm.getLastSuccessfulWeight(exerciseId, equipment, targetReps)
            tvLastSuccessful.text = if (last != null) "Last successful lift: ${last}kg" else "Last successful lift: --kg"

            val suggested = vm.getSuggestedWeight(exerciseId, equipment, targetReps)
            tvSuggestedWeight.text = "Suggested: ${suggested?.let { String.format("%.1f", it) } ?: "--"}kg"
        }
    }
}
