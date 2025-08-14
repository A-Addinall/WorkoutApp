package com.example.safitness.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.safitness.R
import com.example.safitness.core.Equipment
import com.example.safitness.data.repo.Repos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

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
        val container: android.view.View,
        val etWeight: EditText,
        val etReps: EditText,
        val rgResult: RadioGroup
    )
    private val setRows = mutableListOf<SetRow>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_detail)

        sessionId = intent.getLongExtra("SESSION_ID", 0L)
        exerciseId = intent.getLongExtra("EXERCISE_ID", 0L)
        exerciseName = intent.getStringExtra("EXERCISE_NAME") ?: ""
        equipmentName = intent.getStringExtra("EQUIPMENT") ?: "BARBELL"
        targetReps = if (intent.hasExtra("TARGET_REPS"))
            intent.getIntExtra("TARGET_REPS", 0).takeIf { it > 0 } else null

        bindViews()
        tvExerciseName.text = exerciseName
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        refreshHeader()

        // Start with one set row
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
        val rgResult = v.findViewById<RadioGroup>(R.id.rgStrengthResult)

        tvSetNumber.text = "Set $setNumber:"
        etReps.setText(targetReps?.toString() ?: "")
        etReps.isEnabled = false

        // Subtle hint colour for weight input
        etWeight.setHintTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))

        val row = SetRow(v, etWeight, etReps, rgResult)
        setRows += row
        layoutSets.addView(v)
    }

    /** Only logs once per set when the user taps Complete. */
    private fun onCompleteExercise() {
        if (setRows.isEmpty()) {
            Toast.makeText(this, "Please add at least one set.", Toast.LENGTH_SHORT).show()
            return
        }

        val equipment = runCatching { Equipment.valueOf(equipmentName) }
            .getOrElse { Equipment.BARBELL }

        // Validate inputs before logging
        for ((index, row) in setRows.withIndex()) {
            if (row.rgResult.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Select Success/Fail for set ${index + 1}.", Toast.LENGTH_SHORT).show()
                return
            }
            val weightVal = row.etWeight.text.toString().toDoubleOrNull()
            val repsVal = targetReps ?: row.etReps.text.toString().toIntOrNull()
            if (weightVal == null || repsVal == null || repsVal <= 0) {
                Toast.makeText(this, "Enter a valid weight; reps are set by the programme.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            var logged = 0
            setRows.forEachIndexed { index, row ->
                val success = when (row.rgResult.checkedRadioButtonId) {
                    R.id.rbSuccess -> true
                    R.id.rbFail -> false
                    else -> false // shouldn't happen due to validation
                }

                val weightVal = row.etWeight.text.toString().toDoubleOrNull() ?: 0.0
                val repsVal = targetReps ?: row.etReps.text.toString().toIntOrNull() ?: 0

                vm.logStrengthSet(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    equipment = equipment,
                    setNumber = index + 1,
                    reps = repsVal,
                    weight = weightVal,
                    rpe = if (success) 6.0 else 9.0,
                    success = success,
                    notes = etNotes.text?.toString()?.ifBlank { null }
                )
                logged++
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ExerciseDetailActivity,
                    "Exercise completed! $logged sets logged.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun refreshHeader() {
        val equipment = runCatching { Equipment.valueOf(equipmentName) }
            .getOrElse { Equipment.BARBELL }

        lifecycleScope.launch(Dispatchers.IO) {
            val last = vm.getLastSuccessfulWeight(exerciseId, equipment, targetReps)
            val suggested = vm.getSuggestedWeight(exerciseId, equipment, targetReps)

            val lastText = last?.let { String.format(Locale.UK, "%.1f kg", it) } ?: "-- kg"
            val suggestedText = suggested?.let { String.format(Locale.UK, "%.1f kg", it) } ?: "-- kg"

            withContext(Dispatchers.Main) {
                tvLastSuccessful.text = "Last successful lift: $lastText"
                tvSuggestedWeight.text = "Suggested: $suggestedText"
            }
        }
    }
}
