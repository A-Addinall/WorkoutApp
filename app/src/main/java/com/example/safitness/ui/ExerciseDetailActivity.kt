package com.example.safitness.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.safitness.R
import com.example.safitness.core.Equipment
import com.example.safitness.core.PrCelebrationEvent
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
    private lateinit var tvE1rm: TextView

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

    private companion object {
        private const val DEFAULT_SUGGESTED_REPS = 5
    }

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
        tvE1rm = findViewById(R.id.tvE1rm)
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

    /** Preview → Log → Feedback (per set), then close. */
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
            var firstPr: PrCelebrationEvent? = null

            setRows.forEachIndexed { index, row ->
                val success = when (row.rgResult.checkedRadioButtonId) {
                    R.id.rbSuccess -> true
                    R.id.rbFail -> false
                    else -> false
                }

                val weightVal = row.etWeight.text.toString().toDoubleOrNull() ?: 0.0
                val repsVal = targetReps ?: row.etReps.text.toString().toIntOrNull() ?: 0

                if (success && repsVal > 0 && weightVal > 0.0) {
                    // 1) PREVIEW (no DB writes)
                    val preview = vm.previewPrEvent(
                        exerciseId = exerciseId,
                        equipment = equipment,
                        reps = repsVal,
                        weight = weightVal
                    )

                    // store first PR to show as modal after the loop
                    if (firstPr == null && preview != null) firstPr = preview

                    // 2) LOG (DB write)
                    vm.logStrengthSet(
                        sessionId = sessionId,
                        exerciseId = exerciseId,
                        equipment = equipment,
                        setNumber = index + 1,
                        reps = repsVal,
                        weight = weightVal,
                        rpe = 6.0,
                        success = true,
                        notes = etNotes.text?.toString()?.ifBlank { null }
                    )
                } else {
                    // Log failure
                    vm.logStrengthSet(
                        sessionId = sessionId,
                        exerciseId = exerciseId,
                        equipment = equipment,
                        setNumber = index + 1,
                        reps = repsVal,
                        weight = weightVal,
                        rpe = 9.0,
                        success = false,
                        notes = etNotes.text?.toString()?.ifBlank { null }
                    )
                }

                logged++
            }

            withContext(Dispatchers.Main) {
                if (firstPr != null) {
                    showPrDialog(firstPr!!)
                    // finish is called after dialog dismiss
                } else {
                    showCenteredToast("✅ Exercise completed! $logged sets logged.")
                    finish()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun refreshHeader() {
        val equipment = runCatching { Equipment.valueOf(equipmentName) }
            .getOrElse { Equipment.BARBELL }

        lifecycleScope.launch(Dispatchers.IO) {
            val last = vm.getLastSuccessfulWeight(exerciseId, equipment, targetReps)
            val reps = targetReps ?: DEFAULT_SUGGESTED_REPS
            val suggested = vm.suggestNextLoadKg(exerciseId, equipment, reps)

            // calculate e1RM if we have a last successful set
            val e1rm = if (last != null && targetReps != null) {
                com.example.safitness.core.estimateOneRepMax(last, targetReps!!)
            } else null

            val lastText = last?.let { String.format(Locale.UK, "%.1f kg", it) } ?: "-- kg"
            val e1rmText = e1rm?.let { String.format(Locale.UK, "%.1f kg", it) } ?: "-- kg"
            val suggestedText = suggested?.let { String.format(Locale.UK, "%.1f kg", it) } ?: "-- kg"

            withContext(Dispatchers.Main) {
                tvLastSuccessful.text = "Last successful lift: $lastText"
                tvE1rm.text = "e1RM: $e1rmText"
                tvSuggestedWeight.text = "Suggested: $suggestedText"
            }
        }
    }

    // ======== Feedback helpers ========

    private fun showCenteredToast(msg: String) {
        val t = Toast.makeText(this, msg, Toast.LENGTH_SHORT)
        t.setGravity(Gravity.CENTER, 0, 0)
        t.show()
    }

    /** Simple centered modal; user taps OK to dismiss, then we close the screen. */
    private fun showPrDialog(e: com.example.safitness.core.PrCelebrationEvent) {
        val view = layoutInflater.inflate(R.layout.dialog_pr, null)
        val titleTv = view.findViewById<TextView>(R.id.tvPrTitle)
        val bodyTv = view.findViewById<TextView>(R.id.tvPrBody)
        val btnNice = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNice)

        val title = if (e.isHardPr) {
            val repsLabel = "${e.reps ?: 1}RM"
            "New $repsLabel 🎉"
        } else {
            "New e1RM 🎉"
        }

        val body = if (e.isHardPr) {
            val newW = e.newWeightKg?.let { fmtKg(it) } ?: "—"
            val prevW = e.prevWeightKg?.let { fmtKg(it) } ?: "—"
            val e1rmNow = fmtKg(e.newE1rmKg)
            "$newW (prev $prevW)\nNew e1RM: $e1rmNow"
        } else {
            val e1rmNow = fmtKg(e.newE1rmKg)
            val delta = e.prevE1rmKg?.let { e.newE1rmKg - it }
            val deltaText = delta?.let { fmtKg(it) } ?: "—"
            "New e1RM: $e1rmNow\nChange: $deltaText"
        }

        titleTv.text = title
        bodyTv.text = body

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            btnNice.isAllCaps = false
            btnNice.setOnClickListener {
                dialog.dismiss()
                finish()
            }
        }

        dialog.show()
    }

    private fun fmtKg(value: Double): String =
        String.format(Locale.UK, "%.1f kg", value)
}
