// app/src/main/java/com/example/safitness/ui/ExerciseLibraryActivity.kt
package com.example.safitness.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.safitness.R
import com.example.safitness.core.Equipment
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.entities.Exercise
import com.example.safitness.data.repo.Repos
import kotlinx.coroutines.launch

class ExerciseLibraryActivity : AppCompatActivity() {

    private val vm: LibraryViewModel by viewModels {
        LibraryViewModelFactory(Repos.workoutRepository(this))
    }
    private val day get() = intent.getIntExtra("DAY_INDEX", 1).coerceIn(1, 5)

    private lateinit var spinnerType: Spinner
    private lateinit var spinnerEq: Spinner
    private lateinit var listLibrary: ListView
    private lateinit var emptyText: TextView
    private lateinit var btnClearFilters: Button

    private val repChoices = listOf(3, 5, 8, 10, 12, 15)
    private val currentReps = mutableMapOf<Long, Int?>() // exerciseId -> reps or null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_library)

        spinnerType = findViewById(R.id.spinnerType)
        spinnerEq = findViewById(R.id.spinnerEq)
        listLibrary = findViewById(R.id.listLibrary)
        emptyText = findViewById(R.id.tvEmpty)
        btnClearFilters = findViewById(R.id.btnClearFilters)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        // Spinners
        spinnerType.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            (arrayOf("All") + WorkoutType.values().map { it.name })
        )
        spinnerEq.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            (arrayOf("All") + Equipment.values().map { it.name })
        )

        spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val type = if (pos == 0) null else WorkoutType.valueOf(spinnerType.selectedItem as String)
                vm.setTypeFilter(type)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        spinnerEq.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val eq = if (pos == 0) null else Equipment.valueOf(spinnerEq.selectedItem as String)
                vm.setEqFilter(eq)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        btnClearFilters.setOnClickListener {
            spinnerType.setSelection(0)
            spinnerEq.setSelection(0)
        }

        vm.exercises.observe(this) { list ->
            if (list.isNullOrEmpty()) {
                emptyText.visibility = View.VISIBLE
                listLibrary.adapter = null
                return@observe
            }
            emptyText.visibility = View.GONE

            val adapter = object : ArrayAdapter<Exercise>(
                this,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                list
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val row = super.getView(position, convertView, parent)
                    val ex = getItem(position)!!
                    row.findViewById<TextView>(android.R.id.text1).text = ex.name
                    val repsStr = currentReps[ex.id]?.let { "$it reps" }
                        ?: "tap = add/remove, long‑press = reps"
                    row.findViewById<TextView>(android.R.id.text2).text = repsStr
                    return row
                }
            }
            listLibrary.adapter = adapter

            // Tap: toggle add/remove
            listLibrary.setOnItemClickListener { _, _, pos, _ ->
                val ex = list[pos]
                lifecycleScope.launch {
                    val repo = Repos.workoutRepository(this@ExerciseLibraryActivity)
                    // Try to remove first; if not present, add
                    val removed = runCatching { repo.removeFromDay(day, ex.id) }.isSuccess
                    if (removed) {
                        Toast.makeText(this@ExerciseLibraryActivity, "Removed ${ex.name}", Toast.LENGTH_SHORT).show()
                    } else {
                        repo.addToDay(
                            day = day,
                            exercise = ex,
                            required = true,
                            preferred = ex.primaryEquipment,
                            targetReps = currentReps[ex.id]
                        )
                        Toast.makeText(this@ExerciseLibraryActivity, "Added ${ex.name}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Long‑press: cycle reps (3 → 5 → … → 15 → none)
            listLibrary.setOnItemLongClickListener { _, _, pos, _ ->
                val ex = list[pos]
                val current = currentReps[ex.id]
                val next = when (current) {
                    null -> repChoices.first()
                    repChoices.last() -> null
                    else -> repChoices[repChoices.indexOf(current) + 1]
                }
                currentReps[ex.id] = next
                (listLibrary.adapter as ArrayAdapter<*>).notifyDataSetChanged()

                lifecycleScope.launch {
                    Repos.workoutRepository(this@ExerciseLibraryActivity)
                        .setTargetReps(day, ex.id, next)
                }
                true
            }
        }
    }
}
