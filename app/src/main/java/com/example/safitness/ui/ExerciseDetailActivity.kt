// app/src/main/java/com/example/safitness/ui/ExerciseDetailActivity.kt
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
    private lateinit var tvLastWeight: TextView
    private lateinit var tvSuggestedWeight: TextView
    private lateinit var layoutSets: LinearLayout
    private lateinit var btnAddSet: Button
    private lateinit var btnCompleteExercise: Button
    private lateinit var etNotes: EditText

    private var sessionId: Long = 0L
    private var exerciseId: Long = 0L
    private var exerciseName: String = ""
    private var equipmentName: String = "BARBELL"
    private var targetReps: Int = 8

    private data class SetRow(
        val container: View,
        val etWeight: EditText,
        val etReps: EditText,
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
        targetReps = intent.getIntExtra("TARGET_REPS", 8)

        bindViews()
        initHeader()
        loadHeaderData()
        addNewSet()
    }

    private fun bindViews() {
        tvExerciseName = findViewById(R.id.tvExerciseName)
        tvLastWeight = findViewById(R.id.tvLastWeight)
        tvSuggestedWeight = findViewById(R.id.tvSuggestedWeight)
        layoutSets = findViewById(R.id.layoutSets)
        btnAddSet = findViewById(R.id.btnAddSet)
        btnCompleteExercise = findViewById(R.id.btnCompleteExercise)
        etNotes = findViewById(R.id.etNotes)

        btnAddSet.setOnClickListener { addNewSet() }
        btnCompleteExercise.setOnClickListener { onCompleteExercise() }
    }

    private fun initHeader() {
        tvExerciseName.text = exerciseName
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }
    }

    private fun loadHeaderData() {
        lifecycleScope.launch {
            val last = vm.getLastSuccessfulWeight(exerciseId)
            tvLastWeight.text = if (last != null) "Last successful lift: ${last}kg" else "No successful lifts yet"
            val suggested = vm.getSuggestedWeight(exerciseId)
            tvSuggestedWeight.text = "Suggested: ${suggested ?: "—"}kg"
        }
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
        etReps.setText(targetReps.toString())

        val row = SetRow(v, etWeight, etReps, null)
        setRows += row

        val equipment = runCatching { Equipment.valueOf(equipmentName) }.getOrElse { Equipment.BARBELL }

        btnSuccess.setOnClickListener {
            val weightVal = etWeight.text.toString().toDoubleOrNull()
            val repsVal = etReps.text.toString().toIntOrNull() ?: 1
            if (weightVal == null || repsVal <= 0) {
                Toast.makeText(this, "Enter valid weight & reps", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                vm.logStrengthSet(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    equipment = equipment,
                    setNumber = setNumber,
                    reps = repsVal,
                    weight = weightVal,
                    rpe = 6.0,
                    success = true,
                    notes = etNotes.text?.toString()?.ifBlank { null }
                )
                row.success = true
                btnSuccess.setBackgroundColor(ContextCompat.getColor(this@ExerciseDetailActivity, android.R.color.holo_green_light))
                btnFail.setBackgroundColor(ContextCompat.getColor(this@ExerciseDetailActivity, android.R.color.darker_gray))
                Toast.makeText(this@ExerciseDetailActivity, "✅ Set logged", Toast.LENGTH_SHORT).show()
                loadHeaderData()
            }
        }

        btnFail.setOnClickListener {
            val weightVal = etWeight.text.toString().toDoubleOrNull()
            val repsVal = etReps.text.toString().toIntOrNull() ?: 1
            if (weightVal == null || repsVal <= 0) {
                Toast.makeText(this, "Enter valid weight & reps", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                vm.logStrengthSet(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    equipment = equipment,
                    setNumber = setNumber,
                    reps = repsVal,
                    weight = weightVal,
                    rpe = 9.0,
                    success = false,
                    notes = etNotes.text?.toString()?.ifBlank { null }
                )
                row.success = false
                btnFail.setBackgroundColor(ContextCompat.getColor(this@ExerciseDetailActivity, android.R.color.holo_red_light))
                btnSuccess.setBackgroundColor(ContextCompat.getColor(this@ExerciseDetailActivity, android.R.color.darker_gray))
                Toast.makeText(this@ExerciseDetailActivity, "❌ Set logged", Toast.LENGTH_SHORT).show()
                loadHeaderData()
            }
        }

        layoutSets.addView(v)
    }

    private fun onCompleteExercise() {
        val logged = setRows.count { it.success != null }
        if (logged == 0) {
            Toast.makeText(this, "Please mark at least one set", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "Exercise completed! $logged sets logged.", Toast.LENGTH_SHORT).show()
        finish()
    }
}
