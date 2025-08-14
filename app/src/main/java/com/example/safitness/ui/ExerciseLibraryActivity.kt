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
import com.example.safitness.data.entities.MetconPlan
import com.example.safitness.data.repo.Repos
import kotlinx.coroutines.launch

class ExerciseLibraryActivity : AppCompatActivity() {

    private val vm: LibraryViewModel by viewModels {
        LibraryViewModelFactory(Repos.workoutRepository(this))
    }

    private enum class Mode { STRENGTH, METCON }
    private var mode: Mode = Mode.STRENGTH

    private var currentDay = 1

    private lateinit var spinnerType: Spinner
    private lateinit var spinnerEqOrDuration: Spinner
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

    // Selection state per strength exercise
    private val addedState = mutableMapOf<Long, Boolean>()

    // Metcon data + membership for this day
    private var allMetconPlans: List<MetconPlan> = emptyList()
    private var metconAddedIds: Set<Long> = emptySet()

    // Metcon filters (Activity-side)
    private var metconTypeFilter: String? = null
    private var metconDurationFilter: IntRange? = null

    // Remember spinner positions per mode (so switching back restores selection)
    private var strengthTypePos = 0
    private var strengthEqPos = 0
    private var metconTypePos = 0
    private var metconDurPos = 0

    // Metcon spinner options
    private val metconTypeLabels = arrayOf("All", "For time", "AMRAP", "EMOM")
    private val metconTypeMap = arrayOf<String?>(null, "FOR_TIME", "AMRAP", "EMOM")

    private val metconDurationLabels = arrayOf("All", "≤10 min", "11–20 min", "21–30 min", ">30 min")
    private val metconDurationRanges = arrayOf<IntRange?>(null, 0..10, 11..20, 21..30, 31..Int.MAX_VALUE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_library)

        spinnerType = findViewById(R.id.spinnerType)
        spinnerEqOrDuration = findViewById(R.id.spinnerEq)
        spinnerDay = findViewById(R.id.spinnerDay)
        listLibrary = findViewById(R.id.listLibrary)
        emptyText = findViewById(R.id.tvEmpty)
        btnClearFilters = findViewById(R.id.btnClearFilters)

        radioExercises = findViewById(R.id.rbExercises)
        radioMetcons = findViewById(R.id.rbMetcons)
        rvMetconPlans = findViewById(R.id.rvMetconPlans)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        currentDay = intent.getIntExtra("DAY_INDEX", 1).coerceIn(1, 5)

        // Day spinner
        spinnerDay.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Day 1", "Day 2", "Day 3", "Day 4", "Day 5")
        )
        spinnerDay.setSelection(currentDay - 1)
        spinnerDay.onItemSelectedListener = objSel { pos ->
            currentDay = (pos + 1).coerceIn(1, 5)
            vm.setMetconDay(currentDay) // keep metcon membership in sync
            refreshExerciseStates()
        }

        btnClearFilters.setOnClickListener {
            when (mode) {
                Mode.STRENGTH -> {
                    strengthTypePos = 0; strengthEqPos = 0
                    configureSpinnersForMode(Mode.STRENGTH)
                }
                Mode.METCON -> {
                    metconTypePos = 0; metconDurPos = 0
                    configureSpinnersForMode(Mode.METCON)
                    applyMetconFiltersAndSubmit()
                }
            }
        }

        // --- Strength (Exercises) list binding ---
        vm.exercises.observe(this) { list ->
            if (mode == Mode.STRENGTH) {
                renderStrengthList(list)
            } else {
                // keep silent; we'll re-render on mode flip
                cacheStrengthState(list)
            }
        }

        // --- Metcon plans binding ---
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
            allMetconPlans = plans ?: emptyList()
            if (mode == Mode.METCON) applyMetconFiltersAndSubmit()
        }
        vm.metconPlanIdsForDay.observe(this) { idSet ->
            metconAddedIds = idSet ?: emptySet()
            metconAdapter.updateMembership(metconAddedIds)
        }
        vm.setMetconDay(currentDay)

        // Toggle behaviour
        fun applyMode() {
            mode = if (radioMetcons.isChecked) Mode.METCON else Mode.STRENGTH
            rvMetconPlans.visibility = if (mode == Mode.METCON) View.VISIBLE else View.GONE
            listLibrary.visibility = if (mode == Mode.STRENGTH) View.VISIBLE else View.GONE
            emptyText.visibility = View.GONE
            configureSpinnersForMode(mode)
            if (mode == Mode.METCON) applyMetconFiltersAndSubmit()
            if (mode == Mode.STRENGTH) refreshExerciseStates()
        }
        radioExercises.setOnCheckedChangeListener { _, _ -> applyMode() }
        radioMetcons.setOnCheckedChangeListener { _, _ -> applyMode() }

        // Initial state
        radioExercises.isChecked = true
        radioMetcons.isChecked = false
        applyMode()
    }

    /* ---------------------------- Strength list helpers ---------------------------- */

    private fun renderStrengthList(list: List<Exercise>?) {
        if (list.isNullOrEmpty()) {
            emptyText.visibility = View.VISIBLE
            listLibrary.adapter = null
            return
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

    private fun cacheStrengthState(list: List<Exercise>?) {
        if (list.isNullOrEmpty()) return
        lifecycleScope.launch {
            val repo = Repos.workoutRepository(this@ExerciseLibraryActivity)
            list.forEach { ex ->
                addedState[ex.id] = repo.isInProgram(currentDay, ex.id)
                currentReps[ex.id] = repo.selectedTargetReps(currentDay, ex.id)
            }
        }
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

    /* ---------------------------- Spinner mode wiring ---------------------------- */

    private fun configureSpinnersForMode(newMode: Mode) {
        when (newMode) {
            Mode.STRENGTH -> {
                // Spinner 1 = WorkoutType
                spinnerType.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    arrayOf("All") + WorkoutType.values().map { it.name }
                )
                // Spinner 2 = Equipment
                spinnerEqOrDuration.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    arrayOf("All") + Equipment.values().map { it.name }
                )
                // Restore selections
                spinnerType.setSelection(strengthTypePos, false)
                spinnerEqOrDuration.setSelection(strengthEqPos, false)

                spinnerType.onItemSelectedListener = objSel { pos ->
                    strengthTypePos = pos
                    vm.setTypeFilter(if (pos == 0) null else WorkoutType.valueOf(spinnerType.selectedItem as String))
                }
                spinnerEqOrDuration.onItemSelectedListener = objSel { pos ->
                    strengthEqPos = pos
                    vm.setEqFilter(if (pos == 0) null else Equipment.valueOf(spinnerEqOrDuration.selectedItem as String))
                }
            }

            Mode.METCON -> {
                // Spinner 1 = Metcon Type
                spinnerType.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    metconTypeLabels
                )
                // Spinner 2 = Duration
                spinnerEqOrDuration.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    metconDurationLabels
                )
                // Restore selections
                spinnerType.setSelection(metconTypePos, false)
                spinnerEqOrDuration.setSelection(metconDurPos, false)

                spinnerType.onItemSelectedListener = objSel { pos ->
                    metconTypePos = pos
                    metconTypeFilter = metconTypeMap[pos]
                    applyMetconFiltersAndSubmit()
                }
                spinnerEqOrDuration.onItemSelectedListener = objSel { pos ->
                    metconDurPos = pos
                    metconDurationFilter = metconDurationRanges[pos]
                    applyMetconFiltersAndSubmit()
                }
            }
        }
    }

    private fun applyMetconFiltersAndSubmit() {
        val type = metconTypeFilter
        val range = metconDurationFilter
        val filtered = allMetconPlans.asSequence()
            .filter { plan -> type == null || plan.type.name == type }
            .filter { plan ->
                val minutes = plan.durationMinutes ?: 0
                range == null || minutes in range
            }
            .sortedBy { it.title }
            .toList()
        metconAdapter.submit(filtered, metconAddedIds)
        emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    /* ---------------------------- Shared helpers ---------------------------- */

    private fun objSel(block: (Int) -> Unit) =
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) =
                block(position)
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

    /* ---------------------------- Strength list adapter ---------------------------- */

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
                                required = true, // all workouts required
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
