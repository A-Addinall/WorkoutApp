package com.example.safitness.ui

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class ExerciseLibraryActivity : AppCompatActivity() {

    private val vm: LibraryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return LibraryViewModel(Repos.workoutRepository(this@ExerciseLibraryActivity)) as T
            }
        }
    }

    private enum class Mode { STRENGTH, METCON, ENGINE, SKILLS }

    /** Concrete date this screen edits. */
    private var epochDay: Long = LocalDate.now().toEpochDay()

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
    private lateinit var rvPlans: RecyclerView

    // Adapters
    private lateinit var metconAdapter: MetconPlanAdapter // inlined below
    private lateinit var engineAdapter: EnginePlanAdapter
    private lateinit var skillAdapter: SkillPlanAdapter

    // State
    private var mode: Mode = Mode.STRENGTH

    // Strength state
    private val currentReps = mutableMapOf<Long, Int?>()
    private val addedStrength = mutableMapOf<Long, Boolean>()

    // Metcon state
    private var allMetconPlans: List<MetconPlan> = emptyList()
    private var metconAddedIds: Set<Long> = emptySet()
    private var metconTypeFilter: String? = null
    private var metconDurationFilter: IntRange? = null
    private val metconTypeLabels = arrayOf("All", "For time", "AMRAP", "EMOM")
    private val metconTypeMap = arrayOf<String?>(null, "FOR_TIME", "AMRAP", "EMOM")
    private val metconDurationLabels = arrayOf("All", "≤10 min", "11–20 min", "21–30 min", ">30 min")
    private val metconDurationRanges = arrayOf<IntRange?>(null, 0..10, 11..20, 21..30, 31..Int.MAX_VALUE)

    // Engine / Skill state
    private var engineRows: List<EngineRow> = emptyList()
    private var skillRows: List<SkillRow> = emptyList()
    private var engineAddedIds: Set<Long> = emptySet()
    private var skillAddedIds: Set<Long> = emptySet()

    // Remember spinner positions
    private var strengthTypePos = 0
    private var strengthEqPos = 0
    private var metconTypePos = 0
    private var metconDurPos = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_library)

        epochDay = intent.getLongExtra(MainActivity.EXTRA_DATE_EPOCH_DAY, LocalDate.now().toEpochDay())
        vm.setTargetDate(epochDay)

        // Bind views
        spinnerType = findViewById(R.id.spinnerType)
        spinnerEqOrDuration = findViewById(R.id.spinnerEq)
        listLibrary = findViewById(R.id.listLibrary)
        emptyText = findViewById(R.id.tvEmpty)
        btnClearFilters = findViewById(R.id.btnClearFilters)
        rbExercises = findViewById(R.id.rbExercises)
        rbMetcons = findViewById(R.id.rbMetcons)
        rbEngine = findViewById(R.id.rbEngine)
        rbSkills = findViewById(R.id.rbSkills)
        rvPlans = findViewById(R.id.rvMetconPlans)
        findViewById<ImageView>(R.id.ivBack)?.setOnClickListener { finish() }

        // Listeners
        btnClearFilters.setOnClickListener {
            when (mode) {
                Mode.STRENGTH -> { strengthTypePos = 0; strengthEqPos = 0; configureSpinnersForMode(Mode.STRENGTH) }
                Mode.METCON   -> { metconTypePos = 0; metconDurPos = 0; configureSpinnersForMode(Mode.METCON); applyMetconFiltersAndSubmit() }
                Mode.ENGINE, Mode.SKILLS -> Unit
            }
        }

        // Strength
        vm.exercises.observe(this) { renderStrengthList(it) }

        // Recycler setup for (Metcon/Engine/Skills)
        rvPlans.layoutManager = LinearLayoutManager(this)

        // Metcon adapter (inline)
        metconAdapter = MetconPlanAdapter { plan, isAdded ->
            if (isAdded) vm.removeMetconFromDate(epochDay, plan.id)
            else vm.addMetconToDate(epochDay, plan.id, required = true, order = metconAddedIds.size)
        }

        // Engine/Skill adapters
        engineAdapter = EnginePlanAdapter { row, isAdded ->
            if (isAdded) vm.removeEngineFromDate(epochDay, row.id)
            else vm.addEngineToDate(epochDay, row.id, required = true, order = engineAddedIds.size)
        }
        skillAdapter = SkillPlanAdapter { row, isAdded ->
            if (isAdded) vm.removeSkillFromDate(epochDay, row.id)
            else vm.addSkillToDate(epochDay, row.id, required = true, order = skillAddedIds.size)
        }

        // Observe data
        vm.metconPlans.observe(this) { plans ->
            allMetconPlans = plans ?: emptyList()
            if (mode == Mode.METCON) applyMetconFiltersAndSubmit()
        }
        vm.metconPlanIdsForDate.observe(this) { idSet ->
            metconAddedIds = idSet ?: emptySet()
            if (mode == Mode.METCON) metconAdapter.updateMembership(metconAddedIds)
        }
        vm.enginePlanIdsForDate.observe(this) { ids ->
            engineAddedIds = ids ?: emptySet()
            if (mode == Mode.ENGINE) engineAdapter.updateMembership(engineAddedIds)
        }
        vm.skillPlanIdsForDate.observe(this) { ids ->
            skillAddedIds = ids ?: emptySet()
            if (mode == Mode.SKILLS) skillAdapter.updateMembership(skillAddedIds)
        }

        // Mode switching
        fun applyMode() {
            mode = when {
                rbMetcons.isChecked -> Mode.METCON
                rbEngine.isChecked  -> Mode.ENGINE
                rbSkills.isChecked  -> Mode.SKILLS
                else                -> Mode.STRENGTH
            }
            when (mode) {
                Mode.STRENGTH -> {
                    rvPlans.visibility = View.GONE
                    listLibrary.visibility = View.VISIBLE
                    emptyText.visibility = View.GONE
                    configureSpinnersForMode(Mode.STRENGTH)
                    refreshStrengthMembership()
                }
                Mode.METCON -> {
                    rvPlans.adapter = metconAdapter
                    rvPlans.visibility = View.VISIBLE
                    listLibrary.visibility = View.GONE
                    emptyText.visibility = View.GONE
                    configureSpinnersForMode(Mode.METCON)
                    applyMetconFiltersAndSubmit()
                }
                Mode.ENGINE -> {
                    rvPlans.adapter = engineAdapter
                    rvPlans.visibility = View.VISIBLE
                    listLibrary.visibility = View.GONE
                    emptyText.visibility = View.GONE
                    configureSpinnersForMode(Mode.ENGINE)
                    loadEngineRows()
                    engineAdapter.updateMembership(engineAddedIds)
                }
                Mode.SKILLS -> {
                    rvPlans.adapter = skillAdapter
                    rvPlans.visibility = View.VISIBLE
                    listLibrary.visibility = View.GONE
                    emptyText.visibility = View.GONE
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
                addedStrength[ex.id] = repo.isInProgramForDate(epochDay, ex.id)
                currentReps[ex.id] = repo.selectedTargetRepsForDate(epochDay, ex.id)
            }
            val sorted = list.sortedBy { it.name }
            listLibrary.adapter = StrengthAdapter(sorted)
        }
    }

    private fun refreshStrengthMembership() {
        val adapter = listLibrary.adapter as? StrengthAdapter ?: return
        lifecycleScope.launch {
            val repo = Repos.workoutRepository(this@ExerciseLibraryActivity)
            adapter.items.forEach { ex ->
                addedStrength[ex.id] = repo.isInProgramForDate(epochDay, ex.id)
            }
            (listLibrary.adapter as BaseAdapter).notifyDataSetChanged()
        }
    }

    private inner class StrengthAdapter(val items: List<Exercise>) : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = items[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val holder: VH
            val row = if (convertView == null) {
                val v = layoutInflater.inflate(R.layout.item_library_row, parent, false)
                holder = VH(v)
                v.tag = holder
                v
            } else {
                (convertView.tag as VH).also { holder = it }
                convertView
            }
            holder.bind(getItem(position))
            return row
        }

        private inner class VH(v: View) {
            private val tvTitle = v.findViewById<TextView>(R.id.tvTitle)
            private val tvMeta = v.findViewById<TextView>(R.id.tvMeta)
            private val chipGroupReps = v.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupReps)
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
                        else -> Unit
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
                    lifecycleScope.launch {
                        Repos.workoutRepository(this@ExerciseLibraryActivity)
                            .setStrengthTargetRepsForDate(epochDay, ex.id, chosen)
                    }
                }

                fun refreshPrimary() {
                    btnPrimary.text = if (addedStrength[ex.id] == true) "Remove" else "Add to Day"
                }
                refreshPrimary()

                btnPrimary.setOnClickListener {
                    lifecycleScope.launch {
                        val repo = Repos.workoutRepository(this@ExerciseLibraryActivity)
                        if (addedStrength[ex.id] == true) {
                            repo.removeStrengthFromDate(epochDay, ex.id)
                            addedStrength[ex.id] = false
                            Toast.makeText(this@ExerciseLibraryActivity, "Removed ${ex.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            vm.addStrengthToDate(epochDay, ex, required = true, preferred = ex.primaryEquipment, targetReps = currentReps[ex.id])
                            addedStrength[ex.id] = true
                            Toast.makeText(this@ExerciseLibraryActivity, "Added ${ex.name}", Toast.LENGTH_SHORT).show()
                        }
                        refreshPrimary()
                    }
                }
            }
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
                p.mode?.takeIf { it.isNotBlank() }?.let { bits.add(it) }
                p.intent?.takeIf { it.isNotBlank() }?.let { bits.add(it.replace('_',' ').lowercase().replaceFirstChar { c -> c.titlecase() }) }
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
            }.sortedBy { it.title ?: "" }
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
            }.sortedBy { it.title ?: "" }
            skillAdapter.submit(skillRows, skillAddedIds)
            emptyText.visibility = if (skillRows.isEmpty()) View.VISIBLE else View.GONE
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

                spinnerType.onItemSelectedListener = onSel { pos ->
                    strengthTypePos = pos
                    vm.setTypeFilter(if (pos == 0) null else WorkoutType.valueOf(spinnerType.selectedItem as String))
                }
                spinnerEqOrDuration.onItemSelectedListener = onSel { pos ->
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

                spinnerType.onItemSelectedListener = onSel { pos ->
                    metconTypePos = pos
                    metconTypeFilter = metconTypeMap[pos]
                    applyMetconFiltersAndSubmit()
                }
                spinnerEqOrDuration.onItemSelectedListener = onSel { pos ->
                    metconDurPos = pos
                    metconDurationFilter = metconDurationRanges[pos]
                    applyMetconFiltersAndSubmit()
                }

                spinnerType.isEnabled = true
                spinnerEqOrDuration.isEnabled = true
            }
            Mode.ENGINE, Mode.SKILLS -> {
                spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("—"))
                spinnerEqOrDuration.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayOf("—"))
                spinnerType.onItemSelectedListener = null
                spinnerEqOrDuration.onItemSelectedListener = null
                spinnerType.isEnabled = false
                spinnerEqOrDuration.isEnabled = false
            }
        }
    }

    private fun onSel(block: (Int) -> Unit) =
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
        rvPlans.adapter = metconAdapter
        metconAdapter.submit(filtered, metconAddedIds)
        emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    /* ---------------- Inline Metcon Adapter ---------------- */

    private class MetconPlanAdapter(
        private val onPrimary: (MetconPlan, Boolean) -> Unit
    ) : RecyclerView.Adapter<MetconPlanAdapter.VH>() {

        private var items: List<MetconPlan> = emptyList()
        private var membership: Set<Long> = emptySet()

        fun submit(list: List<MetconPlan>, memberIds: Set<Long>) {
            items = list
            membership = memberIds
            notifyDataSetChanged()
        }

        fun updateMembership(memberIds: Set<Long>) {
            membership = memberIds
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_metcon_plan_row, parent, false)
            val title = v.findViewById<TextView>(R.id.tvPlanTitle)
            val subtitle = v.findViewById<TextView>(R.id.tvPlanMeta)  // adjust if your ID differs
            val btn = v.findViewById<Button>(R.id.btnPlanPrimary)
            return VH(v, title, subtitle, btn)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val plan = items[position]
            val minutes = plan.durationMinutes ?: 0
            val type = plan.type.name.replace('_', ' ').lowercase()
                .replaceFirstChar { it.titlecase() }

            holder.title.text = plan.title ?: "Metcon"
            holder.subtitle.text = "$type • ${minutes}m"

            val isAdded = membership.contains(plan.id)
            holder.btn.text = if (isAdded) "Remove" else "Add to Day"
            holder.btn.setOnClickListener { onPrimary(plan, isAdded) }
        }


        override fun getItemCount(): Int = items.size

        class VH(
            view: View,
            val title: TextView,
            val subtitle: TextView,
            val btn: Button
        ) : RecyclerView.ViewHolder(view)
    }
}
