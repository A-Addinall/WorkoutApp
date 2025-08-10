// app/src/main/java/com/example/safitness/ui/ExerciseLibraryActivity.kt
package com.example.safitness.ui

import android.os.Bundle
import android.view.LayoutInflater
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
    private lateinit var spinnerDay: Spinner
    private lateinit var listLibrary: ListView
    private lateinit var emptyText: TextView
    private lateinit var btnClearFilters: Button

    private val repChoices = listOf(3, 5, 8, 10, 12, 15)
    private val repLabels = repChoices.map { "$it reps" } + "—"
    private val currentReps = mutableMapOf<Long, Int?>() // exerciseId -> reps or null
    private val addedState = mutableMapOf<Long, Boolean>() // exerciseId -> is in program

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_library)

        spinnerType = findViewById(R.id.spinnerType)
        spinnerEq = findViewById(R.id.spinnerEq)
        spinnerDay = findViewById(R.id.spinnerDay)
        listLibrary = findViewById(R.id.listLibrary)
        emptyText = findViewById(R.id.tvEmpty)
        btnClearFilters = findViewById(R.id.btnClearFilters)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        // Spinners
        spinnerType.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("All") + WorkoutType.values().map { it.name }
        )
        spinnerEq.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("All") + Equipment.values().map { it.name }
        )
        spinnerDay.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Day 1", "Day 2", "Day 3", "Day 4", "Day 5")
        )
        spinnerDay.setSelection(day - 1)

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

            // Preload per-row state from DB
            lifecycleScope.launch {
                val repo = Repos.workoutRepository(this@ExerciseLibraryActivity)
                list.forEach { ex ->
                    addedState[ex.id] = repo.isInProgram(day, ex.id)
                    currentReps[ex.id] = repo.selectedTargetReps(day, ex.id)
                }
                listLibrary.adapter = LibraryAdapter(list)
            }
        }
    }

    private inner class LibraryAdapter(
        private val items: List<Exercise>
    ) : BaseAdapter() {

        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = items[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val holder: VH
            val row = if (convertView == null) {
                val v = LayoutInflater.from(parent?.context)
                    .inflate(R.layout.item_library_row, parent, false)
                holder = VH(v)
                v.tag = holder
                v
            } else {
                (convertView.tag as VH).also { holder = it }
                convertView
            }

            val ex = getItem(position)
            holder.bind(ex)
            return row
        }

        private inner class VH(v: View) {
            private val tvTitle = v.findViewById<TextView>(R.id.tvTitle)
            private val tvMeta = v.findViewById<TextView>(R.id.tvMeta)
            private val tvEquip = v.findViewById<TextView>(R.id.tvEquip)
            private val spinnerReps = v.findViewById<Spinner>(R.id.spinnerReps)
            private val btnPrimary = v.findViewById<Button>(R.id.btnPrimary)
            // star remains unused for now
            // private val btnRequired = v.findViewById<ImageButton>(R.id.btnRequired)

            fun bind(ex: Exercise) {
                tvTitle.text = ex.name
                tvMeta.text = ex.workoutType.name
                tvEquip.text = ex.primaryEquipment.name

                // Spinner setup (3/5/8/10/12/15/—)
                val repsAdapter = ArrayAdapter(
                    this@ExerciseLibraryActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    repLabels
                )
                spinnerReps.adapter = repsAdapter

                val preSel = currentReps[ex.id]
                val selIndex = preSel?.let { repChoices.indexOf(it) } ?: repLabels.lastIndex
                spinnerReps.setSelection(selIndex)

                spinnerReps.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?, view: View?, pos: Int, id: Long
                    ) {
                        val chosen = if (pos == repLabels.lastIndex) null else repChoices[pos]
                        currentReps[ex.id] = chosen
                        // Persist if already added
                        if (addedState[ex.id] == true) {
                            lifecycleScope.launch {
                                Repos.workoutRepository(this@ExerciseLibraryActivity)
                                    .setTargetReps(day, ex.id, chosen)
                            }
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                // Button label/state
                fun refreshBtn() {
                    btnPrimary.text = if (addedState[ex.id] == true) "Remove" else "Add"
                }
                refreshBtn()

                btnPrimary.setOnClickListener {
                    lifecycleScope.launch {
                        val repo = Repos.workoutRepository(this@ExerciseLibraryActivity)
                        val isAdded = addedState[ex.id] == true
                        if (isAdded) {
                            repo.removeFromDay(day, ex.id)
                            addedState[ex.id] = false
                            Toast.makeText(this@ExerciseLibraryActivity, "Removed ${ex.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            repo.addToDay(
                                day = day,
                                exercise = ex,
                                required = true,
                                preferred = ex.primaryEquipment,
                                targetReps = currentReps[ex.id]
                            )
                            addedState[ex.id] = true
                            Toast.makeText(this@ExerciseLibraryActivity, "Added ${ex.name}", Toast.LENGTH_SHORT).show()
                        }
                        refreshBtn()
                    }
                }
            }
        }
    }
}
