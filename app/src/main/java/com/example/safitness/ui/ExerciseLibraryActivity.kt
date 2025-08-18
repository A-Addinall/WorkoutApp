package com.example.safitness.ui

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.safitness.R
import com.example.safitness.WorkoutApp
import com.example.safitness.core.Equipment
import com.example.safitness.core.WorkoutType
import com.example.safitness.data.entities.Exercise
import com.example.safitness.data.entities.MetconPlan
import com.example.safitness.data.repo.Repos
import com.example.safitness.ui.library.EnginePlanAdapter
import com.example.safitness.ui.library.EngineRow
import com.example.safitness.ui.library.SkillPlanAdapter
import com.example.safitness.ui.library.SkillRow
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExerciseLibraryActivity : AppCompatActivity() {

    private val vm: LibraryViewModel by viewModels {
        LibraryViewModelFactory(Repos.workoutRepository(this))
    }

    private enum class Mode { STRENGTH, METCON, ENGINE, SKILLS }
    private var mode: Mode = Mode.STRENGTH
    private var currentDay = 1

    // UI
    private lateinit var spinnerType: Spinner
    private lateinit var spinnerEqOrDuration: Spinner
    private lateinit var listLibrary: ListView
    private lateinit var emptyText: TextView
    private lateinit var btnClearFilters: Button
    private lateinit var rbExercises: RadioButton
    private lateinit var rbMetcons: RadioButton
    private lateinit var rbEngine: RadioButton
    private lateinit var rbSkills: RadioButton
    private lateinit var rvMetconPlans: RecyclerView

    // Adapters
    private lateinit var metconAdapter: MetconPlanAdapter
    private lateinit var engineAdapter: EnginePlanAdapter
    private lateinit var skillAdapter: SkillPlanAdapter

    // Strength state
    private val currentReps = mutableMapOf<Long, Int?>()
    private val addedState = mutableMapOf<Long, Boolean>()

    // Metcon state
    private var allMetconPlans: List<MetconPlan> = emptyList()
    private var metconAddedIds: Set<Long> = emptySet()
    private var metconTypeFilter: String? = null
    private var metconDurationFilter: IntRange? = null

    // Remember spinner positions
    private var strengthTypePos = 0
    private var strengthEqPos = 0
    private var metconTypePos = 0
    private var metconDurPos = 0

    // Engine/Skill rows + current membership sets (kept in sync via repo observers)
    private var engineRows: List<EngineRow> = emptyList()
    private var skillRows: List<SkillRow> = emptyList()
    private var engineAddedIds: Set<Long> = emptySet()
    private var skillAddedIds: Set<Long> = emptySet()

    // Metcon spinner options
    private val metconTypeLabels = arrayOf("All", "For time", "AMRAP", "EMOM")
    private val metconTypeMap = arrayOf<String?>(null, "FOR_TIME", "AMRAP", "EMOM")
    private val metconDurationLabels = arrayOf("All", "≤10 min", "11–20 min", "21–30 min", ">30 min")
    private val metconDurationRanges = arrayOf<IntRange?>(null, 0..10, 11..20, 21..30, 31..Int.MAX_VALUE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_library)

        currentDay = intent.getIntExtra("DAY_INDEX", 1).coerceIn(1, 5)

        // Bind
        spinnerType = findViewById(R.id.spinnerType)
        spinnerEqOrDuration = findViewById(R.id.spinnerEq)
        listLibrary = findViewById(R.id.listLibrary)
        emptyText = findViewById(R.id.tvEmpty)
        btnClearFilters = findViewById(R.id.btnClearFilters)
        rbExercises = findViewById(R.id.rbExercises)
        rbMetcons = findViewById(R.id.rbMetcons)
        rbEngine = findViewById(R.id.rbEngine)
        rbSkills = findViewById(R.id.rbSkills)
        rvMetconPlans = findViewById(R.id.rvMetconPlans)
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        btnClearFilters.setOnClickListener {
            when (mode) {
                Mode.STRENGTH -> { strengthTypePos = 0; strengthEqPos = 0; configureSpinnersForMode(Mode.STRENGTH) }
                Mode.METCON   -> { metconTypePos = 0; metconDurPos = 0; configureSpinnersForMode(Mode.METCON); applyMetconFiltersAndSubmit() }
                Mode.ENGINE, Mode.SKILLS -> { /* no filters yet */ }
            }
        }

        /* ----- Strength ----- */
        vm.exercises.observe(this) { list -> renderStrengthList(list) }

        /* ----- Metcon / Engine / Skills share the same RecyclerView ----- */
        rvMetconPlans.layoutManager = LinearLayoutManager(this)

        // Metcon adapter (persisted via VM/repo)
        metconAdapter = MetconPlanAdapter(
            onPrimary = { plan, isAdded ->
                if (isAdded) vm.removeMetconFromDay(currentDay, plan.id)
                else vm.addMetconToDay(currentDay, plan.id, required = true, order = metconAddedIds.size)
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

        // Engine/Skills adapters
        engineAdapter = EnginePlanAdapter { row, isAdded ->
            val repo = Repos.workoutRepository(this)
            lifecycleScope.launch {
                if (isAdded) {
                    repo.removeEngineFromDay(currentDay, row.id)
                } else {
                    val order = engineAddedIds.size // use repo-observed membership size
                    repo.addEngineToDay(currentDay, row.id, required = true, orderInDay = order)
                }
            }
        }
        skillAdapter = SkillPlanAdapter { row, isAdded ->
            val repo = Repos.workoutRepository(this)
            lifecycleScope.launch {
                if (isAdded) {
                    repo.removeSkillFromDay(currentDay, row.id)
                } else {
                    val order = skillAddedIds.size // use repo-observed membership size
                    repo.addSkillToDay(currentDay, row.id, required = true, orderInDay = order)
                }
            }
        }

        // Observe Engine/Skill membership sets directly from repo
        val repo = Repos.workoutRepository(this)
        repo.enginePlanIdsForDay(currentDay).asLiveData().observe(this) { ids ->
            engineAddedIds = ids ?: emptySet()
            if (mode == Mode.ENGINE) engineAdapter.updateMembership(engineAddedIds)
        }
        repo.skillPlanIdsForDay(currentDay).asLiveData().observe(this) { ids ->
            skillAddedIds = ids ?: emptySet()
            if (mode == Mode.SKILLS) skillAdapter.updateMembership(skillAddedIds)
        }

        /* ----- Mode toggle ----- */
        fun applyMode() {
            mode = when {
                rbMetcons.isChecked -> Mode.METCON
                rbEngine.isChecked  -> Mode.ENGINE
                rbSkills.isChecked  -> Mode.SKILLS
                else                -> Mode.STRENGTH
            }
            when (mode) {
                Mode.STRENGTH -> {
                    rvMetconPlans.visibility = View.GONE
                    listLibrary.visibility = View.VISIBLE
                    emptyText.visibility = View.GONE
                    configureSpinnersForMode(Mode.STRENGTH)
                    refreshExerciseStates()
                }
                Mode.METCON -> {
                    rvMetconPlans.visibility = View.VISIBLE
                    listLibrary.visibility = View.GONE
                    emptyText.visibility = View.GONE
                    rvMetconPlans.adapter = metconAdapter
                    configureSpinnersForMode(Mode.METCON)
                    applyMetconFiltersAndSubmit()
                }
                Mode.ENGINE -> {
                    rvMetconPlans.visibility = View.VISIBLE
                    listLibrary.visibility = View.GONE
                    emptyText.visibility = View.GONE
                    rvMetconPlans.adapter = engineAdapter
                    configureSpinnersForMode(Mode.ENGINE)
                    loadEngineRows()
                    engineAdapter.updateMembership(engineAddedIds)
                }
                Mode.SKILLS -> {
                    rvMetconPlans.visibility = View.VISIBLE
                    listLibrary.visibility = View.GONE
                    emptyText.visibility = View.GONE
                    rvMetconPlans.adapter = skillAdapter
                    configureSpinnersForMode(Mode.SKILLS)
                    loadSkillRows()
                    skillAdapter.updateMembership(skillAddedIds)
                }
            }
        }
        rbExercises.setOnCheckedChangeListener { _, _ -> applyMode() }
        rbMetcons.setOnCheckedChangeListener   { _, _ -> applyMode() }
        rbEngine.setOnCheckedChangeListener    { _, _ -> applyMode() }
        rbSkills.setOnCheckedChangeListener    { _, _ -> applyMode() }
        rbExercises.isChecked = true
        applyMode()
    }

    /* ---------------- Strength ---------------- */

    private fun renderStrengthList(list: List<Exercise>?) {
        if (mode != Mode.STRENGTH) return
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
            val sorted = list.sortedBy { it.name }
            listLibrary.adapter = LibraryAdapter(sorted)
        }
    }

    private fun refreshExerciseStates() {
        val adapter = listLibrary.adapter as? LibraryAdapter ?: return
        lifecycleScope.launch {
            val repo = Repos.workoutRepository(this@ExerciseLibraryActivity)
            adapter.items.forEach { ex ->
                addedState[ex.id] = repo.isInProgram(currentDay, ex.id)
            }
            (listLibrary.adapter as BaseAdapter).notifyDataSetChanged()
        }
    }

    /* ---------------- Engine ---------------- */

    private fun loadEngineRows() {
        lifecycleScope.launch {
            val db = (application as WorkoutApp).db
            val plans = withContext(Dispatchers.IO) {
                try { db.enginePlanDao().getPlans() } catch (_: Throwable) { emptyList() }
            }
            engineRows = plans.map { p ->
                val bits = mutableListOf<String>()
                p.mode?.let { if (it.isNotBlank()) bits.add(it) }
                p.intent?.let { if (it.isNotBlank()) bits.add(it.replace('_',' ').lowercase().replaceFirstChar(Char::titlecase)) }
                val durationMin = (p.programDurationSeconds ?: 0) / 60
                val dist = p.programDistanceMeters ?: 0
                val cals = p.programTargetCalories ?: 0
                when (p.intent) {
                    "FOR_TIME"     -> if (dist > 0) bits.add("$dist m")
                    "FOR_DISTANCE" -> if (durationMin > 0) bits.add("$durationMin min")
                    "FOR_CALORIES" -> if (cals > 0) bits.add("$cals cal") else if (durationMin > 0) bits.add("$durationMin min")
                }
                EngineRow(
                    id = p.id,
                    title = p.title ?: "Engine",
                    meta = bits.joinToString(" • ")
                )
            }.sortedBy { it.title }
            engineAdapter.submit(engineRows, engineAddedIds)
            emptyText.visibility = if (engineRows.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    /* ---------------- Skills ---------------- */

    private fun loadSkillRows() {
        lifecycleScope.launch {
            val db = (application as WorkoutApp).db
            val plans = withContext(Dispatchers.IO) {
                try { db.skillPlanDao().getPlans() } catch (_: Throwable) { emptyList() }
            }
            skillRows = plans.map { p ->
                val test = p.defaultTestType ?: "ATTEMPTS"
                val meta = when (test) {
                    "MAX_REPS_UNBROKEN" -> "Max reps (unbroken)"
                    "FOR_TIME_REPS"     -> "For time (reps)"
                    "MAX_HOLD_SECONDS"  -> "Max hold (seconds)"
                    else                -> "Attempts"
                }
                SkillRow(
                    id    = p.id,
                    title = p.title ?: "Skill",
                    meta  = meta
                )
            }.sortedBy { it.title }
            skillAdapter.submit(skillRows, skillAddedIds)
            emptyText.visibility = if (skillRows.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    /* ---------------- Spinners ---------------- */

    private fun configureSpinnersForMode(newMode: Mode) {
        when (newMode) {
            Mode.STRENGTH -> {
                spinnerType.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    arrayOf("All") + WorkoutType.values().map { it.name }
                )
                spinnerEqOrDuration.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    arrayOf("All") + Equipment.values().map { it.name }
                )
                spinnerType.setSelection(strengthTypePos, false)
                spinnerEqOrDuration.setSelection(strengthEqPos, false)

                spinnerType.onItemSelectedListener = sel { pos ->
                    strengthTypePos = pos
                    vm.setTypeFilter(if (pos == 0) null else WorkoutType.valueOf(spinnerType.selectedItem as String))
                }
                spinnerEqOrDuration.onItemSelectedListener = sel { pos ->
                    strengthEqPos = pos
                    vm.setEqFilter(if (pos == 0) null else Equipment.valueOf(spinnerEqOrDuration.selectedItem as String))
                }

                spinnerType.isEnabled = true
                spinnerEqOrDuration.isEnabled = true
            }
            Mode.METCON -> {
                spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, metconTypeLabels)
                spinnerEqOrDuration.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, metconDurationLabels)
                spinnerType.setSelection(metconTypePos, false)
                spinnerEqOrDuration.setSelection(metconDurPos, false)

                spinnerType.onItemSelectedListener = sel { pos ->
                    metconTypePos = pos
                    metconTypeFilter = metconTypeMap[pos]
                    applyMetconFiltersAndSubmit()
                }
                spinnerEqOrDuration.onItemSelectedListener = sel { pos ->
                    metconDurPos = pos
                    metconDurationFilter = metconDurationRanges[pos]
                    applyMetconFiltersAndSubmit()
                }

                spinnerType.isEnabled = true
                spinnerEqOrDuration.isEnabled = true
            }
            Mode.ENGINE, Mode.SKILLS -> {
                // No filters (yet) for Engine/Skills
                spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("—"))
                spinnerEqOrDuration.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("—"))
                spinnerType.onItemSelectedListener = null
                spinnerEqOrDuration.onItemSelectedListener = null
                spinnerType.isEnabled = false
                spinnerEqOrDuration.isEnabled = false
            }
        }
    }

    private fun sel(block: (Int) -> Unit) =
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) = block(position)
            override fun onNothingSelected(parent: AdapterView<*>) {}
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

    /* ---------------- Strength list adapter ---------------- */

    private inner class LibraryAdapter(val items: List<Exercise>) : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = items[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val holder: VH
            val row = if (convertView == null) {
                val v = layoutInflater.inflate(R.layout.item_library_row, parent, false)
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
            private val chipGroupReps = v.findViewById<ChipGroup>(R.id.chipGroupReps)
            private val btnPrimary = v.findViewById<Button>(R.id.btnPrimary)

            fun bind(ex: Exercise) {
                tvTitle.text = ex.name
                tvMeta.text = getString(R.string.reps)

                fun selectChipForReps(value: Int?) {
                    chipGroupReps.clearCheck()
                    when (value) {
                        3  -> chipGroupReps.check(R.id.chipReps3)
                        5  -> chipGroupReps.check(R.id.chipReps5)
                        8  -> chipGroupReps.check(R.id.chipReps8)
                        10 -> chipGroupReps.check(R.id.chipReps10)
                        12 -> chipGroupReps.check(R.id.chipReps12)
                        15 -> chipGroupReps.check(R.id.chipReps15)
                        else -> { /* none */ }
                    }
                }
                selectChipForReps(currentReps[ex.id])

                chipGroupReps.setOnCheckedStateChangeListener { _, checkedIds ->
                    val chosen: Int? = when (checkedIds.firstOrNull()) {
                        R.id.chipReps3  -> 3
                        R.id.chipReps5  -> 5
                        R.id.chipReps8  -> 8
                        R.id.chipReps10 -> 10
                        R.id.chipReps12 -> 12
                        R.id.chipReps15 -> 15
                        else -> null
                    }
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
                            Toast.makeText(this@ExerciseLibraryActivity, "Removed ${ex.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            repo.addToDay(
                                day = currentDay,
                                exercise = ex,
                                required = true,
                                preferred = ex.primaryEquipment,
                                targetReps = currentReps[ex.id]
                            )
                            addedState[ex.id] = true
                            Toast.makeText(this@ExerciseLibraryActivity, "Added ${ex.name}", Toast.LENGTH_SHORT).show()
                        }
                        refreshPrimary()
                    }
                }
            }
        }
    }
}
