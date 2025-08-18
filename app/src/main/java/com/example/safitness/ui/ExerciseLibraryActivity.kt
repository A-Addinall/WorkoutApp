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
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// NEW imports for Engine/Skills
import com.example.safitness.core.EngineMode
import com.example.safitness.core.EngineIntent
import com.example.safitness.core.SkillType
import com.example.safitness.core.SkillTestType
import com.example.safitness.data.db.AppDatabase
import com.example.safitness.data.entities.DayEngineSkillEntity
import com.example.safitness.ui.library.EngineLibraryAdapter
import com.example.safitness.ui.library.EngineUiItem
import com.example.safitness.ui.library.SkillLibraryAdapter
import com.example.safitness.ui.library.SkillUiItem

class ExerciseLibraryActivity : AppCompatActivity() {

    private val vm: LibraryViewModel by viewModels {
        LibraryViewModelFactory(Repos.workoutRepository(this))
    }

    // NEW adapters/state for Engine & Skills
    private lateinit var engineAdapter: EngineLibraryAdapter
    private lateinit var skillAdapter: SkillLibraryAdapter
    private var selectedEngineKeys: Set<String> = emptySet()
    private var selectedSkillKeys: Set<String> = emptySet()
    private var engineUiCache: List<EngineUiItem> = emptyList()
    private var skillUiCache: List<SkillUiItem> = emptyList()

    // Existing
    private enum class Mode { STRENGTH, METCON, ENGINE, SKILLS } // NEW modes
    private var mode: Mode = Mode.STRENGTH
    private var currentDay = 1  // passed from MainActivity Edit; used ONLY for membership & persistence

    // UI
    private lateinit var spinnerType: Spinner
    private lateinit var spinnerEqOrDuration: Spinner
    private lateinit var listLibrary: ListView
    private lateinit var emptyText: TextView
    private lateinit var btnClearFilters: Button
    private lateinit var radioExercises: RadioButton
    private lateinit var radioMetcons: RadioButton
    private lateinit var radioEngine: RadioButton    // NEW
    private lateinit var radioSkills: RadioButton    // NEW
    private lateinit var rvMetconPlans: RecyclerView
    private lateinit var metconAdapter: MetconPlanAdapter

    // Strength membership & reps state (in-memory cache for the list)
    private val currentReps = mutableMapOf<Long, Int?>()
    private val addedState = mutableMapOf<Long, Boolean>()

    // Metcon lists + membership
    private var allMetconPlans: List<MetconPlan> = emptyList()
    private var metconAddedIds: Set<Long> = emptySet()
    private var metconTypeFilter: String? = null
    private var metconDurationFilter: IntRange? = null

    // Remember spinner positions per mode
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

        // Day comes from MainActivity -> Edit (NOT a catalogue filter)
        currentDay = intent.getIntExtra("DAY_INDEX", 1).coerceIn(1, 5)

        // Bind
        spinnerType = findViewById(R.id.spinnerType)
        spinnerEqOrDuration = findViewById(R.id.spinnerEq)
        listLibrary = findViewById(R.id.listLibrary)
        emptyText = findViewById(R.id.tvEmpty)
        btnClearFilters = findViewById(R.id.btnClearFilters)
        radioExercises = findViewById(R.id.rbExercises)
        radioMetcons = findViewById(R.id.rbMetcons)
        radioEngine = findViewById(R.id.rbEngine)   // NEW
        radioSkills = findViewById(R.id.rbSkills)   // NEW
        rvMetconPlans = findViewById(R.id.rvMetconPlans)
        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        // NEW: init Engine/Skill adapters
        engineAdapter = EngineLibraryAdapter(this) { item -> toggleEngineItem(item) }
        skillAdapter  = SkillLibraryAdapter(this)  { item -> toggleSkillItem(item) }

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
                Mode.ENGINE, Mode.SKILLS -> {
                    // No filters for these modes; nothing to clear
                }
            }
        }

        // Strength catalogue (ALWAYS full list from VM)
        vm.exercises.observe(this) { list ->
            renderStrengthList(list)
        }

        // Metcon catalogue (ALWAYS full list; membership only affects button text)
        rvMetconPlans.layoutManager = LinearLayoutManager(this)
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
        vm.setMetconDay(currentDay) // only for membership state

        // NEW: observe static Engine/Skill UI lists once
        vm.engineItems.observe(this) { items ->
            engineUiCache = items ?: emptyList()
            if (mode == Mode.ENGINE) {
                engineAdapter.submit(engineUiCache, selectedEngineKeys)
                listLibrary.adapter = engineAdapter
            }
        }
        vm.skillItems.observe(this) { items ->
            skillUiCache = items ?: emptyList()
            if (mode == Mode.SKILLS) {
                skillAdapter.submit(skillUiCache, selectedSkillKeys)
                listLibrary.adapter = skillAdapter
            }
        }

        // Mode toggle
        fun applyMode() {
            mode = when {
                radioMetcons.isChecked -> Mode.METCON
                radioEngine.isChecked  -> Mode.ENGINE
                radioSkills.isChecked  -> Mode.SKILLS
                else -> Mode.STRENGTH
            }

            // Visibility
            rvMetconPlans.visibility = if (mode == Mode.METCON) View.VISIBLE else View.GONE
            listLibrary.visibility   = if (mode != Mode.METCON) View.VISIBLE else View.GONE

            // Spinners: only relevant for Strength & Metcon
            val showSpinners = (mode == Mode.STRENGTH || mode == Mode.METCON)
            spinnerType.visibility = if (showSpinners) View.VISIBLE else View.GONE
            spinnerEqOrDuration.visibility = if (showSpinners) View.VISIBLE else View.GONE
            btnClearFilters.visibility = if (showSpinners) View.VISIBLE else View.GONE

            emptyText.visibility = View.GONE

            when (mode) {
                Mode.METCON -> {
                    configureSpinnersForMode(Mode.METCON)
                    applyMetconFiltersAndSubmit()
                }
                Mode.STRENGTH -> {
                    configureSpinnersForMode(Mode.STRENGTH)
                    refreshExerciseStates()
                }
                Mode.ENGINE -> {
                    // Show engine list with membership ✓s
                    engineAdapter.submit(engineUiCache, selectedEngineKeys)
                    listLibrary.adapter = engineAdapter
                    listLibrary.setOnItemClickListener { _, _, position, _ ->
                        val item = engineAdapter.getItem(position) as EngineUiItem
                        toggleEngineItem(item)
                    }
                    refreshEngineSkillSelections()
                }
                Mode.SKILLS -> {
                    // Show skills list with membership ✓s
                    skillAdapter.submit(skillUiCache, selectedSkillKeys)
                    listLibrary.adapter = skillAdapter
                    listLibrary.setOnItemClickListener { _, _, position, _ ->
                        val item = skillAdapter.getItem(position) as SkillUiItem
                        toggleSkillItem(item)
                    }
                    refreshEngineSkillSelections()
                }
            }
        }

        radioExercises.setOnCheckedChangeListener { _, _ -> applyMode() }
        radioMetcons.setOnCheckedChangeListener { _, _ -> applyMode() }
        radioEngine.setOnCheckedChangeListener { _, _ -> applyMode() } // NEW
        radioSkills.setOnCheckedChangeListener { _, _ -> applyMode() } // NEW

        radioExercises.isChecked = true
        applyMode()
    }

    /* ---------------- Strength helpers ---------------- */

    private fun renderStrengthList(list: List<Exercise>?) {
        if (list.isNullOrEmpty()) {
            emptyText.visibility = View.VISIBLE
            listLibrary.adapter = null
            return
        }
        emptyText.visibility = View.GONE

        // Preload membership + reps (do NOT filter the list)
        lifecycleScope.launch {
            val repo = Repos.workoutRepository(this@ExerciseLibraryActivity)
            list.forEach { ex ->
                addedState[ex.id] = repo.isInProgram(currentDay, ex.id)
                currentReps[ex.id] = repo.selectedTargetReps(currentDay, ex.id)
            }
            // sort by name for stable display
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

    /* ---------------- Spinner wiring ---------------- */

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
                spinnerType.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    metconTypeLabels
                )
                spinnerEqOrDuration.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    metconDurationLabels
                )
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
            Mode.ENGINE, Mode.SKILLS -> {
                // No filters/spinners for these modes.
                spinnerType.onItemSelectedListener = null
                spinnerEqOrDuration.onItemSelectedListener = null
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

    /* ---------------- Shared helpers ---------------- */

    private fun objSel(block: (Int) -> Unit) =
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) =
                block(position)
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

    /* ---------------- NEW: Engine/Skill selection helpers ---------------- */

    private fun refreshEngineSkillSelections() {
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                AppDatabase.get(this@ExerciseLibraryActivity)
                    .dayEngineSkillDao()
                    .forDay(1, currentDay) // dev: week=1
            }
            selectedEngineKeys = items.filter { it.kind == "ENGINE" }
                .map { "${it.engineMode}|${it.engineIntent}" }
                .toSet()
            selectedSkillKeys = items.filter { it.kind == "SKILL" }
                .map { "${it.skill}|${it.skillTestType}" }
                .toSet()

            if (::engineAdapter.isInitialized) {
                engineAdapter.submit(engineUiCache, selectedEngineKeys)
            }
            if (::skillAdapter.isInitialized) {
                skillAdapter.submit(skillUiCache, selectedSkillKeys)
            }
        }
    }

    private fun toggleEngineItem(item: EngineUiItem) {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.get(this@ExerciseLibraryActivity).dayEngineSkillDao()
            val removed = dao.deleteEngine(week = 1, day = currentDay, mode = item.mode, intent = item.intent)
            if (removed == 0) {
                dao.insert(
                    DayEngineSkillEntity(
                        weekIndex = 1, dayIndex = currentDay, orderIndex = 0,
                        kind = "ENGINE",
                        engineMode = item.mode,
                        engineIntent = item.intent
                    )
                )
            }
            withContext(Dispatchers.Main) { refreshEngineSkillSelections() }
        }
    }

    private fun toggleSkillItem(item: SkillUiItem) {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.get(this@ExerciseLibraryActivity).dayEngineSkillDao()
            val removed = dao.deleteSkill(week = 1, day = currentDay, skill = item.skill, testType = item.testType)
            if (removed == 0) {
                dao.insert(
                    DayEngineSkillEntity(
                        weekIndex = 1, dayIndex = currentDay, orderIndex = 0,
                        kind = "SKILL",
                        skill = item.skill,
                        skillTestType = item.testType
                    )
                )
            }
            withContext(Dispatchers.Main) { refreshEngineSkillSelections() }
        }
    }

    /* ---------------- Strength list adapter (unchanged) ---------------- */

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
            private val chipGroupReps = v.findViewById<ChipGroup>(R.id.chipGroupReps)
            private val btnPrimary = v.findViewById<Button>(R.id.btnPrimary)

            fun bind(ex: Exercise) {
                tvTitle.text = ex.name
                tvMeta.text = getString(R.string.reps)

                // Preselect chip from saved reps (or none)
                fun selectChipForReps(value: Int?) {
                    chipGroupReps.clearCheck()
                    when (value) {
                        3  -> chipGroupReps.check(R.id.chipReps3)
                        5  -> chipGroupReps.check(R.id.chipReps5)
                        8  -> chipGroupReps.check(R.id.chipReps8)
                        10 -> chipGroupReps.check(R.id.chipReps10)
                        12 -> chipGroupReps.check(R.id.chipReps12)
                        15 -> chipGroupReps.check(R.id.chipReps15)
                        else -> { /* none selected = null */ }
                    }
                }

                selectChipForReps(currentReps[ex.id])

                // Listen for user changes on chips
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
                                targetReps = currentReps[ex.id]  // may be null
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
