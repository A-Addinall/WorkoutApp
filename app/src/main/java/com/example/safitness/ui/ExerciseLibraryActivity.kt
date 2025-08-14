// app/src/main/java/com/example/safitness/ui/ExerciseLibraryActivity.kt
package com.example.safitness.ui

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    // Toggle between Exercises (strength list) and Metcons (plans)
    private lateinit var radioExercises: RadioButton
    private lateinit var radioMetcons: RadioButton
    private lateinit var rvMetconPlans: RecyclerView
    private lateinit var metconAdapter: MetconPlanAdapter

    // Strength reps support
    private val repChoices = listOf(3, 5, 8, 10, 12, 15)
    private val repLabels = repChoices.map { "$it reps" } + "—" // "—" means unset/null
    private val currentReps = mutableMapOf<Long, Int?>()

    // Selection state per exercise
    private val addedState = mutableMapOf<Long, Boolean>()

    // Track metcon membership for current day
    private var metconAddedIds: Set<Long> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_library)

        spinnerType = findViewById(R.id.spinnerType)
        spinnerEq = findViewById(R.id.spinnerEq)
        spinnerDay = findViewById(R.id.spinnerDay)
        listLibrary = findViewById(R.id.listLibrary)
        emptyText = findViewById(R.id.tvEmpty)
        btnClearFilters = findViewById(R.id.btnClearFilters)

        radioExercises = findViewById(R.id.rbExercises)
        radioMetcons = findViewById(R.id.rbMetcons)
        rvMetconPlans = findViewById(R.id.rvMetconPlans)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        currentDay = intent.getIntExtra("DAY_INDEX", 1).coerceIn(1, 5)

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
        spinnerDay.setSelection(currentDay - 1)

        spinnerType.onItemSelectedListener = objSel { _ ->
            vm.setTypeFilter(
                if (spinnerType.selectedItem == "All") null
                else WorkoutType.valueOf(spinnerType.selectedItem as String)
            )
        }
        spinnerEq.onItemSelectedListener = objSel { _ ->
            vm.setEqFilter(
                if (spinnerEq.selectedItem == "All") null
                else Equipment.valueOf(spinnerEq.selectedItem as String)
            )
        }
        spinnerDay.onItemSelectedListener = objSel { pos ->
            currentDay = (pos + 1).coerceIn(1, 5)
            vm.setMetconDay(currentDay) // keep metcon list in sync
            refreshExerciseStates()
        }

        btnClearFilters.setOnClickListener {
            spinnerType.setSelection(0)
            spinnerEq.setSelection(0)
        }

        // Exercises (Strength) list
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
                }
                listLibrary.adapter = LibraryAdapter(list)
            }
        }

        // Metcon plans
        rvMetconPlans.layoutManager = LinearLayoutManager(this)
        metconAdapter = MetconPlanAdapter(
            onPrimary = { plan, isAdded ->
                if (isAdded) {
                    vm.removeMetconFromDay(currentDay, plan.id)
                } else {
                    val order = metconAddedIds.size
                    vm.addMetconToDay(currentDay, plan.id, required = true, order = order)
                }
            }
        )
        rvMetconPlans.adapter = metconAdapter

        vm.metconPlans.observe(this) { plans ->
            metconAdapter.submit(plans ?: emptyList(), metconAddedIds)
        }
        vm.metconPlanIdsForDay.observe(this) { idSet ->
            metconAddedIds = idSet ?: emptySet()
            metconAdapter.updateMembership(metconAddedIds)
        }
        vm.setMetconDay(currentDay)

        // Toggle behaviour
        fun applyMode() {
            val showMetcons = radioMetcons.isChecked
            rvMetconPlans.visibility = if (showMetcons) View.VISIBLE else View.GONE
            listLibrary.visibility = if (showMetcons) View.GONE else View.VISIBLE
            emptyText.visibility = View.GONE
        }
        radioExercises.setOnCheckedChangeListener { _, _ -> applyMode() }
        radioMetcons.setOnCheckedChangeListener { _, _ -> applyMode() }
        radioExercises.isChecked = true
        radioMetcons.isChecked = false
        applyMode()
    }

    private fun refreshExerciseStates() {
        val adapter = listLibrary.adapter as? LibraryAdapter ?: return
        lifecycleScope.launch {
            val repo = Repos.workoutRepository(this@ExerciseLibraryActivity)
            adapter.items.forEach { ex ->
                addedState[ex.id] = repo.isInProgram(currentDay, ex.id)
                // keep currentReps as-is; repo already persisted any chosen reps
            }
            (listLibrary.adapter as BaseAdapter).notifyDataSetChanged()
        }
    }

    private fun objSel(block: (Int) -> Unit) =
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) =
                block(position)
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

    private inner class LibraryAdapter(val items: List<Exercise>) : BaseAdapter() {
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
            private val spinnerReps = v.findViewById<Spinner>(R.id.spinnerReps)
            private val btnPrimary = v.findViewById<Button>(R.id.btnPrimary)

            fun bind(ex: Exercise) {
                tvTitle.text = ex.name
                // Optional subtitle; keep subtle like metcon
                tvMeta.text = ex.workoutType.name

                // Reps spinner (defaults to saved reps or unset)
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
                    btnPrimary.text = if (addedState[ex.id] == true) "Remove" else "Add to Day"
                }
                refreshPrimary()

                btnPrimary.setOnClickListener {
                    lifecycleScope.launch {
                        val repo = Repos.workoutRepository(this@ExerciseLibraryActivity)
                        if (addedState[ex.id] == true) {
                            repo.removeFromDay(currentDay, ex.id)
                            addedState[ex.id] = false
                            Toast.makeText(
                                this@ExerciseLibraryActivity,
                                "Removed ${ex.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            repo.addToDay(
                                day = currentDay,
                                exercise = ex,
                                required = true, // all workouts are required now
                                preferred = ex.primaryEquipment,
                                targetReps = currentReps[ex.id]
                            )
                            addedState[ex.id] = true
                            Toast.makeText(
                                this@ExerciseLibraryActivity,
                                "Added ${ex.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        refreshPrimary()
                    }
                }
            }
        }
    }
}
