// app/src/main/java/com/example/safitness/ui/ExerciseLibraryActivity.kt
package com.example.safitness.ui

import android.os.Bundle
import android.view.*
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

    private var currentDay = 1

    private lateinit var spinnerType: Spinner
    private lateinit var spinnerEq: Spinner
    private lateinit var spinnerDay: Spinner
    private lateinit var listLibrary: ListView
    private lateinit var emptyText: TextView
    private lateinit var btnClearFilters: Button

    private val repChoices = listOf(3, 5, 8, 10, 12, 15)
    private val repLabels = repChoices.map { "$it reps" } + "—"
    private val currentReps = mutableMapOf<Long, Int?>()
    private val addedState = mutableMapOf<Long, Boolean>()
    private val requiredState = mutableMapOf<Long, Boolean>() // ⭐ required state

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

        currentDay = intent.getIntExtra("DAY_INDEX", 1).coerceIn(1, 5)

        spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            arrayOf("All") + WorkoutType.values().map { it.name })
        spinnerEq.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            arrayOf("All") + Equipment.values().map { it.name })
        spinnerDay.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Day 1","Day 2","Day 3","Day 4","Day 5"))
        spinnerDay.setSelection(currentDay - 1)

        spinnerType.onItemSelectedListener = objSel { pos ->
            vm.setTypeFilter(if (pos == 0) null else WorkoutType.valueOf(spinnerType.selectedItem as String))
        }
        spinnerEq.onItemSelectedListener = objSel { pos ->
            vm.setEqFilter(if (pos == 0) null else Equipment.valueOf(spinnerEq.selectedItem as String))
        }
        spinnerDay.onItemSelectedListener = objSel { pos ->
            currentDay = (pos + 1).coerceIn(1, 5)
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

            lifecycleScope.launch {
                val repo = Repos.workoutRepository(this@ExerciseLibraryActivity)
                list.forEach { ex ->
                    addedState[ex.id] = repo.isInProgram(currentDay, ex.id)
                    currentReps[ex.id] = repo.selectedTargetReps(currentDay, ex.id)
                    requiredState[ex.id] = repo.requiredFor(currentDay, ex.id)
                }
                listLibrary.adapter = LibraryAdapter(list)
            }
        }
    }

    private fun objSel(block: (Int) -> Unit) =
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = block(pos)
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

    private inner class LibraryAdapter(private val items: List<Exercise>) : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = items[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val holder: VH
            val row = if (convertView == null) {
                val v = LayoutInflater.from(parent?.context)
                    .inflate(R.layout.item_library_row, parent, false)
                holder = VH(v); v.tag = holder; v
            } else {
                (convertView.tag as VH).also { holder = it }; convertView
            }
            holder.bind(getItem(position))
            return row
        }

        private inner class VH(v: View) {
            private val tvTitle = v.findViewById<TextView>(R.id.tvTitle)
            private val tvMeta = v.findViewById<TextView>(R.id.tvMeta)
            private val tvEquip = v.findViewById<TextView>(R.id.tvEquip)
            private val spinnerReps = v.findViewById<Spinner>(R.id.spinnerReps)
            private val btnPrimary = v.findViewById<Button>(R.id.btnPrimary)
            private val btnRequired = v.findViewById<ImageButton>(R.id.btnRequired)

            fun bind(ex: Exercise) {
                tvTitle.text = ex.name
                tvMeta.text = ex.workoutType.name
                tvEquip.text = ex.primaryEquipment.name

                // reps spinner
                val repsAdapter = ArrayAdapter(
                    this@ExerciseLibraryActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    repLabels
                )
                spinnerReps.adapter = repsAdapter
                val pre = currentReps[ex.id]
                val idx = pre?.let { repChoices.indexOf(it) } ?: repLabels.lastIndex
                spinnerReps.setSelection(idx)
                spinnerReps.onItemSelectedListener = objSel { pos ->
                    val chosen = if (pos == repLabels.lastIndex) null else repChoices[pos]
                    currentReps[ex.id] = chosen
                    if (addedState[ex.id] == true) {
                        lifecycleScope.launch {
                            Repos.workoutRepository(this@ExerciseLibraryActivity)
                                .setTargetReps(currentDay, ex.id, chosen)
                        }
                    }
                }

                fun refreshPrimary() {
                    btnPrimary.text = if (addedState[ex.id] == true) "Remove" else "Add"
                }
                fun refreshStar() {
                    val isAdded = addedState[ex.id] == true
                    btnRequired.isEnabled = isAdded
                    val on = (requiredState[ex.id] == true) && isAdded
                    btnRequired.setImageResource(
                        if (on) android.R.drawable.btn_star_big_on
                        else android.R.drawable.btn_star_big_off
                    )
                    btnRequired.alpha = if (isAdded) 1f else 0.35f
                }

                refreshPrimary(); refreshStar()

                btnPrimary.setOnClickListener {
                    lifecycleScope.launch {
                        val repo = Repos.workoutRepository(this@ExerciseLibraryActivity)
                        if (addedState[ex.id] == true) {
                            repo.removeFromDay(currentDay, ex.id)
                            addedState[ex.id] = false
                            Toast.makeText(this@ExerciseLibraryActivity, "Removed ${ex.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            repo.addToDay(currentDay, ex, required = (requiredState[ex.id] ?: true),
                                preferred = ex.primaryEquipment, targetReps = currentReps[ex.id])
                            addedState[ex.id] = true
                            Toast.makeText(this@ExerciseLibraryActivity, "Added ${ex.name}", Toast.LENGTH_SHORT).show()
                        }
                        refreshPrimary(); refreshStar()
                    }
                }

                btnRequired.setOnClickListener {
                    if (addedState[ex.id] != true) {
                        Toast.makeText(this@ExerciseLibraryActivity, "Add the exercise first", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    lifecycleScope.launch {
                        val newVal = !(requiredState[ex.id] ?: true)
                        Repos.workoutRepository(this@ExerciseLibraryActivity)
                            .setRequired(currentDay, ex.id, newVal)
                        requiredState[ex.id] = newVal
                        refreshStar()
                    }
                }
            }
        }
    }
}
